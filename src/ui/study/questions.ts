/**
 * Câu hỏi của chế độ nghiên cứu. Sửa chữ ở đây, giao diện tự đổi theo.
 * key là tên cột trong file CSV xuất ra, đổi key thì dữ liệu cũ và mới không ghép được với nhau.
 *
 * Có ba form:
 * - BASELINE: lần đầu, hiện ngay sau khi nhập mã, trước khi xem gợi ý hay hướng dẫn (mức nền).
 * - FOLLOWUP: cuối nghiên cứu. Lặp lại y nguyên ba nhóm tự tin / tự làm / tình huống để lấy hiệu sau − trước.
 * - FEEDBACK: bảy câu cũ, vẫn dùng cho form "Send feedback" chung và hộp chấm sao sau khi lắp xong.
 * Mỗi câu có bản tiếng Việt (vi); người tham gia chọn ngôn ngữ ở đầu form, đáp án lưu theo value nên không phụ thuộc ngôn ngữ.
 */
export type Lang = 'en' | 'vi'

export type StudyOption = { value: string; label: string; vi?: string }

export type StudyQuestion = {
  key: string
  text: string
  hint?: string
  /** number: ô nhập số (tuổi). line: ô một dòng, options là gợi ý chọn nhanh nhưng vẫn gõ tự do được (nghề) */
  type: 'single' | 'multi' | 'scale' | 'text' | 'number' | 'line'
  options?: StudyOption[]
  low?: string
  high?: string
  /** Chỉ hiện khi một câu trước có đáp án nhất định, dạng "key=value|key=value" (thoả một điều kiện là hiện) */
  showIf?: string
  placeholder?: string
  /** Câu dẫn chung cho một nhóm câu, hiện phía trên câu đầu tiên của nhóm */
  section?: string
  vi?: { text: string; hint?: string; low?: string; high?: string; placeholder?: string; section?: string }
}

/** Thang hài lòng 1 đến 5, giống thang trong bảng khảo sát để so được với nhau */
const SATISFACTION = { type: 'scale', low: "Very dissatisfied", high: "Very satisfied" } as const
const SATISFACTION_VI = { low: 'Rất không hài lòng', high: 'Rất hài lòng' }

export const FEEDBACK: StudyQuestion[] = [
  {
    key: 'budget_fit',
    text: "1. How satisfied are you that the recommended build fits your budget?",
    ...SATISFACTION,
    vi: { text: '1. Bạn hài lòng đến đâu với việc cấu hình được gợi ý vừa với ngân sách của bạn?', ...SATISFACTION_VI },
  },
  {
    key: 'purpose_fit',
    text: "2. How satisfied are you that the selected components suit your intended use?",
    ...SATISFACTION,
    vi: { text: '2. Bạn hài lòng đến đâu với việc các linh kiện được chọn đúng với mục đích bạn dùng máy?', ...SATISFACTION_VI },
  },
  {
    key: 'guide',
    text: "3. How satisfied are you with the step-by-step assembly guide?",
    ...SATISFACTION,
    vi: { text: '3. Bạn hài lòng đến đâu với phần hướng dẫn lắp máy từng bước?', ...SATISFACTION_VI },
  },
  {
    key: 'confidence',
    text: "4. After using the app, how has your confidence in choosing and assembling a PC changed?",
    hint: "3 means no change",
    type: 'scale',
    low: "Much less confident",
    high: "Much more confident",
    vi: { text: '4. Sau khi dùng web app, bạn thấy mình tự tin hơn khi tự chọn và lắp máy đến đâu?', hint: '3 là không thay đổi', low: 'Kém tự tin hơn nhiều', high: 'Tự tin hơn nhiều' },
  },
  {
    key: 'next_time',
    text: "5. Next time you need to choose, assemble or upgrade a PC, how independently would you do it?",
    type: 'scale',
    low: "Ask someone else to do it",
    high: "Do it myself without help",
    vi: { text: '5. Lần tới cần chọn, lắp hoặc nâng cấp máy, bạn sẽ tự làm đến đâu?', low: 'Nhờ người khác làm hộ', high: 'Tự làm, không cần hỏi ai' },
  },
  {
    key: 'difficulties',
    text: "6. What difficulties did you encounter? Was there anything the app could not help you with?",
    type: 'text',
    placeholder: "For example: a confusing step or something you needed help with",
    vi: { text: '6. Bạn gặp những khó khăn gì? Có chỗ nào web app chưa giúp được bạn không?', placeholder: 'Ví dụ: bước nào khó hiểu, chỗ nào phải hỏi người khác' },
  },
  {
    key: 'suggestions',
    text: "7. What would improve your experience with this app?",
    type: 'text',
    placeholder: "Use your own words",
    vi: { text: '7. Hãy góp ý để web app mang lại trải nghiệm tốt hơn.', placeholder: 'Viết bằng lời của bạn' },
  },
]

