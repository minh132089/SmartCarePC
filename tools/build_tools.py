"""
Dựng bộ đồ nghề lắp máy theo các tờ vẽ của Codex trong design/tools/ (số trong file .md là chuẩn).
Chạy: blender -b --python tools/build_tools.py -- <thư mục ra> [tên ...]
Mỗi món có gốc và trục ghi trong .md của nó (đơn vị mm, +Y hướng lên khi đặt nằm trên bàn).
Bộ đầy đủ trên thảm (t_kit.glb) đặt từng món theo 00_overview.md.
"""
import sys, os, math
sys.path.insert(0, os.path.dirname(__file__))
from glib import *  # noqa: F401,F403,E402
from glib import _obj  # noqa: E402
from mathutils import Matrix  # noqa: E402

args = sys.argv[sys.argv.index('--') + 1:]
OUT = args[0]
ONLY = set(args[1:])
PARTS = {}


def part(fn):
    PARTS[fn.__name__] = fn
    return fn


def out(name):
    return os.path.join(OUT, f't_{name}.glb')


def helix(x0, x1, turns, R, r, m, name='coil'):
    """Lò xo / dây xoắn dọc trục X từ x0 tới x1, bán kính tâm dây R, bán kính dây r."""
    n = int(turns * 10)
    pts = [(x0 + (x1 - x0) * i / n, R * math.cos(2 * math.pi * turns * i / n), R * math.sin(2 * math.pi * turns * i / n)) for i in range(n + 1)]
    return bezier_tube(pts, r, m, seg=8, name=name, res=2)


# ---------------------------------------------------------------- 02 dây đeo chống tĩnh điện
def wrist_strap(compact=False):
    """Gốc ở tâm vòng đeo, +X từ vòng về đầu kẹp, vòng nằm trong mặt XZ, bản rộng theo Y (02_wrist_strap.md).
    compact: cách bày gọn trên thảm (dây xoắn gập về song song vòng, bao ~165 × 87)."""
    blue = mat('band', '#287AB0', 0.0, 0.85)
    blue_in = mat('band_in', '#215C7D', 0.0, 0.85)
    black = mat('plastic', '#202329', 0.0, 0.6)
    pu = mat('lead', '#17191D', 0.0, 0.55)
    steel = mat('steel', '#B9C0C6', 0.9, 0.3)
    tube((0, 0, 0), 32.0, 31.0, 20.0, 'y', blue, 64)
    tube((0, 0, 0), 31.0, 30.0, 20.0, 'y', blue_in, 64)
    box((29.0, -9.0, -11.0), (30.0, 9.0, 11.0), steel, bevel=0.4)                 # bản tiếp xúc da
    k = box((-38.0, -12.0, -12.0), (-28.0, 12.0, 12.0), black, bevel=1.5)          # khoá chỉnh đai, hai khe luồn
    boolean(k, box((-36.0, -10.5, -13.0), (-33.5, 10.5, 13.0), black, name='cut'))
    boolean(k, box((-32.5, -10.5, -13.0), (-30.0, 10.5, 13.0), black, name='cut'))
    box((30.0, -12.0, -12.0), (41.0, 12.0, 12.0), black, bevel=2.0)                # đế snap
    cyl((42.0, 0, 0), 5.0, 2.0, 'x', steel, 32)
    cyl((44.0, 0, 0), 2.0, 2.0, 'x', steel, 20)
    box((45.0, -4.5, -4.5), (73.0, 4.5, 4.5), pu, bevel=2.0)                       # vỏ điện trở 1 MΩ
    cyl((76.0, 0, 0), 1.6, 8.0, 'x', pu, 12, r2=1.1)                               # chống gập
    if compact:
        bezier_tube([(80.0, 0, 0), (90.0, 0, 12.0), (85.0, 0, 38.0), (80.0, 0, 38.0)], 1.1, pu)
        G_ = Matrix.Translation((70.0, 0, 38.0)) @ Matrix.Rotation(math.pi, 4, 'Y')
        coil = helix(-10.0, 80.0, 22, 6.4, 1.1, pu)
        coil.data.transform(G_)
        bezier_tube([(-10.0, 0, 38.0), (-22.0, 0, 42.0), (-32.0, 0, 48.0)], 1.1, pu)
        clip = Matrix.Translation((350.0, 0, 48.0)) @ Matrix.Rotation(math.pi, 4, 'Y')   # tay bóp bắt đầu ở x -32, mũi hướng -X
    else:
        bezier_tube([(80.0, 0, 0), (130.0, 0, 0), (173.0, 0, 0)], 1.1, pu)
        helix(173.0, 333.0, 40, 6.4, 1.1, pu)
        bezier_tube([(333.0, 0, 0), (360.0, 0, 0), (382.0, 0, 0)], 1.1, pu)
        clip = Matrix.Identity(4)
    before = set(bpy.context.scene.objects)
    # kẹp cá sấu 50 × 16 × 12, mũi hướng +X, bản lề tại x 407
    box((382.0, -6.0, -5.0), (402.0, 6.0, 5.0), black, bevel=1.5)                  # áo nhựa tay bóp
    for e in (-1, 1):
        jaw = prism([(400.0, e * 1.0), (432.0, e * 0.3), (432.0, e * 3.0), (400.0, e * 5.5)][::e], -4.0, 4.0, steel)
        jaw.name = 'jaw'
        for i in range(8):
            x = 414.0 + i * 2.2
            prism([(x, e * 0.3), (x + 1.1, e * 0.3), (x + 0.55, e * -0.6)][::e], -3.5, 3.5, steel, name='tooth')
    cyl((407.0, 0, 0), 1.2, 9.0, 'z', steel, 16)
    for o in set(bpy.context.scene.objects) - before:
        o.data.transform(clip)


