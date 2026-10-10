import { CaretRight } from '@phosphor-icons/react'
import { useEffect, useRef, type ReactNode } from 'react'
import { BOARDS, getBuild } from '../data/builds'
import { FAMILIES, FAMILY_IDS, type FamilyId } from '../data/families'
import { CREDITS, LICENSE } from '../data/credits'
import { PARTS, type PartId } from '../data/parts'
import { useViewer } from '../store'
import { INSPECTABLE } from '../data/hotspots'
import { QUALITY_LABEL, type QualityPref } from '../perf/quality'
import { firstUndone, guideSteps, loadDone } from '../data/guide'
import { FlowSteps } from './AdvicePage'

const press = 'transition-transform duration-100 ease-out active:scale-[0.97] motion-reduce:active:scale-100'
const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

/** Thông tin hiển thị của máy đang xem: máy Gaming đầy đủ hoặc một trong 6 họ case. */
function useMachine() {
  const { buildId, familyId } = useViewer()
  if (familyId) {
    const f = FAMILIES[familyId]
    const [w, h, d] = f.body
    return {
      key: familyId,
      tagline: f.tagline,
      glass: f.side_open?.type === 'glass' || familyId === 'showcase_dual_chamber',
      rows: [
        ['Kích thước thân', `${w} × ${h} × ${d} mm`],
        ['Bo mạch chủ', { ATX: 'ATX (305 × 244 mm)', mATX: 'Micro-ATX (244 × 244 mm)', ITX: 'Mini-ITX (170 × 170 mm)' }[f.board.ff]],
        ['Tản CPU', COOLER_NAME[f.cooler.kind]],
        ['Card đồ họa', `${f.gpu.kind === 'gpu_280' ? '280 mm, 3 quạt' : '240 mm, 2 quạt'}${f.riser ? ', qua cáp riser' : ''}`],
        ['Nguồn', f.psu.kind === 'atx' ? 'ATX' : 'SFX nhỏ gọn'],
      ] as [string, string][],
    }
  }
  const build = getBuild(buildId)
  return {
    key: buildId as string,
    tagline: build.tagline,
    glass: build.case.glass,
    rows: [
      ['Thùng máy', build.caseName],
      ['Bo mạch chủ', BOARDS[build.formFactor].label],
      ['RAM', `${build.ramSticks} thanh`],
      ['Card đồ họa', build.gpu ? 'Card rời' : 'Có sẵn trong CPU'],
    ] as [string, string][],
  }
}

const COOLER_NAME = { tower_air_155: 'Tản khí dạng tháp', low_profile_47: 'Tản khí thấp', aio_240: 'Tản nước 240 mm', aio_360: 'Tản nước 360 mm' }

/** Số thứ tự ảnh concept trong public/families (giữ theo bản vẽ gốc của người dùng). */
const THUMB: Record<FamilyId, string> = { standard_tower: '01', compact_tower: '02', showcase_dual_chamber: '03', mini_sff: '04', full_tower: '06' }

/** Các máy người dùng chọn được: 6 họ case theo dáng và máy Gaming đã dựng đầy đủ. */
const MACHINES: { id: FamilyId | 'gaming'; label: string; img: string }[] = [
  ...FAMILY_IDS.map((id) => ({ id, label: FAMILIES[id].label, img: `/families/${THUMB[id]}_${id}.jpg` })),
  { id: 'gaming', label: 'Gaming (đầy đủ)', img: '' },
]

