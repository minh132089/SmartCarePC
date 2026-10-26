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
const SATISFACTION = { type: 'scale', low: "Very dissatisfied", high: "Very satisfied" } as const

export const FEEDBACK: StudyQuestion[] = [
  {
    key: 'budget_fit',
    text: "1. How satisfied are you that the recommended build fits your budget?",
    ...SATISFACTION,
  },
  {
    key: 'purpose_fit',
    text: "2. How satisfied are you that the selected components suit your intended use?",
    ...SATISFACTION,
  },
  {
    key: 'guide',
    text: "3. How satisfied are you with the step-by-step assembly guide?",
    ...SATISFACTION,
  },
  {
    key: 'confidence',
    text: "4. After using the app, how has your confidence in choosing and assembling a PC changed?",
    hint: "3 means no change",
    type: 'scale',
    low: "Much less confident",
    high: "Much more confident",
  },
  {
    key: 'next_time',
    text: "5. Next time you need to choose, assemble or upgrade a PC, how independently would you do it?",
    type: 'scale',
    low: "Ask someone else to do it",
    high: "Do it myself without help",
  },
  {
    key: 'difficulties',
    text: "6. What difficulties did you encounter? Was there anything the app could not help you with?",
    type: 'text',
    placeholder: "For example: a confusing step or something you needed help with",
  },
  {
    key: 'suggestions',
    text: "7. What would improve your experience with this app?",
    type: 'text',
    placeholder: "Use your own words",
  },
]
