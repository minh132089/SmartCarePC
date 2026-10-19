/**
 * Form phản hồi của thử nghiệm 4 tuần: 5 câu chấm điểm 1 đến 5 và 2 câu tự luận. Sửa chữ ở đây, giao diện tự đổi theo.
 * key là tên cột trong file CSV xuất ra, đổi key thì dữ liệu cũ và mới không ghép được với nhau.
 */
export type StudyQuestion = {
  key: string
  text: string
  hint?: string
  type: 'single' | 'multi' | 'scale' | 'text'
  options?: { value: string; label: string }[]
  low?: string
  high?: string
  /** Chỉ hiện khi một câu trước có đáp án nhất định, dạng "key=value|key=value" (thoả một điều kiện là hiện) */
  showIf?: string
  placeholder?: string
}

/** Thang hài lòng 1 đến 5, giống thang trong bảng khảo sát để so được với nhau */
const SATISFACTION = { type: 'scale', low: 'Rất không hài lòng', high: 'Rất hài lòng' } as const

export const FEEDBACK: StudyQuestion[] = [
  {
    key: 'budget_fit',
    text: '1. Bạn hài lòng đến đâu với việc cấu hình được gợi ý vừa với ngân sách của bạn?',
    ...SATISFACTION,
  },
  {
    key: 'purpose_fit',
    text: '2. Bạn hài lòng đến đâu với việc các linh kiện được chọn đúng với mục đích bạn dùng máy?',
    ...SATISFACTION,
  },
  {
    key: 'guide',
    text: '3. Bạn hài lòng đến đâu với phần hướng dẫn lắp máy từng bước?',
    ...SATISFACTION,
  },
  {
    key: 'confidence',
    text: '4. Sau khi dùng web app, bạn thấy mình tự tin hơn khi tự chọn và lắp máy đến đâu?',
    hint: '3 là không thay đổi',
    type: 'scale',
    low: 'Kém tự tin hơn nhiều',
    high: 'Tự tin hơn nhiều',
  },
  {
    key: 'next_time',
    text: '5. Lần tới cần chọn, lắp hoặc nâng cấp máy, bạn sẽ tự làm đến đâu?',
    type: 'scale',
    low: 'Nhờ người khác làm hộ',
    high: 'Tự làm, không cần hỏi ai',
  },
  {
    key: 'difficulties',
    text: '6. Trong quá trình sử dụng web app, bạn gặp những khó khăn gì? Có chỗ nào web app chưa giúp được bạn không?',
    type: 'text',
    placeholder: 'Ví dụ: bước nào khó hiểu, chỗ nào phải hỏi người khác',
  },
  {
    key: 'suggestions',
    text: '7. Hãy góp ý cho chúng tôi để web app mang lại trải nghiệm tốt hơn.',
    type: 'text',
    placeholder: 'Viết bằng lời của bạn',
  },
]
