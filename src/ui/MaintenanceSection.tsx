
import { useState } from 'react'

const maintenanceTasks = [
  {
    id: 'dust',
    title: 'Clean Dust Inside the PC Case',
    detail: 'Turn off the PC, unplug the power cable, and use compressed air or a soft brush to clean components.',
    interval: 'Every 3–6 months',
  },
  {
    id: 'filter',
    title: 'Clean Dust Filters',
    detail: 'Remove the dust filters, clean them, and let them dry completely before reinstalling.',
    interval: 'Every month',
  },
  {
    id: 'temperature',
    title: 'Check CPU and GPU Temperatures',
    detail: 'Monitor temperatures while idle and under heavy workloads.',
    interval: 'Every month',
  },
  {
    id: 'fan',
    title: 'Check Cooling Fans',
    detail: 'Check for unusual noise, fan speed issues, and proper airflow inside the case.',
    interval: 'Every month',
  },
  {
    id: 'cable',
    title: 'Inspect Power and Data Cables',
    detail: 'Check cable connections and make sure no cables interfere with cooling fans.',
    interval: 'Every 6 months',
  },
  {
    id: 'backup',
    title: 'Back Up Important Data',
    detail: 'Back up important documents to another drive or a reliable cloud storage service.',
    interval: 'Every month',
  },
]

export function MaintenanceSection() {
  const [completed, setCompleted] = useState<string[]>([])

  const progress = Math.round(
    (completed.length / maintenanceTasks.length) * 100
  )

  const toggleTask = (id: string) => {
    setCompleted((current) =>
      current.includes(id)
        ? current.filter((item) => item !== id)
        : [...current, id]
    )
  }

  return (
    <section className="w-full space-y-5">
      <div>
        <h2 className="text-2xl font-bold">PC Maintenance</h2>
        <p className="mt-2 text-sm text-gray-500">
          Track and complete regular computer maintenance tasks.
        </p>
      </div>

      <div className="rounded-2xl border bg-white p-5 shadow-sm">
        <div className="flex justify-between">
          <span className="font-semibold">Maintenance Progress</span>
          <span className="font-semibold text-blue-600">
            {progress}%
          </span>
        </div>

        <div className="mt-3 h-3 overflow-hidden rounded-full bg-gray-200">
          <div
            className="h-full rounded-full bg-blue-600 transition-all"
            style={{ width: `${progress}%` }}
          />
        </div>

        <p className="mt-2 text-sm text-gray-500">
          {completed.length}/{maintenanceTasks.length} tasks completed
        </p>
      </div>

      <div className="space-y-3">
        {maintenanceTasks.map((task) => (
          <label
            key={task.id}
            className="flex cursor-pointer gap-3 rounded-xl border bg-white p-4 shadow-sm"
          >
            <input
              type="checkbox"
              checked={completed.includes(task.id)}
              onChange={() => toggleTask(task.id)}
              className="mt-1 h-4 w-4 accent-blue-600"
            />

            <div className="flex-1">
              <h3 className="font-semibold">{task.title}</h3>

              <p className="mt-1 text-sm text-gray-500">
                {task.detail}
              </p>

              <span className="mt-2 inline-block rounded-full bg-blue-50 px-3 py-1 text-xs text-blue-600">
                {task.interval}
              </span>
            </div>
          </label>
        ))}
      </div>
    </section>
  )
}
