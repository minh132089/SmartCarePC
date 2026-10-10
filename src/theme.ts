import { useSyncExternalStore } from 'react'

/**
 * Chế độ sáng / tối theo cài đặt máy người dùng (prefers-color-scheme). Giao diện HTML đổi màu bằng token CSS
 * trong index.css; cảnh 3D không đọc được CSS nên lấy màu từ STAGE bên dưới, giữ trùng với token --color-stage
 * và --color-panel để khung 3D tan vào nền trang.
 */
const query = typeof window !== 'undefined' ? window.matchMedia('(prefers-color-scheme: dark)') : null

function subscribe(onChange: () => void) {
  query?.addEventListener('change', onChange)
  return () => query?.removeEventListener('change', onChange)
}

export function useDark() {
  return useSyncExternalStore(subscribe, () => query?.matches ?? false, () => false)
}

/** Màu studio 3D: nền và sương mù, sàn thường, sàn phản chiếu, lưới, bóng đổ, nền khung xem cận cảnh */
export const STAGE = {
  light: { bg: '#fafafb', floor: '#d4d4da', reflector: '#c9c9d0', cell: '#dcdce2', section: '#cfcfd7', shadow: '#1d1d1f', inspector: '#f0f0f3' },
  dark: { bg: '#16171a', floor: '#111214', reflector: '#0e0f11', cell: '#24252a', section: '#2e2f35', shadow: '#000000', inspector: '#18181b' },
}

export function useStage() {
  return STAGE[useDark() ? 'dark' : 'light']
}
