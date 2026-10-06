/**
 * Số đo lắp ráp cho máy Gaming, lấy từ bản vẽ của Codex (design/gaming/00_blueprint.md và các tờ 01–10).
 * Đơn vị trong file là mm theo trục của thùng máy: x lưng → trước, y đáy → nóc, z vách kín → kính.
 * Dùng `u()` để đổi sang đơn vị app (1 = 100 mm).
 */
export type Mm3 = [number, number, number]

export const u = ([x, y, z]: Mm3): Mm3 => [x / 100, y / 100, z / 100]
export const add = (a: Mm3, b: Mm3): Mm3 => [a[0] + b[0], a[1] + b[1], a[2] + b[2]]

/** Tâm bo mạch trong thùng (trùng boardOrigin của app cho thùng Gaming). */
export const BOARD: Mm3 = [-88, 62.5, -96]
/** Mặt trên PCB tính từ lưng bo. */
export const PCB_TOP = 1.6

/** Vị trí linh kiện trên bo, toạ độ theo tâm bo, z tính từ lưng bo. */
export const ON_BOARD = {
  cpu: [19, 59, 4.7] as Mm3, // CPU nằm trong lòng socket
  pump: [19, 59, 9.2] as Mm3, // đế bơm áp lên nắp CPU
  backplate: [19, 59, 0] as Mm3,
  ram: [[79, 62, 5.5], [98, 62, 5.5]] as Mm3[], // khe A2 và B2
  ssd: [35, -62, 3.5] as Mm3,
  gpu: [8, -54.7, 14.6] as Mm3, // lá cổng ở x = -132, chân PCIe khớp khe x16 đầu tiên
}

/** Đầu cắm trên bo (tâm bo, z là mặt trên đầu cắm). */
export const HEADERS = {
  atx24: [117, 40, 14.6] as Mm3,
  eps: [-60, 146, 14.6] as Mm3,
  pump: [10, 126, 9.6] as Mm3,
  cpuFan: [35, 126, 9.6] as Mm3,
  sys1: [106, 85, 9.6] as Mm3,
  sys2: [82, -128, 9.6] as Mm3,
  sys3: [95, -134, 9.6] as Mm3,
  sys4: [-96, -135, 9.6] as Mm3,
  panel: [95, -143, 9.1] as Mm3,
  usb3: [113, -127, 10.6] as Mm3,
  audio: [-97, -143, 9.1] as Mm3,
  /** Đầu ARGB 5 V (JRAINBOW1): 4 vị trí bước 2,54, khuyết vị trí 3 (tờ 12_case_front) */
  argb: [55, -143, 9.1] as Mm3,
}
export const onBoard = (p: Mm3): Mm3 => add(BOARD, p)

/** Các linh kiện gắn trên thùng. */
export const CASE_PARTS = {
  psu: [-152, -189, -26] as Mm3, // tâm khối nguồn 140 × 86 × 150: mặt sau áp tấm lưng (x -222), không chạm khay (z -101…49)
  psuExit: [-80, -202, 24] as Mm3, // vòng cao su ra dây ở mặt trước nguồn
  radiator: [-1.5, 231, 36] as Mm3, // giữa mặt trên két
  frontFans: [-142, 0, 142].map((y) => [208.5, y, 0] as Mm3), // 3 quạt 140 mm, mặt hút áp giá quạt (x 221)
  rearFan: [-209.5, 145, 14] as Mm3, // mặt thổi áp mặt trong tấm lưng (x -222)
}

/** Lỗ luồn dây trên khay (tâm), mặt trước khay ở z = -102.5, khe sau khay ở z ≈ -106. */
export const TRAY_FRONT = -101
export const BEHIND = -106.2
/** Tâm sợi dây trong khe sau khay phải nằm trong khoảng này (khay z -102,5 dày 1, nắp kín z -109). */
export const BEHIND_RANGE: [number, number] = [-107.8, -104.7]
export const GROMMET = {
  G0: [-35, -170] as [number, number],
  G1: [-148, 224] as [number, number],
  G2: [57, 110] as [number, number],
  G3: [-65, 224] as [number, number],
  G5: [23, -104] as [number, number],
  G6: [-184, -104] as [number, number],
}
/** Miệng ống che dây GPU (G4), hướng về phía sau máy (-x), mở 18 × 24 mm. Ống che x 95…123, z 13…37. */
export const G4_MOUTH: Mm3 = [95, 10, 25]
export const G4_DUCT_X = 109

/** Đầu ống nước: ở bơm (theo gốc bơm) và ở két (theo gốc két). */
export const PUMP_FITTINGS: Mm3[] = [[-9, 38, 34], [9, 38, 34]]
export const RAD_FITTINGS: Mm3[] = [[129, -40, -15], [129, -40, 15]]

/** Ổ nguồn 8 chân trên GPU (theo gốc GPU), miệng hướng +z. */
export const GPU_POWER: Mm3 = [125, 18.55, 125]
