# Brief (Codex, design engineer): ONE drawing sheet for the case families

Source of truth: `design/families/00_families_spec.md` (read it fully first) plus the concept image of the family
in `design/case-families/` if the sheet belongs to a family. Gaming conventions: `design/gaming/00_blueprint.md`.
You produce exactly ONE image and ONE short spec for the sheet named in the task. Do not edit other files.

Image (built-in image generation tool):
- Clean product reference sheet for a 3D modeller, white / very light grey background, realistic materials,
  thin blue dimension lines, numbered callouts with SHORT English labels, brand-free (no logos, no fake text).
- MUST show the object from ALL sides as clearly separated, labelled views:
  FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM (orthographic, same scale, aligned like a technical drawing)
  plus ONE 3/4 perspective view. For an `00_overview` sheet: one large exploded 3/4 assembly showing every
  component in its install direction, plus small orthographic FRONT / LEFT(glass or open side) / TOP of the
  assembled machine.
- Show overall dimensions in mm, every opening, port, button, fan mount, screw hole/screw, connector (pin count
  and key), and cable grommet relevant to the object.
- If the first image has mistakes (missing view, wrong counts, logos, unreadable labels), regenerate once.

Spec file next to the image, same base name `.md` (Vietnamese, simple): dimensions, sub-parts with positions in
mm (state the coordinate system), materials + colour hex, connectors, screws, what each view shows, and 3-5
"beginner must notice" points. Numbers in the spec are the source of truth; say where the image deviates.
Finish by printing a 3-line summary.
