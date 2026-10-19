"""
Dựng linh kiện mới và vỏ cho 6 họ case (design/families/00_families_spec.md + các tờ vẽ của Codex).
Chạy: blender -b --python tools/build_families.py -- <thư mục ra> [tên ...]
Dùng lại hàm dựng của bộ Gaming (tools/build_gaming.py) nhưng không sửa hay xuất lại linh kiện Gaming.
Đơn vị trong code là mm theo trục app; glib.finish() tự đổi sang đơn vị cảnh.
"""
import sys, os, math, json
sys.path.insert(0, os.path.dirname(__file__))
import build_gaming as G  # noqa: E402
from glib import *  # noqa: F401,F403,E402
from glib import _obj  # noqa: E402

args = sys.argv[sys.argv.index('--') + 1:]
OUT = args[0]
ONLY = set(args[1:])
PARTS = {}
PCB = G.PCB
BOARDS = json.load(open(os.path.join(os.path.dirname(__file__), '..', 'src', 'data', 'boards.json')))


def part(fn):
    PARTS[fn.__name__] = fn
    return fn


def out(name):
    return os.path.join(OUT, f'f_{name}.glb')


# ---------------------------------------------------------------- Bo mATX / ITX (mục 7.1)
def board(ff):
    """Bo theo bố trí trong src/data/boards.json. Gốc ở tâm bo, mặt lưng z = 0, linh kiện hướng +z,
    I/O ở mép -x, EPS ở mép +y. Cùng kiểu socket, khe DIMM, PCIe, M.2, đầu cắm như bo ATX Gaming."""
    L = BOARDS[ff]
    reset()
    pcb = mat('pcb', '#16181d', 0.1, 0.8)
    plastic = mat('plastic', '#101216', 0.0, 0.55)
    plastic2 = mat('plastic_grey', '#2a2f37', 0.0, 0.5)
    hole = mat('hole', '#050506', 0.0, 0.9)
    gold = mat('gold', '#c6a565', 1.0, 0.3)
    alu = mat('alu', '#aeb4bc', 0.85, 0.4)
    steel = mat('steel', '#9aa2ab', 0.9, 0.3)
    silk = mat('silk', '#d2d7dd', 0.0, 0.7)
    cyan = mat('cyan', '#22d3ee', 0.0, 0.45)
    chip = mat('chip', '#1b1d22', 0.2, 0.45)
    tin = mat('tin', '#c3c8ce', 0.9, 0.35)
    choke = mat('choke', '#3a3d43', 0.4, 0.6)
    copper = mat('copper', '#b9874b', 0.9, 0.35)

    W, H = L['w'], L['h']
    sx, sy = L['cpu']
    holes = [tuple(h) for h in L['holes']]
    pump = [(sx + dx, sy + dy) for dx in (-39.0, 39.0) for dy in (-39.0, 39.0)]
    brd = box((-W / 2, -H / 2, 0), (W / 2, H / 2, PCB), pcb)
    cuts = [cyl((x, y, PCB / 2), 1.7, PCB + 2, 'z', hole, 24) for x, y in holes]
    cuts += [cyl((x, y, PCB / 2), 1.75, PCB + 2, 'z', hole, 24) for x, y in pump]
    boolean(brd, join(cuts, 'holes_cut'))
    for i, (x, y) in enumerate(holes):
        ring((x, y), 3.6, 1.7, PCB + 0.02, tin)
        ring((x, y), 3.6, 1.7, -0.02, tin).data.flip_normals()
        tube((x, y, PCB / 2), 1.72, 1.7, PCB, 'z', tin, 24)
        G.phillips((x, y, PCB + 1.0), 0, 2.8, steel, hole, 'z', h=2.0)
    for x, y in pump:
        ring((x, y), 3.2, 1.75, PCB + 0.02, copper)
        ring((x, y), 3.2, 1.75, -0.02, copper).data.flip_normals()

    # Socket LGA (giống bo ATX)
    box((sx - 25, sy - 25, PCB), (sx + 25, sy + 25, PCB + 1.2), steel)
    sock = box((sx - 21.5, sy - 21.5, PCB), (sx + 21.5, sy + 21.5, 6.0), plastic)
    boolean(sock, G.cutter([((sx - 19.0, sy - 19.0, 4.5), (sx + 19.0, sy + 19.0, 7.0))], plastic))
    contacts = [(sx - 14.625 + i * 0.75, sy - 14.625 + j * 0.75, 0.32, 0.32)
                for i in range(40) for j in range(40) if not (10 <= i < 30 and 10 <= j < 30)]
    quads(contacts, 4.65, gold, axis='z', name='lga_contacts')
    for k in (-1, 1):
        box((sx + k * 18.9 - 0.5, sy + 4.2, 4.5), (sx + k * 18.9 + 0.5, sy + 5.8, 6.0), plastic)
    prism([(sx - 24, sy - 24), (sx - 20, sy - 24), (sx - 24, sy - 20)], PCB + 1.2, PCB + 1.25, mat('tri', '#d6ac50', 0.6, 0.4))
    for a, b in (((-22, -22), (22, -17)), ((-22, 17), (22, 22)), ((-22, -17), (-17, 17)), ((17, -17), (22, 17))):
        box((sx + a[0], sy + a[1], 6.2), (sx + b[0], sy + b[1], 7.0), steel, bevel=0.2)
    box((sx - 22, sy + 22, 3.0), (sx + 22, sy + 25.5, 7.0), steel, bevel=0.4)
    box((sx - 6, sy - 25.5, 6.2), (sx + 6, sy - 22, 7.0), steel, bevel=0.2)
    cyl((sx + 27, sy + 1, 5.6), 0.9, 48, 'y', steel, 12)
    cyl((sx + 30.5, sy - 23, 5.6), 0.9, 7, 'x', steel, 12)
    cbox((sx + 27, sy - 20.5, 4.0), (3.5, 2.5, 4.0), steel)

    # VRM: cuộn cảm bên trái socket + khối tản theo bảng
    for k in range(5):
        cbox((sx - 31.0, sy - 20 + k * 9.5, PCB + 3.0), (7, 7, 6), choke, bevel=0.6)
    vx, vy, vw, vh, vz = L['vrm']
    G.heatsink((vx, vy), (vw, vh, vz), alu, 'x', 8)

    # DIMM: gờ khoá DDR4, chốt hai đầu; khe lắp RAM màu xám + chốt cyan
    used = set(L['ram_used'])
    for i, x in enumerate(L['ram_x']):
        ry = L['ram_y']
        body = plastic2 if i in used else plastic
        Ld, top = 142.0, 8.4
        y0, y1 = ry - Ld / 2, ry + Ld / 2
        box((x - 3.0, y0, PCB), (x - 0.8, y1, PCB + top), body)
        box((x + 0.8, y0, PCB), (x + 3.0, y1, PCB + top), body)
        box((x - 0.8, y0, PCB), (x + 0.8, y1, PCB + 3.0), hole)
        quads([(ry, PCB + 4.5, Ld - 6, 3.0)], x - 0.79, gold, axis='x', name='dimm_contacts')
        quads([(ry, PCB + 4.5, Ld - 6, 3.0)], x + 0.79, gold, axis='-x', name='dimm_contacts')
        box((x - 0.8, ry + G.DDR4_KEY - 0.8, PCB), (x + 0.8, ry + G.DDR4_KEY + 0.8, PCB + 6.0), body)
        for e in (-1, 1):
            yy = ry + e * (Ld / 2 + 2.5)
            box((x - 3.2, yy - 2.5, PCB), (x + 3.2, yy + 2.5, PCB + 8.4), body)
            box((x - 2.6, yy - 1.6, PCB + 8.4), (x + 2.6, yy + 1.6, PCB + 15.0), cyan if i in used else plastic2, bevel=0.3)

    # PCIe: khe đầu bọc thép, chốt ở đầu +x
    latch = L.get('pcie_latch', 6.0)
    for k, (x, y) in enumerate(L['pcie']):
        x0, x1 = x - G.PCIE_LEN / 2, x + G.PCIE_LEN / 2
        box((x0, y - 3.75, PCB), (x1, y - 0.75, PCB + 11.0), plastic)
        box((x0, y + 0.75, PCB), (x1, y + 3.75, PCB + 11.0), plastic)
        box((x0, y - 0.75, PCB), (x1, y + 0.75, PCB + 4.0), hole)
        quads([(x, PCB + 6.0, G.PCIE_LEN - 4, 3.0)], y - 0.74, gold, axis='y', name='pcie_contacts')
        quads([(x, PCB + 6.0, G.PCIE_LEN - 4, 3.0)], y + 0.74, gold, axis='-y', name='pcie_contacts')
        box((x0 + G.PCIE_KEY - 0.9, y - 0.75, PCB), (x0 + G.PCIE_KEY + 0.9, y + 0.75, PCB + 9.0), plastic)
        box((x0 - 1.5, y - 3.75, PCB), (x0, y + 3.75, PCB + 11.0), plastic)
        box((x1, y - 3.75, PCB), (x1 + latch, y + 3.75, PCB + 9.0), plastic)
        box((x1 + 1.0, y - 1.4, PCB + 9.0), (x1 + latch + 2.0, y + 1.4, PCB + 12.5), plastic2, bevel=0.3)
        if k == 0:
            for lo, hi in (((x0 - 1.7, y - 4.15, PCB + 2.0), (x1 + 0.2, y - 3.75, PCB + 11.4)),
                           ((x0 - 1.7, y + 3.75, PCB + 2.0), (x1 + 0.2, y + 4.15, PCB + 11.4)),
                           ((x0 - 1.7, y - 4.15, PCB + 11.0), (x1 + 0.2, y - 2.4, PCB + 11.4)),
                           ((x0 - 1.7, y + 2.4, PCB + 11.0), (x1 + 0.2, y + 4.15, PCB + 11.4))):
                box(lo, hi, alu)
        label('PCIe x16' if k == 0 else f'PCIe {k + 1}', x0 + 14, y + 7.5, PCB + 0.02, 2.4, silk, align='LEFT')

    # M.2: miệng khe mở về +x, chân đỡ + lỗ ren
    mx, my = L['m2_mouth']
    scx, scy = L['m2_screw']
    mz0, mz1 = PCB + 2.6, PCB + 4.2
    m2 = box((mx - 6.0, my - 11.0, PCB), (mx, my + 11.0, PCB + 5.6), plastic)
    boolean(m2, G.cutter([((mx - 2.5, my - 9.5, mz0), (mx + 0.5, my + 9.5, mz1))], plastic))
    box((mx - 2.5, my - 6.5 - 0.5, mz0), (mx, my - 6.5 + 0.5, mz1), plastic)
    quads([(my + yy, (mz0 + mz1) / 2, 0.3, 1.2) for yy in [-9.0 + i * 0.5 for i in range(37)] if abs(yy + 6.5) > 0.8],
          mx - 2.48, gold, axis='x', name='m2_contacts')
    cyl((scx, scy, PCB + 1.5), 2.0, 3.0, 'z', mat('brass', '#b8a26a', 0.9, 0.35), 16)
    disc((scx, scy), 0.9, PCB + 3.02, hole, 12)
    for a, b in (((mx, my - 11.2), (scx, my - 10.9)), ((mx, my + 10.9), (scx, my + 11.2))):
        box((a[0], a[1], PCB), (b[0], b[1], PCB + 0.03), silk)
    if L.get('chipset'):
        cx, cy, cw, chh, cz = L['chipset']
        G.heatsink((cx, cy), (cw, chh, cz), alu, 'x', 7)

    # Đầu nguồn
    hd = L['headers']
    ax, ay = hd['atx24']
    G.power_socket((ax, ay), 12, 2, plastic, gold, (1, 0), (10.0, 52.0, 13.0),
                   rounded=(1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0), vertical=True)
    label('ATX_24', ax - 9, ay, PCB + 0.02, 2.4, silk, rot=90)
    ex, ey = hd['eps']
    G.power_socket((ex, ey), 4, 2, plastic, gold, (0, 1), (18.0, 9.0, 13.0), rounded=(0, 1, 1, 0, 1, 0, 0, 1), vertical=False)
    label('CPU_8', ex, ey - 8.0, PCB + 0.02, 2.4, silk)
    fans = [('CPU_FAN', hd['cpu_fan'])] + [(f'SYS_FAN{i + 1}', c) for i, c in enumerate(hd['sys'])]
    for name, c in fans:
        G.header_4pin(tuple(c), plastic, gold, plastic2, silk)
        label(name, c[0], c[1] + 5.0, PCB + 0.02, 1.7, silk)
    fx, fy = hd['panel']
    G.header_grid((fx, fy), 5, 2, {10}, plastic, gold, silk)
    label('F_PANEL', fx, fy - 5.2, PCB + 0.02, 1.6, silk)
    hx, hy = hd['audio']
    G.header_grid((hx, hy), 5, 2, {8}, plastic, gold, silk)
    label('HD_AUDIO', hx, hy - 5.2, PCB + 0.02, 1.6, silk)
    ux, uy = hd['usb3']
    G.header_grid((ux, uy), 10, 2, {20}, plastic, gold, silk, pitch=2.0, shroud=True, m_shroud=plastic2, vertical=True)
    label('USB3', ux - 7.5, uy, PCB + 0.02, 1.7, silk, rot=90)
    if hd.get('type_e'):
        # Đầu USB-C Type-E: thân 8 × 20 × 10 có khoá định hướng, lòng 20 tiếp điểm
        tx, ty = hd['type_e']
        te = box((tx - 4.0, ty - 10.0, PCB), (tx + 4.0, ty + 10.0, PCB + 10.0), plastic2)
        boolean(te, G.cutter([((tx - 2.6, ty - 8.6, PCB + 2.0), (tx + 2.6, ty + 8.6, PCB + 11.0))], plastic2))
        box((tx - 0.4, ty - 7.0, PCB + 2.0), (tx + 0.4, ty + 7.0, PCB + 8.0), plastic)
        for i in range(10):
            for sg in (-1, 1):
                box((tx + sg * 0.42, ty - 6.3 + i * 1.4, PCB + 3.0), (tx + sg * 0.45, ty - 5.9 + i * 1.4, PCB + 7.5), gold)
        box((tx + 4.0, ty - 2.0, PCB + 2.0), (tx + 4.6, ty + 2.0, PCB + 10.0), plastic2)   # gờ khoá
        label('USB-C', tx - 6.5, ty, PCB + 0.02, 1.6, silk, rot=90)

    # I/O sau tối giản: 2 USB-A, 1 RJ45, 3 jack TRS (mục 7.1), cổng hướng -x
    io = L['io']
    SX0, SX1 = io['x']
    XB = io['xb']
    Z = lambda h: PCB + h  # noqa: E731
    shield_m = mat('io_shield', '#bcc2c8', 0.8, 0.35)
    rim = mat('port_metal', '#c9ced4', 0.9, 0.3)
    cave = mat('port_cave', '#080a0d', 0.0, 0.9)
    shield = box((SX0, io['y'][0], PCB), (SX1, io['y'][1], PCB + 36.0), shield_m)
    hc = io['h']

    def yzp(pts, x0, x1, m):
        return prism([(y, Z(h)) for y, h in pts], x0, x1, m, plane='yz')

    def rect(yc, wy, hh):
        return [(yc - wy / 2, hc - hh / 2), (yc + wy / 2, hc - hh / 2), (yc + wy / 2, hc + hh / 2), (yc - wy / 2, hc + hh / 2)]

    for y in io['usb']:
        boolean(shield, yzp(rect(y, 14.6, 6.8), SX0 - 1, SX1 + 1, shield_m))
        p = yzp(rect(y, 14.6, 6.8), SX0 + 0.1, XB, rim)
        boolean(p, yzp(rect(y, 13.0, 5.0), SX0 - 1, SX0 + 9, rim))
        yzp(rect(y, 13.0, 5.0), SX0 + 9, SX0 + 9.2, cave)
        box((SX0 + 1.0, y - 5.5, Z(hc) + 0.2), (SX0 + 9.0, y + 5.5, Z(hc) + 2.0), mat('usb_5', '#1f6fd1', 0.0, 0.55))
        for i in range(4):
            box((SX0 + 2.0, y - 3.4 + i * 2.2, Z(hc) + 0.05), (SX0 + 5.0, y - 2.6 + i * 2.2, Z(hc) + 0.2), gold)
    ry = io['rj45']
    boolean(shield, yzp(rect(ry, 17.0, 15.0), SX0 - 1, SX1 + 1, shield_m))
    lan = yzp(rect(ry, 19.0, 18.0), SX0 + 0.1, XB, rim)
    boolean(lan, yzp(rect(ry, 16.0, 14.0), SX0 - 1, SX0 + 12, rim))
    yzp(rect(ry, 16.0, 14.0), SX0 + 12, SX0 + 12.2, cave)
    for i in range(8):
        hh = hc - 4.55 + i * 1.3
        box((SX0 + 2.0, ry - 6.9, Z(hh) - 0.25), (SX0 + 11.0, ry - 6.6, Z(hh) + 0.25), gold)
    for y, col in zip(io['jacks'], ('#7ebe72', '#e787a0', '#5ec8f2')):
        boolean(shield, cyl(((SX0 + SX1) / 2, y, Z(hc)), 3.1, 3.0, 'x', shield_m, 20))
        box((SX0 + 0.1, y - 6.0, Z(hc) - 6.0), (XB, y + 6.0, Z(hc) + 6.0), mat('io_ink', '#101216', 0.0, 0.6))
        tube((SX0 - 0.1, y, Z(hc)), 3.0, 1.75, 0.8, 'x', mat(f'jack_{col}', col, 0.0, 0.45), 24)
        cyl((SX0 + 0.2, y, Z(hc)), 1.75, 0.2, 'x', cave, 16)
    assert len(shield.data.polygons) > 20, 'lá chắn I/O bị khoét hỏng'
    # nắp nhựa I/O phía trong mép bo, chừa lỗ bắt vít
    cx0, cx1 = io['cover']
    cover = box((cx0, io['y'][0] + 2.0, PCB), (cx1, io['y'][1] - 2.0, PCB + 30.0), mat('io_cover', '#23272e', 0.4, 0.45))
    for x, y in holes:
        if cx0 - 4.5 < x < cx1 + 4.5 and io['y'][0] - 4.5 < y < io['y'][1] + 4.5:
            boolean(cover, cyl((x, y, PCB + 15.0), 4.5, 40.0, 'z', plastic, 24))

    # Chi tiết nhỏ: pin CMOS, chip audio
    bx, by = L['battery']
    cyl((bx, by, PCB + 1.6), 10.0, 3.2, 'z', tin, 32)
    cbox(tuple(L['audio_chip']) + (PCB + 0.5,), (8, 8, 1.0), chip)
    finish(out(f'motherboard_{ff.lower()}'), main='part_motherboard')


