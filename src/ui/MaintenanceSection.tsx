
import { useState } from 'react';

const maintenanceTasks = [
  {
    id: 1,
    title: 'Clean the PC case',
    description: 'Remove dust from the case, fans and air filters.',
    frequency: 'Every 1–2 months',
  },
  {
    id: 2,
    title: 'Clean CPU and GPU fans',
    description: 'Check and remove dust from cooling fans.',
    frequency: 'Every 2–3 months',
  },
  {
    id: 3,
    title: 'Check cable connections',
    description: 'Make sure power and data cables are connected securely.',
    frequency: 'Every 3 months',
  },
  {
    id: 4,
    title: 'Check storage health',
    description: 'Check SSD or HDD health and available storage space.',
    frequency: 'Every 3 months',
  },
  {
    id: 5,
    title: 'Update drivers',
    description: 'Keep graphics, chipset and other important drivers updated.',
    frequency: 'When updates are available',
  },
  {
    id: 6,
    title: 'Check system temperature',
    description: 'Monitor CPU and GPU temperatures during normal use.',
    frequency: 'Every month',
  },
];

export default function MaintenanceSection() {
  const [completed, setCompleted] = useState<number[]>([]);

  const toggleTask = (id: number) => {
    setCompleted((current) =>
      current.includes(id)
        ? current.filter((item) => item !== id)
        : [...current, id]
    );
  };

  const progress = Math.round(
    (completed.length / maintenanceTasks.length) * 100
  );

  return (
    <section className="min-h-screen bg-slate-50 p-6 text-slate-900">
      <div className="mx-auto max-w-5xl space-y-6">
        <div>
          <p className="text-sm font-semibold text-blue-600">
            SMARTCAREPC
          </p>
          <h1 className="mt-2 text-3xl font-bold">
            PC Maintenance
          </h1>
          <p className="mt-2 text-slate-600">
            Keep your computer clean, cool and running smoothly.
          </p>
        </div>

        <div className="rounded-2xl border bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-bold">
              Maintenance Progress
            </h2>
            <span className="font-bold text-blue-600">
              {progress}%
            </span>
          </div>

          <div className="mt-3 h-3 overflow-hidden rounded-full bg-slate-200">
            <div
              className="h-full rounded-full bg-blue-600 transition-all"
              style={{ width: `${progress}%` }}
            />
          </div>

          <p className="mt-2 text-sm text-slate-500">
            {completed.length} of {maintenanceTasks.length} tasks completed
          </p>
        </div>

        <div className="rounded-2xl border bg-white p-5 shadow-sm">
          <h2 className="mb-4 text-xl font-bold">
            Maintenance Checklist
          </h2>

          <div className="space-y-3">
            {maintenanceTasks.map((task) => {
              const isDone = completed.includes(task.id);

              return (
                <label
                  key={task.id}
                  className={`flex cursor-pointer gap-4 rounded-xl border p-4 transition ${
                    isDone
                      ? 'border-green-300 bg-green-50'
                      : 'border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={isDone}
                    onChange={() => toggleTask(task.id)}
                    className="mt-1 h-5 w-5 accent-blue-600"
                  />

                  <div className="flex-1">
                    <h3
                      className={`font-semibold ${
                        isDone ? 'text-green-700 line-through' : ''
                      }`}
                    >
                      {task.title}
                    </h3>
                    <p className="mt-1 text-sm text-slate-600">
                      {task.description}
                    </p>
                    <p className="mt-2 text-xs font-medium text-blue-600">
                      Recommended: {task.frequency}
                    </p>
                  </div>

                  {isDone && (
                    <span className="text-sm font-semibold text-green-600">
                      Done
                    </span>
                  )}
                </label>
              );
            })}
          </div>
        </div>

        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5">
          <h2 className="font-bold text-amber-800">
            Safety First
          </h2>
          <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-amber-900">
            <li>Turn off and unplug the PC before cleaning.</li>
            <li>Never spray liquid directly onto computer components.</li>
            <li>Use compressed air carefully when removing dust.</li>
            <li>Be careful with static electricity when handling components.</li>
          </ul>
        </div>
      </div>
    </section>
  );
}
