import { useViewer } from './store'
import { resumeRun, useMaintenance } from './ui/maintenance/maintenanceState'

/**
 * Đồng bộ màn hình đang mở với địa chỉ trang (QA 23/11 BUG-08): mỗi màn có một đường dẫn, nút Back / Forward của
 * trình duyệt đổi màn trong app thay vì thoát hẳn, tải lại trang (F5) mở lại đúng màn đang xem.
 * Không dùng thư viện router: app chỉ có vài màn và trạng thái màn nằm sẵn trong store.
 * Cloudflare Pages và `vite preview` đều trả index.html cho đường dẫn không có file, nên mở thẳng /maintain vẫn chạy.
 */
type Route = 'home' | 'advice' | 'upgrade' | 'model' | 'assemble' | 'maintain' | 'maintainModel'

const PATH: Record<Route, string> = {
  home: '/',
  advice: '/build',
  upgrade: '/upgrade',
  model: '/3d',
  assemble: '/3d/assemble',
  maintain: '/maintain',
  // Tạm đóng bài bảo dưỡng để xem mô hình: vẫn là ngữ cảnh bảo dưỡng, tải lại thì về đúng bài đó
  maintainModel: '/maintain/3d',
}

const runInProgress = () => {
  const m = useMaintenance.getState()
  return !!m.pc && !!m.run && !m.run.completed
}

function current(): Route {
  const s = useViewer.getState()
  if (s.page !== 'build') return s.page
  if (s.guide) return s.guideKind === 'maintenance' ? 'maintain' : 'assemble'
  return s.guideKind === 'maintenance' && runInProgress() ? 'maintainModel' : 'model'
}

function fromPath(pathname: string): Route | null {
  const clean = pathname.replace(/\/+$/, '') || '/'
  return (Object.keys(PATH) as Route[]).find((r) => PATH[r] === clean) ?? null
}

/** Đưa app tới màn của đường dẫn (khi tải trang hoặc bấm Back / Forward). */
function open(route: Route) {
  const v = useViewer.getState()
  switch (route) {
    case 'home':
    case 'advice':
    case 'upgrade':
      v.setPage(route)
      return
    case 'model':
      useViewer.setState({ page: 'build' })
      if (v.guide || v.guideKind !== 'assembly') v.openAssembly()
      return
    case 'assemble':
      useViewer.setState({ page: 'build' })
      if (!v.guide || v.guideKind !== 'assembly') v.setGuide(true)
      return
    case 'maintain':
      if (v.page === 'build' && v.guide && v.guideKind === 'maintenance') return
      // Mở tiếp bài đang làm dở, đúng máy của bài. Không còn bài dở thì về trang chủ với hộp chọn bài
      if (runInProgress()) return resumeRun()
      v.setPage('home')
      useMaintenance.getState().open()
      return
    case 'maintainModel':
      if (v.page === 'build' && !v.guide && v.guideKind === 'maintenance') return
      if (!runInProgress()) return open('model')
      resumeRun()
      useViewer.getState().setGuide(false) // đóng bài nhưng giữ ngữ cảnh bảo dưỡng
  }
}

let started = false
export function startRouting() {
  if (started || typeof window === 'undefined') return
  started = true
  const first = fromPath(location.pathname)
  if (first && first !== 'home') open(first)
  // Giữ query (mã người tham gia, link mở bài bảo dưỡng từ lịch) để phần đọc chúng vẫn thấy
  history.replaceState(null, '', PATH[current()] + location.search + location.hash)
  let shown = current()
  // Đang áp màn theo nút Back / Forward: các trạng thái trung gian (vd đã sang trang 3D nhưng chưa đóng hướng dẫn)
  // không được ghi thêm vào lịch sử
  let applying = false

  /*
   * Hộp chọn bài bảo dưỡng / đặt lịch nhắc: mở hộp thì thêm một mục lịch sử (cùng địa chỉ), để Back đóng hộp thay vì thoát
   * khỏi app hay đổi màn phía sau mà để hộp còn mở.
   * Đóng hộp bằng nút trong hộp: nếu ngay sau đó app sang màn khác (vd bắt đầu bài bảo dưỡng) thì màn mới thay chỗ mục của
   * hộp; nếu không thì lùi bỏ mục đó, để Back lần sau không phải bấm hai lần.
   */
  let modalEntry = false
  let staleModal = false
  let skipPop = false
  let closingFromPop = false
  useMaintenance.subscribe((s, prev) => {
    if (s.dialog && !prev.dialog && !modalEntry) {
      history.pushState({ modal: true }, '', location.pathname + location.search)
      modalEntry = true
    } else if (!s.dialog && prev.dialog && modalEntry) {
      modalEntry = false
      if (closingFromPop) return
      staleModal = true
      window.setTimeout(() => {
        if (!staleModal) return
        staleModal = false
        skipPop = true
        history.back()
      }, 0)
    }
  })

  window.addEventListener('popstate', () => {
    if (skipPop) {
      skipPop = false
      return
    }
    // Back khi hộp đang mở: chỉ đóng hộp, giữ nguyên màn
    if (modalEntry && useMaintenance.getState().dialog) {
      closingFromPop = true
      useMaintenance.getState().close()
      closingFromPop = false
      return
    }
    const r = fromPath(location.pathname) ?? 'home'
    applying = true
    try {
      open(r)
    } finally {
      applying = false
    }
    // Màn đích không mở được (vd /maintain nhưng không còn bài nào) thì sửa lại địa chỉ cho khớp
    shown = current()
    if (shown !== r) history.replaceState(null, '', PATH[shown])
  })

  const sync = () => {
    const r = current()
    if (applying || r === shown) return
    shown = r
    if (staleModal) {
      // Hộp vừa đóng và app sang màn mới: màn mới thay chỗ mục lịch sử của hộp
      staleModal = false
      history.replaceState(null, '', PATH[r])
    } else history.pushState(null, '', PATH[r])
  }
  useViewer.subscribe(sync)
  // Bài bảo dưỡng xong / bắt đầu cũng đổi được màn (mô hình thường ↔ mô hình trong ngữ cảnh bảo dưỡng)
  useMaintenance.subscribe(sync)
}