@part
def motherboard_matx():
    board('mATX')


@part
def motherboard_itx():
    board('ITX')


# ---------------------------------------------------------------- GPU 240 hai quạt (mục 7.2)
@part
def gpu_240_dual():
    """Card 240 × 45 × 120, hai quạt Ø90. Gốc như GPU 280: x = 0 giữa chiều dài, y = 0 giữa bề dày
    (tấm lưng +y, quạt -y), z = 0 ở mép PCB phía bo. u = x + 120 tính từ lá cổng."""
    reset()
    Lg, T, Hg = 240.0, 45.0, 120.0
    pcb_y = T / 2 - 2.8                       # 19,7
    fan_u = (60.0, 180.0)
    shroud = mat('gpu_shroud', '#16181d', 0.15, 0.55)
    gun = mat('gpu_gunmetal', '#4b5058', 0.85, 0.32)
    silver = mat('gpu_silver', '#8d949c', 0.9, 0.25)
    fin = mat('gpu_fin', '#a3a9b0', 0.85, 0.45)
    pcb = mat('gpu_pcb', '#202327', 0.1, 0.7)
    gold = mat('gold', '#cba34d', 1.0, 0.28)
    back = mat('gpu_backplate', '#25282d', 0.5, 0.45)
    bracket_m = mat('gpu_bracket', '#b9bec5', 0.9, 0.3)
    hole = mat('hole', '#050506', 0.0, 0.9)
    plastic = mat('plastic', '#111317', 0.0, 0.55)
    blade_m = mat('gpu_blade', '#101216', 0.0, 0.45)
    copper = mat('heatpipe', '#b9874b', 0.9, 0.3)
    X = lambda u: u - Lg / 2  # noqa: E731
    yt = T / 2
    vent = [(186, 32), (190, 28), (228, 28), (232, 32), (232, 92), (228, 96), (190, 96), (186, 92)]
    bp = box((X(2), yt - 2.0, 2), (X(239), yt, Hg - 3), back)
    py0, py1 = pcb_y - 0.8, pcb_y + 0.8
    brd = box((X(1.5), py0, 0), (X(238), py1, Hg - 4), pcb)
    for o in (bp, brd):
        boolean(o, prism([(X(u), v) for u, v in vent], py0 - 1.0, yt + 1.0, back, plane='xz'))
    for u in (20, 90, 160):
        for v in (12, Hg - 13):
            cyl((X(u), yt + 0.15, v), 1.6, 0.3, 'y', silver, 12)
    f0, f1 = G.GPU_FINGERS
    kx = f0 + G.PCIE_KEY
    box((X(f0), py0, -8.0), (X(kx - 1.0), py1, 0.01), pcb)
    box((X(kx + 1.0), py0, -8.0), (X(f1), py1, 0.01), pcb)
    box((X(144.5), py0, -6.0), (X(148.5), py1, 0.01), pcb)
    box((X(148.5), py0, -3.0), (X(151.5), py1, 0.01), pcb)
    short = [f0 + 0.5 + (kx - 1.0 - f0 - 1.0) * i / 10 for i in range(11)]
    long_ = [kx + 1.5 + (f1 - 0.5 - kx - 1.5) * i / 70 for i in range(71)]
    pads = [(X(u), -4.2, 0.6, 7.0) for u in short + long_]
    quads(pads, py1 + 0.02, gold, axis='y', name='fingers_a')
    quads(pads, py0 - 0.02, gold, axis='-y', name='fingers_b')
    for i in range(105):   # khối lá nhôm 230 × 106 × 26
        u = 5.0 + i * (230.0 / 104)
        box((X(u) - 0.25, -8.0, 7.0), (X(u) + 0.25, pcb_y - 1.5, Hg - 7.0), fin)
    for v in (30.0, 60.0, 90.0):
        cyl((0, 6.0, v), 3.0, 224.0, 'x', copper, 16)
    outline = [(X(3), 2), (X(224), 2), (X(239), 18), (X(239), Hg - 18), (X(224), Hg - 2), (X(3), Hg - 2)]
    base = prism(outline, -yt, -yt + 2.0, shroud, plane='xz', name='shroud_face')
    boolean(base, join([cyl((X(u), -yt + 1.0, 60.0), 46.0, 6.0, 'y', shroud, 64) for u in fan_u], 'fan_holes'))
    wall = -2.0
    for poly in ([(X(3), Hg - 2), (X(224), Hg - 2), (X(224), Hg), (X(3), Hg)],
                 [(X(3), 0), (X(224), 0), (X(224), 2), (X(3), 2)],
                 [(X(224), 0), (X(239), 16), (X(239), 18), (X(224), 2)],
                 [(X(224), Hg - 2), (X(239), Hg - 18), (X(239), Hg - 16), (X(224), Hg)],
                 [(X(238), 16), (X(240), 18), (X(240), Hg - 18), (X(238), Hg - 16)],
                 [(X(2), 2), (X(4), 2), (X(4), Hg - 2), (X(2), Hg - 2)]):
        prism(poly, -yt, wall, shroud, plane='xz')
    prism([(X(3), Hg - 2), (X(224), Hg - 2), (X(224), Hg), (X(3), Hg)], wall, 9.0, shroud, plane='xz')
    for u in fan_u:
        tube((X(u), -yt + 0.6, 60.0), 48.5, 46.0, 1.2, 'y', gun, 64)
    for st in ([(X(117), 2), (X(123), 2), (X(110), 60), (X(123), Hg - 2), (X(117), Hg - 2), (X(104), 60)],
               [(X(5), 12), (X(10), 12), (X(10), Hg - 12), (X(5), Hg - 12)],
               [(X(224), 4), (X(228), 4), (X(237), 18), (X(233), 18)],
               [(X(224), Hg - 4), (X(228), Hg - 4), (X(237), Hg - 18), (X(233), Hg - 18)]):
        prism(st, -yt, -yt + 0.4, silver, plane='xz', name='accent')
    spin = {}
    for i, u in enumerate(fan_u):
        bl = G.blades(15.0, 44.0, 11.0, 11, blade_m, sweep=30, skew=0.5)
        hub_m = mat('gpu_hub_cap', '#1b1d22', 0.1, 0.8)   # mặt tâm quạt phẳng: để nhám cho khỏi loá đèn
        bl[1].data.materials[0] = hub_m
        bl[2].data.materials[0] = hub_m
        G.transform(bl, Matrix.Rotation(math.pi, 4, 'X'))
        G.transform(bl, Matrix.Translation((X(u), -12.0 - 2.5, 60.0)))
        spin[f'gpu_fan_{i + 1}'] = bl
    # ổ nguồn 8 chân tâm (105; 12,75; 120), miệng +z
    G.power_socket((X(225.0), 12.75), 4, 2, plastic, mat('contact', '#b8bec5', 0.9, 0.3), (0, 1),
                   (18.0, 10.7, 12.0), rounded=(1, 0, 1, 0, 0, 1, 0, 1), vertical=False, base=Hg - 12.0, depth=6.0)
    # lá cổng 2 khe Y -14,94…25,7, Z -2…118; 3 DP + 1 HDMI; 2 tai vít (Y 15,54 / -4,78; Z 113)
    by0, by1 = pcb_y + 6.0 - 40.64, pcb_y + 6.0
    plate = box((X(-1.0), by0, -2.0), (X(0.0), by1, 118.0), bracket_m)
    yc = by1 - 13.0
    port_cut = []
    for v, kind in ((20.0, 'dp'), (44.0, 'dp'), (68.0, 'dp'), (92.0, 'hdmi')):
        if kind == 'dp':
            prof = [(v - 8.0, yc - 2.5), (v + 8.0, yc - 2.5), (v + 8.0, yc + 1.0), (v + 6.5, yc + 2.5), (v - 8.0, yc + 2.5)]
        else:
            prof = [(v - 7.0, yc - 2.5), (v + 7.0, yc - 2.5), (v + 7.0, yc + 0.5), (v + 5.0, yc + 2.5), (v - 5.0, yc + 2.5), (v - 7.0, yc + 0.5)]
        port_cut.append((prof, v))
    vents = [cyl((X(-0.5), by0 + 4.0 + i * 3.6 + (1.8 if jj % 2 else 0), 4 + jj * 6.6), 1.3, 3.0, 'x', hole, 6)
             for i in range(4) for jj in range(17) if by0 + 4.0 + i * 3.6 + (1.8 if jj % 2 else 0) <= by1 - 24.0]
    cuts = [prism([(y, z) for z, y in pr], X(-2.0), X(1.0), hole, plane='yz') for pr, v in port_cut]
    boolean(plate, join(cuts + vents, 'bracket_cut'))
    for prof, v in port_cut:
        shell = prism([(y, z) for z, y in prof], X(0.0), X(6.0), silver, plane='yz')
        inner = [((z - v) * 0.9 + v, (y - yc) * 0.75 + yc) for z, y in prof]
        boolean(shell, prism([(y, z) for z, y in inner], X(-0.5), X(5.0), silver, plane='yz'))
        box((X(5.0), yc - 2.6, v - 8.0), (X(6.0), yc + 2.6, v + 8.0), hole)
        box((X(1.5), yc - 0.5, v - 5.5), (X(5.0), yc + 0.5, v + 5.5), plastic)
    steel = mat('steel', '#a8adb4', 0.9, 0.3)
    for yy in (by1 - 10.16, by1 - 30.48):
        boolean(plate, G.cutter([((X(-2.0), yy - 1.9, 111.0), (X(1.0), yy + 1.9, 119.0))], hole))
        cyl((X(-1.0) - 1.0, yy, 113.0), 2.8, 2.0, 'x', steel, 20)
        cyl((X(-1.0) + 3.0, yy, 113.0), 1.75, 6.0, 'x', steel, 12)
    finish(out('gpu_240_dual'), groups=spin, main='part_gpu')


