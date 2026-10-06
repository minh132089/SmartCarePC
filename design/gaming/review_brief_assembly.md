# Review brief: assembled gaming PC in the app (Codex)

Claude (the worker) has placed every part into the web app (React Three Fiber). Review the WHOLE BUILD.
Inputs:
- Attached app screenshots: assembly_front (glass removed, default camera), assembly_exploded ("Tách rời linh kiện"),
  assembly_back (camera behind the closed side panel), assembly_cables (cables part highlighted in blue).
- Placement code: `src/scene/GamingRig.tsx`, numbers in `src/data/gaming.ts` (mm, case coordinates),
  models from `tools/build_gaming.py` (functions case, case_fan and the others).
- Your drawings/specs: design/gaming/00_blueprint.* and 01..10.

Write `design/gaming/review/assembly_review.md` (Vietnamese, short, max 45 lines):
1. Verdict: OK / cần sửa.
2. Numbered problems, most important first, each with the exact fix (mm / coordinates / counts).
   Check especially: every part sits where the blueprint says; no part intersects another; all cables
   hidden behind the tray except the last 20-40 mm at each connector; cable routes use the right grommet
   (G0-G6); connectors plug into the right header; the AIO hoses, radiator, fans, PSU, GPU power via G4;
   the case itself (01_case spec) and the case fan (09_fans spec); what a beginner sees first.
3. Max 3 optional polish ideas.
Do NOT edit any other file.
