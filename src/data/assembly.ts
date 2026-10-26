import { useViewer } from '../store'

/**
 * Thứ tự lắp máy (bo mạch và vỏ đã nằm sẵn). "Tách rời" tháo ngược từ bước cuối về bước đầu,
 * "Lắp lại" lắp xuôi từ bước đầu, mỗi lần một bước.
 */
export const ASSEMBLY = [
  { key: 'psu', install: "Install the power supply", remove: "Remove the power supply" },
  { key: 'cpu', install: "Place the CPU in its socket", remove: "Lift out the CPU" },
  { key: 'ram', install: "Install the RAM", remove: "Remove the RAM" },
  { key: 'ssd', install: "Install the M.2 SSD", remove: "Remove the SSD" },
  { key: 'cooler', install: "Apply thermal paste and install the cooler", remove: "Remove the cooler" },
  // Quạt trước card: card nằm ngang giữa vỏ sẽ che chỗ bắt vít quạt trước / đáy và chân cắm quạt trên bo
  { key: 'fans', install: "Install the case fans", remove: "Remove the case fans" },
  { key: 'gpu', install: "Install the graphics card", remove: "Remove the graphics card" },
  { key: 'cables', install: "Connect and organise the cables", remove: "Disconnect the cables" },
] as const

export type AssemblyKey = (typeof ASSEMBLY)[number]['key']
export const STEPS = ASSEMBLY.length
/** Mỗi bước chờ bước trước chạy xong (lò xo ~0,45 s) rồi mới bắt đầu. */
export const STEP_MS = 620

const index = (k: AssemblyKey) => ASSEMBLY.findIndex((s) => s.key === k) + 1

/** Bước k đang ở ngoài máy khi số bước đã tháo (`step`) đủ để tới nó: tháo từ bước cuối. */
export const isOut = (step: number, k: AssemblyKey) => step >= STEPS - index(k) + 1

export function useOut(k: AssemblyKey) {
  return useViewer((s) => isOut(s.step, k))
}
