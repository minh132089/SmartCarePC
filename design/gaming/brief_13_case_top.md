# Brief (Codex, design engineer): 13 / case TOP panel redesign

The user wants the roof (top panel) of our gaming case redesigned, using `design/gaming/ref/case_top_style.png`
as a style reference (a top panel with a large perforated vent area held by a clean structural rib pattern,
a perimeter frame, and a removable magnetic filter look). Brand-free, no logos. The user's requests, verbatim intent:
1. Make the vent opening on the top WIDER and add more detail (rib structure, fine perforation, frame, filter).
2. Make the top look beautiful and BALANCED; put the POWER BUTTON in the CENTRE of the top I/O.
3. Add the missing small RESET button and a ROUND HEADPHONE jack (3.5 mm). Decide whether a separate MIC jack is
   also needed or a TRRS combo; keep the existing 2 x USB-A 3.0; you may add a USB-C if it fits the clean look,
   and a small power/HDD LED.

Current state (read `tools/build_gaming.py`, function `case`, blocks "Đáy, nóc" and the top I/O at io_x = 190;
`design/gaming/12_case_front.md`; `src/scene/GamingRig.tsx` for cable routes):
- Case 220 W x 470 H x 450 D mm, case coordinates X back(-225)..front(+225), Y floor..roof (roof plate Y 232..235,
  sheet 3 mm), Z closed side(-110)..glass side(+110). Glass front added in sheet 12 (front frame X 222..225).
- Roof now: one opening X -150..150, Z -28..100 filled with 38 plain slats; 8 radiator mount tabs at
  X -114/-9/6/111, Z -16.5/88.5 (S3 screws of the 240 radiator go through them, radiator top at Y 231); the 240 AIO
  radiator occupies X -140..137, Z -24..96 directly under the roof; rear exhaust fan top edge near Y 205.
- Top I/O now: a row at X = 190 along Z: power button Z -60 (Ø14 with cyan ring), USB-A Z -30 and -12, combo jack Z 6.
- Internal cables from top I/O run to the motherboard: USB 3.0 19-pin, HD_AUDIO, F_PANEL (PWR SW, RESET SW,
  PLED, HDD LED) via grommets G2 then G5/G6. If you add USB-C, say which internal header (Type-E key A) and where
  on the board it would go without colliding (02_motherboard.md headers), or decide against it.

Output (ONE image + ONE spec):
1. `design/gaming/13_case_top.png`: clean product reference sheet: top view (plan) of the whole roof, a 3/4 view of
   the case top, and a close-up of the top I/O strip, numbered callouts, short English labels, brand-free.
2. `design/gaming/13_case_top.md` (Vietnamese, simple): exact dimensions in case coordinates (mm): vent opening,
   rib pattern geometry (so it can be modelled procedurally: rib widths, node positions or a formula), perforation
   (hole diameter, pitch, pattern), filter (frame, magnets), how the vent clears the 8 radiator tabs/screws,
   top I/O layout with every control/port centre, size, colour hex, and the internal connector/cable for each,
   what a beginner should notice. Keep the case size and everything inside unchanged.
Do not edit any other file. Print a 3-line summary at the end.
