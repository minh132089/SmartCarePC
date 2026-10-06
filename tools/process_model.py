"""
Chuẩn hoá một model tải về cho PC SmartCare.
Chạy: blender -b --python tools/process_model.py -- <cấu hình.json> <tên model>

Việc script làm:
  1. Xoá các mesh không dùng hoặc có logo hãng.
  2. Tô đè vùng logo in sẵn trong texture.
  3. Xoay, đưa gốc về tâm và đổi đơn vị (1 = 100 mm, giống app).
  4. Thu nhỏ texture cho nhẹ rồi xuất .glb.
"""
import bpy, sys, os, json, math
import numpy as np
from mathutils import Vector

cfg_path, key = sys.argv[sys.argv.index('--') + 1:][:2]
cfg = json.load(open(cfg_path))[key]
root = os.path.dirname(os.path.abspath(cfg_path))

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=os.path.join(root, cfg['src']))

# 1. Xoá mesh theo tên hoặc theo tên vật liệu
keep_names = set(cfg.get('keep_objects', []))
drop_names = set(cfg.get('drop_objects', []))
drop_mats = set(cfg.get('drop_materials', []))
for o in list(bpy.context.scene.objects):
    if o.type != 'MESH':
        continue
    mats = {m.name for m in o.data.materials if m}
    if o.name in drop_names or mats & drop_mats or (keep_names and o.name not in keep_names):
        bpy.data.objects.remove(o, do_unlink=True)

# 2. Tô đè vùng logo. Toạ độ tính theo phần của ảnh (0..1), gốc ở góc trên bên trái.
def paint(img, box, color):
    w, h = img.size
    px = np.array(img.pixels[:], dtype=np.float32).reshape(h, w, img.channels)
    x0, y0, x1, y1 = box
    # pixels của Blender tính từ dưới lên
    r0, r1 = int((1 - y1) * h), int((1 - y0) * h)
    c0, c1 = int(x0 * w), int(x1 * w)
    if isinstance(color, list) and color and color[0] == 'sample':
        sx, sy = color[1], color[2]
        color = px[int((1 - sy) * h), int(sx * w)].copy()
    elif color == 'auto':
        ring = np.concatenate([px[r0 - 3:r0, c0:c1].reshape(-1, img.channels), px[r1:r1 + 3, c0:c1].reshape(-1, img.channels)])
        color = np.median(ring, axis=0)
    px[r0:r1, c0:c1] = color
    img.pixels[:] = px.ravel()

for p in cfg.get('paint', []):
    for img in bpy.data.images:
        if img.name in p['images']:
            paint(img, p['box'], p['colors'].get(img.name, 'auto'))

# 2b. Gán vật liệu mới cho từng mesh (model tải về chưa có màu)
def srgb(h):
    h = h.lstrip('#')
    c = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    return tuple(x / 12.92 if x <= 0.04045 else ((x + 0.055) / 1.055) ** 2.4 for x in c) + (1.0,)

for obj_name, spec in cfg.get('paint_objects', {}).items():
    o = bpy.data.objects.get(obj_name)
    if not o:
        continue
    m = bpy.data.materials.new(obj_name + '_mat')
    m.use_nodes = True
    bsdf = m.node_tree.nodes['Principled BSDF']
    bsdf.inputs['Base Color'].default_value = srgb(spec['color'])
    bsdf.inputs['Metallic'].default_value = spec.get('metal', 0.2)
    bsdf.inputs['Roughness'].default_value = spec.get('rough', 0.5)
    o.data.materials.clear()
    o.data.materials.append(m)

# 2c. Giảm số mặt cho model quá chi tiết (ví dụ quạt xuất hiện nhiều lần trong cảnh)
if cfg.get('decimate'):
    for o in [o for o in bpy.context.scene.objects if o.type == 'MESH']:
        mod = o.modifiers.new('dec', 'DECIMATE'); mod.ratio = cfg['decimate']
        bpy.context.view_layer.objects.active = o
        bpy.ops.object.modifier_apply(modifier=mod.name)

# 3. Gom các mesh còn lại làm con của một gốc, xoay và canh giữa
meshes = [o for o in bpy.context.scene.objects if o.type == 'MESH']
for o in bpy.context.scene.objects:
    o.select_set(o in meshes)
bpy.context.view_layer.objects.active = meshes[0]
bpy.ops.object.parent_clear(type='CLEAR_KEEP_TRANSFORM')
for o in list(bpy.context.scene.objects):
    if o.type != 'MESH':
        bpy.data.objects.remove(o, do_unlink=True)
bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)

