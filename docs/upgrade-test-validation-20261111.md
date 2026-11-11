# Upgrade adviser validation — 11 November 2026

Base: `feature/upgrade`, commit `b7963d4`. The final source changes and runtime JAR are recorded by SHA-256 in the evidence directory. The current machine is tested; this report does not certify other hardware or future marketplace prices.

## Fixed regressions

- Prevent DDR4 sticks being retained when adding DDR5 during a platform change.
- Enforce GPU vendor preferences from follow-ups, including releasing a preference. The initial existing-GPU description does not lock the shopping brand. The better-value check uses the same preference.
- Reject fractional, zero, negative, wrapped or oversized quantities; allow only one CPU/GPU/board/PSU/case/cooler per desktop plan.
- On the last failed over-budget proposal, consider only subsets of already verified items. Each subset must pass the same platform, RAM, PSU and budget checks. For an office PC still using an HDD, an affordable SSD can be kept while optional RAM is dropped.
- Recognize the B650E-I Mini-ITX suffix, so two occupied slots cannot accept a third module. Also reject replacing RAM with the same or less capacity on the same DDR platform.
- Show physical module counts separately from the number of kits purchased; charge each kit once.
- Build RAM explanations from the accepted product, quantity and total capacity. Do not reinterpret “16GB, 2 sticks” as two 16GB modules.
- When no purchase is accepted, report the application's verified rejection reasons or a neutral no-purchase answer rather than invented stock/market claims.

The B650E-I has two DIMM slots according to [ASUS specifications](https://rog.asus.com/us/motherboards/rog-strix/rog-strix-b650e-i-gaming-wifi-model/spec/).

## Declared live cases

| Request | Expected result |
|---|---|
| Laptop | Explain desktop-only scope; no purchase card |
| GTX 1650 to RTX for gaming | Verified RTX GPU within budget; keep usable CPU, motherboard and RAM |
| Cheaper RTX follow-up | A strictly lower total while preserving NVIDIA |
| Office PC on HDD | An affordable SSD, without unnecessary CPU/GPU/platform replacement |
| B650E-I, 16GB DDR5 in two occupied slots, 32GB target | Replacement within two slots; no purchase is allowed if no verified 32GB option fits the budget |
| Same PC with a larger budget | A purchasable DDR5 replacement reaching 32GB within two slots |
| Unknown RAM generation and motherboard | No guessed memory purchase; explain what to check |
| CUDA/PyTorch GPU upgrade | NVIDIA GPU with at least 12GB VRAM; retain sufficient PSU |
| GPU budget VND 300,000 | No unaffordable purchase card |

The low-budget DDR5 case permits an honest refusal; it does not require the app to invent a cheaper available product. The positive DDR5 case requires an actual purchasable plan. Automated checks verify totals, stock and price flags, source URLs, required and forbidden parts, vendor, RAM generation, capacity and physical slot count. Human review checks the saved replies and independently captured store pages.

## Rejected earlier rounds

Earlier raw runs remain in `PCsmartcare-testloop`. The first oracle missed a physical-slot error; that output was rejected after review. Subsequent runs exposed incorrect free-text memory counts despite correct cards. Those observations led to the source fixes above. Earlier passes are not substituted for failures in the final round.

## Evidence and limits

See the companion JSON for final counts and the accepted evidence directory. Backend tests use mocked AI/offers to exercise rejection logic; the nine final API steps use the configured real AI provider and store search. Store snapshots are captured independently of the Java price parser and manually reviewed for primary product identity, unit price, availability and quantity.

No physical assembly, benchmark, PSU electrical test or exhaustive catalogue search was performed. Missing fit details remain conditions to check before buying. A passing declared suite is not a guarantee of universal zero issues or future price stability. Source changes are local and are not pushed or deployed by this validation task.

## Accepted result

360 backend tests passed, including all 45 upgrade tests, with no failures/errors/skips. All 30 evaluator tests passed. All 9 real API steps in this single final run passed their declared gates. Independent review of 4 product pages matched all 5 line-price observations exactly (0 VND difference); maximum snapshot-to-verification gap was 46.763 seconds. The final runtime JAR and source hashes match. Frontend production build passed.
