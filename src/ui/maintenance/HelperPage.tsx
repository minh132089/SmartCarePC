import type { ReactNode } from 'react'
import { BackHome } from '../BackHome'
import { FeedbackButton } from '../study/FeedbackButton'
import { focus, press } from '../study/StudyParts'
import { useStudy } from '../study/study'
import { SendPlanButton } from './MaintenanceHub'
import { usePlanLink, useMaintenance } from './maintenanceState'

/**
 * Link tải bộ cài SmartCarePC Helper. Để trống có chủ ý: Minh chưa chọn nơi để file cài (GitHub Releases hay chỗ khác).
 * Có link thì điền vào đây, trang tự hiện nút tải. Không tự tạo release hay đăng file lên đâu.
 */
const DOWNLOAD_URL = ''

/**
 * Trang /helper: giới thiệu app khay Windows cho người không rành công nghệ. Ghi rõ app đọc gì, không gửi gì đi trừ khi
 * đang tham gia nghiên cứu, cài là tuỳ chọn, cảnh báo SmartScreen và cách gỡ. Nội dung khớp desktop-agent/README.md.
 */
export function HelperPage() {
  const link = usePlanLink()
  const inStudy = useStudy(s => !!s.code)
  return <div className="h-full overflow-y-auto bg-canvas">
    <header className="flex items-center justify-between gap-3 px-4 pt-5 pb-2 md:px-8">
      <BackHome />
      <FeedbackButton />
    </header>
    <main className="mx-auto max-w-[720px] px-4 pb-16 md:px-8">
      <h1 className="mt-6 text-[clamp(1.8rem,1.3rem+2vw,2.6rem)] leading-[1.1] font-bold tracking-[-0.03em] text-ink">Desktop reminders for Windows</h1>
      <p className="mt-3 text-[15.5px] leading-[1.55] text-ink-2">
        SmartCarePC Helper is a small, free app that sits next to the clock on your taskbar. It reminds you on the dates you
        planned here, even when your browser is closed. It also watches for a few warning signs, such as a PC that keeps running
        hot, and tells you which guide to follow.
      </p>
      <p className="mt-3 rounded-xl bg-info-bg p-3 text-[14px] text-info">
        Installing it is optional. Everything on this website works without it, and you can still add reminders to your calendar instead.
      </p>

      <Section title="1. Download">
        {DOWNLOAD_URL
          ? <a href={DOWNLOAD_URL} className={`inline-block rounded-full bg-accent px-4 py-2 text-[14px] font-medium text-white hover:bg-accent-hover ${press} ${focus}`}>Download SmartCarePC Helper</a>
          : <p className="rounded-xl bg-surface p-3 text-[14px] text-ink-2 ring-1 ring-line"><strong className="text-ink">The download is not available yet.</strong> We will add the link here when the installer is published.</p>}
        <p className="mt-2 text-[13px] text-ink-3">For Windows 10 or 11. You do not need an administrator password: it installs only for your Windows account.</p>
      </Section>

      <Section title="2. Open the installer">
        <p>Windows may show a blue box that says <strong>“Windows protected your PC”</strong>. This happens because the app is new and not signed with a paid certificate, not because a problem was found.</p>
        <ol className="mt-3 list-decimal space-y-1 pl-5">
          <li>Click <strong>More info</strong> (small text under the message).</li>
          <li>Click the <strong>Run anyway</strong> button that appears.</li>
        </ol>
        <SmartScreenSketch />
        <p className="mt-3">If you are unsure, stop and ask someone you trust. You do not have to install it.</p>
      </Section>

      <Section title="3. Send your plan to the app">
        {link ? <>
          <p>Press the button below. Your browser asks whether to open SmartCarePC Helper: choose <strong>Open</strong>. A notification saying “Your reminders are on” confirms it worked.</p>
          <div className="mt-3"><SendPlanButton primary /></div>
          <p className="mt-2 text-[13px] text-ink-3">After you finish a guide, send your plan again so the app knows your new dates. If nothing happens when you press it, the app is not installed yet.</p>
        </> : <>
          <p>First choose your PC and plan your maintenance dates on this website. Then come back here, or use the button in the reminder window.</p>
          <button onClick={() => useMaintenance.getState().open()} className={`mt-3 rounded-full bg-accent px-4 py-2 text-[14px] font-medium text-white hover:bg-accent-hover ${press} ${focus}`}>Set up my reminders</button>
        </>}
      </Section>

      <Section title="What the app reads on your PC">
        <ul className="list-disc space-y-1 pl-5">
          <li>Your maintenance dates and PC type, only when you send them from this website.</li>
          <li>Processor and memory use, and graphics card temperature where available.</li>
          <li>Processor temperature and fan speed. On many PCs Windows does not allow this without administrator rights; the app then skips those checks and never asks for more access.</li>
          <li>Disk health and free space on your main drive.</li>
          <li>The Windows log of unexpected shutdowns.</li>
        </ul>
        <p className="mt-2">It does not read your files, browsing, passwords, or your name. Readings stay on your PC in a folder for your account only.</p>
      </Section>

      <Section title="What the app sends">
        <p><strong>Nothing</strong>, unless you are taking part in the SmartCarePC study with a participant code{inStudy ? ' (you are, on this browser)' : ''}.</p>
        <p className="mt-2">For study participants only, it sends short anonymous notes to the SmartCarePC study server: that a reminder or warning appeared, which kind (for example “PC running hot”, with rounded readings), and whether you pressed Start guide or Remind me later. These notes carry your participant code, never your name, email, or anything that identifies your PC.</p>
        <p className="mt-2">Outside the study the app has no internet connection at all. It only opens this website when you press a notification.</p>
      </Section>

      <Section title="Pause or remove it">
        <ul className="list-disc space-y-1 pl-5">
          <li><strong>Pause:</strong> right-click the SmartCarePC icon near the clock and choose Pause reminders. You can also turn off Start with Windows there.</li>
          <li><strong>Remove:</strong> open Windows <strong>Settings → Apps → Installed apps</strong>, find SmartCarePC Helper, then choose Uninstall. Windows asks whether to also delete your saved plan and readings.</li>
        </ul>
      </Section>
    </main>
  </div>
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return <section className="mt-6 rounded-[20px] bg-panel p-5 text-[14.5px] leading-[1.55] text-ink-2 ring-1 ring-line">
    <h2 className="mb-2 text-[17px] font-semibold text-ink">{title}</h2>
    {children}
  </section>
}

