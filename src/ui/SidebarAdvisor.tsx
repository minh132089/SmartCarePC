/**
 * SidebarAdvisor — Chuyên viên tư vấn AI nâng cấp PC
 *
 * Flow:
 * 1. Người dùng chọn: linh kiện muốn nâng cấp + mục đích + ngân sách
 * 2. Frontend tạo 2 phương án tư vấn (Tiết kiệm / Tối ưu) dựa trên rule-based logic
 * 3. Gọi /api/search-deals để lấy link mua hàng thực từ Bing/DDG (Vite middleware)
 * 4. Hiển thị 2 card kết quả, mỗi card có nút [Xem trên 3D →] cập nhật scene
 */
import { useState } from 'react'
import { Sparkle, ArrowRight, ShoppingBag, Robot, CurrencyCircleDollar, Desktop, ArrowsClockwise } from '@phosphor-icons/react'
import { useViewer } from '../store'

// ─── Types ─────────────────────────────────────────────────────────────────

type Component = 'ram' | 'ssd' | 'gpu'
type Purpose = 'render_game' | 'gaming' | 'office' | 'video'

interface AdvisorInput {
  component: Component
  purpose: Purpose
  budget: number // VND
  currentRam: number // GB
  currentSsd: number  // số ổ
}

interface Plan {
  id: 'budget' | 'optimal'
  label: string
  color: 'teal' | 'violet'
  headline: string
  description: string
  whatToBuy: string
  estimatedPrice: string
  whyThisWorks: string
  tags: string[]
  scene3D: {
    ram?: number
    ssd?: number
    gpu?: 'gpu_240' | 'gpu_280'
  }
  searchQuery: string
}

interface ShopResult {
  url: string
  title: string
  snippet: string
}

type AdvisorState = 'idle' | 'thinking' | 'done' | 'error'

// ─── Rule-based plan generator ────────────────────────────────────────────