/* ---------- Ba nhóm câu lõi: hỏi y nguyên ở lần đầu và cuối nghiên cứu ---------- */

const CONFIDENCE_ITEMS: [string, string, string][] = [
  ['conf_choose', "Choose components that fit your budget and intended use", 'Chọn linh kiện hợp với ngân sách và mục đích dùng máy'],
  ['conf_build', "Assemble a complete PC from separate parts", 'Lắp một bộ máy hoàn chỉnh từ linh kiện rời'],
  ['conf_maintain', "Open the PC, clean out dust and replace the thermal paste", 'Mở máy, vệ sinh bụi và thay keo tản nhiệt'],
  ['conf_upgrade', "Replace or add a component (RAM, SSD, graphics card)", 'Tự thay hoặc gắn thêm một linh kiện (RAM, SSD, card đồ hoạ)'],
]

const CONFIDENCE: StudyQuestion[] = CONFIDENCE_ITEMS.map(([key, text, vi], i) => ({
  key,
  text,
  type: 'scale',
  low: "Not confident at all",
  high: "Completely confident",
  ...(i === 0 ? { section: "Right now, if you had to do it alone with no one helping, how confident are you that you could do each of these?" } : {}),
  vi: {
    text: vi,
    low: 'Hoàn toàn không tự tin',
    high: 'Hoàn toàn tự tin',
    ...(i === 0 ? { section: 'Ngay lúc này, nếu phải tự làm một mình, không ai giúp, bạn tự tin đến đâu rằng mình làm được từng việc sau?' } : {}),
  },
}))

const INDEPENDENCE_LEVELS: StudyOption[] = [
  { value: '1', label: "Ask someone else to do it", vi: 'Nhờ người khác làm hết' },
  { value: '2', label: "Someone else does it, I help", vi: 'Người khác làm, tôi phụ' },
  { value: '3', label: "I do it with someone guiding me", vi: 'Tôi làm, có người đứng cạnh chỉ' },
  { value: '4', label: "I do it myself and ask only if stuck", vi: 'Tôi tự làm, chỉ hỏi khi bí' },
  { value: '5', label: "I do it myself without help", vi: 'Tôi tự làm, không cần ai' },
]

const INDEPENDENCE_ITEMS: [string, string, string][] = [
  ['ind_choose', "Choosing which components to buy", 'Chọn mua linh kiện'],
  ['ind_build', "Assembling a PC", 'Lắp một bộ máy'],
  ['ind_maintain', "Cleaning the PC and replacing the thermal paste", 'Vệ sinh máy và thay keo tản nhiệt'],
  ['ind_upgrade', "Upgrading a component", 'Nâng cấp một linh kiện'],
]

const INDEPENDENCE: StudyQuestion[] = INDEPENDENCE_ITEMS.map(([key, text, vi], i) => ({
  key,
  text,
  type: 'single',
  options: INDEPENDENCE_LEVELS,
  ...(i === 0 ? { section: "If you had to do each of these next week, how would you do it?" } : {}),
  vi: { text: vi, ...(i === 0 ? { section: 'Nếu tuần tới phải làm từng việc sau, bạn sẽ làm thế nào?' } : {}) },
}))

