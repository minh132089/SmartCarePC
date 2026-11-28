import type { PartId } from './parts'
import type { Cooler } from './maintenance'

/*
 * Chỉ dẫn cho người mới, thêm sau đợt phản hồi 22/11: người dùng thấy kế hoạch nâng cấp "chung chung, thiếu dẫn dắt để tự
 * làm" và hướng dẫn bảo dưỡng "không tìm thấy linh kiện trên máy mình". Chữ ngắn, nói cái người dùng nhìn thấy được.
 */

/** Bảo dưỡng: nhận ra linh kiện trên máy thật (máy của người dùng có thể khác mô hình 3D). Khoá theo part hoặc id bước. */
export function findOnYourPc(key: string, cooler: Cooler): string | null {
  switch (key) {
    case 'filters':
      return 'Fine mesh sheets in front of the fans: behind the front panel, under the PC (they slide out), and sometimes a magnetic sheet on top. Not every case has all three.'
    case 'fans':
      return 'Round fans fixed to the case walls: usually two or three at the front, one at the back next to the ports, sometimes on top. Each has a thin cable to the motherboard.'
    case 'cooler':
      return cooler === 'aio'
        ? 'A round or square block in the middle of the motherboard with two thick tubes. The tubes lead to the radiator: a flat block of thin fins with fans, usually at the top or front of the case.'
        : cooler === 'lowprofile'
          ? 'A flat metal block with one fan lying on top, in the middle of the motherboard. The CPU is hidden underneath it.'
          : 'A tall block of thin metal fins with a fan on one side, in the middle of the motherboard. The CPU is hidden underneath it.'
    case 'gpu':
      return 'The biggest card, lying flat across the middle of the case below the CPU cooler, with one to three fans facing down. Its metal end, with the display ports, shows at the back of the PC.'
    case 'psu':
      return 'A metal box, usually at the bottom back of the case (at the top in some older cases). The mains cable and the I/O switch on the back of the PC belong to it.'
    default:
      return null
  }
}

export interface InstallHelp {
  /** Mức độ cho người mới tự làm */
  level: 'Easy' | 'Moderate' | 'Ask a shop if unsure'
  time: string
  steps: string[]
  /** Cách biết đã lắp đúng sau khi bật máy */
  after: string
}

/** Nâng cấp: tự lắp linh kiện mới thế nào. Bước nào cũng giả định đã tắt máy, rút điện và mở nắp hông. */
export const INSTALL_HELP: Partial<Record<PartId, InstallHelp>> = {
  ram: {
    level: 'Easy',
    time: 'About 10 minutes',
    steps: [
      'Shut down, unplug the PC and open the side panel. Touch a bare metal part of the case first.',
      'Push open the clips at the ends of a free RAM slot. With two sticks, use the slots the motherboard manual marks first (often the 2nd and 4th).',
      'Line up the notch on the stick with the bump in the slot, then press both ends down firmly until the clips click shut.',
    ],
    after: 'Windows: Settings › System › About shows the new RAM total.',
  },
  ssd: {
    level: 'Moderate',
    time: '20–30 minutes, plus copying or reinstalling Windows if it replaces your old drive',
    steps: [
      'M.2 SSD: find the small slot on the motherboard (sometimes under a metal cover). Remove its screw, slide the SSD in at a slight angle, press it flat and refit the screw.',
      'SATA SSD (2.5-inch box): fix it in a drive bay, then connect a SATA data cable to the motherboard and a SATA power cable from the power supply.',
      'To move Windows onto it, use the SSD maker’s free cloning tool, or ask a shop to do it.',
    ],
    after: 'Windows: right-click Start › Disk Management lists the new drive. A new empty drive must be initialised there before it shows in File Explorer.',
  },
  gpu: {
    level: 'Moderate',
    time: 'About 20 minutes',
    steps: [
      'Check that your power supply has the PCIe power plugs the new card needs (the plan lists the wattage).',
      'Unplug the old card’s power cables, press the small latch at the end of its slot and lift it out. Remove the screws holding its bracket first.',
      'Push the new card straight down into the long top slot until the latch clicks, screw the bracket in, then connect the PCIe power cables.',
      'Plug your monitor into the graphics card, not into the motherboard.',
    ],
    after: 'Install the newest driver from the NVIDIA, AMD or Intel website. Task Manager › Performance shows the card.',
  },
  psu: {
    level: 'Ask a shop if unsure',
    time: '45–60 minutes',
    steps: [
      'Photograph every power cable before you start: every part gets its power from here.',
      'Unplug all its cables from the parts, undo the four screws at the back and lift it out.',
      'Fit the new one the same way round (fan facing a vent), then connect the motherboard, CPU, graphics card and drive cables again.',
    ],
    after: 'The PC starts, all fans spin, and there is no burning smell or clicking.',
  },
  cpu: {
    level: 'Ask a shop if unsure',
    time: '45–60 minutes',
    steps: [
      'Check first whether your motherboard needs a BIOS update for the new CPU. Do this before removing the old CPU.',
      'Remove the cooler, lift the socket lever, swap the CPU by matching the small triangle on its corner, and close the lever.',
      'Clean the old paste, apply a pea-sized drop of new paste and refit the cooler.',
    ],
    after: 'The PC starts and Windows: Settings › System › About shows the new processor.',
  },
  cooler: {
    level: 'Moderate',
    time: '30–45 minutes',
    steps: [
      'Fit the new cooler’s back plate or brackets as its manual shows, if needed.',
      'Remove the old cooler, clean the old paste off the CPU and apply a pea-sized drop of new paste (skip if the new cooler has paste already).',
      'Tighten the cooler evenly in a cross pattern and plug its fan into CPU_FAN.',
    ],
    after: 'Fans spin at start-up and the CPU temperature stays steady under your usual work.',
  },
  fans: {
    level: 'Easy',
    time: 'About 15 minutes',
    steps: [
      'Fix the fan to a free mount with its four screws. The arrows on the frame show airflow: front fans blow in, back and top fans blow out.',
      'Plug its cable into a SYS_FAN or CHA_FAN header on the motherboard, or into the case’s fan hub.',
    ],
    after: 'The new fan spins when the PC is on.',
  },
  motherboard: {
    level: 'Ask a shop if unsure',
    time: '2 hours or more',
    steps: ['Every other part has to come out and go back in, and Windows may need reactivating. A shop can do this safely, often for a small fee.'],
    after: 'The PC starts and shows the BIOS screen.',
  },
  case: {
    level: 'Ask a shop if unsure',
    time: '2 hours or more',
    steps: ['Every part moves into the new case. Use the 3D assembly guide as the order of work, or ask a shop.'],
    after: 'The PC starts and all front ports and fans work.',
  },
}
