import type { CoolerKind } from './builds'
import type { PartId } from './parts'

/**
 * Chấm chú thích cho chế độ "Xem cận cảnh". Toạ độ theo gốc của từng model (đơn vị app, 1 = 100 mm),
 * đo bằng tools/find_features.py và ảnh lưới khi xử lý model. Lời giải thích viết cho người mới,
 * để họ đối chiếu với linh kiện thật đang cầm trên tay.
 */
export interface Hotspot {
  at: [number, number, number]
  title: string
  text: string
}

/** Linh kiện có chế độ xem cận cảnh (vỏ case và dây nguồn thì không cần). */
export const INSPECTABLE: PartId[] = ['motherboard', 'cpu', 'cooler', 'ram', 'gpu', 'ssd', 'psu', 'fans', 'tools']

type Variant = 'heatsink' | 'plain'

export function hotspotsFor(
  id: PartId,
  opts: { ram?: Variant; board?: 'ITX' | 'mATX' | 'ATX'; cooler?: CoolerKind } = {},
): Hotspot[] {
  switch (id) {
    case 'ram': {
      // Khấc khoá đo trên từng model: RAM có tản nhiệt lệch tâm -0,151, RAM thường khoảng -0,107
      const notch = opts.ram === 'plain' ? -0.107 : -0.151
      const top = opts.ram === 'plain' ? 0.3 : 0.5
      return [
        {
          at: [0, notch, 0.01],
          title: "Keying notch",
          text: "The off-centre gap in the contacts matches the slot's ridge, so the module fits only one way. Check orientation rather than forcing it.",
        },
        {
          at: [0, 0.38, 0.015],
          title: "Gold-plated contacts",
          text: "These contacts fit into the slot. Hold the RAM by its edges and avoid touching the gold contacts.",
        },
        {
          at: [0, 0.64, 0.08],
          title: "Latch notch",
          text: "The notches at both ends catch the plastic slot latches when the RAM is fully seated.",
        },
        {
          at: [0.03, 0.1, top * 0.7],
          title: opts.ram === 'plain' ? "Memory chips" : "Heat spreader",
          text:
            opts.ram === 'plain'
              ? "The black chips hold temporary data. Standard RAM exposes these chips."
              : "The metal heat spreader helps cool the RAM. Check that tall modules clear the CPU cooler.",
        },
      ]
    }
    case 'cpu':
      return [
        {
          at: [-0.18, 0.175, 0.016],
          title: "Triangle marker",
          text: "Align the CPU's gold triangle with the socket triangle. Lower it gently without pressing or sliding.",
        },
        {
          at: [0, 0, 0.045],
          title: "Metal heat spreader",
          text: "This surface contacts the cooler base. Apply a pea-sized drop of thermal paste at the centre before mounting the cooler.",
        },
        {
          at: [0.19, -0.1, 0.006],
          title: "Circuit substrate",
          text: "The underside contacts are easily damaged. Hold the CPU only by its edges.",
        },
      ]
    case 'gpu':
      return [
        {
          at: [-0.6, 0, 0.24],
          title: "PCIe edge connector",
          text: "Insert into the longest PCIe slot nearest the CPU. Open the end latch first and press evenly until it locks.",
        },
        {
          at: [-1.38, 0, 0.66],
          title: "Rear bracket and display outputs",
          text: "The metal bracket screws into the rear of the case. Connect the monitor here when using this GPU.",
        },
        {
          at: [0.5, 0, 1.32],
          title: "PCIe power socket",
          text: "Connect the PSU's PCIe cable here. A card requiring this connection may not output an image without it.",
        },
        {
          at: [0.3, -0.27, 0.66],
          title: "Cooling fans",
          text: "The fans face down towards the PSU. Leave clearance beneath the card for airflow.",
        },
      ]
    case 'ssd':
      return [
        {
          at: [-0.39, 0, 0.012],
          title: "Keyed M.2 connector",
          text: "Insert at roughly 30° into the keyed M.2 slot, then lower the end of the drive.",
        },
        {
          at: [0.39, 0, 0.012],
          title: "Screw cutout",
          text: "The semicircular cutout at the end takes the small motherboard screw that holds the SSD flat.",
        },
        {
          at: [0.05, 0, 0.026],
          title: "Memory chips",
          text: "The chips face upwards. Remove the protective film beneath the motherboard's M.2 heatsink if fitted.",
        },
      ]
    case 'psu':
      return [
        {
          at: [0.7, 0.05, 0.1],
          title: "Cable sockets",
          text: "Each socket is labelled CPU, PCIe or 24-pin. Connecting cables before placing the PSU in the case is easier.",
        },
        {
          at: [-0.7, -0.15, 0.35],
          title: "AC inlet and switch",
          text: "These face the rear. Set the switch to I only after assembly is complete.",
        },
        {
          at: [0, 0.41, 0],
          title: "PSU fan",
          text: "When the case has a bottom vent, face the fan downwards to draw cool air from below.",
        },
      ]
    case 'cooler':
      // Tản nước: xem cận cảnh hiện két tản ở trên, bơm ở dưới (toạ độ theo cách xếp trong Inspector)
      if (opts.cooler === 'aio')
        return [
          { at: [0, -1.1, 0.43], title: "Pump", text: "The round CPU block replaces an air cooler. Its pump circulates coolant through the radiator." },
          { at: [0.33, -1.1, 0.03], title: "Copper contact plate", text: "This contacts the CPU. Remove protective film and check whether paste is pre-applied before mounting." },
          { at: [-1.05, -0.08, 0.6], title: "Radiator", text: "Mount at the top or front of the case. Fans cool the coolant as it flows through the radiator and back to the pump." },
          { at: [0.59, -0.33, 0.3], title: "Radiator fans", text: "For a top radiator, exhaust air upwards. Follow the cooler manual for the correct CPU_FAN or pump header connections." },
        ]
      if (opts.cooler && opts.cooler !== 'tower')
        return [
          { at: [0, 0, 0.02], title: "Contact base", text: "This contacts the CPU. Stock coolers often have pre-applied paste; do not add more if already present." },
          { at: [0, 0, 0.45], title: "Downward-facing fan", text: "The fan blows directly over the CPU and surrounding components." },
          { at: [0.4, 0.3, 0.2], title: "Cooling fins", text: "Receive heat from the base and release it into passing air." },
        ]
      return [
        {
          at: [0, 0, 0.03],
          title: "Contact base",
          text: "This contacts the CPU. Remove any protective film before installation.",
        },
        {
          at: [0, 0.3, 0.9],
          title: "Cooling fins",
          text: "CPU heat travels here and is carried away by airflow.",
        },
        {
          at: [0.42, 0, 0.95],
          title: "Fan",
          text: "The frame arrow shows airflow direction. Aim airflow towards the rear of the case.",
        },
      ]
    case 'fans':
      return [
        {
          at: [0.1, 0, 0],
          title: "Intake side",
          text: "The open blade side draws air in. The side with motor supports exhausts air.",
        },
        {
          at: [0, 0.44, 0.44],
          title: "Mounting holes",
          text: "Four corner holes attach the fan to the case. Front fans draw air in; rear and top fans exhaust it.",
        },
      ]
    case 'motherboard': {
      const dy = opts.board === 'mATX' ? -0.33 : 0
      if (opts.board === 'ITX')
        return [
          { at: [0.07, 0.17, 0.08], title: 'Socket CPU', text: "The CPU sits here. Open the lever, align the triangle markers and secure the lever." },
          { at: [0.58, 0.06, 0.14], title: 'Khe RAM', text: "Open the latches, align the RAM notch with the slot guide and press straight down." },
          { at: [0.84, 0.45, 0.1], title: "24-pin motherboard power", text: "The largest connector on the board. Press until its latch clicks." },
          { at: [-0.35, 0.8, 0.1], title: "CPU power connector", text: "The small connector near the top corner supplies CPU power. The PC will not boot without it." },
          { at: [-0.75, -0.1, 0.25], title: "Rear I/O ports", text: "Rear-facing USB, network and audio ports." },
        ]
      return [
        { at: [0.19, 0.59 + dy, 0.06], title: 'Socket CPU', text: "The CPU sits here. Open the lever, align the triangle markers and secure the lever." },
        { at: [0.84, 0.62 + dy, 0.12], title: 'Khe RAM', text: "For two modules, use the second and fourth slots from the CPU for dual-channel memory. Confirm with the board manual." },
        { at: [-0.34, -0.3 + dy, 0.08], title: 'Khe PCIe x16', text: "The longest slot nearest the CPU holds the graphics card." },
        { at: [1.17, 0.4 + dy, 0.1], title: "24-pin motherboard power", text: "The largest connector on the board. Press until its latch clicks." },
        { at: [-0.6, 1.46 + dy, 0.1], title: "8-pin CPU power", text: "Near the upper-left corner. The PC will not boot without this power connection." },
        { at: [0.35, -0.62 + dy, 0.05], title: 'Khe M.2', text: "Insert the M.2 SSD here and secure it with the small screw." },
      ]
    }
    default:
      return []
  }
}
