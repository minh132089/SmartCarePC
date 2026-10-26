import { ASSEMBLY, STEPS, type AssemblyKey } from './assembly'
import { getBuild, type BuildId } from './builds'
import { FAMILIES, gpuBehindBoard, type FamilyId } from './families'
import type { BuildCard } from '../ui/chat/chatStore'

/**
 * Hướng dẫn lắp máy từng bước cho người mới. Thứ tự các bước lắp trùng với data/assembly.ts
 * để hình 3D lắp đúng linh kiện đang được hướng dẫn. Thêm bước "Chuẩn bị" ở đầu và "Bật máy" ở cuối.
 * Nội dung đổi theo máy đang xem: tản khí / tản thấp / tản nước, nguồn ATX / SFX, card cắm thẳng / qua riser.
 */
export interface GuideStep {
  id: string
  /** Linh kiện được lắp ở bước này; null là bước không gắn với linh kiện nào (chuẩn bị, bật máy) */
  part: AssemblyKey | null
  title: string
  /** Một câu: bước này để làm gì */
  why: string
  actions: string[]
  tools?: string[]
  warning?: string
  /** Dấu hiệu cho biết đã lắp đúng */
  check: string
}

interface GuideMachine {
  cooler: 'air' | 'lowprofile' | 'aio'
  psu: 'atx' | 'sfx'
  board: 'ATX' | 'mATX' | 'ITX'
  riser: boolean
  /** Card nằm sau tấm đỡ bo mạch: lắp từ hông bên kia của vỏ */
  gpuBehind: boolean
  hub: boolean
}

function machineOf(familyId: FamilyId | null, buildId: BuildId): GuideMachine {
  if (familyId) {
    const f = FAMILIES[familyId]
    const kind = f.cooler.kind
    return {
      cooler: kind.startsWith('aio') ? 'aio' : kind.startsWith('low') ? 'lowprofile' : 'air',
      psu: f.psu.kind,
      board: f.board.ff,
      riser: !!f.riser,
      gpuBehind: gpuBehindBoard(f),
      hub: familyId === 'full_tower' || familyId === 'showcase_dual_chamber',
    }
  }
  const b = getBuild(buildId)
  return {
    cooler: b.cooler === 'aio' ? 'aio' : b.cooler === 'lowprofile' ? 'lowprofile' : 'air',
    psu: b.psu === 'SFX' ? 'sfx' : 'atx',
    board: b.formFactor,
    riser: false,
    gpuBehind: false,
    hub: false,
  }
}

