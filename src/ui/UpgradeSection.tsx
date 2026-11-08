
import { useState } from 'react';

const upgrades = [
  {
    id: 'ram',
    name: 'RAM Upgrade',
    description: 'Increase memory capacity for better multitasking.',
    benefits: ['Run more applications', 'Improve multitasking', 'Reduce memory bottlenecks'],
    advice: 'Check DDR generation, motherboard compatibility and maximum RAM capacity.',
  },
  {
    id: 'ssd',
    name: 'SSD Upgrade',
    description: 'Replace an HDD or older SSD with a faster storage drive.',
    benefits: ['Faster boot time', 'Shorter loading time', 'Faster file transfers'],
    advice: 'Check M.2 or SATA support, drive size and available motherboard slots.',
  },
  {
    id: 'gpu',
    name: 'GPU Upgrade',
    description: 'Improve graphics performance for gaming and creative work.',
    benefits: ['Higher gaming performance', 'Better rendering', 'Support for demanding applications'],
    advice: 'Check case clearance, PSU wattage, power connectors and motherboard compatibility.',
  },
  {
    id: 'cpu',
    name: 'CPU Upgrade',
    description: 'Improve processing performance with a compatible processor.',
    benefits: ['Faster processing', 'Better application performance', 'Improved multitasking'],
    advice: 'Check CPU socket, motherboard support, BIOS version and CPU cooling.',
  },
  {
    id: 'psu',
    name: 'PSU Upgrade',
    description: 'Provide sufficient and stable power for upgraded components.',
    benefits: ['More power capacity', 'Support for stronger GPUs', 'Improved power headroom'],
    advice: 'Check total system power requirements, PSU quality and cable connectors.',
  },
];

export default function UpgradeSection() {
  const [selected, setSelected] = useState('ram');
  const [checked, setChecked] = useState<string[]>([]);

  const currentUpgrade = upgrades.find(
    (item) => item.id === selected
  )!;

  const compatibilityItems = [
    'Check motherboard compatibility',
    'Check physical dimensions',
    'Check power requirements',
    'Check connectors and slots',
    'Check cooling requirements',
  ];

  const toggleCheck = (item: string) => {
    setChecked((current) =>
      current.includes(item)
        ? current.filter((value) => value !== item)
        : [...current, item]
    );
  };

  return (
    <section className="min-h-screen bg-slate-50 p-6 text-slate-900">
      <div className="mx-auto max-w-5xl space-y-6">
        <div>
          <p className="text-sm font-semibold text-blue-600">
            SMARTCAREPC
          </p>
          <h1 className="mt-2 text-3xl font-bold">
            PC Upgrade Guide
          </h1>
          <p className="mt-2 text-slate-600">
            Learn how to choose compatible components for your PC upgrade.
          </p>
        </div>

        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {upgrades.map((item) => (
            <button
              key={item.id}
              onClick={() => setSelected(item.id)}
              className={`rounded-xl border p-4 text-left transition ${
                selected === item.id
                  ? 'border-blue-500 bg-blue-50 ring-2 ring-blue-200'
                  : 'border-slate-200 bg-white hover:border-blue-300'
              }`}
            >
              <h2 className="font-bold">{item.name}</h2>
              <p className="mt-2 text-sm text-slate-600">
                {item.description}
              </p>
            </button>
          ))}
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          <div className="rounded-2xl border bg-white p-5 shadow-sm">
            <h2 className="text-xl font-bold">
              {currentUpgrade.name}
            </h2>

            <p className="mt-2 text-slate-600">
              {currentUpgrade.description}
            </p>

            <h3 className="mt-5 font-semibold">
              Main Benefits
            </h3>

            <ul className="mt-3 space-y-2">
              {currentUpgrade.benefits.map((benefit) => (
                <li key={benefit} className="flex gap-2 text-sm">
                  <span className="text-green-600">✓</span>
                  {benefit}
                </li>
              ))}
            </ul>

            <div className="mt-5 rounded-xl bg-blue-50 p-4">
              <h3 className="font-semibold text-blue-800">
                Compatibility Advice
              </h3>
              <p className="mt-2 text-sm text-blue-900">
                {currentUpgrade.advice}
              </p>
            </div>
          </div>

          <div className="rounded-2xl border bg-white p-5 shadow-sm">
            <h2 className="text-xl font-bold">
              Upgrade Compatibility Checklist
            </h2>

            <p className="mt-2 text-sm text-slate-600">
              Check these requirements before purchasing new hardware.
            </p>

            <div className="mt-5 space-y-3">
              {compatibilityItems.map((item) => (
                <label
                  key={item}
                  className="flex cursor-pointer items-center gap-3 rounded-lg border p-3 hover:bg-slate-50"
                >
                  <input
                    type="checkbox"
                    checked={checked.includes(item)}
                    onChange={() => toggleCheck(item)}
                    className="h-5 w-5 accent-blue-600"
                  />
                  <span className="text-sm">{item}</span>
                </label>
              ))}
            </div>

            <p className="mt-4 text-sm font-medium text-blue-600">
              {checked.length} / {compatibilityItems.length} checks completed
            </p>
          </div>
        </div>

        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5">
          <h2 className="font-bold text-amber-800">
            Before You Upgrade
          </h2>
          <p className="mt-2 text-sm text-amber-900">
            Always verify component compatibility with your motherboard,
            power supply and PC case before purchasing. Turn off and unplug
            your computer before installing hardware.
          </p>
        </div>
      </div>
    </section>
  );
}
