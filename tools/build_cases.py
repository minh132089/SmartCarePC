"""
Dựng vỏ case cho 4 loại máy bằng code, kích thước lấy thẳng từ src/data/builds.ts.
Chạy: blender -b --python tools/build_cases.py -- <builds.json> <thư mục ra>

Trục Blender → trục app: x → x (trước +), z → y (trên +), -y → z (phía mặt kính +).
Mặt hông phía kính (app +z = Blender -y) để trống: app tự vẽ kính hoặc nắp kim loại trượt được.
"""
import bpy, bmesh, sys, os, json, math
import numpy as np
from mathutils import Vector

args = sys.argv[sys.argv.index('--') + 1:]
builds = json.load(open(args[0]))
out_dir = args[1]
os.makedirs(out_dir, exist_ok=True)

T = 0.03          # độ dày vách, trùng hằng số T trong Case.tsx
STANDOFF = 0.14   # vách phải → mặt sau bo, trùng BOARD_STANDOFF trong src/data/layout.ts
FRONT = 0.06      # độ dày mặt trước
STYLE = {'office': 'solid_groove', 'study': 'solid_lines', 'gaming': 'mesh_rgb', 'workstation': 'mesh'}
BOARD = {'ATX': (2.44, 3.05), 'mATX': (2.44, 2.44), 'ITX': (1.7, 1.7)}
PCIE_COVERS = {'ATX': 7, 'mATX': 4, 'ITX': 2}


def srgb_to_linear(c):
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def hex_color(h):
    h = h.lstrip('#')
    return tuple(srgb_to_linear(int(h[i:i + 2], 16) / 255) for i in (0, 2, 4)) + (1.0,)


def material(name, color, metal=0.4, rough=0.45, emit=None, strength=4.0):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    p = m.node_tree.nodes['Principled BSDF']
    p.inputs['Base Color'].default_value = hex_color(color) if isinstance(color, str) else color
    p.inputs['Metallic'].default_value = metal
    p.inputs['Roughness'].default_value = rough
    if emit:
        p.inputs['Emission Color'].default_value = hex_color(emit)
        p.inputs['Emission Strength'].default_value = strength
    return m


def perforated_material(name, color):
    """Tấm lưới đục lỗ: texture có kênh alpha, lỗ trong suốt để nhìn thấy quạt phía sau."""
    size, pitch, radius = 512, 64, 24
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    row = (yy // pitch).astype(int)
    cx = (xx + (row % 2) * pitch / 2) % pitch - pitch / 2
    cy = yy % pitch - pitch / 2
    hole = (cx ** 2 + cy ** 2) < radius ** 2
    # Kim loại sáng hơn màu vỏ một chút, lỗ tô đen: vẫn đọc ra là tấm đục lỗ kể cả khi trình xem bỏ qua alpha
    base = np.array([c ** (1 / 2.2) for c in hex_color(color)[:3]])
    metal = base * 0.6 + np.array([0.42, 0.44, 0.47]) * 0.4
    px = np.zeros((size, size, 4), np.float32)
    for k in range(3):
        px[..., k] = np.where(hole, 0.02, metal[k])
    px[..., 3] = np.where(hole, 0.0, 1.0)
    img = bpy.data.images.new(name + '_tex', size, size, alpha=True)
    img.pixels[:] = px.ravel()
    img.pack()

    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    p = nt.nodes['Principled BSDF']
    tex = nt.nodes.new('ShaderNodeTexImage'); tex.image = img
    # Nối alpha thẳng (không qua Round) để exporter dùng alphaMode BLEND.
    # Nhìn xa, mipmap trộn lỗ và kim loại thành tấm bán trong suốt, vẫn thấy quạt phía sau như lưới thật.
    # (Chế độ MASK sẽ biến cả tấm thành đặc khi nhìn xa.)
    nt.links.new(tex.outputs['Color'], p.inputs['Base Color'])
    nt.links.new(tex.outputs['Alpha'], p.inputs['Alpha'])
    p.inputs['Metallic'].default_value = 0.6
    p.inputs['Roughness'].default_value = 0.4
    m.use_backface_culling = False
    return m


def box(name, lo, hi, mat=None):
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0)
    for v in bm.verts:
        v.co = Vector([lo[i] + (v.co[i] + 0.5) * (hi[i] - lo[i]) for i in range(3)])
    me = bpy.data.meshes.new(name); bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new(name, me); bpy.context.scene.collection.objects.link(o)
    if mat: me.materials.append(mat)
    return o


