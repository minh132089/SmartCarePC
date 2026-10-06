# Brief (Codex, design engineer): ONE drawing sheet for the PC assembly tool kit

Context: PC SmartCare is a friendly 3D "look inside a PC / build it yourself" web app for non-technical Vietnamese
users. Read `design/tools/00_assembly_needs.md` first: it lists the tools and accessory parts a person must hold
while assembling the machines (the PC parts themselves are already modelled). Hardware sizes must match the
machines: `design/families/00_families_spec.md` and `design/gaming/00_blueprint.md` (screw types, standoff height
6.5, M.2 screw, PSU #6-32, fan screws, C14 inlet).
You produce exactly ONE image and ONE short spec for the sheet named in the task. Do not edit other files.

Image (built-in image generation tool):
- Clean product reference sheet for a 3D modeller, white / very light grey background, realistic materials,
  thin blue dimension lines, numbered callouts with SHORT English labels, brand-free (no logos, no fake text).
- MUST show the object from ALL sides as clearly separated, labelled views: FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM
  (orthographic, same scale, aligned like a technical drawing) plus ONE 3/4 perspective view. Add an "in use"
  inset showing a hand using it on the PC part it is meant for.
- For `00_overview`: a top-down view of every item laid out on an assembly mat (mat size in mm), each numbered, plus
  a small 3/4 view of the mat; no PC in the picture.
- Show overall dimensions in mm and every functional detail (tip shape, thread, plug pins, clip, compartments).
- If the first image has mistakes (missing view, wrong counts, logos, unreadable labels), regenerate once.

Spec file next to the image, same base name `.md` (Vietnamese, simple): dimensions, sub-parts with positions in
mm (state the origin and axes; long axis of the object along +X), materials + colour hex, counts, and
3-5 "người mới cần biết" points (what it is for, at which assembly step, safety). For `00_overview` give the mat
size and the X/Z position + rotation of every item on the mat. Numbers in the spec are the source of truth; say
where the image deviates. Finish by printing a 3-line summary.
