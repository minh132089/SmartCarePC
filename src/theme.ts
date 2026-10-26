/**
 * SmartCarePC dùng giao diện sáng, đồng nhất HTML và cảnh 3D dù máy đang ở chế độ tối.
 * Giữ màu STAGE trùng với token --color-stage và --color-panel trong index.css.
 */
export function useDark() {
  return false
}

/** Màu studio 3D: nền và sương mù, sàn thường, sàn phản chiếu, lưới, bóng đổ, nền khung xem cận cảnh */
export const STAGE = {
  light: { bg: '#fafafb', floor: '#d4d4da', reflector: '#c9c9d0', cell: '#dcdce2', section: '#cfcfd7', shadow: '#1d1d1f', inspector: '#f0f0f3' },
  dark: { bg: '#16171a', floor: '#111214', reflector: '#0e0f11', cell: '#24252a', section: '#2e2f35', shadow: '#000000', inspector: '#18181b' },
}

export function useStage() {
  return STAGE[useDark() ? 'dark' : 'light']
}
