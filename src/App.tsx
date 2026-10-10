import { Scene } from './scene/Scene'
import { useViewer } from './store'
import { AdvicePage } from './ui/AdvicePage'
import { AssemblySteps } from './ui/AssemblySteps'
import { ChatPanel } from './ui/ChatPanel'
import { GuideCaption, GuidePanel } from './ui/GuidePanel'
import { InspectButton } from './ui/InspectButton'
import { Inspector } from './ui/Inspector'
import { Sidebar } from './ui/Sidebar'

export default function App() {
  const guide = useViewer((s) => s.guide)
  const page = useViewer((s) => s.page)
  // Trang 1 tư vấn chọn máy; trang 2 là mô hình 3D kèm hướng dẫn lắp và khung chat hỏi khi lắp
  if (page === 'advice') return <AdvicePage />
  return (
    <div className="flex h-full flex-col-reverse bg-canvas md:flex-row">
      {guide ? <GuidePanel /> : <Sidebar />}
      {/* Khung 3D như ảnh sản phẩm: thẻ bo góc lớn, nền trắng loang nhẹ */}
      <main className="relative m-3 min-h-0 flex-1 overflow-hidden rounded-[28px] bg-stage md:my-4 md:mr-4 md:ml-0">
        <Scene />
        <AssemblySteps />
        <GuideCaption />
        <InspectButton />
        <Inspector />
        <ChatPanel />
      </main>
    </div>
  )
}
