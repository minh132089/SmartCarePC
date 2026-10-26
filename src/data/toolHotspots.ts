import type { Hotspot } from './hotspots'

/**
 * Chú thích "Xem cận cảnh" cho bộ đồ nghề trên thảm (t_kit.glb). Toạ độ theo gốc thảm (tâm mặt trên),
 * đơn vị cảnh (1 = 100 mm), lấy từ bảng bố trí trong design/tools/00_overview.md.
 */
export const TOOL_HOTSPOTS: Hotspot[] = [
  {
    at: [-1.6, 0.34, -1.4],
    title: "1. Interchangeable-bit screwdriver",
    text: "Use the larger PH2 bit for motherboard, PSU and fan screws. The smaller PH1 bit beside it is for the M.2 SSD screw. Magnetic tips help hold screws.",
  },
  {
    at: [1.3, 0.22, -1.35],
    title: "2. Anti-static wrist strap",
    text: "Unplug the PC first. Wear the strap with its metal plate against your skin and clip it to bare case metal. Remove the strap before connecting mains power for testing.",
  },
  {
    at: [-1.9, 0.14, -0.45],
    title: "3. Thermal paste",
    text: "Apply a pea-sized drop to the centre of the CPU just before mounting the cooler. Do not add paste if it is pre-applied. Use the spreader only if the cooler manual calls for it.",
  },
  {
    at: [0.3, 0.2, -0.2],
    title: "4. Screw organiser",
    text: "Eight compartments hold motherboard M3 screws, standoffs, M.2 screws, PSU and slot-cover screws, fan screws, short and long radiator screws, and a 5 mm standoff socket. Use the correct screw type without forcing it.",
  },
  {
    at: [-1.85, 0.05, 0.8],
    title: "5. Cable ties and hook-and-loop straps",
    text: "Bundle cables behind the motherboard tray. Tighten cable ties gently and trim the ends; hook-and-loop straps can be reused.",
  },
  {
    at: [0.25, 0.1, 1.35],
    title: "6. Mains power cable",
    text: "The three-hole C13 connector fits the PSU's rear C14 inlet; the wall plug fits a compatible outlet. Connect it last, after assembly is complete.",
  },
  {
    at: [2.25, 0.1, -0.2],
    title: "7. Wi-Fi antennas",
    text: "Only boards with Wi-Fi, such as this Gaming model, need these. Screw both antennas onto the threaded rear connectors and point them upwards.",
  },
  {
    at: [2.05, 0.08, 1.3],
    title: "8. Flush cutters",
    text: "Trim cable-tie ends close to the locking head. Keep the cutter jaws away from power and fan cables.",
  },
]