function generatePlans(input: AdvisorInput): Plan[] {
  const { component, purpose, budget, currentRam, currentSsd } = input

  if (component === 'ram') {
    const needDoubleRam = purpose === 'render_game' || purpose === 'video'
    const targetRam = needDoubleRam ? currentRam * 2 : Math.max(currentRam + 8, 16)

    // Plan A — Tiết kiệm: thêm thanh vào slot trống
    const planA: Plan = {
      id: 'budget',
      label: 'Phương án 1 · Tiết kiệm',
      color: 'teal',
      headline: `Thêm ${currentRam}GB vào slot trống`,
      description: `Giữ nguyên ${currentRam}GB RAM đang có, chỉ mua thêm ${currentRam}GB để lắp vào slot còn trống. Tiết kiệm hơn vì không cần bỏ RAM cũ.`,
      whatToBuy: `1× RAM ${currentRam}GB DDR4 3200MHz (cùng thương hiệu RAM cũ nếu có thể)`,
      estimatedPrice: budget < 500_000 ? '250.000 – 350.000 ₫' : '300.000 – 500.000 ₫',
      whyThisWorks: `Chạy 2 kênh Dual-Channel sẽ tăng tốc đọc/ghi RAM ~15–20%. Phù hợp khi bo mạch còn slot trống.`,
      tags: ['Dual-Channel', 'Tiết kiệm', 'Không cần tháo RAM cũ'],
      scene3D: { ram: 2 },
      searchQuery: `RAM DDR4 ${currentRam}GB 3200MHz chính hãng giá rẻ Việt Nam Memoryzone Phong Vũ Shopee`,
    }

    // Plan B — Tối ưu: kit đôi 2×16GB
    const planB: Plan = {
      id: 'optimal',
      label: 'Phương án 2 · Tối ưu',
      color: 'violet',
      headline: `Nâng lên ${targetRam}GB kit 2 thanh`,
      description: `Thay toàn bộ RAM cũ bằng kit ${targetRam}GB (2×${targetRam / 2}GB) chính hãng, cùng tần số. Hiệu năng đồng đều, tránh lỗi xung đột tần số.`,
      whatToBuy: `Kit 2× ${targetRam / 2}GB DDR4 3600MHz CL18 (Kingston Fury, Corsair Vengeance, hay G.Skill Ripjaws)`,
      estimatedPrice:
        targetRam === 32 ? '800.000 – 1.200.000 ₫' : targetRam === 64 ? '1.800.000 – 2.800.000 ₫' : '500.000 – 800.000 ₫',
      whyThisWorks: `Kit 2 thanh cùng hãng, cùng tần số đảm bảo Dual-Channel hoạt động ổn định nhất. Dành cho render đồ họa và chơi game nặng.`,
      tags: ['Kit đôi', 'Ổn định nhất', 'DDR4 3600 CL18'],
      scene3D: { ram: 2 }, // Kit 2 thanh cắm khe 2 & 4 (Dual-Channel)
      searchQuery: `Kit RAM 2 thanh ${targetRam / 2}GB DDR4 3600 Kingston Corsair GSkill giá rẻ Việt Nam`,
    }

    return [planA, planB]
  }

  if (component === 'ssd') {
    const isHeavyUse = purpose === 'render_game' || purpose === 'video'

    const planA: Plan = {
      id: 'budget',
      label: 'Phương án 1 · Tiết kiệm',
      color: 'teal',
      headline: 'Lắp thêm SSD M.2 1TB Gen3',
      description: `Giữ SSD cũ làm ổ chứa HĐH, lắp thêm 1TB Gen3 vào slot M.2 còn trống để mở rộng dung lượng lưu trữ.`,
      whatToBuy: `1× SSD M.2 2280 NVMe 1TB Gen3 (Kingston NV3, Samsung 980, hay WD Green SN350)`,
      estimatedPrice: '550.000 – 850.000 ₫',
      whyThisWorks: `Gen3 đủ nhanh cho game và lưu trữ dữ liệu (đọc ~3500 MB/s). Tiết kiệm hơn Gen4 khoảng 30–40%.`,
      tags: ['Gen3 NVMe', 'Mở rộng dung lượng', 'Giữ SSD cũ'],
      scene3D: { ssd: Math.min(currentSsd + 1, 2) },
      searchQuery: `SSD M.2 NVMe 1TB Gen3 Kingston Samsung WD giá rẻ Việt Nam`,
    }

    const planB: Plan = {
      id: 'optimal',
      label: 'Phương án 2 · Tối ưu',
      color: 'violet',
      headline: isHeavyUse ? 'SSD M.2 2TB Gen4 — đọc 7000 MB/s' : 'SSD M.2 2TB Gen4 chính hãng',
      description: isHeavyUse
        ? `Gen4 2TB phù hợp render video/3D nặng. Tốc độ đọc gấp đôi Gen3, giảm thời gian load file dự án lớn.`
        : `2TB Gen4 vừa nhanh vừa đủ dung lượng lâu dài. Giá chênh không nhiều so với Gen3 mà tốc độ cao hơn hẳn.`,
      whatToBuy: `1× SSD M.2 2280 NVMe 2TB Gen4 (Samsung 990 Pro, WD Black SN850X, hay Crucial P5 Plus)`,
      estimatedPrice: '1.400.000 – 2.200.000 ₫',
      whyThisWorks: `PCIe Gen4 cho tốc độ đọc ~7000 MB/s — giảm đáng kể thời gian load scene Blender/Premiere. Cần bo mạch hỗ trợ Gen4.`,
      tags: ['Gen4 NVMe', '7000 MB/s', 'Cho render & game AAA'],
      scene3D: { ssd: 2 },
      searchQuery: `SSD M.2 NVMe 2TB PCIe Gen4 Samsung WD Black Crucial giá rẻ Việt Nam`,
    }

    return [planA, planB]
  }

  // GPU
  const isBudgetTight = budget < 6_000_000

  const planA: Plan = {
    id: 'budget',
    label: 'Phương án 1 · Tiết kiệm',
    color: 'teal',
    headline: isBudgetTight ? 'RTX 4060 8GB — 2 quạt gọn' : 'RTX 4060 Ti 16GB',
    description: isBudgetTight
      ? `RTX 4060 8GB đủ sức chơi game Full HD 1080p ultra settings và render nhẹ. Card 2 quạt compact, dễ lắp vào case nhỏ.`
      : `RTX 4060 Ti 16GB VRAM phù hợp render 3D tầm trung và game 1440p. Hiệu năng vượt trội so với 4060 thuần.`,
    whatToBuy: isBudgetTight ? `RTX 4060 8GB OC 2-fan (ASUS Dual, MSI Ventus 2X, hay Gigabyte Eagle)` : `RTX 4060 Ti 16GB (MSI Ventus 3X, ASUS TUF, hay Palit Dual)`,
    estimatedPrice: isBudgetTight ? '5.500.000 – 7.000.000 ₫' : '8.500.000 – 11.000.000 ₫',
    whyThisWorks: isBudgetTight
      ? `RTX 4060 tiết kiệm điện (115W TDP), vừa túi tiền, game 1080p mượt 60+ fps ở ultra. DLSS 3 hỗ trợ boosting FPS.`
      : `16GB VRAM tránh out-of-memory khi render texture 4K, lắp 3D scene phức tạp.`,
    tags: isBudgetTight ? ['2 Quạt', 'DLSS 3', '115W TDP', 'Full HD Ultra'] : ['16GB VRAM', '1440p Gaming', 'Render 3D'],
    scene3D: { gpu: 'gpu_240' },
    searchQuery: isBudgetTight
      ? `Card màn hình RTX 4060 8GB ASUS MSI Gigabyte giá rẻ Việt Nam`
      : `Card màn hình RTX 4060 Ti 16GB ASUS MSI giá rẻ Việt Nam`,
  }

  const planB: Plan = {
    id: 'optimal',
    label: 'Phương án 2 · Tối ưu',
    color: 'violet',
    headline: purpose === 'render_game' || purpose === 'video' ? 'RTX 4070 Super 12GB — render nhanh hơn 40%' : 'RTX 4070 12GB — gaming & đa năng',
    description:
      purpose === 'render_game' || purpose === 'video'
        ? `RTX 4070 Super mạnh hơn 4060 khoảng 40% trong render Blender/DaVinci. 12GB VRAM xử lý dự án phức tạp hơn nhiều.`
        : `RTX 4070 12GB ngọt nước ở 1440p và 4K nhẹ. Hiệu năng/tiền xứng đáng nhất phân khúc mid-high hiện tại.`,
    whatToBuy: `RTX 4070 Super 12GB hoặc RTX 4070 12GB OC (ASUS TUF, MSI Gaming X Trio)`,
    estimatedPrice: '11.000.000 – 15.000.000 ₫',
    whyThisWorks: `DLSS 3.5 + Frame Gen. Hiệu năng render Blender Cycles nhanh hơn 4060 ~45%. 3 quạt tản nhiệt tốt hơn cho ép xung.`,
    tags: ['3 Quạt', '12GB VRAM', 'DLSS 3.5 + Frame Gen', 'Render & 1440p'],
    scene3D: { gpu: 'gpu_280' },
    searchQuery: `Card màn hình RTX 4070 Super 12GB ASUS MSI giá rẻ Việt Nam`,
  }

  return [planA, planB]
}

