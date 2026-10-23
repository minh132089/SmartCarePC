#!/usr/bin/env python3
"""Đọc trang bán qua Jina, tách khỏi parser giá của app; mọi nhãn vẫn cần người đọc xác nhận."""
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import time
import threading
import subprocess
import urllib.request

MONEY = re.compile(r"(?<![\d/])(\d{1,3}(?:[.,]\d{3})+|\d{5,9})\s*(?:₫|đ(?:ồng)?|vn[dđ])", re.I)
RATE_LOCK = threading.Lock()
NEXT_REQUEST = 0.0


class PageText(HTMLParser):
    def __init__(self):
        super().__init__(); self.hidden=0; self.text=[]; self.scripts=[]; self.script=None; self.h1=None; self.headings=[]
    def handle_starttag(self, tag, attrs):
        if tag=='h1':self.h1=[]
        if tag in ('script','style'):
            self.hidden+=1
            if tag=='script' and dict(attrs).get('type')=='application/ld+json':self.script=[]
    def handle_endtag(self, tag):
        if tag=='h1' and self.h1 is not None:self.headings.append(''.join(self.h1).strip());self.h1=None
        if tag in ('script','style'):
            self.hidden=max(0,self.hidden-1)
            if tag=='script' and self.script is not None:self.scripts.append(''.join(self.script));self.script=None
    def handle_data(self, data):
        if self.h1 is not None:self.h1.append(data)
        if self.script is not None:self.script.append(data)
        if not self.hidden and data.strip():self.text.append(data.strip())


def product_nodes(node):
    if isinstance(node,list):
        for item in node:yield from product_nodes(item)
    elif isinstance(node,dict):
        if node.get('@type')=='Product' or 'Product' in (node.get('@type') or []):yield node
        if '@graph' in node:yield from product_nodes(node['@graph'])


def direct_evidence(url, path):
    req=urllib.request.Request(url,headers={'User-Agent':'SmartCarePC-evaluation/1.0 (independent price comparison)'})
    try:
        with urllib.request.urlopen(req,timeout=30) as response:html=response.read().decode('utf-8',errors='replace')
    except OSError:
        # Một số cửa hàng ngắt TLS với urllib; thử một lần bằng curl, vẫn đọc trang gốc và cùng UA trung thực.
        response=subprocess.run(['curl','--fail','--location','--max-time','30','--silent','--show-error',
                                 '--user-agent','SmartCarePC-evaluation/1.0 (independent price comparison)',url],
                                capture_output=True,check=True,timeout=35)
        html=response.stdout.decode('utf-8',errors='replace')
    path.write_text(html)
    page=PageText();page.feed(html)
    visible='\n'.join(page.text)
    for script in page.scripts:
        try:nodes=list(product_nodes(json.loads(script)))
        except ValueError:continue
        for node in nodes:
            if not node.get('name'):continue
            price,excerpt=price_excerpt(visible,url,node['name'])
            offers=node.get('offers') or []
            if isinstance(offers,dict):offers=[offers]
            prices={int(float(str(o.get('price')).replace(',',''))) for o in offers if isinstance(o,dict) and re.fullmatch(r'\d+(?:\.0+)?',str(o.get('price')))}
            if len(prices)==1:
                quoted=next(iter(prices))
                if price is not None and price != quoted:
                    return node['name'], None, f'Giá có cấu trúc: {quoted}; vùng chữ có giá khác: {price}. Cần đọc vùng sản phẩm chính.\n'+excerpt
                return node['name'], price or quoted, excerpt or json.dumps(offers,ensure_ascii=False)
    title=next((h for h in page.headings if h),'')
    price,excerpt=price_excerpt(visible,url,title)
    return title,price,excerpt


def price_excerpt(text, url, title=None):
    lines = text.splitlines()
    headings=[i for i,line in enumerate(lines) if re.match(r'^#\s+\S',line)]
    if headings:lines=lines[headings[0]:]
    elif title:
        headings=[i for i,line in enumerate(lines) if line.strip()==title.strip()]
        if headings:lines=lines[headings[0]:]
    for i,line in enumerate(lines):
        if re.match(r'^(?:#+\s*)?(?:\*\*)?\[?Sản phẩm (?:đã xem|liên quan|tương tự|gợi ý)',line.strip(),re.I):
            lines=lines[:i];break
    candidates = []
    for i, line in enumerate(lines):
        # Giá trong danh sách liên quan / menu / ảnh không phải giá mua của trang này.
        if '](http' in line or line.lstrip().startswith(('![', '|')):
            continue
        matches = [m for m in MONEY.finditer(line) if int(re.sub(r'\D', '', m.group(1))) >= 10000]
        if not matches:
            continue
        context = ' '.join(lines[max(0, i-2):i+1])
        if re.search(r'trả góp|trả trước|phí vận chuyển|giảm thêm|giá gốc', line, re.I):
            continue
        # Khi trang ghi cả khuyến mại và mua online, dùng đúng giá kênh mua trực tuyến.
        priority = 0 if re.search(r'giá mua online|deal sốc', context, re.I) else 1 if re.search(r'giá tân doanh|giá bán|giá khuyến m[ãạ]i', context, re.I) else 2
        candidates.append((priority, i, int(re.sub(r'\D', '', matches[0].group(1))), '\n'.join(lines[max(0,i-2):i+3])))
    candidates.sort(key=lambda x: (x[0], x[1]))
    return candidates[0][2:] if candidates else (None, '')