# ---------------------------------------------------------------- 03 keo tản nhiệt + thẻ gạt
def thermal_paste():
    """Gốc ở tâm mặt ngoài đầu nắp, +X dọc ống từ nắp về đĩa ấn (03_thermal_paste.md)."""
    shell = mat('syringe', '#D6DADD', 0.0, 0.35, alpha=0.55)
    cap = mat('cap', '#34383D', 0.0, 0.7)
    plunger = mat('plunger', '#D9DCDE', 0.0, 0.45)
    rubber = mat('plunger_tip', '#8A8E92', 0.0, 0.8)
    paste = mat('paste', '#909398', 0.2, 0.6)
    card = mat('spreader', '#91C5E5', 0.0, 0.5)
    cyl((5.5, 0, 0), 3.0, 11.0, 'x', cap, 32)
    for i in range(12):
        a = 2 * math.pi * i / 12
        cbox((6.0, 2.9 * math.cos(a), 2.9 * math.sin(a)), (9.0, 0.5, 0.5), cap)
    cyl((12.0, 0, 0), 1.5, 4.0, 'x', shell, 24, r2=5.0)                            # vai côn
    tube((31.5, 0, 0), 5.0, 4.0, 35.0, 'x', shell, 40)
    cyl((28.0, 0, 0), 3.95, 28.0, 'x', paste, 32)
    box((47.0, -1.5, -9.0), (49.0, 1.5, 9.0), shell, bevel=0.8)                    # tai tì ngón (xoay nằm theo Z để ống nằm yên trên bàn)
    cyl((43.0, 0, 0), 3.95, 2.0, 'x', rubber, 32)
    cyl((55.5, 0, 0), 1.75, 23.0, 'x', plunger, 20)
    cyl((68.5, 0, 0), 6.5, 3.0, 'x', plunger, 40)
    prism(rounded_rect(40.0, 25.0, 2.0, cx=35.0, cy=27.0), -6.5, -5.5, card, plane='xz')   # thẻ gạt nằm phẳng cạnh ống



