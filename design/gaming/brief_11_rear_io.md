# Brief (Codex, design engineer): 11 / rear I/O redesign

The user compared our gaming motherboard with a real modern ATX board (reference screenshot
`design/gaming/ref/buildcores_rear_io.png`, a BuildCores render of a B860-class ATX board inside a case).
Our current rear I/O (spec 02_motherboard.md) is far too sparse: only 2 USB-A, 1 RJ45, 3 audio jacks.
Redesign the rear I/O of OUR generic, brand-free ATX board so a beginner sees a realistic, complete panel.

Constraints:
- Standard ATX I/O shield aperture 158.75 x 44.45 mm. Our board: 244 x 305, I/O on the board's -X edge.
  In our model the shield now spans board-local Y = -18 ... 140 (it was moved up 20 mm so it ends above the
  first PCIe slot at Y = -30; the GPU bracket top is at board Y = -24). Keep the panel inside Y -18 ... 140,
  height above PCB up to 44.45. Look at `tools/build_gaming.py` (function motherboard, block "I/O sau", IO_DY)
  and `src/data/gaming.ts` for the real numbers.
- Brand-free: no logos, no product names. Short port labels only where a real board prints them.
- Typical modern mid-range set (decide the final list): BIOS flashback + clear CMOS buttons, 2 Wi-Fi antenna
  SMA jacks, USB-C, several USB-A in colours by speed (black 2.0, blue 5 Gbps, red 10 Gbps), 2.5G RJ45 with
  LEDs, HDMI + DisplayPort for integrated graphics, audio (line-out, mic, optical S/PDIF or a 5-jack stack).

Output (ONE image + ONE spec):
1. `design/gaming/11_rear_io.png`: clean product reference sheet (same style as 02_motherboard.png):
   straight-on rear elevation of the I/O shield as seen from behind the PC, plus a small 3/4 view, every
   port numbered with a short English label. Correct port orientation for a TOWER (board vertical): USB-A
   long side runs along the board's Y axis (vertical in the rear view).
2. `design/gaming/11_rear_io.md` (Vietnamese, simple): exact table of every port: type, count, centre
   (board-local Y, height above PCB in mm), opening size, colour hex, contact count, what a beginner plugs in.
Do not edit any other file. Print a 3-line summary at the end.