export function guideSteps(familyId: FamilyId | null, buildId: BuildId, hasGpu = true): GuideStep[] {
  const m = machineOf(familyId, buildId)
  const steps: GuideStep[] = [
    {
      id: 'prep',
      part: null,
      title: 'Chuẩn bị chỗ làm và đồ nghề',
      why: 'Chuẩn bị kỹ giúp bạn không mất vít và không làm hỏng linh kiện vì tĩnh điện.',
      actions: [
        'Dọn một mặt bàn rộng, khô và đủ sáng. Không làm trên thảm len hay chăn vì dễ sinh tĩnh điện.',
        'Đeo dây chống tĩnh điện, kẹp đầu kia vào phần kim loại của vỏ máy.',
        'Đặt vỏ máy nằm nghiêng, tháo hai nắp hông. Trong hướng dẫn này bo mạch chủ đã được bắt sẵn trong vỏ.',
        'Chỉ mở hộp linh kiện nào sắp dùng tới. Đổ vít ra khay, mỗi loại một ô.',
      ],
      tools: ['Tua vít 4 cạnh số 2', 'Dây chống tĩnh điện', 'Khay đựng vít', 'Dây rút'],
      warning: 'Chưa cắm điện vào nguồn cho tới bước cuối cùng.',
      check: 'Bàn đã gọn, đồ nghề đủ, vỏ máy đã mở nắp hông.',
    },
    {
      id: 'psu',
      part: 'psu',
      title: m.psu === 'sfx' ? 'Lắp nguồn SFX' : 'Lắp nguồn (PSU)',
      why: 'Nguồn đổi điện từ ổ cắm thành điện cho từng linh kiện. Lắp trước để dễ luồn dây về sau.',
      actions:
        m.psu === 'sfx'
          ? [
              'Nguồn SFX nhỏ hơn nguồn thường. Tháo khung đỡ nguồn ra khỏi vỏ trước.',
              'Bắt nguồn vào khung đỡ bằng 4 vít, quạt nguồn hướng ra phía có lỗ thông gió.',
              'Đặt cả khung vào lại vỏ và bắt vít giữ khung.',
              'Cắm sẵn dây điện nối dài từ cổng cắm điện của vỏ vào nguồn (nếu vỏ có).',
            ]
          : [
              'Tìm khoang nguồn ở đáy vỏ, phía sau.',
              'Xoay nguồn cho quạt hướng xuống đáy vỏ, nơi có lưới lọc bụi, để nguồn hút được gió mát.',
              'Đẩy nguồn vào khoang cho tới khi 4 lỗ vít khớp với mặt sau vỏ.',
              'Bắt 4 vít nguồn ở mặt sau vỏ.',
            ],
      tools: ['Tua vít số 2', '4 vít nguồn (trong hộp vỏ hoặc hộp nguồn)'],
      warning: 'Gạt công tắc ở mặt sau nguồn về vị trí tắt (O).',
      check: 'Nguồn nằm chắc, đẩy nhẹ không bị lắc.',
    },
    {
      id: 'cpu',
      part: 'cpu',
      title: 'Đặt CPU vào socket',
      why: 'CPU là bộ não của máy. Nó nằm trong ổ cắm vuông trên bo mạch, gọi là socket.',
      actions: [
        'Nhấn cần gạt cạnh socket xuống, đẩy ra ngoài rồi nâng lên để mở khung giữ.',
        'Cầm CPU ở hai mép, không chạm vào mặt dưới.',
        'Tìm dấu tam giác nhỏ ở một góc CPU. Đặt góc đó trùng với dấu tam giác trên socket.',
        'Thả CPU nhẹ xuống. Không ấn, CPU phải tự nằm phẳng.',
        'Hạ khung giữ và gạt cần về chỗ cũ. Nắp nhựa bảo vệ có thể tự bật ra, cứ cất nắp đó đi.',
      ],
      tools: ['Không cần dụng cụ'],
      warning: 'Không bao giờ ấn mạnh CPU. Chân tiếp xúc rất dễ cong, cong là phải thay bo mạch. CPU không nằm phẳng thì nhấc ra, xem lại dấu tam giác.',
      check: 'CPU nằm phẳng trong socket, cần gạt đã khoá.',
    },
    {
      id: 'ram',
      part: 'ram',
      title: 'Cắm RAM',
      why: 'RAM là bộ nhớ tạm. Máy không có RAM thì không khởi động được.',
      actions: [
        'Mở lẫy khoá ở đầu khe RAM. Có bo chỉ có lẫy ở một đầu.',
        m.board === 'ITX'
          ? 'Bo Mini-ITX chỉ có 2 khe, cắm cả hai.'
          : 'Có 2 thanh thì cắm vào khe thứ 2 và thứ 4 tính từ CPU (thường in chữ A2 và B2). Xem sách bo mạch cho chắc.',
        'So khấc ở chân RAM với gờ trong khe. RAM chỉ cắm được một chiều.',
        'Ấn đều hai đầu thanh RAM thẳng xuống cho tới khi nghe "tách" và lẫy tự khoá.',
      ],
      tools: ['Không cần dụng cụ'],
      warning: 'Phải ấn khá mạnh, nhưng nếu ấn mãi không vào thì kiểm tra lại chiều khấc. RAM DDR4 và DDR5 không cắm lẫn được.',
      check: 'Lẫy đã đóng, các thanh RAM đứng thẳng và cao bằng nhau.',
    },
    {
      id: 'ssd',
      part: 'ssd',
      title: 'Lắp ổ SSD M.2',
      why: 'SSD lưu hệ điều hành và dữ liệu. Loại M.2 cắm thẳng lên bo mạch, không cần dây.',
      actions: [
        'Tìm khe M.2 trên bo. Khe có thể nằm dưới một tấm tản nhiệt nhỏ, tháo tấm này ra trước.',
        'Tháo con vít nhỏ (hoặc chốt nhựa) ở cuối khe.',
        'Cắm SSD vào khe theo góc nghiêng khoảng 30 độ, đầu có chân vàng vào trước.',
        'Ấn đầu còn lại xuống cho SSD nằm phẳng rồi bắt lại vít.',
        'Nếu tấm tản có lớp nilon ở mặt dưới thì bóc ra rồi mới lắp tấm tản lại.',
      ],
      tools: ['Tua vít nhỏ số 1'],
      warning: 'Vít M.2 rất nhỏ và dễ rơi vào khe kẹt. Làm chậm, để vít trên khay.',
      check: 'SSD nằm phẳng sát bo mạch, đã bắt vít.',
    },
    coolerStep(m),
    {
      id: 'fans',
      part: 'fans',
      title: 'Bắt quạt case',
      why: 'Quạt case hút gió mát vào và đẩy gió nóng ra khỏi vỏ. Lắp quạt trước card đồ họa, vì card nằm ngang sẽ che chỗ bắt vít quạt và chân cắm quạt trên bo.',
      actions: [
        'Xem hai mũi tên nhỏ ở cạnh khung quạt: một mũi chỉ hướng gió, một mũi chỉ chiều quay.',
        'Quạt mặt trước và đáy thổi gió vào trong. Quạt phía sau và trên nóc thổi gió ra ngoài.',
        'Bắt 4 vít quạt vào lỗ trên vỏ.',
        m.hub
          ? 'Cắm dây quạt vào hub quạt của vỏ, rồi cắm dây của hub vào bo mạch.'
          : 'Cắm dây quạt vào cổng SYS_FAN (có bo ghi CHA_FAN) trên bo mạch.',
        'Quạt có đèn thì cắm thêm dây đèn 3 chân vào cổng ARGB.',
      ],
      tools: ['Tua vít số 2', 'Vít quạt (trong hộp quạt)'],
      warning: 'Dây đèn ARGB 3 chân (5 V) và RGB 4 chân (12 V) khác nhau. Cắm nhầm có thể làm cháy đèn.',
      check: 'Quạt bắt chắc, không chạm dây, mũi tên hướng gió đúng.',
    },
    gpuStep(m),
    {
      id: 'cables',
      part: 'cables',
      title: 'Cắm và gom dây',
      why: 'Dây đưa điện từ nguồn tới từng linh kiện và nối các nút bấm ở vỏ với bo mạch.',
      actions: [
        'Cắm dây 24 chân lớn nhất từ nguồn vào cạnh phải bo mạch.',
        'Cắm dây 8 chân ghi chữ CPU (hoặc EPS) vào góc trên bên trái bo, gần CPU.',
        'Cắm cụm dây nút nguồn, nút reset và đèn vào cổng F_PANEL ở góc dưới bên phải bo. Làm theo sơ đồ trong sách bo mạch.',
        'Cắm dây USB mặt trước và dây âm thanh HD_AUDIO.',
        'Luồn dây ra sau tấm đỡ bo mạch, dùng dây rút gom lại. Không để dây chạm cánh quạt.',
      ],
      tools: ['Dây rút', 'Kìm cắt'],
      warning: 'Đầu 8 chân của CPU và đầu 8 chân của card đồ họa trông giống nhau nhưng không dùng lẫn được. Xem chữ in trên dây trước khi cắm.',
      check: 'Mọi đầu cắm đã vào hết chiều sâu, không còn dây lỏng gần quạt.',
    },
    {
      id: 'finish',
      part: null,
      title: 'Kiểm tra và bật máy lần đầu',
      why: 'Kiểm tra lại trước khi cấp điện giúp tránh chập điện và hỏng linh kiện.',
      actions: [
        'Nhìn lại một lượt: dây quạt CPU, dây 24 chân và dây 8 chân CPU đã cắm chặt, không còn vít rơi trong vỏ.',
        hasGpu ? 'Cắm dây màn hình vào cổng của card đồ họa, không cắm vào cổng trên bo mạch.'
          : 'Cấu hình dùng đồ họa tích hợp: cắm dây màn hình vào cổng HDMI hoặc DisplayPort trên bo mạch chủ.',
        'Cắm dây điện, gạt công tắc sau nguồn sang (I), rồi bấm nút nguồn.',
        'Quạt quay và màn hình hiện chữ hoặc logo là đã lắp đúng. Tắt máy, lắp lại hai nắp hông.',
      ],
      warning: 'Máy không lên hình: tắt, rút điện, rồi kiểm tra lại RAM đã cắm chặt chưa và dây 8 chân CPU. Đây là hai lỗi hay gặp nhất.',
      check: 'Máy bật lên và hiện hình.',
    },
  ]
  return hasGpu ? steps : steps.filter((s) => s.id !== 'gpu')
}

