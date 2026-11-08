import { useState, useMemo } from 'react'
import {
  CheckCircle,
  WarningCircle,
  Desktop,
  Sparkle,
} from '@phosphor-icons/react'
import { useViewer } from '../store'
import type { PartId } from '../data/parts'
import {
  UPGRADE_CATEGORIES,
  SYSTEM_PRESETS,
  UPGRADE_OPTIONS,
  checkUpgradeCompatibility,
  type SystemPreset,
  type UpgradeTargetOption,
} from '../data/upgrades'
import { SidebarAdvisor } from './SidebarAdvisor'

const press = 'transition-transform duration-100 ease-out active:scale-[0.97] motion-reduce:active:scale-100'
const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

export function SidebarUpgrade() {
  const familyId = useViewer((s) => s.familyId)
  const selectPart = useViewer((s) => s.select)
  const inspectPart = useViewer((s) => s.inspect)

  // 3D component states
  const ramSticks = useViewer((s) => s.ramSticks)
  const setRamSticks = useViewer((s) => s.setRamSticks)
  const ssdCount = useViewer((s) => s.ssdCount)
  const setSsdCount = useViewer((s) => s.setSsdCount)
  const customGpu = useViewer((s) => s.customGpu)
  const setCustomGpu = useViewer((s) => s.setCustomGpu)

  // Sub-sections inside Upgrade tab
  const [viewMode, setViewMode] = useState<'check' | 'advisor' | 'power'>('check')

  // Selected upgrade target
  const [selectedOptionId, setSelectedOptionId] = useState<string>('ram_add_8gb_ddr4')

  // Calculator states
  const [cpuWatts, setCpuWatts] = useState<number>(65)
  const [gpuWatts, setGpuWatts] = useState<number>(160)

  // Derive current specs based on the 3D model being viewed
  const currentSpecs: SystemPreset['specs'] = useMemo(() => {
    if (familyId === 'mini_sff') {
      const p = SYSTEM_PRESETS.find((x) => x.id === 'mini_itx')!
      return p.specs
    }
    if (familyId === 'compact_tower') {
      const p = SYSTEM_PRESETS.find((x) => x.id === 'compact_matx')!
      return p.specs
    }
    if (familyId === 'showcase_dual_chamber') {
      const p = SYSTEM_PRESETS.find((x) => x.id === 'showcase_beast')!
      return p.specs
    }
    // Default gaming or standard tower
    return SYSTEM_PRESETS[0].specs
  }, [familyId])

  const currentOption = UPGRADE_OPTIONS.find((o) => o.id === selectedOptionId) ?? UPGRADE_OPTIONS[0]

  const compatResult = checkUpgradeCompatibility(currentSpecs, currentOption)

  const viewPartIn3D = (partId: PartId) => {
    selectPart(partId)
    inspectPart(partId)
  }

  // Tự động cập nhật mô hình 3D khi chọn gói nâng cấp
  const handleSelectUpgradeOption = (opt: UpgradeTargetOption) => {
    setSelectedOptionId(opt.id)
    if (opt.id === 'ram_add_8gb_ddr4') {
      setRamSticks(2)
      selectPart('ram')
    } else if (opt.id === 'ram_up_32gb_ddr4' || opt.id === 'ram_up_32gb_ddr5') {
      setRamSticks(4)
      selectPart('ram')
    } else if (opt.id === 'ssd_add_1tb_gen4' || opt.id === 'ssd_add_2tb_gen4') {
      setSsdCount(2)
      selectPart('ssd')
    } else if (opt.id === 'gpu_rtx4060_2fan' || opt.id === 'gpu_rtx4070_mini_2fan') {
      setCustomGpu('gpu_240')
      selectPart('gpu')
    } else if (opt.id === 'gpu_rtx4070_3fan') {
      setCustomGpu('gpu_280')
      selectPart('gpu')
    }
  }

  // Calculated power
  const totalBaseCalc = cpuWatts + gpuWatts + 100
  const recommendedPsu = Math.ceil((totalBaseCalc * 1.3) / 50) * 50

  return (
    <div className="space-y-4 pt-2">
      {/* Sub Mode Selector */}
      <div className="grid grid-cols-3 gap-1 rounded-full bg-fill p-1 text-[12px]">
        <button
          onClick={() => setViewMode('check')}
          className={`rounded-full py-1.5 font-medium transition-all ${
            viewMode === 'check' ? 'bg-surface text-ink shadow-sm font-semibold' : 'text-ink-2 hover:text-ink'
          }`}
        >
          Tương thích
        </button>
        <button
          onClick={() => setViewMode('advisor')}
          className={`rounded-full py-1.5 font-medium transition-all ${
            viewMode === 'advisor' ? 'bg-surface text-ink shadow-sm font-semibold' : 'text-ink-2 hover:text-ink'
          }`}
        >
          Tư vấn AI
        </button>
        <button
          onClick={() => setViewMode('power')}
          className={`rounded-full py-1.5 font-medium transition-all ${
            viewMode === 'power' ? 'bg-surface text-ink shadow-sm font-semibold' : 'text-ink-2 hover:text-ink'
          }`}
        >
          Tính nguồn
        </button>
      </div>

      {/* SUB-VIEW 1: CHECK TƯƠNG THÍCH */}
      {viewMode === 'check' && (
        <div className="space-y-3.5">
          {/* Thông tin máy đang xem */}
          <div className="rounded-2xl border border-line bg-surface p-3.5 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-semibold text-accent uppercase tracking-wide">Máy đang xem trên 3D</span>
              <span className="text-[11px] text-ink-3">Vỏ: max {currentSpecs.caseMaxGpuMm} mm</span>
            </div>
            <p className="mt-1 text-[13px] font-bold text-ink">{currentSpecs.caseName}</p>
            <p className="text-[12px] text-ink-2">
              Nguồn: {currentSpecs.psuWatt}W {currentSpecs.psuType} • Bo mạch: {currentSpecs.motherboard} ({currentSpecs.ramDdr})
            </p>
          </div>

          {/* BỘ ĐIỀU KHIỂN THAY ĐỔI 3D TRỰC TIẾP */}
          <div className="rounded-2xl border border-accent/30 bg-accent/5 p-3.5 space-y-3">
            <div className="flex items-center justify-between">
              <span className="flex items-center gap-1.5 text-[12px] font-bold text-accent">
                <Sparkle size={14} weight="fill" />
                Thao tác thay đổi trực tiếp trên 3D
              </span>
              <span className="text-[10px] text-ink-3">Cập nhật thời gian thực</span>
            </div>

            {/* RAM 3D Switcher */}
            <div>
              <div className="flex items-center justify-between text-[11px] mb-1">
                <span className="font-semibold text-ink">Số thanh RAM trên bo mạch:</span>
                <span className="font-bold text-accent">{ramSticks} thanh</span>
              </div>
              <div className="grid grid-cols-3 gap-1 text-[11px]">
                {[
                  { n: 1, label: '1 Thanh (8G)' },
                  { n: 2, label: '2 Thanh (Dual)' },
                  { n: 4, label: '4 Thanh (Full)' },
                ].map((item) => (
                  <button
                    key={item.n}
                    onClick={() => {
                      setRamSticks(item.n)
                      selectPart('ram')
                    }}
                    className={`rounded-lg py-1.5 font-medium transition-all ${press} ${focus} ${
                      ramSticks === item.n
                        ? 'bg-accent text-white font-semibold shadow-sm'
                        : 'bg-surface text-ink-2 hover:text-ink'
                    }`}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>

            {/* SSD 3D Switcher */}
            <div>
              <div className="flex items-center justify-between text-[11px] mb-1">
                <span className="font-semibold text-ink">Số ổ SSD M.2 trên bo mạch:</span>
                <span className="font-bold text-accent">{ssdCount} ổ</span>
              </div>
              <div className="grid grid-cols-3 gap-1 text-[11px]">
                {[
                  { n: 0, label: '0 Ổ (Tháo rời)' },
                  { n: 1, label: '1 Ổ (Mặc định)' },
                  { n: 2, label: '2 Ổ (Lắp thêm)' },
                ].map((item) => (
                  <button
                    key={item.n}
                    onClick={() => {
                      setSsdCount(item.n)
                      selectPart('ssd')
                    }}
                    className={`rounded-lg py-1.5 font-medium transition-all ${press} ${focus} ${
                      ssdCount === item.n
                        ? 'bg-accent text-white font-semibold shadow-sm'
                        : 'bg-surface text-ink-2 hover:text-ink'
                    }`}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>

            {/* GPU 3D Switcher */}
            <div>
              <div className="flex items-center justify-between text-[11px] mb-1">
                <span className="font-semibold text-ink">Card đồ họa (GPU) trong máy:</span>
                <span className="font-bold text-accent">
                  {customGpu === 'gpu_240' ? '2 quạt (240mm)' : '3 quạt (280mm)'}
                </span>
              </div>
              <div className="grid grid-cols-2 gap-1 text-[11px]">
                <button
                  onClick={() => {
                    setCustomGpu('gpu_240')
                    selectPart('gpu')
                  }}
                  className={`rounded-lg py-1.5 font-medium transition-all ${press} ${focus} ${
                    customGpu === 'gpu_240'
                      ? 'bg-accent text-white font-semibold shadow-sm'
                      : 'bg-surface text-ink-2 hover:text-ink'
                  }`}
                >
                  2 Quạt (240 mm)
                </button>
                <button
                  onClick={() => {
                    setCustomGpu('gpu_280')
                    selectPart('gpu')
                  }}
                  className={`rounded-lg py-1.5 font-medium transition-all ${press} ${focus} ${
                    customGpu !== 'gpu_240'
                      ? 'bg-accent text-white font-semibold shadow-sm'
                      : 'bg-surface text-ink-2 hover:text-ink'
                  }`}
                >
                  3 Quạt (280 mm)
                </button>
              </div>
            </div>
          </div>

          {/* Chọn linh kiện nâng cấp */}
          <div>
            <label className="text-[12px] font-semibold text-ink-2">Gói nâng cấp đề xuất (bấm để xem mô phỏng 3D):</label>
            <div className="mt-1.5 space-y-1.5">
              {UPGRADE_OPTIONS.map((opt) => {
                const active = opt.id === selectedOptionId
                return (
                  <button
                    key={opt.id}
                    onClick={() => handleSelectUpgradeOption(opt)}
                    className={`flex w-full items-start justify-between rounded-xl border p-2.5 text-left transition-all ${press} ${focus} ${
                      active
                        ? 'border-accent bg-accent/5 ring-1.5 ring-accent'
                        : 'border-line bg-surface hover:border-line-strong'
                    }`}
                  >
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="rounded bg-fill px-1.5 py-0.5 text-[10px] font-bold uppercase text-ink-2">
                          {opt.category}
                        </span>
                        <h4 className="text-[13px] font-semibold text-ink">{opt.name}</h4>
                      </div>
                      <p className="mt-0.5 text-[11px] text-ink-2">{opt.impact}</p>
                    </div>
                    <span className="shrink-0 text-[11px] font-bold text-ink-2">{opt.costEstimate}</span>
                  </button>
                )
              })}
            </div>
          </div>

          {/* Kết quả kiểm tra tương thích */}
          <div
            className={`rounded-2xl border p-3.5 shadow-sm transition-all ${
              compatResult.status === 'ok'
                ? 'border-ok-strong/30 bg-ok-bg/30'
                : compatResult.status === 'warning'
                  ? 'border-warn-line bg-warn-bg/40'
                  : 'border-bad/30 bg-bad-bg/40'
            }`}
          >
            <div className="flex items-start gap-2.5">
              <div
                className={`mt-0.5 grid size-6 shrink-0 place-items-center rounded-full ${
                  compatResult.status === 'ok'
                    ? 'bg-ok-strong text-white'
                    : compatResult.status === 'warning'
                      ? 'bg-warn text-white'
                      : 'bg-bad text-white'
                }`}
              >
                {compatResult.status === 'ok' ? (
                  <CheckCircle size={15} weight="bold" />
                ) : (
                  <WarningCircle size={15} weight="bold" />
                )}
              </div>
              <div>
                <h4
                  className={`text-[13px] font-bold ${
                    compatResult.status === 'ok'
                      ? 'text-ok'
                      : compatResult.status === 'warning'
                        ? 'text-warn'
                        : 'text-bad'
                  }`}
                >
                  {compatResult.headline}
                </h4>
                <p className="mt-1 text-[12px] leading-relaxed text-ink-2">{compatResult.advice}</p>
              </div>
            </div>

            {/* Chi tiết từng điều kiện */}
            <ul className="mt-2.5 space-y-1 rounded-xl bg-surface/80 p-2.5 text-[11px] text-ink backdrop-blur-sm">
              {compatResult.details.map((d, i) => (
                <li key={i} className="flex items-start gap-1.5 leading-snug">
                  <span className="shrink-0 text-ink-3">•</span>
                  <span>{d}</span>
                </li>
              ))}
            </ul>

            {/* Nút soi 3D */}
            <div className="mt-3 flex items-center justify-between border-t border-line/60 pt-2">
              <button
                onClick={() => {
                  const catObj = UPGRADE_CATEGORIES.find((c) => c.id === currentOption.category)
                  if (catObj) viewPartIn3D(catObj.partId)
                }}
                className={`flex items-center gap-1 text-[12px] font-semibold text-accent hover:underline ${focus}`}
              >
                <Desktop size={14} />
                Xem cận cảnh linh kiện trên 3D ›
              </button>
              <button
                onClick={() => {
                  setViewMode('advisor')
                }}
                className="text-[12px] font-medium text-ink-2 hover:text-ink"
              >
                Nhờ AI tư vấn chi tiết ›
              </button>
            </div>
          </div>
        </div>
      )}

      {/* SUB-VIEW 2: TƯ VẤN AI */}
      {viewMode === 'advisor' && (
        <div className="space-y-3.5">
          <SidebarAdvisor />
        </div>
      )}

      {/* SUB-VIEW 3: TÍNH CÔNG SUẤT NGUỒN */}
      {viewMode === 'power' && (
        <div className="space-y-3.5 rounded-2xl border border-line bg-surface p-4 shadow-sm">
          <div>
            <h3 className="text-[14px] font-bold text-ink">Ước tính công suất nguồn (PSU)</h3>
            <p className="mt-0.5 text-[11px] text-ink-2">
              Kéo chỉnh để tính công suất cần thiết khi bạn thay CPU hoặc Card đồ họa.
            </p>
          </div>

          <div className="space-y-2.5">
            <div>
              <div className="flex justify-between text-[12px]">
                <span className="font-medium text-ink">CPU TDP (Watt):</span>
                <span className="font-bold text-accent">{cpuWatts} W</span>
              </div>
              <input
                type="range"
                min={35}
                max={250}
                step={5}
                value={cpuWatts}
                onChange={(e) => setCpuWatts(Number(e.target.value))}
                className="w-full accent-accent"
              />
            </div>

            <div>
              <div className="flex justify-between text-[12px]">
                <span className="font-medium text-ink">Card đồ họa GPU (Watt):</span>
                <span className="font-bold text-accent">{gpuWatts} W</span>
              </div>
              <input
                type="range"
                min={0}
                max={450}
                step={10}
                value={gpuWatts}
                onChange={(e) => setGpuWatts(Number(e.target.value))}
                className="w-full accent-accent"
              />
            </div>
          </div>

          <div className="rounded-xl border border-accent/20 bg-accent/5 p-3 text-center">
            <p className="text-[11px] text-ink-2">Tổng tiêu thụ ước tính (kèm 100W bo mạch & quạt):</p>
            <p className="text-[20px] font-black text-ink">{totalBaseCalc} W</p>
            <p className="mt-1 text-[11px] font-semibold text-accent">Nguồn khuyến nghị nên dùng:</p>
            <p className="text-[26px] font-black text-accent">{recommendedPsu} W</p>
            <p className="text-[10px] text-ink-3">(Đã cộng 30% khoảng đệm chống sụt áp)</p>
          </div>

          <button
            onClick={() => viewPartIn3D('psu')}
            className={`w-full rounded-full bg-accent py-2 text-center text-[12px] font-semibold text-white hover:bg-accent-hover ${press} ${focus}`}
          >
            Xem vị trí nguồn trên 3D ›
          </button>
        </div>
      )}
    </div>
  )
}