def lathe(profile, m, seg=48, name='lathe'):
    """Khối tròn xoay quanh trục X từ danh sách (x, bán kính)."""
    import bmesh
    bm = bmesh.new()
    rings = []
    for x, r in profile:
        rings.append([bm.verts.new((x, r * math.cos(2 * math.pi * k / seg), r * math.sin(2 * math.pi * k / seg))) for k in range(seg)])
    for a, b in zip(rings, rings[1:]):
        for k in range(seg):
            bm.faces.new((a[k], a[(k + 1) % seg], b[(k + 1) % seg], b[k]))
    bm.faces.new(rings[0][::-1])
    bm.faces.new(rings[-1])
    return _obj(bm, m, name, smooth_angle=50)


def phillips_bit(x0, x1, tip, m, slot):
    """Bit lục giác 6,35 dọc X từ x0 tới x1, hai mũi chữ thập dài 8 (bán kính tạo hình tip)."""
    cyl(((x0 + x1) / 2, 0, 0), 3.67, x1 - x0 - 16.0, 'x', m, 6)
    for e, xe in ((1, x1), (-1, x0)):
        cyl((xe - e * 4.0, 0, 0), tip, 8.0, 'x', m, 24, r2=tip * 0.25) if e > 0 else cyl((xe + 4.0, 0, 0), tip * 0.25, 8.0, 'x', m, 24, r2=tip)
        for a in (0.0, math.pi / 2):
            o = box((xe - e * 5.0 if e > 0 else xe, -0.3, -tip), (xe if e > 0 else xe + 5.0, 0.3, tip), slot, name='slot')
            o.data.transform(Matrix.Translation(((xe), 0, 0)) @ Matrix.Rotation(a, 4, 'X') @ Matrix.Translation((-xe, 0, 0)))


# ---------------------------------------------------------------- 01 tua vít đổi đầu
def screwdriver():
    """Gốc ở tâm mặt đuôi cán, +X từ đuôi cán tới mũi (01_screwdriver.md). Gắn bit PH2, bit PH1 để rời ở (175; 0; 30)."""
    core = mat('grip_black', '#202329', 0.0, 0.8)
    blue = mat('grip_blue', '#5796C5', 0.0, 0.8)
    steel = mat('steel_satin', '#AEB4BC', 1.0, 0.3)
    bit = mat('bit', '#858B93', 1.0, 0.4)
    dark = mat('hex_hole', '#0A0B0D', 0.0, 0.9)
    lathe([(0.0, 0.0), (0.0, 9.0), (1.5, 13.0), (6.0, 16.0), (55.0, 16.0), (75.0, 12.5), (90.0, 9.0), (100.0, 9.0), (100.0, 0.0)], core, 48)
    for k in range(4):                         # 4 dải cao su xanh ở ±Y, ±Z
        o = box((20.0, -3.5, 15.6), (55.0, 3.5, 16.4), blue, bevel=0.8, name='stripe')
        o.data.transform(Matrix.Rotation(k * math.pi / 2, 4, 'X'))
    cyl((96.0, 0, 0), 9.6, 4.0, 'x', blue, 40)
    cyl((121.0, 0, 0), 3.0, 42.0, 'x', steel, 24)
    cyl((159.5, 0, 0), 5.0, 35.0, 'x', steel, 32)
    cyl((177.05, 0, 0), 3.67, 0.1, 'x', dark, 6)
    phillips_bit(150.0, 200.0, 2.25, bit, dark)                                    # PH2 đang gắn
    before = set(bpy.context.scene.objects)
    phillips_bit(150.0, 200.0, 1.5, bit, dark)                                     # PH1 để rời
    for o in set(bpy.context.scene.objects) - before:
        o.data.transform(Matrix.Translation((0, -12.33, 30.0)))                   # nằm trên bàn cạnh tua vít