export function Sidebar() {
  const { familyId, setFamily, setBuild, exploded, toggleExploded, sideOpen, toggleSide, selected, select, inspect } = useViewer()
  const machine = useMachine()
  // máy slim không có quạt vỏ nên bỏ dòng "Quạt"
  const parts: PartId[] = ['case', 'motherboard', 'cpu', 'cooler', 'ram', 'gpu', 'ssd', 'psu', 'cables', 'fans', 'tools'].filter(
    (p) => p !== 'fans' || !familyId || FAMILIES[familyId].fans.length > 0,
  ) as PartId[]
  const current = familyId ?? 'gaming'
  const listRef = useRef<HTMLDivElement>(null)

  // Chọn linh kiện bằng cách bấm vào hình 3D: cuộn danh sách tới đúng dòng đó
  useEffect(() => {
    if (!selected) return
    const row = listRef.current?.querySelector<HTMLElement>(`[data-part="${selected}"]`)
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    row?.scrollIntoView({ block: 'nearest', behavior: reduce ? 'auto' : 'smooth' })
  }, [selected])

  return (
    <aside className="h-[46%] w-full shrink-0 overflow-y-auto px-5 pt-6 pb-8 md:h-full md:w-[400px] md:px-7 md:pt-10">
      <div className="enter" style={{ '--i': 0 } as React.CSSProperties}>
        <FlowSteps current={2} />
        <p className="mt-6 text-[13px] font-semibold tracking-[0.01em] text-ink-2">PC SmartCare</p>
        <h1 className="display mt-1">Nhìn vào bên trong chiếc máy tính.</h1>
        <p className="mt-3 text-[15px] leading-[1.47] text-ink-2">Chọn kiểu máy bạn thích, rồi bấm vào từng linh kiện để biết nó làm gì.</p>
      </div>

      <div className="enter" style={{ '--i': 1 } as React.CSSProperties}>
      <SectionTitle>Chọn kiểu máy</SectionTitle>
      <div role="radiogroup" aria-label="Kiểu máy" className="grid grid-cols-3 gap-2">
        {MACHINES.map((m) => {
          const on = m.id === current
          return (
            <button
              key={m.id}
              role="radio"
              aria-checked={on}
              onClick={() => (m.id === 'gaming' ? setBuild('gaming') : setFamily(m.id))}
              className={`flex flex-col items-center gap-1 rounded-xl bg-surface p-1.5 pb-2 text-center ring-1 transition-[box-shadow,color] duration-150 ${press} ${focus} ${
                on ? 'text-ink ring-2 ring-accent' : 'text-ink-2 ring-line hover:text-ink'
              }`}
            >
              {m.img ? (
                <img src={m.img} alt="" className="aspect-square w-full rounded-lg bg-canvas object-contain" loading="lazy" />
              ) : (
                <span className="grid aspect-square w-full place-items-center rounded-lg bg-[#16181d] text-[11px] font-semibold text-[#22d3ee]">GAMING</span>
              )}
              <span className={`text-[12px] leading-tight ${on ? 'font-semibold' : 'font-medium'}`}>{m.label}</span>
            </button>
          )
        })}
      </div>
      {/* key đổi theo máy: khối này được tạo lại và mờ dần vào */}
      <div key={machine.key} className="swap">
        <p className="mt-4 text-[15px] leading-[1.47] text-ink">{machine.tagline}</p>
        <Group className="mt-4">
          {machine.rows.map(([label, value], i) => (
            <Row key={label} label={label} value={value} last={i === machine.rows.length - 1} />
          ))}
        </Group>
      </div>
      </div>

      <div className="enter mt-5 flex flex-wrap gap-2.5" style={{ '--i': 2 } as React.CSSProperties}>
        <button
          onClick={toggleExploded}
          className={`rounded-full bg-accent px-5 py-2.5 text-[15px] font-medium text-white hover:bg-accent-hover ${press} ${focus}`}
        >
          {exploded ? 'Lắp lại' : 'Tách rời linh kiện'}
        </button>
        <button
          onClick={toggleSide}
          className={`rounded-full bg-fill px-5 py-2.5 text-[15px] font-medium text-ink hover:bg-fill-hover ${press} ${focus}`}
        >
          {machine.glass ? (sideOpen ? 'Lắp kính hông' : 'Tháo kính hông') : sideOpen ? 'Đóng nắp hông' : 'Mở nắp hông'}
        </button>
        <PowerButton />
      </div>
      <QualityPicker />
      <GuideCard />

      <div ref={listRef} className="enter" style={{ '--i': 3 } as React.CSSProperties}>
      <SectionTitle>Linh kiện trong máy</SectionTitle>
      <Group>
        {parts.map((id, i) => {
          const open = selected === id
          return (
            <div key={id} data-part={id} className={i < parts.length - 1 ? 'border-b border-line' : ''}>
              <button
                onClick={() => select(open ? null : id)}
                aria-expanded={open}
                className={`flex w-full items-center justify-between px-4 py-3 text-left text-[15px] transition-colors duration-150 ease-[ease] active:bg-line ${focus} ${
                  open ? 'text-link' : 'text-ink'
                }`}
              >
                <span className={open ? 'font-medium' : ''}>{PARTS[id].name}</span>
                <Chevron open={open} />
              </button>
              <div className="reveal" data-open={open}>
                <div>
                  <p className="px-4 pb-3 text-[14px] leading-[1.45] text-ink-2">{PARTS[id].about}</p>
                  {INSPECTABLE.includes(id) && (
                    <button
                      onClick={() => inspect(id)}
                      tabIndex={open ? 0 : -1}
                      className={`mx-4 mb-3.5 text-[14px] font-medium text-link hover:underline ${focus}`}
                    >
                      Xem cận cảnh ›
                    </button>
                  )}
                </div>
              </div>
            </div>
          )
        })}
      </Group>
      </div>

      <p className="mt-5 text-[12px] leading-[1.45] text-ink-3">
        Kéo để xoay, cuộn để phóng to. Bấm vào một linh kiện trong hình để xem nó là gì.
      </p>

      <details className="mt-3 text-[12px] leading-[1.45] text-ink-3">
        <summary className="cursor-pointer select-none hover:text-ink-2">Nguồn model 3D</summary>
        <ul className="mt-2 space-y-1">
          {CREDITS.map((c) => (
            <li key={c.url}>
              {c.part}: “
              <a href={c.url} target="_blank" rel="noreferrer" className="text-link hover:underline">
                {c.title}
              </a>
              ” của {c.author}
            </li>
          ))}
        </ul>
        <p className="mt-2">
          Dùng theo giấy phép{' '}
          <a href={LICENSE.url} target="_blank" rel="noreferrer" className="text-link hover:underline">
            {LICENSE.label}
          </a>
          . Đã chỉnh sửa: xoá logo, đổi hướng, thu nhỏ.
        </p>
      </details>
    </aside>
  )
}

