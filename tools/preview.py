# Chụp ảnh model đã xử lý theo đúng góc nhìn của app.
# Chạy: blender -b --python tools/preview.py -- <file.glb> <ảnh ra, không đuôi>
import bpy, sys, math
from mathutils import Vector
f, out = sys.argv[sys.argv.index('--') + 1:][:2]
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=f)
sc = bpy.context.scene
sc.render.engine = 'BLENDER_WORKBENCH'
sc.display.shading.light = 'STUDIO'
sc.display.shading.color_type = 'TEXTURE'
sc.render.resolution_x, sc.render.resolution_y = 700, 500
sc.world = bpy.data.worlds.new('w')
pts = [o.matrix_world @ Vector(c) for o in sc.objects if o.type == 'MESH' for c in o.bound_box]
lo = Vector([min(p[i] for p in pts) for i in range(3)]); hi = Vector([max(p[i] for p in pts) for i in range(3)])
c = (lo + hi) / 2; r = (hi - lo).length
cam = bpy.data.objects.new('cam', bpy.data.cameras.new('cam')); sc.collection.objects.link(cam); sc.camera = cam
# Góc 0: nhìn thẳng từ phía app (+Z của three = -Y của Blender). Góc 1: chéo 3/4.
for i, d in enumerate([Vector((0, -1, 0)), Vector((0.6, -0.8, 0.45)).normalized(), Vector((0.9, -0.3, 0.25)).normalized()]):
    cam.data.type = 'ORTHO' if i == 0 else 'PERSP'
    cam.data.ortho_scale = r * 1.1
    cam.location = c + d * r * 2.2
    cam.rotation_euler = (c - cam.location).to_track_quat('-Z', 'Y').to_euler()
    sc.render.filepath = f'{out}_{i}.png'
    bpy.ops.render.render(write_still=True)
print('SIZE', ' '.join(f'{v:.2f}' for v in (hi - lo)))
