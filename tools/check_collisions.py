"""
Kiểm tra va chạm giữa các linh kiện máy Gaming khi đã lắp, đặt đúng vị trí như src/data/gaming.ts.
Chạy: blender -b --python tools/check_collisions.py -- assets-src/gaming
In ra từng cặp linh kiện có tam giác cắt nhau. Các cặp lắp khớp (CPU trong socket, RAM trong khe...)
được đánh dấu "khớp" vì chạm nhau là đúng.
"""
import bpy, sys, os
from mathutils import Matrix, Vector
from mathutils.bvhtree import BVHTree

src = sys.argv[sys.argv.index('--') + 1]
B = (-88.0, 62.5, -96.0)
ob = lambda x, y, z: (B[0] + x, B[1] + y, B[2] + z)
PLACE = {
    'case': [(0, 0, 0)], 'motherboard': [B], 'aio_backplate': [ob(19, 59, 0)], 'cpu': [ob(19, 59, 4.7)],
    'ram': [ob(79, 62, 5.5), ob(98, 62, 5.5)], 'ssd': [ob(35, -62, 3.5)], 'gpu': [ob(8, -54.7, 14.6)],
    'aio_pump': [ob(19, 59, 9.2)], 'aio_radiator': [(-1.5, 231, 36)],
    'case_fan_argb': [(208.5, -142, 0), (208.5, 0, 0), (208.5, 142, 0)], 'case_fan': [(-209.5, 145, 14)], 'psu': [(-152, -189, -26)],
}
# Cặp chạm nhau là đúng thiết kế (cắm vào khe, bắt vít, áp lên nhau)
MATING = {frozenset(p) for p in [('motherboard', 'cpu'), ('motherboard', 'ram'), ('motherboard', 'ssd'), ('motherboard', 'gpu'),
                                  ('cpu', 'aio_pump'), ('motherboard', 'aio_backplate'), ('motherboard', 'case'), ('aio_pump', 'aio_backplate'),
                                  ('case', 'case_fan'), ('case', 'case_fan_argb'), ('case', 'psu'), ('case', 'aio_radiator'), ('case', 'gpu'), ('motherboard', 'aio_pump')]}

bpy.ops.wm.read_factory_settings(use_empty=True)
trees = []
for name, spots in PLACE.items():
    for k, p in enumerate(spots):
        before = set(bpy.data.objects)
        bpy.ops.import_scene.gltf(filepath=os.path.join(src, f'g_{name}.glb'))
        new = [o for o in bpy.data.objects if o not in before and o.type == 'MESH']
        # mm theo trục app -> Blender (đơn vị app)
        off = Vector((p[0] / 100, -p[2] / 100, p[1] / 100))
        for o in new:
            if o.name.startswith('peel_film') or o.name.startswith('side_panel'):
                continue
            m = Matrix.Translation(off) @ o.matrix_world
            verts = [m @ v.co for v in o.data.vertices]
            polys = [list(f.vertices) for f in o.data.polygons]
            trees.append((f'{name}#{k}', o.name, BVHTree.FromPolygons(verts, polys, epsilon=0.0), verts, polys))

def cluster(pts, cell=25.0):
    """Gom các điểm va chạm (mm, trục app) thành cụm theo ô lưới để biết va chạm nằm ở đâu."""
    cells = {}
    for p in pts:
        k = tuple(int(c // cell) for c in p)
        cells.setdefault(k, []).append(p)
    out = []
    for v in cells.values():
        lo = [min(p[i] for p in v) for i in range(3)]
        hi = [max(p[i] for p in v) for i in range(3)]
        out.append((len(v), lo, hi))
    return sorted(out, key=lambda c: -c[0])

hits = {}
for i in range(len(trees)):
    for j in range(i + 1, len(trees)):
        a, an, ta, va, pa = trees[i]
        b, bn, tb, vb, pb = trees[j]
        if a == b:
            continue
        pairs = ta.overlap(tb)
        if pairs:
            key = tuple(sorted((a, b)))
            for fa, _ in pairs:
                c = sum((va[k] for k in pa[fa]), Vector()) / len(pa[fa])
                hits.setdefault(key, []).append((c.x * 100, c.z * 100, -c.y * 100))
for (a, b), pts in sorted(hits.items(), key=lambda kv: -len(kv[1])):
    kind = 'khớp' if frozenset((a.split('#')[0], b.split('#')[0])) in MATING else 'VA CHẠM'
    print(f'HIT {kind:8s} {a:16s} × {b:16s} {len(pts)} cặp tam giác')
    for n, lo, hi in cluster(pts)[:6]:
        print('    vùng', n, 'x %.0f…%.0f  y %.0f…%.0f  z %.0f…%.0f' % (lo[0], hi[0], lo[1], hi[1], lo[2], hi[2]))
print('DONE', len(hits))
