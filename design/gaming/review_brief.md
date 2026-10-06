# Review brief for the design engineer (Codex)

Claude (the worker) built a 3D model from your drawing and spec. Review it like a strict design engineer.
Inputs you get: your drawing `design/gaming/<NN>_<id>.png`, your spec `design/gaming/<NN>_<id>.md`,
the worker's Blender script `tools/build_gaming.py` (function for this part), and attached renders of the
model (front = looking from the glass side of the PC, iso/iso2 = 3/4 views, back = underside/back).
Axes in the script: millimetres, x = length, y = up, z = toward the glass/viewer.

Write `design/gaming/review/<id>_review.md` (Vietnamese, short) with:
1. Verdict: OK / cần sửa.
2. Numbered list of concrete problems, most important first. For each: what is wrong, where, and the exact
   fix with numbers (mm, counts, colours, positions) so the worker can apply it without guessing.
   Check: dimensions, counts (pins, screws, fans, slots), orientation marks, connector shapes/keys,
   screw holes, materials/colours, anything a beginner needs to recognise the part, brand-free.
3. Optional polish ideas (max 3), clearly marked as optional.
Do NOT edit any other file. Keep it under 40 lines.
