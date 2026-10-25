import { useCallback, useEffect, useMemo, useState } from 'react'
import { ArrowClockwise, DownloadSimple, Star } from '@phosphor-icons/react'
import { FEEDBACK } from './questions'
import { PrimaryButton, QuietButton, focus } from './StudyParts'
import { FeedbackOverview } from './FeedbackOverview'
import { participantOf, score as num, type FeedbackRow as Row } from './feedbackStats'
import LatticeLoader from '../../components/LatticeLoader'

/**
 * Trang xem phản hồi cho nhóm nghiên cứu: mở /feedback (hoặc #feedback), nhập khoá FEEDBACK_EXPORT_KEY trong backend/.env.
 * Khoá chỉ giữ trong tab đang mở (sessionStorage) và gửi qua header, không nằm trong địa chỉ trang.
 */
const KEY = 'pcsc-export-key'
const SCALES = FEEDBACK.filter((q) => q.type === 'scale')
const TEXTS = FEEDBACK.filter((q) => q.type === 'text')
const PHASE: Record<string, string> = { before: 'Lần đầu', after: 'Sau 4 tuần' }

function readKey() {
  try {
    return sessionStorage.getItem(KEY) ?? ''
  } catch {
    return ''
  }
}

export function FeedbackAdmin() {
  const [key, setKey] = useState(readKey)
  const [draft, setDraft] = useState(key)
  const [rows, setRows] = useState<Row[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [filter, setFilter] = useState<'all' | 'stars' | 'study'>('all')

  const load = useCallback(async (k: string) => {
    if (!k) return
    setBusy(true)
    setError(null)
    try {
      const res = await fetch('/api/feedback/list', { headers: { 'X-Export-Key': k } })
      if (res.status === 403) throw new Error('Sai khoá, hoặc máy chủ chưa đặt FEEDBACK_EXPORT_KEY.')
      if (!res.ok) throw new Error(res.status === 503 ? 'Máy chủ demo đang tắt.' : `Lỗi ${res.status}`)
      setRows((await res.json()) as Row[])
      try {
        sessionStorage.setItem(KEY, k)
      } catch {
        /* bỏ qua */
      }
    } catch (e) {
      setRows(null)
      setError(e instanceof Error ? e.message : 'Không tải được dữ liệu')
    } finally {
      setBusy(false)
    }
  }, [])

  useEffect(() => {
    void load(key)
  }, [key, load])

  // Cùng khoá mà lần trước lỗi tạm thời (mạng, 503): setKey không đổi giá trị nên effect không chạy lại, phải tải trực tiếp
  const submitKey = () => {
    const k = draft.trim()
    if (k === key) void load(k)
    else setKey(k)
  }

  const shown = useMemo(
    () =>
      (rows ?? []).filter((r) =>
        filter === 'stars' ? num(r.answers.stars) != null : filter === 'study' ? r.participantCode != null : true,
      ),
    [rows, filter],
  )
  const download = async () => {
    // Khoá gửi qua header, không nằm trong địa chỉ (tránh lưu vào log truy cập)
    const res = await fetch('/api/feedback/export.csv', { headers: { 'X-Export-Key': key } })
    if (!res.ok) return setError(`Không tải được CSV (lỗi ${res.status})`)
    const url = URL.createObjectURL(await res.blob())
    const a = document.createElement('a')
    a.href = url
    a.download = 'pcsmartcare-feedback.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  if (!key || (!rows && error)) {
    return (
      <div className="grid min-h-full place-items-center bg-canvas p-4">
        <form
          onSubmit={(e) => {
            e.preventDefault()
            submitKey()
          }}
          className="w-full max-w-[420px] rounded-[24px] bg-panel p-6 ring-1 ring-line"
        >
          <h1 className="text-[20px] font-semibold text-ink">Dữ liệu phản hồi</h1>
          <p className="mt-2 text-[14px] leading-[1.5] text-ink-2">Nhập khoá xem dữ liệu (FEEDBACK_EXPORT_KEY trong backend/.env).</p>
          <input
            type="password"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            autoComplete="off"
            aria-label="Khoá xem dữ liệu"
            className={`mt-4 w-full rounded-xl bg-surface px-3 py-2 text-[15px] text-ink ring-1 ring-line-strong ${focus}`}
          />
          {error && <p role="alert" className="mt-2 text-[13px] text-bad">{error}</p>}
          <div className="mt-4 flex justify-end">
            <PrimaryButton onClick={submitKey} disabled={!draft.trim() || busy}>
              Xem dữ liệu
            </PrimaryButton>
          </div>
        </form>
      </div>
    )
  }

  return (
    <div className="feedback-dashboard h-full overflow-y-auto bg-canvas">
      <div className="mx-auto max-w-[1240px] px-4 py-8 md:px-8">
        <a href="/" className={`mb-5 inline-block text-[13px] text-link hover:underline ${focus}`}>← Về SmartCare PC</a>
        <p className="mb-2 text-[11px] font-semibold tracking-[.14em] text-ink-3">SMARTCARE PC · NGHIÊN CỨU TRẢI NGHIỆM</p>
        <header className="flex flex-wrap items-center gap-3">
          <h1 className="display min-w-0 flex-1">Dữ liệu phản hồi</h1>
          <QuietButton onClick={() => void load(key)}>
            <span className="flex items-center gap-1.5">
              <ArrowClockwise size={15} aria-hidden /> Tải lại
            </span>
          </QuietButton>
          <PrimaryButton onClick={() => void download()}>
            <span className="flex items-center gap-1.5">
              <DownloadSimple size={15} aria-hidden /> Tải CSV
            </span>
          </PrimaryButton>
        </header>

        <div className="mt-4 flex flex-wrap gap-1.5" role="radiogroup" aria-label="Lọc">
          {(
            [
              ['all', 'Tất cả'],
              ['stars', 'Có chấm sao'],
              ['study', 'Người tham gia thử nghiệm'],
            ] as const
          ).map(([v, label]) => (
            <button
              key={v}
              role="radio"
              aria-checked={filter === v}
              onClick={() => setFilter(v)}
              className={`rounded-full px-3 py-1.5 text-[13.5px] ring-1 ${focus} ${
                filter === v ? 'bg-ink font-medium text-on-ink ring-ink' : 'bg-surface text-ink ring-line-strong hover:bg-fill'
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        {error && <p role="alert" className="mt-3 text-[13px] text-bad">{error}</p>}
        {busy && <div className="mt-5 text-ink-2"><LatticeLoader label="Đang tải phản hồi" fontSize={13} /></div>}

        {rows && <FeedbackOverview rows={shown} />}

        <section className="mt-5">
          <p className="text-[13px] font-semibold text-ink">Từng phản hồi (mới nhất trước)</p>
          {shown.length === 0 && <p className="mt-2 text-[14px] text-ink-3">Chưa có phản hồi nào.</p>}
          <ul className="mt-2 space-y-2">
            {shown.map((r) => {
              const stars = num(r.answers.stars)
              const person = participantOf(r)
              return (
                <li key={r.id} className="rounded-2xl bg-panel p-4 ring-1 ring-line">
                  <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[12.5px] text-ink-3">
                    <span className="font-semibold text-ink">#{r.id}</span>
                    <span>{r.createdAt ?? ''}</span>
                    {r.participantCode && (
                      <span className="rounded-full bg-info-bg px-2 py-0.5 text-info">
                        {r.participantCode}
                        {r.phase ? ` · ${PHASE[r.phase] ?? r.phase}` : ''}
                      </span>
                    )}
                    {r.machine && <span>Máy: {r.machine}</span>}
                    {stars != null && (
                      <span className="flex items-center gap-0.5 text-[#f5a524]" aria-label={`${stars} sao`}>
                        {[1, 2, 3, 4, 5].map((n) => (
                          <Star key={n} size={14} weight={n <= stars ? 'fill' : 'regular'} aria-hidden />
                        ))}
                      </span>
                    )}
                  </div>
                  <div className="feedback-person">
                    <strong>{person.name || "Chưa có thông tin hồ sơ"}</strong>
                    {person.age != null && <span>{person.age} tuổi</span>}
                    {person.occupation && <span>{person.occupation}</span>}
                  </div>
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    {SCALES.map((q, i) => {
                      const v = num(r.answers[q.key])
                      return v == null ? null : (
                        <span key={q.key} title={q.text} className="rounded-full bg-fill px-2 py-0.5 text-[12px] tabular-nums text-ink-2">
                          Câu {i + 1}: {v}
                        </span>
                      )
                    })}
                  </div>
                  {TEXTS.map((q) => {
                    const v = r.answers[q.key]
                    return typeof v === 'string' && v.trim() ? (
                      <div key={q.key} className="mt-2">
                        <p className="text-[12px] font-semibold text-ink-2">{q.text}</p>
                        <p className="mt-0.5 whitespace-pre-wrap text-[14px] leading-[1.45] text-ink">{v}</p>
                      </div>
                    ) : null
                  })}
                </li>
              )
            })}
          </ul>
        </section>
      </div>
    </div>
  )
}
