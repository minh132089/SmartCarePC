import { Scene } from './scene/Scene'
import { AssemblySteps } from './ui/AssemblySteps'
import { InspectButton } from './ui/InspectButton'
import { Inspector } from './ui/Inspector'
import { Sidebar } from './ui/Sidebar'

export default function App() {
  return (
    <div className="flex h-full flex-col-reverse bg-canvas md:flex-row">
      <Sidebar />
      {/* Khung 3D như ảnh sản phẩm: thẻ bo góc lớn, nền trắng loang nhẹ */}
      <main className="relative m-3 min-h-0 flex-1 overflow-hidden rounded-[28px] bg-[#fafafb] md:my-4 md:mr-4 md:ml-0">
        <Scene />
        <AssemblySteps />
        <InspectButton />
        <Inspector />
      </main>
    </div>
  )
}