def cylinder(name, center, radius, depth, axis='Z', mat=None, verts=40):
    bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=True, segments=verts, radius1=radius, radius2=radius, depth=depth)
    rot = {'Z': None, 'X': ('Y', 90), 'Y': ('X', 90)}[axis]
    if rot:
        from mathutils import Matrix
        bmesh.ops.rotate(bm, verts=bm.verts, matrix=Matrix.Rotation(math.radians(rot[1]), 3, rot[0]))
    bmesh.ops.translate(bm, verts=bm.verts, vec=Vector(center))
    me = bpy.data.meshes.new(name); bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new(name, me); bpy.context.scene.collection.objects.link(o)
    if mat: me.materials.append(mat)
    return o


def plane_yz(name, x, y0, y1, z0, z1, mat, tile=0.7):
    """Mặt phẳng đứng (song song YZ) có UV lặp theo kích thước thật."""
    bm = bmesh.new()
    vs = [bm.verts.new((x, y, z)) for y, z in ((y0, z0), (y1, z0), (y1, z1), (y0, z1))]
    f = bm.faces.new(vs)
    uv = bm.loops.layers.uv.new()
    for loop in f.loops:
        loop[uv].uv = (loop.vert.co.y / tile, loop.vert.co.z / tile)
    me = bpy.data.meshes.new(name); bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new(name, me); bpy.context.scene.collection.objects.link(o)
    me.materials.append(mat)
    return o


def plane_xy(name, z, x0, x1, y0, y1, mat, tile=0.7):
    bm = bmesh.new()
    vs = [bm.verts.new((x, y, z)) for x, y in ((x0, y0), (x1, y0), (x1, y1), (x0, y1))]
    f = bm.faces.new(vs)
    uv = bm.loops.layers.uv.new()
    for loop in f.loops:
        loop[uv].uv = (loop.vert.co.x / tile, loop.vert.co.y / tile)
    me = bpy.data.meshes.new(name); bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new(name, me); bpy.context.scene.collection.objects.link(o)
    me.materials.append(mat)
    return o


def apply_modifiers(o):
    bpy.context.view_layer.objects.active = o
    for m in list(o.modifiers):
        bpy.ops.object.modifier_apply(modifier=m.name)


def bevel(o, width, segments=4, angle=30):
    m = o.modifiers.new('bevel', 'BEVEL')
    m.width, m.segments, m.limit_method = width, segments, 'ANGLE'
    m.angle_limit = math.radians(angle)
    m.harden_normals = True
    apply_modifiers(o)


def cut(o, cutters):
    for c in cutters:
        m = o.modifiers.new('cut', 'BOOLEAN')
        m.operation, m.solver, m.object = 'DIFFERENCE', 'EXACT', c
        apply_modifiers(o)
        bpy.data.objects.remove(c, do_unlink=True)


def smooth(o, angle=35):
    """Chỉ làm mịn chỗ bo cong, giữ cạnh gãy và mặt phẳng sắc nét."""
    bpy.context.view_layer.objects.active = o
    for x in bpy.context.scene.objects:
        x.select_set(x == o)
    bpy.ops.object.shade_smooth_by_angle(angle=math.radians(angle))