function SectionTitle({ children }: { children: ReactNode }) {
  return <h2 className="mt-8 mb-2.5 px-1 text-[13px] font-semibold text-ink-2">{children}</h2>
}

/** Danh sách nhóm kiểu app Cài đặt: thẻ trắng bo góc, các dòng ngăn bằng vạch mảnh. */
function Group({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`overflow-hidden rounded-2xl bg-surface ${className}`}>{children}</div>
}

function Row({ label, value, last }: { label: string; value: string; last?: boolean }) {
  return (
    <div className={`flex items-baseline justify-between gap-4 px-4 py-3 text-[15px] ${last ? '' : 'border-b border-line'}`}>
      <span className="text-ink">{label}</span>
      <span className="text-right text-ink-2">{value}</span>
    </div>
  )
}

function Chevron({ open }: { open: boolean }) {
  return (
    <CaretRight
      size={13}
      aria-hidden
      className={`shrink-0 transition-transform duration-200 ease-out motion-reduce:transition-none ${open ? 'rotate-90 text-link' : 'text-ink-3'}`}
    />
  )
}

/** Bật máy: quạt quay, đèn chạy, cảnh vẽ liên tục. Mặc định tắt để máy người dùng không nóng khi chỉ xem. */
function PowerButton() {
  const powered = useViewer((s) => s.powered)
  const togglePower = useViewer((s) => s.togglePower)
  return (
    <button
      onClick={togglePower}
      aria-pressed={powered}
      className={`flex items-center gap-2 rounded-full px-5 py-2.5 text-[15px] font-medium ${press} ${focus} ${
        powered ? 'bg-ink text-on-ink' : 'bg-fill text-ink hover:bg-fill-hover'
      }`}
    >
      <span className={`size-2 rounded-full ${powered ? 'bg-ok-strong' : 'bg-ink-3'}`} aria-hidden />
      {powered ? 'Tắt máy' : 'Bật máy'}
    </button>
  )
}

