import { ASSEMBLY, STEPS, type AssemblyKey } from './assembly'
import { getBuild, type BuildId } from './builds'
import { FAMILIES, gpuBehindBoard, type FamilyId } from './families'
import type { BuildCard } from '../ui/chat/chatStore'

/**
 * Hướng dẫn lắp máy từng bước cho người mới. Thứ tự các bước lắp trùng với data/assembly.ts
 * để hình 3D lắp đúng linh kiện đang được hướng dẫn. Thêm bước "Chuẩn bị" ở đầu và "Bật máy" ở cuối.
 * Nội dung đổi theo máy đang xem: tản khí / tản thấp / tản nước, nguồn ATX / SFX, card cắm thẳng / qua riser.
 */
export interface GuideStep {
  id: string
  /** Linh kiện được lắp ở bước này; null là bước không gắn với linh kiện nào (chuẩn bị, bật máy) */
  part: AssemblyKey | null
  title: string
  /** Một câu: bước này để làm gì */
  why: string
  actions: string[]
  tools?: string[]
  warning?: string
  /** Dấu hiệu cho biết đã lắp đúng */
  check: string
}

interface GuideMachine {
  cooler: 'air' | 'lowprofile' | 'aio'
  psu: 'atx' | 'sfx'
  board: 'ATX' | 'mATX' | 'ITX'
  riser: boolean
  /** Card nằm sau tấm đỡ bo mạch: lắp từ hông bên kia của vỏ */
  gpuBehind: boolean
  hub: boolean
}

function machineOf(familyId: FamilyId | null, buildId: BuildId): GuideMachine {
  if (familyId) {
    const f = FAMILIES[familyId]
    const kind = f.cooler.kind
    return {
      cooler: kind.startsWith('aio') ? 'aio' : kind.startsWith('low') ? 'lowprofile' : 'air',
      psu: f.psu.kind,
      board: f.board.ff,
      riser: !!f.riser,
      gpuBehind: gpuBehindBoard(f),
      hub: familyId === 'full_tower' || familyId === 'showcase_dual_chamber',
    }
  }
  const b = getBuild(buildId)
  return {
    cooler: b.cooler === 'aio' ? 'aio' : b.cooler === 'lowprofile' ? 'lowprofile' : 'air',
    psu: b.psu === 'SFX' ? 'sfx' : 'atx',
    board: b.formFactor,
    riser: false,
    gpuBehind: false,
    hub: false,
  }
}

