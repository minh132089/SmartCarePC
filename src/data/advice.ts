import { FAMILIES, type FamilyId } from './families'
import { getBuild } from './builds'

/**
 * Trang tư vấn: vài câu bấm chọn rồi gợi ý một trong 6 máy của app, không cần AI.
 * Mỗi câu trả lời cộng điểm cho từng máy, máy nhiều điểm nhất được gợi ý, hai máy kế tiếp là lựa chọn khác.
 * Điểm và lời giải thích viết theo kinh nghiệm chung về dáng máy, không gắn với hãng hay giá cụ thể.
 */
export type MachineId = FamilyId | 'gaming'

export const QUESTIONS = [
  {
    id: 'use',
    title: 'Bạn dùng máy chủ yếu để làm gì?',
    options: [
      { id: 'study', label: 'Học tập, văn phòng', hint: 'Soạn thảo, học online, xem phim' },
      { id: 'game', label: 'Chơi game', hint: 'Game 3D, game online nặng' },
      { id: 'creative', label: 'Đồ họa, dựng phim', hint: 'Photoshop, dựng video, làm 3D' },
    ],
  },
  {
    id: 'budget',
    title: 'Ngân sách cho thùng máy khoảng bao nhiêu?',
    options: [
      { id: 'low', label: 'Dưới 15 triệu', hint: 'Chưa tính màn hình, bàn phím' },
      { id: 'mid', label: '15 đến 30 triệu', hint: 'Mức phổ biến nhất' },
      { id: 'high', label: 'Trên 30 triệu', hint: 'Muốn máy mạnh, dùng lâu' },
    ],
  },
  {
    id: 'size',
    title: 'Bạn muốn máy trông thế nào?',
    options: [
      { id: 'small', label: 'Nhỏ gọn', hint: 'Bàn chật, muốn để trên bàn' },
      { id: 'normal', label: 'Bình thường', hint: 'Đặt dưới hoặc cạnh bàn' },
      { id: 'show', label: 'Đẹp để trưng bày', hint: 'Mặt kính, quạt có đèn' },
    ],
  },
  {
    id: 'upgrade',
    title: 'Sau này bạn có muốn nâng cấp thêm không?',
    options: [
      { id: 'yes', label: 'Có, muốn dễ nâng cấp', hint: 'Thêm RAM, ổ cứng, đổi card' },
      { id: 'no', label: 'Không, dùng sao để vậy', hint: 'Lắp một lần là xong' },
    ],
  },
] as const

export type QuestionId = (typeof QUESTIONS)[number]['id']
export type Answers = Partial<Record<QuestionId, string>>

const MACHINES: MachineId[] = ['standard_tower', 'compact_tower', 'showcase_dual_chamber', 'mini_sff', 'full_tower', 'gaming']

/** Điểm cộng cho từng máy theo mỗi lựa chọn. Máy không có tên thì được 0 điểm. */
const SCORE: Record<string, Partial<Record<MachineId, number>>> = {
  'use.study': { compact_tower: 3, mini_sff: 2, standard_tower: 2 },
  'use.game': { gaming: 3, standard_tower: 2, showcase_dual_chamber: 2, full_tower: 2, compact_tower: 1, mini_sff: 1 },
  'use.creative': { full_tower: 3, standard_tower: 2, gaming: 1, showcase_dual_chamber: 1 },
  'budget.low': { compact_tower: 3, standard_tower: 1, showcase_dual_chamber: -2, full_tower: -2, gaming: -2 },
  'budget.mid': { standard_tower: 3, compact_tower: 2, showcase_dual_chamber: 2, mini_sff: 2, gaming: 1 },
  'budget.high': { full_tower: 3, gaming: 3, showcase_dual_chamber: 2, mini_sff: 1, standard_tower: 1 },
  'size.small': { mini_sff: 3, compact_tower: 2, gaming: -1, full_tower: -2 },
  'size.normal': { standard_tower: 2, compact_tower: 1, gaming: 1 },
  'size.show': { showcase_dual_chamber: 3, gaming: 2, full_tower: 1 },
  'upgrade.yes': { full_tower: 2, standard_tower: 2, gaming: 1, showcase_dual_chamber: 1, mini_sff: -1 },
  'upgrade.no': { mini_sff: 1, compact_tower: 1 },
}

