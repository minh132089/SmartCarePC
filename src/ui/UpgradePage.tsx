import { useEffect, useRef, useState } from 'react'
import { ArrowCounterClockwise, House, PencilSimple } from '@phosphor-icons/react'
import { useViewer } from '../store'
import { useUpgradeChat, type CaseSize, type UpgradeForm } from './chat/chatStore'
import { Composer, IconButton, Thread } from './chat/ChatParts'
import { ProposalView } from './chat/Proposal'
import { Choices, PrimaryButton, Question, focus } from './study/StudyParts'
import { StudyBar } from './study/StudyBar'

/**
 * Trang nâng cấp máy đang có. Người dùng điền từng linh kiện (ô trống là không biết), mục đích và số tiền dành cho nâng
 * cấp; backend (mode "upgrade") tìm hàng thật, kiểm tra giá, còn hàng, ngân sách và lắp được với máy cũ rồi trả về một
 * thẻ kế hoạch. Sau đó hỏi tiếp được trong khung chat ("card rẻ hơn được không?").
 */
const KEY = 'pcsc-upgrade-form-v1'
const USES = [
  { value: 'Gaming', label: 'Gaming' },
  { value: 'Study and office work', label: 'Study & office' },
  { value: 'Photo and video editing', label: 'Photo & video' },
  { value: '3D and graphic design', label: '3D & design' },
  { value: 'Programming', label: 'Programming' },
  { value: 'AI and machine learning', label: 'AI' },
]
const CASES: { value: CaseSize; label: string }[] = [
  { value: 'mini', label: 'Mini (very small)' },
  { value: 'compact', label: 'Compact tower' },
  { value: 'standard', label: 'Standard tower' },
  { value: 'large', label: 'Large tower' },
  { value: 'unknown', label: 'I don’t know' },
]
const RAM_SIZES = ['4', '8', '16', '32', '64']
const RAM_TYPES = ['DDR3', 'DDR4', 'DDR5']
const BUSY = ['Reading your current PC…', 'Checking store prices…', 'Checking the parts fit your PC…', 'Writing your upgrade plan…']
const input = `w-full rounded-xl bg-surface px-3 py-2 text-[14.5px] text-ink ring-1 ring-line-strong placeholder:text-ink-3 ${focus}`

type Draft = {
  cpu: string
  motherboard: string
  ramSize: string
  ramType: string
  ramSticks: string
  gpu: string
  noGpu: boolean
  storage: string
  psu: string
  caseSize: CaseSize | ''
  uses: string[]
  details: string
  budget: string
  laptop: boolean
}
const EMPTY: Draft = { cpu: '', motherboard: '', ramSize: '', ramType: '', ramSticks: '', gpu: '', noGpu: false, storage: '', psu: '', caseSize: '', uses: [], details: '', budget: '', laptop: false }

/** Bản nháp chỉ lưu trong trình duyệt này để quay lại không phải gõ lại; bị chặn lưu thì vẫn dùng được trong phiên */
function loadDraft(): Draft {
  try {
    const saved = JSON.parse(localStorage.getItem(KEY) ?? 'null') as Partial<Draft> | null
    return saved ? { ...EMPTY, ...saved } : EMPTY
  } catch {
    return EMPTY
  }
}
function saveDraft(d: Draft) {
  try {
    localStorage.setItem(KEY, JSON.stringify(d))
  } catch {
    /* bỏ qua */
  }
}

/** Ngân sách nhập theo triệu đồng: "5", "5.5", "5,5" → 5 500 000 */
function budgetOf(text: string): number | null {
  const n = Number(text.replace(',', '.').trim())
  return Number.isFinite(n) && n > 0 ? Math.round(n * 1_000_000) : null
}