def build_case(b):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    D, H, W = b['case']['d'], b['case']['h'], b['case']['w']
    style = STYLE[b['id']]
    body = material('body', b['case']['body'], metal=0.15 if b['id'] == 'office' else 0.55, rough=0.42)
    front_mat = material('front', b['case']['front'], metal=0.15 if b['id'] == 'office' else 0.5, rough=0.5)
    dark = material('dark', '#0c0d10', metal=0.2, rough=0.7)
    trim = material('trim', '#9aa1ab', metal=0.9, rough=0.3)
    perf = perforated_material('mesh', b['case']['front'])

    # --- Thân vỏ: hộp rỗng, bỏ mặt hông phía kính và mặt trước
    shell = box('shell', (-D / 2, -W / 2, -H / 2), (D / 2 - FRONT, W / 2, H / 2), body)
    bm = bmesh.new(); bm.from_mesh(shell.data)
    kill = [f for f in bm.faces if f.normal.y < -0.9 or f.normal.x > 0.9]
    bmesh.ops.delete(bm, geom=kill, context='FACES')
    bm.to_mesh(shell.data); bm.free()
    sol = shell.modifiers.new('solid', 'SOLIDIFY'); sol.thickness = T; sol.offset = -1; sol.use_even_offset = True
    apply_modifiers(shell)
    bevel(shell, 0.035)

    # --- Lỗ ở mặt lưng: I/O, quạt sau, khe PCIe, nguồn
    bw, bh = BOARD[b['ff']]
    board_top = H / 2 - 0.2
    board_center_z = board_top - bh / 2
    board_y = W / 2 - STANDOFF      # mặt sau bo mạch (app z = -W/2 + STANDOFF)
    x_back = (-D / 2 - 0.1, -D / 2 + T + 0.1)
    cutters = []
    io_h = min(1.6, bh * 0.9)
    cutters.append(box('c_io', (x_back[0], board_y - 0.45, board_top - io_h), (x_back[1], board_y - 0.02, board_top), None))
    rf = b['rear']
    fan_z = H / 2 - 0.3 - rf / 2
    # Giống rearFanZ() trong src/data/layout.ts: dời quạt sau tránh nắp che cổng I/O của bo
    io_depth = 0.43 if b['ff'] == 'ITX' else 0.47
    fan_app_z = max(0.0, -W / 2 + STANDOFF + io_depth + rf / 2 + 0.03)
    fan_y = -fan_app_z
    cutters.append(cylinder('c_fan', (-D / 2 + T / 2, fan_y, fan_z), rf * 0.46, 0.3, axis='X'))
    # Khe PCIe: toạ độ giống boardLayout() trong src/data/layout.ts
    if b['ff'] == 'ITX':
        pcie0 = -0.75 if True else None     # bo ITX dùng model thật, khe đo trên model
    else:
        socket_y = bh / 2 - 0.75
        pcie0 = socket_y - 1.25
    slot_top = board_center_z + pcie0 + 0.12
    slots = []
    for i in range(PCIE_COVERS[b['ff']]):
        z1 = slot_top - i * 0.2032
        cutters.append(box(f'c_slot{i}', (x_back[0], -W / 2 + 0.25, z1 - 0.15), (x_back[1], board_y - 0.1, z1), None))
        slots.append(z1)
    psu = (1.4, 0.86, 1.5) if b['psu'] == 'ATX' else (1.0, 0.635, 1.25)
    psu_zc = -H / 2 + T + psu[1] / 2
    psu_yc = W / 2 - T - psu[2] / 2
    cutters.append(box('c_psu', (x_back[0], psu_yc - psu[2] / 2 + 0.04, psu_zc - psu[1] / 2 + 0.04),
                       (x_back[1], psu_yc + psu[2] / 2 - 0.04, psu_zc + psu[1] / 2 - 0.04), None))

    # --- Nóc: lưới (gaming, đồ họa) hoặc rãnh thoát khí (văn phòng, học tập)
    mesh_style = style.startswith('mesh')
    top_x = (-D / 2 + 0.35, D / 2 - FRONT - 0.5)
    top_y = (-W / 2 + 0.25, W / 2 - 0.25)
    if mesh_style:
        cutters.append(box('c_top', (top_x[0], top_y[0], H / 2 - 0.2), (top_x[1], top_y[1], H / 2 + 0.2), None))
    else:
        for i in range(6):
            x0 = -D / 2 + 0.4 + i * 0.16
            cutters.append(box(f'c_vent{i}', (x0, top_y[0] + 0.2, H / 2 - 0.2), (x0 + 0.06, top_y[1] - 0.2, H / 2 + 0.2), None))
    cut(shell, cutters)
    smooth(shell)

    parts = [shell]
    # Nắp khe PCIe, lùi vào trong một chút
    for i, z1 in enumerate(slots):
        parts.append(box(f'slot_cover{i}', (-D / 2 + T, -W / 2 + 0.24, z1 - 0.155), (-D / 2 + T + 0.008, board_y - 0.09, z1 + 0.005), trim))
    # Lưới chắn quạt sau: 4 thanh mảnh
    for k in range(-2, 3):
        parts.append(box(f'grill{k}', (-D / 2, fan_y - rf * 0.46, fan_z + k * rf * 0.15 - 0.006), (-D / 2 + T, fan_y + rf * 0.46, fan_z + k * rf * 0.15 + 0.006), body))
    if mesh_style:
        parts.append(plane_xy('top_mesh', H / 2 - T / 2, top_x[0], top_x[1], top_y[0], top_y[1], perf))

    # --- Mặt trước
    fx0, fx1 = D / 2 - FRONT, D / 2
    if mesh_style:
        frame = box('front', (fx0, -W / 2, -H / 2), (fx1, W / 2, H / 2), front_mat)
        border = 0.14
        cut(frame, [box('c_front', (fx0 - 0.1, -W / 2 + border, -H / 2 + border + 0.1), (fx1 + 0.1, W / 2 - border, H / 2 - border), None)])
        bevel(frame, 0.03)
        smooth(frame)
        parts.append(frame)
        parts.append(plane_yz('front_mesh', fx1 - 0.015, -W / 2 + border, W / 2 - border, -H / 2 + border + 0.1, H / 2 - border, perf))
        if b['accent']:
            glow = material('rgb', b['accent'], metal=0.0, rough=0.4, emit=b['accent'], strength=1.5)
            parts.append(box('rgb_strip', (fx1 - 0.005, -W / 2 + 0.05, -H / 2 + 0.3), (fx1 + 0.008, -W / 2 + 0.08, H / 2 - 0.3), glow))
    else:
        panel = box('front', (fx0, -W / 2, -H / 2), (fx1, W / 2, H / 2), front_mat)
        grooves = []
        if style == 'solid_lines':
            for i in range(3):
                z = H / 2 - 0.9 - i * 0.12
                grooves.append(box(f'g{i}', (fx1 - 0.012, -W / 2 + 0.2, z), (fx1 + 0.1, W / 2 - 0.2, z + 0.03), None))
        else:
            grooves.append(box('g', (fx1 - 0.012, -W / 2 + 0.22, -H / 2 + 0.3), (fx1 + 0.1, -W / 2 + 0.25, H / 2 - 0.3), None))
        cut(panel, grooves)
        bevel(panel, 0.035)
        smooth(panel)
        parts.append(panel)
        # Khe hút gió hai bên mặt trước
        parts.append(box('intake_l', (fx0 - 0.004, -W / 2 + 0.002, -H / 2 + 0.2), (fx0 + 0.004, -W / 2 + 0.03, H / 2 - 0.2), dark))

    # --- Cụm nút trên nóc: nút nguồn, 2 cổng USB, đèn báo
    top = H / 2
    parts.append(cylinder('power_btn', (D / 2 - 0.35, 0.25, top + 0.008), 0.07, 0.02, mat=trim))
    for i in range(2):
        parts.append(box(f'usb{i}', (D / 2 - 0.42, -0.05 - i * 0.2, top - 0.005), (D / 2 - 0.28, -0.05 - i * 0.2 + 0.05, top + 0.004), dark))
    # --- Chân đế cao su
    for sx in (-1, 1):
        for sy in (-1, 1):
            parts.append(cylinder(f'foot{sx}{sy}', (sx * (D / 2 - 0.35), sy * (W / 2 - 0.3), -H / 2 - 0.035), 0.13, 0.07, mat=dark))

    for o in bpy.context.scene.objects:
        o.select_set(True)
    bpy.context.view_layer.objects.active = shell
    bpy.ops.object.join()
    shell.name = shell.data.name = f"case_{b['id']}"
    faces = len(shell.data.polygons)
    path = os.path.join(out_dir, f"case_{b['id']}.glb")
    bpy.ops.export_scene.gltf(filepath=path, export_format='GLB')
    print(f"RESULT case_{b['id']}: {faces} faces, style={style}")


for b in builds:
    build_case(b)
