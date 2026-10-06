/**
 * Mức chất lượng hình theo sức máy người dùng. Máy yếu bỏ bớt hiệu ứng nặng để không giật, không nóng.
 * "auto": tự chọn khi mở trang (detectQuality) rồi tự hạ thêm nếu đo thấy khung hình thấp (FpsWatch).
 */
export type QualityLevel = 'high' | 'medium' | 'low'
export type QualityPref = 'auto' | QualityLevel

export const QUALITY_LABEL: Record<QualityLevel, string> = { high: 'Cao', medium: 'Vừa', low: 'Nhẹ' }

export interface QualitySettings {
  /** Giới hạn tỉ lệ điểm ảnh: màn Retina vẽ gấp 4 lần điểm ảnh ở mức 2 */
  dpr: [number, number]
  /** Bóng tối ở khe giữa linh kiện (N8AO), hiệu ứng nặng nhất */
  ao: boolean
  /** Quầng sáng của đèn ARGB */
  bloom: boolean
  /** Khử răng cưa trong bộ hậu kỳ */
  multisampling: number
  /** Độ phân giải ảnh phản chiếu của sàn; 0 = sàn thường không phản chiếu */
  reflector: number
  /** Độ phân giải bóng mềm dưới chân máy */
  shadow: number
}

export const QUALITY: Record<QualityLevel, QualitySettings> = {
  high: { dpr: [1, 2], ao: true, bloom: true, multisampling: 4, reflector: 1024, shadow: 512 },
  medium: { dpr: [1, 1.5], ao: false, bloom: true, multisampling: 2, reflector: 512, shadow: 512 },
  low: { dpr: [1, 1], ao: false, bloom: false, multisampling: 0, reflector: 0, shadow: 256 },
}

export const LOWER: Record<QualityLevel, QualityLevel | null> = { high: 'medium', medium: 'low', low: null }

/** Đoán sức máy lúc mở trang: loại GPU, số nhân CPU, RAM, máy cảm ứng màn nhỏ. */
export function detectQuality(): QualityLevel {
  if (typeof window === 'undefined') return 'medium'
  let gpu = ''
  try {
    const gl = document.createElement('canvas').getContext('webgl2') ?? document.createElement('canvas').getContext('webgl')
    if (!gl) return 'low'
    const ext = gl.getExtension('WEBGL_debug_renderer_info')
    gpu = String(ext ? gl.getParameter(ext.UNMASKED_RENDERER_WEBGL) : gl.getParameter(gl.RENDERER))
    gl.getExtension('WEBGL_lose_context')?.loseContext()
  } catch {
    return 'medium'
  }
  // Trình duyệt vẽ bằng CPU (không có GPU dùng được)
  if (/SwiftShader|llvmpipe|Software|Basic Render/i.test(gpu)) return 'low'
  const nav = navigator as Navigator & { deviceMemory?: number }
  const cores = nav.hardwareConcurrency ?? 4
  const mem = nav.deviceMemory ?? 8
  const touchSmall = matchMedia('(pointer: coarse)').matches && Math.min(screen.width, screen.height) < 820
  if (touchSmall || cores <= 2 || mem <= 2) return 'low'
  // GPU tích hợp đời cũ / di động
  if (/Mali|Adreno|PowerVR|Intel.*(HD|UHD) Graphics [0-9]{3}\b|Intel\(R\) HD/i.test(gpu)) return 'medium'
  if (cores <= 4 || mem <= 4) return 'medium'
  return 'high'
}

const KEY = 'pcsc-quality'
export function loadPref(): QualityPref {
  try {
    const v = localStorage.getItem(KEY)
    if (v === 'auto' || v === 'high' || v === 'medium' || v === 'low') return v
  } catch {
    /* trình duyệt chặn lưu trữ: dùng mặc định */
  }
  return 'auto'
}
export function savePref(p: QualityPref) {
  try {
    localStorage.setItem(KEY, p)
  } catch {
    /* bỏ qua */
  }
}