# ---------------------------------------------------------------- Nguồn SFX (mục 7.2)
@part
def psu_sfx_100():
    """SFX 100 (x) × 63,5 (y) × 125 (z), gốc tâm. C14 + công tắc ở mặt -x, dây liền ra +x, quạt 92 hút -y."""
    reset()
    body_m = mat('psu_body', '#16181d', 0.25, 0.6)
    inner_m = mat('psu_inside', '#0b0c0e', 0.2, 0.8)
    grille = mat('psu_grille', '#252932', 0.7, 0.4)
    plastic = mat('plastic', '#101216', 0.0, 0.55)
    rubber = mat('rubber', '#17191c', 0.0, 0.8)
    metal = mat('contact', '#b8bec5', 0.9, 0.3)
    white = mat('print', '#f2f4f7', 0.0, 0.6)
    steel = mat('steel', '#a8adb4', 0.9, 0.3)
    D, Hh, Wd = 100.0, 63.5, 125.0
    xb = -D / 2
    body = box((-D / 2, -Hh / 2, -Wd / 2), (D / 2, Hh / 2, Wd / 2), body_m, bevel=1.0)
    c14 = [(-9.0 - 9.0, 43 - 11.5), (-9.0 + 6.0, 43 - 11.5), (-9.0 + 9.0, 43 - 8.5), (-9.0 + 9.0, 43 + 8.5), (-9.0 + 6.0, 43 + 11.5), (-9.0 - 9.0, 43 + 11.5)]
    boolean(body, prism(c14, xb - 1.0, xb + 12.0, inner_m, plane='yz'))
    boolean(body, cyl((0, -Hh / 2 + 7.0, 0), 45.0, 14.5, 'y', inner_m, 64))           # khoang quạt 92
    boolean(body, cutter_box((xb + 1.0, -24.0, -52.0), (xb + 20.0, 24.0, 28.0), inner_m))  # khoang sau lưới, chừa tấm lưng dày 1
    hexes = []
    row, y = 0, -22.0
    while y < 22.0:
        z = -52.0 + (2.5 if row % 2 else 0)
        while z < 26.0:
            hexes.append(cyl((xb + 0.5, y, z), 2.0, 3.0, 'x', inner_m, 6))
            z += 5.0
        y += 4.35
        row += 1
    boolean(body, join(hexes, 'hex_cut'))
    box((xb + 19.0, -24.0, -52.0), (xb + 19.5, 24.0, 28.0), inner_m)   # vách tối sau khoang
    for dy, dz in ((5.0, 0.0), (-4.0, -7.0), (-4.0, 7.0)):
        box((xb + 2.0, -9.0 + dy - 2.0, 43 + dz - 0.4), (xb + 10.0, -9.0 + dy + 2.0, 43 + dz + 0.4), metal)
    box((xb - 0.8, 17 - 10.0, 43 - 7.0), (xb + 0.5, 17 + 10.0, 43 + 7.0), plastic)
    rk = box((xb - 2.6, 17 - 8.5, 43 - 5.0), (xb - 0.8, 17 + 8.5, 43 + 5.0), plastic, bevel=0.5)
    rk.data.transform(Matrix.Translation((-(xb - 0.8), -17, 0)))
    rk.data.transform(Matrix.Rotation(math.radians(8), 4, 'Z'))
    rk.data.transform(Matrix.Translation((xb - 0.8, 17, 0)))
    box((xb - 2.7, 20.0, 42.6), (xb - 2.6, 23.0, 43.4), white)
    for y, z in ((25, 55), (-25, 55), (25, -55), (-25, -55)):    # 4 lỗ ren M3 mặt sau
        cyl((xb - 0.1, y, z), 2.4, 0.3, 'x', steel, 16)
        cyl((xb - 0.3, y, z), 1.3, 0.2, 'x', mat('hole', '#050506', 0, 0.9), 12)
    yb = -Hh / 2
    for R in (45.0, 36.0, 26.0, 16.0):
        torus((0, yb - 0.8, 0), R, 0.7, 'y', grille, 64, 8)
    for k in range(4):
        o = cyl((22.5, yb - 0.8, 0), 0.7, 45.0, 'x', grille, 8)
        o.data.transform(Matrix.Rotation(math.radians(45 + 90 * k), 4, 'Y'))
    for sx in (-1, 1):                      # 4 vít quạt 92 ở mặt đáy, bước 82,5 (tờ parts/psu_sfx_100)
        for sz in (-1, 1):
            G.phillips((sx * 41.25, yb - 0.3, sz * 41.25), 0, 2.6, steel, mat('hole', '#050506', 0, 0.9), 'y', h=0.6, sign=-1)
    bl = G.blades(12.0, 42.0, 12.0, 7, mat('psu_blade', '#101216', 0.0, 0.5), sweep=40)
    G.transform(bl, Matrix.Translation((0, -24.0, 0)))
    gy, gz = -13.0, 35.0
    tube((D / 2 + 1.0, gy, gz), 9.0, 6.5, 3.0, 'x', rubber, 32)
    for (dy, dz), rad in zip(((3.0, 0.0), (-2.5, -3.0), (-2.5, 3.0)), (3.4, 2.6, 2.6)):
        bezier_tube([(D / 2 - 2.0, gy + dy, gz + dz), (D / 2 + 8.0, gy + dy * 1.4, gz + dz * 1.4), (D / 2 + 16.0, gy + dy * 1.8 - 4.0, gz + dz * 1.8)], rad, rubber)
    finish(out('psu_sfx_100'), main='part_psu')


def cutter_box(lo, hi, m):
    return box(lo, hi, m, name='cut')


# ---------------------------------------------------------------- Gá tản dùng chung: tai 78 × 78, vít lò xo
def cooler_mount(body):
    """Bộ gá 4 tai quanh đế (z 4…6 trên IHS), lỗ Ø3,4 ở (±39; ±39), 4 vít lò xo M3 × 12 như bơm Gaming."""
    steel = mat('steel', '#929aa3', 0.9, 0.3)
    slot = mat('hole', '#050506', 0.0, 0.9)
    R = 39.0 * math.sqrt(2)
    tab = [(18.0, -4.5), (R - 5.0, -4.5)] + [(R + 6.0 * math.cos(math.radians(a)), 6.0 * math.sin(math.radians(a))) for a in range(-110, 111, 10)] + [(R - 5.0, 4.5), (18.0, 4.5)]
    for sx in (-1, 1):
        for sy in (-1, 1):
            arm = prism(tab, 4.0, 6.0, body, name='arm')
            arm.data.transform(Matrix.Rotation(math.atan2(sy, sx), 4, 'Z'))
            boolean(arm, cyl((39.0 * sx, 39.0 * sy, 5.0), 1.7, 4.0, 'z', body, 16))
            c = (39.0 * sx, 39.0 * sy)
            cyl((c[0], c[1], 5.0), 1.5, 12.0, 'z', steel, 12)
            G.phillips((c[0], c[1], 12.5), 0, 3.2, steel, slot, 'z', h=3.0)
            G.spring(c, 2.25, 6.0, 11.0, 4, 0.6, steel)


# ---------------------------------------------------------------- Tản khí tháp 155 (mục 7.2)
@part
def tower_air_155():
    """Gốc ở tâm mặt IHS, z hướng ra khỏi bo. Đế 40 × 40 × 5, 4 ống đồng Ø6, khối lá X -40…10 / Y ±60 / Z 30…155
    (lá ngang dày 0,3 bước 1,8), quạt 120 tại X 10…35 thổi -X, kẹp bằng 2 kẹp thép."""
    reset()
    copper = mat('copper', '#b87333', 0.95, 0.28)
    alu = mat('alu_fin', '#b8bec6', 0.45, 0.4)   # ít kim loại hơn để lá nhôm sáng bạc trong app
    body = mat('mount_black', '#16181d', 0.3, 0.5)
    nickel = mat('fin_top', '#2a2e35', 0.6, 0.35)
    box((-20.0, -20.0, 0.0), (20.0, 20.0, 5.0), copper, bevel=0.6)
    box((-22.0, -22.0, 5.0), (22.0, 22.0, 8.0), body)
    cooler_mount(body)
    for x in (-32.0, -12.0):                 # 4 ống: lên từ đế, xuyên khối lá, nhô nắp trên
        for y in (-22.0, 22.0):
            bezier_tube([(x * 0.4, y * 0.5, 2.5), (x * 0.4, y * 0.5, 14.0), (x, y, 28.0), (x, y, 158.0)], 3.0, copper, seg=10, res=10)
    n = int((155.0 - 30.0) / 1.8)
    for k in range(n):
        z = 30.0 + k * 1.8
        box((-40.0, -60.0, z), (10.0, 60.0, z + 0.3), alu)
    box((-40.0, -60.0, 152.0), (10.0, 60.0, 155.0), nickel, bevel=0.4)       # nắp trên
    for x in (-32.0, -12.0):
        for y in (-22.0, 22.0):
            cyl((x, y, 155.6), 3.4, 1.2, 'z', nickel, 20)
    frame_m = mat('fan_frame', '#16181d', 0.0, 0.6)
    parts = G.fan_frame(120.0, 25.0, frame_m, r_open=57.0, struts_side=1, hole_d=4.3, arrow_face='+z')
    bl = G.blades(20.0, 55.0, 18.0, 7, mat('fan_blade', '#252932', 0.0, 0.45), sweep=42)
    # quạt dựng trục y -> quay để trục theo x (thổi -x), đặt tâm (22,5; 0; 90)
    R = Matrix.Rotation(math.radians(90), 4, 'Z')
    G.transform(parts + bl, R)
    G.transform(parts + bl, Matrix.Translation((22.5, 0.0, 90.0)))
    steel = mat('steel', '#929aa3', 0.9, 0.3)
    for y in (-58.0, 58.0):                   # hai kẹp thép Ø1,2 giữ quạt
        bezier_tube([(34.0, y, 40.0), (12.0, y, 40.0), (8.0, y + (2 if y < 0 else -2), 60.0), (8.0, y, 120.0), (12.0, y, 140.0), (34.0, y, 140.0)], 0.6, steel, seg=6)
    finish(out('tower_air_155'), groups={'cooler_fan': bl}, main='part_cooler')