def capture(url, output):
    global NEXT_REQUEST
    with RATE_LOCK:
        slot = max(time.monotonic(), NEXT_REQUEST)
        NEXT_REQUEST = slot + 3.2
    time.sleep(max(0, slot-time.monotonic()))
    at = datetime.now(timezone.utc).isoformat()
    key = hashlib.sha256((url+at).encode()).hexdigest()[:20]
    path = output / (key+'.txt')
    try:
        req = urllib.request.Request('https://r.jina.ai/'+url, headers={'User-Agent':'SmartCarePC-evaluation/1.0', 'X-No-Cache':'true', 'X-Respond-With':'markdown'})
        with urllib.request.urlopen(req, timeout=70) as response:
            text = response.read().decode()
        path.write_text(text)
        title = next((line[7:] for line in text.splitlines() if line.startswith('Title: ')), '')
        price, excerpt = price_excerpt(text, url)
        direct_path=None
        if price is None:
            try:
                direct_path=path.with_suffix('.html')
                direct_title,price,excerpt=direct_evidence(url,direct_path)
                if direct_title:title=direct_title
            except Exception as error:excerpt='Jina không đọc được giá; đọc trực tiếp cũng lỗi: '+str(error)
        return dict(url=url, title=title, capturedAt=at, snapshot=str(path), directSnapshot=str(direct_path) if direct_path and direct_path.is_file() else None, unitPriceVnd=price, excerpt=excerpt, reviewed=False)
    except Exception as error:
        try:
            direct_path=path.with_suffix('.html')
            title,price,excerpt=direct_evidence(url,direct_path)
            return dict(url=url,title=title,capturedAt=at,snapshot=str(direct_path),directSnapshot=str(direct_path),
                        unitPriceVnd=price,excerpt=excerpt,jinaError=str(error),reviewed=False)
        except Exception as direct_error:
            return dict(url=url,capturedAt=at,error=f'{error}; trực tiếp: {direct_error}',reviewed=False)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--runs', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--watch-seconds', type=int, default=0)
    p.add_argument('--workers', type=int, default=2, choices=(1,2,3))
    args=p.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    index=args.output/'index.json'
    labels=json.loads(index.read_text()) if index.exists() else []
    attempted={x['url']:datetime.fromisoformat(x['capturedAt']).timestamp() for x in labels}
    end=time.monotonic()+args.watch_seconds
    while True:
        wanted={}
        for path in args.runs.glob('*.json'):
            try:
                payload=json.loads(path.read_text())
                if not isinstance(payload,dict):continue
                result=payload.get('result') or {}
                cards=result.get('options') or ([result['build']] if result.get('build') else [])
                for card in cards:
                    for line in card.get('items',[]):
                        if not line.get('sourceUrl'):continue
                        checked = datetime.fromisoformat(line['checkedAt'].replace('Z','+00:00')).timestamp() if line.get('checkedAt') else path.stat().st_mtime
                        wanted[line['sourceUrl']] = max(wanted.get(line['sourceUrl'],0),checked)
            except (ValueError, TypeError):
                continue
        pending=sorted((u for u,at in wanted.items() if u not in attempted or at-attempted[u]>300),key=lambda u:wanted[u])
        with ThreadPoolExecutor(max_workers=args.workers) as pool:
            tasks=[pool.submit(capture,u,args.output) for u in pending]
            for task in as_completed(tasks):
                label=task.result(); labels.append(label)
                attempted[label['url']]=datetime.fromisoformat(label['capturedAt']).timestamp()
                tmp=index.with_suffix('.tmp'); tmp.write_text(json.dumps(labels,ensure_ascii=False,indent=2));tmp.replace(index)
                print(label['url'], label.get('unitPriceVnd'), label.get('error',''), flush=True)
        if time.monotonic()>=end:break
        time.sleep(10)
    print('captured',len(labels),'readable',sum(x.get('unitPriceVnd') is not None for x in labels),flush=True)


if __name__=='__main__':
    raise SystemExit(main())
