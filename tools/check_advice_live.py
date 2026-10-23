#!/usr/bin/env python3
"""Gửi ca tư vấn thật, lưu phản hồi và đối chiếu các ràng buộc đã ghi trước."""
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
import json
import time
import urllib.request
from pathlib import Path

from evaluate_advice import evaluate_case


def request_json(url, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.load(response)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cases", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--base", default="http://127.0.0.1:8083")
    parser.add_argument("--workers", type=int, choices=range(1, 5), default=1)
    parser.add_argument("--resume", action="store_true", help="Dùng lại job đã xong trong đúng thư mục của lượt này")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    failed = unavailable = 0
    cases = json.loads(args.cases.read_text())
    def run_case(case):
        print("START", case["id"], flush=True)
        start = time.monotonic()
        path = args.output / (case["id"] + ".json")
        job = json.loads(path.read_text()) if args.resume and path.exists() else None
        if job is None or job.get("status") != "done":
            job = request_json(args.base + "/api/chat/jobs", {"mode": "advice", "messages": case["messages"]})
            while True:
                job = request_json(args.base + "/api/chat/jobs/" + job["id"])
                path.write_text(json.dumps(job, ensure_ascii=False, indent=2))
                if job["status"] != "running":
                    break
                if time.monotonic() - start >= 600:
                    raise TimeoutError(f"{case['id']}: chưa xong sau 600 giây; job lưu tại {path}")
                time.sleep(3)
        result = job.get("result") or {}
        cards, issues = evaluate_case(case, result) if job["status"] == "done" else ([], [job.get("error") or "API lỗi"])
        output = [f"{case['id']} {'FAIL: ' + '; '.join(issues) if issues else 'PASS ràng buộc'} {round(time.monotonic() - start)} giây", result.get("reply", "")]
        for card in cards:
            lines = card.get("items") or []
            total = sum(line.get("priceMaxVnd") or line.get("priceMinVnd") or 0 for line in lines)
            output.append(str((card.get("tier"), total, [(line.get("category"), line.get("productName"), line.get("quantity")) for line in lines if line.get("category") in ("cpu", "gpu", "ram")])))
        print("\n".join(output), flush=True)
        return bool(issues), not cards and not case["expected"].get("noCards")
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        tasks = {pool.submit(run_case, case): case for case in cases}
        for task in as_completed(tasks):
            try:
                bad, missing = task.result()
                failed += bad
                unavailable += missing
            except Exception as error:
                failed += 1
                print(tasks[task]["id"], "FAIL:", str(error), flush=True)
    print(f"Ca vi phạm: {failed}/{len(cases)}; chưa tìm được cấu hình: {unavailable}. Phần lời tư vấn và giá cần chấm độc lập.")
    return int(failed > 0)


if __name__ == "__main__":
    raise SystemExit(main())