# ---------------------------------------------------------------- Tản thấp 47 (mục 7.2)
@part
def low_profile_47():
    """Gốc ở tâm mặt IHS. Bao 92 × 92 × 47: đế 40 × 40 × 5, lá dọc + ống thấp Z 5…33, quạt 92 × 14 tại Z 33…47 thổi -z."""
    reset()
    copper = mat('copper', '#b87333', 0.95, 0.28)
    alu = mat('alu_fin', '#b8bec6', 0.45, 0.4)   # ít kim loại hơn để lá nhôm sáng bạc trong app
    body = mat('mount_black', '#16181d', 0.3, 0.5)
    box((-20.0, -20.0, 0.0), (20.0, 20.0, 5.0), copper, bevel=0.6)
    cooler_mount(body)
    # lá dọc; bốn góc chừa hốc 13 × 13 để tuốc nơ vít xuống tới vít lò xo (±39; ±39)
    for k in range(int(92 / 1.8)):
        x = -46.0 + 0.9 + k * 1.8
        if abs(x) > 32.5:
            box((x - 0.15, -32.5, 9.0), (x + 0.15, 32.5, 33.0), alu)
        else:
            box((x - 0.15, -46.0, 9.0), (x + 0.15, 46.0, 33.0), alu)
    box((-24.0, -24.0, 5.0), (24.0, 24.0, 9.0), copper)                  # khối truyền nhiệt trên đế
    for y in (-30.0, -10.0, 10.0, 30.0):
        cyl((0.0, y, 12.0), 3.0, 92.0, 'x', copper, 12)
    frame_m = mat('fan_frame', '#16181d', 0.0, 0.6)
    fr = box((-46.0, -46.0, 33.0), (46.0, 46.0, 47.0), frame_m)
    boolean(fr, cyl((0, 0, 40.0), 44.0, 16.0, 'z', frame_m, 64))
    for sx in (-1, 1):
        for sy in (-1, 1):
            boolean(fr, cyl((41.25 * sx, 41.25 * sy, 40.0), 1.7, 16.0, 'z', frame_m, 12))
            G.phillips((41.25 * sx, 41.25 * sy, 47.5), 0, 2.6, mat('steel', '#929aa3', 0.9, 0.3), mat('hole', '#050506', 0, 0.9), 'z', h=1.0)
    cyl((0, 0, 34.5), 14.0, 3.0, 'z', frame_m, 40)
    for k in range(4):
        o = cbox((0, 0, 34.5), (44.0, 3.0, 3.0), frame_m)
        o.data.transform(Matrix.Translation((22.0, 0, 0)))
        o.data.transform(Matrix.Rotation(math.radians(45 + 90 * k), 4, 'Z'))
    bl = G.blades(12.0, 42.0, 10.0, 7, mat('fan_blade', '#252932', 0.0, 0.45), sweep=40)
    G.transform(bl, Matrix.Rotation(math.radians(90), 4, 'X'))       # trục y -> trục z
    G.transform(bl, Matrix.Translation((0, 0, 41.0)))
    finish(out('low_profile_47'), groups={'cooler_fan': bl}, main='part_cooler')


# ---------------------------------------------------------------- Két AIO 360 (mục 7.3)
@part
def aio_360():
    """Két 397 × 120 × 27, gốc giữa mặt trên, treo xuống -y; 3 quạt 120 dưới két thổi +y. Node blades_1…3 quay quanh y."""
    reset()
    frame = mat('rad_frame', '#16181d', 0.2, 0.55)
    fin = mat('rad_fin', '#101216', 0.6, 0.55)
    tube_m = mat('rad_tube', '#2a2d33', 0.6, 0.4)
    steel = mat('steel', '#929aa3', 0.9, 0.3)
    fan_m = mat('fan_frame', '#101216', 0.0, 0.55)
    blade_m = mat('fan_blade', '#101216', 0.0, 0.45)
    hole = mat('hole', '#050506', 0.0, 0.9)
    core, half, W, T = 360.0, 198.5, 120.0, 27.0
    y0, y1 = -T, 0.0
    box((-half, y0, -W / 2), (-core / 2, y1, W / 2), frame, bevel=1.2)
    box((core / 2, y0, -W / 2), (half, y1, W / 2), frame, bevel=1.2)
    for sg in (-1, 1):
        box((-core / 2, y0, min(sg * 50.0, sg * 60.0)), (core / 2, y1, max(sg * 50.0, sg * 60.0)), frame)
    for k in range(12):
        z = -50.0 + (k + 0.5) * (100.0 / 12)
        box((-core / 2, y0 + 0.5, z - 0.9), (core / 2, y1 - 0.5, z + 0.9), tube_m)
    for k in range(int(core / 2.4)):
        x = -core / 2 + 1.2 + k * 2.4
        box((x - 0.18, y0 + 1.0, -50.0), (x + 0.18, y1 - 1.0, 50.0), fin)
    centers = (-120.0, 0.0, 120.0)
    grid = [(c + dx, z) for c in centers for dx in (-52.5, 52.5) for z in (-52.5, 52.5)]
    for x, z in grid:
        cyl((x, y1 + 0.01, z), 1.5, 0.02, 'y', hole, 16)
        tube((x, 3.5, z), 3.5, 1.7, 1.0, 'y', steel, 20)
        G.phillips((x, 5.0, z), 0, 2.8, steel, hole, 'y', h=2.0, sign=1)
    for z in (-15.0, 15.0):
        cyl((189.0, y0 - 4.0, z), 6.0, 8.0, 'y', steel, 24)
        cyl((189.0, y0 - 10.0, z), 5.6, 6.0, 'y', mat('hose', '#202329', 0.0, 0.8), 24)
    cyl((189.0, y1 + 1.0, 40.0), 5.0, 2.0, 'y', steel, 24)
    spin = {}
    for i, cx in enumerate(centers):
        fy = y0 - 12.5
        parts = G.fan_frame(120.0, 25.0, fan_m, struts_side=1, arrow_face='+z')
        G.transform(parts, Matrix.Translation((cx, fy, 0)))
        bl = G.blades(20.0, 56.0, 21.0, 7, blade_m)
        G.transform(bl, Matrix.Translation((cx, fy - 1.0, 0)))
        spin[f'blades_{i + 1}'] = bl
        for x in (-52.5, 52.5):
            for z in (-52.5, 52.5):
                cyl((cx + x, -38.0, z), 1.5, 30.0, 'y', steel, 12)
                tube((cx + x, -52.5, z), 3.5, 1.7, 1.0, 'y', steel, 20)
                G.phillips((cx + x, -54.0, z), 0, 2.8, steel, hole, 'y', h=2.0, sign=-1)
    finish(out('aio_360'), groups=spin, main='part_aio_radiator')


# ---------------------------------------------------------------- Quạt vỏ theo cỡ (dùng hàm quạt Gaming)
def fan_variant(size, argb):
    """Gọi hàm quạt vỏ của Gaming nhưng xuất ra tên f_fan<cỡ>[_argb].glb trong thư mục họ case."""
    old = G.out
    G.out = lambda name: out(f'fan{int(size)}' + ('_argb' if argb else ''))
    try:
        G._case_fan(argb, size)
    finally:
        G.out = old


@part
def fan120():
    fan_variant(120.0, False)


@part
def fan140():
    fan_variant(140.0, False)


@part
def fan120_argb():
    fan_variant(120.0, True)


# ---------------------------------------------------------------- Đầu cáp riser PCIe (dải mềm dựng trong app)
@part
def riser_ends():
    """Hai đầu cứng của riser: node 'riser_male' (89 × 12 × 1,6, cắm khe bo, gốc ở tâm mép tiếp điểm)
    và node 'riser_slot' (adapter cái 110 × 18 × 14 có khe x16 và chốt, gốc ở tâm đáy). Dải mềm 70 mm do app dựng."""
    reset()
    pcb = mat('riser_pcb', '#16181d', 0.1, 0.7)
    gold = mat('gold', '#c6a565', 1.0, 0.3)
    plastic = mat('plastic', '#101216', 0.0, 0.55)
    shield = mat('riser_shield', '#1b1d22', 0.3, 0.6)
    hole = mat('hole', '#050506', 0.0, 0.9)
    male = [box((-44.5, -0.8, -8.0), (44.5, 0.8, 0.0), pcb), box((-44.5, -1.6, 0.0), (44.5, 1.6, 12.0), shield)]
    pads = [(-44.0 + 0.5 + i * 1.0, -4.2, 0.6, 7.0) for i in range(88) if abs(-44.0 + 0.5 + i * 1.0 - (-44.5 + G.PCIE_KEY)) > 1.2]
    male.append(quads(pads, 0.82, gold, axis='y', name='riser_pads_a'))
    male.append(quads(pads, -0.82, gold, axis='-y', name='riser_pads_b'))
    slot = [box((-55.0, -9.0, 0.0), (55.0, 9.0, 4.0), shield)]
    body = box((-48.0, -4.0, 4.0), (48.0, 4.0, 14.0), plastic)
    boolean(body, G.cutter([((-45.0, -0.8, 6.0), (45.0, 0.8, 15.0))], plastic))
    slot.append(body)
    slot.append(box((48.0, -4.0, 4.0), (54.0, 4.0, 12.0), plastic))
    slot.append(box((49.0, -1.4, 12.0), (55.0, 1.4, 15.0), mat('plastic_grey', '#2a2f37', 0.0, 0.5)))
    slot.append(box((-45.0, -0.75, 4.1), (45.0, 0.75, 6.0), hole))
    for x in (-50.0, 50.0):
        slot.append(cyl((x, 6.5, 2.0), 1.7, 4.2, 'z', mat('steel', '#929aa3', 0.9, 0.3), 12))
    finish(out('riser_ends'), groups={'riser_slot': slot}, main='riser_male')


# ---------------------------------------------------------------- Đế đứng console (mục 7)
@part
def console_stand():
    """Đế 280 (x) × 18 (y) × 140 (z), gốc giữa bao. Hai ray vát đỡ hai mép thân (khe giữa rộng 100),
    hai cầu nối tại x ±85 có lỗ M4, đệm cao su 1 mm trong lòng ray."""
    reset()
    m = mat('stand', '#343639', 0.4, 0.5)
    rub = mat('rubber', '#202329', 0.0, 0.85)
    for sz in (-1, 1):
        sec = [(-9.0, 50.0), (-9.0, 70.0), (9.0, 70.0), (9.0, 56.0), (3.0, 50.0)]   # (y; z), mép trong vát
        rail = prism([(y, sz * z) for y, z in sec][::(1 if sz > 0 else -1)], -140.0, 140.0, m, plane='yz')
        # hai đầu ray vát xuống như tờ parts/console_stand: mặt trên dài 250, đáy 280
        for e in (-1, 1):
            boolean(rail, prism([(e * 125.0, 10.0), (e * 142.0, 10.0), (e * 142.0, -6.0)][::e], -80.0, 80.0, m, name='cut'))
        box((-120.0, 2.0, min(sz * 51.0, sz * 52.0)), (120.0, 9.0, max(sz * 51.0, sz * 52.0)), rub)
    for x in (-85.0, 85.0):
        box((x - 6.0, -9.0, -50.0), (x + 6.0, -1.0, 50.0), m)          # thanh nối mảnh
        boss = box((x - 9.0, -9.0, -9.0), (x + 9.0, 1.0, 9.0), m, bevel=1.0)   # cục gá vuông có lỗ M4
        boolean(boss, cyl((x, -4.0, 0), 2.2, 14.0, 'y', m, 16))
    for sx in (-1, 1):
        for sz in (-1, 1):
            box((sx * 135.0 - 4.0, -9.4, sz * 64.0 - 4.0), (sx * 135.0 + 4.0, -9.0, sz * 64.0 + 4.0), rub)
    finish(out('console_stand'), main='part_stand')


# ---------------------------------------------------------------- Hub quạt 8 cổng + bộ lấy nguồn ATX24
@part
def fan_hub_8():
    """Hộp 80 (x) × 50 (y) × 18 (z), hai tai tổng 92 theo x, lỗ (±43; 0). 8 cổng PWM ở mép +y, 8 cổng ARGB ở mép -y,
    số 1–8 in trên mặt +z, ba dây vào ở đầu +x. Gốc ở tâm đáy hộp."""
    reset()
    m = mat('hub_body', '#16181d', 0.2, 0.55)
    plastic = mat('plastic', '#101216', 0.0, 0.55)
    gold = mat('gold', '#c6a565', 1.0, 0.3)
    silk = mat('silk', '#d2d7dd', 0.0, 0.7)
    box((-40.0, -25.0, 0.0), (40.0, 25.0, 18.0), m, bevel=1.0)
    for sx in (-1, 1):
        t = box((min(sx * 40.0, sx * 46.0), -6.0, 0.0), (max(sx * 40.0, sx * 46.0), 6.0, 2.0), m)
        boolean(t, cyl((sx * 43.0, 0, 1.0), 1.7, 4.0, 'z', m, 12))
    rubber = mat('rubber', '#17191c', 0.0, 0.8)
    # 8 cổng PWM ở mép +y, 8 cổng ARGB ở mép -y (tờ parts/fan_hub_8): vỏ 9 × 6, chân hướng ra ngoài
    for i in range(8):
        x = -31.5 + i * 9.0
        for e, keys in ((1, (0, 1, 2, 3)), (-1, (0, 1, 3))):
            y0, y1 = sorted((e * 25.0, e * 31.0))
            sh = box((x - 4.5, y0, 6.0), (x + 4.5, y1, 12.0), plastic)
            boolean(sh, G.cutter([((x - 3.7, min(e * 26.0, e * 32.0), 6.8), (x + 3.7, max(e * 26.0, e * 32.0), 11.2))], plastic))
            for k in keys:
                px = x - 2.7 + k * 1.8
                box((px - 0.3, min(e * 25.0, e * 30.0), 8.7), (px + 0.3, max(e * 25.0, e * 30.0), 9.3), gold)
            label(str(i + 1), x, e * 21.0, 18.02, 3.0, silk)
    label('PWM', -36.0, 13.0, 18.02, 2.0, silk, align='LEFT')
    label('ARGB', -36.0, -13.0, 18.02, 2.0, silk, align='LEFT')
    # đầu +x: ba ống dây ra (PWM điều khiển y +14, nguồn từ adapter ATX24 y 0, ARGB điều khiển y -14)
    for y in (14.0, 0.0, -14.0):
        cyl((43.0, y, 9.0), 3.5, 6.0, 'x', rubber, 20)
        cyl((52.0, y, 9.0), 2.6, 12.0, 'x', rubber, 16)
    finish(out('fan_hub_8'), main='part_hub')


