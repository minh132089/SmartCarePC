
import { useState } from 'react'

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

import MaintenanceSection from './ui/MaintenanceSection'
import UpgradeSection from './ui/UpgradeSection'

type FeaturePage = 'maintenance' | 'upgrade' | null

export default function App() {
  const guide = useViewer((s) => s.guide)
  const page = useViewer((s) => s.page)

  const [featurePage, setFeaturePage] = useState<FeaturePage>(null)

  // Display Maintenance or Upgrade page
  if (featurePage !== null) {
    return (
      <div className="min-h-screen bg-slate-50">
        <header className="sticky top-0 z-50 flex items-center justify-between gap-3 border-b bg-white px-4 py-3 shadow-sm">
          <button
            onClick={() => setFeaturePage(null)}
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100"
          >
            ← Back to SmartCarePC
          </button>

          <div className="flex gap-2">
            <button
              onClick={() => setFeaturePage('maintenance')}
              className={`rounded-lg px-3 py-2 text-sm font-semibold ${
                featurePage === 'maintenance'
                  ? 'bg-blue-600 text-white'
                  : 'bg-slate-100 text-slate-700'
              }`}
            >
              Maintenance
            </button>

            <button
              onClick={() => setFeaturePage('upgrade')}
              className={`rounded-lg px-3 py-2 text-sm font-semibold ${
                featurePage === 'upgrade'
                  ? 'bg-blue-600 text-white'
                  : 'bg-slate-100 text-slate-700'
              }`}
            >
              Upgrade
            </button>
          </div>
        </header>

        {featurePage === 'maintenance' ? (
          <MaintenanceSection />
        ) : (
          <UpgradeSection />
        )}
      </div>
    )
  }

  // Main navigation buttons
  const featureNavigation = (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2">
      <button
        onClick={() => setFeaturePage('maintenance')}
        className="rounded-full bg-blue-600 px-5 py-3 text-sm font-semibold text-white shadow-lg transition hover:bg-blue-700"
      >
        🔧 Maintenance
      </button>

      <button
        onClick={() => setFeaturePage('upgrade')}
        className="rounded-full bg-emerald-600 px-5 py-3 text-sm font-semibold text-white shadow-lg transition hover:bg-emerald-700"
      >
        ⚙️ Upgrade
      </button>
    </div>
  )

  // Page 1: PC recommendation
  if (page === 'advice') {
    return (
      <>
        <AdvicePage />
        <StudyDialogs />
        {featureNavigation}
      </>
    )
  }

  // Page 2: 3D PC assembly
  return (
    <div className="flex h-full flex-col-reverse bg-canvas md:flex-row">
      {guide ? <GuidePanel /> : <Sidebar />}

      <main className="relative m-3 min-h-0 flex-1 overflow-hidden rounded-[28px] bg-stage md:my-4 md:mr-4 md:ml-0">
        <Scene />
        <AssemblySteps />
        <GuideCaption />
        <InspectButton />
        <Inspector />
        <ChatPanel />
      </main>

      <StudyDialogs />
      {featureNavigation}
    </div>
  )
}