# Xoay lần lượt theo từng bước, ví dụ [["Z", 90], ["Y", 90]]
from mathutils import Matrix
for axis, deg in cfg.get('rotate_steps', []):
    rot = Matrix.Rotation(math.radians(deg), 4, axis)
    for o in meshes:
        o.data.transform(rot)

if cfg.get('mirror_x'):
    # Model bị lật gương so với linh kiện thật: lật lại trục X rồi đảo mặt cho đúng chiều sáng
    import bmesh
    flip = Matrix.Diagonal((-1, 1, 1, 1))
    for o in meshes:
        o.data.transform(flip)
        bm = bmesh.new(); bm.from_mesh(o.data)
        bmesh.ops.reverse_faces(bm, faces=bm.faces)
        bm.to_mesh(o.data); bm.free()

def bounds(objs):
    pts = [v.co for o in objs for v in o.data.vertices]
    lo = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    hi = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    return lo, hi

ref = [o for o in meshes if o.name in cfg.get('center_on', [])] or meshes
lo, hi = bounds(ref)
center = (lo + hi) / 2
if cfg.get('origin') == 'back_face':
    # Gốc nằm ở mặt sau (mặt áp vào bo mạch / vách case), theo trục -Y của Blender
    center.y = hi.y
elif cfg.get('origin') == 'top_face':
    # Gốc nằm ở mặt trên: dùng cho két tản nước treo dưới nóc case
    center.z = hi.z
scale = cfg.get('scale', 10.0)
if 'fit' in cfg:
    # Co giãn để cạnh theo trục chỉ định đúng bằng kích thước thật (đơn vị app)
    axis, length = cfg['fit']
    scale = length / (hi - lo)['xyz'.index(axis)]
for o in meshes:
    for v in o.data.vertices:
        v.co = (v.co - center) * scale

# Cắt bỏ phần dưới một mặt phẳng ngang (dùng để suy ra bo Micro-ATX từ bo ATX)
if 'cut_below_z' in cfg:
    import bmesh
    zc = cfg['cut_below_z']
    for o in meshes:
        bm = bmesh.new(); bm.from_mesh(o.data)
        bmesh.ops.bisect_plane(bm, geom=bm.verts[:] + bm.edges[:] + bm.faces[:], plane_co=(0, 0, zc), plane_no=(0, 0, 1), clear_inner=True)
        bm.to_mesh(o.data); bm.free()
    if cfg.get('recenter_after_cut'):
        lo2, hi2 = bounds([o for o in meshes if o.name in cfg.get('center_on', [])] or meshes)
        dz = (lo2.z + hi2.z) / 2
        for o in meshes:
            for v in o.data.vertices:
                v.co.z -= dz
        print(f'RECENTER_DZ {dz:.3f}')

separate = cfg.get('separate', {})
rest = [o for o in meshes if o.name not in separate]
for src_name, new_name in separate.items():
    o = bpy.data.objects.get(src_name)
    if o:
        o.name = o.data.name = new_name
bpy.ops.object.select_all(action='DESELECT')
for o in rest:
    o.select_set(True)
bpy.context.view_layer.objects.active = rest[0]
if len(rest) > 1:
    bpy.ops.object.join()
obj = bpy.context.view_layer.objects.active
obj.name = cfg['name']
obj.data.name = cfg['name']
# Các mesh tách riêng làm con của mesh chính để xuất thành node riêng.
# Gốc của mỗi mesh tách riêng đặt vào tâm của nó, để app xoay (ví dụ cánh quạt) quanh đúng trục.
from mathutils import Matrix as _M
for new_name in separate.values():
    child = bpy.data.objects.get(new_name)
    if child:
        c_lo, c_hi = bounds([child])
        c = (c_lo + c_hi) / 2
        child.data.transform(_M.Translation(-c))
        child.location = c
        child.parent = obj

# 4. Thu nhỏ texture
max_tex = cfg.get('max_texture', 1024)
for img in bpy.data.images:
    if img.size[0] > max_tex:
        img.scale(max_tex, max(1, int(img.size[1] * max_tex / img.size[0])))

lo, hi = bounds([o for o in bpy.context.scene.objects if o.type == 'MESH'])
size = hi - lo
print(f"RESULT {cfg['name']}: faces={len(obj.data.polygons)} size(app units, x/y/z blender)={size.x:.2f} {size.y:.2f} {size.z:.2f}")

out = os.path.join(root, cfg['out'])
os.makedirs(os.path.dirname(out), exist_ok=True)
bpy.ops.export_scene.gltf(filepath=out, export_format='GLB', export_image_format='AUTO', export_apply=True)