# ================================================================ VỎ CASE
FAMILIES = json.load(open(os.path.join(os.path.dirname(__file__), '..', 'src', 'data', 'families.json')))
ATX_HOLES = [(x, y) for y in (142.5, 0.0, -142.5) for x in (-112.0, 0.0, 112.0)]


def board_holes(ff):
    return ATX_HOLES if ff == 'ATX' else [tuple(h) for h in BOARDS[ff]['holes']]


def case_mats():
    return dict(
        steel=mat('case_steel', '#24262a', 0.25, 0.65),
        inner=mat('case_inner', '#1d1f23', 0.2, 0.7),
        mesh=perforated_material('case_mesh', '#101216', 2.4, 3.6),
        filt=mat('dust_filter', '#202329', 0.0, 0.9, alpha=0.45),
        rubber=mat('rubber', '#202329', 0.0, 0.85),
        dark=mat('grommet_skin', '#0e0f12', 0.0, 0.9),
        hole=mat('hole', '#050506', 0.0, 0.9),
        screw=mat('screw', '#929aa5', 0.9, 0.3),
        thumb=mat('thumb', '#2a2e35', 0.6, 0.35),
        brass=mat('brass', '#b89451', 0.9, 0.35),
        glass=mat('glass', '#20262b', 0.0, 0.05, alpha=0.32),
        frit=mat('glass_frit', '#101216', 0.0, 0.5),
        port=mat('port_metal', '#c9ced4', 0.9, 0.3),
        usb=mat('usb_tongue', '#1f6fd1', 0.0, 0.5),
        btn=mat('btn', '#2a2e35', 0.7, 0.3),
        pled=mat('pled', '#d2d7dd', 0.0, 0.4, emit='#d2d7dd', strength=0.6),
        cyan=mat('case_accent', '#22d3ee', 0.0, 0.35, emit='#22d3ee', strength=0.8),
        hdd=mat('hdd_led', '#f59e0b', 0.0, 0.3, emit='#f59e0b', strength=1.2),
    )


def io_controls(items, face, plane_v, M, ring=None, horiz=False):
    """Cụm nút/cổng trên một mặt vỏ. face 'x': mặt đứng phía trước, item (kind, y, z); face 'y': mặt nóc, item (kind, x, z).
    plane_v là toạ độ của mặt (x hoặc y). Trả về các vật cắt để khoét tấm vỏ."""
    cuts = []
    for kind, a, b in items:
        if face == 'x':
            c = (plane_v, a, b)
            ax = 'x'
        else:
            c = (a, plane_v, b)
            ax = 'y'
        def at(d):   # điểm lùi vào trong vỏ d mm
            return (c[0] - d, c[1], c[2]) if ax == 'x' else (c[0], c[1] - d, c[2])
        if kind == 'power':
            cuts.append(cyl(at(1.5), 7.2, 5.0, ax, M['hole'], 32))
            cyl(at(-0.6), 7.0, 1.6, ax, M['btn'], 40)
            tube(at(-0.3), 8.1, 7.2, 1.0, ax, ring or M['pled'], 48)
        elif kind == 'reset':
            cuts.append(cyl(at(1.5), 2.2, 5.0, ax, M['hole'], 16))
            cyl(at(0.4), 2.0, 0.6, ax, M['btn'], 16)
        elif kind in ('jack', 'mic'):
            cuts.append(cyl(at(1.5), 3.6, 5.0, ax, M['hole'], 20))
            tube(at(0.3), 3.5, 1.75, 0.6, ax, M['screw'], 20)
            cyl(at(0.8), 1.75, 0.2, ax, M['hole'], 16)
        elif kind == 'hdd':
            cyl(at(-0.1), 0.75, 0.2, ax, M['hdd'], 12)
        elif kind in ('usb', 'usbc'):
            w, h = (13.0, 6.0) if kind == 'usb' else (8.4, 2.6)
            # cạnh dài theo trục còn lại của mặt (y nếu mặt trước, z nếu mặt nóc)
            if ax == 'x' and horiz:   # cổng nằm ngang trên mặt trước hẹp (mini, slim)
                lo, hi = (c[0] - 8, c[1] - h / 2, c[2] - w / 2), (c[0] + 1, c[1] + h / 2, c[2] + w / 2)
            elif ax == 'x':
                lo, hi = (c[0] - 8, c[1] - w / 2, c[2] - h / 2), (c[0] + 1, c[1] + w / 2, c[2] + h / 2)
            else:
                lo, hi = (c[0] - h / 2, c[1] - 8, c[2] - w / 2), (c[0] + h / 2, c[1] + 1, c[2] + w / 2)
            cuts.append(box(lo, hi, M['hole'], name='cut'))
            shell_lo = tuple(l - (0.6 if i != (0 if ax == 'x' else 1) else 0) for i, l in enumerate(lo))
            shell_hi = tuple(h_ + (0.6 if i != (0 if ax == 'x' else 1) else -1.2) for i, h_ in enumerate(hi))
            p = box(shell_lo, shell_hi, M['port'])
            boolean(p, box(lo, tuple(h_ + (0 if i != (0 if ax == 'x' else 1) else 1.0) for i, h_ in enumerate(hi)), M['port'], name='cut'))
            if kind == 'usb':
                if ax == 'x' and horiz:
                    box((c[0] - 6.5, c[1] + 0.2, c[2] - 5.5), (c[0] - 0.4, c[1] + 1.8, c[2] + 5.5), M['usb'])
                elif ax == 'x':
                    box((c[0] - 6.5, c[1] - 5.5, c[2] + 0.2), (c[0] - 0.4, c[1] + 5.5, c[2] + 1.8), M['usb'])
                else:
                    box((c[0] + 0.2, c[1] - 6.5, c[2] - 5.5), (c[0] + 1.8, c[1] - 0.4, c[2] + 5.5), M['usb'])
    return cuts


def tower_case(cid):
    """Vỏ tháp (01 standard, 02 compact, 06 full) dựng theo families.json. Gốc ở tâm thân, trục app (mm).
    Node: part_case (khung cố định), panel_open (hông +Z: kính hoặc thép), panel_closed (hông -Z)."""
    C = FAMILIES[cid]
    reset()
    M = case_mats()
    D2, H2, W2 = C['body'][2] / 2, C['body'][1] / 2, C['body'][0] / 2
    T = 1.2
    steel, inner = M['steel'], M['inner']
    xr, xf, yb, yt, zb, zg = -D2, D2, -H2, H2, -W2, W2
    B = C['board']['origin']
    ff = C['board']['ff']

    # ---- Đáy: cửa hút PSU + lọc kéo, chân
    floor = box((xr, yb, zb), (xf, yb + T, zg), steel)
    px, pz, ps = C['psu_intake']
    boolean(floor, G.cutter([((px - ps / 2, yb - 1, pz - ps / 2), (px + ps / 2, yb + T + 1, pz + ps / 2))], steel))
    box((px - ps / 2 - 4, yb - 1.5, pz - ps / 2 - 4), (px + ps / 2 + 4, yb - 0.3, pz + ps / 2 + 4), M['filt'])
    if C.get('floor_vent'):
        x0, x1, z0, z1 = C['floor_vent']
        boolean(floor, G.cutter([((x0, yb - 1, z0), (x1, yb + T + 1, z1))], steel))
        perforated_plate(rounded_rect(x1 - x0, z1 - z0, 4.0, 4, cx=(x0 + x1) / 2, cy=(z0 + z1) / 2), yb - 0.8, yb, M['mesh'], 'floor_mesh')
    for f in C['feet']:
        if f['shape'] == 'round':
            cyl((f['at'][0], f['at'][1], f['at'][2]), f['size'][0] / 2, f['size'][1], 'y', M['rubber'], 32)
        else:
            cbox(tuple(f['at']), tuple(f['size']), M['rubber'], bevel=2.0)

    # ---- Nóc: cửa thoáng lưới + lọc nam châm (+ tai gá két nếu có)
    roof = box((xr, yt - T, zb), (xf, yt, zg), steel)
    vx0, vx1, vz0, vz1 = C['top_vent']
    vent = rounded_rect(vx1 - vx0, vz1 - vz0, 6.0, 4, cx=(vx0 + vx1) / 2, cy=(vz0 + vz1) / 2)
    boolean(roof, prism(vent, yt - T - 1, yt + 1, steel, plane='xz'))
    perforated_plate(vent, yt - T, yt - T + 0.8, M['mesh'], 'top_mesh')
    fr = prism(rounded_rect(vx1 - vx0 + 8, vz1 - vz0 + 8, 8.0, 4, cx=(vx0 + vx1) / 2, cy=(vz0 + vz1) / 2), yt, yt + 1.2, M['rubber'], plane='xz', name='filter_frame')
    boolean(fr, prism(vent, yt - 1, yt + 2, M['rubber'], plane='xz'))
    if C.get('rad_mount'):
        rx, rz, n = C['rad_mount']
        for i in range(n):
            cx = rx + (i - (n - 1) / 2) * 120.0
            for dx in (-52.5, 52.5):
                for dz in (-52.5, 52.5):
                    tube((cx + dx, yt - 1.5, rz + dz), 5.0, 1.7, 3.0, 'y', steel, 24)
        box((vx0, yt - T - 3.0, rz - 62.0), (vx1, yt - T, rz - 56.0), steel)
        box((vx0, yt - T - 3.0, rz + 56.0), (vx1, yt - T, rz + 62.0), steel)

    # ---- Mặt trước
    fr_ = C['front']
    cx0, cx1 = fr_['x']
    if fr_['style'] == 'mesh':
        y0, y1, z0, z1 = fr_['cassette']
        cas = box((cx0, y0, z0), (cx1, y1, z1), steel, bevel=0.8)
        oy0, oy1, oz0, oz1 = fr_['opening']
        boolean(cas, G.cutter([((cx0 - 1, oy0, oz0), (cx1 + 1, oy1, oz1))], steel))
        mx = fr_['mesh_x']
        perforated_plate([(oy0 - 2, oz0 - 2), (oy1 + 2, oz0 - 2), (oy1 + 2, oz1 + 2), (oy0 - 2, oz1 + 2)], mx, mx + 0.8, M['mesh'], 'front_mesh', plane='yz')
        box((fr_['filter_x'], oy0, oz0), (fr_['filter_x'] + 0.5, oy1, oz1), M['filt'])
        for y, z in fr_['screws']:
            G.phillips((cx1 - 2.0, y, z), 0, 2.4, M['screw'], M['hole'], 'x', h=1.0)
        # khung trước phía ngoài cassette
        box((cx0, yb, zb), (xf, yt, zb + (z0 - zb)), steel)
        strip = box((cx0, yb, z1), (xf, yt, zg), steel)     # dải I/O phía +Z ngoài cassette
        box((cx0, yb, z0), (xf, y0, z1), steel)
        box((cx0, y1, z0), (xf, yt, z1), steel)
    else:   # mặt đặc có 4 cột × 7 hàng khe ngang ở thấp và hai khe hút cạnh
        strip = None
        plate = box((cx0, yb + T, zb), (xf, yt - T, zg), steel, bevel=1.0)
        slots = []
        for zc in fr_['slot_cols']:
            for j in range(fr_['slot_rows']):
                y = fr_['slot_y0'] + 14.0 * j
                slots.append(G.cutter([((cx0 - 1, y - 2.5, zc - 15.0), (xf + 1, y + 2.5, zc + 15.0))], steel))
        for zs in fr_['side_slits']:
            slots.append(G.cutter([((cx0 - 1, fr_['slit_y'][0], zs - 3.0), (xf + 1, fr_['slit_y'][1], zs + 3.0))], steel))
        boolean(plate, join(slots, 'front_slots'))
        box((fr_['filter_x'], yb + 8, zb + 6), (fr_['filter_x'] + 0.5, yt - 8, zg - 6), M['filt'])
    # giá quạt trước: tấm có lỗ tròn + lỗ vít
    if C.get('front_fans'):
        bx = C['front_bracket_x']
        fans = C['front_fans']
        ys = [f[1] for f in fans]
        szm = max(f[3] for f in fans)
        br = box((bx, min(ys) - szm / 2 - 6, -szm / 2 - 6 + fans[0][2]), (bx + 1.2, max(ys) + szm / 2 + 6, szm / 2 + 6 + fans[0][2]), steel)
        cuts = []
        for f in fans:
            hp = 52.5 if f[3] <= 120 else 62.25
            cuts.append(cyl((bx + 0.6, f[1], f[2]), f[3] / 2 - 3.0, 4.0, 'x', steel, 64))
            cuts += [cyl((bx + 0.6, f[1] + dy, f[2] + dz), 2.25, 4.0, 'x', steel, 12) for dy in (-hp, hp) for dz in (-hp, hp)]
        boolean(br, join(cuts, 'fan_bracket_cut'))

    # ---- Cụm I/O vỏ
    io = C['io']
    io_cuts = io_controls([tuple(i) for i in io['items']], io['face'], xf if io['face'] == 'x' else yt, M)
    target = roof if io['face'] == 'y' else strip
    if target is not None:
        for c in io_cuts:
            boolean(target, c)
    else:
        for c in io_cuts:
            bpy.data.objects.remove(c, do_unlink=True)

    # ---- Lưng (design/rear/standard_tower.md): khung gấp, hốc I/O, ô quạt lưới, dãy PCIe có lưới + thanh giữ, khe thoáng, gờ gá PSU
    rear_panel(C, xr, yb, yt, zb, zg, T, M)

    # ---- Khay bo: tấm trên + phần dưới, cửa CPU, lỗ luồn dây, chân đế
    tz = C['tray']['z']
    plates = [box((a[0], a[1], tz - 1.0), (a[2], a[3], tz), inner) for a in C['tray']['rects']]
    cw = C['tray']['cpu_window']
    win = [((cw[0] - cw[2] / 2, cw[1] - cw[2] / 2, tz - 2), (cw[0] + cw[2] / 2, cw[1] + cw[2] / 2, tz + 1))]
    gr = C['grommets']
    holes_g = [((g[0] - g[2] / 2, g[1] - g[3] / 2, tz - 2), (g[0] + g[2] / 2, g[1] + g[3] / 2, tz + 1)) for g in gr.values()]
    for pl in plates:
        xs = [v.co.x for v in pl.data.vertices]
        ys = [v.co.y for v in pl.data.vertices]
        mine = [h for h in win + holes_g if min(xs) < h[0][0] and h[1][0] < max(xs) and min(ys) < h[0][1] and h[1][1] < max(ys)]
        if mine:
            boolean(pl, G.cutter(mine, inner))
    for name, g in gr.items():
        G.grommet(g[0], g[1], g[2], g[3], tz - 0.5, M['rubber'], M['dark'])
    for hx, hy in board_holes(ff):
        x, y = B[0] + hx, B[1] + hy
        cyl((x, y, (tz + B[2]) / 2), 2.75, B[2] - tz, 'z', M['brass'], 6)
        cyl((x, y, B[2] + 0.01), 1.2, 0.02, 'z', M['hole'], 12)

    # ---- Nắp che PSU
    S = C['shroud']
    box((S['x'][0], S['top_y'] - 1.0, tz), (S['x'][1], S['top_y'], S['z1']), steel)
    side = box((S['x'][0], yb + T, S['z1'] - 1.0), (S['x'][1], S['top_y'], S['z1']), steel)
    boolean(side, G.cutter([((S['x'][1] - 90 + k * 11.0, yb + 20.0, S['z1'] - 2.0), (S['x'][1] - 84 + k * 11.0, S['top_y'] - 12.0, S['z1'] + 1.0)) for k in range(7)], steel))
    for x in S['screws_x']:
        G.phillips((x, S['top_y'] + 0.5, S['z1'] - 11.0), 0, 2.4, M['screw'], M['hole'], 'y', h=1.0)

    # ---- Khung đứng quanh hai hông
    for zz0, zz1 in ((zg - 4.0, zg), (zb, zb + 4.0)):
        box((xr, yb + T, zz0), (xr + 6.0, yt - T, zz1), steel)
        box((xr, yb + T, zz0), (xf, yb + T + 6.0, zz1), steel)
        box((xr, yt - T - 6.0, zz0), (xf, yt - T, zz1), steel)

    # ---- Hai hông (node riêng)
    so = C['side_open']
    x0, x1, y0, y1 = so['rect']
    open_parts = []
    if so['type'] == 'glass':
        open_parts.append(box((x0, y0, zg - 4.0), (x1, y1, zg), M['glass']))
        for lo, hi in (((x0, y0, zg - 4.05), (x1, y0 + 10, zg - 4.0)), ((x0, y1 - 10, zg - 4.05), (x1, y1, zg - 4.0)),
                       ((x0, y0, zg - 4.05), (x0 + 10, y1, zg - 4.0)), ((x1 - 10, y0, zg - 4.05), (x1, y1, zg - 4.0))):
            open_parts.append(box(lo, hi, M['frit']))
    else:
        open_parts.append(box((x0, y0, zg - 1.0), (x1, y1, zg), steel))
    for sx, sy in so['screws']:
        open_parts.append(cyl((sx, sy, zg + 2.5), 5.0, 3.0, 'z', M['thumb'], 24))
        open_parts.append(cyl((sx, sy, zg - 0.5), 4.0, 1.0, 'z', M['rubber'], 20))
    sc = C['side_closed']
    x0, x1, y0, y1 = sc['rect']
    closed_parts = [box((x0, y0, zb), (x1, y1, zb + 1.0), steel)]
    for sx, sy in sc['screws']:
        closed_parts.append(cyl((sx, sy, zb - 2.0), 4.5, 4.0, 'z', M['thumb'], 24))
    finish(out(f'case_{cid}'), groups={'panel_open': open_parts, 'panel_closed': closed_parts}, main='part_case')