const UNKNOWN: StudyOption = { value: 'unknown', label: "I don't know", vi: 'Tôi không biết' }

/** Bốn tình huống có một đáp án đúng (DECISION_CORRECT). Không hiện đáp án sau khi trả lời. */
const DECISIONS: StudyQuestion[] = [
  {
    key: 'dq_budget',
    section: "Four short situations. Choose what you would do. If you are not sure, choose “I don't know”.",
    text: "A 12 million VND budget for a PC used only for Word, web browsing and online meetings. Which purchase is the most wasteful?",
    type: 'single',
    options: [
      { value: 'ssd', label: "An SSD", vi: 'Ổ SSD' },
      { value: 'ram', label: "16GB of RAM", vi: '16GB RAM' },
      { value: 'gpu', label: "An expensive dedicated graphics card", vi: 'Card đồ hoạ rời đắt tiền' },
      UNKNOWN,
    ],
    vi: {
      section: 'Bốn tình huống ngắn. Chọn cách bạn sẽ làm. Không chắc thì chọn “Tôi không biết”.',
      text: 'Ngân sách 12 triệu, máy chỉ dùng Word, lướt web và họp online. Khoản nào lãng phí nhất?',
    },
  },
  {
    key: 'dq_upgrade',
    text: "An office PC with 8GB RAM and a hard disk drive is very slow to start and open apps. You have about 1 million VND. Which upgrade helps most?",
    type: 'single',
    options: [
      { value: 'gpu', label: "Buy a dedicated graphics card", vi: 'Mua card đồ hoạ rời' },
      { value: 'ssd', label: "Replace the hard disk with an SSD", vi: 'Đổi sang ổ SSD' },
      { value: 'psu', label: "Buy a higher-wattage power supply", vi: 'Đổi nguồn công suất lớn hơn' },
      UNKNOWN,
    ],
    vi: { text: 'Máy văn phòng có 8GB RAM và ổ HDD, khởi động và mở ứng dụng rất chậm. Bạn có khoảng 1 triệu. Nâng cấp nào hiệu quả nhất?' },
  },
  {
    key: 'dq_maintain',
    text: "A 3-year-old PC runs fine normally, but under heavy load it gets very hot, the fans are loud and it stutters. What should you do first?",
    type: 'single',
    options: [
      { value: 'new', label: "Buy a new PC because it is old", vi: 'Mua máy mới vì máy đã cũ' },
      { value: 'reinstall', label: "Reinstall Windows", vi: 'Cài lại Windows' },
      { value: 'clean', label: "Clean out the dust and replace the thermal paste", vi: 'Vệ sinh bụi và thay keo tản nhiệt' },
      UNKNOWN,
    ],
    vi: { text: 'Máy đã dùng 3 năm. Bình thường vẫn ổn, nhưng khi chạy nặng thì rất nóng, quạt kêu to và giật. Nên làm gì đầu tiên?' },
  },
  {
    key: 'dq_compat',
    text: "You want to add a dedicated graphics card to your current PC. What should you check before buying?",
    type: 'single',
    options: [
      { value: 'psu_space', label: "The power supply's wattage and connectors, and the space inside the case", vi: 'Công suất và đầu cấp điện của nguồn, khoảng trống trong thùng máy' },
      { value: 'storage', label: "The storage capacity", vi: 'Dung lượng ổ cứng' },
      { value: 'windows', label: "The Windows version", vi: 'Phiên bản Windows' },
      UNKNOWN,
    ],
    vi: { text: 'Bạn muốn gắn thêm card đồ hoạ rời vào máy đang có. Cần kiểm tra gì trước khi mua?' },
  },
]

