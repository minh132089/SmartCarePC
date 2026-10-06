"""
Kiểm tra va chạm của một họ case khi đã lắp (đặt linh kiện giống src/data/families.ts + FamilyRig.tsx).
Chạy: blender -b --python tools/check_family.py -- <family_id> [...]
In các cặp linh kiện có tam giác cắt nhau kèm vùng (mm, trục app). Cặp "khớp" là chạm đúng thiết kế.
"""
import bpy, sys, os, json, math
from mathutils import Matrix, Vector
from mathutils.bvhtree import BVHTree

ROOT = os.path.join(os.path.dirname(__file__), '..')
FAM = json.load(open(os.path.join(ROOT, 'src/data/families.json')))
BRD = json.load(open(os.path.join(ROOT, 'src/data/boards.json')))
ATX = {'cpu': [19, 59], 'ram_x': [70, 79, 89, 98], 'ram_y': 62, 'ram_used': [1, 3], 'm2_center': [35, -62]}
G = os.path.join(ROOT, 'assets-src/gaming')
F = os.path.join(ROOT, 'assets-src/families')
C = Matrix(((1, 0, 0, 0), (0, 0, -1, 0), (0, 1, 0, 0), (0, 0, 0, 1)))   # app -> Blender (trục)
S = Matrix.Scale(0.01, 4)


def app_mat(pos, rx=0.0, ry=0.0, rz=0.0):
    """Ma trận đặt vật trong hệ app (mm) rồi đổi sang Blender (đơn vị app)."""
    m = Matrix.Translation(Vector(pos)) @ Matrix.Rotation(rx, 4, 'X') @ Matrix.Rotation(ry, 4, 'Y') @ Matrix.Rotation(rz, 4, 'Z')
    return C @ S @ m @ S.inverted() @ C.inverted()


ROT = {'R0': 0.0, 'R+': math.pi / 2, 'R-': -math.pi / 2}
FAN_ROT = {'-x': (0, 0, 0), '+y': (0, 0, -math.pi / 2), '+z': (0, math.pi / 2, 0)}
MATING = [('motherboard', x) for x in ('cpu', 'ram', 'ssd', 'gpu', 'backplate', 'cooler', 'riser_male', 'case')] + \
         [('case', x) for x in ('fan', 'psu', 'radiator', 'gpu', 'stand', 'backplate', 'riser_slot', 'hub', 'riser_male')] + \
         [('cpu', 'cooler'), ('backplate', 'cooler'), ('gpu', 'riser_slot')]


def rotate(r, p):
    x, y, z = p
    return [x, y, z] if r == 'R0' else ([x, -z, y] if r == 'R+' else [x, z, -y])


def add(a, b):
    return [a[i] + b[i] for i in range(3)]


