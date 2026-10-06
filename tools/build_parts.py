"""
Dựng các linh kiện không tìm được model sạch: CPU, tản nhiệt thấp, tản nhiệt tròn.
Tản nhiệt dùng lại model quạt thật (assets-src/processed/fan.glb).
Chạy: blender -b --python tools/build_parts.py -- <thư mục processed>

Trục: app z (hướng ra mặt kính) = -y của Blender. Đế linh kiện nằm ở y = 0, thân mọc về -y.
"""
import bpy, bmesh, sys, os, math
from mathutils import Vector, Matrix

out = sys.argv[sys.argv.index('--') + 1]


def lin(h):
    h = h.lstrip('#')
    c = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    return tuple(x / 12.92 if x <= 0.04045 else ((x + 0.055) / 1.055) ** 2.4 for x in c) + (1.0,)


def mat(name, color, metal=0.3, rough=0.5):
    m = bpy.data.materials.get(name) or bpy.data.materials.new(name)
    m.use_nodes = True
    p = m.node_tree.nodes['Principled BSDF']
    p.inputs['Base Color'].default_value = lin(color)
    p.inputs['Metallic'].default_value = metal
    p.inputs['Roughness'].default_value = rough
    return m


def box(lo, hi, m, bevel=0.0):
    bm = bmesh.new(); bmesh.ops.create_cube(bm, size=1.0)
    for v in bm.verts:
        v.co = Vector([lo[i] + (v.co[i] + 0.5) * (hi[i] - lo[i]) for i in range(3)])
    me = bpy.data.meshes.new('b'); bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new('b', me); bpy.context.scene.collection.objects.link(o)
    me.materials.append(m)
    if bevel:
        mod = o.modifiers.new('bv', 'BEVEL'); mod.width = bevel; mod.segments = 3
    return o


def cyl(center, r, depth, axis, m, seg=32):
    bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=True, segments=seg, radius1=r, radius2=r, depth=depth)
    if axis == 'Y':
        bmesh.ops.rotate(bm, verts=bm.verts, matrix=Matrix.Rotation(math.radians(90), 3, 'X'))
    elif axis == 'X':
        bmesh.ops.rotate(bm, verts=bm.verts, matrix=Matrix.Rotation(math.radians(90), 3, 'Y'))
    bmesh.ops.translate(bm, verts=bm.verts, vec=Vector(center))
    me = bpy.data.meshes.new('c'); bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new('c', me); bpy.context.scene.collection.objects.link(o)
    me.materials.append(m)
    return o


def fan_on_top(y_center, size):
    """Nhập quạt thật, xoay cho mặt quạt úp xuống (thổi gió về phía bo mạch)."""
    before = set(bpy.data.objects)
    bpy.ops.import_scene.gltf(filepath=os.path.join(out, 'fan.glb'))
    new = [o for o in bpy.data.objects if o not in before]
    rot = Matrix.Rotation(math.radians(90), 4, 'Z')   # pháp tuyến quạt: X → Y
    for o in new:
        if o.type == 'MESH':
            o.data.transform(o.matrix_world)
            o.matrix_world = Matrix.Identity(4)
            o.parent = None
            o.data.transform(Matrix.Scale(size, 4))
            o.data.transform(rot)
            o.data.transform(Matrix.Translation((0, y_center, 0)))
    for o in new:
        if o.type != 'MESH':
            bpy.data.objects.remove(o, do_unlink=True)


def export(name):
    for o in bpy.context.scene.objects:
        if o.modifiers:
            bpy.context.view_layer.objects.active = o
            for m in list(o.modifiers):
                bpy.ops.object.modifier_apply(modifier=m.name)
    bpy.ops.object.select_all(action='SELECT')
    bpy.context.view_layer.objects.active = [o for o in bpy.context.scene.objects if o.type == 'MESH'][0]
    bpy.ops.object.join()
    o = bpy.context.view_layer.objects.active
    o.name = o.data.name = name
    for p in o.data.polygons:
        p.use_smooth = False
    bpy.ops.export_scene.gltf(filepath=os.path.join(out, name.replace('part_', '') + '.glb'), export_format='GLB')
    print(f'RESULT {name}: {len(o.data.polygons)} faces')


def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)


# ---------- CPU: đế xanh, nắp kim loại (IHS) vát cạnh, dấu tam giác vàng ở góc
reset()
sub = mat('substrate', '#1f5a36', 0.1, 0.6)
ihs = mat('ihs', '#c9ced6', 0.95, 0.28)
gold = mat('gold', '#d4a843', 1.0, 0.3)
box((-0.2, -0.012, -0.2), (0.2, 0.0, 0.2), sub)
box((-0.17, -0.042, -0.17), (0.17, -0.012, 0.17), ihs, bevel=0.012)
box((-0.195, -0.014, 0.16), (-0.165, -0.012, 0.19), gold)
export('part_cpu')

# ---------- Tản nhiệt thấp (máy văn phòng): đế đồng, khối lá nhôm, 2 ống đồng, quạt úp
reset()
copper = mat('copper', '#b87333', 0.95, 0.3)
alu = mat('alu', '#aeb4bd', 0.85, 0.32)
box((-0.2, -0.03, -0.2), (0.2, 0.0, 0.2), copper, bevel=0.005)
fins = 17
for i in range(fins):
    x = -0.44 + i * (0.88 / (fins - 1))
    box((x - 0.006, -0.28, -0.44), (x + 0.006, -0.03, 0.44), alu)
box((-0.46, -0.30, -0.46), (0.46, -0.28, 0.46), alu)
for z in (-0.12, 0.12):
    cyl((0, -0.06, z), 0.028, 0.95, 'X', copper, 16)
fan_on_top(-0.30 - 0.1, 0.92)
export('part_cooler_lowprofile')

# ---------- Tản nhiệt tròn (máy học tập): lõi đồng, lá nhôm toả tròn, quạt úp
reset()
copper = mat('copper', '#b87333', 0.95, 0.3)
alu = mat('alu', '#aeb4bd', 0.85, 0.32)
cyl((0, -0.17, 0), 0.13, 0.34, 'Y', copper, 40)
n = 40
for i in range(n):
    a = i / n * math.tau
    o = box((0.12, -0.34, -0.006), (0.46, -0.0, 0.006), alu)
    o.data.transform(Matrix.Rotation(a, 4, 'Y'))
fan_on_top(-0.34 - 0.1, 0.9)
export('part_cooler_stock')
