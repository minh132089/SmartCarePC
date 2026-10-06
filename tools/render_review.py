# Chụp model đã dựng ở nhiều góc (EEVEE, nền sáng) để Codex soát lỗi.
# Chạy: blender -b --python tools/render_review.py -- <file.glb> <tiền tố ảnh ra> [zoom]
# Các góc tính theo trục app: +z là phía người xem (mặt kính), +y là hướng lên.
import bpy, sys, math
from mathutils import Vector

args = sys.argv[sys.argv.index('--') + 1:]
f, out = args[:2]
zoom = float(args[2]) if len(args) > 2 else 1.0
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=f)
sc = bpy.context.scene
sc.render.engine = 'BLENDER_EEVEE'
sc.render.resolution_x, sc.render.resolution_y = 1100, 800
sc.view_settings.view_transform = 'Standard'
sc.world = bpy.data.worlds.new('w')
sc.world.use_nodes = True
bg = sc.world.node_tree.nodes['Background']
bg.inputs['Color'].default_value = (0.92, 0.93, 0.95, 1)
bg.inputs['Strength'].default_value = 0.9


def light(name, loc, energy, size):
    l = bpy.data.lights.new(name, 'AREA')
    l.energy, l.size = energy, size
    o = bpy.data.objects.new(name, l)
    sc.collection.objects.link(o)
    o.location = loc
    o.rotation_euler = (Vector((0, 0, 0)) - Vector(loc)).to_track_quat('-Z', 'Y').to_euler()


pts = [o.matrix_world @ Vector(c) for o in sc.objects if o.type == 'MESH' for c in o.bound_box]
lo = Vector([min(p[i] for p in pts) for i in range(3)])
hi = Vector([max(p[i] for p in pts) for i in range(3)])
c = (lo + hi) / 2
r = (hi - lo).length
light('key', c + Vector((-1.2, -1.5, 2.0)) * r, 260 * r * r, r)
light('fill', c + Vector((1.6, -0.8, 0.6)) * r, 110 * r * r, r)
light('rim', c + Vector((0.3, 1.6, 1.2)) * r, 140 * r * r, r)

cam = bpy.data.objects.new('cam', bpy.data.cameras.new('cam'))
sc.collection.objects.link(cam)
sc.camera = cam
cam.data.lens = 60


def app(x, y, z):  # hướng theo trục app -> Blender
    return Vector((x, -z, y)).normalized()


views = {
    'front': app(0, 0, 1),
    'iso': app(-0.55, 0.55, 0.65),
    'iso2': app(0.6, 0.45, 0.65),
    'back': app(0.35, -0.35, -0.85),
    'rear': app(-1, 0.12, 0.25),
}
# Tuỳ chọn: chỉ chụp một vùng (toạ độ app, mm): ... -- <glb> <out> <zoom> <x> <y> <z>
if len(args) >= 6:
    c = Vector((float(args[3]) / 100, -float(args[5]) / 100, float(args[4]) / 100))
for name, d in views.items():
    cam.location = c + d * r * 2.0 / zoom
    up = 'Y'
    cam.rotation_euler = (c - cam.location).to_track_quat('-Z', up).to_euler()
    sc.render.filepath = f'{out}_{name}.png'
    bpy.ops.render.render(write_still=True)
print('SIZE(app mm)', ' '.join(f'{v * 100:.1f}' for v in (hi.x - lo.x, hi.z - lo.z, hi.y - lo.y)))
