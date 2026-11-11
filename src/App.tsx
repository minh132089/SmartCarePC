import { Fade } from '@animate-ui/react/primitives/effects/fade'
import { Scene } from './scene/Scene'
import { useViewer } from './store'
import { AdvicePage } from './ui/AdvicePage'
import { AssemblySteps } from './ui/AssemblySteps'
import { ChatPanel } from './ui/ChatPanel'
import { GuideCaption, GuidePanel } from './ui/GuidePanel'
import { HomePage } from './ui/HomePage'
import { InspectButton } from './ui/InspectButton'
import { Inspector } from './ui/Inspector'
import { Sidebar } from './ui/Sidebar'
import { StudyDialogs } from './ui/study/StudyBar'
import { UpgradePage } from './ui/UpgradePage'

export default function App() {
  const guide = useViewer((s) => s.guide)
  const page = useViewer((s) => s.page)
  const machine = useViewer((s) => `${s.familyId ?? s.buildId}:${s.guideKind}:${s.maintenanceTask}:${s.maintenanceCooler}:${s.maintenanceHasGpu}:${s.maintenanceRunId}`)
  // Màn đầu chọn hướng; tư vấn máy mới và tư vấn nâng cấp không vẽ cảnh 3D; trang build là mô hình 3D kèm hướng dẫn
  if (page !== 'build')
    return (
      <>
        <Fade key={page} className="h-full">
          {page === 'home' ? <HomePage /> : page === 'upgrade' ? <UpgradePage /> : <AdvicePage />}
        </Fade>
        <StudyDialogs />
      </>
    )
  return (
    <div className="flex h-full flex-col-reverse bg-canvas md:flex-row">
      {guide ? <GuidePanel key={machine} /> : <Sidebar />}
      {/* Khung 3D như ảnh sản phẩm: thẻ bo góc lớn, nền trắng loang nhẹ */}
      <main className="relative m-3 min-h-0 flex-1 overflow-hidden rounded-[28px] bg-stage md:my-4 md:mr-4 md:ml-0">
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
