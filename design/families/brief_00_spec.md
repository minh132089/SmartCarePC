# Brief (Codex, design engineer): 6 case families, master spec

PC SmartCare is a 3D PC-build simulator for complete beginners (Vietnamese UI, brand-free parts, realistic).
The user made 6 concept images + a grouping doc in `design/case-families/` (read `PHAN_CHIA_MODEL.md`, look at
the 6 attached PNGs in order 01..06). They are AI concepts only, no dimensions.
Claude (the worker) will build every family as a full 3D model in Blender from YOUR drawings and specs, exactly like
the existing Gaming build (keep it; it stays a separate build). Study how that was done so you follow the same
conventions and level of detail: `design/gaming/00_blueprint.md`, `design/gaming/02_motherboard.md`,
`design/gaming/12_case_front.md`, `design/gaming/13_case_top.md`, `src/data/gaming.ts`, `tools/build_gaming.py`.

Families (all NEW models, including the standard tower):
01 standard_tower, 02 compact_tower, 03 showcase_dual_chamber, 04 mini_sff, 05 slim_console, 06 full_tower.

Reusable existing parts (do not redesign): CPU (LGA 37.5 x 37.5), DDR4 RAM stick 133.35 x 42, M.2 2280 SSD,
ATX motherboard 244 x 305 (only for ATX families), ATX PSU 150 x 86 x 140, GPU 280 x 125 x 55 3-fan,
AIO 240 (pump + radiator 277 x 120 x 27 + 2 fans), case fans 120/140 (plain or cyan ARGB ring).
You may (and should) define NEW shared parts where a family needs them, e.g. Micro-ATX board 244 x 244,
Mini-ITX board 170 x 170, SFX PSU 125 x 63.5 x 100, 2-fan GPU ~240 long, tower air cooler, low-profile cooler,
AIO 360, PCIe riser cable, vertical stand. Keep the number of new parts small and shared between families.

Write `design/families/00_families_spec.md` (Vietnamese, precise, in mm, same coordinate convention as gaming:
X back(-)->front(+), Y floor->roof, Z closed side(-)->glass/open side(+), origin at the case body centre) with:
1. For EACH family: outer dimensions (W x H x D, feet/stand), panels and materials/colours (hex), front/top/side/bottom
   style matching its concept image, board form factor + board centre and orientation, PSU type + position, GPU
   type + mounting (horizontal slot, or riser/sandwich), CPU cooler type, every fan (size, centre, intake/exhaust),
   radiator if any, PCIe slot count at the rear, front/top I/O (power button centre, reset, USB-A/C, headphone jack),
   cable management concept (tray, grommets with codes and centres, shrouds/chambers), screws per panel,
   clearances (max GPU length, max cooler height, radiator support) and what a beginner should notice.
2. A table of NEW shared parts with exact overall dimensions, key features and which families use them.
3. A drawing plan: the exact list of image sheets to produce next, one image per sheet:
   - per family: `NN_<family>/00_overview.png` (exploded assembly + all components) and
     `NN_<family>/01_case.png` (the empty case in 6 orthographic views: front, back, left, right, top, bottom, plus one 3/4 view)
   - per new shared part: `parts/<part>.png` (6 orthographic views + one 3/4 view)
   Use folder names `design/families/01_standard_tower/` ... `06_full_tower/` and `design/families/parts/`.
Do NOT generate images in this run, do not edit any other file. Print a short summary at the end.
