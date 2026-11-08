import { useState, useEffect } from 'react'
import {
  Wrench,
  ShieldCheck,
  Desktop,
  CaretRight,
  Sparkle,
} from '@phosphor-icons/react'
import { useViewer } from '../store'
import type { PartId } from '../data/parts'
import {
  MAINTENANCE_TASKS,
  DIAGNOSTIC_ISSUES,
  MAINTENANCE_TOOLS,
  SAFETY_RULES,
  type DiagnosticIssue,
} from '../data/maintenance'

const press = 'transition-transform duration-100 ease-out active:scale-[0.97] motion-reduce:active:scale-100'
const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

const STORAGE_KEY = 'smartcare_maintenance_completed_tasks'

export function SidebarMaintenance() {
  const selectPart = useViewer((s) => s.select)
  const inspectPart = useViewer((s) => s.inspect)

  const [viewMode, setViewMode] = useState<'checklist' | 'diagnose' | 'tools'>('checklist')
  const [completed, setCompleted] = useState<string[]>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      return saved ? JSON.parse(saved) : []
    } catch {
      return []
    }
  })

  // Selected diagnostic issue
  const [selectedIssueId, setSelectedIssueId] = useState<string>('no_display')
  const currentIssue: DiagnosticIssue =
    DIAGNOSTIC_ISSUES.find((item) => item.id === selectedIssueId) ?? DIAGNOSTIC_ISSUES[0]

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(completed))
    } catch {
      // ignore
    }
  }, [completed])

  const toggleTask = (id: string) => {
    setCompleted((prev) => (prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]))
  }

  const healthScore = Math.round((completed.length / MAINTENANCE_TASKS.length) * 100)

  const viewPartIn3D = (partId?: PartId) => {
    if (partId) {
      selectPart(partId)
      inspectPart(partId)
    }
  }

  return (
    <div className="space-y-4 pt-2">
      {/* Sub Mode Selector */}
      <div className="grid grid-cols-3 gap-1 rounded-full bg-fill p-1 text-[12px]">
        <button
          onClick={() => setViewMode('checklist')}
          className={`rounded-full py-1.5 font-medium transition-all ${
            viewMode === 'checklist' ? 'bg-surface text-ink shadow-sm font-semibold' : 'text-ink-2 hover:text-ink'
          }`}
        >
          Checklist
        </button>
        <button
          onClick={() => setViewMode('diagnose')}
          className={`rounded-full py-1.5 font-medium transition-all ${
            viewMode === 'diagnose' ? 'bg-surface text-ink shadow-sm font-semibold' : 'text-ink-2 hover:text-ink'
          }`}
        >
          Bắt bệnh
        </button>
        <button
          onClick={() => setViewMode('tools')}
          className={`rounded-full py-1.5 font-medium transition-all ${
            viewMode === 'tools' ? 'bg-surface text-ink shadow-sm font-semibold' : 'text-ink-2 hover:text-ink'
          }`}
        >
          Đồ nghề
        </button>
      </div>

      {/* SUB-VIEW 1: CHECKLIST BẢO DƯỠNG */}
      {viewMode === 'checklist' && (
        <div className="space-y-3.5">
          {/* Health Score Box */}
          <div className="rounded-2xl border border-line bg-surface p-3.5 shadow-sm">
            <div className="flex items-center justify-between text-[12px]">
              <span className="font-semibold text-ink flex items-center gap-1">
                <ShieldCheck size={16} className="text-accent" weight="fill" />
                Điểm Sức khỏe PC:
              </span>
              <span className={`font-bold ${healthScore >= 70 ? 'text-ok' : healthScore >= 30 ? 'text-warn' : 'text-ink-2'}`}>
                {healthScore}% ({completed.length}/{MAINTENANCE_TASKS.length} việc)
              </span>
            </div>
            <div className="mt-2 h-2.5 w-full overflow-hidden rounded-full bg-fill">
              <div
                className={`h-full rounded-full transition-all duration-300 ${
                  healthScore >= 70 ? 'bg-ok-strong' : healthScore >= 30 ? 'bg-accent' : 'bg-warn'
                }`}
                style={{ width: `${healthScore}%` }}
              />
            </div>
          </div>

          {/* List of Tasks */}
          <div className="space-y-2">
            {MAINTENANCE_TASKS.map((task) => {
              const isDone = completed.includes(task.id)
              return (
                <div
                  key={task.id}
                  className={`rounded-xl border p-3 transition-all ${
                    isDone ? 'border-ok-strong/30 bg-ok-bg/20' : 'border-line bg-surface'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <label className="flex cursor-pointer items-start gap-2.5 flex-1">
                      <input
                        type="checkbox"
                        checked={isDone}
                        onChange={() => toggleTask(task.id)}
                        className="mt-0.5 size-4 rounded accent-ok-strong cursor-pointer"
                      />
                      <div>
                        <h4 className={`text-[13px] font-bold ${isDone ? 'text-ok line-through' : 'text-ink'}`}>
                          {task.title}
                        </h4>
                        <p className="mt-0.5 text-[11px] text-ink-2 leading-relaxed">{task.summary}</p>
                      </div>
                    </label>

                    <span className="shrink-0 rounded bg-fill px-1.5 py-0.5 text-[10px] font-medium text-ink-2">
                      {task.periodLabel}
                    </span>
                  </div>

                  {/* Actions */}
                  {task.partId && (
                    <div className="mt-2 flex justify-end border-t border-line/50 pt-1.5">
                      <button
                        onClick={() => viewPartIn3D(task.partId)}
                        className="flex items-center gap-1 text-[11px] font-semibold text-accent hover:underline"
                      >
                        <Desktop size={13} />
                        Xem linh kiện trên 3D ›
                      </button>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* SUB-VIEW 2: TRỢ LÝ BẮT BỆNH */}
      {viewMode === 'diagnose' && (
        <div className="space-y-3.5">
          {/* Select Issue */}
          <div>
            <label className="text-[12px] font-semibold text-ink-2">Chọn triệu chứng máy gặp phải:</label>
            <div className="mt-1.5 space-y-1">
              {DIAGNOSTIC_ISSUES.map((issue) => {
                const active = issue.id === selectedIssueId
                return (
                  <button
                    key={issue.id}
                    onClick={() => setSelectedIssueId(issue.id)}
                    className={`flex w-full items-center justify-between rounded-xl border p-2 text-left transition-all ${press} ${focus} ${
                      active
                        ? 'border-accent bg-accent/5 font-semibold text-ink ring-1.5 ring-accent'
                        : 'border-line bg-surface text-ink-2 hover:border-line-strong hover:text-ink'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <span
                        className={`size-2 rounded-full shrink-0 ${
                          issue.severity === 'urgent' ? 'bg-bad' : 'bg-warn'
                        }`}
                      />
                      <span className="text-[12px] leading-tight">{issue.title}</span>
                    </div>
                    <CaretRight size={12} className="shrink-0 text-ink-3" />
                  </button>
                )
              })}
            </div>
          </div>

          {/* Issue Details */}
          <div className="rounded-2xl border border-line bg-surface p-3.5 shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <span
                className={`rounded-full px-2 py-0.5 text-[10px] font-bold uppercase ${
                  currentIssue.severity === 'urgent' ? 'bg-bad-bg text-bad' : 'bg-warn-bg text-warn'
                }`}
              >
                {currentIssue.severity === 'urgent' ? 'Khẩn cấp' : 'Cảnh báo'}
              </span>

              {currentIssue.targetPart && (
                <button
                  onClick={() => viewPartIn3D(currentIssue.targetPart)}
                  className="flex items-center gap-1 text-[11px] font-semibold text-accent hover:underline"
                >
                  <Desktop size={13} />
                  Soi trên 3D
                </button>
              )}
            </div>

            <div>
              <h4 className="text-[14px] font-bold text-ink">{currentIssue.title}</h4>
              <p className="mt-1 text-[11px] text-ink-2">
                <strong>Nguyên nhân cốt lõi:</strong> {currentIssue.cause}
              </p>
            </div>

            {/* Steps */}
            <div className="space-y-2">
              <p className="flex items-center gap-1 text-[12px] font-bold text-ink">
                <Wrench size={14} className="text-accent" />
                Các bước xử lý từ dễ đến khó:
              </p>
              {currentIssue.steps.map((st) => (
                <div key={st.order} className="rounded-xl border border-line bg-canvas/60 p-2.5 text-[11px]">
                  <p className="font-bold text-ink">
                    {st.order}. {st.title}
                  </p>
                  <p className="mt-0.5 text-ink-2 leading-relaxed">{st.action}</p>
                </div>
              ))}
            </div>

            {/* Pro tip */}
            <div className="rounded-xl border border-accent/20 bg-accent/5 p-2.5 text-[11px]">
              <p className="flex items-center gap-1 font-bold text-accent">
                <Sparkle size={13} />
                Mẹo thợ:
              </p>
              <p className="mt-0.5 text-ink">{currentIssue.proTip}</p>
            </div>
          </div>
        </div>
      )}

      {/* SUB-VIEW 3: ĐỒ NGHỀ & AN TOÀN */}
      {viewMode === 'tools' && (
        <div className="space-y-3.5">
          {/* Nút xem thảm đồ nghề 3D */}
          <div className="rounded-2xl border border-line bg-surface p-3.5 shadow-sm text-center">
            <h4 className="text-[13px] font-bold text-ink">Bộ Đồ nghề Lắp & Bảo trì Máy</h4>
            <p className="mt-1 text-[11px] text-ink-2">
              Xem toàn bộ tua vít, keo tản nhiệt, thảm chống tĩnh điện, kìm cắt ngay trên sàn 3D.
            </p>
            <button
              onClick={() => viewPartIn3D('tools')}
              className={`mt-2.5 w-full rounded-full bg-accent py-2 text-center text-[12px] font-semibold text-white hover:bg-accent-hover ${press} ${focus}`}
            >
              Xem bộ đồ nghề trên 3D ›
            </button>
          </div>

          {/* 5 Nguyên tắc an toàn */}
          <div className="rounded-2xl border border-amber-300/40 bg-amber-50/40 dark:bg-amber-950/20 p-3.5 text-[11px]">
            <p className="flex items-center gap-1 font-bold text-amber-900 dark:text-amber-300">
              <ShieldCheck size={14} className="text-amber-600" weight="fill" />
              5 Nguyên tắc an toàn vàng:
            </p>
            <ul className="mt-1.5 space-y-1.5 text-zinc-800 dark:text-zinc-200">
              {SAFETY_RULES.map((r, i) => (
                <li key={i} className="flex items-start gap-1.5">
                  <span className="font-bold text-amber-700 shrink-0">{i + 1}.</span>
                  <span>{r}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Dụng cụ cơ bản */}
          <div className="space-y-2">
            <p className="text-[12px] font-bold text-ink-2">Dụng cụ thiết yếu:</p>
            {MAINTENANCE_TOOLS.map((t) => (
              <div key={t.id} className="rounded-xl border border-line bg-surface p-2.5 text-[11px]">
                <h5 className="font-bold text-ink">{t.name}</h5>
                <p className="text-ink-3">Quy cách: {t.spec}</p>
                <p className="mt-1 text-ink-2">{t.purpose}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
