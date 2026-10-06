import { PARTS } from '../data/parts'
import { useViewer } from '../store'
import { INSPECTABLE } from '../data/hotspots'

/** Nút nổi ở chân khung 3D: chọn một linh kiện thì mời xem cận cảnh linh kiện đó. */
export function InspectButton() {
  const selected = useViewer((s) => s.selected)
  const inspecting = useViewer((s) => s.inspecting)
  const inspect = useViewer((s) => s.inspect)
  if (!selected || inspecting || !INSPECTABLE.includes(selected)) return null
  return (
    <div className="pointer-events-none absolute bottom-5 left-0 z-10 flex w-full justify-center">
      <button
        key={selected}
        onClick={() => inspect(selected)}
        className="swap glass pointer-events-auto flex items-center gap-2 rounded-full bg-white/80 py-2.5 pr-5 pl-4 text-[15px] font-medium text-ink shadow-[0_6px_24px_rgba(0,0,0,0.12)] ring-1 ring-black/5 backdrop-blur-xl transition-transform duration-100 active:scale-[0.97]"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden className="text-accent">
          <circle cx="7" cy="7" r="4.8" fill="none" stroke="currentColor" strokeWidth="1.7" />
          <path d="M10.6 10.6 14 14" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
        </svg>
        Xem cận cảnh {PARTS[selected].name}
      </button>
    </div>
  )
}