# ---------------------------------------------------------------- 05 dây rút + dây dán
def zip_tie(m, slot):
    """Một dây rút 100 × 2,5: gốc giữa cạnh ngoài đầu khoá, +X dọc dây, +Y chiều dày, +Z chiều rộng (05_cable_ties.md)."""
    head = box((0.0, -0.5, -2.25), (5.0, 3.0, 2.25), m, bevel=0.4)
    boolean(head, box((1.0, -1.0, -1.4), (3.0, 3.5, 1.4), m, name='cut'))
    box((5.0, -0.5, -1.25), (90.0, 0.5, 1.25), m)
    for i in range(0, 104, 4):                # răng một chiều (vẽ thưa, mỗi vạch đại diện 4 răng)
        box((8.0 + i * 0.8, 0.5, -0.9), (8.3 + i * 0.8, 0.62, 0.9), slot, name='tooth')
    prism([(90.0, -1.25), (100.0, -0.4), (100.0, 0.4), (90.0, 1.25)], -0.5, 0.5, m, plane='xz')


def cable_ties():
    """Nhóm bày trên thảm (00_overview.md mục 5): 6 dây rút đầu khoá phía -X, 2 dây dán gai; gốc ở tâm nhóm."""
    nylon = mat('nylon', '#202226', 0.0, 0.6)
    groove = mat('nylon_groove', '#121316', 0.0, 0.7)
    loop = mat('loop_face', '#292B2E', 0.0, 0.95)
    hook = mat('hook_face', '#17191C', 0.0, 0.8)
    for v in (-22.0, -17.0, -12.0, -7.0, -2.0, 3.0):
        before = set(bpy.context.scene.objects)
        zip_tie(nylon, groove)
        for o in set(bpy.context.scene.objects) - before:
            o.data.transform(Matrix.Translation((-75.0, 0.5, v)))
    for v in (15.0, 31.0):
        prism(rounded_rect(150.0, 12.0, 2.0, cx=0.0, cy=v), 0.0, 0.6, hook, plane='xz')
        prism(rounded_rect(150.0, 12.0, 2.0, cx=0.0, cy=v), 0.6, 1.2, loop, plane='xz')


# ---------------------------------------------------------------- 06 dây nguồn AC
def power_cord():
    """Dây 1500, Ø6,8, đầu C13 → phích tường hai chân tròn có tiếp đất (06_power_cord.md).
    Tư thế bày trên thảm (00_overview.md mục 6): cuộn dẹt quanh (0; 15), C13 ở (-55; -35) mặt cắm hướng -X,
    phích ở (45; -35) chân hướng +X. Gốc ở tâm nhóm, mặt bàn y = 0."""
    pvc = mat('pvc', '#17191C', 0.0, 0.6)
    soft = mat('cable_pvc', '#15171B', 0.0, 0.55)
    relief = mat('strain_relief', '#202329', 0.0, 0.7)
    metal = mat('contact', '#B8BEC5', 0.9, 0.3)
    hole = mat('hole', '#050506', 0.0, 0.9)
    # C13: mặt cắm x = -75, thân tới -35; mũi cắm vát hai góc trên, ba lỗ (tâm cục bộ (y; z)): (5; 0), (-4; -7), (-4; 7)
    cy, cz = 10.5, -35.0
    box((-65.0, cy - 10.5, cz - 14.0), (-35.0, cy + 10.5, cz + 14.0), pvc, bevel=2.0)
    nose = prism([(cy - 8.8, cz - 11.3), (cy - 8.8, cz + 11.3), (cy + 6.0, cz + 11.3), (cy + 8.8, cz + 8.5), (cy + 8.8, cz - 8.5), (cy + 6.0, cz - 11.3)], -75.0, -65.0, pvc, plane='yz')
    for dy, dz in ((5.0, 0.0), (-4.0, -7.0), (-4.0, 7.0)):
        boolean(nose, box((-76.0, cy + dy - 2.3, cz + dz - 0.7), (-63.0, cy + dy + 2.3, cz + dz + 0.7), hole, name='cut'))
        box((-66.0, cy + dy - 1.8, cz + dz - 0.15), (-65.0, cy + dy + 1.8, cz + dz + 0.15), metal)
    cyl((-22.5, cy * 0.6, cz), 6.0, 25.0, 'x', relief, 24, r2=3.4)
    # phích tường: cổ x 0…25, thân Ø38 x 25…65, hai chân Ø4,8 dài 19 ở z ±9,5, lỗ tiếp đất, hai dải đất hai bên
    py = 19.0
    cyl((12.5, py * 0.55, cz), 3.4, 25.0, 'x', relief, 24, r2=6.0)
    body = cyl((45.0, py, cz), 19.0, 40.0, 'x', pvc, 48)
    boolean(body, cyl((65.0, py + 10.0, cz), 2.75, 30.0, 'x', hole, 20, name='cut'))
    for dz in (-9.5, 9.5):
        cyl((74.5, py, cz + dz), 2.4, 19.0, 'x', metal, 20)
    for dy in (-18.6, 18.6):
        box((43.0, py + dy - 0.25, cz - 4.0), (63.0, py + dy + 0.25, cz + 4.0), metal)
    # cáp: từ cổ C13 xuống mặt bàn, quấn 2 vòng dẹt thu nhỏ dần quanh (0; 15), rồi bắc qua vòng ngoài vào cổ phích
    r = 3.4
    pts = [(-10.0, cy * 0.6, cz), (-16.0, r, cz + 4.0)]
    turns, n = 2.0, 48
    a0 = -math.pi / 2 - 0.35
    for i in range(n + 1):
        t = i / n
        a = a0 + (2 * math.pi * turns + 0.7) * t
        k = 1.0 - 0.3 * t
        pts.append((70.0 * k * math.cos(a), r, 15.0 + 42.0 * k * math.sin(a)))
    pts += [(4.0, r + 5.0, -24.0), (-8.0, py * 0.55, -33.0), (-2.0, py * 0.55, cz), (0.0, py * 0.55, cz)]
    bezier_tube(pts, 3.4, soft, seg=12, res=6)


