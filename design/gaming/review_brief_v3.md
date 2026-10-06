# Review brief (Codex): rear I/O (sheet 11) + glass front / ARGB (sheet 12) as built

Claude built your sheets 11_rear_io and 12_case_front. Review strictly against those specs.
Code: `tools/build_gaming.py` (motherboard: block "I/O sau" and JRAINBOW1; case: block "Mặt trước kính";
_case_fan with argb), `src/scene/GamingRig.tsx` (ARGB routes at the end of gamingWiring), `src/data/gaming.ts`.
Attached in order: v3_front_iso (app, glass side removed), v3_glass_front (front glass), v3_rear_io (app, rear),
v3_cables (cables highlighted, closed side panel hidden), v3_rear_io_closeup (Blender close-up of the I/O shield).
A collision tool `tools/check_collisions.py` reports no unintended intersections.

Write `design/gaming/review/v3_review.md` (Vietnamese, max 35 lines): verdict, numbered concrete problems with
exact fixes (mm/counts/colours), max 3 optional polish ideas. Do not edit other files.