/** Gửi cùng nội dung chuẩn đang dùng cho mô hình, thay vì chỉ tên bước để AI tự đoán thao tác. */
export function guideChatContext(familyId: FamilyId | null, buildId: BuildId, index: number | null, card: BuildCard | null) {
  const hasGpu = card == null || card.items.some((i) => i.category === 'gpu')
  const steps = guideSteps(familyId, buildId, hasGpu)
  const current = index == null ? null : steps[Math.max(0, Math.min(index, steps.length - 1))]
  const ram = steps.find((s) => s.id === 'ram')!
  const parts = card?.items.map((i) => `${i.category}: ${i.quantity} × ${(i.productName || i.name).slice(0, 80)}`).join('; ')
  const context = [
    `Mô hình 3D: ${familyId ?? buildId}. ${hasGpu ? 'Có card đồ họa rời.' : 'Không có card rời; dùng đồ họa tích hợp và bỏ bước lắp GPU.'}`,
    `Quy tắc RAM của hướng dẫn: ${ram.actions[1]}`,
    ...(current ? [`Bước ${steps.indexOf(current) + 1}/${steps.length}: ${current.title}`, current.warning ? `An toàn: ${current.warning}` : '', `Thao tác chuẩn: ${current.actions.join(' ')}`, `Lắp đúng khi: ${current.check}`] : []),
    ...(parts ? [`Cấu hình thực tế được ưu tiên hơn bộ máy mẫu (dùng đúng các món và số lượng này): ${parts}`] : []),
  ]
  return context.filter(Boolean).join('\n').slice(0, 2000)
}

