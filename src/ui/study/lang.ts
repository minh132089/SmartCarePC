import { create } from 'zustand'
import type { Lang } from './questions'

/**
 * Ngôn ngữ của các form nghiên cứu. Giao diện app bằng tiếng Anh, nhưng người tham gia là người Việt không rành kỹ thuật,
 * nên form cho chọn tiếng Việt. Mặc định theo ngôn ngữ trình duyệt, nhớ lựa chọn trên máy.
 */
const KEY = 'pcsc-form-lang'

function initial(): Lang {
  try {
    const saved = localStorage.getItem(KEY)
    if (saved === 'en' || saved === 'vi') return saved
  } catch { /* bỏ qua */ }
  return typeof navigator !== 'undefined' && navigator.language?.toLowerCase().startsWith('vi') ? 'vi' : 'en'
}

export const useFormLang = create<{ lang: Lang; setLang: (lang: Lang) => void }>((set) => ({
  lang: initial(),
  setLang: (lang) => {
    try { localStorage.setItem(KEY, lang) } catch { /* bỏ qua */ }
    set({ lang })
  },
}))

/** Chọn chữ theo ngôn ngữ form */
export const tr = (lang: Lang, en: string, vi: string) => (lang === 'vi' ? vi : en)