export function guideSteps(familyId: FamilyId | null, buildId: BuildId, hasGpu = true): GuideStep[] {
  const m = machineOf(familyId, buildId)
  const steps: GuideStep[] = [
    {
      id: 'prep',
      part: null,
      title: "Prepare your workspace and tools",
      why: "A tidy workspace helps you keep track of screws and avoid damage from static electricity.",
      actions: [
        "Clear a large, dry, well-lit table. Avoid carpets and blankets, which can build up static electricity.",
        "Wear an anti-static wrist strap and attach its clip to bare metal on the case.",
        "Lay the case on its side and remove both side panels. In this guide, the motherboard is already mounted in the case.",
        "Open each component box only when needed. Sort the screws into separate tray compartments.",
      ],
      tools: ["No. 2 Phillips screwdriver", "Anti-static wrist strap", "Screw tray", "Cable ties"],
      warning: "Leave mains power disconnected until the final step.",
      check: "The table is clear, tools are ready and the side panels are removed.",
    },
    {
      id: 'psu',
      part: 'psu',
      title: m.psu === 'sfx' ? "Install the SFX power supply" : "Install the power supply (PSU)",
      why: "The PSU supplies power to every component. Install it first to make cable routing easier.",
      actions:
        m.psu === 'sfx'
          ? [
              "An SFX PSU is smaller than a standard unit. Remove the PSU mounting bracket from the case first.",
              "Secure the PSU to the bracket with four screws, with its fan facing a ventilated panel.",
              "Place the bracket and PSU back into the case and secure the bracket.",
              "Connect the case's power extension cable to the PSU if one is provided.",
            ]
          : [
              "Locate the PSU compartment at the bottom rear of the case.",
              "Face the PSU fan towards the filtered bottom vent so it can draw in cool air.",
              "Slide the PSU into place until its four screw holes line up with the rear panel.",
              "Fit the four PSU screws from the rear of the case.",
            ],
      tools: ["No. 2 Phillips screwdriver", "Four PSU screws (included with the case or PSU)"],
      warning: "Set the PSU's rear switch to O (off).",
      check: "The PSU is secure and does not move when gently pushed.",
    },
    {
      id: 'cpu',
      part: 'cpu',
      title: "Place the CPU in its socket",
      why: "The CPU processes instructions. It sits in the square socket on the motherboard.",
      actions: [
        "Press the socket lever down and outwards, then lift it to open the retention frame.",
        "Hold the CPU by its edges without touching the underside.",
        "Find the small triangle on one CPU corner and align it with the triangle on the socket.",
        "Lower the CPU gently. Do not press; it should sit flat by itself.",
        "Lower the frame and secure the lever. The plastic socket cover may pop off; keep it for future use.",
      ],
      tools: ["No tools needed"],
      warning: "Never force the CPU. The socket contacts bend easily. If the CPU does not sit flat, lift it out and check the triangle alignment.",
      check: "The CPU sits flat in the socket and the lever is locked.",
    },
    {
      id: 'ram',
      part: 'ram',
      title: "Install the RAM",
      why: "RAM is temporary working memory. A PC cannot boot without it.",
      actions: [
        "Open the RAM slot latches. Some boards have a latch at only one end.",
        m.board === 'ITX'
          ? "This Mini-ITX board has two slots; use both for two modules."
          : "For two modules, use the second and fourth slots from the CPU, usually labelled A2 and B2. Confirm with the motherboard manual.",
        "Align the notch on the RAM contacts with the ridge in the slot. It fits only one way.",
        "Press evenly on both ends until the module clicks into place and the latches close.",
      ],
      tools: ["No tools needed"],
      warning: "Firm pressure is needed, but stop if the module will not seat and check the notch. DDR4 and DDR5 are not interchangeable.",
      check: "The latches are closed and the modules are straight and level.",
    },
    {
      id: 'ssd',
      part: 'ssd',
      title: "Install the M.2 SSD",
      why: "The SSD stores your operating system and files. An M.2 drive connects directly to the motherboard without cables.",
      actions: [
        "Find the motherboard's M.2 slot. Remove its small heatsink cover first if fitted.",
        "Remove the small screw, or release the plastic latch, at the end of the slot.",
        "Insert the SSD's gold-contact end at roughly a 30° angle.",
        "Lower the other end so the SSD lies flat and secure the screw.",
        "If the heatsink has protective film underneath, peel it off before reinstalling the heatsink.",
      ],
      tools: ["No. 1 Phillips screwdriver"],
      warning: "The M.2 screw is easy to lose. Work slowly and keep it in the screw tray.",
      check: "The SSD lies flat against the board and is secured.",
    },
    coolerStep(m),
    {
      id: 'fans',
      part: 'fans',
      title: "Install the case fans",
      why: "Case fans bring cool air in and push warm air out. Install them before the GPU, which can obstruct screws and fan headers.",
      actions: [
        "Check the two frame arrows: one shows airflow and the other shows fan rotation.",
        "Front and bottom fans draw air in. Rear and top fans exhaust air.",
        "Secure each fan to the case with four screws.",
        m.hub
          ? "Connect the fans to the case fan hub, then connect the hub to the motherboard."
          : "Connect the fan cable to a SYS_FAN header, sometimes labelled CHA_FAN.",
        "For illuminated fans, connect the separate 3-pin lighting cable to an ARGB header.",
      ],
      tools: ["No. 2 Phillips screwdriver", "Fan screws (included with the fans)"],
      warning: "3-pin 5 V ARGB and 4-pin 12 V RGB are different. Connecting them incorrectly can damage the lighting.",
      check: "The fans are secure, clear of cables and facing the correct airflow direction.",
    },
    gpuStep(m),
    {
      id: 'cables',
      part: 'cables',
      title: "Connect and organise the cables",
      why: "Cables supply power to the components and connect the case controls to the motherboard.",
      actions: [
        "Connect the large 24-pin PSU cable to the motherboard's right edge.",
        "Connect the 8-pin cable labelled CPU or EPS near the upper-left corner, close to the CPU.",
        "Connect the power button, reset button and LED leads to F_PANEL near the lower-right corner. Follow the motherboard manual's pin diagram.",
        "Connect the front USB and HD_AUDIO cables.",
        "Route cables behind the motherboard tray and secure them with cable ties. Keep every cable clear of fan blades.",
      ],
      tools: ["Cable ties", "Side cutters"],
      warning: "CPU and GPU 8-pin connectors can look similar but are not interchangeable. Check the cable labels before connecting.",
      check: "All connectors are fully seated and no loose cables are near the fans.",
    },
    {
      id: 'finish',
      part: null,
      title: "Final checks and first boot",
      why: "Check the build before applying power to avoid short circuits and component damage.",
      actions: [
        "Check the CPU fan, 24-pin and CPU 8-pin connections. Remove any loose screws from inside the case.",
        hasGpu ? "Connect the monitor to the graphics card's output, not the motherboard's display ports."
          : "This build uses integrated graphics. Connect the monitor to HDMI or DisplayPort on the motherboard.",
        "Connect the mains cable, set the PSU switch to I and press the case power button.",
        "Spinning fans and a display logo indicate a successful start. Shut the PC down and reinstall both side panels.",
      ],
      warning: "No display? Switch off and unplug the PC, then check that the RAM is seated and the CPU 8-pin cable is connected.",
      check: "The PC powers on and displays an image.",
    },
  ]
  return hasGpu ? steps : steps.filter((s) => s.id !== 'gpu')
}

