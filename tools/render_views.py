# Chụp một GLB từ 7 góc (FRONT +X, BACK -X, LEFT +Z, RIGHT -Z, TOP +Y, BOTTOM -Y theo trục app, và 3/4)
# rồi ghép thành một tờ 4 × 2 để soát lỗi. Chạy: blender -b --python tools/render_views.py -- <file.glb> <ảnh ra.png>
import bpy, sys, os
import numpy as np
from mathutils import Vector, Matrix

f, out = sys.argv[sys.argv.index('--') + 1:][:2]
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=f)
sc = bpy.context.scene
sc.render.engine = 'BLENDER_EEVEE'
TW, TH = 640, 480
sc.render.resolution_x, sc.render.resolution_y = TW, TH
sc.view_settings.view_transform = 'Standard'
sc.world = bpy.data.worlds.new('w')
sc.world.use_nodes = True
bg = sc.world.node_tree.nodes['Background']
bg.inputs['Color'].default_value = (0.93, 0.94, 0.96, 1)
bg.inputs['Strength'].default_value = 1.0
pts = [o.matrix_world @ Vector(c) for o in sc.objects if o.type == 'MESH' for c in o.bound_box]
lo = Vector([min(p[i] for p in pts) for i in range(3)])
hi = Vector([max(p[i] for p in pts) for i in range(3)])
c = (lo + hi) / 2
r = (hi - lo).length
for name, loc, e in (('key', (-1.2, -1.5, 2.0), 260), ('fill', (1.6, -0.8, 0.6), 120), ('rim', (0.3, 1.6, 1.2), 140), ('under', (0.2, 0.3, -1.6), 90)):
    l = bpy.data.lights.new(name, 'AREA')
    l.energy, l.size = e * r * r, r
    o = bpy.data.objects.new(name, l)
    sc.collection.objects.link(o)
    o.location = c + Vector(loc) * r
    o.rotation_euler = (c - o.location).to_track_quat('-Z', 'Y').to_euler()
cam = bpy.data.objects.new('cam', bpy.data.cameras.new('cam'))
sc.collection.objects.link(cam)
sc.camera = cam


def app(x, y, z):   # hướng theo trục app -> Blender
    return Vector((x, -z, y)).normalized()


def look(forward, up_img):
    """Ma trận quay camera: nhìn theo `forward`, hướng lên của ảnh là `up_img` (toạ độ Blender)."""
    f = forward.normalized()
    back = -f
    up = (up_img - back * up_img.dot(back)).normalized()
    right = up.cross(back)
    return Matrix((right, up, back)).transposed().to_euler()


UPW = Vector((0, 0, 1))   # trục đứng của app (y) trong Blender
# (tên, hướng camera nhìn từ tâm ra, hướng lên của ảnh)
views = [('FRONT', app(1, 0, 0), UPW), ('BACK', app(-1, 0, 0), UPW), ('LEFT_open', app(0, 0, 1), UPW), ('RIGHT_closed', app(0, 0, -1), UPW),
         ('TOP', app(0, 1, 0), Vector((-1, 0, 0))), ('BOTTOM', app(0, -1, 0), Vector((1, 0, 0))),
         ('ISO', app(0.6, 0.45, 0.65), UPW), ('ISO_back', app(-0.6, 0.45, -0.65), UPW)]
tiles = []
tmp = os.path.splitext(out)[0] + '_tmp.png'
for name, d, up in views:
    ortho = not name.startswith('ISO')
    cam.data.type = 'ORTHO' if ortho else 'PERSP'
    cam.data.ortho_scale = r * 1.02
    cam.data.lens = 50
    cam.location = c + d * r * 2.2
    cam.rotation_euler = look(c - cam.location, up)
    sc.render.filepath = tmp
    bpy.ops.render.render(write_still=True)
    img = bpy.data.images.load(tmp)
    px = np.array(img.pixels[:], dtype=np.float32).reshape(TH, TW, 4)
    tiles.append(px)
    bpy.data.images.remove(img)
grid = np.ones((TH * 2 + 6, TW * 4 + 18, 4), np.float32)
for i, t in enumerate(tiles):
    rr, cc = divmod(i, 4)
    y0 = (1 - rr) * (TH + 6)
    x0 = cc * (TW + 6)
    grid[y0:y0 + TH, x0:x0 + TW] = t
sheet = bpy.data.images.new('sheet', grid.shape[1], grid.shape[0], alpha=True)
sheet.pixels.foreach_set(grid.ravel())
sheet.filepath_raw = out
sheet.file_format = 'PNG'
sheet.save()
os.remove(tmp)
print('VIEWS', out, 'SIZE(app mm)', ' '.join(f'{v * 100:.1f}' for v in (hi.x - lo.x, hi.z - lo.z, hi.y - lo.y)))