def ring_wall(w_out, h_out, r, t, cy, cz, x0, x1, m, name='step_wall'):
    """Thành bậc dập quanh một ô chữ nhật bo góc trên mặt lưng: vành dày t, đùn theo x từ x0 tới x1."""
    o = prism(rounded_rect(w_out, h_out, r, 4, cx=cy, cy=cz), x0, x1, m, plane='yz', name=name)
    boolean(o, prism(rounded_rect(w_out - 2 * t, h_out - 2 * t, max(r - t, 0.5), 4, cx=cy, cy=cz), x0 - 1, x1 + 1, m, plane='yz', name='cut'))
    return o


def rear_panel(C, xr, yb, yt, zb, zg, T, M):
    """Mặt lưng vỏ tháp theo design/rear/standard_tower.md, dùng chung cho standard / compact / full.
    Tấm ngoài ở x = xr; các ô lắp linh kiện dập lùi vào trong 3 mm (mặt gá quạt, PCIe, PSU) để mọi thứ nhìn
    từ sau thẳng hàng với linh kiện bên trong: I/O đúng trên cụm cổng bo, ô quạt đúng quạt sau,
    nắp khe đúng hàng PCIe, ô nguồn đúng mặt C14 / công tắc của nguồn."""
    steel, mesh = M['steel'], M['mesh']
    B = C['board']['origin']
    STEP = 3.0
    xm = xr + STEP                              # mặt gá lùi vào trong
    back = box((xr, yb + T, zb + T), (xr + T, yt - T, zg - T), steel)
    cuts = []
    # 01 viền gấp quay vào trong 8 mm quanh mép tấm (thấy được khi tháo hông)
    for lo, hi in (((xr, yb + T, zb + T), (xr + 8, yb + T + T, zg - T)), ((xr, yt - 2 * T, zb + T), (xr + 8, yt - T, zg - T))):
        box(lo, hi, steel, name='rim')
    # 02 hốc I/O: ô dập 172 × 49 lùi 0,6, cửa lá chắn 159 × 37 đúng cụm cổng bo
    io_y0, io_y1 = C['board']['io_y']
    iy, iz = B[1] + (io_y0 + io_y1) / 2, B[2] + 1.6 + 18.5
    ih, iw = io_y1 - io_y0 + 1.0, 37.0
    cuts.append(prism(rounded_rect(ih + 13, iw + 12, 3.0, 4, cx=iy, cy=iz), xr - 1, xr + T + 1, steel, plane='yz', name='cut'))
    io_pl = prism(rounded_rect(ih + 13, iw + 12, 3.0, 4, cx=iy, cy=iz), xr + 0.6, xr + 0.6 + T, steel, plane='yz', name='io_recess')
    boolean(io_pl, G.cutter([((xr - 1, iy - ih / 2, iz - iw / 2), (xr + 4, iy + ih / 2, iz + iw / 2))], steel))
    ring_wall(ih + 13, iw + 12, 3.0, T, iy, iz, xr, xr + 0.6 + T, steel)
    # 03 ô quạt sau: vùng dập 128 vuông lùi 3, cửa vuông R8 bằng (cỡ quạt - 4), lưới lục giác, 4 vít đúng lỗ quạt
    ry, rz, rs = C['rear_fan']
    fw = rs + 8.0
    cuts.append(prism(rounded_rect(fw, fw, 6.0, 4, cx=ry, cy=rz), xr - 1, xr + T + 1, steel, plane='yz', name='cut'))
    ring_wall(fw, fw, 6.0, T, ry, rz, xr, xm + T, steel)
    fan_pl = prism(rounded_rect(fw, fw, 6.0, 4, cx=ry, cy=rz), xm, xm + T, steel, plane='yz', name='fan_field')
    op = rs - 4.0
    boolean(fan_pl, prism(rounded_rect(op, op, 8.0, 4, cx=ry, cy=rz), xm - 1, xm + T + 1, steel, plane='yz', name='cut'))
    perforated_plate(rounded_rect(op + 2, op + 2, 8.0, 4, cx=ry, cy=rz), xr + 1.0, xr + 1.8, mesh, 'fan_mesh', plane='yz')
    hp = 52.5 if rs <= 120 else 62.25
    for dy in (-hp, hp):
        for dz in (-hp, hp):
            G.phillips((xm - 1.0, ry + dy, rz + dz), 0, 2.8, M['screw'], M['hole'], 'x', h=2.0, sign=-1)
    # 04 dãy PCIe: cửa xuyên, nắp còn lại có trường lưới 10 × (rộng - 20), thanh giữ 8 mm với vít từng khe
    pcie = C['pcie']
    pc1 = pcie['top_y']
    pc0 = pc1 - pcie['count'] * 20.32
    pz0, pz1 = pcie['z']
    cuts.append(G.cutter([((xr - 1, pc0 - 2.0, pz0 - 2.0), (xr + T + 1, pc1 + 2.0, pz1 + 2.0))], steel))
    ring_wall(pc1 - pc0 + 8.0, pz1 - pz0 + 8.0, 3.0, T, (pc0 + pc1) / 2, (pz0 + pz1) / 2, xr, xm, steel)
    removed = pcie.get('removed', 2)
    for k in range(removed, pcie['count']):
        y1 = pc1 - k * 20.32
        y0 = y1 - 18.82
        cv = box((xm, y0, pz0), (xm + 1.0, y1, pz1 - 8.0), steel, name='slot_cover')
        cy_, cz_ = (y0 + y1) / 2, (pz0 + pz1 - 8.0) / 2
        fl = (pz1 - 8.0 - pz0) - 20.0
        boolean(cv, prism(rounded_rect(10.0, fl, 2.0, 3, cx=cy_, cy=cz_), xm - 1, xm + 2, steel, plane='yz', name='cut'))
        perforated_plate(rounded_rect(10.6, fl + 0.6, 2.0, 3, cx=cy_, cy=cz_), xm + 0.3, xm + 0.7, mesh, 'cover_mesh', plane='yz')
        G.phillips((xm - 1.0, cy_ - 0.75, pz1 - 4.0), 0, 2.6, M['screw'], M['hole'], 'x', h=2.0, sign=-1)
    for k in range(removed):   # vít giữ lá cổng card đồ họa ở các khe đã tháo nắp
        y1 = pc1 - k * 20.32
        G.phillips((xm - 1.0, y1 - 10.16, pz1 - 4.0), 0, 2.6, M['screw'], M['hole'], 'x', h=2.0, sign=-1)
    box((xm, pc0 - 4.0, pz1 - 8.0), (xm + T, pc1 + 4.0, pz1), steel, name='retention_bar')
    # 05 khe thoáng cạnh dãy PCIe (phía hông kính), lưới, thông khoang card
    vz0 = pz1 + 19.0
    vz1 = min(vz0 + 24.0, zg - 12.0)
    if vz1 - vz0 > 10:
        vh = min(126.0, pc1 - pc0 - 16.0)
        vy = (pc0 + pc1) / 2
        vent = rounded_rect(vh, vz1 - vz0, 3.0, 4, cx=vy, cy=(vz0 + vz1) / 2)
        cuts.append(prism(vent, xr - 1, xr + T + 1, steel, plane='yz', name='cut'))
        perforated_plate(vent, xr + T, xr + T + 0.8, mesh, 'side_vent_mesh', plane='yz')
    # 06 gờ gá PSU dập lùi 3: cửa lộ mặt nguồn, 4 vít bất đối xứng khớp lỗ trên mặt sau nguồn ATX
    P = C['psu']
    pc = P['center']
    psz = P['size']
    ph, pw = psz[1] + 12.0, psz[2] + 12.0
    cuts.append(prism(rounded_rect(ph, pw, 3.0, 4, cx=pc[1], cy=pc[2]), xr - 1, xr + T + 1, steel, plane='yz', name='cut'))
    ring_wall(ph, pw, 3.0, T, pc[1], pc[2], xr, xm, steel)
    psu_pl = prism(rounded_rect(ph, pw, 3.0, 4, cx=pc[1], cy=pc[2]), xm - T, xm, steel, plane='yz', name='psu_frame')
    boolean(psu_pl, G.cutter([((xm - T - 1, pc[1] - psz[1] / 2 + 10, pc[2] - psz[2] / 2 + 10), (xm + 1, pc[1] + psz[1] / 2 - 9, pc[2] + psz[2] / 2 - 13.5))], steel))
    for dy, dz in ((-37.0, -69.0), (-37.0, 69.0), (37.0, -69.0), (27.0, 63.0)):
        G.phillips((xm - T - 1.0, pc[1] + dy, pc[2] + dz), 0, 2.6, M['screw'], M['hole'], 'x', h=2.0, sign=-1)
    for c in cuts:
        boolean(back, c)
    assert len(back.data.polygons) > 20, 'tấm lưng bị khoét hỏng'
    return back