function coolerStep(m: GuideMachine): GuideStep {
  const paste = 'Nếu đáy tản đã có sẵn lớp keo xám thì không bôi thêm. Nếu chưa có, nặn một chấm keo cỡ hạt đậu vào giữa mặt CPU, không cần dàn đều.'
  if (m.cooler === 'aio') {
    return {
      id: 'cooler',
      part: 'cooler',
      title: 'Bôi keo và lắp tản nước',
      why: 'Tản nước dẫn nhiệt từ CPU qua ống tới két tản, quạt trên két thổi nhiệt ra ngoài.',
      actions: [
        'Bắt quạt vào két tản, rồi bắt két vào nóc hoặc mặt trước vỏ.',
        'Lắp bộ gá đúng loại socket theo sách đi kèm tản.',
        paste,
        'Đặt đầu bơm lên CPU, siết vít theo hình chữ X, mỗi vít vài vòng, cho tới khi chặt.',
        'Cắm dây bơm vào cổng AIO_PUMP (hoặc CPU_FAN), dây quạt két vào CPU_FAN.',
      ],
      tools: ['Tua vít số 2', 'Keo tản nhiệt', 'Khăn giấy'],
      warning: 'Đầu bơm không được là điểm cao nhất của vòng nước, nếu không bọt khí sẽ đọng lại trong bơm và gây ồn. Quên cắm dây bơm thì CPU nóng rất nhanh.',
      check: 'Đầu bơm không xoay khi lay nhẹ, dây bơm và dây quạt đã cắm.',
    }
  }
  return {
    id: 'cooler',
    part: 'cooler',
    title: 'Bôi keo và lắp tản nhiệt',
    why: 'Tản nhiệt hút nhiệt từ CPU ra. Keo tản nhiệt lấp các khe hở li ti giữa CPU và đế tản.',
    actions: [
      'Lắp bộ gá đúng loại socket theo sách đi kèm tản.',
      'Bóc miếng nilon bảo vệ ở đáy tản (nếu có).',
      paste,
      m.cooler === 'lowprofile'
        ? 'Đặt tản thẳng xuống, bắt 4 vít theo hình chữ X cho tới khi chặt.'
        : 'Đặt tản thẳng xuống, quạt tản thổi về phía sau vỏ. Siết vít theo hình chữ X, mỗi vít vài vòng, cho tới khi chặt.',
      'Cắm dây quạt tản vào cổng CPU_FAN trên bo mạch.',
    ],
    tools: ['Tua vít số 2', 'Keo tản nhiệt', 'Khăn giấy'],
    warning: 'Bôi nhiều keo không làm mát hơn mà còn dễ tràn ra ngoài. Quên cắm dây CPU_FAN thì máy sẽ quá nóng và tự tắt.',
    check: 'Tản không xoay khi lay nhẹ, dây quạt đã cắm vào CPU_FAN.',
  }
}