# ---------------------------------------------------------------- 07 ăng-ten Wi-Fi
def wifi_antenna():
    """Một ăng-ten 110: gốc ở tâm miệng đai ốc, +X tới chóp, bản lề trục Z tại x 20 (07_wifi_antenna.md)."""
    abs_ = mat('antenna', '#202329', 0.0, 0.7)
    nylon = mat('hinge', '#101216', 0.0, 0.6)
    brass = mat('brass', '#C6A565', 1.0, 0.3)
    pin = mat('pin', '#AEB4BC', 1.0, 0.3)
    hole = mat('hole', '#050506', 0.0, 0.9)
    cyl((4.0, 0, 0), 4.62, 8.0, 'x', brass, 6)
    cyl((0.0, 0, 0), 2.2, 0.2, 'x', mat('insulator', '#E9E4D2', 0.0, 0.6), 20)
    cyl((-0.05, 0, 0), 0.5, 0.2, 'x', hole, 12)
    cyl((11.0, 0, 0), 4.0, 6.0, 'x', nylon, 24)
    box((14.0, -6.0, -5.0), (26.0, 6.0, 5.0), nylon, bevel=1.5)
    for z in (-5.25, 5.25):
        cyl((20.0, 0, z), 2.5, 0.5, 'z', pin, 16)
    cyl((64.0, 0, 0), 4.5, 76.0, 'x', abs_, 32, r2=3.6)
    lathe([(102.0, 3.6), (105.0, 3.5), (108.0, 2.6), (109.5, 1.4), (110.0, 0.0)], abs_, 32)


def wifi_antennas():
    """Hai chiếc bày song song (00_overview.md mục 7), tâm cục bộ (0; ∓12), gốc ở tâm nhóm."""
    for v in (-12.0, 12.0):
        before = set(bpy.context.scene.objects)
        wifi_antenna()
        for o in set(bpy.context.scene.objects) - before:
            o.data.transform(Matrix.Translation((-55.0, 0, v)))


