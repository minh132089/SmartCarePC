import { lazy, Suspense } from 'react'
import App from './App'
import { ParticipantGate } from './ui/study/ParticipantGate'

const FeedbackAdmin = lazy(() => import('./ui/study/FeedbackAdmin').then((m) => ({ default: m.FeedbackAdmin })))
const FeedbackPreview = lazy(() => import('./ui/study/FeedbackPreview'))
const uiPreview = import.meta.env.VITE_UI_PREVIEW === 'true'

export default function Root() {
  const feedbackRoute = location.pathname.replace(/\/$/, '') === '/feedback' || location.hash === '#feedback'
  const sampleRoute = uiPreview && location.pathname === '/feedback-preview'
  if (feedbackRoute || sampleRoute) return (
    <Suspense fallback={<div role="status" className="grid h-full place-items-center text-ink-2">Opening feedback data…</div>}>
      {sampleRoute ? <FeedbackPreview /> : <FeedbackAdmin />}
    </Suspense>
  )
  return <>
    <App />
    <ParticipantGate />
    {uiPreview && <a href="/feedback-preview" className="fixed right-3 bottom-3 z-40 rounded-full border border-line-strong bg-surface px-4 py-2 text-[12px] font-medium text-ink shadow-sm">UI preview · View sample dashboard ↗</a>}
  </>
}