def tray_with_holes(C, x0, x1, y0, y1, M, ff, B):
    """Khay bo tại C['tray']['z'] (mặt trước), dày 1,2, khoét cửa CPU + lỗ luồn dây, chân đế bo."""
    tz = C['tray']['z']
    pl = box((x0, y0, tz - 1.2), (x1, y1, tz), M['inner'])
    cw = C['tray']['cpu_window']
    cuts = [((cw[0] - cw[2] / 2, cw[1] - cw[2] / 2, tz - 3), (cw[0] + cw[2] / 2, cw[1] + cw[2] / 2, tz + 1))]
    for g in C['grommets'].values():
        cuts.append(((g[0] - g[2] / 2, g[1] - g[3] / 2, tz - 3), (g[0] + g[2] / 2, g[1] + g[3] / 2, tz + 1)))
    boolean(pl, G.cutter(cuts, M['inner']))
    for g in C['grommets'].values():
        G.grommet(g[0], g[1], g[2], g[3], tz - 0.6, M['rubber'], M['dark'])
    for hx, hy in board_holes(ff):
        x, y = B[0] + hx, B[1] + hy
        cyl((x, y, (tz + B[2]) / 2), 2.75, B[2] - tz, 'z', M['brass'], 6)
    return pl


def vertical_pcie(xr, T, C, M):
    """Cassette khe PCIe dọc ở lưng (riser): khe dài theo y, bước 20,32 theo z. Nắp đã tháo cho GPU bỏ qua."""
    P = C['pcie']
    y0, y1 = P['y']
    z_top = P['z_start']
    step = P['dir'] * 20.32
    for k in range(P['count']):
        za, zb_ = z_top + k * step, z_top + (k + 1) * step
        lo, hi = min(za, zb_) + 0.75, max(za, zb_) - 0.75
        if k < P.get('removed', 2):
            continue
        cv = box((xr + T, y0, lo), (xr + T + 1.0, y1, hi), M['steel'])
        boolean(cv, G.cutter([((xr + T - 1, y0 + 12.0 + j * 14.0, lo + 4.0), (xr + T + 2, y0 + 20.0 + j * 14.0, hi - 4.0)) for j in range(int((y1 - y0 - 30) / 14))], M['steel']))
        G.phillips((xr + T + 2.0, y1 - 1.5, (lo + hi) / 2), 0, 2.6, M['screw'], M['hole'], 'x', h=2.0, sign=1)


def fan_mount(c, size, axis, M, t=1.2, holes=True):
    """Tấm gá quạt: lỗ tròn + 4 lỗ vít, pháp tuyến theo `axis`."""
    hp = 52.5 if size <= 120 else 62.25
    s = size / 2 + 6
    lo = [c[i] - (t / 2 if 'xyz'[i] == axis else s) for i in range(3)]
    hi = [c[i] + (t / 2 if 'xyz'[i] == axis else s) for i in range(3)]
    pl = box(tuple(lo), tuple(hi), M['steel'])
    cuts = [cyl(c, size / 2 - 3.0, t + 2, axis, M['steel'], 64)]
    ia, ib = [i for i in range(3) if 'xyz'[i] != axis]
    for da in (-hp, hp):
        for db in (-hp, hp):
            p = list(c)
            p[ia] += da
            p[ib] += db
            cuts.append(cyl(tuple(p), 2.25, t + 2, axis, M['steel'], 12))
    boolean(pl, join(cuts, 'fan_mount_cut'))
    return pl


def inlet_c14(c, M):
    """Ổ AC C14 rời trên lưng vỏ (SFF / slim), mặt hướng -x."""
    x, y, z = c
    box((x - 0.5, y - 12.5, z - 15.0), (x + 12.0, y + 12.5, z + 15.0), M['btn'])
    box((x - 0.6, y - 9.0, z - 11.5), (x + 6.0, y + 9.0, z + 11.5), M['hole'])
    for dy, dz in ((5.0, 0.0), (-4.0, -7.0), (-4.0, 7.0)):
        box((x + 1.0, y + dy - 2.0, z + dz - 0.4), (x + 6.0, y + dy + 2.0, z + dz + 0.4), M['port'])


@part
def case_showcase_dual_chamber():
    """Showcase hai khoang 300 × 450 × 450: kính hông +Z và kính trước gặp nhau ở góc, khoang sau chứa PSU/dây,
    ống gió cấp cho 2 quạt bên, 3 quạt đáy, két 240 trên nóc, GPU dựng dọc qua riser."""
    C = FAMILIES['showcase_dual_chamber']
    reset()
    M = case_mats()
    steel = M['steel']
    D2, H2, W2 = 225.0, 225.0, 150.0
    T = 1.2
    xr, xf, yb, yt, zb, zg = -D2, D2, -H2, H2, -W2, W2
    B = C['board']['origin']
    # đáy + lưới 3 quạt + chân vát
    floor = box((xr, yb, zb), (xf, yb + T, zg), steel)
    boolean(floor, G.cutter([((-197, yb - 1, -1), (187, yb + T + 1, 129))], steel))
    perforated_plate(rounded_rect(384, 130, 4, 4, cx=-5, cy=64), yb - 0.8, yb, M['mesh'], 'floor_mesh')
    for f in C['feet']:
        cbox(tuple(f['at']), tuple(f['size']), M['rubber'], bevel=3.0)
    box((-205, yb + T, -6), (195, yb + T + 3, -3), steel)        # ray gá quạt đáy
    box((-205, yb + T, 129), (195, yb + T + 3, 132), steel)
    # nóc: lưới + ray két 240
    roof = box((xr, yt - T, zb), (xf, yt, zg), steel)
    vent = rounded_rect(370, 141, 6, 4, cx=-5, cy=62.5)
    boolean(roof, prism(vent, yt - T - 1, yt + 1, steel, plane='xz'))
    perforated_plate(vent, yt - T, yt - T + 0.8, M['mesh'], 'top_mesh')
    rx, rz = C['radiator']['origin'][0], C['radiator']['origin'][2]
    for cx in (rx - 60, rx + 60):
        for dx in (-52.5, 52.5):
            for dz in (-52.5, 52.5):
                tube((cx + dx, yt - 1.5, rz + dz), 5.0, 1.7, 3.0, 'y', steel, 24)
    box((-190, yt - T - 3, rz - 62), (180, yt - T, rz - 56), steel)
    box((-190, yt - T - 3, rz + 56), (180, yt - T, rz + 62), steel)
    # I/O nóc trên khoang sau
    io_cuts = io_controls([tuple(i) for i in C['io']['items']], 'y', yt, M, ring=M['cyan'])
    for c in io_cuts:
        boolean(roof, c)
    # mặt trước: kính phía khoang trưng bày + dải thép che khoang sau
    box((221, -211, -24), (225, 211, 145), M['glass'])
    box((221, -211, -24), (221.05, 211, -14), M['frit'])
    box((221, yb + T, zb), (xf, yt - T, -24), steel)
    box((221, yb + T, -24), (xf, -211, zg), steel)
    box((221, 211, -24), (xf, yt - T, zg), steel)
    # lưng (design/rear/showcase_dual_chamber.md): hốc I/O, ô quạt tròn có lưới, cassette 3 khe dọc + thanh giữ,
    # gờ gá nguồn đứng 150 × 86 lộ C14 / công tắc, lưới thoát khoang phụ và lưới cạnh card
    back = box((xr, yb + T, zb + T), (xr + T, yt - T, zg - T), steel)
    io_y0, io_y1 = C['board']['io_y']
    ry, rz2, rs = C['rear_fan']
    P = C['pcie']
    pz = sorted([P['z_start'], P['z_start'] + P['dir'] * 20.32 * P['count']])
    pc = C['psu']['center']
    mesh = M['mesh']
    xm = xr + 3.0
    for lo, hi in (((xr, yb + T, zb + T), (xr + 12, yb + 2 * T, zg - T)), ((xr, yt - 2 * T, zb + T), (xr + 12, yt - T, zg - T))):
        box(lo, hi, steel, name='rim')
    cuts = []
    # I/O: hốc 172 × 49 lùi 0,6 quanh cửa lá chắn 159 × 37
    iy, iz = B[1] + (io_y0 + io_y1) / 2, B[2] + 1.6 + 18.5
    ih, iw = io_y1 - io_y0 + 1.0, 37.0
    cuts.append(prism(rounded_rect(ih + 13, iw + 12, 3.0, 4, cx=iy, cy=iz), xr - 1, xr + T + 1, steel, plane='yz', name='cut'))
    io_pl = prism(rounded_rect(ih + 13, iw + 12, 3.0, 4, cx=iy, cy=iz), xr + 0.6, xr + 0.6 + T, steel, plane='yz', name='io_recess')
    boolean(io_pl, G.cutter([((xr - 1, iy - ih / 2, iz - iw / 2), (xr + 4, iy + ih / 2, iz + iw / 2))], steel))
    ring_wall(ih + 13, iw + 12, 3.0, T, iy, iz, xr, xr + 0.6 + T, steel)
    # quạt sau: trường gá 126 × 132 lùi 3, cửa tròn Ø116 có lưới, 4 vít bước 105
    cuts.append(prism(rounded_rect(126.0, 132.0, 3.0, 4, cx=ry, cy=rz2), xr - 1, xr + T + 1, steel, plane='yz', name='cut'))
    ring_wall(126.0, 132.0, 3.0, T, ry, rz2, xr, xm + T, steel)
    fan_pl = prism(rounded_rect(126.0, 132.0, 3.0, 4, cx=ry, cy=rz2), xm, xm + T, steel, plane='yz', name='fan_field')
    boolean(fan_pl, cyl((xm + T / 2, ry, rz2), 58.0, T + 2, 'x', steel, 64, name='cut'))
    disc_pts = [(ry + 59.0 * math.cos(2 * math.pi * k / 64), rz2 + 59.0 * math.sin(2 * math.pi * k / 64)) for k in range(64)]
    perforated_plate(disc_pts, xr + 1.8, xr + 2.6, mesh, 'fan_mesh', plane='yz')
    for dy in (-52.5, 52.5):
        for dz in (-52.5, 52.5):
            G.phillips((xm - 1.0, ry + dy, rz2 + dz), 0, 2.8, M['screw'], M['hole'], 'x', h=2.0, sign=-1)
    # cassette 3 khe dọc: cửa thông 110 × 56,96 trong hốc lùi 3, thanh giữ 8 × 73 phía trên
    py0, py1 = P['y']
    cuts.append(G.cutter([((xr - 1, py0 - 2.0, pz[0] - 2.0), (xr + T + 1, py1 + 2.0, pz[1] + 2.0))], steel))
    ring_wall(py1 - py0 + 8.0, pz[1] - pz[0] + 8.0, 3.0, T, (py0 + py1) / 2, (pz[0] + pz[1]) / 2, xr, xm, steel)
    bar = box((xm - T, py1 + 2.0, pz[0] - 6.0), (xm, py1 + 10.0, pz[1] + 6.0), steel, name='retention_bar')
    for k in range(P['count']):
        zc = P['z_start'] + (k + 0.5) * P['dir'] * 20.32
        G.phillips((xm - T - 1.0, py1 + 6.0, zc), 0, 2.6, M['screw'], M['hole'], 'x', h=2.0, sign=-1)
    # nguồn đứng: gờ gá 162 × 98 lùi 3, cửa 150 × 86 lộ trọn mặt sau nguồn, 4 vít ở các tai
    ph, pw = 162.0, 98.0
    cuts.append(prism(rounded_rect(ph, pw, 2.0, 4, cx=pc[1], cy=pc[2]), xr - 1, xr + T + 1, steel, plane='yz', name='cut'))
    ring_wall(ph, pw, 2.0, T, pc[1], pc[2], xr, xm, steel)
    pf = prism(rounded_rect(ph, pw, 2.0, 4, cx=pc[1], cy=pc[2]), xm - T, xm, steel, plane='yz', name='psu_frame')
    boolean(pf, G.cutter([((xm - T - 1, pc[1] - 75.0, pc[2] - 43.0), (xm + 1, pc[1] + 75.0, pc[2] + 43.0))], steel))
    for y, z in ((-26.0, -131.0), (-164.0, -131.0), (-26.0, -57.0), (-158.0, -67.0)):
        G.phillips((xm - T - 1.0, y, z), 0, 2.6, M['screw'], M['hole'], 'x', h=2.0, sign=-1)
    # lưới thoát khoang phụ (phía trên nguồn) và lưới cạnh card
    for (yc, zc, hh, ww) in ((100.0, -94.0, 130.0, 60.0), (-60.0, 15.0, 100.0, 26.0)):
        outline = rounded_rect(hh, ww, 3.0, 4, cx=yc, cy=zc)
        cuts.append(prism(outline, xr - 1, xr + T + 1, steel, plane='yz', name='cut'))
        perforated_plate(outline, xr + T, xr + T + 0.8, mesh, 'rear_vent_mesh', plane='yz')
    for c in cuts:
        boolean(back, c)
    assert len(back.data.polygons) > 20
    vertical_pcie(xm - T, T, C, M)
    # vách phân khoang (khay bo) + ống gió quạt bên
    tray_with_holes(C, -221, 74, -220, 220, M, 'ATX', B)
    for x in (78.0, 207.0):
        box((x - 0.6, -112, -148), (x + 0.6, 147, -35), M['inner'])
    for y in (-112.0, 147.0):
        box((78, y - 0.6, -148), (207, y + 0.6, -35), M['inner'])
    for f in C['side_fans']:
        fan_mount((f[0], f[1], -36.0), f[3], 'z', M)
    # giá đỡ GPU dọc
    box((-221, -117, 75), (79, -115, 93), steel)
    # khung hông và cột góc mảnh
    for zz0, zz1 in ((zg - 4.0, zg), (zb, zb + 4.0)):
        box((xr, yb + T, zz0), (xr + 6.0, yt - T, zz1), steel)
        box((xr, yb + T, zz0), (xf, yb + T + 12.0, zz1), steel)
        box((xr, yt - T - 12.0, zz0), (xf, yt - T, zz1), steel)
    box((220, yb + T, 145), (xf, yt - T, zg), steel)
    # hông +Z kính, hông -Z thép có hai cửa lưới (PSU + ống gió)
    open_parts = [box((-215, -211, 146), (220, 211, 150), M['glass'])]
    for lo, hi in (((-215, -211, 145.95), (220, -201, 146)), ((-215, 201, 145.95), (220, 211, 146)),
                   ((-215, -211, 145.95), (-205, 211, 146))):
        open_parts.append(box(lo, hi, M['frit']))
    cl = box((-219, -219, zb), (219, 219, zb + 1.0), steel)
    boolean(cl, G.cutter([((-215, -170, zb - 1), (-85, -20, zb + 2)), ((78, -112, zb - 1), (207, 147, zb + 2))], steel))
    closed_parts = [cl,
                    perforated_plate([(-215, -170), (-85, -170), (-85, -20), (-215, -20)], zb + 1.0, zb + 1.6, M['mesh'], 'psu_mesh', plane='xy'),
                    perforated_plate([(78, -112), (207, -112), (207, 147), (78, 147)], zb + 1.0, zb + 1.6, M['mesh'], 'side_mesh', plane='xy')]
    for sy in (190.0, -190.0):
        closed_parts.append(cyl((-209, sy, zb - 2.0), 4.5, 4.0, 'z', M['thumb'], 24))
    finish(out('case_showcase_dual_chamber'), groups={'panel_open': open_parts, 'panel_closed': closed_parts}, main='part_case')


