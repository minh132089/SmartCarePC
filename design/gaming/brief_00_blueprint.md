# Brief for the design engineer (Codex)

You are the DESIGN ENGINEER for "PC SmartCare", a friendly PC-build simulator for complete beginners
(non-technical people, Vietnamese UI). Another agent (Claude) is the WORKER who will build the 3D models
in Blender from your drawings and specs. Your drawings are the source of truth, so be precise and clean.

Project context (read-only, do NOT edit anything outside design/gaming/):
- src/data/builds.ts  -> the "gaming" build (1 scene unit = 100 mm)
- src/data/layout.ts  -> motherboard slot positions, standoff height, power connector positions
- src/data/parts.ts   -> part ids and Vietnamese descriptions

Gaming build facts:
- Mid-tower case with tempered-glass side panel: 220 W x 470 H x 450 D mm, dark body (#16181d), cyan accent (#22d3ee)
- ATX motherboard 305 x 244 mm, generic, NO brand logos anywhere
- CPU (LGA-style, generic) + 240 mm AIO liquid cooler: pump block on the CPU, radiator with 2 x 120 mm fans mounted UNDER the top panel
- 2 x DDR RAM sticks with heatspreaders
- Graphics card ~280 mm long, 3 fans, 2-slot bracket
- 1 x M.2 NVMe SSD on the board
- ATX power supply in the bottom chamber, hidden under a PSU shroud
- 3 x 120 mm front intake fans, 1 x 120 mm rear exhaust fan

HARD RULES
1. Brand-free. No logos, no real product names.
2. Every cable must be hidden: routed behind the motherboard tray through rubber-grommet cutouts, and only
   the last few cm come out next to the connector. Show the grommet cutouts.
3. Show every connector/socket (where a cable plugs in) and every screw (where it goes, how many, what type)
   because beginners will practise re-assembling from this.
4. Clean, modern, easy to read. Think "Apple-clean technical blueprint", not cluttered CAD.

YOUR TASK NOW (step 0 = overall blueprint, nothing else yet)
A. Write `design/gaming/00_blueprint.md` (Vietnamese text, simple words) containing:
   - Bill of parts for the gaming build with real-world dimensions in mm
   - Connector list: name, which part -> which part, pin count, where it plugs in
     (24-pin ATX, 8-pin CPU EPS, PCIe power, AIO pump header, CPU fan header, case fan headers,
      front-panel F_PANEL header, USB 3.0 header, HD audio header, SATA if any)
   - Screw list: per part, count, screw type (M3 standoff screw, 6-32 UNC, fan screw, M.2 screw...), location
   - Cable route plan: for each cable, which grommet it goes through and where it comes out
   - Assembly order for the simulator (numbered steps, each one short)
   - A "per-part drawing plan": one line per part describing the single image you will draw for it later
     (case, motherboard, CPU, AIO pump + radiator, RAM, GPU, SSD, PSU, case fan, cables)
B. Generate ONE image with your image generation tool: `design/gaming/00_blueprint.png`
   - A single clean blueprint sheet, white or very light background with thin blue/dark lines
   - Large exploded isometric view of the whole gaming PC in the middle, parts slightly pulled apart
     along their install direction
   - Small side elevation (glass side) and small rear-of-tray view (cable-management side) showing the
     hidden cable routes as coloured dashed lines and the grommet holes
   - Numbered callouts (1..N) for parts, small screw icons where screws go, short English labels only
     (image text must be short and legible), a legend box
   - Brand-free, no logos
Save both files, then print a short summary of what you made.