def check(fid):
    f = FAM[fid]
    L = ATX if f['board']['ff'] == 'ATX' else BRD[f['board']['ff']]
    B = f['board']['origin']
    ob = lambda x, y, z: add(B, [x, y, z])  # noqa: E731
    items = [('case', os.path.join(F, f'f_case_{fid}.glb'), app_mat([0, 0, 0]), ('panel_open', 'panel_closed')),
             ('motherboard', os.path.join(G, 'g_motherboard.glb') if f['board']['ff'] == 'ATX' else os.path.join(F, f"f_motherboard_{f['board']['ff'].lower()}.glb"), app_mat(B), ()),
             ('backplate', os.path.join(G, 'g_aio_backplate.glb'), app_mat(ob(L['cpu'][0], L['cpu'][1], 0)), ()),
             ('cpu', os.path.join(G, 'g_cpu.glb'), app_mat(ob(L['cpu'][0], L['cpu'][1], 4.7)), ()),
             ('ssd', os.path.join(G, 'g_ssd.glb'), app_mat(ob(L['m2_center'][0], L['m2_center'][1], 3.5)), ())]
    for i in L['ram_used']:
        items.append(('ram', os.path.join(G, 'g_ram.glb'), app_mat(ob(L['ram_x'][i], L['ram_y'], 5.5)), ()))
    ihs = ob(L['cpu'][0], L['cpu'][1], 9.2)
    ck = f['cooler']['kind']
    items.append(('cooler', os.path.join(G, 'g_aio_pump.glb') if ck.startswith('aio') else os.path.join(F, f'f_{ck}.glb'), app_mat(ihs), ('peel_film',)))
    if f.get('radiator'):
        r = f['radiator']
        items.append(('radiator', os.path.join(G, 'g_aio_radiator.glb') if r['kind'] == 'aio_240' else os.path.join(F, 'f_aio_360.glb'), app_mat(r['origin']), ()))
    gk = f['gpu']['kind']
    items.append(('gpu', os.path.join(G, 'g_gpu.glb') if gk == 'gpu_280' else os.path.join(F, 'f_gpu_240_dual.glb'), app_mat(f['gpu']['origin'], ROT[f['gpu']['rot']]), ()))
    if f.get('riser'):
        fing = [-42, 24.7, 0] if gk == 'gpu_280' else [-22, 19.7, 0]
        n = rotate(f['gpu']['rot'], [0, 0, 1])
        fingers = add(f['gpu']['origin'], rotate(f['gpu']['rot'], fing))
        slot = add(fingers, [-n[0] * 14, -n[1] * 14, -n[2] * 14])
        items.append(('riser_male', os.path.join(F, 'f_riser_ends.glb'), app_mat(f['riser']['board']), ('riser_slot',)))
        items.append(('riser_slot', os.path.join(F, 'f_riser_ends.glb'), app_mat(slot, ROT[f['gpu']['rot']]), ('riser_male',)))
    pk = f['psu']['kind']
    items.append(('psu', os.path.join(G, 'g_psu.glb') if pk == 'atx' else os.path.join(F, 'f_psu_sfx_100.glb'), app_mat(f['psu']['center'], ROT[f['psu']['rot']]), ()))
    for fan in f['fans']:
        rx, ry, rz = FAN_ROT[fan['blow']]
        items.append(('fan', os.path.join(F, f"f_{fan['model']}.glb"), app_mat(fan['at'], rx + math.radians(fan.get('spin', 0)), ry, rz), ()))
    if f.get('hub'):
        items.append(('hub', os.path.join(F, 'f_fan_hub_8.glb'), app_mat(f['hub'], 0, math.pi), ()))
    if f.get('stand'):
        items.append(('stand', os.path.join(F, 'f_console_stand.glb'), app_mat(f['stand']['origin']), ()))

    bpy.ops.wm.read_factory_settings(use_empty=True)
    trees = []
    for k, (name, path, M, skip) in enumerate(items):
        before = set(bpy.data.objects)
        bpy.ops.import_scene.gltf(filepath=path)
        for o in [o for o in bpy.data.objects if o not in before and o.type == 'MESH']:
            if any(o.name.startswith(sk) for sk in skip):
                continue
            m = M @ o.matrix_world
            verts = [m @ v.co for v in o.data.vertices]
            polys = [list(p.vertices) for p in o.data.polygons]
            trees.append((f'{name}#{k}', name, BVHTree.FromPolygons(verts, polys, epsilon=0.0), verts, polys))
    hits = {}
    for i in range(len(trees)):
        for j in range(i + 1, len(trees)):
            a, an, ta, va, pa = trees[i]
            b, bn, tb, vb, pb = trees[j]
            if a == b:
                continue
            for fa, _ in ta.overlap(tb):
                c = sum((va[q] for q in pa[fa]), Vector()) / len(pa[fa])
                hits.setdefault((a, b, an, bn), []).append((c.x * 100, c.z * 100, -c.y * 100))
    print(f'== {fid}')
    for (a, b, an, bn), pts in sorted(hits.items(), key=lambda kv: -len(kv[1])):
        mating = (an, bn) in MATING or (bn, an) in MATING
        lo = [min(p[i] for p in pts) for i in range(3)]
        hi = [max(p[i] for p in pts) for i in range(3)]
        print(f"HIT {'khớp   ' if mating else 'VA CHẠM'} {a:14s} × {b:14s} {len(pts):5d}  x {lo[0]:.0f}…{hi[0]:.0f}  y {lo[1]:.0f}…{hi[1]:.0f}  z {lo[2]:.0f}…{hi[2]:.0f}")


for fid in sys.argv[sys.argv.index('--') + 1:]:
    check(fid)
