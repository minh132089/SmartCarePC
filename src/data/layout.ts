import { BOARDS, type Build, type FormFactor } from './builds'

// Bo mạch nào đã có model thật. Các bo còn lại dùng khối tự dựng.
export const HAS_BOARD_MODEL: Record<FormFactor, boolean> = { ITX: true, mATX: true, ATX: true }

/**
 * Vị trí các khe cắm trên bo mạch, tính theo gốc toạ độ ở tâm bo mạch.
 * x: sau (-) → trước (+), y: dưới (-) → trên (+), z: hướng ra phía mặt kính.
 * `surface` là độ cao mặt bo, linh kiện cắm lên sẽ bắt đầu từ đó.
 */
export function boardLayout(ff: FormFactor) {
  const b = BOARDS[ff]

  if (HAS_BOARD_MODEL[ff] && ff === 'ITX') {
    // Đo trên model mainboard thật (tools/models.json → motherboard), xem ảnh lưới khi xử lý.
    const ram = [0.55, 0.61]
    return {
      ...b,
      socket: { x: 0.07, y: 0.17 },
      ramX: (i: number) => ram[i],
      ramY: 0.06,
      pcieY: () => -0.75,
      slotX: 0.05,
      m2: { x: 0.2, y: -0.52 },
      surface: 0.05,
    }
  }

  if (HAS_BOARD_MODEL[ff]) {
    // Đo trên model mainboard ATX. Bo Micro-ATX là bo ATX cắt bỏ phần dưới,
    // sau khi cắt gốc toạ độ dời xuống 0,33 (xem RECENTER_DZ khi chạy npm run models).
    const dy = ff === 'mATX' ? -0.33 : 0
    const ram = [0.7, 0.79, 0.89, 0.98]
    const pcie = ff === 'mATX' ? [-0.3] : [-0.3, -0.91, -1.32]
    return {
      ...b,
      socket: { x: 0.19, y: 0.59 + dy },
      ramX: (i: number) => ram[i],
      ramY: 0.62 + dy,
      pcieY: (i: number) => pcie[Math.min(i, pcie.length - 1)] + dy,
      slotX: -0.34,
      m2: { x: 0.35, y: -0.62 + dy },
      surface: 0.03,
    }
  }

  const socket = { x: -b.w / 2 + Math.min(1.05, b.w * 0.42), y: b.h / 2 - 0.75 }
  const pcieY0 = ff === 'ITX' ? -b.h / 2 + 0.45 : socket.y - 1.25
  const slotX = -b.w / 2 + 0.3 + 0.445
  return {
    ...b,
    socket,
    ramX: (i: number) => socket.x + 0.55 + i * 0.11,
    ramY: socket.y,
    pcieY: (i: number) => pcieY0 - i * 0.4,
    slotX,
    m2: { x: slotX, y: ff === 'ITX' ? -b.h / 2 + 0.17 : pcieY0 + 0.3 },
    surface: 0,
  }
}

// 2 thanh RAM trên bo 4 khe thì cắm khe 2 và 4, đúng cách lắp chạy kênh đôi.
export function ramSlotsUsed(sticks: number, slots: number) {
  if (sticks >= slots) return Array.from({ length: slots }, (_, i) => i)
  return sticks === 2 && slots === 4 ? [1, 3] : Array.from({ length: sticks }, (_, i) => i)
}

/**
 * Khoảng cách từ vách phải tới mặt sau bo mạch. Chừa 14 mm như khay mainboard của case thật,
 * để dây nguồn luồn được phía sau bo (tools/build_cases.py dùng cùng con số).
 */
export const BOARD_STANDOFF = 0.14

/** Vị trí bo mạch trong thùng: cách vách phải BOARD_STANDOFF, mép sau sát lưng case, mép trên cách nóc 20 mm. */
export function boardOrigin(build: Build): [number, number, number] {
  const { w, h, d } = build.case
  const b = BOARDS[build.formFactor]
  return [-d / 2 + 0.15 + b.w / 2, h / 2 - 0.2 - b.h / 2, -w / 2 + BOARD_STANDOFF]
}

/**
 * Tâm quạt sau theo chiều sâu thùng (trục z). Nắp che cổng I/O của bo thật nhô ra khỏi mặt bo
 * khoảng 43–47 mm, nên quạt sau dời về phía mặt kính cho vừa tránh nắp đó.
 * tools/build_cases.py dùng cùng công thức để khoét lỗ quạt trên lưng case.
 */
export function rearFanZ(build: Build) {
  const ioDepth = build.formFactor === 'ITX' ? 0.43 : 0.47
  const ioFront = -build.case.w / 2 + BOARD_STANDOFF + ioDepth
  return Math.max(0, ioFront + build.rearFan / 2 + 0.03)
}

/**
 * Cổng cấp nguồn trên bo (toạ độ theo tâm bo), đo trên ảnh lưới của model mainboard.
 * atx24: cổng 24 pin cấp điện cho bo. cpu: cổng 8 pin (ITX là 4 pin) cấp điện cho CPU.
 */
export function powerConnectors(ff: FormFactor) {
  if (ff === 'ITX') return { atx24: { x: 0.84, y: 0.45 }, cpu: { x: -0.35, y: 0.8, pins: 4 } }
  const dy = ff === 'mATX' ? -0.33 : 0
  return { atx24: { x: 1.17, y: 0.4 + dy }, cpu: { x: -0.6, y: 1.46 + dy, pins: 8 } }
}
