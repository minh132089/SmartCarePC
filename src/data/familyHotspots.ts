import { BOARD_LAYOUT, type Family } from './families'
import { gamingHotspots } from './gamingHotspots'
import type { Hotspot } from './hotspots'
import type { PartId } from './parts'

/**
 * Chú thích "Xem cận cảnh" cho 6 họ case. Linh kiện dùng chung model Gaming (CPU, RAM, SSD, GPU 280, PSU ATX, AIO 240)
 * lấy lại chú thích Gaming, chỉ sửa câu nào phụ thuộc cách lắp. Linh kiện mới đọc toạ độ từ boards.json và
 * tools/build_families.py. Đơn vị app (1 = 100 mm), theo gốc của từng model.
 */
const mm = (x: number, y: number, z: number): [number, number, number] => [x / 100, y / 100, z / 100]

/** Đổi lời giải thích của một chú thích có sẵn theo tiêu đề. */
function retext(spots: Hotspot[], fix: Record<string, string>): Hotspot[] {
  return spots.map((s) => (fix[s.title] ? { ...s, text: fix[s.title] } : s))
}

function boardSpots(f: Family): Hotspot[] {
  if (f.board.ff === 'ATX') {
    const argb = f.fans.some((x) => x.model === 'fan120_argb')
    const aio = f.cooler.kind.startsWith('aio')
    return gamingHotspots('motherboard').filter((s) => (argb || !s.title.startsWith('JRAINBOW')) && (aio || !s.title.startsWith('AIO_PUMP')))
  }
  const L = BOARD_LAYOUT[f.board.ff]
  const itx = f.board.ff === 'ITX'
  const io = (L as unknown as { io: { x: [number, number]; y: [number, number] } }).io
  const ramX = L.ram_used.reduce((a, i) => a + L.ram_x[i], 0) / L.ram_used.length
  const [px, py] = L.pcie[0]
  const spots: Hotspot[] = [
    { at: mm(L.cpu[0], L.cpu[1], 7), title: 'Socket CPU', text: "Lift the lever, open the metal retention frame, align the CPU corner triangle, then lower the frame and lever." },
    {
      at: mm(ramX, L.ram_y + 74, 16),
      title: itx ? 'Hai khe RAM' : "RAM slots A2 and B2",
      text: itx ? "This small board has two slots. Fill both for dual-channel memory. Press evenly until the latches lock." : "This board has four slots. For two RAM modules, use the slots with blue latches (A2 and B2).",
    },
    {
      at: mm(px, py, 13),
      title: 'Khe PCIe x16',
      text: f.riser ? "Connect the motherboard end of the riser here. The cable connects to the graphics card in its separate mounting position." : "The long reinforced slot holds the graphics card. The latch at the end locks it in place.",
    },
    { at: mm(L.headers.atx24[0], L.headers.atx24[1], 15), title: "24-pin motherboard power", text: "The largest connector on the board. Press the cable plug in until its latch clicks." },
    { at: mm(L.headers.eps[0], L.headers.eps[1], 15), title: "8-pin CPU power", text: "On the upper edge, marked CPU_8. Use the 4+4 plug labelled CPU, not the GPU's 6+2 plug." },
    { at: mm(L.headers.cpu_fan[0], L.headers.cpu_fan[1], 10), title: 'CPU_FAN', text: "The 4-pin CPU cooler fan header. A guide ridge ensures the plug fits one way." },
    { at: mm(L.m2_center[0], L.m2_center[1], 6), title: 'Khe M.2', text: "Insert the SSD at an angle, lower it onto the end standoff and secure it with the small screw." },
    { at: mm(L.headers.panel[0], L.headers.panel[1], 9), title: 'F_PANEL', text: "Connect the case power button, reset button and LEDs here. Match the + and − markings for LEDs." },
    { at: mm(L.headers.usb3[0], L.headers.usb3[1], 11), title: "Front USB 3.0", text: "A shrouded 19-pin connector with a keyed notch. Insert straight in the correct orientation; do not force it at an angle." },
  ]
  if (L.headers.type_e) spots.push({ at: mm(L.headers.type_e[0], L.headers.type_e[1], 11), title: "Front USB-C header", text: "A small keyed header for the case USB-C port. Align the guide ridge and insert straight." })
  spots.push(
    { at: mm(io.x[0], (io.y[0] + io.y[1]) / 2, 20), title: "Rear I/O ports", text: "USB, network and audio ports face the rear of the case. When using a dedicated GPU, connect the monitor to the GPU." },
    { at: mm(L.holes[0][0], L.holes[0][1], 2), title: "Motherboard screw hole", text: `The board has ${L.holes.length} holes aligned with ${L.holes.length} case standoffs. Fit all M3 screws and tighten gently.` },
  )
  return spots
}

