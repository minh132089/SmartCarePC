import { ArrowRight, ArrowCircleUp, Desktop, Wrench } from '@phosphor-icons/react'
import type { ReactNode } from 'react'
import { useViewer } from '../store'
import { useMaintenance } from './maintenance/maintenanceState'
import { useStudy } from './study/study'
import { StudyBar } from './study/StudyBar'

const press = 'transition-transform duration-100 ease-out active:scale-[0.985] motion-reduce:active:scale-100'
const focus = 'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent'

/**
 * Màn đầu tiên: người dùng chọn một trong ba hướng. Lắp máy mới thì sang trang tư vấn (cần mục đích và ngân sách),
 * bảo dưỡng thì mở hộp chọn kiểu máy và loại tản (MaintenanceHub), nâng cấp thì sang trang nhập cấu hình hiện tại,
 * mục đích và ngân sách nâng cấp. Mỗi thẻ ghi rõ cần chuẩn bị gì, để người không rành biết trước việc phải làm.
 */
export function HomePage() {
  const setPage = useViewer((s) => s.setPage)
  const openStudy = useStudy((s) => s.open)
  const code = useStudy((s) => s.code)
  return (
    <div className="h-full overflow-y-auto bg-canvas">
      <div className="mx-auto flex min-h-full max-w-[1080px] flex-col px-4 pt-5 pb-10 md:px-8">
        <header className="flex items-center justify-between gap-3">
          <span className="text-[15px] font-semibold tracking-[-0.01em] text-ink">SmartCare PC</span>
          {!code && (
            <button onClick={() => openStudy('join')} className={`rounded-full px-3 py-1.5 text-[13px] font-medium text-link hover:bg-fill ${focus}`}>
              Study participant? Enter your code
            </button>
          )}
        </header>
        <StudyBar />

        <main className="flex flex-1 flex-col justify-center py-10">
          <p className="font-mono text-[12px] tracking-[0.18em] text-ink-2 uppercase">Your desktop PC, step by step</p>
          <h1 className="mt-2 max-w-[640px] text-[clamp(2rem,1.3rem+3vw,3.4rem)] leading-[1.05] font-bold tracking-[-0.035em] text-balance text-ink">
            What would you like to do with your PC?
          </h1>
          <p className="mt-3 max-w-[560px] text-[16px] leading-[1.5] text-ink-2">
            Choose one. Each path asks only for what it needs, and you can come back here at any time.
          </p>

          <ul className="mt-8 grid gap-3 md:grid-cols-3">
            <Choice
              icon={<Desktop size={26} aria-hidden />}
              title="Build a new PC"
              text="Get three component options for your budget, then follow a 3D guide to assemble it yourself."
              need="Your budget and what you will use the PC for."
              onClick={() => setPage('advice')}
            />
            <Choice
              icon={<Wrench size={26} aria-hidden />}
              title="Maintain my PC"
              text="Clean out dust, replace thermal paste or run a quick health check, with reminders when it is due."
              need="Your PC's shape and cooler type. Pictures help you choose."
              onClick={() => useMaintenance.getState().open()}
            />
            <Choice
              icon={<ArrowCircleUp size={26} aria-hidden />}
              title="Upgrade my PC"
              text="Find out which parts to replace for your use, with store prices and checks that they fit your PC."
              need="Your current parts if you know them, what you use the PC for, and your upgrade budget."
              onClick={() => setPage('upgrade')}
            />
          </ul>
          <p className="mt-5 text-[13px] text-ink-3">For desktop PCs. Laptops need a model-specific service guide.</p>
        </main>
      </div>
    </div>
  )
}

function Choice({ icon, title, text, need, onClick }: { icon: ReactNode; title: string; text: string; need: string; onClick: () => void }) {
  return (
    <li>
      <button
        onClick={onClick}
        className={`group flex h-full w-full flex-col rounded-[24px] bg-panel p-5 text-left ring-1 ring-line transition-shadow duration-150 hover:shadow-lg hover:ring-line-strong ${press} ${focus}`}
      >
        <span className="grid size-12 place-items-center rounded-2xl bg-fill text-accent">{icon}</span>
        <span className="mt-4 text-[20px] leading-tight font-semibold tracking-[-0.015em] text-ink">{title}</span>
        <span className="mt-2 text-[14.5px] leading-[1.5] text-ink-2">{text}</span>
        <span className="mt-4 rounded-xl bg-surface px-3 py-2.5 text-[13px] leading-[1.45] text-ink-2">
          <strong className="block text-[12px] font-semibold tracking-[0.04em] text-ink-3 uppercase">You will need</strong>
          {need}
        </span>
        <span className="mt-auto flex items-center gap-1.5 pt-5 text-[14px] font-medium text-link">
          Start
          <ArrowRight size={14} aria-hidden className="transition-transform duration-150 group-hover:translate-x-0.5" />
        </span>
      </button>
    </li>
  )
}