/**
 * Hình vẽ đơn giản hộp cảnh báo SmartScreen, chỉ đúng chỗ cần bấm. Không phải ảnh chụp: chưa có máy Windows để chụp,
 * khi Minh chụp được ảnh thật (README, việc cần thử số 1) thì thay bằng ảnh.
 */
function SmartScreenSketch() {
  const box = 'rounded-lg bg-[#1f5fa8] p-3 text-[12px] leading-snug text-white'
  return <figure className="mt-4">
    <div className="grid gap-3 sm:grid-cols-2" aria-hidden="true">
      <div className={box}>
        <p className="text-[15px] font-semibold">Windows protected your PC</p>
        <p className="mt-1 opacity-90">Microsoft Defender SmartScreen prevented an unrecognised app from starting…</p>
        <p className="mt-2 inline-block rounded px-1 font-semibold underline ring-2 ring-[#ffd400]">More info</p>
        <div className="mt-3 flex justify-end"><span className="rounded bg-white/15 px-3 py-1">Don’t run</span></div>
      </div>
      <div className={box}>
        <p className="text-[15px] font-semibold">Windows protected your PC</p>
        <p className="mt-1 opacity-90">App: SmartCarePCHelper-Setup.exe<br />Publisher: Unknown publisher</p>
        <div className="mt-3 flex justify-end gap-2"><span className="rounded bg-white/15 px-3 py-1 ring-2 ring-[#ffd400]">Run anyway</span><span className="rounded bg-white/15 px-3 py-1">Don’t run</span></div>
      </div>
    </div>
    <figcaption className="mt-2 text-[12px] text-ink-3">Simplified drawing: first click More info (left), then Run anyway (right).</figcaption>
  </figure>
}