function gpuSpots(f: Family): Hotspot[] {
  const fanText = f.gpu.rot === 'R0' ? "The fans face the bottom of the case and draw air through the heatsink." : "The card uses a riser cable, with its fans facing the mesh or glass panel to draw in cool air."
  const pcieText = f.riser ? "Insert into the riser socket and press evenly until the latch locks. The other end connects to the board's x16 slot." : "Use the reinforced PCIe slot nearest the CPU. Open the latch first, then press evenly until it locks."
  if (f.gpu.kind === 'gpu_280') return retext(gamingHotspots('gpu'), { "Three fans": fanText, "PCIe x16 edge connector": pcieText })
  return [
    { at: mm(-22, 19.7, -4), title: "PCIe x16 edge connector", text: pcieText },
    { at: mm(105, 12.75, 120), title: "8-pin power socket", text: "Connect the PSU's 6+2-pin PCIe plug. Join the 6 and 2 pins, then press until the latch locks." },
    { at: mm(-60, -25, 60), title: "Two fans", text: fanText },
    { at: mm(-122, 12.7, 56), title: "Display outputs", text: "Three DisplayPort outputs and one HDMI output. Connect the monitor here when using this graphics card." },
    { at: mm(-122, 15.5, 113), title: "Two retaining screws", text: "The two tabs on the card bracket screw into the rear expansion slots." },
    { at: mm(89, 22.5, 62), title: "Air vent", text: "The backplate opening lets warm air escape." },
  ]
}

function psuSpots(f: Family): Hotspot[] {
  if (f.psu.kind === 'atx') {
    const fix: Record<string, string> = {}
    if (f.psu.rot !== 'R0') fix["Intake fan"] = "The PSU sits behind the motherboard tray with its fan facing the side mesh to draw in cool air."
    if (f.id === 'showcase_dual_chamber') fix["Three fixed cables"] = "The 24-pin, CPU 4+4 and PCIe 6+2 cables run behind the tray, then through the rubber grommets into the main chamber."
    return retext(gamingHotspots('psu'), fix)
  }
  return [
    { at: mm(-52, -9, 43), title: "AC power socket", text: "In a small case, an extension cable connects this socket to the rear power inlet. Connect mains power only after assembly." },
    { at: mm(-53, 17, 43), title: "I/O power switch", text: "Keep the switch at O (off) during assembly. Switch to I only when the build is complete." },
    { at: mm(0, -33, 0), title: "92 mm fan", text: "An SFX power supply is much smaller than ATX. Its fan draws air through the side mesh." },
    { at: mm(58, -13, 35), title: "Three fixed cables", text: "The fixed 24-pin, CPU 4+4 and PCIe 6+2 cables are bundled neatly along the case wall." },
    { at: mm(-50.5, 25, 55), title: "Four screw holes", text: "Four M3 holes attach the PSU to the case's L-shaped bracket." },
  ]
}

