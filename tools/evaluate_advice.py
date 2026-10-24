#!/usr/bin/env python3
"""Chấm phản hồi tư vấn với nhãn quyết định và giá cửa hàng được thu thập độc lập."""

import argparse
import json
import re
from datetime import datetime
from pathlib import Path

THRESHOLD = 0.98
MIN_CASES = 100
MIN_PRICES = 100
MAX_CAPTURE_GAP_SECONDS = 600


def acceptance_passed(cases_total, price_total, decision_rate, advice_rate, price_rate, threshold=THRESHOLD):
    return (cases_total >= MIN_CASES and price_total >= MIN_PRICES
            and min(decision_rate, advice_rate, price_rate) >= threshold)


def timestamp(value):
    return datetime.fromisoformat(value.replace("Z", "+00:00")) if value else None


def gpu_vendor(title):
    title = title.lower().replace('™', '').replace('®', '')
    if re.search(r"\b(?:rtx|gtx)\s?\d{4}|nvidia|geforce", title):
        return "nvidia"
    if re.search(r"\brx\s?\d{4}|radeon", title):
        return "amd"
    if re.search(r"\barc\s?[ab]\d{3}", title):
        return "intel"
    return None


def vram_gb(title):
    found = re.search(r"\b(4|6|8|10|12|16|20|24)\s*g(?:b)?\b", title, re.I)
    return int(found.group(1)) if found else None


def gpu_model(title):
    title = title.replace('™', '').replace('®', '')
    match = re.search(r"\b(rtx|gtx|rx)\s*(\d{4})(?:\s*(ti\s*super|ti|super|xtx|xt|gre))?\b", title, re.I)
    return re.sub(r"\s+", "", match.group()).lower() if match else None


def tier_cap(budget, tier):
    return round(budget * 0.9) if tier == "cheap" else budget + 3_000_000 if tier == "quality" else budget


def ram_shape(title, source_url=None):
    # Đúng URL/SKU đã đối chiếu h1 trang FPT và datasheet Kingston ngày 23/10/2026.
    if re.search(r'\bKF432C16BB1AK2/32\b',title,re.I) or (source_url or '').split('?')[0].rstrip('/') == 'https://fptshop.com.vn/linh-kien/ram-kingston-fury-32-gb-ddr4-3200-mhz':return 2,16
    kit=re.search(r"(\d+)\s*x\s*(\d+)\s*GB",title,re.I)
    if kit:return int(kit.group(1)),int(kit.group(2))
    # Oracle theo thông số G.SKILL của đúng SKU; không dùng lại parser SKU của backend.
    if re.search(r'\bF5-6000U4040E16G[XA]2-TZ5K\b',title,re.I):return 2,16
    single=re.search(r'\b(\d+)\s*GB\b',title,re.I)
    return 1,int(single.group(1)) if single else 0


def fan_pack_size(title):
    match=re.search(r'(?:bộ|pack)\s*(\d+)\s*(?:quạt|fans?)|\b(\d+)\s*pack\b',title,re.I)
    return int(match.group(1) or match.group(2)) if match else 1


