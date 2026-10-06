"""
Dựng bộ linh kiện cho máy Gaming theo bản vẽ của Codex (design/gaming/*.md).
Chạy: blender -b --python tools/build_gaming.py -- <thư mục ra> [tên linh kiện ...]
Không ghi tên thì dựng tất cả. Đơn vị trong code là mm theo trục app, glib.finish() tự đổi.
"""
import sys, os, math
sys.path.insert(0, os.path.dirname(__file__))
from glib import *  # noqa: F401,F403
from glib import _obj

args = sys.argv[sys.argv.index('--') + 1:]
OUT = args[0]
ONLY = set(args[1:])
PARTS = {}


def part(fn):
    PARTS[fn.__name__] = fn
    return fn


def out(name):
    return os.path.join(OUT, f'g_{name}.glb')


# ---------------------------------------------------------------- 03 CPU
@part
def cpu():
    """CPU LGA 37,5 × 37,5 × 4,5. Mặt dưới (tiếp điểm) ở z = 0, nắp kim loại hướng +z.
    Góc tam giác ở (-x, -y). Hai rãnh định hướng ở hai cạnh x = ±18,75, tâm y = +5."""
    reset()
    sub = mat('substrate', '#21483d', 0.05, 0.62)
    ihs = mat('ihs', '#c8cdd2', 1.0, 0.3)
    gold = mat('gold', '#cba34d', 1.0, 0.28)

    h = 37.5 / 2
    nw, nd, ny = 2.0, 1.2, 5.0  # rãnh: rộng theo y, sâu vào đế, tâm y
    outline = [(-h, -h), (h, -h),
               (h, ny - nw / 2), (h - nd, ny - nw / 2), (h - nd, ny + nw / 2), (h, ny + nw / 2),
               (h, h), (-h, h),
               (-h, ny + nw / 2), (-h + nd, ny + nw / 2), (-h + nd, ny - nw / 2), (-h, ny - nw / 2)]
    prism(outline, 0.0, 1.2, sub, name='substrate')

    # Nắp tản nhiệt 32 × 32 × 3,3, bo cạnh 0,5, mặt trên phẳng
    box((-16.0, -16.0, 1.2), (16.0, 16.0, 4.5), ihs, bevel=0.5, seg=4)

    # Dấu tam giác cạnh 1,5 ở góc (-x, -y)
    tri = mat('triangle', '#d6ac50', 0.6, 0.4)
    prism([(-18.15, -18.15), (-16.65, -18.15), (-18.15, -16.65)], 1.2, 1.24, tri, name='triangle')

    # 1.200 tiếp điểm phẳng sát mặt đáy: lưới 40 × 40 bước 0,75, bỏ 20 × 20 ở giữa
    pads = []
    for i in range(40):
        for j in range(40):
            if 10 <= i < 30 and 10 <= j < 30:
                continue
            pads.append((-14.625 + i * 0.75, -14.625 + j * 0.75, 0.55, 0.55))
    quads(pads, -0.005, gold, axis='-z', name='pads')

    finish(out('cpu'), main='part_cpu')


# ---------------------------------------------------------------- 02 Mainboard
# Rãnh khoá DDR4: tâm rãnh lệch khỏi tâm thanh RAM theo y (mm). Dùng chung cho khe và thanh RAM.
DDR4_KEY = 4.25
PCB = 1.6          # mặt trên PCB
SOCKET = (19.0, 59.0)
RAM_X = (70.0, 79.0, 89.0, 98.0)
RAM_Y = 62.0
PCIE = ((-34.0, -30.0), (-34.0, -91.0), (-34.0, -132.0))
PCIE_LEN = 89.0
PCIE_KEY = 11.65   # khoảng từ đầu -x của khe tới vách chia khoá
M2 = (-5.0, -62.0)
M2_SCREW = (75.0, -62.0)
# Cụm I/O sau nằm trọn phía trên khe PCIe đầu (lá cổng GPU cao tới y -24 theo toạ độ bo)
IO_DY = 20.0
ARGB_HDR = (55.0, -143.0)   # đầu ARGB 5 V mới (tờ 12_case_front)
HOLES = [(x, y) for y in (142.5, 0.0, -142.5) for x in (-112.0, 0.0, 112.0)]
PUMP_HOLES = [(-20.0, 20.0), (58.0, 20.0), (-20.0, 98.0), (58.0, 98.0)]


def cutter(boxes, m, name='cutter'):
    """Gộp nhiều khối (lo, hi) thành một vật cắt để khoét một lần."""
    bm = bmesh.new()
    for lo, hi in boxes:
        r = bmesh.ops.create_cube(bm, size=1.0)
        for v in r['verts']:
            v.co = Vector([lo[i] + (v.co[i] + 0.5) * (hi[i] - lo[i]) for i in range(3)])
    return _obj(bm, m, name)


def tri_mark(x, y, m, size=1.5, z=PCB + 0.02):
    """Dấu tam giác chân 1 in trên bo."""
    prism([(x, y), (x + size, y), (x, y + size)], z - 0.01, z, m, name='pin1')


def header_4pin(c, m_body, m_pin, m_key, m_mark):
    """Đầu quạt 4 chân 1 hàng (bước 2,54): đế nhựa, 4 chân vàng, vách gờ dẫn hướng, dấu chân 1 bên trái."""
    x, y = c
    box((x - 5, y - 2.5, PCB), (x + 5, y + 2.5, PCB + 1.2), m_body)
    box((x - 5, y + 1.6, PCB), (x + 5, y + 2.5, PCB + 8.0), m_key)
    pins([(x - 3.81 + i * 2.54, y) for i in range(4)], PCB + 1.2, PCB + 8.0, m_pin)
    tri_mark(x - 6.8, y - 2.8, m_mark)


def header_grid(c, cols, rows, missing, m_body, m_pin, m_mark, pitch=2.54, shroud=False, m_shroud=None, vertical=False):
    """Hàng chân 2 hàng. Chân lẻ ở hàng dưới (1, 3, 5…), chân chẵn ở hàng trên, chân 1 ở góc dưới trái.
    missing: các chân bỏ trống. vertical: hàng dài chạy theo y."""
    x0, y0 = c
    pts = []
    for i in range(cols):
        for j in range(rows):
            n = i * rows + j + 1
            if n in missing:
                continue
            u = (i - (cols - 1) / 2) * pitch
            v = (j - (rows - 1) / 2) * pitch
            pts.append((x0 - v, y0 + u) if vertical else (x0 + u, y0 + v))
    w = cols * pitch + 0.6
    h = rows * pitch + 0.6
    if vertical:
        w, h = h, w
    box((x0 - w / 2, y0 - h / 2, PCB), (x0 + w / 2, y0 + h / 2, PCB + 1.6), m_body)
    pins(pts, PCB + 1.6, PCB + 7.5, m_pin)
    if vertical:
        tri_mark(x0 + w / 2 + 0.4, y0 - h / 2 - 1.6, m_mark)
    else:
        tri_mark(x0 - w / 2 - 1.9, y0 - h / 2 - 1.6, m_mark)
    if shroud:
        t = 0.8
        W, H = w + 2.2, h + 2.2
        bx, by = x0 - W / 2, y0 - H / 2
        walls = [box((bx, by, PCB), (bx + W, by + t, PCB + 9), m_shroud),
                 box((bx, by + H - t, PCB), (bx + W, by + H, PCB + 9), m_shroud),
                 box((bx, by, PCB), (bx + t, by + H, PCB + 9), m_shroud),
                 box((bx + W - t, by, PCB), (bx + W, by + H, PCB + 9), m_shroud)]
        # khe khoá xuyên vách, rộng 5, giữa cạnh dài phía -x
        boolean(walls[2] if vertical else walls[0],
                cutter([((bx - 0.5, y0 - 2.5, PCB + 1.6), (bx + t + 0.5, y0 + 2.5, PCB + 9.5))] if vertical
                       else [((x0 - 2.5, by - 0.5, PCB + 1.6), (x0 + 2.5, by + t + 0.5, PCB + 9.5))], m_shroud))


def power_socket(c, cols, rows, m_body, m_contact, latch_dir, size, rounded, vertical=True, base=PCB, depth=7.0):
    """Ổ nguồn Mini-Fit (ATX 24, EPS 8): thân nhựa có hốc sâu 7 (vuông hoặc vát góc theo `rounded`),
    tiếp điểm ở đáy hốc, ngàm khoá ở một cạnh."""
    x, y = c
    sx, sy, sz = size
    body = box((x - sx / 2, y - sy / 2, base), (x + sx / 2, y + sy / 2, base + sz), m_body)
    pitch = 4.2
    top = base + sz
    bm = bmesh.new()
    k = 0
    for i in range(cols):
        for j in range(rows):
            u = (i - (cols - 1) / 2) * pitch
            v = (j - (rows - 1) / 2) * pitch
            cx, cy = (x + v, y + u) if vertical else (x + u, y + v)
            c6 = 0.6 if rounded[k % len(rounded)] else 0.0
            pts = [(-1.5 + c6, -1.5), (1.5 - c6, -1.5), (1.5, -1.5 + c6), (1.5, 1.5), (-1.5, 1.5), (-1.5, -1.5 + c6)]
            vb = [bm.verts.new((cx + px, cy + py, top - depth)) for px, py in pts]
            vt = [bm.verts.new((cx + px, cy + py, top + 0.5)) for px, py in pts]
            bm.faces.new(vb[::-1])
            bm.faces.new(vt)
            for q in range(6):
                bm.faces.new((vb[q], vb[(q + 1) % 6], vt[(q + 1) % 6], vt[q]))
            k += 1
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    holes = _obj(bm, m_body, 'socket_holes')
    contacts = []
    for o in [holes]:
        for poly in o.data.polygons:
            if poly.normal.z > 0.9 and poly.center.z < top:
                contacts.append((poly.center.x, poly.center.y))
    boolean(body, holes)
    quads([(cx, cy, 1.6, 1.6) for cx, cy in contacts], top - depth + 0.05, m_contact, axis='z')
    lx, ly = latch_dir
    if lx:
        box((x + lx * sx / 2, y - 2.5, top - 6), (x + lx * (sx / 2 + 1.6), y + 2.5, top - 1), m_body)
        box((x + lx * (sx / 2 + 1.6) - (1.2 if lx > 0 else 0), y - 2.5, top - 1.5), (x + lx * (sx / 2 + 1.6) + (0 if lx > 0 else 1.2), y + 2.5, top + 0.8), m_body)
    else:
        box((x - 2.5, y + ly * sy / 2, top - 6), (x + 2.5, y + ly * (sy / 2 + 1.6), top - 1), m_body)


def heatsink(c, size, m, fins_axis='y', fin_count=7):
    """Khối tản nhôm có rãnh nông trên mặt."""
    x, y = c
    sx, sy, sz = size
    box((x - sx / 2, y - sy / 2, PCB), (x + sx / 2, y + sy / 2, PCB + sz * 0.7), m, bevel=0.6)
    n = fin_count
    for k in range(n):
        t = (k + 0.5) / n
        if fins_axis == 'y':
            fx = x - sx / 2 + t * sx
            box((fx - sx / n * 0.32, y - sy / 2 + 0.6, PCB + sz * 0.7 - 0.2), (fx + sx / n * 0.32, y + sy / 2 - 0.6, PCB + sz), m, bevel=0.25)
        else:
            fy = y - sy / 2 + t * sy
            box((x - sx / 2 + 0.6, fy - sy / n * 0.32, PCB + sz * 0.7 - 0.2), (x + sx / 2 - 0.6, fy + sy / n * 0.32, PCB + sz), m, bevel=0.25)


