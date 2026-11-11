import { Scene } from './scene/Scene'
import { useViewer } from './store'
import { AdvicePage } from './ui/AdvicePage'
import { AssemblySteps } from './ui/AssemblySteps'
import { ChatPanel } from './ui/ChatPanel'
import { GuideCaption, GuidePanel } from './ui/GuidePanel'
import { InspectButton } from './ui/InspectButton'
import { Inspector } from './ui/Inspector'
import { Sidebar } from './ui/Sidebar'
import { StudyDialogs } from './ui/study/StudyBar'
import { AuthPage, clearSession, loadSession, type Session } from './ui/AuthPage'
import { useState } from 'react'
import { AccountMenu } from './ui/AccountMenu'

export default function App() {
  const [session, setSession] = useState<Session | null>(() => loadSession())
  const guide = useViewer((s) => s.guide)
  const page = useViewer((s) => s.page)
  if (!session) return <AuthPage onLogin={setSession} />
  const account = <AccountMenu session={session} onLogout={() => { clearSession(); setSession(null) }} />
  // Trang 1 tư vấn chọn máy; trang 2 là mô hình 3D kèm hướng dẫn lắp và khung chat hỏi khi lắp
  if (page === 'advice')
    return (
      <>
        <AdvicePage account={account} />
        <StudyDialogs />
      </>
    )
  return (
    <div className="flex h-full flex-col-reverse bg-canvas md:flex-row">
      {guide ? <GuidePanel /> : <Sidebar />}
      {/* Khung 3D như ảnh sản phẩm: thẻ bo góc lớn, nền trắng loang nhẹ */}
      <main className="relative m-3 min-h-0 flex-1 overflow-hidden rounded-[28px] bg-stage md:my-4 md:mr-4 md:ml-0">
        <div className="absolute right-4 top-4 z-30">{account}</div>
        <Scene />
        <AssemblySteps />
        <GuideCaption />
        <InspectButton />
        <Inspector />
        <ChatPanel />
      </main>
      <StudyDialogs />
    </div>
  )
}