def evaluate_case(gold, result):
    expected = gold["expected"]
    cards = result.get("options") or ([result["build"]] if result.get("build") else [])
    by_tier = {card.get("tier"): card for card in cards}
    issues = []
    budget = expected.get("budgetVnd")
    if budget is not None and cards and all(sum(line.get("priceMaxVnd") or line.get("priceMinVnd") or 0 for line in card.get("items", [])) > budget for card in cards):
        issues.append("không có phương án nào trong ngân sách gốc")
    if "tiers" in expected and set(by_tier) != set(expected["tiers"]):
        issues.append(f"tier {sorted(by_tier)} != {sorted(expected['tiers'])}")
    if expected.get("noCards") and cards:
        issues.append("câu hỏi này không được dựng lại cấu hình")
    verified_cheap = "cheap" in by_tier and all(line.get("priceChecked") is True and line.get("inStock") is True for line in by_tier["cheap"].get("items", []))
    if expected.get("preferCheapest") and verified_cheap and (result.get("build") or {}).get("tier") != "cheap":
        issues.append("đã có bản Rẻ đủ yêu cầu nhưng không ưu tiên khi người dùng muốn rẻ nhất")
    if not cards and "tiers" not in expected and not expected.get("noCards"):
        explanation = result.get("reply", "")
        specific = re.search(r"Phương án .*(?:thiếu|vượt|chưa tìm|không tìm|chưa đủ)|chưa kiểm chứng|chưa xác minh|(?:RAM|VRAM|SSD|CPU|card).*(?:thiếu|chưa đủ)", explanation, re.I)
        if not expected.get("allowUnavailable") or not specific:
            issues.append("chưa có phương án và chưa giải thích được lý do")
    if expected.get("mustExplainMoney") and not re.search(r"\d+(?:[.,]\d+)?\s*(?:triệu|tr\b)", result.get("reply", ""), re.I):
        issues.append("thiếu giải thích bằng số tiền")
    if expected.get("replyPattern") and not re.search(expected["replyPattern"], result.get("reply", ""), re.I):
        issues.append("chưa hỏi lại hoặc giải thích đúng điểm cần xác nhận")
    for tier, card in by_tier.items():
        lines = card.get("items") or []
        missing = {"case", "motherboard", "cpu", "ram", "ssd", "psu"} - {line.get("category") for line in lines}
        if missing:
            issues.append(f"{tier}: thiếu loại linh kiện {sorted(missing)}")
        if card.get("compatible") is not True or card.get("errors"):
            issues.append(f"{tier}: lỗi lắp")
        if (card.get("verdict") or {}).get("status") in (None, "HOLD", "BLOCKED"):
            issues.append(f"{tier}: trạng thái chưa thể tư vấn mua")
        if not lines:
            issues.append(f"{tier}: thiếu linh kiện")
        total = sum(line.get("priceMaxVnd") or line.get("priceMinVnd") or 0 for line in lines)
        budget = expected.get("budgetVnd")
        if budget is not None and total > tier_cap(budget, tier):
            issues.append(f"{tier}: vượt ngân sách")
        if budget is not None and total > budget:
            name = {"cheap": "Rẻ", "balanced": "Phù hợp", "quality": "Chất lượng"}.get(tier, tier)
            row = next((row for row in result.get("reply", "").splitlines() if f"**{name}:" in row), "")
            if "vượt ngân sách" not in row or "gợi ý chính" in row:
                issues.append(f"{tier}: lời tư vấn chưa ghi rõ vượt ngân sách gốc")
        for line in lines:
            if not line.get("sourceUrl") or line.get("priceMaxVnd") is None:
                issues.append(f"{tier}: {line.get('code')} thiếu link hoặc giá")
            claimed_warranty=re.search(r'bảo hành\s*(\d+)\s*tháng',line.get('spec') or '',re.I)
            if claimed_warranty and (line.get('warrantyMonths') is None or int(claimed_warranty.group(1)) != line['warrantyMonths']):
                issues.append(f"{tier}: thông số bảo hành tự ghi không khớp trang bán")
            if line.get("inStock") is False:
                issues.append(f"{tier}: {line.get('code')} hết hàng")
            if line.get("category") == "fans" and re.search(r'cầm tay|du lịch|tích điện|quạt để bàn|jisulife', line.get('productName') or '', re.I):
                issues.append(f"{tier}: quạt gia dụng không phải quạt case PC")
            if line.get("category") in ("cooler", "fans") and re.search(r'đế\s+tản|tản\s+nhiệt\s+(?:laptop|notebook)|notepal|cooling\s*pad|notebook\s+cooler', line.get('productName') or '', re.I):
                issues.append(f"{tier}: đế tản laptop không phải tản CPU máy bàn")
        for line in lines:
            if (line.get('code') == 'gpu_240' or any(x.get('category') == 'case' and re.search(r'Silverstone.*SG13',x.get('productName') or '',re.I) for x in lines)) and re.search(r'\bSPARKLE\b.*\bB580\b.*\bTITAN\b|\bSB580T-12GOC\b',line.get('productName') or '',re.I):
                issues.append(f"{tier}: Sparkle B580 TITAN ba quạt dài315mm không khớp mẫu GPU240/vỏ nhỏ")
        gpu = next((line for line in lines if line.get("category") == "gpu"), None)
        if expected.get("noDiscreteGpu") and gpu is not None:
            issues.append(f"{tier}: thêm card rời trái nhu cầu")
        if expected.get("miniItx") and not any(line.get("code") == "case_mini_sff" for line in lines):
            issues.append(f"{tier}: đổi sai kiểu vỏ mini ITX")
        if expected.get("notMiniItx") and any(line.get("code") == "case_mini_sff" for line in lines):
            issues.append(f"{tier}: vẫn chọn mini ITX dù người dùng đã bỏ yêu cầu")
        if expected.get("noEntryCpu") and any(line.get("category") == "cpu" and re.search(r"\b(?:athlon|celeron|pentium)\b", line.get("productName") or "", re.I) for line in lines):
            issues.append(f"{tier}: CPU quá thấp cho nhu cầu game / GPU")
        if expected.get("minRamGb"):
            ram = next((line for line in lines if line.get("category") == "ram"), {})
            title = ram.get("productName") or ""
            modules,per_stick = ram_shape(title, ram.get("sourceUrl"))
            if ram.get('quantity',0) % modules:issues.append(f"{tier}: RAM ghi thiếu thanh trong bộ kit")
            if per_stick * ram.get("quantity", 0) < expected["minRamGb"]:
                issues.append(f"{tier}: thiếu RAM")
        if expected.get("minSsdGb"):
            ssd = next((line for line in lines if line.get("category") == "ssd"), {})
            size = re.search(r"\b(\d+(?:[.,]\d+)?)\s*(TB|GB)\b", ssd.get("productName") or "", re.I)
            capacity = float(size.group(1).replace(',', '.')) * (1000 if size.group(2).upper() == "TB" else 1) * ssd.get("quantity", 0) if size else 0
            if capacity < expected["minSsdGb"]:
                issues.append(f"{tier}: thiếu SSD")
        titles = {line.get("category"): line.get("productName") or "" for line in lines}
        cpu_title, board_title, ram_title = titles.get("cpu", ""), titles.get("motherboard", ""), titles.get("ram", "")
        two_slots = re.search(r'\bMSI\s+PRO\s+(?:B760M|H610M)[- ]E\b|\bGIGABYTE\s+H610M[- ]H\s+V3\b', board_title, re.I)
        if two_slots and re.search(r'DDR4', board_title, re.I) and sum(line.get("quantity", 0) for line in lines if line.get("category") == "ram") > 2:
            issues.append(f"{tier}: quá 2 khe RAM của đúng mã bo (thông số hãng)")
        cpu_intel = re.search(r"\bi[3579][ -]?(1[0-4])\d{3}([a-z]*)\b", cpu_title, re.I)
        cpu_ryzen = re.search(r"\b(?:Ryzen|R)\s*[3579][ -]?(\d)\d{3}([a-z0-9]*)\b", cpu_title, re.I)
        board_chip = re.search(r"\b([ABHXZ])([45678])\d{2}A?[A-Z]?\b", board_title, re.I)
        if board_chip:
            chip_family, series = board_chip.group(1).upper(), int(board_chip.group(2))
            intel_board = chip_family in ('H', 'Z') or bool(re.search(r'\bB[45678]60', board_title, re.I))
            amd_board = chip_family in ('A', 'X') or bool(re.search(r'\bB[34568]50', board_title, re.I))
            if cpu_intel and amd_board or cpu_ryzen and intel_board:
                issues.append(f"{tier}: CPU/bo khác nền tảng theo tên hàng")
            elif cpu_intel and intel_board and (int(cpu_intel.group(1)) >= 12) != (series >= 6):
                issues.append(f"{tier}: đời CPU Intel không cùng socket với bo")
            elif cpu_ryzen and amd_board and (int(cpu_ryzen.group(1)) >= 7) != (series >= 6):
                issues.append(f"{tier}: CPU và bo khác AM4/AM5")
        board_ddr = re.search(r"\bDDR\s*([45])\b|\bD([45])\b", board_title, re.I)
        ram_ddr = re.search(r"\bDDR\s*([45])\b", ram_title, re.I)
        if board_ddr and ram_ddr and (board_ddr.group(1) or board_ddr.group(2)) != ram_ddr.group(1):
            issues.append(f"{tier}: RAM khác loại DDR với bo")
        if gpu is None and cpu_intel and 'f' in cpu_intel.group(2).lower():
            issues.append(f"{tier}: CPU đuôi F không có card xuất hình")
        if gpu is None and cpu_ryzen and (int(cpu_ryzen.group(1)) < 7 and 'g' not in cpu_ryzen.group(2).lower() or 'f' in cpu_ryzen.group(2).lower()):
            issues.append(f"{tier}: Ryzen không có đồ họa tích hợp và thiếu card")
        if expected.get("gpuVendor") or expected.get("gpuModel") or expected.get("minGpuVramGb"):
            if gpu is None:
                issues.append(f"{tier}: thiếu card")
            else:
                title = gpu.get("productName") or ""
                if expected.get("gpuVendor") and gpu_vendor(title) != expected["gpuVendor"]:
                    issues.append(f"{tier}: sai hãng card")
                if expected.get("gpuModel") and gpu_model(title) != expected["gpuModel"]:
                    issues.append(f"{tier}: sai model card")
                if expected.get("minGpuVramGb") and (vram_gb(title) or 0) < expected["minGpuVramGb"]:
                    issues.append(f"{tier}: thiếu VRAM")
    return cards, issues