@part
def motherboard():
    """Bo ATX 244 × 305. Gốc ở tâm bo, mặt lưng z = 0, mặt linh kiện hướng +z.
    I/O ở mép -x (lưng máy), EPS ở mép trên, ATX 24 ở mép +x."""
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

    W, H = 244.0, 305.0
    board = box((-W / 2, -H / 2, 0), (W / 2, H / 2, PCB), pcb)

    # 9 lỗ bắt vít bo (H1–H9, Ø3,4) và 4 lỗ gá bơm (Ø3,5): khoét xuyên, vành kim loại hai mặt
    cuts = [cyl((x, y, PCB / 2), 1.7, PCB + 2, 'z', hole, 24) for x, y in HOLES]
    cuts += [cyl((x, y, PCB / 2), 1.75, PCB + 2, 'z', hole, 24) for x, y in PUMP_HOLES]
    boolean(board, join(cuts, 'holes_cut'))
    for i, (x, y) in enumerate(HOLES):
        ring((x, y), 3.6, 1.7, PCB + 0.02, tin)
        ring((x, y), 3.6, 1.7, -0.02, tin).data.flip_normals()
        tube((x, y, PCB / 2), 1.72, 1.7, PCB, 'z', tin, 24)
        label(f'H{i + 1}', x + 6.5, y - 4.5, PCB + 0.02, 2.2, silk)
    for x, y in HOLES:
        phillips((x, y, PCB + 1.0), 0, 2.8, steel, hole, 'z', h=2.0)
    for x, y in PUMP_HOLES:
        ring((x, y), 3.2, 1.75, PCB + 0.02, copper)
        ring((x, y), 3.2, 1.75, -0.02, copper).data.flip_normals()

    # ---- Socket LGA: khung thép, thân đen có lòng 38 × 38 sâu tới z 4,5, lưới tiếp điểm,
    # khung ép có cửa giữa, bản lề phía +y, cần khoá dọc cạnh +x
    sx, sy = SOCKET
    box((sx - 25, sy - 25, PCB), (sx + 25, sy + 25, PCB + 1.2), steel)
    sock = box((sx - 21.5, sy - 21.5, PCB), (sx + 21.5, sy + 21.5, 6.0), plastic)
    boolean(sock, cutter([((sx - 19.0, sy - 19.0, 4.5), (sx + 19.0, sy + 19.0, 7.0))], plastic))
    contacts = []
    for i in range(40):
        for j in range(40):
            if 10 <= i < 30 and 10 <= j < 30:
                continue
            contacts.append((sx - 14.625 + i * 0.75, sy - 14.625 + j * 0.75, 0.32, 0.32))
    quads(contacts, 4.65, gold, axis='z', name='lga_contacts')
    for k in (-1, 1):  # gờ khớp hai rãnh CPU, tâm y +5
        box((sx + k * 18.9 - 0.5, sy + 4.2, 4.5), (sx + k * 18.9 + 0.5, sy + 5.8, 6.0), plastic)
    tri = mat('tri', '#d6ac50', 0.6, 0.4)
    prism([(sx - 24, sy - 24), (sx - 20, sy - 24), (sx - 24, sy - 20)], PCB + 1.2, PCB + 1.25, tri)
    for a, b in (((-22, -22), (22, -17)), ((-22, 17), (22, 22)), ((-22, -17), (-17, 17)), ((17, -17), (22, 17))):
        box((sx + a[0], sy + a[1], 6.2), (sx + b[0], sy + b[1], 7.0), steel, bevel=0.2)
    box((sx - 22, sy + 22, 3.0), (sx + 22, sy + 25.5, 7.0), steel, bevel=0.4)
    box((sx - 6, sy - 25.5, 6.2), (sx + 6, sy - 22, 7.0), steel, bevel=0.2)
    cyl((sx + 27, sy + 1, 5.6), 0.9, 48, 'y', steel, 12)
    cyl((sx + 30.5, sy - 23, 5.6), 0.9, 7, 'x', steel, 12)
    cbox((sx + 27, sy - 20.5, 4.0), (3.5, 2.5, 4.0), steel)
    label('CPU', sx - 30, sy + 28, PCB + 0.02, 3.0, silk)

    # ---- VRM
    for k in range(7):
        cbox((sx - 30.5, sy - 30 + k * 9.5, PCB + 3.0), (7, 7, 6), choke, bevel=0.6)
    for k in range(5):
        cbox((sx - 18 + k * 9, sy + 32, PCB + 3.0), (7, 7, 6), choke, bevel=0.6)
    heatsink((-41.0, 59.0), (18, 68, 18), alu, 'x', 9)
    heatsink((10.0, 110.0), (44, 12, 16), alu, 'y', 9)
    for k in range(5):
        cyl((-27.0, 96.0 + k * 4.4, PCB + 3.5), 1.9, 7, 'z', tin, 16)

    # ---- 4 khe DIMM cao 12, rãnh có tiếp điểm hai bên, gờ khoá lệch tâm, chốt hai đầu. A2/B2 xám + chốt cyan
    for i, x in enumerate(RAM_X):
        body = plastic2 if i in (1, 3) else plastic
        L, top = 142.0, 8.4
        y0, y1 = RAM_Y - L / 2, RAM_Y + L / 2
        box((x - 3.0, y0, PCB), (x - 0.8, y1, PCB + top), body)
        box((x + 0.8, y0, PCB), (x + 3.0, y1, PCB + top), body)
        box((x - 0.8, y0, PCB), (x + 0.8, y1, PCB + 3.0), hole)
        quads([(RAM_Y, PCB + 4.5, L - 6, 3.0)], x - 0.79, gold, axis='x', name='dimm_contacts')
        quads([(RAM_Y, PCB + 4.5, L - 6, 3.0)], x + 0.79, gold, axis='-x', name='dimm_contacts')
        box((x - 0.8, RAM_Y + DDR4_KEY - 0.8, PCB), (x + 0.8, RAM_Y + DDR4_KEY + 0.8, PCB + 6.0), body)
        for e in (-1, 1):
            yy = RAM_Y + e * (L / 2 + 2.5)
            box((x - 3.2, yy - 2.5, PCB), (x + 3.2, yy + 2.5, PCB + 8.4), body)
            box((x - 2.6, yy - 1.6, PCB + 8.4), (x + 2.6, yy + 1.6, PCB + 15.0), cyan if i in (1, 3) else plastic2, bevel=0.3)
        label(['A1', 'A2', 'B1', 'B2'][i], x, y1 + 10.5, PCB + 0.02, 2.6, cyan if i in (1, 3) else silk)
    label('DDR4', 84, RAM_Y - 82, PCB + 0.02, 2.6, silk)

    # ---- 3 khe PCIe dọc theo x, chốt ở đầu +x. Khe đầu bọc thép
    for k, (x, y) in enumerate(PCIE):
        x0, x1 = x - PCIE_LEN / 2, x + PCIE_LEN / 2
        m = plastic
        box((x0, y - 3.75, PCB), (x1, y - 0.75, PCB + 11.0), m)
        box((x0, y + 0.75, PCB), (x1, y + 3.75, PCB + 11.0), m)
        box((x0, y - 0.75, PCB), (x1, y + 0.75, PCB + 4.0), hole)
        quads([(x, PCB + 6.0, PCIE_LEN - 4, 3.0)], y - 0.74, gold, axis='y', name='pcie_contacts')
        quads([(x, PCB + 6.0, PCIE_LEN - 4, 3.0)], y + 0.74, gold, axis='-y', name='pcie_contacts')
        box((x0 + PCIE_KEY - 0.9, y - 0.75, PCB), (x0 + PCIE_KEY + 0.9, y + 0.75, PCB + 9.0), m)
        box((x0 - 1.5, y - 3.75, PCB), (x0, y + 3.75, PCB + 11.0), m)
        box((x1, y - 3.75, PCB), (x1 + 6.0, y + 3.75, PCB + 9.0), m)
        box((x1 + 1.0, y - 1.4, PCB + 9.0), (x1 + 8.0, y + 1.4, PCB + 12.5), plastic2, bevel=0.3)
        if k == 0:
            box((x0 - 1.7, y - 4.15, PCB + 2.0), (x1 + 0.2, y - 3.75, PCB + 11.4), alu)
            box((x0 - 1.7, y + 3.75, PCB + 2.0), (x1 + 0.2, y + 4.15, PCB + 11.4), alu)
            box((x0 - 1.7, y - 4.15, PCB + 11.0), (x1 + 0.2, y - 2.4, PCB + 11.4), alu)
            box((x0 - 1.7, y + 2.4, PCB + 11.0), (x1 + 0.2, y + 4.15, PCB + 11.4), alu)
        label(['PCIe x16', 'PCIe 2', 'PCIe 3'][k], x0 + 14, y + 7.5, PCB + 0.02, 2.4, silk, align='LEFT')

    # ---- M.2: thân x -11…-5, miệng khe sâu 2,5 mở về +x (SSD nằm ở z 4,6…5,4), gờ M-key, tiếp điểm
    mx, my = M2
    mz0, mz1 = PCB + 2.6, PCB + 4.2
    m2 = box((mx - 6.0, my - 11.0, PCB), (mx, my + 11.0, PCB + 5.6), plastic)
    boolean(m2, cutter([((mx - 2.5, my - 9.5, mz0), (mx + 0.5, my + 9.5, mz1))], plastic))
    box((mx - 2.5, my - 6.5 - 0.5, mz0), (mx, my - 6.5 + 0.5, mz1), plastic)
    quads([(my + yy, (mz0 + mz1) / 2, 0.3, 1.2) for yy in [-9.0 + i * 0.5 for i in range(37)] if abs(yy + 6.5) > 0.8],
          mx - 2.48, gold, axis='x', name='m2_contacts')
    st = cyl((M2_SCREW[0], M2_SCREW[1], PCB + 1.5), 2.0, 3.0, 'z', mat('brass', '#b8a26a', 0.9, 0.35), 16)
    disc((M2_SCREW[0], M2_SCREW[1]), 0.9, PCB + 3.02, hole, 12)
    for (a, b) in (((mx, my - 11.2), (M2_SCREW[0], my - 10.9)), ((mx, my + 10.9), (M2_SCREW[0], my + 11.2)),
                   ((M2_SCREW[0] - 0.3, my - 11.2), (M2_SCREW[0], my - 4.0)), ((M2_SCREW[0] - 0.3, my + 4.0), (M2_SCREW[0], my + 11.2))):
        box((a[0], a[1], PCB), (b[0], b[1], PCB + 0.03), silk)
    label('M2_1  2280', mx + 30, my + 15, PCB + 0.02, 2.4, silk)

    # ---- Tản chipset
    heatsink((75.0, -97.0), (38, 30, 9), alu, 'x', 8)

    # ---- Đầu nguồn: ATX 24 (2 × 12) và EPS 8 (2 × 4), hốc có khoá vuông / vát
    power_socket((117.0, 40.0), 12, 2, plastic, gold, (1, 0), (10.0, 52.0, 13.0),
                 rounded=(1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0), vertical=True)
    tri_mark(110.0, 13.0, silk)
    label('ATX_24', 108, 40, PCB + 0.02, 2.4, silk, rot=90)
    power_socket((-60.0, 146.0), 4, 2, plastic, gold, (0, 1), (18.0, 9.0, 13.0), rounded=(0, 1, 1, 0, 1, 0, 0, 1), vertical=False)
    tri_mark(-71.0, 140.0, silk)
    label('CPU_8', -60, 138.0, PCB + 0.02, 2.4, silk)

    # ---- Đầu quạt 4 chân 1 hàng
    for name, c, lab in (('AIO_PUMP', (10.0, 126.0), (10.0, 131.0, 'CENTER')), ('CPU_FAN', (35.0, 126.0), (35.0, 131.0, 'CENTER')),
                         ('SYS_FAN1', (106.0, 85.0), (106.0, 90.0, 'CENTER')), ('SYS_FAN2', (82.0, -128.0), (82.0, -123.0, 'CENTER')),
                         ('SYS_FAN3', (95.0, -134.0), (88.0, -137.0, 'RIGHT')), ('SYS_FAN4', (-96.0, -135.0), (-96.0, -130.0, 'CENTER'))):
        header_4pin(c, plastic, gold, plastic2, silk)
        label(name, lab[0], lab[1], PCB + 0.02, 1.9, silk, align=lab[2])

    # ---- F_PANEL (2 × 5, bỏ chân 10), HD_AUDIO (2 × 5, bỏ chân 8), USB 3.0 (2 × 10 bước 2,0 có vỏ, bỏ chân 20)
    fx, fy = 95.0, -143.0
    header_grid((fx, fy), 5, 2, {10}, plastic, gold, silk)
    for i, (t_top, t_bot) in enumerate((('PLED+', 'HDD+'), ('PLED-', 'HDD-'), ('PWR', 'RST'), ('PWR', 'RST'), ('', ''))):
        if t_top:
            label(t_top, fx + (i - 2) * 2.54, fy + 4.4, PCB + 0.02, 0.9, silk)
        if t_bot:
            label(t_bot, fx + (i - 2) * 2.54, fy - 4.4, PCB + 0.02, 0.9, silk)
    label('F_PANEL', fx - 9.5, fy, PCB + 0.02, 1.7, silk, align='RIGHT')
    header_grid((-97.0, -143.0), 5, 2, {8}, plastic, gold, silk)
    label('HD_AUDIO', -97, -148.6, PCB + 0.02, 1.9, silk)
    header_grid((113.0, -127.0), 10, 2, {20}, plastic, gold, silk, pitch=2.0, shroud=True, m_shroud=plastic2, vertical=True)
    label('USB3_1', 105.0, -127, PCB + 0.02, 1.9, silk, rot=90)

    # ---- I/O sau (tờ 11_rear_io): 15 chỗ cắm + 2 nút trên lá chắn 158 × 36 tại x -136,2…-135,4 (lọt cửa lưng case).
    # Toạ độ (Y; h) theo tâm bo, h tính từ mặt PCB. Nhìn từ sau máy: Y hướng lên, h sang phải. Cổng mở về -x.
    io = mat('io_shield', '#bcc2c8', 0.8, 0.35)
    rim = mat('port_metal', '#c9ced4', 0.9, 0.3)
    cave = mat('port_cave', '#080a0d', 0.0, 0.9)
    ink = mat('io_ink', '#101216', 0.0, 0.6)
    SX0, SX1 = -136.2, -135.4
    XB = -118.0                     # đuôi thân cổng phía trong, chừa H4 / H1
    Z = lambda h: PCB + h           # noqa: E731
    shield = box((SX0, -18.0, PCB), (SX1, 140.0, PCB + 36.0), io)

    def yz_prism(pts, x0, x1, m):
        """Đa giác trên mặt (Y; h) đùn theo x."""
        return prism([(y, Z(h)) for y, h in pts], x0, x1, m, plane='yz')

    def rect(yc, hc, wy, hh, r=0.0):
        if r <= 0:
            return [(yc - wy / 2, hc - hh / 2), (yc + wy / 2, hc - hh / 2), (yc + wy / 2, hc + hh / 2), (yc - wy / 2, hc + hh / 2)]
        return [(y + yc, h + hc) for y, h in rounded_rect(wy, hh, r, 4)]

    def hole(poly):   # khoét lá chắn theo đa giác (Y; h)
        boolean(shield, yz_prism(poly, SX0 - 1.0, SX1 + 1.0, io))

    def socket(poly_out, poly_in, depth, shell=rim):
        """Vỏ cổng kim loại sau lá chắn, lòng tối sâu `depth`."""
        o = yz_prism(poly_out, SX0 + 0.1, XB, shell)
        boolean(o, yz_prism(poly_in, SX0 - 1.0, SX0 + depth, shell))
        yz_prism(poly_in, SX0 + depth, SX0 + depth + 0.2, cave)
        return o

    def text(t, y, h, size=1.5):
        o = label(t, 0, 0, 0, size, ink)
        o.data.transform(Matrix.Rotation(math.radians(-90), 4, 'Y'))
        o.data.transform(Matrix.Translation((SX0 - 0.02, y, Z(h))))

    # 01, 02: nút BIOS Flashback và Clear CMOS (Ø4, nút CMOS lùi 0,8)
    for h, deep, t in ((10.0, 0.0, 'BIOS'), (25.0, 0.8, 'CLR CMOS')):
        boolean(shield, cyl((SX0 + 0.4, 131.0, Z(h)), 2.3, 3.0, 'x', io, 24))
        cyl((SX0 + 1.0 + deep, 131.0, Z(h)), 2.0, 2.0, 'x', ink, 24)
        cyl((-128.0, 131.0, Z(h)), 3.0, 14.0, 'x', mat('btn_body', '#2a2e35', 0.2, 0.5), 16)
        text(t, 125.5, h, 1.3)
    # 03 HDMI (hình thang, 19 tiếp điểm) và 04 DisplayPort (vát một góc, 20 tiếp điểm), cạnh dài theo Y
    hdmi = [(108.0, 7.7), (122.0, 7.7), (122.0, 10.8), (120.0, 12.3), (110.0, 12.3), (108.0, 10.8)]
    hdmi_o = [(107.5, 7.2), (122.5, 7.2), (122.5, 11.0), (120.2, 12.8), (109.8, 12.8), (107.5, 11.0)]
    dp = [(106.9, 22.6), (123.1, 22.6), (123.1, 25.9), (121.6, 27.4), (106.9, 27.4)]
    dp_o = [(106.4, 22.1), (123.6, 22.1), (123.6, 26.1), (121.8, 27.9), (106.4, 27.9)]
    for inner, outer, n, hc, t in ((hdmi, hdmi_o, 19, 10.0, 'HDMI'), (dp, dp_o, 20, 25.0, 'DP')):
        hole(outer)
        socket([(y + (y - 115) * 0.08, h + (h - hc) * 0.2) for y, h in outer], inner, 6.0)
        box((SX0 + 1.5, 115.0 - 5.5, Z(hc) - 0.6), (SX0 + 5.8, 115.0 + 5.5, Z(hc) + 0.6), ink)
        for i in range(n):   # hai hàng tiếp điểm trên lưỡi giữa
            yy = 115.0 - 4.8 + (i // 2) * (9.6 / ((n + 1) // 2 - 1))
            sg = 1 if i % 2 else -1
            box((SX0 + 2.0, yy - 0.15, Z(hc) + sg * 0.6), (SX0 + 5.5, yy + 0.15, Z(hc) + sg * 0.65), gold)
        text(t, 104.0, hc, 1.3)
    # 05–10: USB-A, cạnh dài 13 theo Y; lưỡi nhựa lệch về +h, màu theo tốc độ
    usb_cols = {'2': '#101216', '5': '#1f6fd1', '10': '#c83c45'}
    for y, kind in ((94.0, '2'), (74.0, '5'), (54.0, '10')):
        tongue = mat(f'usb_{kind}', usb_cols[kind], 0.0, 0.55)
        for hc in (10.0, 25.0):
            hole(rect(y, hc, 14.6, 6.8))
            socket(rect(y, hc, 14.6, 6.8), rect(y, hc, 13.0, 5.0), 9.0)
            box((SX0 + 1.0, y - 5.5, Z(hc) + 0.2), (SX0 + 9.0, y + 5.5, Z(hc) + 2.0), tongue)
            for i in range(4):   # 4 tiếp điểm USB 2.0 sát miệng
                box((SX0 + 2.0, y - 3.4 + i * 2.2, Z(hc) + 0.05), (SX0 + 5.0, y - 2.6 + i * 2.2, Z(hc) + 0.2), gold)
            if kind != '2':      # thêm 5 tiếp điểm nằm sâu hơn
                for i in range(5):
                    box((SX0 + 6.0, y - 4.4 + i * 2.2, Z(hc) + 0.05), (SX0 + 8.0, y - 3.8 + i * 2.2, Z(hc) + 0.2), gold)
        text({'2': 'USB 2.0', '5': '5G', '10': '10G'}[kind], y - 9.5, 17.5, 1.3)
    text('BIOS', 94.0 - 9.5, 4.0, 1.1)
    # 11: LAN 2.5G (RJ45 8 tiếp điểm, rãnh ngàm phía +Y, 2 đèn)
    hole(rect(32.0, 12.0, 17.0, 15.0))
    hole(rect(39.5, 12.0, 3.0, 4.0))                     # rãnh ngàm RJ45 phía +Y, tới Y = 41
    lan = socket(rect(32.0, 12.0, 19.0, 18.0), rect(32.0, 12.0, 16.0, 14.0), 12.0)
    boolean(lan, yz_prism(rect(39.5, 12.0, 3.0, 4.0), SX0 - 1.0, SX0 + 12.0, rim))
    for i in range(8):
        hh = 12.0 - 4.55 + i * 1.3
        box((SX0 + 2.0, 32.0 - 6.9, Z(hh) - 0.25), (SX0 + 11.0, 32.0 - 6.6, Z(hh) + 0.25), gold)
    for hc, col in ((7.0, '#f2b544'), (17.0, '#63c64d')):
        box((SX0 - 0.05, 39.0 - 0.9, Z(hc) - 0.9), (SX0 + 0.3, 39.0 + 0.9, Z(hc) + 0.9), mat(f'led_{col}', col, 0.0, 0.3, emit=col, strength=0.6))
    text('2.5G', 21.0, 12.0, 1.3)
    # 12: USB-C 10 Gbps (lưỡi giữa, 12 + 12 tiếp điểm)
    hole(rect(32.0, 29.0, 10.0, 4.5, 2.2))
    socket(rect(32.0, 29.0, 10.0, 4.5, 2.2), rect(32.0, 29.0, 9.0, 3.5, 1.7), 7.0)
    box((SX0 + 1.0, 32.0 - 3.3, Z(29.0) - 0.35), (SX0 + 7.0, 32.0 + 3.3, Z(29.0) + 0.35), ink)
    for i in range(12):
        for sg in (-1, 1):
            box((SX0 + 1.5, 32.0 - 2.9 + i * 0.52, Z(29.0) + sg * 0.36), (SX0 + 5.0, 32.0 - 2.7 + i * 0.52, Z(29.0) + sg * 0.4), gold)
    text('10G', 21.5, 29.0, 1.3)
    # 13, 14: hai đầu anten Wi-Fi RP-SMA: ren ngoài Ø6,35 nhô 4 mm, đai ốc lục giác 8 mm, chân giữa
    sma = mat('sma_gold', '#c6a565', 1.0, 0.28)
    for hc in (10.0, 25.0):
        hole([(12.0 + 3.25 * math.cos(math.radians(a)), hc + 3.25 * math.sin(math.radians(a))) for a in range(0, 360, 20)])
        cyl((SX0 - 1.6, 12.0, Z(hc)), 3.175, 4.8, 'x', sma, 24)
        cyl((SX0 - 0.6, 12.0, Z(hc)), 4.6, 1.2, 'x', sma, 6)          # đai ốc lục giác
        for k in range(4):   # bước ren
            torus((SX0 - 1.4 - k * 0.8, 12.0, Z(hc)), 3.2, 0.18, 'x', sma, 24, 4)
        cyl((SX0 - 3.95, 12.0, Z(hc)), 2.2, 0.1, 'x', mat('sma_ins', '#e9e4d2', 0.0, 0.5), 20)
        cyl((SX0 - 3.3, 12.0, Z(hc)), 0.45, 1.4, 'x', sma, 12)
        cyl((-128.0, 12.0, Z(hc)), 3.0, 14.0, 'x', rim, 16)
    text('WIFI', 4.5, 17.5, 1.3)
    # 15, 16: jack 3,5 mm (vòng màu Ø6), 17: cổng quang có cửa che bụi
    for hc, col, t in ((7.0, '#7ebe72', 'OUT'), (19.0, '#e787a0', 'MIC')):
        hole([(-6.0 + 3.1 * math.cos(math.radians(a)), hc + 3.1 * math.sin(math.radians(a))) for a in range(0, 360, 20)])
        box((SX0 + 0.1, -12.0, Z(hc) - 6.0), (XB, 0.0, Z(hc) + 6.0), ink)
        tube((SX0 - 0.1, -6.0, Z(hc)), 3.0, 1.75, 0.8, 'x', mat(f'jack_{col}', col, 0.0, 0.45), 24)
        cyl((SX0 + 0.2, -6.0, Z(hc)), 1.75, 0.2, 'x', cave, 16)
        text(t, -13.0, hc, 1.2)
    hole(rect(-6.0, 30.0, 10.0, 10.0))
    socket(rect(-6.0, 30.0, 10.0, 10.0), rect(-6.0, 30.0, 9.0, 9.0), 3.0, shell=ink)
    box((SX0 + 0.3, -6.0 - 2.5, Z(30.0) - 2.5), (SX0 + 0.6, -6.0 + 2.5, Z(30.0) + 2.5), mat('optical_door', '#1b1d22', 0.2, 0.4))
    text('OPTICAL', -13.0, 30.0, 1.0)
    assert len(shield.data.polygons) > 50, 'lá chắn I/O bị khoét hỏng'

    # nắp nhựa I/O phía trong bo, chừa lỗ H4
    cover = box((-W / 2 + 4.0, -16.0, PCB), (-W / 2 + 20.0, 138.0, PCB + 30.0), mat('io_cover', '#23272e', 0.4, 0.45))
    boolean(cover, cyl((HOLES[3][0], HOLES[3][1], PCB + 15.0), 4.5, 40.0, 'z', plastic, 24))
    box((-W / 2 + 20.0, 60.0, PCB + 26.0), (-W / 2 + 21.0, 138.0, PCB + 30.0), alu)

    # ---- JRAINBOW1: đầu ARGB 5 V, 4 vị trí bước 2,54, khuyết vị trí 3 (tờ 12_case_front)
    jx, jy = ARGB_HDR
    box((jx - 5.75, jy - 2.5, PCB), (jx + 5.75, jy + 2.5, PCB + 1.6), plastic)
    box((jx - 5.75, jy + 1.6, PCB), (jx + 5.75, jy + 2.5, PCB + 7.0), plastic2)
    pins([(jx - 3.81 + i * 2.54, jy) for i in (0, 1, 3)], PCB + 1.6, PCB + 7.5, gold)
    tri_mark(jx - 7.6, jy - 2.8, silk)
    label('JRAINBOW1', jx, jy + 5.0, PCB + 0.02, 1.7, silk)
    label('+5V D - G', jx, jy - 4.6, PCB + 0.02, 1.0, silk)

    # ---- Chi tiết nhỏ
    cbox((-70.0, -110.0, PCB + 0.5), (8, 8, 1.0), chip)
    cbox((40.0, -138.0, PCB + 0.6), (12, 12, 1.2), chip)
    cbox((60.0, 10.0, PCB + 0.5), (7, 7, 1.0), chip)
    cyl((-30.0, -112.0, PCB + 1.6), 10.0, 3.2, 'z', tin, 32)
    label('BAT', -30, -125, PCB + 0.02, 2.2, silk)
    smd = mat('smd', '#7c6f5c', 0.2, 0.5)
    for k in range(10):
        cbox((sx - 12 + k * 2.6, sy - 29.5, PCB + 0.3), (1.6, 0.8, 0.6), smd)
        cbox((sx + 30.5, sy - 12 + k * 2.6, PCB + 0.3), (0.8, 1.6, 0.6), smd)
    for k in range(6):
        cbox((55.0 + k * 2.6, -78.0, PCB + 0.3), (1.6, 0.8, 0.6), smd)

    finish(out('motherboard'), main='part_motherboard')


# ---------------------------------------------------------------- Quạt dùng chung (két AIO, quạt vỏ)
def blades(r_hub, r_tip, thick, n, m, sweep=34.0, skew=0.35, name='blades'):
    """Cánh quạt quanh trục y (gió thổi theo +y). Mỗi cánh là một dải cong có góc nghiêng, dày 1 mm."""
    bm = bmesh.new()
    nr, na = 6, 7
    for b in range(n):
        th0 = b / n * 360.0
        grid = []
        for i in range(nr + 1):
            r = r_hub + (r_tip - r_hub) * i / nr
            row = []
            for j in range(na + 1):
                u = j / na
                th = math.radians(th0 + u * sweep + skew * (r - r_hub))
                y = (u - 0.5) * thick * 0.75
                row.append(bm.verts.new((r * math.cos(th), y, r * math.sin(th))))
            grid.append(row)
        for i in range(nr):
            for j in range(na):
                bm.faces.new((grid[i][j], grid[i][j + 1], grid[i + 1][j + 1], grid[i + 1][j]))
    o = _obj(bm, m, name, smooth_angle=60)
    sol = o.modifiers.new('sol', 'SOLIDIFY')
    sol.thickness = 1.0
    sol.offset = 0
    hub = cyl((0, 0, 0), r_hub + 0.5, thick * 0.8, 'y', m, 40, name='hub')
    cap = cyl((0, thick * 0.4 + 0.3, 0), r_hub * 0.7, 0.6, 'y', mat('hub_cap', '#1d2026', 0.3, 0.35), 32)
    return [o, hub, cap]


def fan_frame(size, thick, m, r_open=None, struts_side=1, hole_d=4.5, arrow_face='+z'):
    """Khung quạt vuông quanh trục y, lỗ tròn giữa, 4 lỗ vít góc, nan đỡ motor ở phía struts_side (±1)."""
    r_open = r_open or size / 2 - 2.0
    f = box((-size / 2, -thick / 2, -size / 2), (size / 2, thick / 2, size / 2), m, name='frame')
    boolean(f, cyl((0, 0, 0), r_open, thick + 2, 'y', m, 64))
    s = size / 2 - 7.5
    for x in (-s, s):
        for z in (-s, s):
            boolean(f, cyl((x, 0, z), hole_d / 2, thick + 2, 'y', m, 16))
    for md in f.data.polygons:
        md.use_smooth = False
    parts = [f]
    # nan đỡ motor + đế motor
    yy = struts_side * (thick / 2 - 1.5)
    parts.append(cyl((0, yy, 0), size * 0.17, 3.0, 'y', m, 40))
    for k in range(4):
        a = math.radians(45 + 90 * k + 8)
        o = cbox((0, yy, 0), (r_open + 1.0, 3.0, 3.2), m)
        o.data.transform(Matrix.Translation((r_open / 2, 0, 0)))
        o.data.transform(Matrix.Rotation(-a, 4, 'Y'))
        parts.append(o)
    # mũi tên chiều gió (+y) đúc nổi giữa một mặt bên của khung
    am = mat('arrow', '#3b4048', 0.2, 0.5)
    arrow = [(-2.5, -4.0), (2.5, -4.0), (2.5, 0.0), (4.0, 0.0), (0.0, 4.0), (-4.0, 0.0), (-2.5, 0.0)]
    sgn = 1 if arrow_face[0] == '+' else -1
    if arrow_face[1] == 'z':
        parts.append(prism(arrow, sgn * size / 2, sgn * (size / 2 + 0.4), am, plane='xy'))
    else:
        parts.append(prism(arrow, sgn * size / 2, sgn * (size / 2 + 0.4), am, plane='yz'))
    return parts


def transform(objs, M):
    for o in objs:
        o.data.transform(M)


# ---------------------------------------------------------------- 04 Tản nước AIO 240
PUMP_FITTINGS = [(-9.0, 30.0, 34.0), (9.0, 30.0, 34.0)]   # đầu ống trên bơm (mm, gốc ở tâm đáy bơm)


def spring(c, r, z0, z1, turns, wire, m):
    """Lò xo xoắn liên tục quanh trục z."""
    n = int(turns * 16)
    pts = [(c[0] + r * math.cos(t / 16 * math.tau), c[1] + r * math.sin(t / 16 * math.tau), z0 + (z1 - z0) * t / n) for t in range(n + 1)]
    return bezier_tube(pts, wire / 2, m, seg=8, res=2)


def phillips(c, z, r, m_head, m_slot, axis='z', h=2.0, sign=1):
    """Đầu vít tròn có rãnh chữ thập. Mặt có rãnh hướng theo sign × trục."""
    x, y, zz = c
    cyl((x, y, zz), r, h, axis, m_head, 20)
    face = sign * h / 2 + sign * 0.01
    L, w = r * 1.3, 0.8
    if axis == 'z':
        box((x - L / 2, y - w / 2, zz + face - 0.02), (x + L / 2, y + w / 2, zz + face + 0.02), m_slot)
        box((x - w / 2, y - L / 2, zz + face - 0.02), (x + w / 2, y + L / 2, zz + face + 0.02), m_slot)
    elif axis == 'y':
        box((x - L / 2, y + face - 0.02, zz - w / 2), (x + L / 2, y + face + 0.02, zz + w / 2), m_slot)
        box((x - w / 2, y + face - 0.02, zz - L / 2), (x + w / 2, y + face + 0.02, zz + L / 2), m_slot)
    else:
        box((x + face - 0.02, y - L / 2, zz - w / 2), (x + face + 0.02, y + L / 2, zz + w / 2), m_slot)
        box((x + face - 0.02, y - w / 2, zz - L / 2), (x + face + 0.02, y + w / 2, zz + L / 2), m_slot)


PUMP_FITTINGS = [(-9.0, 36.0, 34.0), (9.0, 36.0, 34.0)]   # tâm miệng ra của hai cút (mm, gốc ở tâm đáy bơm)


@part
def aio_pump():
    """Bơm Ø54 × 48 nằm trên CPU. Gốc ở tâm đế đồng, mặt nắp hướng +z.
    Gá 4 tai sơn đen, lỗ Ø3,4 ở 4 góc hình vuông 78 × 78, 4 vít lò xo M3 × 12. Hai cút ống ra phía +y."""
    reset()
    body = mat('pump_body', '#16181d', 0.2, 0.5)
    cyan = mat('cyan_ring', '#22d3ee', 0.0, 0.35)
    copper = mat('copper', '#b87333', 0.95, 0.25)
    steel = mat('steel', '#929aa3', 0.9, 0.3)
    slot = mat('hole', '#050506', 0.0, 0.9)
    hose_m = mat('hose', '#202329', 0.0, 0.8)
    film = mat('peel_film', '#e8f4ff', 0.0, 0.2, alpha=0.55)

    cyl((0, 0, 0.75), 21.0, 1.5, 'z', copper, 48)
    # màng bảo vệ có tai bóc (bóc trước khi lắp)
    film_parts = [cyl((0, 0, -0.03), 21.0, 0.05, 'z', film, 48), box((19.0, -3.0, -0.055), (29.0, 3.0, -0.005), film)]
    cyl((0, 0, 3.75), 26.0, 4.5, 'z', body, 48)
    # gá 4 tai liền một khối: mỗi tai là dải rộng 9 nở thành đầu tròn Ø12 quanh lỗ Ø3,4
    R = 39.0 * math.sqrt(2)
    tab = [(24.0, -4.5), (R - 5.0, -4.5)]
    tab += [(R + 6.0 * math.cos(math.radians(a)), 6.0 * math.sin(math.radians(a))) for a in range(-110, 111, 10)]
    tab += [(R - 5.0, 4.5), (24.0, 4.5)]
    arms = []
    for sx in (-1, 1):
        for sy in (-1, 1):
            arm = prism(tab, 4.0, 6.0, body, name='arm')
            arm.data.transform(Matrix.Rotation(math.atan2(sy, sx), 4, 'Z'))
            boolean(arm, cyl((39.0 * sx, 39.0 * sy, 5.0), 1.7, 4.0, 'z', body, 16))
            arms.append(arm)
    # 4 vít lò xo: thân Ø3 z -1…11, đầu Ø6,4 z 11…14 có rãnh chữ thập, lò xo liền z 6…11
    for sx in (-1, 1):
        for sy in (-1, 1):
            c = (39.0 * sx, 39.0 * sy)
            cyl((c[0], c[1], 5.0), 1.5, 12.0, 'z', steel, 12)
            phillips((c[0], c[1], 12.5), 0, 3.2, steel, slot, 'z', h=3.0)
            spring(c, 2.25, 6.0, 11.0, 4, 0.6, steel)
    # thân z 6…41, vòng cyan z 41…43, nắp trơn z 43…48
    cyl((0, 0, 23.5), 27.0, 35.0, 'z', body, 64)
    cyl((0, 0, 42.0), 27.0, 2.0, 'z', cyan, 64)
    cyl((0, 0, 45.5), 27.0, 5.0, 'z', body, 64, r2=26.0)
    # hai cút xoay: vòng xoay trên thân, khớp, đầu ống có ngấn, miệng ra (±9; 36; 34) hướng +y
    fit = mat('fitting', '#1b1d22', 0.6, 0.3)
    for x, y, z in PUMP_FITTINGS:
        cyl((x, 26.5, z), 6.2, 3.0, 'y', steel, 24)
        cyl((x, 29.5, z), 5.5, 3.0, 'y', fit, 24)
        tube((x, 31.5, z), 5.9, 4.0, 1.0, 'y', steel, 24)
        cyl((x, 34.0, z), 5.5, 4.0, 'y', fit, 24)
        cyl((x, 37.0, z), 5.6, 2.0, 'y', hose_m, 24)
    # gốc dây bơm: dây mềm Ø2,5 lộ 20 mm, men theo ống
    bezier_tube([(15.5, 24.0, 27.25), (15.5, 32.0, 28.0), (14.0, 42.0, 30.0)], 1.25, hose_m, seg=8)
    finish(out('aio_pump'), groups={'peel_film': film_parts}, main='part_aio_pump')


@part
def aio_backplate():
    """Tấm đỡ sau bo 90 × 90 kèm đệm cách điện và 4 trụ nhận vít M3.
    Gốc ở tâm socket trên MẶT LƯNG bo (z = 0); tấm nằm sau bo (z < 0), trụ xuyên lỗ bơm lên tới mặt dưới tai gá."""
    reset()
    plate = mat('backplate', '#16181d', 0.5, 0.45)
    pad = mat('insulator', '#30343b', 0.0, 0.8)
    steel = mat('steel', '#929aa3', 0.9, 0.3)
    hole = mat('hole', '#050506', 0.0, 0.9)
    box((-45.0, -45.0, -3.0), (45.0, 45.0, -1.0), plate, bevel=0.6)
    box((-44.0, -44.0, -1.0), (44.0, 44.0, -0.02), pad)
    top = 9.2 + 4.0   # mặt CPU (9,2) + đáy tai gá (4)
    for sx in (-1, 1):
        for sy in (-1, 1):
            c = (39.0 * sx, 39.0 * sy)
            cyl((c[0], c[1], 0.3), 1.5, 2.6, 'z', steel, 12)              # đoạn ren xuyên lỗ bo
            cyl((c[0], c[1], (PCB + top) / 2), 3.0, top - PCB, 'z', plate, 20)
            disc(c, 1.5, top + 0.01, hole, 12)
    finish(out('aio_backplate'), main='part_aio_backplate')


RAD_LEN, RAD_W, RAD_T, FAN_T = 277.0, 120.0, 27.0, 25.0


@part
def aio_radiator():
    """Két 277 × 120 × 27 kèm 2 quạt 120 bên dưới. Gốc ở giữa mặt trên, két treo xuống (-y).
    Bình đầu có 2 đầu ống và nắp nạp ở phía +x. Cánh quạt là node blades_1, blades_2 quay quanh y."""
    reset()
    frame = mat('rad_frame', '#16181d', 0.2, 0.55)
    fin = mat('rad_fin', '#101216', 0.6, 0.55)
    tube_m = mat('rad_tube', '#2a2d33', 0.6, 0.4)
    steel = mat('steel', '#929aa3', 0.9, 0.3)
    fan_m = mat('fan_frame', '#101216', 0.0, 0.55)
    blade_m = mat('fan_blade', '#101216', 0.0, 0.45)
    hole = mat('hole', '#050506', 0.0, 0.9)

    core = 240.0
    half = RAD_LEN / 2
    y0, y1 = -RAD_T, 0.0
    # hai bình đầu
    box((-half, y0, -RAD_W / 2), (-core / 2, y1, RAD_W / 2), frame, bevel=1.2)
    box((core / 2, y0, -RAD_W / 2), (half, y1, RAD_W / 2), frame, bevel=1.2)
    # hai thanh viền dọc rộng 10 (z 50…60) chứa lỗ ren
    for sg in (-1, 1):
        box((-core / 2, y0, min(sg * 50.0, sg * 60.0)), (core / 2, y1, max(sg * 50.0, sg * 60.0)), frame)
    # ống nước dẹt + lá tản xếp sát nhau
    zin = 50.0
    n_t = 12
    for k in range(n_t):
        z = -zin + (k + 0.5) * (2 * zin / n_t)
        box((-core / 2, y0 + 0.5, z - 0.9), (core / 2, y1 - 0.5, z + 0.9), tube_m)
    for k in range(int(core / 2.4)):
        x = -core / 2 + 1.2 + k * 2.4
        box((x - 0.18, y0 + 1.0, -zin), (x + 0.18, y1 - 1.0, zin), fin)
    # lỗ ren trên và dưới (8 + 8): x = ±7,5; ±112,5, z = ±52,5
    grid = [(x, z) for x in (-112.5, -7.5, 7.5, 112.5) for z in (-52.5, 52.5)]
    for x, z in grid:
        cyl((x, y1 + 0.01, z), 1.5, 0.02, 'y', hole, 16)
        cyl((x, y0 - 0.01, z), 1.5, 0.02, 'y', hole, 16)
    # S3: 8 vít M3 × 6 + 8 đệm, bắt từ trên nóc case (nóc dày 3 mm) xuống két
    for x, z in grid:
        tube((x, 3.5, z), 3.5, 1.7, 1.0, 'y', steel, 20)
        phillips((x, 5.0, z), 0, 2.8, steel, hole, 'y', h=2.0, sign=1)
    # đầu ống (2) dưới bình phía +x, lệch ±15 theo z; nắp nạp Ø10 trên bình
    for z in (-15.0, 15.0):
        cyl((129.0, y0 - 4.0, z), 6.0, 8.0, 'y', steel, 24)
        cyl((129.0, y0 - 10.0, z), 5.6, 6.0, 'y', mat('hose', '#202329', 0.0, 0.8), 24)
    cyl((129.0, y1 + 1.0, 40.0), 5.0, 2.0, 'y', steel, 24)

    # hai quạt bên dưới, nan đỡ motor hướng lên két, gió thổi +y
    spin = {}
    for i, cx in enumerate((-60.0, 60.0)):
        fy = y0 - FAN_T / 2
        parts = fan_frame(120.0, FAN_T, fan_m, struts_side=1, arrow_face='+z')
        transform(parts, Matrix.Translation((cx, fy, 0)))
        bl = blades(20.0, 56.0, FAN_T - 4, 7, blade_m)
        transform(bl, Matrix.Translation((cx, fy - 1.0, 0)))
        spin[f'blades_{i + 1}'] = bl
        # S4: 4 vít M3 × 30 xuyên khung quạt vào két, đệm 1 mm, đầu có rãnh chữ thập ở mặt dưới
        for x in (-52.5, 52.5):
            for z in (-52.5, 52.5):
                cyl((cx + x, -38.0, z), 1.5, 30.0, 'y', steel, 12)
                tube((cx + x, -52.5, z), 3.5, 1.7, 1.0, 'y', steel, 20)
                phillips((cx + x, -54.0, z), 0, 2.8, steel, hole, 'y', h=2.0, sign=-1)
        # dây quạt ra ở góc phía bo (-z), đoạn ngắn men khung
        sgx = -1 if cx < 0 else 1
        bezier_tube([(cx + sgx * 52.5, -39.5, -58.0), (cx + sgx * 52.5, -32.0, -63.0), (cx + sgx * 40.0, -29.0, -66.0)],
                    1.3, mat('hose', '#202329', 0.0, 0.8), seg=8)
    finish(out('aio_radiator'), groups=spin, main='part_aio_radiator')


# ---------------------------------------------------------------- 05 RAM DDR4
@part
def ram():
    """Thanh DDR4 133,35 × 42 × 6. Chân vàng ở z = 0, thân hướng +z, chiều dài theo y, bề dày theo x.
    Rãnh khoá lệch tâm tại y = DDR4_KEY (khớp gờ trong khe của bo). Hai hõm giữ chốt ở hai đầu, z 7…10."""
    reset()
    pcb = mat('ram_pcb', '#18201e', 0.1, 0.7)
    gold = mat('gold', '#c9a34a', 1.0, 0.28)
    spreader = mat('spreader', '#16181d', 0.5, 0.45)
    accent = mat('spreader_edge', '#3a3f47', 0.85, 0.3)
    cyan = mat('cyan_edge', '#22d3ee', 0.0, 0.35)

    L = 133.35
    h = L / 2
    k = DDR4_KEY
    r = 0.3
    key = [(k - 1.0, 0), (k - 1.0, 3.0 - r), (k - 1.0 + r, 3.0), (k + 1.0 - r, 3.0), (k + 1.0, 3.0 - r), (k + 1.0, 0)]
    outline = [(-h, 0)] + key + [(h, 0), (h, 7.0), (h - 1.5, 7.0), (h - 1.5, 10.0), (h, 10.0), (h, 31.25),
                                 (-h, 31.25), (-h, 10.0), (-h + 1.5, 10.0), (-h + 1.5, 7.0), (-h, 7.0)]
    prism(outline, -0.6, 0.6, pcb, plane='yz', name='ram_pcb')
    # 288 tiếp điểm: 144 mỗi mặt, bước 0,85, chừa rãnh khoá
    pads = []
    for i in range(147):
        y = -61.9 + i * 0.85
        if abs(y - k) < 1.5:
            continue
        pads.append((y, 2.6, 0.55, 4.2))
    assert len(pads) == 144, len(pads)
    quads(pads, 0.62, gold, axis='x', name='pads_a')
    quads(pads, -0.62, gold, axis='-x', name='pads_b')
    # Hai má tản nhiệt: vát hai góc trên, hõm giữ chốt ở hai đầu, gờ chéo trang trí, vát cạnh 0,4
    shape = [(-h, 6.0), (h, 6.0), (h, 7.0), (h - 1.5, 7.0), (h - 1.5, 10.0), (h, 10.0), (h, 37.0),
             (65.0, 42.0), (-65.0, 42.0), (-h, 37.0), (-h, 10.0), (-h + 1.5, 10.0), (-h + 1.5, 7.0), (-h, 7.0)]
    for sg in (-1, 1):
        bevel_obj(prism(shape, min(sg * 0.7, sg * 3.0), max(sg * 0.7, sg * 3.0), spreader, plane='yz', name='spreader'), 0.4)
        for e in (-1, 1):
            ridge = [(e * 40.0, 16.0), (e * 52.0, 36.0), (e * 62.0, 36.0), (e * 62.0, 33.0), (e * 54.0, 33.0), (e * 43.0, 16.0)]
            if e > 0:
                ridge = ridge[::-1]
            prism(ridge, min(sg * 3.0, sg * 3.0 + sg * 0.01), max(sg * 3.0, sg * 3.0 + sg * 0.01), accent, plane='yz', name='ridge')
    # Thanh nóc + dải cyan (màu, không phát sáng)
    box((-2.6, -65.0, 40.0), (2.6, 65.0, 41.0), spreader)
    box((-2.0, -65.0, 41.0), (2.0, 65.0, 42.0), cyan)
    finish(out('ram'), main='part_ram')


# ---------------------------------------------------------------- 06 GPU
GPU_L, GPU_H, GPU_T = 280.0, 125.0, 55.0
GPU_PCB_Y = GPU_T / 2 - 2.0 - 0.8      # tâm PCB theo y (lưng card ở +y)
GPU_FINGERS = (53.5, 142.5)            # u (mm từ lá cổng) của dải chân PCIe, khớp khe x16 của bo
GPU_FAN_U = (50.0, 140.0, 230.0)


@part
def gpu():
    """Card đồ hoạ 3 quạt, dáng card phổ thông (vỏ đen, viền xám bạc, tấm lưng kim loại), không logo.
    Gốc: x = 0 ở giữa chiều dài, y = 0 ở giữa bề dày (tấm lưng +y, quạt -y), z = 0 ở mép PCB phía bo.
    u = x + 140 tính từ lá cổng (x = -140)."""
    reset()
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

    X = lambda u: u - GPU_L / 2  # noqa: E731
    yt = GPU_T / 2
    vent = [(224, 34), (228, 30), (268, 30), (272, 34), (272, 104), (268, 108), (228, 108), (224, 104)]

    # Tấm lưng + PCB, cùng khoét cửa thoát khí xuyên (u 224…272, v 30…108, vát góc 4)
    bp = box((X(2), yt - 2.0, 2), (X(279), yt, 122), back)
    py0, py1 = GPU_PCB_Y - 0.8, GPU_PCB_Y + 0.8
    board = box((X(1.5), py0, 0), (X(278), py1, 121), pcb)
    for o in (bp, board):
        boolean(o, prism([(X(u), v) for u, v in vent], py0 - 1.0, yt + 1.0, back, plane='xz'))
    for u in (20, 90, 160, 215):
        for v in (12, 112):
            cyl((X(u), yt + 0.15, v), 1.6, 0.3, 'y', silver, 12)
    # Chân PCIe x16: 11 chân đoạn ngắn + 71 chân đoạn dài mỗi mặt (164 tổng), rãnh khoá, móc giữ cuối dải
    f0, f1 = GPU_FINGERS
    kx = f0 + PCIE_KEY
    box((X(f0), py0, -8.0), (X(kx - 1.0), py1, 0.01), pcb)
    box((X(kx + 1.0), py0, -8.0), (X(f1), py1, 0.01), pcb)
    box((X(144.5), py0, -6.0), (X(148.5), py1, 0.01), pcb)
    box((X(148.5), py0, -3.0), (X(151.5), py1, 0.01), pcb)
    short = [f0 + 0.5 + (kx - 1.0 - f0 - 1.0) * i / 10 for i in range(11)]
    long_ = [kx + 1.5 + (f1 - 0.5 - kx - 1.5) * i / 70 for i in range(71)]
    pads = [(X(u), -4.2, 0.6, 7.0) for u in short + long_]
    quads(pads, py1 + 0.02, gold, axis='y', name='fingers_a')
    quads(pads, py0 - 0.02, gold, axis='-y', name='fingers_b')
    # Khối tản 270 × 110 × 34: lá nhôm vuông góc chiều dài, 3 ống đồng
    for i in range(123):
        u = 5.25 + i * (269.5 / 122)
        box((X(u) - 0.25, -10.5, 7.5), (X(u) + 0.25, 23.5, 117.5), fin)
    for v in (30.0, 62.0, 94.0):
        cyl((0, 8.0, v), 3.0, 262.0, 'x', copper, 16)
    # Vỏ quạt: mặt dưới dày 2 (y -27,5…-25,5) có 3 lỗ Ø82, đầu trước vát góc
    outline = [(X(3), 2), (X(262), 2), (X(279), 20), (X(279), 105), (X(262), 123), (X(3), 123)]
    base = prism(outline, -yt, -yt + 2.0, shroud, plane='xz', name='shroud_face')
    boolean(base, join([cyl((X(u), -yt + 1.0, 62.5), 41.0, 6.0, 'y', shroud, 64) for u in GPU_FAN_U], 'fan_holes'))
    wall = -2.0   # thành vỏ (dày 2) che tới đây, phần lá tản phía trên vẫn lộ như card thật
    prism([(X(3), 123), (X(262), 123), (X(262), 125), (X(3), 125)], wall, 12.0, shroud, plane='xz')
    prism([(X(20), 125.0), (X(250), 125.0), (X(250), 125.3), (X(20), 125.3)], 4.0, 7.0, silver, plane='xz')
    for poly in ([(X(3), 123), (X(262), 123), (X(262), 125), (X(3), 125)],
                 [(X(3), 0), (X(262), 0), (X(262), 2), (X(3), 2)],
                 [(X(262), 0), (X(279), 18), (X(279), 20), (X(262), 2)],
                 [(X(262), 123), (X(279), 105), (X(279), 107), (X(262), 125)],
                 [(X(278), 18), (X(280), 20), (X(280), 105), (X(278), 107)],
                 [(X(2), 2), (X(4), 2), (X(4), 123), (X(2), 123)]):
        prism(poly, -yt, wall, shroud, plane='xz')
    for u in GPU_FAN_U:
        tube((X(u), -yt + 0.6, 62.5), 43.5, 41.0, 1.2, 'y', gun, 64)
    strips = [
        [(X(95), 2), (X(101), 2), (X(88), 62), (X(101), 123), (X(95), 123), (X(82), 62)],
        [(X(185), 2), (X(191), 2), (X(178), 62), (X(191), 123), (X(185), 123), (X(172), 62)],
        [(X(5), 12), (X(10), 12), (X(10), 113), (X(5), 113)],
        [(X(262), 4), (X(266), 4), (X(277), 20), (X(273), 20)],
        [(X(262), 121), (X(266), 121), (X(277), 105), (X(273), 105)],
    ]
    for st in strips:
        prism(st, -yt, -yt + 0.4, silver, plane='xz', name='accent')
    # Ba quạt Ø80 (node riêng để app cho quay)
    spin = {}
    for i, u in enumerate(GPU_FAN_U):
        bl = blades(14.0, 40.0, 11.0, 11, blade_m, sweep=30, skew=0.55)
        transform(bl, Matrix.Rotation(math.pi, 4, 'X'))   # gió thổi vào card (+y)
        transform(bl, Matrix.Translation((X(u), -yt + 8.0, 62.5)))
        spin[f'gpu_fan_{i + 1}'] = bl
    # Ổ nguồn 8 chân (2 × 4, kiểu 6+2) trên mép ngoài gần đầu trước, miệng hướng +z, ngàm phía +y
    power_socket((X(265.0), GPU_PCB_Y - 6.15), 4, 2, plastic, mat('contact', '#b8bec5', 0.9, 0.3), (0, 1),
                 (18.0, 10.7, 12.0), rounded=(1, 0, 1, 0, 0, 1, 0, 1), vertical=False, base=113.0, depth=6.0)
    # Lá cổng 2 khe (v -2…118), 3 DP + 1 HDMI xếp dọc v, lưới thoát khí xuyên, 2 tai có rãnh hở 3,8
    by0, by1 = GPU_PCB_Y + 6.0 - 40.64, GPU_PCB_Y + 6.0
    plate = box((X(-1.0), by0, -2.0), (X(0.0), by1, 118.0), bracket_m)
    yc = by1 - 13.0
    port_cut = []
    ports = ((20.0, 'dp'), (44.0, 'dp'), (68.0, 'dp'), (92.0, 'hdmi'))
    for v, kind in ports:
        if kind == 'dp':
            prof = [(v - 8.0, yc - 2.5), (v + 8.0, yc - 2.5), (v + 8.0, yc + 1.0), (v + 6.5, yc + 2.5), (v - 8.0, yc + 2.5)]
        else:
            prof = [(v - 7.0, yc - 2.5), (v + 7.0, yc - 2.5), (v + 7.0, yc + 0.5), (v + 5.0, yc + 2.5), (v - 5.0, yc + 2.5), (v - 7.0, yc + 0.5)]
        port_cut.append((prof, v))
    vent_holes = []
    for i in range(4):
        for jj in range(17):
            vv = 4 + jj * 6.6
            yy = by0 + 4.0 + i * 3.6 + (1.8 if jj % 2 else 0)
            vent_holes.append(cyl((X(-0.5), yy, vv), 1.3, 3.0, 'x', hole, 6))
    cut_objs = [prism([(z, y) for z, y in prof], X(-2.0), X(1.0), hole, plane='yz') for prof, v in
                [([(y, z) for z, y in pr], v) for pr, v in port_cut]]
    boolean(plate, join(cut_objs + vent_holes, 'bracket_cut'))
    for prof, v in port_cut:
        # vỏ ổ: khung kim loại dày 0,6, lòng sâu 6, lưỡi tiếp điểm ở giữa
        shell = prism([(y, z) for z, y in prof], X(0.0), X(6.0), silver, plane='yz')
        inner = [((z - v) * 0.9 + v, (y - yc) * 0.75 + yc) for z, y in prof]
        boolean(shell, prism([(y, z) for z, y in inner], X(-0.5), X(5.0), silver, plane='yz'))
        box((X(5.0), yc - 2.6, v - 8.0), (X(6.0), yc + 2.6, v + 8.0), hole)
        box((X(1.5), yc - 0.5, v - 5.5), (X(5.0), yc + 0.5, v + 5.5), plastic)
        for c2 in range(10):
            vv = v - 4.5 + c2 * 1.0
            box((X(1.6), yc + 0.5, vv - 0.15), (X(4.8), yc + 0.55, vv + 0.15), gold)
            if c2 < (10 if v < 90 else 9):
                box((X(1.6), yc - 0.55, vv - 0.15), (X(4.8), yc - 0.5, vv + 0.15), gold)
    # 2 tai bắt vít ở đầu xa bo (v 108…118), rãnh hở 3,8; 2 vít #6-32 × 6 theo trục x, đầu ở ngoài (-x)
    steel = mat('steel', '#a8adb4', 0.9, 0.3)
    for yy in (by1 - 10.16, by1 - 30.48):
        boolean(plate, cutter([((X(-2.0), yy - 1.9, 111.0), (X(1.0), yy + 1.9, 119.0))], hole))
        cyl((X(-1.0) - 1.0, yy, 113.0), 2.8, 2.0, 'x', steel, 20)
        box((X(-1.0) - 2.02, yy - 0.4, 113.0 - 1.8), (X(-1.0) - 1.98, yy + 0.4, 113.0 + 1.8), hole)
        box((X(-1.0) - 2.02, yy - 1.8, 113.0 - 0.4), (X(-1.0) - 1.98, yy + 1.8, 113.0 + 0.4), hole)
        cyl((X(-1.0) + 3.0, yy, 113.0), 1.75, 6.0, 'x', steel, 12)
    finish(out('gpu'), groups=spin, main='part_gpu')


# ---------------------------------------------------------------- 07 SSD M.2 2280
SSD_Z0 = 1.1   # mặt dưới PCB SSD so với gốc model (ngồi đúng trên chân đỡ 3 mm của bo)


@part
def ssd():
    """SSD M.2 2280: 80 × 22 × 0,8, chip ở mặt trên. Gốc ở tâm, đầu cắm M-key ở -x, rãnh bán nguyệt ở +x.
    Vít M2 × 2,5 là node riêng (ssd_screw)."""
    reset()
    pcb = mat('ssd_pcb', '#20252a', 0.1, 0.7)
    chip = mat('ssd_chip', '#111418', 0.25, 0.4)
    gold = mat('gold', '#c8a65c', 1.0, 0.28)
    steel = mat('steel', '#a8adb4', 0.9, 0.3)
    smd = mat('smd', '#7c6f5c', 0.2, 0.5)
    hole = mat('hole', '#050506', 0.0, 0.9)
    z0, z1 = SSD_Z0, SSD_Z0 + 0.8
    k0, k1 = -7.45, -5.55   # rãnh M-key
    r = 1.2
    arc = [(40.0 + r * math.cos(math.radians(a)), r * math.sin(math.radians(a))) for a in range(90, 271, 15)]
    outline = [(-40, -11), (-40, k0), (-36, k0), (-36, k1), (-40, k1), (-40, 11), (40, 11)] + arc + [(40, -11)]
    prism(outline[::-1], z0, z1, pcb, name='ssd_pcb')
    top = [(-38.0, -9.75 + 0.5 * i, 3.6, 0.35) for i in range(38) if not 5 <= i <= 8]
    bot = [(-38.0, -9.75 + 0.5 * i, 3.6, 0.35) for i in range(37) if not 5 <= i <= 8]
    quads(top, z1 + 0.02, gold, axis='z', name='m2_pads_top')
    quads(bot, z0 - 0.02, gold, axis='-z', name='m2_pads_bottom')
    # nửa vành kim loại quanh rãnh vít (x ≤ 40)
    bm = bmesh.new()
    outer = [bm.verts.new((40.0 + 2.4 * math.cos(math.radians(a)), 2.4 * math.sin(math.radians(a)), z1 + 0.02)) for a in range(90, 271, 10)]
    inner = [bm.verts.new((40.0 + r * math.cos(math.radians(a)), r * math.sin(math.radians(a)), z1 + 0.02)) for a in range(90, 271, 10)]
    for i in range(len(outer) - 1):
        bm.faces.new((outer[i], outer[i + 1], inner[i + 1], inner[i]))
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    _obj(bm, steel, 'notch_ring')
    cbox((14.0 - 40.0, 0.0, z1 + 0.6), (10.0, 10.0, 1.2), chip, bevel=0.2)
    cbox((33.0 - 40.0, 0.0, z1 + 0.6), (16.0, 18.0, 1.2), chip, bevel=0.2)
    cbox((54.0 - 40.0, 0.0, z1 + 0.6), (16.0, 18.0, 1.2), chip, bevel=0.2)
    for k in range(6):
        cbox((-34.0 + k * 1.6, 8.5, z1 + 0.3), (0.8, 1.6, 0.6), smd)
        cbox((25.0 + k * 1.6, -9.0, z1 + 0.3), (0.8, 1.6, 0.6), smd)
    # vít M2 × 2,5: đầu Ø3,5 × 0,7, thân Ø2 dài 2,5 dưới đầu, rãnh chữ thập khoét sâu 0,2
    head = cyl((40.0, 0.0, z1 + 0.35), 1.75, 0.7, 'z', steel, 24)
    cut_each(head, [((38.8, -0.2, z1 + 0.5), (41.2, 0.2, z1 + 0.8)), ((39.8, -1.2, z1 + 0.5), (40.2, 1.2, z1 + 0.8))], steel)
    shaft = cyl((40.0, 0.0, z1 - 1.25), 1.0, 2.5, 'z', steel, 16)
    finish(out('ssd'), groups={'ssd_screw': [head, shaft]}, main='part_ssd')


# ---------------------------------------------------------------- 08 Nguồn ATX
@part
def psu():
    """Nguồn ATX 150 rộng (z) × 86 cao (y) × 140 sâu (x), dây liền. Gốc ở tâm khối.
    Mặt sau (C14, công tắc, lưới xuyên) ở -x, mặt ra dây ở +x, quạt hút ở đáy (-y)."""
    reset()
    body_m = mat('psu_body', '#16181d', 0.25, 0.6)
    inner_m = mat('psu_inside', '#0b0c0e', 0.2, 0.8)
    grille = mat('psu_grille', '#252932', 0.7, 0.4)
    plastic = mat('plastic', '#101216', 0.0, 0.55)
    rubber = mat('rubber', '#17191c', 0.0, 0.8)
    metal = mat('contact', '#b8bec5', 0.9, 0.3)
    hole = mat('hole', '#050506', 0.0, 0.9)
    white = mat('print', '#f2f4f7', 0.0, 0.6)
    blade_m = mat('psu_blade', '#101216', 0.0, 0.5)
    steel = mat('steel', '#a8adb4', 0.9, 0.3)
    D, Hh, Wd = 140.0, 86.0, 150.0
    xb = -D / 2
    Z = lambda u: u - Wd / 2  # noqa: E731
    Y = lambda v: v - Hh / 2  # noqa: E731
    body = box((-D / 2, -Hh / 2, -Wd / 2), (D / 2, Hh / 2, Wd / 2), body_m, bevel=1.0)
    # khoang rỗng sau lưới, lòng C14, khoang quạt đáy Ø116, 4 lỗ ren
    cav = [((xb + 1.0, Y(12), Z(12)), (xb + 30.0, Y(74), Z(104)))]
    c14 = [(Y(27) - 9.0, Z(124) - 11.5), (Y(27) + 9.0 - 3.0, Z(124) - 11.5), (Y(27) + 9.0, Z(124) - 8.5),
           (Y(27) + 9.0, Z(124) + 8.5), (Y(27) + 9.0 - 3.0, Z(124) + 11.5), (Y(27) - 9.0, Z(124) + 11.5)]
    cuts = [cutter(cav, inner_m),
            prism(c14, xb - 1.0, xb + 12.0, inner_m, plane='yz'),
            cyl((0, -Hh / 2 + 12.5, 0), 58.0, 27.0, 'y', inner_m, 64)]
    cuts += [cyl((xb + 2.0, Y(v), Z(u)), 1.75, 8.0, 'x', inner_m, 16) for u, v in ((6, 6), (144, 6), (6, 80), (138, 70))]
    boolean(body, join(cuts, 'psu_cut'))
    # lưới tổ ong xuyên tấm sau dày 1
    hexes = []
    d = 4.0
    rr = d / 2
    row, y = 0, Y(12) + rr
    while y < Y(74) - rr:
        z = Z(12) + rr + (2.5 if row % 2 else 0)
        while z < Z(104) - rr:
            hexes.append(cyl((xb + 0.5, y, z), rr, 3.0, 'x', inner_m, 6))
            z += 5.0
        y += 4.35
        row += 1
    boolean(body, join(hexes, 'hex_cut'))
    # 3 lá C14 nằm trong lòng ổ
    for dy, dz in ((5.0, 0.0), (-4.0, -7.0), (-4.0, 7.0)):
        box((xb + 2.0, Y(27) + dy - 2.0, Z(124) + dz - 0.4), (xb + 10.0, Y(27) + dy + 2.0, Z(124) + dz + 0.4), metal)
    # Công tắc bập bênh (ở O), ký hiệu I/O nằm trên mặt rocker và xoay cùng
    box((xb - 0.8, Y(57) - 13.5, Z(124) - 9.5), (xb + 0.5, Y(57) + 13.5, Z(124) + 9.5), plastic)
    rk = [box((xb - 3.0, Y(57) - 11.0, Z(124) - 7.0), (xb - 0.8, Y(57) + 11.0, Z(124) + 7.0), plastic, bevel=0.6),
          box((xb - 3.1, Y(57) + 4.0, Z(124) - 0.4), (xb - 3.0, Y(57) + 8.0, Z(124) + 0.4), white),
          torus((xb - 3.05, Y(57) - 6.0, Z(124)), 1.9, 0.3, 'x', white, 24, 6)]
    for o in rk:
        o.data.transform(Matrix.Translation((-(xb - 0.8), -Y(57), 0)))
        o.data.transform(Matrix.Rotation(math.radians(8), 4, 'Z'))
        o.data.transform(Matrix.Translation(((xb - 0.8), Y(57), 0)))
    # 4 vít S8 #6-32 × 6, đầu chữ thập ở ngoài lưng vỏ (vỏ dày 1)
    for u, v in ((6, 6), (144, 6), (6, 80), (138, 70)):
        cyl((xb - 4.0, Y(v), Z(u)), 2.8, 2.0, 'x', steel, 20)      # đầu vít ngoài lưng case (tôn 3 mm)
        box((xb - 5.02, Y(v) - 0.4, Z(u) - 1.8), (xb - 4.98, Y(v) + 0.4, Z(u) + 1.8), hole)
        box((xb - 5.02, Y(v) - 1.8, Z(u) - 0.4), (xb - 4.98, Y(v) + 1.8, Z(u) + 0.4), hole)
        cyl((xb, Y(v), Z(u)), 1.75, 6.0, 'x', steel, 12)
    # Quạt đáy 120: khung trong khoang, 7 cánh, lồng dây thép tròn Ø1,5
    yb = -Hh / 2
    for R in (59.25, 47.0, 36.0, 25.0, 14.0):
        torus((0, yb - 1.0, 0), R, 0.75, 'y', grille, 72, 8)
    for k in range(4):
        o = cyl((29.6, yb - 1.0, 0), 0.75, 59.25, 'x', grille, 8)
        o.data.transform(Matrix.Rotation(math.radians(45 + 90 * k), 4, 'Y'))
    tube((0, yb + 12.5, 0), 58.0, 56.5, 25.0, 'y', mat('fan_frame', '#101216', 0.0, 0.55), 64)
    bl = blades(16.0, 55.0, 14.0, 7, blade_m, sweep=40)
    transform(bl, Matrix.Translation((0, yb + 12.0, 0)))
    # Mặt trước: vòng cao su lối ra dây (Ø24/Ø18) + 3 nhánh dây tách rõ (phần còn lại vẽ trong app)
    xf = D / 2
    gy, gz = -Hh / 2 + 30.0, Wd / 2 - 25.0
    tube((xf + 1.0, gy, gz), 12.0, 9.0, 3.0, 'x', rubber, 32)
    for (dy, dz), rad in zip(((4.0, 0.0), (-3.0, -4.0), (-3.0, 4.0)), (4.0, 3.0, 3.0)):
        bezier_tube([(xf - 2.0, gy + dy, gz + dz), (xf + 14.0, gy + dy * 1.6, gz + dz * 1.6), (xf + 30.0, gy + dy * 2.2 - 6.0, gz + dz * 2.4)], rad, rubber)
    finish(out('psu'), main='part_psu')


# ---------------------------------------------------------------- 09 Quạt vỏ 120
def _case_fan(argb, size=120.0):
    """Quạt vỏ 120 × 120 × 25. Gốc ở tâm, trục quay theo x: mặt hút +x, mặt nan (mặt thổi) -x, gió đi -x.
    Cánh là node 'blades' quay quanh x. 4 góc cao su, mũi tên gió nổi trên mặt +z, dây ra ở góc (y +, z -)."""
    reset()
    frame_m = mat('fan_frame', '#16181d', 0.0, 0.6)
    blade_m = mat('fan_blade', '#252932', 0.0, 0.45)
    rubber = mat('fan_rubber', '#101216', 0.0, 0.85)
    wire = mat('wire', '#17191c', 0.0, 0.7)
    h = size / 2
    hp = h - 7.75 if size > 120 else 52.5      # bước lỗ vít: 105 (120 mm), 124,5 (140 mm)
    parts = fan_frame(size, 25.0, frame_m, r_open=h - 3.0, struts_side=1, hole_d=4.3, arrow_face='+z')
    bl = blades(h / 3, h - 5.0, 18.0, 7, blade_m, sweep=42)
    bl[1].data.transform(Matrix.Scale(20.0 / (18.0 * 0.8), 4, Vector((0, 1, 0))))   # hub cao 20
    # 4 góc cao su trên mặt hút (-y trước khi xoay), có lỗ trùng lỗ khung
    pads = []
    for sx in (-1, 1):
        for sz in (-1, 1):
            lo = (min(sx * (h - 20.0), sx * h), -12.6, min(sz * (h - 20.0), sz * h))
            hi = (max(sx * (h - 20.0), sx * h), -10.6, max(sz * (h - 20.0), sz * h))
            o = box(lo, hi, rubber)
            boolean(o, cyl((sx * hp, -11.6, sz * hp), 2.15, 4.0, 'y', rubber, 16))
            pads.append(o)
    # mũi tên chiều quay (cong theo tiếp tuyến) trên cùng mặt bên với mũi tên gió
    am = mat('arrow', '#3b4048', 0.2, 0.5)
    rot = [(-14.0, -2.0), (-4.0, -2.0), (-4.0, -4.5), (0.0, 0.0), (-4.0, 4.5), (-4.0, 2.0), (-14.0, 2.0)]
    pads.append(prism([(u + 30.0, v) for u, v in rot], h, h + 0.4, am, plane='xy'))
    # dây ra ở góc: cổ chống gập + đoạn dây ngắn (phần còn lại vẽ trong app)
    sx0 = h - 8.0   # dây ra ở góc phía khay
    stub = [cyl((sx0, 10.0, -(h + 1.0)), 2.0, 5.0, 'z', rubber, 12),
            bezier_tube([(sx0, 10.0, -(h + 3.0)), (sx0, 10.0, -(h + 10.0)), (sx0 - 7.0, 10.0, -(h + 16.0))], 1.4, wire, seg=8)]
    allp = parts + pads + stub
    if argb:
        # vòng khuếch tán ARGB Ø114/Ø110 ở mặt hút, sáng cyan dịu; chụp tâm đen
        ring_m = mat('argb_ring', '#e8f4f4', 0.0, 0.35, emit='#22d3ee', strength=2.2)
        allp.append(tube((0, -12.75, 0), h - 3.0, h - 5.0, 1.5, 'y', ring_m, 96))
        led = mat('argb_led', '#22d3ee', 0.0, 0.3, emit='#22d3ee', strength=4.0)
        for k in range(12):   # 12 LED địa chỉ hoá sau vòng khuếch tán
            a = k / 12 * math.tau
            allp.append(cbox(((h - 4.0) * math.cos(a), -11.9, (h - 4.0) * math.sin(a)), (1.4, 0.3, 1.4), led))
        allp.append(cyl((0, -9.6, 0), 14.0, 0.6, 'y', mat('hub_cap_black', '#252932', 0.2, 0.4), 40))
    # xoay để gió (+y cục bộ) thành -x
    R = Matrix.Rotation(math.radians(90), 4, 'Z')
    transform(allp + bl, R)
    finish(out('case_fan_argb' if argb else 'case_fan'), groups={'blades': bl}, main='part_fan')


@part
def case_fan():
    """Quạt vỏ thường (quạt sau)."""
    _case_fan(False)


@part
def case_fan_argb():
    """Quạt trước 140 mm có vòng ARGB cyan."""
    _case_fan(True, 140.0)


# ---------------------------------------------------------------- 01 Vỏ mid-tower
CASE_W, CASE_H, CASE_D = 220.0, 470.0, 450.0
TRAY_Z = -102.5
BOARD_O = (-88.0, 62.5, -96.0)          # tâm bo trong vỏ (boardOrigin của app)
FRONT_FANS_Y = (-142.0, 0.0, 142.0)    # 3 quạt trước 140 mm, hở nhau 2 mm
FRONT_HP = 62.25                         # nửa bước lỗ vít quạt 140 (124,5)
REAR_FAN = (145.0, 14.0)                # (y, z) tâm quạt sau
SHROUD_Y, SHROUD_X = -120.0, 170.0
GROMMETS = {  # tâm (x, y), phần mở (x, y)
    'G0': ((-35.0, -170.0), (45.0, 28.0)), 'G1': ((-148.0, 224.0), (32.0, 10.0)), 'G2': ((57.0, 110.0), (22.0, 55.0)),
    'G3': ((-65.0, 224.0), (40.0, 10.0)), 'G5': ((23.0, -104.0), (48.0, 16.0)), 'G6': ((-184.0, -104.0), (42.0, 16.0)),
}
G4_DUCT = (95.0, 123.0, 13.0, 37.0, -2.0, 22.0)   # x0, x1, z0, z1 của ống che; miệng y0…y1 hướng -x, tâm (95; 10; 25)
PCIE_TOP, PCIE_PITCH = 38.5, 20.32                # mép trên khe 1 (y) và bước khe
PSU_C = (-152.0, -189.0, -26.0)                   # tâm nguồn: mặt sau áp mặt trong tấm lưng (x -222), đáy y -232, z -101…49


def grommet(cx, cy, w, h, z, rubber, dark):
    """Lỗ luồn dây trên khay: viền cao su rộng 3 hai mặt, màng cao su có khe ở giữa."""
    t = 3.0
    for zz in (z + 0.5, z - 1.5):
        box((cx - w / 2 - t, cy - h / 2 - t, zz), (cx + w / 2 + t, cy - h / 2, zz + 1.0), rubber, bevel=0.3)
        box((cx - w / 2 - t, cy + h / 2, zz), (cx + w / 2 + t, cy + h / 2 + t, zz + 1.0), rubber, bevel=0.3)
        box((cx - w / 2 - t, cy - h / 2, zz), (cx - w / 2, cy + h / 2, zz + 1.0), rubber, bevel=0.3)
        box((cx + w / 2, cy - h / 2, zz), (cx + w / 2 + t, cy + h / 2, zz + 1.0), rubber, bevel=0.3)
    skin = box((cx - w / 2, cy - h / 2, z - 0.3), (cx + w / 2, cy + h / 2, z + 0.3), dark)
    if w >= h:
        boolean(skin, cutter([((cx - w / 2 + 2.0, cy - 1.5, z - 1), (cx + w / 2 - 2.0, cy + 1.5, z + 1))], dark))
    else:
        boolean(skin, cutter([((cx - 1.5, cy - h / 2 + 2.0, z - 1), (cx + 1.5, cy + h / 2 - 2.0, z + 1))], dark))


def grille_rings(c, R, axis, m, x_plane):
    """Lưới quạt kiểu vòng đồng tâm + 4 nan, dùng cho cửa quạt sau."""
    for r in (R, R * 0.78, R * 0.56, R * 0.34):
        torus(c, r, 0.9, axis, m, 64, 6)
    for k in range(4):
        o = cyl((0, R / 2, 0), 0.9, R, 'y', m, 6)
        o.data.transform(Matrix.Rotation(math.radians(45 + 90 * k), 4, 'X'))
        o.data.transform(Matrix.Translation(c))


@part
def case():
    """Vỏ mid-tower 220 × 470 × 450 (không gồm chân). Gốc ở tâm vỏ.
    x: lưng (-) → trước (+), y: đáy → nóc, z: nắp kín (-) → kính (+). Mặt kính để trống, app tự vẽ kính."""
    reset()
    steel = mat('case_steel', '#16181d', 0.2, 0.65)
    inner = mat('case_inner', '#191b20', 0.15, 0.7)
    meshm = mat('case_mesh', '#101216', 0.15, 0.7)
    rubber = mat('rubber', '#202329', 0.0, 0.85)
    dark = mat('grommet_skin', '#0e0f12', 0.0, 0.9)
    hole = mat('hole', '#050506', 0.0, 0.9)
    screw_m = mat('screw', '#929aa5', 0.9, 0.3)
    brass = mat('brass', '#b89451', 0.9, 0.35)
    cyan = mat('case_accent', '#22d3ee', 0.0, 0.35, emit='#22d3ee', strength=0.8)
    port_metal = mat('port_metal', '#c9ced4', 0.9, 0.3)
    usb_blue = mat('usb_tongue', '#1f6fd1', 0.0, 0.5)

    W, H, D = CASE_W, CASE_H, CASE_D
    xr, xf = -D / 2, D / 2
    yb, yt = -H / 2, H / 2
    zb, zg = -W / 2, W / 2
    T = 3.0

    # ---- Đáy, nóc, khung kính
    floor = box((xr, yb, zb), (xf, yb + T, zg), steel)
    # cửa hút gió dưới quạt nguồn + lọc bụi nam châm
    px, _, pz = PSU_C
    boolean(floor, cutter([((px - 60.0, yb - 1, pz - 60.0), (px + 60.0, yb + T + 1, pz + 60.0))], steel))
    for k in range(24):
        box((px - 60.0 + k * 5.0, yb - 2.0, pz - 60.0), (px - 58.5 + k * 5.0, yb - 1.0, pz + 60.0), meshm)
    box((px - 64.0, yb - 2.5, pz - 64.0), (px + 64.0, yb - 1.0, pz - 60.0), meshm)
    box((px - 64.0, yb - 2.5, pz + 60.0), (px + 64.0, yb - 1.0, pz + 64.0), meshm)
    for sx in (-1, 1):
        for sz in (-1, 1):
            cbox((sx * (D / 2 - 35.0), yb - 6.0, sz * (W / 2 - 25.0)), (50.0, 12.0, 30.0), rubber, bevel=2.0)
    # ---- Nóc (tờ 13_case_top): cửa thoáng 330 × 188 R6, gân chữ X ba ô + sống giữa, tấm đục lỗ Ø1,2 bước 1,8,
    # lọc nam châm trong hốc bậc, 8 tai gá két giữ nguyên, I/O cân giữa (Reset · USB · Power · USB · Headset)
    roof = box((xr, yt - T, zb), (xf, yt, zg), steel)
    VX0, VX1, VZ = -175.0, 155.0, 94.0
    vent = rounded_rect(VX1 - VX0, 2 * VZ, 6.0, 4, cx=(VX0 + VX1) / 2, cy=0.0)
    pocket = rounded_rect(348.4, 206.4, 15.2, 6, cx=-10.0, cy=0.0)
    boolean(roof, prism(pocket, yt - 0.6, yt + 1.0, steel, plane='xz'))                 # hốc bậc đặt lọc, đáy Y 234,4
    boolean(roof, prism(vent, yt - T - 1.0, yt + 1.0, steel, plane='xz'))               # cửa thoáng xuyên tôn
    boolean(roof, cutter([((164.0, yt - 0.6, -7.0), (170.0, yt + 1.0, 7.0))], steel))   # hõm móng tay kéo lọc
    rib = mat('roof_rib', '#3a3f47', 0.4, 0.5)
    ry0, ry1 = yt - 1.55, yt - 1.05            # gân Y 233,45…233,95: nổi trên tấm đục lỗ, dưới đệm vít S3 (234) và lưới lọc
    rib_objs = []

    def bar(p0, p1, w):
        """Gân thẳng rộng w trên mặt phẳng xz, hai đầu tròn."""
        dx, dz = p1[0] - p0[0], p1[1] - p0[1]
        L = math.hypot(dx, dz)
        o = cbox(((p0[0] + p1[0]) / 2, (ry0 + ry1) / 2, (p0[1] + p1[1]) / 2), (L, ry1 - ry0, w), rib)
        rib_objs.append(o)
        o.data.transform(Matrix.Translation((-(p0[0] + p1[0]) / 2, 0, -(p0[1] + p1[1]) / 2)))
        o.data.transform(Matrix.Rotation(-math.atan2(dz, dx), 4, 'Y'))
        o.data.transform(Matrix.Translation(((p0[0] + p1[0]) / 2, 0, (p0[1] + p1[1]) / 2)))
        for q in (p0, p1):
            rib_objs.append(cyl((q[0], (ry0 + ry1) / 2, q[1]), w / 2, ry1 - ry0, 'y', rib, 16))

    edge = VZ - 3.0                       # đầu gân nằm trong mép cửa (bo R6)
    for i in range(3):
        a0, a1 = VX0 + 110.0 * i, VX0 + 110.0 * (i + 1)
        c = (a0 + 55.0, 0.0)
        for corner in ((a0 + (3.0 if i == 0 else 0.0), -edge), (a0 + (3.0 if i == 0 else 0.0), edge),
                       (a1 - (3.0 if i == 2 else 0.0), -edge), (a1 - (3.0 if i == 2 else 0.0), edge)):
            bar(c, corner, 5.0)
    bar((VX0 + 3.0, 0.0), (VX1 - 3.0, 0.0), 6.0)                     # sống giữa
    for xg in (-65.0, 45.0):
        bar((xg, -edge), (xg, edge), 5.0)                             # hai gân ngang
    for zr in (-16.5, 88.5):                                          # hai ray đỡ tai gá két, ngắt quanh mỗi tai (chừa đệm Ø7)
        stops = [VX0 + 3.0] + [v for x in (-114.0, -9.0, 6.0, 111.0) for v in (x - 4.5, x + 4.5)] + [VX1 - 3.0]
        for k in range(0, len(stops), 2):
            if stops[k + 1] - stops[k] > 1.0:
                box((stops[k], ry0, zr - 2.0), (stops[k + 1], ry1, zr + 2.0), rib)
    grid = [(x, z) for x in (-114.0, -9.0, 6.0, 111.0) for z in (-16.5, 88.5)]
    for x, z in grid:   # 8 tai gá két Ø10 / lỗ Ø3,4 (vít S3 nằm trong model két)
        tube((x, yt - 1.5, z), 5.0, 1.7, 3.0, 'y', steel, 24)
    # tấm đục lỗ Ø1,2 bước 1,8 (lưới tam giác), khoét 8 cửa Ø12 quanh tai
    perf_m = perforated_material('roof_perf', '#101216', 1.2, 1.8)
    plate = perforated_plate(rounded_rect(334.0, 192.0, 8.0, 4, cx=-10.0, cy=0.0), yt - 1.95, yt - 1.55, perf_m, 'roof_perf')
    for x, z in grid:
        boolean(plate, cyl((x, yt - 1.75, z), 6.0, 2.0, 'y', perf_m, 24))
    # lọc nam châm: viền nylon 9 mm (Y 234,4…236), lưới PET mịn có 8 lỗ Ø10 + vòng gia cường, tai kéo
    nylon = mat('filter_frame', '#202329', 0.0, 0.8)
    ring_f = prism(rounded_rect(348.0, 206.0, 15.0, 6, cx=-10.0, cy=0.0), yt - 0.6, yt + 1.0, nylon, plane='xz', name='filter_frame')
    boolean(ring_f, prism(vent, yt - 1.0, yt + 2.0, nylon, plane='xz'))
    mesh_top = prism(vent, yt + 0.2, yt + 0.5, mat('filter_mesh_top', '#202329', 0.0, 0.9, alpha=0.3), plane='xz', name='filter_mesh')
    for x, z in grid:
        boolean(mesh_top, cyl((x, yt + 0.35, z), 5.0, 1.0, 'y', nylon, 24))
        tube((x, yt + 0.35, z), 6.0, 5.0, 0.3, 'y', nylon, 24)
    box((154.0, yt + 0.65, -4.0), (164.0, yt + 0.9, 4.0), nylon)                      # tai kéo
    for x in (-156.0, -58.0, 38.0, 136.0):                                            # 8 nam châm (ẩn trong viền)
        for z in (-98.5, 98.5):
            cbox((x, yt - 0.15, z), (8.0, 0.8, 5.0), mat('magnet', '#636a73', 0.8, 0.4))
    box((xr, yt - T - 8.0, zg - 4.0), (xf, yt - T, zg), steel)        # mép khung trên phía kính
    box((xr, yb + T, zg - 4.0), (xf, yb + T + 8.0, zg), steel)        # mép khung dưới phía kính

    # Cụm I/O trên nóc, cân giữa tại X 190: Reset Z -68 · USB-A Z -32 · Power Z 0 · USB-A Z +32 · Headset Z +68,
    # đèn HDD amber ở (204; 0). Vùng I/O khắc viền X 174…210, Z ±88.
    io_x = 190.0
    for z in (-32.0, 32.0):
        boolean(roof, cutter([((io_x - 3.0, yt - T - 1, z - 6.5), (io_x + 3.0, yt + 1, z + 6.5))], steel))
    for z, r in ((0.0, 7.2), (-68.0, 2.7), (68.0, 3.6)):
        boolean(roof, cyl((io_x, yt - 1.5, z), r, T + 2, 'y', steel, 32))
    boolean(roof, cyl((204.0, yt - 1.5, 0.0), 0.8, T + 2, 'y', steel, 12))
    btn = mat('btn', '#2a2e35', 0.7, 0.3)
    rim = mat('io_rim', '#929aa5', 0.9, 0.3)
    cyl((io_x, yt + 0.8, 0.0), 7.0, 1.6, 'y', btn, 40)                                 # 06 Power Ø14
    tube((io_x, yt + 0.6, 0.0), 8.1, 7.2, 1.2, 'y', cyan, 48)                          # vòng PLED Ø16,2 / 14,4
    cyl((io_x, yt + 1.61, 0.0), 2.6, 0.02, 'y', mat('pwr_icon', '#aeb4bc', 0.0, 0.6), 24)
    cyl((io_x, yt - 0.25, -68.0), 2.5, 0.5, 'y', btn, 24)                              # 08 Reset Ø5, lõm 0,5
    tube((io_x, yt - 0.25, -68.0), 3.5, 2.7, 0.5, 'y', rim, 24)
    tube((io_x, yt - 0.25, 68.0), 3.5, 1.75, 0.5, 'y', rim, 24)                        # 09 jack tai nghe TRRS Ø3,5
    cyl((io_x, yt - 0.6, 68.0), 1.75, 0.2, 'y', hole, 16)
    cyl((204.0, yt - 0.1, 0.0), 0.75, 0.2, 'y', mat('hdd_led', '#f59e0b', 0.0, 0.3, emit='#f59e0b', strength=1.2), 12)
    for z in (-32.0, 32.0):                                                            # 07 USB-A 3.0
        p = box((io_x - 3.5, yt - 8.0, z - 7.0), (io_x + 3.5, yt + 0.2, z + 7.0), port_metal)
        boolean(p, cutter([((io_x - 2.5, yt - 7.0, z - 6.2), (io_x + 2.5, yt + 1.0, z + 6.2))], port_metal))
        box((io_x + 0.2, yt - 6.5, z - 5.5), (io_x + 1.8, yt - 0.4, z + 5.5), usb_blue)
        for k in range(9):   # 9 tiếp điểm USB 3.0 (4 dài + 5 ngắn)
            box((io_x - 0.05, yt - (3.0 if k < 4 else 2.0), z - 4.5 + k * 1.1), (io_x + 0.2, yt - 0.6, z - 4.1 + k * 1.1), mat('gold', '#c6a565', 1.0, 0.3))
    # đường khắc viền vùng I/O + chữ khắc (đọc được khi đứng trước máy nhìn xuống)
    groove = mat('io_groove', '#0b0c0e', 0.0, 0.8)
    io_out = rounded_rect(36.0, 176.0, 6.0, 6, cx=192.0, cy=0.0)
    io_in = rounded_rect(35.4, 175.4, 5.7, 6, cx=192.0, cy=0.0)
    g = prism(io_out, yt - 0.15, yt + 0.01, groove, plane='xz')
    boolean(g, prism(io_in, yt - 0.5, yt + 0.5, groove, plane='xz'))
    ink2 = mat('io_print', '#aeb4bc', 0.0, 0.6)
    R = Matrix(((0, -1, 0, 0), (0, 0, 1, 0), (-1, 0, 0, 0), (0, 0, 0, 1)))   # chữ: chạy theo -Z, đầu chữ hướng -X, mặt +Y
    for t, z, x, size in (('RESET', -68.0, 202.0, 1.6), ('USB', -32.0, 202.0, 2.0), ('USB', 32.0, 202.0, 2.0),
                          ('HEADSET', 68.0, 202.0, 1.5), ('HDD', 0.0, 207.5, 1.5)):
        o = label(t, 0, 0, 0, size, ink2)
        o.data.transform(R)
        o.data.transform(Matrix.Translation((x, yt + 0.02, z)))

    # ---- Mặt trước kính (tờ 12_case_front): khung phẳng, kính trước 2 mm trên giá mang tháo được,
    # hai dải hút đục lỗ ở hai mép, lọc bụi phía sau, dải nhấn cyan. Giá 3 quạt 120 giữ nguyên.
    glass = mat('front_glass', '#11161b', 0.0, 0.05, alpha=0.42)   # kính khói tối, nhìn xuyên thấy quạt sáng
    frit = mat('glass_frit', '#101216', 0.0, 0.5)
    filt = mat('dust_filter', '#202329', 0.0, 0.9)
    gasket = mat('gasket', '#202329', 0.0, 0.85)
    for sz in (-1, 1):   # hai trụ đứng ngoài và hai thanh ngang
        for za, zb2, xe in ((101.0, 103.0, xf), (103.0, 106.0, 224.8), (106.0, 110.0, xf)):
            if sz < 0 and za == 103.0:
                xe = xf      # dải nhấn chỉ ở phía kính hông
            box((xf - T, -232.0, min(sz * za, sz * zb2)), (xe, 232.0, max(sz * za, sz * zb2)), steel)
    box((xf - T, yb, zb), (xf, -228.0, zg), steel)
    box((xf - T, 228.0, zb), (xf, yt, zg), steel)
    # kính 158 (Z) × 456 (Y) × 2 (X), viền men đen 5 mm ở mặt trong
    box((222.9, -228.0, -79.0), (224.9, 228.0, 79.0), glass, bevel=0.2, seg=1)
    for lo, hi in (((222.85, -228.0, -79.0), (222.9, 228.0, -74.0)), ((222.85, -228.0, 74.0), (222.9, 228.0, 79.0)),
                   ((222.85, -228.0, -74.0), (222.9, -223.0, 74.0)), ((222.85, 223.0, -74.0), (222.9, 228.0, 74.0))):
        box(lo, hi, frit)
    # giá mang kính: hai thanh đứng |Z| 77…81 và hai thanh ngang ngoài vùng quạt, gioăng EPDM
    for sz in (-1, 1):
        box((219.9, -228.0, min(sz * 77.0, sz * 81.0)), (222.9, 228.0, max(sz * 77.0, sz * 81.0)), frit)
        box((222.5, -226.0, min(sz * 78.0, sz * 80.0)), (222.9, 226.0, max(sz * 78.0, sz * 80.0)), gasket)
    for y0, y1 in ((-228.0, -225.0), (225.0, 228.0)):
        box((222.1, y0, -81.0), (222.9, y1, 81.0), frit)
        box((222.5, y0, -78.0), (222.9, y1, 78.0), gasket)          # gioăng liên tục trên / dưới
    # 4 kẹp chữ U (ẩn sau viền men) + 4 vít M2 khoá kẹp, trục Z
    for sy in (-1, 1):
        for sz in (-1, 1):
            y0, y1 = sy * 210.0 - 6.0, sy * 210.0 + 6.0
            zi, zo = sz * 76.0, sz * 79.6        # ôm mép kính 3 mm (|Z| 76…79) + lưng kẹp ngoài mép
            box((222.4, y0, min(zi, zo)), (222.9, y1, max(zi, zo)), frit)                       # má sau
            box((224.9, y0, min(zi, zo)), (225.0, y1, max(zi, zo)), frit)                       # má trước
            box((222.4, y0, min(sz * 79.0, zo)), (225.0, y1, max(sz * 79.0, zo)), frit)         # lưng chữ U
            cyl((221.0, sy * 210.0, sz * 80.6), 1.6, 0.8, 'z', mat('screw_black', '#353a42', 0.8, 0.35), 12)
    # 2 chốt định vị đáy + 2 lẫy lò xo đỉnh
    for sz in (-1, 1):
        cyl((221.0, -229.5, sz * 77.0), 1.0, 3.0, 'y', screw_m, 12)
        box((221.0, 216.0, sz * 79.0 - 2.5), (221.4, 224.0, sz * 79.0 + 2.5), mat('latch', '#2a2e35', 0.4, 0.5))
        cyl((221.0, -229.5, sz * 77.0), 2.0, 2.0, 'y', gasket, 16)            # ổ nhận chốt có cao su
    # hai dải hút: 4 cột × 107 hàng lỗ Ø2,6 bước 4, lỗ khoét thật; lọc bụi phía sau có nam châm
    for sz in (-1, 1):
        z0, z1 = min(sz * 82.0, sz * 101.0), max(sz * 82.0, sz * 101.0)
        strip = box((223.5, -225.0, z0), (224.2, 225.0, z1), meshm)
        holes = [cyl((223.85, -212.0 + 4.0 * j, sz * zc), 1.3, 2.0, 'x', meshm, 12)
                 for j in range(107) for zc in (85.5, 89.5, 93.5, 97.5)]
        boolean(strip, join(holes, 'vent_holes'))
        box((222.0, -216.0, z0), (223.5, -214.0, z1), meshm)      # tai đầu dải
        box((222.0, 214.0, z0), (223.5, 216.0, z1), meshm)
        for sy in (-1, 1):
            cyl((224.55, sy * 222.0, sz * 91.5), 1.6, 0.7, 'x', mat('screw_black', '#353a42', 0.8, 0.35), 12)
        # lọc bụi: khung viền + vùng lưới mịn nhìn xuyên được + tai kéo
        mesh_m = mat('filter_mesh', '#202329', 0.0, 0.9, alpha=0.55)
        zi0, zi1 = z0 + 2.0, z1 - 2.0
        box((221.0, -216.0, z0), (221.8, 216.0, zi0), filt)
        box((221.0, -216.0, zi1), (221.8, 216.0, z1), filt)
        box((221.0, -216.0, zi0), (221.8, -212.0, zi1), filt)
        box((221.0, 212.0, zi0), (221.8, 216.0, zi1), filt)
        box((221.3, -212.0, zi0), (221.5, 212.0, zi1), mesh_m)
        box((221.8, -214.0, sz * 91.5 - 3.0), (222.6, -210.0, sz * 91.5 + 3.0), filt)   # tai kéo
        for sy in (-1, 1):
            for zc in (84.0, 99.0):
                box((221.0, sy * 214.0 - 2.0, sz * zc - 1.5), (221.8, sy * 214.0 + 2.0, sz * zc + 1.5), mat('magnet', '#5d636c', 0.8, 0.4))
    # dải nhấn cyan trên trụ phía kính
    box((224.8, -195.0, 103.0), (225.0, 195.0, 106.0), cyan)     # nằm trong hốc trụ, mặt ngoài đúng x 225
    bracket = box((xf - 4.0, -214.0, -76.0), (xf - 3.0, 216.0, 76.0), steel)
    holes = [cyl((xf - 3.5, y0, 0), 67.0, 3.0, 'x', steel, 64) for y0 in FRONT_FANS_Y]
    holes += [cyl((xf - 3.5, y0 + dy, dz), 2.25, 3.0, 'x', steel, 12) for y0 in FRONT_FANS_Y for dy in (-FRONT_HP, FRONT_HP) for dz in (-FRONT_HP, FRONT_HP)]
    boolean(bracket, join(holes, 'bracket_cut'))
    for y0 in FRONT_FANS_Y:   # 12 vít quạt S5, bắt từ phía trước vào giá
        for dz in (-FRONT_HP, FRONT_HP):
            for dy in (-FRONT_HP, FRONT_HP):
                phillips((xf - 2.96, y0 + dy, dz), 0, 2.6, screw_m, hole, 'x', h=0.8, sign=1)   # đầu thấp, đỉnh ≤ x 222,5

    # ---- Lưng (design/rear/gaming.png): hốc I/O, ô quạt sau lưới lục giác (thấy vòng ARGB), 7 khe PCIe:
    # 2 khe cho lá cổng GPU + 5 nắp có lưới, thanh giữ dọc có vít, dải lưới thoát cạnh khe, cửa nguồn lộ trọn mặt sau
    rmesh = perforated_material('rear_mesh', '#101216', 3.0, 4.2)
    back = box((xr, yb + T, zb), (xr + T, yt - T, zg), steel)
    bx, by, bz = BOARD_O
    fy, fz = REAR_FAN
    pc0, pc1 = PCIE_TOP - 7 * PCIE_PITCH, PCIE_TOP
    iy0, iy1 = by - 38.5 + IO_DY, by + 120.5 + IO_DY
    iy, iz, ih, iw = (iy0 + iy1) / 2, -76.0, iy1 - iy0, 38.0
    cuts = [cutter([((xr - 1, iy0, iz - iw / 2), (xr + T + 1, iy1, iz + iw / 2))], steel),                       # cửa lá chắn I/O
            cutter([((xr - 1, pc0, -84.0), (xr + T + 1, pc1, 37.0))], steel),                                   # dãy PCIe
            prism(rounded_rect(82.0, 146.0, 3.0, 4, cx=-189.0, cy=-26.0), xr - 1, xr + T + 1, steel, plane='yz', name='cut'),   # mặt sau nguồn
            cyl((xr + T / 2, fy, fz), 58.0, T + 2, 'x', steel, 64, name='cut')]                                  # ô quạt Ø116
    vent = rounded_rect(130.0, 26.0, 3.0, 4, cx=(pc0 + pc1) / 2, cy=68.0)                                     # dải lưới cạnh khe
    cuts.append(prism(vent, xr - 1, xr + T + 1, steel, plane='yz', name='cut'))
    for c in cuts:   # khoét từng lỗ một: gộp các vật cắt chạm nhau làm phép khoét hỏng
        boolean(back, c)
    assert len(back.data.polygons) > 20, 'tấm lưng bị khoét hỏng'
    # hốc dập quanh I/O (lùi 0,8) và quanh ô quạt (vuông 128, lùi 2) — gờ nổi nhìn rõ từ sau
    for (cy_, cz_, hh, ww, depth) in ((iy, iz, ih + 12.0, iw + 12.0, 0.8), (fy, fz, 128.0, 128.0, 2.0)):
        rim = prism(rounded_rect(hh, ww, 4.0, 4, cx=cy_, cy=cz_), xr - depth, xr, steel, plane='yz', name='rear_rim')
        boolean(rim, prism(rounded_rect(hh - 6.0, ww - 6.0, 2.0, 4, cx=cy_, cy=cz_), xr - depth - 1, xr + 1, steel, plane='yz', name='cut'))
    disc = [(fy + 59.0 * math.cos(2 * math.pi * k / 64), fz + 59.0 * math.sin(2 * math.pi * k / 64)) for k in range(64)]
    perforated_plate(disc, xr + 0.6, xr + 1.4, rmesh, 'fan_mesh', plane='yz')
    perforated_plate(vent, xr + T - 0.8, xr + T, rmesh, 'vent_mesh', plane='yz')
    for dy in (-52.5, 52.5):
        for dz in (-52.5, 52.5):
            phillips((xr - 1.0, fy + dy, fz + dz), 0, 2.6, screw_m, hole, 'x', h=2.0, sign=-1)
    for k in range(2, 7):   # 5 nắp PCIe (khe 1–2 đã tháo cho GPU): trường lưới, 1 vít mỗi nắp
        y1 = PCIE_TOP - k * PCIE_PITCH
        y0 = y1 - PCIE_PITCH + 1.5
        c = box((xr + T, y0, -83.0), (xr + T + 1.0, y1, 36.0), steel)
        cy_, cz_ = (y0 + y1) / 2, -25.0
        boolean(c, prism(rounded_rect(10.0, 96.0, 2.0, 3, cx=cy_, cy=cz_), xr + T - 1, xr + T + 2, steel, plane='yz', name='cut'))
        perforated_plate(rounded_rect(10.6, 96.6, 2.0, 3, cx=cy_, cy=cz_), xr + T + 0.3, xr + T + 0.7, rmesh, 'cover_mesh', plane='yz')
    # thanh giữ dọc 12 mm ở ngoài lưng, 7 vít đúng tai các khe (cả 2 tai lá GPU)
    box((xr - 2.0, pc0 - 4.0, 37.0), (xr, pc1 + 4.0, 49.0), steel, bevel=0.6)
    for k in range(7):
        phillips((xr - 3.0, PCIE_TOP - (k + 0.5) * PCIE_PITCH, 43.0), 0, 2.6, screw_m, hole, 'x', h=2.0, sign=-1)
    box((xr + T, pc0 - 4.0, 37.0), (xr + T + 3.0, pc1 + 4.0, 42.0), steel)   # gờ đỡ tai phía trong

    # ---- Nắp kín phía sau khay (z -110) + 2 vít tay ở mép sau
    panel = [box((-215.0, -222.5, zb), (215.0, 222.5, zb + 1.0), steel)]
    for y in (222.5 - 20.0, -222.5 + 20.0):   # 2 vít tay giữ liền nắp, ở mép sau
        panel.append(cyl((-215.0 + 6.0, y, zb - 2.0), 4.5, 4.0, 'z', mat('thumb', '#2a2e35', 0.6, 0.35), 24))
    for lo, hi in (((xr, yb, zb), (-215.0, yt, zb + 8.0)), ((215.0, yb, zb), (xf, yt, zb + 8.0)),
                   ((xr, yb, zb), (xf, -222.5, zb + 8.0)), ((xr, 222.5, zb), (xf, yt, zb + 8.0))):
        box(lo, hi, steel)
    # ---- Khay bo (z -102,5): cửa CPU 100 × 100, 6 lỗ luồn dây có viền cao su, 9 cọc đỡ M3 cao 6,5
    tray = box((xr + T, yb + T, TRAY_Z - 1.0), (xf - 25.0, yt - T, TRAY_Z), inner)
    tray_cuts = [((-69.0 - 50.0, 121.5 - 50.0, TRAY_Z - 2), (-69.0 + 50.0, 121.5 + 50.0, TRAY_Z + 1))]
    for (cx, cy), (w, h) in GROMMETS.values():
        tray_cuts.append(((cx - w / 2, cy - h / 2, TRAY_Z - 2), (cx + w / 2, cy + h / 2, TRAY_Z + 1)))
    boolean(tray, cutter(tray_cuts, inner))
    for name, ((cx, cy), (w, h)) in GROMMETS.items():
        grommet(cx, cy, w, h, TRAY_Z - 0.5, rubber, dark)
    for x in (-200.0, -88.0, 24.0):
        for y in (-80.0, 62.5, 205.0):
            cyl((x, y, TRAY_Z + 3.25), 2.75, 6.5, 'z', brass, 6)
            cyl((x, y, TRAY_Z + 6.51), 1.2, 0.02, 'z', hole, 12)
    # rãnh đi dây + 6 đai dán ở mặt sau khay
    strap = mat('strap', '#2a2e35', 0.0, 0.8)
    for x, y in ((-210.0, 150.0), (-210.0, -10.0), (150.0, 150.0), (150.0, 0.0), (-120.0, -110.0), (60.0, -110.0)):
        box((x - 12.0 if abs(x) < 200 else x - 4, y - 4.0, -108.8), (x + 12.0 if abs(x) < 200 else x + 4, y + 4.0, -107.8), strap)

    # ---- Nắp che nguồn: mặt trên y -120 tới x 170, mặt trước, vách phía kính; ống che G4
    shroud_top = box((xr + T, SHROUD_Y - 2.0, TRAY_Z), (SHROUD_X, SHROUD_Y, zg - 4.0), steel)
    boolean(shroud_top, cutter([((G4_DUCT[0] + 2.0, SHROUD_Y - 3.0, G4_DUCT[2] + 2.0), (G4_DUCT[1] - 2.0, SHROUD_Y + 1.0, G4_DUCT[3] - 2.0))], steel))
    box((SHROUD_X - 2.0, yb + T, TRAY_Z), (SHROUD_X, SHROUD_Y, zg - 4.0), steel)
    shroud_side = box((xr + T, yb + T, zg - 6.0), (SHROUD_X, SHROUD_Y, zg - 4.0), steel)
    boolean(shroud_side, cutter([((60.0 + k * 12.0, -200.0, zg - 7.0), (66.0 + k * 12.0, -140.0, zg - 3.0)) for k in range(8)], steel))
    x0, x1, z0, z1, my0, my1 = G4_DUCT
    duct = box((x0, SHROUD_Y, z0), (x1, my1 + 2.0, z1), steel, bevel=1.0)
    cut_each(duct, [((x0 + 2.0, SHROUD_Y - 3.0, z0 + 2.0), (x1 - 2.0, my1, z1 - 2.0)),
                    ((x0 - 1.0, my0, z0 + 3.0), (x0 + 3.0, my1, z1 - 3.0))], steel)
    for zz in (z0 + 1.5, z1 - 1.5):
        box((x0 - 1.0, my0 - 1.5, zz - 1.5), (x0 + 0.5, my1 + 1.5, zz + 1.5), rubber)
    label('G4', (x0 + x1) / 2, my1 - 30.0, z1 + 0.02, 6.0, mat('silk', '#d2d7dd', 0.0, 0.7))
    finish(out('case'), groups={'side_panel': panel}, main='part_case')


if __name__ == '__main__':
    for name, fn in PARTS.items():
        if not ONLY or name in ONLY:
            fn()
