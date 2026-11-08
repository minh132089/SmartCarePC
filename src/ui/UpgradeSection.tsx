
import { useState } from 'react'

const upgradeItems = [
  {
    id: 'ram',
    name: 'RAM Upgrade',
    icon: '💾',
    description: 'Improve multitasking performance and run multiple applications smoothly.',
    checks: [
      'Check DDR4 or DDR5 compatibility.',
      'Check the number of available RAM slots.',
      'Check the maximum RAM capacity supported by the motherboard.',
      'Check supported RAM speeds.',
    ],
  },
  {
    id: 'ssd',
    name: 'SSD Upgrade',
    icon: '⚡',
    description: 'Improve boot times, application loading, and overall system responsiveness.',
    checks: [
      'Check for available M.2 or SATA connections.',
      'Determine whether the SSD uses NVMe or SATA.',
      'Check the supported SSD form factor.',
      'Back up important data before replacing the drive.',
    ],
  },
  {
    id: 'gpu',
    name: 'GPU Upgrade',
    icon: '🎮',
    description: 'Improve gaming performance and graphics processing capabilities.',
    checks: [
      'Check the graphics card dimensions.',
      'Check the power supply wattage.',
      'Check the required GPU power connectors.',
      'Verify compatibility with the motherboard.',
    ],
  },
  {
    id: 'cpu',
    name: 'CPU Upgrade',
    icon: '🧠',
    description: 'Improve the overall processing performance of your PC.',
    checks: [
      'Check CPU socket compatibility.',
      'Check the motherboard CPU support list.',
      'Check the required BIOS version.',
      'Check cooling system compatibility.',
    ],
  },
  {
    id: 'psu',
    name: 'Power Supply Upgrade',
    icon: '🔌',
    description: 'Ensure your system receives sufficient and stable power.',
    checks: [
      'Calculate the required system power.',
      'Check PSU dimensions and case compatibility.',
      'Check all required power connectors.',
      'Do not reuse modular cables from an old PSU unless compatibility is confirmed by the manufacturer.',
    ],
  },
]

export function UpgradeSection() {
  const [selected, setSelected] = useState<string | null>(null)

  return (
    <section className="w-full space-y-5">
      <div>
        <h2 className="text-2xl font-bold">PC Upgrades</h2>
        <p className="mt-2 text-sm text-gray-500">
          Explore hardware upgrades to improve your computer's performance.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        {upgradeItems.map((item) => (
          <div
            key={item.id}
            className="rounded-2xl border bg-white p-5 shadow-sm"
          >
            <div className="flex items-center gap-3">
              <span className="text-3xl">{item.icon}</span>
              <h3 className="font-bold">{item.name}</h3>
            </div>

            <p className="mt-3 text-sm text-gray-600">
              {item.description}
            </p>

            <button
              onClick={() =>
                setSelected(selected === item.id ? null : item.id)
              }
              className="mt-4 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700"
            >
              {selected === item.id
                ? 'Hide Details'
                : 'View Upgrade Guide'}
            </button>

            {selected === item.id && (
              <div className="mt-4 rounded-xl bg-gray-50 p-4">
                <h4 className="mb-3 font-semibold">
                  Compatibility Checklist:
                </h4>

                <ul className="space-y-2">
                  {item.checks.map((check, index) => (
                    <li
                      key={index}
                      className="flex gap-2 text-sm text-gray-600"
                    >
                      <span className="text-green-600">✓</span>
                      {check}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        ))}
      </div>
    </section>
  )
}