# ---------------------------------------------------------------- 04 khay vít
def screw_x(c, head_d, head_h, shank_d, L, m_head, m_shank, slot, tip=False, knurl=False):
    """Vít nằm ngang dọc +X, tâm chiều dài tổng ở c, đầu vít phía -X có rãnh chữ thập."""
    cx, cy, cz = c
    x0 = cx - (head_h + L) / 2
    cyl((x0 + head_h / 2, cy, cz), head_d / 2, head_h, 'x', m_head, 24 if knurl else 20)
    if knurl:
        for k in range(16):
            a = 2 * math.pi * k / 16
            cbox((x0 + head_h / 2, cy + head_d / 2 * math.cos(a), cz + head_d / 2 * math.sin(a)), (head_h, 0.5, 0.5), m_head)
    for a in (0.0, math.pi / 2):
        o = cbox((x0 - 0.01, 0, 0), (0.2, head_d * 0.55, 0.5), slot, name='slot')
        o.data.transform(Matrix.Translation((0, cy, cz)) @ Matrix.Rotation(a, 4, 'X'))
    if tip:
        cyl((x0 + head_h + (L - 2.0) / 2, cy, cz), shank_d / 2, L - 2.0, 'x', m_shank, 12)
        cyl((x0 + head_h + L - 1.0, cy, cz), shank_d / 2, 2.0, 'x', m_shank, 12, r2=0.3)
    else:
        cyl((x0 + head_h + L / 2, cy, cz), shank_d / 2, L, 'x', m_shank, 12)