// ─── Component ────────────────────────────────────────────────────────────

const COMPONENT_OPTIONS: { id: Component; label: string; icon: string }[] = [
  { id: 'ram', label: 'RAM', icon: '🧠' },
  { id: 'ssd', label: 'SSD', icon: '💾' },
  { id: 'gpu', label: 'GPU / Card', icon: '🎮' },
]

const PURPOSE_OPTIONS: { id: Purpose; label: string }[] = [
  { id: 'render_game', label: 'Render đồ họa + Chơi game' },
  { id: 'gaming', label: 'Chơi game là chính' },
  { id: 'video', label: 'Dựng video / Chỉnh ảnh' },
  { id: 'office', label: 'Làm việc văn phòng + Đa nhiệm' },
]

const press = 'transition-transform duration-100 ease-out active:scale-[0.97] motion-reduce:active:scale-100'
const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'


function extractPrice(text: string | undefined): string | null {
  if (!text) return null
  const m = text.match(/(\d{1,3}(?:\.\d{3})+)\s*(?:₫|đ|VND|vnđ)/i) || text.match(/(\d{3,4})k/i)
  return m ? m[0] : null
}

export function SidebarAdvisor() {
  const ramSticks = useViewer((s) => s.ramSticks)
  const ssdCount = useViewer((s) => s.ssdCount)
  const setRamSticks = useViewer((s) => s.setRamSticks)
  const setSsdCount = useViewer((s) => s.setSsdCount)
  const setCustomGpu = useViewer((s) => s.setCustomGpu)
  const selectPart = useViewer((s) => s.select)

  // ── Form state ────────────────────────────────────────────────────────
  const [component, setComponent] = useState<Component>('ram')
  const [purpose, setPurpose] = useState<Purpose>('render_game')
  const [budgetStr, setBudgetStr] = useState<string>('1200000')
  const [currentRam, setCurrentRam] = useState<number>(16)
  const [currentSsd, setCurrentSsd] = useState<number>(ssdCount || 1)

  // ── Result state ──────────────────────────────────────────────────────
  const [advisorState, setAdvisorState] = useState<AdvisorState>('idle')
  const [plans, setPlans] = useState<Plan[]>([])
  const [shopResults, setShopResults] = useState<Record<string, ShopResult[]>>({})
  const [errorMsg, setErrorMsg] = useState<string>('')
  const [activeScene, setActiveScene] = useState<string | null>(null)

  // ── Handlers ──────────────────────────────────────────────────────────
  const handleSubmit = async () => {
    const budget = Number(budgetStr.replace(/[^0-9]/g, '')) || 1_200_000
    const input: AdvisorInput = { component, purpose, budget, currentRam, currentSsd }
    const generatedPlans = generatePlans(input)

    setPlans(generatedPlans)
    setShopResults({})
    setActiveScene(null)
    setAdvisorState('thinking')
    setErrorMsg('')

    // Tìm link mua hàng song song cho cả 2 phương án
    const results: Record<string, ShopResult[]> = {}
    await Promise.allSettled(
      generatedPlans.map(async (plan) => {
        try {
          const params = new URLSearchParams({ q: plan.searchQuery, max: '4' })
          const resp = await fetch(`/api/search-deals?${params}`)
          if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
          const data = await resp.json() as { sources: ShopResult[] }
          results[plan.id] = data.sources.slice(0, 4)
        } catch {
          results[plan.id] = []
        }
      })
    )

    setShopResults(results)
    setAdvisorState('done')
  }

  const handleView3D = (plan: Plan) => {
    setActiveScene(plan.id)
    if (plan.scene3D.ram !== undefined) {
      setRamSticks(plan.scene3D.ram)
      selectPart('ram')
    }
    if (plan.scene3D.ssd !== undefined) {
      setSsdCount(plan.scene3D.ssd)
      selectPart('ssd')
    }
    if (plan.scene3D.gpu) {
      setCustomGpu(plan.scene3D.gpu)
      selectPart('gpu')
    }
  }

  const handleReset = () => {
    setAdvisorState('idle')
    setPlans([])
    setShopResults({})
    setActiveScene(null)
  }

  const formatBudget = (val: string) => {
    const n = val.replace(/[^0-9]/g, '')
    return n ? Number(n).toLocaleString('vi-VN') : ''
  }

  // ── Render ────────────────────────────────────────────────────────────
  return (
    <div className="space-y-4 pt-1">
      {/* Header */}
      <div className="flex items-center gap-2 rounded-2xl border border-accent/30 bg-accent/5 px-3.5 py-3">
        <Robot size={22} weight="duotone" className="shrink-0 text-accent" />
        <div>
          <p className="text-[13px] font-bold text-ink">Chuyên viên tư vấn AI</p>
          <p className="text-[11px] text-ink-2">Nhập thông tin, tôi sẽ gợi ý phù hợp nhất với bạn</p>
        </div>
      </div>

      {advisorState === 'idle' || advisorState === 'thinking' ? (
        <div className="space-y-4">
          {/* Step 1: Chọn linh kiện */}
          <div>
            <p className="mb-1.5 text-[12px] font-semibold text-ink">1. Bạn muốn nâng cấp gì?</p>
            <div className="grid grid-cols-3 gap-1.5">
              {COMPONENT_OPTIONS.map((opt) => (
                <button
                  key={opt.id}
                  onClick={() => setComponent(opt.id)}
                  className={`flex flex-col items-center gap-1 rounded-xl border py-2.5 text-[12px] font-semibold transition-all ${press} ${focus} ${
                    component === opt.id
                      ? 'border-accent bg-accent text-white shadow-sm'
                      : 'border-line bg-surface text-ink-2 hover:border-line-strong hover:text-ink'
                  }`}
                >
                  <span className="text-lg">{opt.icon}</span>
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {/* Step 2: Thực trạng hiện tại */}
          <div>
            <p className="mb-1.5 text-[12px] font-semibold text-ink">2. Máy bạn hiện tại thế nào?</p>
            <div className="rounded-xl border border-line bg-surface p-3 space-y-2.5 text-[12px]">
              {component === 'ram' && (
                <div className="flex items-center justify-between">
                  <span className="text-ink-2">RAM hiện tại:</span>
                  <div className="flex gap-1">
                    {[8, 16, 32].map((n) => (
                      <button
                        key={n}
                        onClick={() => setCurrentRam(n)}
                        className={`rounded-lg px-2.5 py-1 font-semibold transition-all ${press} ${focus} ${
                          currentRam === n ? 'bg-accent text-white' : 'bg-fill text-ink-2 hover:text-ink'
                        }`}
                      >
                        {n}GB
                      </button>
                    ))}
                  </div>
                </div>
              )}
              {component === 'ssd' && (
                <div className="flex items-center justify-between">
                  <span className="text-ink-2">SSD hiện tại:</span>
                  <div className="flex gap-1">
                    {[0, 1, 2].map((n) => (
                      <button
                        key={n}
                        onClick={() => setCurrentSsd(n)}
                        className={`rounded-lg px-2.5 py-1 font-semibold transition-all ${press} ${focus} ${
                          currentSsd === n ? 'bg-accent text-white' : 'bg-fill text-ink-2 hover:text-ink'
                        }`}
                      >
                        {n} ổ
                      </button>
                    ))}
                  </div>
                </div>
              )}
              {component === 'gpu' && (
                <p className="text-ink-2">GPU: Tư vấn dựa trên mục đích và ngân sách bên dưới.</p>
              )}
              <div className="text-[11px] text-ink-3">
                (Dựa vào mô hình 3D đang xem: {ramSticks} thanh RAM, {ssdCount} ổ SSD)
              </div>
            </div>
          </div>

          {/* Step 3: Mục đích */}
          <div>
            <p className="mb-1.5 text-[12px] font-semibold text-ink">3. Bạn dùng PC chủ yếu để làm gì?</p>
            <div className="space-y-1">
              {PURPOSE_OPTIONS.map((opt) => (
                <button
                  key={opt.id}
                  onClick={() => setPurpose(opt.id)}
                  className={`flex w-full items-center justify-between rounded-xl border px-3 py-2 text-left text-[12px] font-medium transition-all ${press} ${focus} ${
                    purpose === opt.id
                      ? 'border-accent bg-accent/5 text-ink font-semibold'
                      : 'border-line bg-surface text-ink-2 hover:border-line-strong'
                  }`}
                >
                  {opt.label}
                  {purpose === opt.id && <ArrowRight size={12} className="text-accent" />}
                </button>
              ))}
            </div>
          </div>

          {/* Step 4: Ngân sách */}
          <div>
            <p className="mb-1.5 text-[12px] font-semibold text-ink">4. Ngân sách bạn có (VNĐ)?</p>
            <div className="relative">
              <CurrencyCircleDollar
                size={16}
                className="absolute left-2.5 top-1/2 -translate-y-1/2 text-ink-3"
              />
              <input
                type="text"
                inputMode="numeric"
                value={formatBudget(budgetStr)}
                onChange={(e) => setBudgetStr(e.target.value.replace(/[^0-9]/g, ''))}
                placeholder="1.200.000"
                className={`w-full rounded-xl border border-line bg-surface py-2 pl-8 pr-3 text-[13px] text-ink placeholder-ink-3 focus-visible:border-accent focus-visible:outline-none ${focus}`}
              />
            </div>
            <div className="mt-1.5 flex gap-1 flex-wrap">
              {[500_000, 1_200_000, 2_500_000, 5_000_000, 10_000_000].map((v) => (
                <button
                  key={v}
                  onClick={() => setBudgetStr(String(v))}
                  className={`rounded-full border px-2 py-0.5 text-[10px] font-semibold transition-all ${press} ${focus} ${
                    Number(budgetStr) === v
                      ? 'border-accent bg-accent text-white'
                      : 'border-line text-ink-3 hover:border-accent/50 hover:text-ink-2'
                  }`}
                >
                  {v >= 1_000_000 ? `${v / 1_000_000}M` : `${v / 1_000}k`}
                </button>
              ))}
            </div>
          </div>

          {/* Submit */}
          <button
            onClick={handleSubmit}
            disabled={advisorState === 'thinking'}
            className={`flex w-full items-center justify-center gap-2 rounded-xl bg-accent py-2.5 text-[13px] font-bold text-white shadow-sm transition-all ${press} ${focus} disabled:opacity-60`}
          >
            {advisorState === 'thinking' ? (
              <>
                <span className="size-4 animate-spin rounded-full border-2 border-white/40 border-t-white" />
                Đang tìm kiếm deal tốt nhất…
              </>
            ) : (
              <>
                <Sparkle size={16} weight="fill" />
                Tư vấn ngay
              </>
            )}
          </button>

          {errorMsg && (
            <p className="text-center text-[11px] text-bad">{errorMsg}</p>
          )}
        </div>
      ) : (
        /* ── KẾT QUẢ ─────────────────────────────────────────────── */
        <div className="space-y-3.5">
          {/* Nút làm lại */}
          <button
            onClick={handleReset}
            className={`flex items-center gap-1.5 text-[12px] font-semibold text-ink-2 hover:text-ink ${press} ${focus}`}
          >
            <ArrowsClockwise size={14} />
            Tư vấn lại
          </button>

          {plans.map((plan) => {
            const shops = shopResults[plan.id] ?? []
            const isActive = activeScene === plan.id
            const colorCls =
              plan.color === 'teal'
                ? { border: 'border-teal-500/30', bg: 'bg-teal-50/40', badge: 'bg-teal-500/10 text-teal-700', btn: 'bg-teal-500 hover:bg-teal-600' }
                : { border: 'border-violet-500/30', bg: 'bg-violet-50/40', badge: 'bg-violet-500/10 text-violet-700', btn: 'bg-violet-500 hover:bg-violet-600' }

            return (
              <div
                key={plan.id}
                className={`rounded-2xl border p-3.5 transition-all ${colorCls.border} ${colorCls.bg} ${
                  isActive ? 'ring-2 ring-accent ring-offset-1' : ''
                }`}
              >
                {/* Plan header */}
                <div className="flex items-start justify-between gap-2 mb-2">
                  <div>
                    <span className={`inline-block rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide ${colorCls.badge}`}>
                      {plan.label}
                    </span>
                    <h3 className="mt-1 text-[14px] font-bold text-ink">{plan.headline}</h3>
                  </div>
                  <span className="shrink-0 rounded-lg bg-surface px-2 py-1 text-[11px] font-bold text-ink shadow-sm">
                    {plan.estimatedPrice}
                  </span>
                </div>

                {/* Mô tả */}
                <p className="text-[12px] leading-relaxed text-ink-2">{plan.description}</p>

                {/* Mua gì */}
                <div className="mt-2 rounded-xl bg-surface/80 px-2.5 py-2 text-[11px] text-ink backdrop-blur-sm">
                  <span className="font-bold text-ink-2">Mua gì: </span>
                  {plan.whatToBuy}
                </div>

                {/* Vì sao chọn */}
                <p className="mt-2 text-[11px] leading-relaxed text-ink-3">💡 {plan.whyThisWorks}</p>

                {/* Tags */}
                <div className="mt-2 flex flex-wrap gap-1">
                  {plan.tags.map((tag) => (
                    <span key={tag} className="rounded-full bg-surface px-2 py-0.5 text-[10px] font-semibold text-ink-2 shadow-sm">
                      {tag}
                    </span>
                  ))}
                </div>

                
                {/* Cảnh báo tương thích BIOS / Firmware quan trọng */}
                <div className="mt-2.5 rounded-xl border border-amber-500/20 bg-amber-50/50 p-2.5 text-[11px] text-amber-900 dark:bg-amber-950/20 dark:text-amber-200">
                  <div className="flex items-center gap-1 font-bold text-amber-700 dark:text-amber-400">
                    <span>⚠️</span>
                    <span>Xác minh trước khi nâng cấp (BIOS & Firmware):</span>
                  </div>
                  <ul className="mt-1 space-y-1 list-disc list-inside text-ink-2 text-[10.5px] leading-relaxed">
                    <li>
                      <strong>Cập nhật BIOS:</strong> Cần update BIOS bo mạch chủ lên bản mới nhất để nhận diện đúng bus RAM (3200/3600MHz) và tránh lỗi kén RAM.
                    </li>
                    <li>
                      <strong>Bật XMP / EXPO:</strong> Sau khi cắm RAM, bắt buộc vào BIOS bật Profile 1 (XMP) để đạt tốc độ tối đa, nếu không RAM sẽ bị hạ về 2133MHz.
                    </li>
                    <li>
                      <strong>Khuyến nghị cắm khe:</strong> Với Kit 2 thanh, ưu tiên cắm khe <strong>DIMM 2 & DIMM 4</strong> (tính từ CPU sang) để kích hoạt Dual-Channel tối ưu.
                    </li>
                  </ul>
                </div>

                {/* Shop links */}
                {shops.length > 0 && (
                  <div className="mt-2.5 space-y-1">
                    <p className="flex items-center gap-1 text-[11px] font-bold text-ink-2">
                      <ShoppingBag size={12} />
                      Tìm mua tại:
                    </p>
                    {shops.map((s, i) => {
                      let domain = ''
                      try { domain = new URL(s.url).hostname.replace('www.', '') } catch { domain = s.url }
                      const price = extractPrice(s.title) || extractPrice(s.snippet)
                      return (
                        <a
                          key={i}
                          href={s.url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="flex flex-col rounded-lg border border-line bg-surface/80 px-2.5 py-1.5 hover:border-accent/40 hover:bg-surface transition-all"
                        >
                          <div className="flex items-start justify-between gap-1.5">
                            <span className="text-[11px] font-semibold text-accent truncate">{s.title || domain}</span>
                            {price && (
                              <span className="shrink-0 rounded bg-emerald-500/10 px-1.5 py-0.5 text-[10px] font-bold text-emerald-600 dark:text-emerald-400">
                                {price}
                              </span>
                            )}
                          </div>
                          <span className="text-[10px] text-ink-3 truncate">{domain}</span>
                          {s.snippet && (
                            <span className="mt-0.5 text-[10px] text-ink-2 line-clamp-2 leading-relaxed">{s.snippet}</span>
                          )}
                        </a>
                      )
                    })}
                  </div>
                )}

                {shops.length === 0 && advisorState === 'done' && (
                  <p className="mt-2 text-[11px] text-ink-3">Không tải được link mua hàng. Thử lại hoặc tìm thủ công trên Shopee/GearVN.</p>
                )}

                {/* Xem 3D */}
                <button
                  onClick={() => handleView3D(plan)}
                  className={`mt-3 flex w-full items-center justify-center gap-1.5 rounded-xl py-2 text-[12px] font-bold text-white shadow-sm transition-all ${press} ${focus} ${colorCls.btn}`}
                >
                  <Desktop size={14} />
                  {isActive ? 'Đang xem trên 3D ✓' : 'Xem trên 3D →'}
                </button>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
