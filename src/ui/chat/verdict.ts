import type { VerdictStatus } from './chatStore'

/** Cách hiện trạng thái phương án cho người không rành kỹ thuật (docs/pc-build-verifier, mục 1.3) */
export const VERDICT_INFO: Record<VerdictStatus, { label: string; tone: string }> = {
  READY: { label: 'Sẵn sàng mua', tone: 'bg-ok-bg text-ok' },
  CONDITIONAL: { label: 'Cần xác nhận vài điểm', tone: 'bg-warn-bg text-warn' },
  HOLD: { label: 'Chưa đủ thông tin', tone: 'bg-fill text-ink-2' },
  BLOCKED: { label: 'Chưa mua được', tone: 'bg-bad-bg text-bad' },
}