def screw_kit():
    """Khay nhựa trong 150 × 100 × 18,5, 9 ô, nắp mở 105° ra sau (04_screw_kit.md). Gốc ở tâm mặt đáy ngoài,
    +X cạnh dài, +Z về phía chốt trước. Mọi vít nằm ngang dọc +X theo lưới trong bảng."""
    clear = mat('clear_tray', '#E5EDF0', 0.0, 0.16, alpha=0.35)
    ink = mat('cell_number', '#24496B', 0.0, 0.6)
    brass = mat('brass', '#C6A35C', 1.0, 0.3)
    steel = mat('zinc', '#AEB4BC', 1.0, 0.28)
    black = mat('black_screw', '#25272A', 0.6, 0.45)
    slot = mat('slot', '#0A0B0D', 0.0, 0.9)
    tray = prism(rounded_rect(150.0, 100.0, 3.0), 0.0, 18.5, clear, plane='xz')
    boolean(tray, prism(rounded_rect(147.0, 97.0, 1.5), 1.5, 19.5, clear, plane='xz', name='cut'))
    for x in (-24.75, 24.75):
        box((x - 0.75, 1.5, -48.5), (x + 0.75, 18.5, 48.5), clear)
    for z in (-16.25, 16.25):
        box((-73.5, 1.5, z - 0.75), (73.5, 18.5, z + 0.75), clear)
    lid = prism(rounded_rect(150.0, 100.0, 3.0), 18.5, 20.0, clear, plane='xz')
    lid.data.transform(Matrix.Translation((0, 18.5, -48.5)) @ Matrix.Rotation(math.radians(-105), 4, 'X') @ Matrix.Translation((0, -18.5, 48.5)))
    for x in (-48.0, 48.0):
        cyl((x, 18.5, -48.5), 1.5, 12.0, 'x', clear, 16)
    box((-8.0, 10.5, 49.0), (8.0, 18.5, 50.5), clear)
    cells = {i + 1: (x, z) for i, (z, x) in enumerate([(z, x) for z in (-32.75, 0.0, 32.75) for x in (-49.0, 0.0, 49.0)])}
    for i, (x, z) in cells.items():
        t = label(str(i), x - 20.0, 12.0 - z, 1.52, 4.0, ink, align='LEFT')   # số ô in trên đáy, đọc từ phía trước
        t.data.transform(Matrix.Rotation(-math.pi / 2, 4, 'X'))
    grid = lambda c, nx, nz, px, pz: [(c[0] + (ix - (nx - 1) / 2) * px, c[1] + (iz - (nz - 1) / 2) * pz) for iz in range(nz) for ix in range(nx)]  # noqa: E731
    # 1: 9 cọc đồng lục giác 6,5 + chân ren 3,5
    for x, z in grid(cells[1], 3, 3, 13.0, 9.0):
        cyl((x - 1.75, 4.4, z), 2.887, 6.5, 'x', brass, 6)
        cyl((x + 3.25, 4.4, z), 1.5, 3.5, 'x', brass, 12)
        cyl((x - 5.0, 4.4, z), 1.3, 0.2, 'x', slot, 12)
    # 2: 9 vít bo M3 × 5
    for x, z in grid(cells[2], 3, 3, 13.0, 9.0):
        screw_x((x, 4.25, z), 5.5, 2.0, 3.0, 5.0, steel, steel, slot)
    # 3: 11 vít #6-32 × 6 đầu vành Ø7
    for x, z in grid(cells[3], 4, 3, 11.0, 9.0)[:11]:
        screw_x((x, 5.0, z), 7.0, 2.5, 3.5, 6.0, steel, steel, slot)
    # 4: 16 vít quạt tự ren 5 × 10 đầu Ø8, hai lớp so le
    for layer, (dy, dz) in enumerate(((0.0, 0.0), (4.5, 3.75))):
        for x, z in grid(cells[4], 2, 4, 22.0, 7.5):
            screw_x((x, 5.5 + dy, z + dz - 1.9), 8.0, 2.5, 5.0, 10.0, black, black, slot, tip=True)
    # 5, 6: 8 vít két M3 × 6 và 8 vít M3 × 30, hai lớp × 4
    for cell, L in ((5, 6.0), (6, 30.0)):
        for yy in (4.25, 9.75):
            for dz in (-10.5, -3.5, 3.5, 10.5):
                screw_x((cells[cell][0], yy, cells[cell][1] + dz), 5.5, 2.0, 3.0, L, black, black, slot)
    # 7: vít M2 × 2,5 + cọc M.2 Ø4 cao 3
    screw_x((cells[7][0] - 8.0, 3.25, cells[7][1]), 3.5, 0.7, 2.0, 2.5, steel, steel, slot)
    cyl((cells[7][0] + 8.0 - 1.0, 3.5, cells[7][1]), 2.0, 3.0, 'x', steel, 20)
    cyl((cells[7][0] + 8.0 + 1.5, 3.5, cells[7][1]), 1.0, 2.0, 'x', steel, 12)
    # 8: 6 vít tay #6-32 đầu khía Ø9 (4 dài 8 cho kính, 2 dài 6 cho nắp kín)
    for k, (x, z) in enumerate(grid(cells[8], 3, 2, 15.0, 13.0)):
        screw_x((x, 6.0, z), 9.0, 5.0, 3.5, 8.0 if k < 4 else 6.0, black, black, slot, knurl=True)
    # 9: đầu tuýp lục giác 5 (dài 25): đầu Ø8 có miệng lục giác ở -X, cổ côn, chuôi lục giác 6,35
    cx, cz = cells[9]
    sock = cyl((cx - 6.0, 5.5, cz), 4.0, 13.0, 'x', steel, 32)
    boolean(sock, cyl((cx - 12.0, 5.5, cz), 2.887, 14.0, 'x', slot, 6, name='cut'))
    cyl((cx - 12.4, 5.5, cz), 2.887, 0.2, 'x', slot, 6)
    cyl((cx + 1.5, 5.5, cz), 4.0, 2.0, 'x', steel, 32, r2=3.67)
    cyl((cx + 7.5, 5.5, cz), 3.67, 10.0, 'x', steel, 6)


