# Chạy: blender -b --python tools/inspect_models.py -- <thư mục glb> <thư mục ảnh>
# Nạp từng model, in thông tin và chụp ảnh xem trước để kiểm tra logo.
import bpy, sys, os, math
from mathutils import Vector

src, out = sys.argv[sys.argv.index('--') + 1:][:2]
os.makedirs(out, exist_ok=True)

def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)

def bounds(objs):
    pts = [o.matrix_world @ Vector(c) for o in objs if o.type == 'MESH' for c in o.bound_box]
    lo = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    hi = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    return lo, hi

for name in sorted(f for f in os.listdir(src) if f.endswith('.glb')):
    reset()
    bpy.ops.import_scene.gltf(filepath=os.path.join(src, name))
    meshes = [o for o in bpy.context.scene.objects if o.type == 'MESH']
    faces = sum(len(o.data.polygons) for o in meshes)
    lo, hi = bounds(meshes)
    size = hi - lo
    print(f'\n### {name}: {len(meshes)} mesh, {faces} faces, size {size.x:.3f} x {size.y:.3f} x {size.z:.3f} m')
    for img in bpy.data.images:
        if img.size[0]:
            print(f'   tex {img.name}: {img.size[0]}x{img.size[1]}')
    print('   mats:', ', '.join(m.name for m in bpy.data.materials)[:300])

    scene = bpy.context.scene
    scene.render.engine = 'BLENDER_WORKBENCH'
    scene.display.shading.light = 'STUDIO'
    scene.display.shading.color_type = 'TEXTURE'
    scene.render.resolution_x, scene.render.resolution_y = 900, 600
    world = bpy.data.worlds.new('w'); scene.world = world
    center = (lo + hi) / 2
    radius = size.length / 2
    cam_data = bpy.data.cameras.new('cam'); cam = bpy.data.objects.new('cam', cam_data)
    scene.collection.objects.link(cam); scene.camera = cam
    cam_data.lens = 50
    for i, (az, el) in enumerate([(35, 25), (215, 25), (90, 80)]):
        a, e = math.radians(az), math.radians(el)
        d = radius * 3.2
        cam.location = center + Vector((d * math.cos(e) * math.cos(a), d * math.cos(e) * math.sin(a), d * math.sin(e)))
        cam.rotation_euler = (center - cam.location).to_track_quat('-Z', 'Y').to_euler()
        scene.render.filepath = os.path.join(out, f'{name[:-4]}_{i}.png')
        bpy.ops.render.render(write_still=True)