export const DECISION_CORRECT: Record<string, string> = { dq_budget: 'gpu', dq_upgrade: 'ssd', dq_maintain: 'clean', dq_compat: 'psu_space' }
export const CONFIDENCE_KEYS = CONFIDENCE_ITEMS.map(([k]) => k)
export const INDEPENDENCE_KEYS = INDEPENDENCE_ITEMS.map(([k]) => k)
export const DECISION_KEYS = DECISIONS.map((q) => q.key)

/** Nguồn hỏi khi phải quyết định (Q2). Ba nguồn đầu là "người khác" mà proposal nêu tên. */
const SOURCE_OPTIONS: StudyOption[] = [
  { value: 'shop', label: "Shop staff", vi: 'Nhân viên cửa hàng' },
  { value: 'online', label: "Forums, social media or YouTube", vi: 'Diễn đàn, mạng xã hội, YouTube' },
  { value: 'people', label: "Friends or family", vi: 'Bạn bè, người nhà' },
  { value: 'ai', label: "An AI chatbot", vi: 'Chatbot AI' },
  { value: 'self', label: "No one, I work it out myself", vi: 'Không hỏi ai, tự tìm hiểu' },
  { value: 'never', label: "I have never had to decide", vi: 'Chưa từng phải quyết định' },
]
export const EXTERNAL_SOURCES = ['shop', 'online', 'people']

const sources = (extra: StudyOption[] = []): StudyQuestion => ({
  key: 'sources',
  text: "When you have to make a decision about PC hardware (buying, repairing, upgrading), who do you usually ask?",
  hint: "Choose all that apply",
  type: 'multi',
  options: [...SOURCE_OPTIONS.slice(0, 5), ...extra, SOURCE_OPTIONS[5]],
  vi: { text: 'Khi phải quyết định một việc về phần cứng máy tính (mua, sửa, nâng cấp), bạn thường hỏi ai?', hint: 'Chọn tất cả các ý đúng' },
})

/** Nền kỹ thuật (Q4): hai mức đầu là "không có nền kỹ thuật" */
export const TECH_GROUP: Record<string, 'non_tech' | 'tech'> = { never: 'non_tech', watched: 'non_tech', part: 'tech', built: 'tech', it: 'tech' }

/** Gợi ý nghề, giống danh sách của form hồ sơ cũ. Lưu chữ tiếng Anh (value) để biểu đồ nghề không bị tách đôi theo ngôn ngữ */
const OCCUPATIONS: StudyOption[] = [
  { value: 'School student', label: 'School student', vi: 'Học sinh' },
  { value: 'University student', label: 'University student', vi: 'Sinh viên' },
  { value: 'Office worker', label: 'Office worker', vi: 'Nhân viên văn phòng' },
  { value: 'Information technology', label: 'Information technology', vi: 'Công nghệ thông tin' },
  { value: 'Design / creative work', label: 'Design / creative work', vi: 'Thiết kế / sáng tạo' },
  { value: 'Teacher / lecturer', label: 'Teacher / lecturer', vi: 'Giáo viên / giảng viên' },
  { value: 'Business / self-employed', label: 'Business / self-employed', vi: 'Kinh doanh / tự do' },
  { value: 'Not currently employed', label: 'Not currently employed', vi: 'Chưa đi làm' },
]