# ---------------------------------------------------------------- 08 kìm cắt sát
def flush_cutter():
    """Kìm 125 × 15 × 46 khi khép: gốc giữa hai đầu cán, +X tới mũi, +Y chiều dày, cán xoè theo ±Z (08_flush_cutter.md)."""
    steel = mat('cutter_steel', '#363A3D', 0.85, 0.4)
    edge = mat('cutter_edge', '#BEC4C8', 0.9, 0.25)
    rivet = mat('rivet', '#ABB2B8', 0.9, 0.3)
    spring = mat('spring', '#AEB7BD', 0.9, 0.3)
    grip = mat('grip_rubber', '#659CCB', 0.0, 0.8)
    for e in (-1, 1):
        bezier_tube([(3.0, 0, e * 16.2), (7.0, 0, e * 16.0), (40.0, 0, e * 15.0), (71.0, 0, e * 8.0), (78.0, 0, e * 6.5)], 7.0, grip, seg=16, res=8)
        prism([(76.0, e * 8.5), (78.0, e * 4.5), (97.0, e * -2.0), (99.0, e * 1.0), (97.0, e * 4.5), (80.0, e * 9.5)][::-e], -1.5, 1.5, steel, plane='xz')
        prism([(95.0, 0.0), (97.0, e * 5.5), (103.0, e * 9.0), (113.0, e * 6.0), (125.0, e * 0.3), (125.0, 0.0)][::-e], -2.5, 2.5, steel, plane='xz')
        box((113.0, 2.5, min(0.0, e * 1.6)), (125.0, 2.6, max(0.0, e * 1.6)), edge)   # mép mài, vát từ mặt +Y
    cyl((97.0, 0, 0), 2.0, 6.0, 'y', rivet, 16)
    for y in (-3.5, 3.5):
        cyl((97.0, y, 0), 4.5, 1.0, 'y', rivet, 24)
    bezier_tube([(84.0, 0, 7.0), (72.0, 0, 5.0), (66.0, 0, 0.0), (72.0, 0, -5.0), (84.0, 0, -7.0)], 0.6, spring, seg=8)



TOOLS = {
    'screwdriver': (screwdriver, 16.0),
    'wrist_strap': (wrist_strap, 10.0),
    'thermal_paste': (thermal_paste, 6.5),
    'cable_ties': (cable_ties, 0.0),
    'power_cord': (power_cord, 0.0),
    'wifi_antennas': (wifi_antennas, 6.0),
    'screw_kit': (screw_kit, 0.0),
    'flush_cutter': (flush_cutter, 7.5),
}
# Vị trí trên thảm theo 00_overview.md: (X0, Z0, θ độ, hàm dựng riêng cho thảm nếu có)
LAYOUT = {
    'screwdriver': (-160.0 - 100.0, -140.0, 0.0),   # gốc tua vít ở đuôi cán, tâm nhóm ở giữa thân
    'wrist_strap': (130.0, -135.0, 0.0),
    'thermal_paste': (-190.0 - 35.0, -35.0 - 10.0, 0.0),
    'cable_ties': (-185.0, 80.0, 0.0),
    'power_cord': (25.0, 135.0, 0.0),
    'wifi_antennas': (225.0, -20.0, 90.0),
    'screw_kit': (30.0, -20.0, 0.0),
    'flush_cutter': (205.0 - 62.5, 130.0, 0.0),   # tâm nhóm ở giữa chiều dài kìm
}


def kit():
    """Thảm 600 × 400 × 3, gốc ở tâm mặt trên thảm; các món đặt theo LAYOUT, nâng theo nửa bề dày."""
    pad = mat('mat', '#A9AEB3', 0.0, 0.9)   # thảm xám nhạt, tối hơn bản vẽ một chút để đồ màu sáng nổi lên
    prism(rounded_rect(600.0, 400.0, 8.0), -3.0, 0.0, pad, plane='xz')
    for name, (fn, lift) in TOOLS.items():
        if name not in LAYOUT:
            continue
        x0, z0, th = LAYOUT[name]
        before = set(bpy.context.scene.objects)
        fn(compact=True) if name == 'wrist_strap' else fn()
        M = Matrix.Translation((x0, lift, z0)) @ Matrix.Rotation(-math.radians(th), 4, 'Y')
        for o in set(bpy.context.scene.objects) - before:
            if o.type == 'MESH':
                o.data.transform(M)


if __name__ == '__main__':
    for name, (fn, _) in TOOLS.items():
        if ONLY and name not in ONLY:
            continue
        reset()
        fn()
        finish(out(name), main=f'tool_{name}')
    if not ONLY or 'kit' in ONLY:
        reset()
        kit()
        finish(out('kit'), main='tool_kit')