function gpuStep(m: GuideMachine): GuideStep {
  const power = 'Cắm dây nguồn PCIe 8 chân từ nguồn vào card cho tới khi lẫy khoá kêu "tách".'
  return {
    id: 'gpu',
    part: 'gpu',
    title: 'Cắm card đồ họa',
    why: 'Card đồ họa xử lý hình ảnh, cần cho chơi game, dựng phim và làm 3D.',
    actions: m.gpuBehind
      ? [
          'Máy này dựng card bằng cáp riser: một dải cáp dẹt nối khe PCIe trên bo với card ở buồng phía sau tấm đỡ bo.',
          'Trước khi lật vỏ: cắm đầu riser phía bo vào khe PCIe x16 trên bo (khe dài nhất, gần CPU nhất) cho tới khi kêu "tách".',
          'Lật vỏ nằm nghiêng, hông có bo mạch úp xuống bàn, rồi tháo tấm hông đang hướng lên trên (hình bên cạnh đã xoay sang mặt này).',
          'Mặt này là một buồng riêng chỉ dành cho card, nằm ngay dưới tấm hông vừa tháo, nên card được đặt từ trên xuống, không phải luồn vào sâu.',
          'Ở mặt sau vỏ, tháo 2 tấm chắn ngang với khung card để lộ chỗ cho cổng màn hình.',
          'Đặt card vào buồng, quạt card hướng lên phía tấm hông vừa tháo, cổng màn hình hướng ra mặt sau vỏ. Cắm card vào đầu riser còn lại cho tới khi lẫy kêu "tách", rồi bắt vít giữ card vào mặt sau vỏ.',
          'Luồn dây nguồn PCIe 8 chân từ phía nguồn qua lỗ trên tấm đỡ sang buồng card.',
          power,
        ]
      : m.riser
      ? [
          'Máy này dựng card bằng cáp riser: một dải cáp dẹt nối khe PCIe trên bo với card ở chỗ khác trong vỏ.',
          'Cắm đầu riser vào khe PCIe x16 trên bo (khe dài nhất, gần CPU nhất).',
          'Cắm card vào đầu riser còn lại cho tới khi lẫy kêu "tách", rồi bắt vít card vào khung đỡ.',
          power,
        ]
      : [
          'Tháo 2 tấm chắn ở mặt sau vỏ, ngang với khe PCIe trên cùng.',
          'Mở lẫy nhựa ở cuối khe PCIe x16 (khe dài nhất, gần CPU nhất).',
          'Cầm card ở hai mép, cắm thẳng xuống khe cho tới khi lẫy kêu "tách".',
          'Bắt vít giữ đuôi card vào mặt sau vỏ.',
          power,
        ],
    tools: ['Tua vít số 2'],
    warning: 'Card khá nặng, đỡ bằng tay khi cắm. Dây nguồn phải cắm hết chiều sâu, cắm lỏng có thể làm đầu cắm nóng chảy.',
    check: 'Card nằm thẳng, không chúc xuống, đã bắt vít và cắm dây nguồn.',
  }
}

/**
 * Trạng thái hình 3D cho một bước, tính bằng số bước đã tháo (store.step):
 * bước chuẩn bị thì mọi linh kiện còn ở ngoài, bước lắp linh kiện thứ k thì k-1 linh kiện trước đã vào,
 * bước bật máy thì máy đã lắp đủ.
 */
export function sceneFor(step: GuideStep): { before: number; after: number } {
  if (!step.part) return step.id === 'prep' ? { before: STEPS, after: STEPS } : { before: 0, after: 0 }
  const k = ASSEMBLY.findIndex((a) => a.key === step.part) + 1
  return { before: STEPS - k + 1, after: STEPS - k }
}

// ---- Dấu tích lưu trong trình duyệt, riêng cho từng máy -------------------
// localStorage có thể bị chặn (cửa sổ ẩn danh, chặn cookie): khi đó dấu tích chỉ sống tới lúc tải lại trang.

const storageKey = (machine: string) => `pcsc-guide-done:${machine}`

export function loadDone(machine: string): Set<string> {
  try {
    const raw = localStorage.getItem(storageKey(machine))
    return new Set(raw ? (JSON.parse(raw) as string[]) : [])
  } catch {
    return new Set()
  }
}

export function saveDone(machine: string, done: Set<string>) {
  try {
    localStorage.setItem(storageKey(machine), JSON.stringify([...done]))
  } catch {
    // bỏ qua: không lưu được thì vẫn dùng bình thường trong phiên này
  }
}

/** Bước đầu tiên chưa đánh dấu xong (hết rồi thì về bước cuối) */
export function firstUndone(steps: GuideStep[], done: Set<string>) {
  const i = steps.findIndex((s) => !done.has(s.id))
  return i < 0 ? steps.length - 1 : i
}
