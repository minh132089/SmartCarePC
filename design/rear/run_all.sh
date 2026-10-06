#!/bin/sh
cd "$(dirname "$0")"
./run_rear.sh standard_tower "family standard_tower (ATX, tower air cooler, GPU 280 horizontal, ATX PSU bottom, 120 rear fan)" &
./run_rear.sh compact_tower "family compact_tower (mATX, GPU 240 horizontal, ATX PSU bottom, 4 PCIe covers)" &
./run_rear.sh showcase_dual_chamber "family showcase_dual_chamber (ATX, GPU 280 VERTICAL via riser with 3 vertical slots, ATX PSU standing behind the tray in the rear chamber, rear fan flipped)" &
./run_rear.sh gaming "the Gaming mid-tower (design/gaming, ATX board, AIO 240, GPU 280 horizontal, ATX PSU bottom, ARGB rear fan, Wi-Fi antenna ports on the board I/O)" &
wait
./run_rear.sh mini_sff "family mini_sff (ITX sandwich, GPU 240 behind the board via riser, 3 vertical slots, SFX PSU at the front with internal AC extension to a C14 inlet on the rear)" &
./run_rear.sh full_tower "family full_tower (ATX, AIO 360 on the roof, GPU 280 horizontal, 8 PCIe covers, ATX PSU bottom, 140 rear fan)" &
wait
