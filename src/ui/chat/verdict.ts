import type { VerdictStatus } from './chatStore'

/** Cách hiện trạng thái phương án cho người không rành kỹ thuật (docs/pc-build-verifier, mục 1.3) */
export const VERDICT_INFO: Record<VerdictStatus, { label: string; tone: string }> = {
  READY: { label: "Ready to buy", tone: 'bg-ok-bg text-ok' },
  CONDITIONAL: { label: "Confirm a few details", tone: 'bg-warn-bg text-warn' },
  HOLD: { label: "More information needed", tone: 'bg-fill text-ink-2' },
  BLOCKED: { label: "Not ready to buy", tone: 'bg-bad-bg text-bad' },
}