/** Gửi cùng nội dung chuẩn đang dùng cho mô hình, thay vì chỉ tên bước để AI tự đoán thao tác. */
export function guideChatContext(familyId: FamilyId | null, buildId: BuildId, index: number | null, card: BuildCard | null) {
  const hasGpu = card == null || card.items.some((i) => i.category === 'gpu')
  const steps = guideSteps(familyId, buildId, hasGpu)
  const current = index == null ? null : steps[Math.max(0, Math.min(index, steps.length - 1))]
  const ram = steps.find((s) => s.id === 'ram')!
  const parts = card?.items.map((i) => `${i.category}: ${i.quantity} × ${(i.productName || i.name).slice(0, 80)}`).join('; ')
  const context = [
    `3D model: ${familyId ?? buildId}. ${hasGpu ? "A dedicated graphics card is installed." : "No dedicated GPU; use integrated graphics and skip GPU installation."}`,
    `RAM rule from the guide: ${ram.actions[1]}`,
    ...(current ? [`Step ${steps.indexOf(current) + 1}/${steps.length}: ${current.title}`, current.warning ? `Safety: ${current.warning}` : '', `Canonical instructions: ${current.actions.join(' ')}`, `Check: ${current.check}`] : []),
    ...(parts ? [`The selected build takes priority over the sample model; use these components and quantities: ${parts}`] : []),
  ]
  return context.filter(Boolean).join('\n').slice(0, 2000)
}

function coolerStep(m: GuideMachine): GuideStep {
  const paste = "If thermal paste is already applied to the cooler base, do not add more. Otherwise, apply a pea-sized drop at the centre of the CPU without spreading it."
  if (m.cooler === 'aio') {
    return {
      id: 'cooler',
      part: 'cooler',
      title: "Apply thermal paste and install the liquid cooler",
      why: "Coolant carries heat from the CPU to the radiator, where fans exhaust it.",
      actions: [
        "Attach the fans to the radiator, then mount the radiator at the top or front of the case.",
        "Fit the correct socket mounting hardware using the cooler's manual.",
        paste,
        "Place the pump block on the CPU. Tighten its screws in a cross pattern, a few turns at a time, until secure.",
        "Connect the pump to AIO_PUMP, or CPU_FAN as specified by its manual, and the radiator fans to CPU_FAN.",
      ],
      tools: ["No. 2 Phillips screwdriver", "Thermal paste", "Paper towels"],
      warning: "Keep the pump below the highest point of the cooling loop to avoid trapped air and noise. An unpowered pump can cause the CPU to overheat quickly.",
      check: "The pump block does not rotate when gently touched, and the pump and fan cables are connected.",
    }
  }
  return {
    id: 'cooler',
    part: 'cooler',
    title: "Apply thermal paste and install the CPU cooler",
    why: "The cooler removes heat from the CPU. Thermal paste fills microscopic gaps between the CPU and cooler base.",
    actions: [
      "Fit the correct socket mounting hardware using the cooler's manual.",
      "Peel any protective film off the cooler base.",
      paste,
      m.cooler === 'lowprofile'
        ? "Lower the cooler straight down and tighten the four screws in a cross pattern until secure."
        : "Lower the cooler straight down, with its fan blowing towards the rear. Tighten the screws diagonally, a few turns at a time.",
      "Connect the cooler fan to the motherboard's CPU_FAN header.",
    ],
    tools: ["No. 2 Phillips screwdriver", "Thermal paste", "Paper towels"],
    warning: "Extra paste will not improve cooling and can spill over. An unplugged CPU_FAN cable can cause overheating and shutdown.",
    check: "The cooler does not rotate when gently touched, and the fan is connected to CPU_FAN.",
  }
}

