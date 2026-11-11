#!/usr/bin/env python3
"""Run upgrade advice through the real jobs API and check declared expectations."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import json
from pathlib import Path
import re
import time
from check_advice_live import request_json


def check(case, result, expected, previous_total=None):
    issues = []
    card = result.get('build')
    lines = (card or {}).get('items') or []
    if expected.get('noCard'):
        if lines:
            issues.append('Expected no purchase card')
    elif not lines and not expected.get('allowNoCard'):
        issues.append('Expected a useful purchasable upgrade, got no card')
    total = sum(l.get('priceMaxVnd') or 0 for l in lines)
    if total > case['upgrade']['budgetVnd']:
        issues.append('Exceeded upgrade budget')
    if expected.get('cheaper') and (previous_total is None or total >= previous_total):
        issues.append('Follow-up did not reduce the total cost')
    categories = {l['category'] for l in lines}
    for c in expected.get('require', []) if lines or not expected.get('allowNoCard') else []:
        if c not in categories:
            issues.append('Missing category: ' + c)
    for c in expected.get('forbid', []):
        if c in categories:
            issues.append('Unexpected category: ' + c)
    for l in lines:
        if not (l.get('priceChecked') and l.get('inStock') is True and (l.get('priceMinVnd') or 0) > 0):
            issues.append('Unverified price/stock: ' + str(l.get('productName')))
        if not (l.get('sourceUrl') or '').startswith('https://'):
            issues.append('Missing HTTPS product page')
        if l['category'] == 'gpu' and expected.get('gpuPattern'):
            if not re.search(expected['gpuPattern'], l.get('productName', ''), re.I):
                issues.append('Wrong GPU requirement')
        if l['category'] == 'ram' and expected.get('maxRamSticks'):
            old_sticks = expected.get('oldRamSticks', 0) if l.get('name', '').lower().startswith('extra') else 0
            if old_sticks + l['quantity'] > expected['maxRamSticks']:
                issues.append('RAM plan exceeds physical slot count')
            title = l.get('productName', '')
            kit = re.search(r'(\d+)\s*[x×]\s*(\d+)\s*GB', title, re.I)
            size = re.search(r'(\d+)\s*GB', title, re.I)
            if size:
                per_module = int(kit[2]) if kit else int(size[1])
                total_gb = per_module * l['quantity'] + (expected.get('oldRamGb', 0) if old_sticks else 0)
                if total_gb < expected.get('minRamGb', 0):
                    issues.append('RAM plan does not reach required capacity')
        if l['category'] == 'ram' and expected.get('ramType'):
            if expected['ramType'] not in l.get('productName', '').upper():
                issues.append('Wrong RAM generation')
    if expected.get('replyPattern') and not re.search(expected['replyPattern'], result.get('reply', ''), re.I):
        issues.append('Reply did not explain the expected condition')
    return {'issues': issues, 'totalVnd': total, 'categories': sorted(categories)}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--cases', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--base', default='http://127.0.0.1:8084')
    p.add_argument('--workers', type=int, choices=[1, 2], default=1)
    args = p.parse_args()
    cases = json.loads(args.cases.read_text())
    args.output.mkdir(parents=True, exist_ok=True)

    def run(case):
        messages = case['messages'][:]
        previous = None
        outcomes = []
        for step, spec in enumerate([{'expected': case['expected']}] + case.get('followups', [])):
            if step:
                wire = '; '.join(f"{l['category']}: {l.get('productName')} ({l['priceMaxVnd']} VND)" for l in previous.get('build', {}).get('items', []))
                messages.append({'role': 'assistant', 'content': previous['reply'] + '\n[Previous upgrade plan: ' + wire + ']'})
                messages.append({'role': 'user', 'content': spec['message']})
            label = f"{case['id']}-{step}"
            request = {'mode': 'upgrade', 'upgrade': case['upgrade'], 'messages': messages}
            (args.output / f'{label}-request.json').write_text(json.dumps(request, indent=2))
            print('START', label, flush=True)
            start = time.monotonic()
            job = request_json(args.base + '/api/chat/jobs', request)
            while job['status'] == 'running':
                if time.monotonic() - start > 180:
                    raise TimeoutError(label)
                time.sleep(2)
                job = request_json(args.base + '/api/chat/jobs/' + job['id'])
                (args.output / f'{label}-job.json').write_text(json.dumps(job, ensure_ascii=False, indent=2))
            result = job.get('result') or {}
            prior_total = outcomes[-1]['totalVnd'] if outcomes else None
            verdict = check(case, result, spec['expected'], prior_total) if job['status'] == 'done' else {'issues': [job.get('error') or 'Job failed'], 'totalVnd': 0}
            verdict.update({'id': label, 'status': job['status'], 'seconds': round(time.monotonic()-start), 'reply': result.get('reply')})
            outcomes.append(verdict)
            print(json.dumps(verdict, ensure_ascii=False), flush=True)
            previous = result
            if not result or not result.get('build'):
                break
        return outcomes

    def safe_run(case):
        try:
            return run(case)
        except Exception as e:
            return [{'id': case['id'], 'issues': [str(e)], 'status': 'error'}]

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        results = [v for rows in pool.map(safe_run, cases) for v in rows]
    (args.output / 'summary.json').write_text(json.dumps(results, ensure_ascii=False, indent=2))
    return int(any(r['issues'] for r in results))


if __name__ == '__main__':
    raise SystemExit(main())
