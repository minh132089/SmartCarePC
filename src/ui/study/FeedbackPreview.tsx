import { useState } from 'react'
import LatticeLoader from '../../components/LatticeLoader'
import { FeedbackOverview } from './FeedbackOverview'
import type { FeedbackRow } from './feedbackStats'

// Chỉ hiển thị khi build VITE_UI_PREVIEW=true; không đọc hay ghi database.
const occupations = ['Sinh viên', 'Học sinh', 'Nhân viên văn phòng', 'Thiết kế / sáng tạo', 'Công nghệ thông tin', 'Kinh doanh / tự do']
const rows: FeedbackRow[] = Array.from({ length: 31 }, (_, i) => ({
  id: i + 1,
  createdAt: `2026-10-${String(12 + i % 14).padStart(2, '0')} 12:00:00`,
  participantCode: null, phase: null, machine: 'gaming',
  answers: { participant_name: `Người tham gia ${i + 1}`, participant_age: 16 + i % 30,
    participant_occupation: occupations[i % occupations.length], stars: i % 5 + 1,
    budget_fit: 3 + i % 3, purpose_fit: 3 + i % 3, guide: 4 + i % 2, confidence: 3 + i % 3, next_time: 3 + i % 3 },
}))

export default function FeedbackPreview() {
  const [loading, setLoading] = useState(false)
  return <div className="feedback-dashboard h-full overflow-y-auto bg-canvas"><main className="mx-auto max-w-[1240px] px-5 py-8">
    <a href="/" className="text-[13px] text-link hover:underline">← Về SmartCare PC</a>
    <p className="mt-6 text-[11px] font-semibold tracking-[.12em] text-ink-3">BẢN XEM GIAO DIỆN · DỮ LIỆU MINH HỌA</p>
    <h1 className="display mt-2">Dữ liệu phản hồi</h1>
    <p className="mt-3 max-w-[600px] text-[14px] text-ink-2">Các số liệu bên dưới được tạo để xem trước biểu đồ và bố cục. Bản preview này không kết nối backend và không lưu phản hồi lên máy chủ.</p>
    <button className="mt-5 rounded-full bg-fill px-4 py-2 text-[13px] font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent" onClick={() => setLoading((s) => !s)}>{loading ? 'Dừng xem thử loader' : 'Xem thử loader của AI'}</button>
    {loading && <div className="mt-4 text-ink-2"><LatticeLoader label="Đang tìm linh kiện · trạng thái minh họa" pattern="dots" fontSize={13} /></div>}
    <FeedbackOverview rows={rows} />
  </main></div>
}