/** Vì sao nên chọn máy này và điều cần biết trước khi mua, viết cho người không rành kỹ thuật */
export const MACHINE_INFO: Record<MachineId, { why: string[]; note: string; level: string }> = {
  standard_tower: {
    why: [
      'Bên trong rộng nên dễ lắp nhất cho người mới.',
      'Đủ chỗ cho card đồ họa dài và tản nhiệt to, máy mát và êm.',
      'Bo mạch cỡ lớn, sau này dễ thêm RAM, ổ cứng hay đổi card.',
    ],
    note: 'Thân máy cỡ vừa, cần chỗ đặt dưới hoặc cạnh bàn.',
    level: 'Dễ lắp',
  },
  compact_tower: {
    why: ['Nhỏ hơn tower thường, vừa với bàn học.', 'Đủ cho học, văn phòng và game nhẹ.', 'Linh kiện phổ biến, dễ mua, giá mềm.'],
    note: 'Ít chỗ hơn, sau này khó gắn card đồ họa rất dài.',
    level: 'Dễ lắp',
  },
  showcase_dual_chamber: {
    why: [
      'Kính mặt trước và hông, nhiều quạt đèn, đặt trên bàn rất đẹp.',
      'Tản nước giữ CPU mát khi chơi game lâu.',
      'Card đồ họa dựng đứng để nhìn thấy rõ.',
    ],
    note: 'Lắp phức tạp hơn vì có cáp riser, tản nước và nhiều dây quạt. Nên làm chậm theo từng bước.',
    level: 'Khó vừa',
  },
  mini_sff: {
    why: ['Rất nhỏ, để gọn trên bàn hoặc mang đi được.', 'Vẫn gắn được card đồ họa rời để chơi game.'],
    note: 'Bên trong chật nên khó lắp và khó nâng cấp. Tản nhiệt nhỏ, không hợp chạy việc nặng nhiều giờ.',
    level: 'Khó',
  },
  full_tower: {
    why: [
      'Rộng nhất, gắn được tản nước lớn và nhiều quạt.',
      'Hợp dựng phim, làm 3D, chạy máy nặng nhiều giờ liền.',
      'Nâng cấp thoải mái về sau.',
    ],
    note: 'Thân máy to và nặng, nên đặt dưới sàn.',
    level: 'Dễ lắp',
  },
  gaming: {
    why: ['Cấu hình đầy đủ cho chơi game: card đồ họa 3 quạt và tản nước.', 'Mặt kính để nhìn thấy bên trong.', 'Nhiều quạt nên máy mát khi chơi lâu.'],
    note: 'Tốn tiền và tốn điện hơn, cần nguồn đủ công suất.',
    level: 'Khó vừa',
  },
}

export function machineLabel(id: MachineId) {
  return id === 'gaming' ? 'Gaming (đầy đủ)' : FAMILIES[id].label
}

export function machineTagline(id: MachineId) {
  return id === 'gaming' ? getBuild('gaming').tagline : FAMILIES[id].tagline
}

/** Ảnh thu nhỏ của máy (máy Gaming chưa có ảnh nên trả null) */
export function machineImage(id: MachineId): string | null {
  const n: Record<FamilyId, string> = { standard_tower: '01', compact_tower: '02', showcase_dual_chamber: '03', mini_sff: '04', full_tower: '06' }
  return id === 'gaming' ? null : `/families/${n[id]}_${id}.jpg`
}

/** Xếp hạng máy theo câu trả lời: máy đầu là gợi ý chính. Hoà điểm thì giữ thứ tự trong MACHINES. */
export function rank(answers: Answers): MachineId[] {
  const score = (m: MachineId) =>
    Object.entries(answers).reduce((sum, [q, a]) => sum + (SCORE[`${q}.${a}`]?.[m] ?? 0), 0)
  return [...MACHINES].sort((a, b) => score(b) - score(a))
}

export const isComplete = (answers: Answers) => QUESTIONS.every((q) => answers[q.id])
