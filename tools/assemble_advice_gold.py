#!/usr/bin/env python3
"""Ghép ca định trước với đánh giá lời tư vấn và nhãn trang bán đã được đọc lại."""
import argparse
from datetime import datetime
import json
from pathlib import Path


def stamp(value):
    return datetime.fromisoformat(value.replace('Z','+00:00')).timestamp() if value else 0


def choose_price_label(labels, url, times):
    candidates=[l for l in labels if l['url']==url]
    # Lỗi mạng / trang chặn không phải một quan sát giá. Mọi giá đọc được, kể cả sai giá app hoặc
    # chưa được chấm, vẫn cạnh tranh theo thời gian; không lọc theo kết quả so giá để tăng điểm.
    readable=[l for l in candidates if isinstance(l.get('unitPriceVnd'),(int,float)) and l['unitPriceVnd']>0
              and l.get('title') and l.get('snapshot') and Path(l['snapshot']).is_file()]
    pool=readable or candidates
    return min(pool,key=lambda l:max(abs(stamp(l['capturedAt'])-t) for t in times)) if pool else None


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--cases',type=Path,required=True)
    p.add_argument('--runs',type=Path,required=True)
    p.add_argument('--prices',type=Path,required=True)
    p.add_argument('--reviews',type=Path,required=True)
    p.add_argument('--output',type=Path,required=True)
    a=p.parse_args()
    cases=json.loads(a.cases.read_text())
    reviews=json.loads(a.reviews.read_text())
    labels=[dict(label,**reviews.get('prices',{}).get(label.get('snapshot'),{})) for label in json.loads(a.prices.read_text())]
    gold=[]
    for c in cases:
        row=dict(c,run=c['id']+'.json',adviceQuality=reviews.get('advice',reviews).get(c['id'],{}),prices=[])
        file=a.runs/row['run']
        if file.exists():
            r=json.loads(file.read_text()).get('result') or {}
            cards=r.get('options') or ([r['build']] if r.get('build') else [])
            by_url={}
            for card in cards:
                for line in card.get('items',[]):
                    url=line.get('sourceUrl')
                    if url:by_url.setdefault(url,[]).append(stamp(line.get('checkedAt')))
            for url,times in by_url.items():
                label=choose_price_label(labels,url,times)
                if label:
                    row['prices'].append(label)
        gold.append(row)
    a.output.write_text(json.dumps(gold,ensure_ascii=False,indent=2)+'\n')
    print('gold',len(gold),'reviewed advice',sum(g['adviceQuality'].get('pass') is not None for g in gold))


if __name__=='__main__':
    raise SystemExit(main())
