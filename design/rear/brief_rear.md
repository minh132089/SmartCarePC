# Brief (Codex, design engineer): redesign the REAR PANEL of one PC case

The user (PC SmartCare, a friendly 3D "look inside a PC" web app) says the back of our cases looks like a flat sheet
with holes and must be redesigned so that it is realistic and **logically consistent with the parts inside**.
Reference photo from the user (layout to emulate, brand-free): `design/rear/ref_rear.png`.
Current renders of our rear: `design/rear/current_standard_rear.png`, `design/rear/current_full_rear.png`.

Inputs (numbers are the source of truth, mm, app axes: X back(−)→front(+), Y floor→roof, Z closed side(−)→open/glass side(+),
origin at the body centre; the rear face is at X = −D/2):
- Families: `src/data/families.json` (body, board origin/io_y, rear_fan, pcie, psu center/rot/kind, ac_inlet, feet) and
  `src/data/boards.json` (I/O shield x/y/h, usb/rj45/jacks), plus `design/families/00_families_spec.md` and the
  `design/families/0N_<family>/01_case.md` sheet of the family.
- Gaming: `design/gaming/00_blueprint.md`, `design/gaming/11_rear_io.md`, `src/data/gaming.ts` (CASE_PARTS) and `tools/build_gaming.py`.
- PSU models: ATX `g_psu` (C14 + I/O switch + honeycomb on its −X face), SFX `f_psu_sfx_100` (see `design/families/parts/psu_sfx_100.md`).
- GPU brackets: 280 = 3-slot bracket (3 DP + 1 HDMI), 240 = 2-slot (`design/families/parts/gpu_240_dual.md`).

Everything visible from behind must line up with what is inside: I/O shield exactly over the board's rear I/O, rear fan
mount where the rear fan is, the PCIe slot covers in the right number and position with the GPU bracket filling the slots
the card uses (ports visible), the PSU opening/fan-grille matching the PSU size, orientation and C14/switch position
(or the case C14 inlet + cable for SFF), vents where hot air leaves, side-panel thumbscrews where the panels attach,
feet. Add the realistic details from the reference: folded steel frame border, recessed I/O + fan field, slot-cover
retention bar and screws, vent slots/hex mesh beside the slots, PSU bracket with 4 screws, rounded corners, screw heads.
No logos, no text on the panel.

Produce exactly ONE image and ONE spec for the sheet named in the task. Do not edit other files.

Image (built-in image generation tool): white background reference sheet, brand-free. Large orthographic REAR view (X−)
with dimension lines to every opening, plus a 3/4 rear view, plus a section/side view showing how the I/O shield, GPU bracket
and PSU sit against the inside of the rear wall. Short English labels, numbered callouts.

Spec `.md` next to it (Vietnamese, simple): every opening / part on the rear wall with centre (Y; Z) and size in mm in the
app axes above, depth (X) of recesses, which inside part each opening belongs to, screw positions and types, material/colour
hex, and a short "đã sửa so với bản hiện tại" list. Numbers must agree with families.json / gaming.ts; if you think a
number inside must change for the rear to make sense, say so explicitly instead of silently changing it.
Finish by printing a 3-line summary.
