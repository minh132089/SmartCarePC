import type { Hotspot } from './hotspots'
import type { PartId } from './parts'

/**
 * Chú thích "Xem cận cảnh" cho bộ linh kiện Gaming. Toạ độ theo gốc của từng model g_*.glb (đơn vị app,
 * 1 = 100 mm), lấy thẳng từ số đo trong tools/build_gaming.py nên khớp đúng chi tiết trên model.
 */
export function gamingHotspots(id: PartId): Hotspot[] {
  switch (id) {
    case 'motherboard':
      return [
        { at: [0.19, 0.59, 0.07], title: 'Socket CPU', text: "Lift the right-hand lever, open the steel frame, align the CPU triangle with the lower-left corner marker, then lower the frame and lever." },
        { at: [0.885, 1.36, 0.16], title: "RAM slots A2 and B2", text: "The two slots with blue latches. This PC uses two RAM modules in A2 and B2 for dual-channel memory." },
        { at: [-0.34, -0.3, 0.13], title: 'Khe PCIe x16', text: "The long reinforced slot nearest the CPU. Insert the graphics card here; the plastic latch at the right end locks it in place." },
        { at: [1.17, 0.4, 0.15], title: "24-pin motherboard power", text: "The largest connector on the board's right edge. Press the cable plug until its latch clicks." },
        { at: [-0.6, 1.46, 0.15], title: "8-pin CPU power", text: "On the upper edge, marked CPU_8. Use the 4+4 plug labelled CPU, not the GPU's 6+2 plug." },
        { at: [0.225, 1.26, 0.1], title: "AIO_PUMP and CPU_FAN", text: "Two adjacent 4-pin headers. Connect the pump to AIO_PUMP and the radiator fans, through their splitter, to CPU_FAN." },
        { at: [-0.05, -0.62, 0.06], title: 'Khe M.2', text: "Insert the SSD here at an angle, lower it onto the right-hand standoff and secure it with the small screw." },
        { at: [0.95, -1.43, 0.09], title: 'F_PANEL', text: "Connect the case power button, reset button and LEDs here. Follow the printed labels and match the + and − LED pins." },
        { at: [1.13, -1.27, 0.11], title: "Front USB 3.0", text: "A shrouded 19-pin connector with a keyed notch. Insert straight in the correct orientation; do not force it at an angle." },
        { at: [0.55, -1.43, 0.09], title: "JRAINBOW1 (ARGB lighting)", text: "A keyed 3-pin 5 V header for the front fan light rings. Never connect to a 12 V RGB header." },
        { at: [-1.37, 0.6, 0.2], title: "Rear I/O ports", text: "Rear-facing USB ports, USB-C, network, two Wi-Fi antenna connectors, HDMI/DP and audio. With a dedicated GPU, connect the monitor to that card." },
        { at: [0, 0, 0.02], title: "Motherboard screw hole", text: "Nine holes align with nine case standoffs. Fit all nine M3 screws and tighten gently." },
      ]
    case 'cpu':
      return [
        { at: [-0.174, -0.174, 0.013], title: "Triangle marker", text: "Match the gold CPU triangle with the socket's triangle. Lower the CPU gently without pressing or sliding it." },
        { at: [0.1875, 0.05, 0.006], title: "Alignment notches", text: "Two edge notches match the socket's plastic guides. The CPU will not sit flat in the wrong orientation." },
        { at: [0, 0, 0.046], title: "Metal heat spreader", text: "This surface contacts the liquid cooler's pump base. Apply a pea-sized drop of thermal paste at the centre before mounting the pump." },
        { at: [0, -0.12, -0.001], title: "Gold contact pads", text: "The flat gold pads underneath are easily contaminated. Hold the CPU only by its edges." },
      ]
    case 'cooler':
      return [
        { at: [0, -1.1, 0.48], title: "Pump", text: "The round block sits on the CPU. Its internal pump circulates coolant through the radiator and back." },
        { at: [0.39, -0.71, 0.14], title: "Spring-loaded screws", text: "Four spring-loaded screws secure the pump. Tighten diagonally, a few turns at a time, stopping when seated." },
        { at: [0.25, -1.1, -0.001], title: "Protective film", text: "Clear film protects the copper base. Peel it off using the small tab before installation; leaving it on can overheat the CPU." },
        { at: [0, -0.72, 0.34], title: "Two coolant tubes", text: "Connect the pump and radiator. Do not kink or stretch them." },
        { at: [-1.125, 0.05, -0.525], title: "Short top screws", text: "Eight M3 × 6 screws attach the radiator to the case roof. Longer fan screws can puncture the radiator." },
        { at: [0.6, -0.4, 0.61], title: "Airflow arrow", text: "Radiator fans push air upwards through the top panel. The moulded arrows show airflow direction." },
        { at: [-1.125, -0.54, 0.525], title: "Long fan screws", text: "Eight M3 × 30 screws pass through the fan frames into the radiator." },
      ]
    case 'ram':
      return [
        { at: [0, 0.0425, 0.015], title: "Keying notch", text: "An off-centre gap in the contact edge matches the RAM slot guide, so the module fits only one way." },
        { at: [0.007, -0.3, 0.026], title: "Gold-plated contacts", text: "288 contacts, 144 on each side. Hold the RAM by its edges and avoid touching the gold contacts." },
        { at: [0, 0.66, 0.085], title: "Latch recesses", text: "Small recesses at both ends catch the slot latches when the RAM is fully seated." },
        { at: [0.031, 0.2, 0.25], title: "Heat spreader", text: "The aluminium cover helps cool the RAM. No screws or cables are needed." },
      ]
    case 'gpu':
      return [
        { at: [-0.42, 0.247, -0.04], title: "PCIe x16 edge connector", text: "Use the reinforced PCIe slot nearest the CPU. Open the end latch first and press evenly until it locks." },
        { at: [1.25, 0.185, 1.25], title: "8-pin power socket", text: "Connect the PSU's 6+2-pin PCIe plug. Join the 6 and 2 pins, then press until the latch locks." },
        { at: [0, -0.275, 0.625], title: "Three fans", text: "The fans face the bottom of the case and draw air through the heatsink." },
        { at: [-1.41, 0.17, 0.56], title: "Display outputs", text: "Three DisplayPort outputs and one HDMI output. Connect the monitor here when using this graphics card." },
        { at: [-1.42, 0.2, 1.13], title: "Two retaining screws", text: "Remove two rear PCIe slot covers and keep the two screws to secure the card." },
        { at: [1.08, 0.276, 0.69], title: "Air vent", text: "The backplate opening lets warm air escape upwards." },
      ]
    case 'ssd':
      return [
        { at: [-0.39, -0.065, 0.02], title: "M-key notch", text: "The off-centre notch matches the M.2 slot guide. Insert at roughly 25°, then lower the drive." },
        { at: [0.4, 0, 0.025], title: "Retaining screw", text: "One small M2 screw fits the semicircular cutout at the end of the drive. Tighten gently without bending the drive." },
        { at: [-0.07, 0, 0.032], title: "Memory chips", text: "Store your data. An M.2 SSD needs no separate power or data cables." },
      ]
    case 'psu':
      return [
        { at: [-0.71, -0.16, 0.49], title: "AC power socket", text: "Connect the wall power cable here only at the final step, once the PC is fully assembled." },
        { at: [-0.72, 0.14, 0.49], title: "I/O power switch", text: "Keep the switch at O (off) during assembly. Switch to I only when the build is complete." },
        { at: [0, -0.44, 0], title: "Intake fan", text: "Face the fan towards the case's filtered bottom intake so the PSU can draw cool air from below." },
        { at: [0.71, -0.13, 0.5], title: "Three fixed cables", text: "The 24-pin, CPU 4+4 and PCIe 6+2 cables pass through G0 and run behind the motherboard tray." },
        { at: [-0.72, -0.37, -0.69], title: "Four rear screws", text: "Four asymmetrical holes fit only one orientation. Install all four screws from the rear of the case." },
      ]
    case 'fans':
      return [
        { at: [0, 0, 0.705], title: "Airflow arrow", text: "The frame arrow shows airflow direction. Three front fans draw air in and the rear fan exhausts it." },
        { at: [-0.13, 0, 0.2], title: "Support-strut side", text: "The side with four support struts is the exhaust side." },
        { at: [0.125, 0.6225, 0.6225], title: "Screw holes", text: "The 140 mm front fans have mounting holes spaced 124.5 mm apart. Self-tapping screws secure them from the front to the fan bracket." },
        { at: [0.13, 0.35, 0.55], title: "ARGB light ring", text: "A cyan ring on the intake side. Daisy-chain the separate 3-pin 5 V lighting cables, then connect to JRAINBOW1." },
        { at: [-0.1, 0.55, -0.71], title: "4-pin cable", text: "Connect to the nearest SYS_FAN header on the motherboard. The keyed plug fits only one way." },
      ]
    default:
      return []
  }
}