export const BASELINE: StudyQuestion[] = [
  ...CONFIDENCE,
  ...INDEPENDENCE,
  ...DECISIONS,
  {
    key: 'participant_age',
    section: "About you",
    text: "How old are you?",
    type: 'number',
    placeholder: "e.g. 21",
    vi: { section: 'Về bạn', text: 'Bạn bao nhiêu tuổi?', placeholder: 'Ví dụ 21' },
  },
  {
    key: 'participant_occupation',
    text: "What is your occupation?",
    hint: "Choose one or type your own",
    type: 'line',
    placeholder: "e.g. University student",
    options: OCCUPATIONS,
    vi: { text: 'Nghề nghiệp của bạn là gì?', hint: 'Chọn một gợi ý hoặc tự gõ', placeholder: 'Ví dụ Sinh viên' },
  },
  {
    key: 'tech_bg',
    text: "Which of these describes you best?",
    type: 'single',
    options: [
      { value: 'never', label: "I have never opened a PC case", vi: 'Tôi chưa từng mở thùng máy' },
      { value: 'watched', label: "I have watched someone else do it but never done it myself", vi: 'Tôi đã xem người khác làm nhưng chưa tự làm' },
      { value: 'part', label: "I have replaced or added a component myself", vi: 'Tôi đã tự thay hoặc gắn một linh kiện' },
      { value: 'built', label: "I have built a complete PC myself", vi: 'Tôi đã tự lắp trọn một bộ máy' },
      { value: 'it', label: "I study or work in hardware or IT", vi: 'Tôi học hoặc làm nghề về phần cứng, IT' },
    ],
    vi: { text: 'Điều nào đúng nhất với bạn?' },
  },
  {
    key: 'own_pc',
    text: "Do you have your own computer at the moment?",
    hint: "If you have a desktop PC, the app will help you set a maintenance reminder next.",
    type: 'single',
    options: [
      { value: 'desktop', label: "Yes, a desktop PC", vi: 'Có, máy bàn' },
      { value: 'laptop', label: "Only a laptop", vi: 'Chỉ có laptop' },
      { value: 'none', label: "No computer of my own", vi: 'Chưa có máy riêng' },
    ],
    vi: { text: 'Hiện bạn có máy tính riêng không?', hint: 'Nếu có máy bàn, bước tiếp theo app sẽ giúp bạn đặt lời nhắc bảo dưỡng.' },
  },
  sources(),
]

/** Bảy câu cũ trừ next_time (nhóm "tự làm" đã thay nó), bỏ số thứ tự vì nằm giữa form khác */
const ABOUT_APP: StudyQuestion[] = FEEDBACK.filter((q) => q.key !== 'next_time').map((q, i) => ({
  ...q,
  text: q.text.replace(/^\d+\. /, ''),
  ...(i === 0 ? { section: "About the app" } : {}),
  vi: q.vi && { ...q.vi, text: q.vi.text.replace(/^\d+\. /, ''), ...(i === 0 ? { section: 'Về web app' } : {}) },
}))

export const FOLLOWUP: StudyQuestion[] = [
  ...CONFIDENCE,
  ...INDEPENDENCE,
  ...DECISIONS,
  { ...sources([{ value: 'app', label: "This web app", vi: 'Web app này' }]), section: "Since you joined the study", vi: { ...sources().vi!, section: 'Từ khi tham gia nghiên cứu' } },
  {
    key: 'maint_done',
    text: "Since the study started, have you cleaned or serviced your own PC?",
    type: 'single',
    options: [
      { value: 'reminder', label: "Yes, after the app's reminder", vi: 'Có, sau khi app nhắc' },
      { value: 'problem', label: "Yes, because the PC had a problem (hot, noisy, slow)", vi: 'Có, vì máy có vấn đề (nóng, ồn, chậm)' },
      { value: 'other', label: "Yes, for another reason", vi: 'Có, vì lý do khác' },
      { value: 'no', label: "No, not yet", vi: 'Chưa' },
    ],
    vi: { text: 'Từ khi bắt đầu nghiên cứu, bạn đã tự vệ sinh hay bảo dưỡng máy của mình chưa?' },
  },
  ...ABOUT_APP,
]

/** Chữ hiển thị theo ngôn ngữ đang chọn (thiếu bản dịch thì dùng tiếng Anh) */
export function localize(q: StudyQuestion, lang: Lang): StudyQuestion {
  if (lang === 'en' || !q.vi) return q
  return {
    ...q,
    text: q.vi.text,
    hint: q.vi.hint ?? q.hint,
    low: q.vi.low ?? q.low,
    high: q.vi.high ?? q.high,
    placeholder: q.vi.placeholder ?? q.placeholder,
    section: q.section && (q.vi.section ?? q.section),
    options: q.options?.map((o) => ({ ...o, label: o.vi ?? o.label })),
  }
}