function gpuStep(m: GuideMachine): GuideStep {
  const power = "Connect the PSU's 8-pin PCIe cable to the card until its latch clicks."
  return {
    id: 'gpu',
    part: 'gpu',
    title: "Install the graphics card",
    why: "The graphics card handles visuals for gaming, video editing and 3D work.",
    actions: m.gpuBehind
      ? [
          "This case uses a flat riser cable between the motherboard PCIe slot and the GPU chamber behind the motherboard tray.",
          "Before turning the case over, connect the motherboard end of the riser to the PCIe x16 slot nearest the CPU until it clicks.",
          "Lay the case on its side with the motherboard side facing the table. Remove the upward-facing panel; the 3D view shows this side.",
          "This separate GPU chamber sits directly beneath the removed panel. Lower the card into it from above.",
          "Remove the two rear slot covers aligned with the GPU bracket to expose the display outputs.",
          "Lower the card into the chamber with its fans facing the open side and its display ports facing the rear. Insert it into the riser socket until it clicks, then secure the rear bracket screws.",
          "Route the PSU's 8-pin PCIe cable through the tray opening into the GPU chamber.",
          power,
        ]
      : m.riser
      ? [
          "This case uses a flat riser cable to connect the motherboard PCIe slot to the GPU's separate mounting position.",
          "Connect the riser to the PCIe x16 slot nearest the CPU.",
          "Insert the card into the other riser socket until the latch clicks, then secure it to its mounting bracket.",
          power,
        ]
      : [
          "Remove the two rear slot covers aligned with the upper PCIe slot.",
          "Open the latch at the end of the PCIe x16 slot nearest the CPU.",
          "Hold the card by its edges and insert it straight into the slot until the latch clicks.",
          "Secure the card's bracket to the rear of the case.",
          power,
        ],
    tools: ["No. 2 Phillips screwdriver"],
    warning: "Support the heavy card during installation. Fully seat its power connector; a loose connection can overheat.",
    check: "The card is level, secured with screws and connected to power.",
  }
}

/**
 * Trạng thái hình 3D cho một bước, tính bằng số bước đã tháo (store.step):
 * bước chuẩn bị thì mọi linh kiện còn ở ngoài, bước lắp linh kiện thứ k thì k-1 linh kiện trước đã vào,
 * bước bật máy thì máy đã lắp đủ.
 */
export function sceneFor(step: GuideStep): { before: number; after: number } {
  if (!step.part) return step.id === 'prep' ? { before: STEPS, after: STEPS } : { before: 0, after: 0 }
  const k = ASSEMBLY.findIndex((a) => a.key === step.part) + 1
  return { before: STEPS - k + 1, after: STEPS - k }
}

// ---- Dấu tích lưu trong trình duyệt, riêng cho từng máy -------------------
// localStorage có thể bị chặn (cửa sổ ẩn danh, chặn cookie): khi đó dấu tích chỉ sống tới lúc tải lại trang.

const storageKey = (machine: string) => `pcsc-guide-done:${machine}`

export function loadDone(machine: string): Set<string> {
  try {
    const raw = localStorage.getItem(storageKey(machine))
    return new Set(raw ? (JSON.parse(raw) as string[]) : [])
  } catch {
    return new Set()
  }
}

export function saveDone(machine: string, done: Set<string>) {
  try {
    localStorage.setItem(storageKey(machine), JSON.stringify([...done]))
  } catch {
    // bỏ qua: không lưu được thì vẫn dùng bình thường trong phiên này
  }
}

/** Bước đầu tiên chưa đánh dấu xong (hết rồi thì về bước cuối) */
export function firstUndone(steps: GuideStep[], done: Set<string>) {
  const i = steps.findIndex((s) => !done.has(s.id))
  return i < 0 ? steps.length - 1 : i
}