/** Lối vào hướng dẫn lắp từng bước, kèm tiến độ đã đánh dấu của máy đang xem */
function GuideCard() {
  const familyId = useViewer((s) => s.familyId)
  const buildId = useViewer((s) => s.buildId)
  const setGuide = useViewer((s) => s.setGuide)
  const setGuideIndex = useViewer((s) => s.setGuideIndex)
  const steps = guideSteps(familyId, buildId)
  const done = loadDone(familyId ?? buildId)
  const count = steps.filter((s) => done.has(s.id)).length
  return (
    <div className="enter mt-6 rounded-2xl bg-surface p-4" style={{ '--i': 3 } as React.CSSProperties}>
      <p className="text-[15px] font-semibold text-ink">Tự lắp máy này?</p>
      <p className="mt-1 text-[14px] leading-[1.45] text-ink-2">
        Làm theo hướng dẫn từng bước, có hình minh hoạ. Lắp xong bước nào thì đánh dấu bước đó.
      </p>
      {count > 0 && (
        <div className="mt-3">
          <div className="h-1.5 overflow-hidden rounded-full bg-fill">
            <div className="h-full rounded-full bg-ok-strong" style={{ width: `${(count / steps.length) * 100}%` }} />
          </div>
          <p className="mt-1 text-[12px] text-ink-3">
            Đã xong {count}/{steps.length} bước
          </p>
        </div>
      )}
      <button
        onClick={() => {
          setGuideIndex(firstUndone(steps, done))
          setGuide(true)
        }}
        className={`mt-3 rounded-full bg-accent px-5 py-2.5 text-[15px] font-medium text-white hover:bg-accent-hover ${press} ${focus}`}
      >
        {count === 0 ? 'Bắt đầu hướng dẫn' : count === steps.length ? 'Xem lại hướng dẫn' : 'Tiếp tục hướng dẫn'}
      </button>
    </div>
  )
}

const PREFS: QualityPref[] = ['auto', 'high', 'medium', 'low']

/** Chọn chất lượng hình. "Tự động" đoán theo sức máy và tự hạ khi thấy giật. */
function QualityPicker() {
  const pref = useViewer((s) => s.qualityPref)
  const level = useViewer((s) => s.quality)
  const setPref = useViewer((s) => s.setQualityPref)
  return (
    <div className="enter mt-4" style={{ '--i': 2 } as React.CSSProperties}>
      <div className="flex items-baseline justify-between px-1">
        <span className="text-[13px] font-semibold text-ink-2">Chất lượng hình</span>
        {pref === 'auto' && <span className="text-[12px] text-ink-3">đang dùng: {QUALITY_LABEL[level]}</span>}
      </div>
      <div role="radiogroup" aria-label="Chất lượng hình" className="mt-1.5 grid grid-cols-4 gap-1 rounded-full bg-fill p-1">
        {PREFS.map((p) => (
          <button
            key={p}
            role="radio"
            aria-checked={pref === p}
            onClick={() => setPref(p)}
            className={`rounded-full py-1.5 text-[13px] font-medium transition-colors duration-150 ${focus} ${
              pref === p ? 'bg-surface text-ink shadow-[0_1px_4px_rgba(0,0,0,0.12)]' : 'text-ink-2 hover:text-ink'
            }`}
          >
            {p === 'auto' ? 'Tự động' : QUALITY_LABEL[p]}
          </button>
        ))}
      </div>
      <p className="mt-1.5 px-1 text-[12px] leading-[1.4] text-ink-3">Máy chậm hoặc nóng thì chọn Nhẹ. Quạt chỉ quay khi bấm Bật máy.</p>
    </div>
  )
}