def evaluate_prices(gold, cards, seen=None):
    labels = {item["url"]: item for item in gold.get("prices", [])}
    if seen is None:
        seen = {}
    passed = total = 0
    issues = []
    for card in cards:
        for line in card.get("items") or []:
            url = line.get("sourceUrl")
            label = labels.get(url)
            key = (url, label.get("capturedAt") if label else None) if url else (gold["id"], card.get("tier"), line.get("code"))
            # Chỉ đếm một lần chụp, nhưng kiểm MỌI dòng dùng lần chụp đó.
            # Một dòng đúng trước không được che dòng sai ở tier/câu trả lời sau.
            if key not in seen:
                seen[key] = True
                total += 1
                passed += 1
            def fail(message):
                nonlocal passed
                if seen[key]:
                    passed -= 1
                    seen[key] = False
                issues.append(message)
            if label is None:
                fail(f"{line.get('code')}: thiếu nhãn giá {url}")
                continue
            try:
                checked, captured = timestamp(line.get("checkedAt")), timestamp(label.get("capturedAt"))
                fresh = checked is not None and captured is not None and abs((checked - captured).total_seconds()) <= MAX_CAPTURE_GAP_SECONDS
            except (ValueError, TypeError):
                fresh = False
            modules = ram_shape(line.get('productName') or '', line.get('sourceUrl'))[0] if line.get('category') == 'ram' else 1
            buy_qty = line.get('quantity',1) // modules
            if line.get('category') == 'fans':
                per_pack=fan_pack_size(line.get('productName') or '')
                buy_qty=(line.get('quantity',1)+per_pack-1)//per_pack
            if (label.get("reviewed") is True and label.get("inStock") is True
                    and isinstance(label.get("unitPriceVnd"), int) and label["unitPriceVnd"] > 0
                    and line.get("priceChecked") is True and fresh and line.get("inStock") is True
                    and line.get("productName") and label.get("title")
                    and line.get("priceMaxVnd") == label["unitPriceVnd"] * label.get("buyQty", buy_qty)):
                pass
            else:
                fail(f"{line.get('code')}: giá hoặc chứng cứ trang không khớp {url}")
    return passed, total, issues


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--gold", required=True, type=Path)
    parser.add_argument("--runs", required=True, type=Path)
    parser.add_argument("--zero-issues", action="store_true", help="Yêu cầu cả ba chỉ tiêu đạt 100%%, đủ mẫu như cũ")
    args = parser.parse_args()
    cases = json.loads(args.gold.read_text(encoding="utf-8"))
    if len({case["id"] for case in cases}) != len(cases) or len({case["run"] for case in cases}) != len(cases):
        raise ValueError("Mỗi ca phải có id và file phản hồi riêng; không nhân bản kết quả để đủ mẫu.")
    decision_ok = advice_ok = price_ok = price_total = 0
    seen_prices = {}
    for case in cases:
        run = json.loads((args.runs / case["run"]).read_text(encoding="utf-8"))
        cards, decision_issues = evaluate_case(case, run.get("result", run))
        advice = case.get("adviceQuality") or {}
        advice_issues = [] if advice.get("pass") is True else ["tư vấn chưa đạt hoặc chưa được chấm độc lập"]
        good, count, price_issues = evaluate_prices(case, cards, seen_prices)
        decision_ok += not decision_issues
        advice_ok += not advice_issues
        price_ok += good
        price_total += count
        if decision_issues or advice_issues or price_issues:
            print(f"{case['id']}: {'; '.join(decision_issues + advice_issues + price_issues)}")
    cases_total = len(cases)
    decision_rate = decision_ok / cases_total if cases_total else 0
    advice_rate = advice_ok / cases_total if cases_total else 0
    price_rate = price_ok / price_total if price_total else 0
    print(f"Quyết định: {decision_ok}/{cases_total} ({decision_rate:.1%}); "
          f"chất lượng tư vấn: {advice_ok}/{cases_total} ({advice_rate:.1%}); "
          f"giá đúng: {price_ok}/{price_total} ({price_rate:.1%})")
    threshold = 1.0 if args.zero_issues else THRESHOLD
    passed = acceptance_passed(cases_total, price_total, decision_rate, advice_rate, price_rate, threshold)
    print("ĐẠT" if passed else f"CHƯA ĐẠT: cần đủ nhãn độc lập và đạt cả ba ngưỡng {threshold:.0%}")
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
