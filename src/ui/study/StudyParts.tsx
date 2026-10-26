import { useEffect, useId, useRef, type ReactNode } from 'react'
import { X } from '@phosphor-icons/react'
import { IconButton } from '../chat/ChatParts'

export const press = 'transition-transform duration-100 ease-out active:scale-[0.97] motion-reduce:active:scale-100'
export const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

/** Hộp thoại giữa màn hình. Esc hoặc bấm ra ngoài để đóng; đóng xong trả focus về nút đã mở nó. */
export function Dialog({ title, onClose, children, footer }: { title: string; onClose: () => void; children: ReactNode; footer: ReactNode }) {
  const box = useRef<HTMLDivElement>(null)
  const titleId = useId()
  useEffect(() => {
    const back = document.activeElement as HTMLElement | null
    // Có ô nhập một dòng (ô mã) thì đặt con trỏ vào đó, không thì focus chính hộp thoại. Không nhảy vào ô góp ý
    // ở cuối form, vì như vậy form bị cuộn xuống giữa chừng.
    const field = box.current?.querySelector<HTMLElement>('input:not([type]), input[type=text]')
    const target = field ?? box.current
    target?.focus()
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => {
      window.removeEventListener('keydown', onKey)
      back?.focus()
    }
  }, [onClose])

  return (
    <div className="fixed inset-0 z-[60] grid place-items-center bg-black/30 p-3" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div
        ref={box}
        role="dialog"
        aria-modal="true"
        tabIndex={-1}
        aria-labelledby={titleId}
        className="chat-pop flex max-h-[min(720px,100%)] outline-none w-full max-w-[560px] flex-col overflow-hidden rounded-[24px] bg-panel shadow-[0_18px_60px_rgba(0,0,0,0.22)] ring-1 ring-line"
      >
        <header className="flex items-center gap-3 border-b border-line px-5 py-3.5">
          <h2 id={titleId} className="min-w-0 flex-1 text-[17px] font-semibold text-ink">
            {title}
          </h2>
          <IconButton label="Close" onClick={onClose} icon={<X size={16} aria-hidden />} />
        </header>
        <div className="min-h-0 flex-1 overflow-y-auto px-5 py-4">{children}</div>
        <footer className="flex flex-wrap items-center justify-end gap-2 border-t border-line px-5 py-3">{footer}</footer>
      </div>
    </div>
  )
}

/** Một câu hỏi: tiêu đề in đậm, chú thích nhỏ, rồi chỗ trả lời */
export function Question({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <fieldset className="mt-5 first:mt-0">
      <legend className="text-[14.5px] leading-snug font-semibold text-ink">{label}</legend>
      {hint && <p className="mt-0.5 text-[12.5px] text-ink-3">{hint}</p>}
      <div className="mt-2">{children}</div>
    </fieldset>
  )
}

/** Chọn một (radio) hoặc nhiều (checkbox) đáp án, hiện dạng viên thuốc */
export function Choices<T extends string>({
  options,
  value,
  onChange,
  multi = false,
  name,
}: {
  options: { value: T; label: string }[]
  value: T[]
  onChange: (v: T[]) => void
  multi?: boolean
  name: string
}) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {options.map((o) => {
        const on = value.includes(o.value)
        return (
          <label key={o.value} className="relative cursor-pointer">
            <input
              type={multi ? 'checkbox' : 'radio'}
              name={name}
              checked={on}
              onChange={() => onChange(multi ? (on ? value.filter((v) => v !== o.value) : [...value, o.value]) : [o.value])}
              aria-label={o.label}
              className="peer absolute inset-0 cursor-pointer opacity-0"
            />
            <span
              className={`block rounded-full px-3 py-1.5 text-[13.5px] leading-snug ring-1 transition-colors duration-150 peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-accent ${
                on ? 'bg-ink font-medium text-on-ink ring-ink' : 'bg-surface text-ink ring-line-strong hover:bg-fill'
              }`}
            >
              {o.label}
            </span>
          </label>
        )
      })}
    </div>
  )
}

/** Thang 1 đến 5 có chữ ở hai đầu, giống thang trong bảng khảo sát để số liệu so được với nhau */
export function Scale({ value, onChange, low, high, name }: { value: number | null; onChange: (v: number) => void; low: string; high: string; name: string }) {
  return (
    <div>
      <div className="flex gap-1.5" role="radiogroup">
        {[1, 2, 3, 4, 5].map((n) => (
          <label key={n} className="relative flex-1 cursor-pointer">
            <input
              type="radio"
              name={name}
              checked={value === n}
              onChange={() => onChange(n)}
              aria-label={n === 1 ? `1, ${low}` : n === 5 ? `5, ${high}` : String(n)}
              className="peer absolute inset-0 cursor-pointer opacity-0"
            />
            <span
              className={`grid h-9 place-items-center rounded-xl text-[14px] font-semibold tabular-nums ring-1 transition-colors duration-150 peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-accent ${
                value === n ? 'bg-ink text-on-ink ring-ink' : 'bg-surface text-ink ring-line-strong hover:bg-fill'
              }`}
            >
              {n}
            </span>
          </label>
        ))}
      </div>
      <div className="mt-1 flex justify-between text-[11.5px] text-ink-3">
        <span>{low}</span>
        <span>{high}</span>
      </div>
    </div>
  )
}

export function Note({ value, onChange, placeholder }: { value: string; onChange: (v: string) => void; placeholder: string }) {
  return (
    <textarea
      value={value}
      onChange={(e) => onChange(e.target.value.slice(0, 2000))}
      rows={3}
      placeholder={placeholder}
      className={`w-full resize-none rounded-xl bg-surface px-3 py-2 text-[14px] leading-[1.45] text-ink ring-1 ring-line-strong placeholder:text-ink-3 ${focus}`}
    />
  )
}

export function PrimaryButton({ children, onClick, disabled }: { children: ReactNode; onClick: () => void; disabled?: boolean }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`rounded-full bg-accent px-4 py-2 text-[14px] font-medium text-white hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-40 ${press} ${focus}`}
    >
      {children}
    </button>
  )
}

export function QuietButton({ children, onClick }: { children: ReactNode; onClick: () => void }) {
  return (
    <button onClick={onClick} className={`rounded-full bg-fill px-4 py-2 text-[14px] font-medium text-ink hover:bg-fill-hover ${press} ${focus}`}>
      {children}
    </button>
  )
}