@part
def case_mini_sff():
    """Mini SFF nhôm 180 × 250 × 350, bố trí sandwich: bo phía +Z, GPU phía -Z qua riser, SFX phía trước."""
    C = FAMILIES['mini_sff']
    reset()
    M = case_mats()
    alu = mat('alu_brushed', '#45474a', 0.8, 0.38)
    M['steel'] = alu
    D2, H2, W2 = 175.0, 125.0, 90.0
    T = 2.0
    xr, xf, yb, yt, zb, zg = -D2, D2, -H2, H2, -W2, W2
    B = C['board']['origin']
    # khung: 4 thanh dọc + 4 thanh ngang (nhôm), đáy, nóc, lưng, mặt trước
    # đáy và nóc nhôm khe dọc theo x (bản vẽ 04/01: vùng khe 277 × 134 đáy, 281 × 140 nóc), lọc mịn phía trong
    def slotted(y0, y1, x0, x1, zw, name):
        pl = box((xr + 2, y0, zb + 2), (xf - 2, y1, zg - 2), alu, name=name)
        n = int((zw - 3) // 6) + 1
        z0 = -(n - 1) * 3.0
        boolean(pl, join([G.cutter([((x0, y0 - 1, z0 + 6 * j - 1.5), (x1, y1 + 1, z0 + 6 * j + 1.5))], alu) for j in range(n)], name + '_slots'))
        return pl
    slotted(yb, yb + T, -142, 135, 134, 'floor')
    box((-142, yb + T, -67), (135, yb + T + 0.3, 67), M['filt'])
    for f in C['feet']:
        cyl(tuple(f['at']), f['size'][0] / 2, f['size'][1], 'y', M['rubber'], 32)
    # khe nóc phủ cả quạt nóc (dời ra phía trước, trên ngăn nguồn, để không cắt ngang khay giữa)
    slotted(yt - T, yt, -148, 160, 140, 'roof')
    for sx in (-1, 1):
        for sz in (-1, 1):
            G.phillips((sx * 157, yt + 0.4, sz * 72), 0, 2.4, M['screw'], M['hole'], 'y', h=0.8)
    fan_mount((C['fans'][0]['at'][0], 120.0, C['fans'][0]['at'][2]), 120, 'y', M)
    for x in (xr, xf - 2):
        for z in (zb, zg - 2):
            box((x, yb, z), (x + 2, yt, z + 2), alu)
    # mặt trước nhôm bo góc, nút nguồn + USB-C thấp phía +Z
    front = prism(rounded_rect(240, 172, 8, 6), 173, 175, alu, plane='yz')
    for c in io_controls([tuple(i) for i in C['io']['items']], 'x', 175, M, horiz=True):
        boolean(front, c)
    # lưng: I/O ITX (+Z), 3 khe PCIe dọc (-Z), ổ AC rời
    back = box((xr, yb + T, zb + 2), (xr + T, yt - T, zg - 2), alu)
    io_y0, io_y1 = C['board']['io_y']
    P = C['pcie']
    pz = sorted([P['z_start'], P['z_start'] + P['dir'] * 20.32 * P['count']])
    ac = C['ac_inlet']
    for c in (G.cutter([((xr - 1, B[1] + io_y0 - 0.5, B[2] + 1.1), (xr + T + 1, B[1] + io_y1 + 0.5, B[2] + 38.1))], alu),
              G.cutter([((xr - 1, P['y'][0] + 2, pz[0] + 2), (xr + T + 1, P['y'][1] - 8, pz[1] - 2))], alu),
              G.cutter([((xr - 1, ac[1] - 9.5, ac[2] - 12), (xr + T + 1, ac[1] + 9.5, ac[2] + 12))], alu)):
        boolean(back, c)
    assert len(back.data.polygons) > 12
    vertical_pcie(xr, T, C, M)
    inlet_c14((xr + 0.2, ac[1], ac[2]), M)
    # khay giữa, giá GPU, giá chữ L của SFX
    tray_with_holes(C, -162, 43, -113, 113, M, 'ITX', B)
    box((-171, -66, -61), (84, -64, -43), alu)
    for lo, hi in (((48.8, -112, 11.5), (50, -100, 75)), ((48.8, 9, 11.5), (50, 15, 75)),        # giá chữ L dạng khung,
                   ((48.8, -100, 11.5), (50, 9, 18)), ((48.8, -100, 70), (50, 9, 75))):          # chừa cửa cho ổ C14
        box(lo, hi, alu)
    box((48.8, -112, 9), (150, -110.8, 75), alu)
    # hai hông nhôm có khe đứng
    def side(z0, z1, name):
        pl = box((-167, -116, z0), (167, 116, z1), alu)
        cuts = [G.cutter([((-144 + 9 * j - 2, -85, z0 - 1), (-144 + 9 * j + 2, 85, z1 + 1))], alu) for j in range(33)]
        boolean(pl, join(cuts, 'slots'))
        parts = [pl, box((-160, -100, (z0 + z1) / 2 - 0.25 - (1 if z0 > 0 else -1)), (160, 100, (z0 + z1) / 2 + 0.25 - (1 if z0 > 0 else -1)), M['filt'])]
        for sx in (-1, 1):
            for sy in (-1, 1):
                parts.append(cyl((sx * 152, sy * 101, z1 + 0.3 if z0 > 0 else z0 - 0.3), 2.6, 0.6, 'z', M['screw'], 16))
        return parts
    open_parts = side(88, 90, 'open')
    closed_parts = side(-90, -88, 'closed')
    finish(out('case_mini_sff'), groups={'panel_open': open_parts, 'panel_closed': closed_parts}, main='part_case')


@part
def case_slim_console():
    """Slim 100 × 360 × 330 đứng trên đế: hai tầng (bo dưới, GPU trên qua riser), SFX phía trước, hông đục lỗ."""
    C = FAMILIES['slim_console']
    reset()
    M = case_mats()
    gr = mat('graphite', '#343639', 0.25, 0.55)
    M['steel'] = gr
    D2, H2, W2 = 165.0, 180.0, 50.0
    T = 1.2
    xr, xf, yb, yt, zb, zg = -D2, D2, -H2, H2, -W2, W2
    B = C['board']['origin']
    floor = box((xr + 3, yb, zb + 3), (xf - 3, yb + T, zg - 3), gr)
    boolean(floor, G.cutter([((-138, yb - 1, -30), (-20, yb + T + 1, 30)), ((20, yb - 1, -30), (138, yb + T + 1, 30))], gr))
    for x0, x1 in ((-138, -20), (20, 138)):
        perforated_plate([(x0, -30), (x1, -30), (x1, 30), (x0, 30)], yb - 0.6, yb, M['mesh'], 'floor_mesh')
    for x in (-85.0, 85.0):
        cyl((x, yb + 0.6, 0), 2.2, 1.4, 'y', M['hole'], 12)
    roof = box((xr + 3, yt - T, zb + 3), (xf - 3, yt, zg - 3), gr)
    boolean(roof, G.cutter([((-140, yt - T - 1, -34), (130, yt + 1, 34))], gr))
    perforated_plate([(-140, -34), (130, -34), (130, 34), (-140, 34)], yt - T, yt - T + 0.8, M['mesh'], 'top_mesh')
    for x in (xr, xf - 3):
        for z in (zb, zg - 3):
            box((x, yb, z), (x + 3, yt, z + 3), gr, bevel=1.0)
    # mặt trước: cassette lưới bo góc R8, cụm I/O thấp ở z = 0
    fr = prism(rounded_rect(348, 90, 8, 6), 153, 165, gr, plane='yz')
    boolean(fr, G.cutter([((152, -130, -40), (166, 168, 40))], gr))
    perforated_plate([(-130, -40), (168, -40), (168, 40), (-130, 40)], 162, 162.8, M['mesh'], 'front_mesh', plane='yz')
    io_strip = box((160, -172, -40), (165, -130, 40), gr)
    for c in io_controls([tuple(i) for i in C['io']['items']], 'x', 165, M, horiz=True):
        boolean(io_strip, c)
    for sy in (-159, 159):
        G.phillips((165.3, sy, 0), 0, 2.2, M['screw'], M['hole'], 'x', h=0.6)
    # lưng: I/O ITX thấp, 3 khe dọc phía trên, ổ AC
    back = box((xr, yb + T, zb + 3), (xr + T, yt - T, zg - 3), gr)
    io_y0, io_y1 = C['board']['io_y']
    P = C['pcie']
    pz = sorted([P['z_start'], P['z_start'] + P['dir'] * 20.32 * P['count']])
    ac = C['ac_inlet']
    for c in (G.cutter([((xr - 1, B[1] + io_y0 - 0.5, B[2] + 1.1), (xr + T + 1, B[1] + io_y1 + 0.5, min(B[2] + 38.1, zg - 4)))], gr),
              G.cutter([((xr - 1, P['y'][0] + 2, max(pz[0] + 2, zb + 4)), (xr + T + 1, P['y'][1] - 8, min(pz[1] - 2, zg - 4)))], gr),
              G.cutter([((xr - 1, ac[1] - 9.5, ac[2] - 12), (xr + T + 1, ac[1] + 9.5, ac[2] + 12))], gr)):
        boolean(back, c)
    assert len(back.data.polygons) > 12
    vertical_pcie(xr, T, C, M)
    inlet_c14((xr + 0.2, ac[1], ac[2]), M)
    tray_with_holes(C, -158, 43, -174, 38, M, 'ITX', B)
    box((-161, 34, -3), (94, 36, 15), gr)                     # giá đỡ GPU
    for lo, hi in (((45.8, -152.5, -31.75), (47, -142, 31.75)), ((45.8, -33, -31.75), (47, -27.5, 31.75)),   # giá chữ L của SFX,
                   ((45.8, -142, -31.75), (47, -33, -24)), ((45.8, -142, 24), (47, -33, 31.75))):           # khung chừa cửa C14
        box(lo, hi, gr)
    # hai hông đục lỗ (node riêng)
    def side(z0, z1):
        pl = box((-157, -170, z0), (157, 170, z1), gr)
        boolean(pl, G.cutter([((-142, -155, z0 - 1), (142, 155, z1 + 1))], gr))
        parts = [pl, perforated_plate([(-142, -155), (142, -155), (142, 155), (-142, 155)], z0, z1, M['mesh'], 'side_mesh', plane='xy')]
        for sy in (150.0, -150.0):
            parts.append(cyl((-151, sy, z1 + 2.0 if z0 > 0 else z0 - 2.0), 4.5, 4.0, 'z', M['thumb'], 24))
        return parts
    finish(out('case_slim_console'), groups={'panel_open': side(49, 50), 'panel_closed': side(-50, -49)}, main='part_case')


@part
def case_standard_tower():
    tower_case('standard_tower')


@part
def case_compact_tower():
    tower_case('compact_tower')


@part
def case_full_tower():
    tower_case('full_tower')


if __name__ == '__main__':
    for name, fn in PARTS.items():
        if not ONLY or name in ONLY:
            fn()