function coolerSpots(f: Family): Hotspot[] {
  switch (f.cooler.kind) {
    case 'aio_240':
      return gamingHotspots('cooler')
    case 'aio_360':
      return gamingHotspots('cooler').map((s) => {
        if (s.title === "Short top screws") return { ...s, at: mm(-172.5, 5, -52.5), text: "Twelve M3 × 6 screws secure the 360 mm radiator to the top panel. Do not confuse them with the longer fan screws." }
        if (s.title === "Long fan screws") return { ...s, at: mm(-172.5, -54, 52.5), text: "Twelve M3 × 30 screws pass through the three fan frames into the radiator." }
        if (s.title === "Airflow arrow") return { ...s, at: mm(120, -40, 61) }
        return s
      })
    case 'tower_air_155':
      return [
        { at: mm(0, -21, 1), title: "Copper contact plate", text: "This surface contacts the CPU. Remove the protective film and apply a pea-sized drop of thermal paste before installation." },
        { at: mm(39, 39, 12.5), title: "Spring-loaded screws", text: "Four spring-loaded screws. Tighten in a cross pattern, a few turns at a time, stopping when seated." },
        { at: mm(-12, 22, 156), title: "Heat pipes", text: "Four pipes carry heat from the base into the aluminium fins." },
        { at: mm(-40, 30, 100), title: "Aluminium fin stack", text: "155 mm tall. Check that the case is wide enough to close the side panel before buying." },
        { at: mm(35, 0, 90), title: "120 mm fan", text: "Air flows through the fins towards the rear of the case. Connect the fan cable to CPU_FAN." },
        { at: mm(8, -58, 140), title: "Fan clips", text: "Two steel clips hold the fan to the fins. Gently squeeze the clips to remove the fan when installing RAM." },
      ]
    case 'low_profile_47':
      return [
        { at: mm(0, -21, 1), title: "Copper contact plate", text: "This surface contacts the CPU. Remove the protective film and apply a pea-sized drop of thermal paste before installation." },
        { at: mm(39, 39, 12.5), title: "Spring-loaded screws", text: "Four recessed corner screws. Reach them with a screwdriver and tighten in a cross pattern." },
        { at: mm(46, 30, 12), title: "Heat pipes", text: "The heat pipes run horizontally through the aluminium fins." },
        { at: mm(-40, 46, 20), title: "Low-profile fins", text: "Only 47 mm tall, designed to fit small and slim cases." },
        { at: mm(0, 0, 47), title: "92 mm fan", text: "Air blows down over the CPU and motherboard. Connect the fan cable to CPU_FAN." },
      ]
  }
}

function fanSpots(f: Family): Hotspot[] {
  const fan = f.fans[0]
  if (!fan) return []
  const size = fan.model === 'fan140' ? 140 : 120
  const k = size / 140
  const hub = f.hub ? "Connect each fan to a numbered PWM port on the fan hub." : "Connect to the nearest SYS_FAN header on the motherboard."
  return gamingHotspots('fans')
    .filter((s) => fan.model === 'fan120_argb' || !s.title.startsWith("Light ring"))
    .map((s) => {
      const at = [s.at[0], s.at[1] * k, s.at[2] * k] as [number, number, number]
      if (s.title === "Airflow arrow") return { ...s, at, text: "The frame arrow shows airflow direction. Front and bottom fans draw air in; rear and top fans exhaust it." }
      if (s.title === "Screw holes") return { ...s, at, text: `A ${size} mm fan has four mounting holes spaced ${size === 140 ? '124,5' : '105'} mm apart, secured with self-tapping screws.` }
      if (s.title === "ARGB light ring") return { ...s, at, text: `The illuminated ring is on the intake side. The 3-pin 5 V lighting cable ${f.hub ? "connects to the hub's ARGB row" : "daisy-chains to the board's ARGB header"}.` }
      if (s.title === "4-pin cable") return { ...s, at, text: `${hub} The keyed plug only fits one way.` }
      return { ...s, at }
    })
}

export function familyHotspots(f: Family, id: PartId): Hotspot[] {
  switch (id) {
    case 'motherboard':
      return boardSpots(f)
    case 'cpu':
      return retext(gamingHotspots('cpu'), {
        "Metal heat spreader": f.cooler.kind.startsWith('aio') ? "This surface contacts the liquid cooler's pump base. Apply a pea-sized drop of thermal paste at the centre before mounting the pump." : "This surface contacts the air cooler's copper base. Apply a pea-sized drop of thermal paste at the centre before mounting the cooler.",
      })
    case 'ram':
    case 'ssd':
      return gamingHotspots(id)
    case 'gpu':
      return gpuSpots(f)
    case 'psu':
      return psuSpots(f)
    case 'cooler':
      return coolerSpots(f)
    case 'fans':
      return fanSpots(f)
    default:
      return []
  }
}
