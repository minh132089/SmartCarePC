"""
Đo vị trí chi tiết thật trên model đã xử lý, để đặt chấm chú thích ở chế độ "Xem cận cảnh".
Chạy: blender -b --python tools/find_features.py -- <file.glb>
In ra toạ độ theo trục của app (x, y, z) = (x, z, -y) của Blender.
"""
import bpy, sys
f = sys.argv[sys.argv.index('--') + 1]
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=f)
vs = [o.matrix_world @ v.co for o in bpy.context.scene.objects if o.type == 'MESH' for v in o.data.vertices]
app = [(v.x, v.z, -v.y) for v in vs]
lo = [min(p[i] for p in app) for i in range(3)]
hi = [max(p[i] for p in app) for i in range(3)]
print('APP_BBOX', [round(x, 3) for x in lo], [round(x, 3) for x in hi])

# Dải chân cắm: các đỉnh sát mép cắm (z nhỏ nhất). Tìm khoảng hở dài nhất theo trục dài để ra khấc khoá.
axis = 1 if (hi[1] - lo[1]) > (hi[0] - lo[0]) else 0
edge = sorted(p[axis] for p in app if p[2] < lo[2] + 0.012)
gaps = [(edge[i + 1] - edge[i], (edge[i + 1] + edge[i]) / 2) for i in range(len(edge) - 1)]
mid = [g for g in gaps if lo[axis] + 0.15 < g[1] < hi[axis] - 0.15]
if mid:
    g = max(mid)
    print(f'KEY_NOTCH axis={"xy"[axis]} at={g[1]:.3f} width={g[0]:.3f} (khoảng hở dài nhất trên mép cắm)')
