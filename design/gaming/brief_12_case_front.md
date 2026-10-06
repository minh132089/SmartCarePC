# Brief (Codex, design engineer): 12 / case front redesign (clean glass style)

The user wants the gaming case front to look clean like the reference photo
`design/gaming/ref/case_front_style.png` (brand-free mid-tower: tempered-glass FRONT panel and glass side,
three 120 mm ARGB fans behind the front glass with glowing rings, slim vertical intake vents along the front
edges, flat black frame, small feet, perforated PSU shroud area). Do NOT copy the brand logo on the fans.

Current case: `design/gaming/01_case.md` + `tools/build_gaming.py` (function case): 220 W x 470 H x 450 D mm,
currently a flat perforated mesh front with 3 front fans at Y = -85, 40, 165 (Z = 0), fan bracket at X 221..222,
case sheet 3 mm, top I/O cluster near the front edge of the roof, cyan accent strip. Keep the overall
dimensions, the three front fan positions and everything inside the case unchanged.

Design a new FRONT for our case:
- Tempered glass front panel (thickness, inset, edge chamfer), how it is held (clips / pins / thumb screws),
  side intake vents (two vertical perforated strips on the front edges, width, hole pattern, dust filter),
  the frame around it, where air enters.
- Front fans become ARGB: glowing diffuser ring on each fan. If ARGB is added, specify the extra cables and
  connectors (3-pin 5 V ARGB, daisy chain or hub, which motherboard header: add a generic `JRAINBOW`-style
  ARGB header position on our board that does not collide with existing headers in 02_motherboard.md).
  Choose a calm colour scheme suitable for a clean look (you may suggest cyan to match the accent, or a soft
  rainbow like the reference) and say which.
- Keep top I/O (power button, 2 USB-A, audio) but say if it should move.

Output (ONE image + ONE spec):
1. `design/gaming/12_case_front.png`: clean product reference sheet: 3/4 front view of the whole case in the
   new style (fans glowing), a small exploded view of the front panel layers (glass, frame, vent strips, dust
   filter, fan bracket), and numbered callouts with short English labels. Brand-free.
2. `design/gaming/12_case_front.md` (Vietnamese): all dimensions in mm in case coordinates (X back->front,
   Y floor->roof, Z closed side->glass), materials/colours hex, fixings (count/type/position), ARGB parts and
   cable routing (which grommet G0..G6), what a beginner should notice.
Do not edit any other file. Print a 3-line summary at the end.