function toForm(d: Draft): UpgradeForm {
  const ram = [[d.ramSize && `${d.ramSize}GB`, d.ramType].filter(Boolean).join(' '), d.ramSticks && `${d.ramSticks} ${d.ramSticks === '1' ? 'stick' : 'sticks'}`].filter(Boolean).join(', ')
  return {
    cpu: d.cpu.trim(),
    motherboard: d.motherboard.trim(),
    ram,
    gpu: d.noGpu ? 'none' : d.gpu.trim(),
    storage: d.storage.trim(),
    psu: d.psu.trim(),
    caseSize: d.caseSize || 'unknown',
    purpose: [d.uses.join(', '), d.details.trim().replace(/[.\s]+$/, '')].filter(Boolean).join('. '),
    budgetVnd: budgetOf(d.budget) ?? 0,
  }
}

export function UpgradePage() {
  const setPage = useViewer((s) => s.setPage)
  const { messages, busy, stage, error, send, retry, reset } = useUpgradeChat()
  const [draft, setDraft] = useState<Draft>(loadDraft)
  const [editing, setEditing] = useState(messages.length === 0)
  const [question, setQuestion] = useState('')
  const listRef = useRef<HTMLDivElement>(null)
  const form = toForm(draft)
  const budget = budgetOf(draft.budget)
  const budgetError = draft.budget.trim() && (budget == null || budget < 300_000 || budget > 200_000_000)
  const ready = form.purpose.length > 0 && budget != null && !budgetError && !draft.laptop
  // Thẻ theo câu trả lời mới nhất: câu đó kết luận không nên mua gì thì không còn trưng thẻ mua hàng cũ
  const latest = messages.findLast((m) => m.role === 'assistant')?.build ?? null

  useEffect(() => saveDraft(draft), [draft])
  useEffect(() => {
    const el = listRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [messages, busy, error])

  const update = (v: Partial<Draft>) => setDraft((d) => ({ ...d, ...v }))
  const submit = () => {
    if (!ready) return
    reset()
    setEditing(false)
    const known = [form.cpu, form.gpu === 'none' ? '' : form.gpu, form.ram].filter(Boolean).join(', ')
    void send(
      `Please suggest an upgrade for my PC${known ? ` (${known})` : ''}. Main use: ${form.purpose}. Upgrade budget: VND ${(budget! / 1e6).toLocaleString('en-GB')} million.`,
      { upgrade: form },
      [],
    )
  }
  const ask = () => {
    const text = question
    setQuestion('')
    void send(text, { upgrade: form })
  }

  const header = (
    <header className="flex items-center justify-between gap-3 px-4 pt-5 pb-2 md:px-8">
      <button onClick={() => setPage('home')} className={`flex items-center gap-1.5 rounded-full px-2 py-1 text-[14px] font-medium text-link hover:bg-fill ${focus}`}>
        <House size={16} aria-hidden /> Home
      </button>
      {!editing && (
        <div className="flex items-center gap-2">
          <button onClick={() => setEditing(true)} className={`flex items-center gap-1.5 rounded-full bg-fill px-3 py-1.5 text-[13.5px] font-medium text-ink hover:bg-fill-hover ${focus}`}>
            <PencilSimple size={14} aria-hidden /> Edit PC details
          </button>
          <IconButton label="Start over" onClick={() => { reset(); setEditing(true) }} icon={<ArrowCounterClockwise size={16} aria-hidden />} />
        </div>
      )}
    </header>
  )

  if (editing) {
    return (
      <div className="h-full overflow-y-auto bg-canvas">
        {header}
        <div className="mx-auto max-w-[760px] px-4 pb-16 md:px-8">
          <StudyBar />
          <h1 className="mt-6 text-[clamp(1.8rem,1.3rem+2vw,2.6rem)] leading-[1.1] font-bold tracking-[-0.03em] text-ink">Upgrade my PC</h1>
          <p className="mt-2 text-[15.5px] leading-[1.5] text-ink-2">
            Tell us what you have now, what you use the PC for and how much you want to spend. We suggest only the parts worth
            replacing, with store prices, and check that they fit your PC.
          </p>

          <section className="mt-7 rounded-[24px] bg-panel p-5 ring-1 ring-line md:p-6">
            <h2 className="text-[17px] font-semibold text-ink">Your current PC</h2>
            <label className="mt-2 flex items-center gap-2 text-[14px] text-ink-2">
              <input type="checkbox" checked={draft.laptop} onChange={(e) => update({ laptop: e.target.checked })} /> I use a laptop
            </label>
            {draft.laptop && (
              <p role="status" className="mt-2 rounded-xl bg-info-bg p-3 text-[14px] leading-[1.5] text-info">
                This adviser is for desktop PCs. Laptop upgrades depend on the exact model: many have soldered memory and a single M.2
                slot, so check the manufacturer's service manual or ask a repair shop.
              </p>
            )}
            <p className="mt-1 text-[13px] leading-[1.5] text-ink-2">
              Leave a box empty if you do not know. On Windows, <strong>Settings › System › About</strong> shows the processor and RAM.{' '}
              <strong>Task Manager › Performance</strong> shows the graphics card and drives. The power supply wattage is printed on its label.
            </p>
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              <Field label="Processor (CPU)" value={draft.cpu} onChange={(cpu) => update({ cpu })} placeholder="e.g. Intel Core i5-10400" />
              <Field label="Motherboard" hint="Helps check which CPU and RAM fit" value={draft.motherboard} onChange={(motherboard) => update({ motherboard })} placeholder="e.g. ASUS PRIME B460M-A" />
              <div className="md:col-span-2">
                <span className="text-[13.5px] font-medium text-ink">Memory (RAM)</span>
                <div className="mt-1.5 grid gap-2 sm:grid-cols-3">
                  <select aria-label="RAM amount" value={draft.ramSize} onChange={(e) => update({ ramSize: e.target.value })} className={input}>
                    <option value="">Amount: not sure</option>
                    {RAM_SIZES.map((s) => <option key={s} value={s}>{s} GB</option>)}
                  </select>
                  <select aria-label="RAM type" value={draft.ramType} onChange={(e) => update({ ramType: e.target.value })} className={input}>
                    <option value="">Type: not sure</option>
                    {RAM_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                  </select>
                  <select aria-label="Number of RAM sticks" value={draft.ramSticks} onChange={(e) => update({ ramSticks: e.target.value })} className={input}>
                    <option value="">Sticks: not sure</option>
                    {['1', '2', '4'].map((n) => <option key={n} value={n}>{n} {n === '1' ? 'stick' : 'sticks'}</option>)}
                  </select>
                </div>
              </div>
              <div>
                <Field label="Graphics card" value={draft.noGpu ? '' : draft.gpu} onChange={(gpu) => update({ gpu })} placeholder="e.g. GTX 1650 4GB" disabled={draft.noGpu} />
                <label className="mt-2 flex items-center gap-2 text-[13.5px] text-ink-2">
                  <input type="checkbox" checked={draft.noGpu} onChange={(e) => update({ noGpu: e.target.checked })} /> No separate graphics card
                </label>
              </div>
              <Field label="Storage" value={draft.storage} onChange={(storage) => update({ storage })} placeholder="e.g. HDD 1TB, or SSD 256GB + HDD 1TB" />
              <Field label="Power supply" value={draft.psu} onChange={(psu) => update({ psu })} placeholder="e.g. 550W" />
            </div>
            <Question label="Case size">
              <Choices name="case-size" value={draft.caseSize ? [draft.caseSize] : []} options={CASES} onChange={(v) => update({ caseSize: v[0] })} />
            </Question>
          </section>

          <section className="mt-4 rounded-[24px] bg-panel p-5 ring-1 ring-line md:p-6">
            <h2 className="text-[17px] font-semibold text-ink">What you need</h2>
            <Question label="What do you mainly use the PC for?" hint="Choose all that apply.">
              <Choices name="uses" multi value={draft.uses} options={USES} onChange={(uses) => update({ uses })} />
            </Question>
            <label className="mt-4 block">
              <span className="text-[13.5px] font-medium text-ink">Details</span>
              <span className="block text-[12.5px] text-ink-3">Which games or software? What feels slow now?</span>
              <textarea
                value={draft.details}
                onChange={(e) => update({ details: e.target.value.slice(0, 400) })}
                rows={3}
                placeholder="e.g. GTA V stutters and Windows takes two minutes to start"
                className={`${input} mt-1.5 resize-none leading-[1.45]`}
              />
            </label>
            <label className="mt-4 block max-w-[320px]">
              <span className="text-[13.5px] font-medium text-ink">Upgrade budget</span>
              <span className="block text-[12.5px] text-ink-3">For the new parts only</span>
              <span className="mt-1.5 flex items-center gap-2">
                <input inputMode="decimal" value={draft.budget} onChange={(e) => update({ budget: e.target.value.replace(/[^\d.,]/g, '') })} placeholder="e.g. 5" aria-invalid={!!budgetError} className={input} />
                <span className="shrink-0 text-[14px] text-ink-2">million VND</span>
              </span>
              {budgetError && <span className="mt-1 block text-[12.5px] text-bad">Enter an amount between 0.3 and 200 million VND.</span>}
            </label>
          </section>

          <div className="mt-6 flex flex-wrap items-center gap-3">
            <PrimaryButton onClick={submit} disabled={!ready || busy}>Get my upgrade plan</PrimaryButton>
            {!ready && !draft.laptop && <span className="text-[13px] text-ink-3">Choose at least one use and enter your budget.</span>}
            {messages.length > 0 && <button onClick={() => setEditing(false)} className={`text-[14px] font-medium text-link hover:underline ${focus}`}>Back to my plan</button>}
          </div>
          <p className="mt-3 text-[12px] text-ink-3">This takes about a minute while we check store prices. Your answers stay in this browser.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-full flex-col bg-canvas">
      {header}
      <div className="min-h-0 flex-1 overflow-y-auto" ref={listRef}>
        <div className="mx-auto grid max-w-[1180px] gap-6 px-4 pb-6 md:px-8 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.05fr)]">
          <div className="min-w-0 space-y-4 pt-2" aria-live="polite">
            <StudyBar />
            <Thread messages={messages} busy={busy} stage={stage} error={error} onRetry={() => retry({ upgrade: form })} compactCards busyHints={BUSY} />
          </div>
          <aside className="min-w-0 lg:pt-2">
            {latest ? (
              <div className="rounded-[24px] bg-panel p-5 ring-1 ring-line md:p-6">
                <ProposalView card={latest} />
              </div>
            ) : (
              <div className="rounded-[24px] bg-panel p-5 text-[14px] leading-[1.5] text-ink-2 ring-1 ring-line">
                {busy ? 'Your plan appears here once store prices and fit have been checked.' : 'The latest answer has no upgrade plan to buy. Ask a follow-up or edit your PC details.'}
              </div>
            )}
          </aside>
        </div>
      </div>
      <div className="shrink-0 border-t border-line bg-canvas px-4 pt-3 pb-3">
        <div className="mx-auto max-w-[820px]">
          <Composer value={question} onChange={setQuestion} onSend={ask} busy={busy} placeholder="Ask a follow-up, e.g. “Is there a cheaper graphics card?”" />
          <p className="mt-1.5 text-center text-[11px] text-ink-3">AI can make mistakes. Check the details on the store page before buying.</p>
        </div>
      </div>
    </div>
  )
}

function Field({ label, hint, value, onChange, placeholder, disabled }: { label: string; hint?: string; value: string; onChange: (v: string) => void; placeholder: string; disabled?: boolean }) {
  return (
    <label className="block">
      <span className="text-[13.5px] font-medium text-ink">{label}</span>
      {hint && <span className="block text-[12.5px] text-ink-3">{hint}</span>}
      <input value={value} onChange={(e) => onChange(e.target.value.slice(0, 160))} placeholder={placeholder} disabled={disabled} className={`${input} mt-1.5 disabled:opacity-50`} />
    </label>
  )
}
