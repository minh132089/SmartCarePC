"""
Thư viện dựng hình dùng chung cho bộ linh kiện Gaming (tools/build_gaming.py).

Quy ước: mọi hàm nhận toạ độ theo TRỤC CỦA APP và đơn vị MILIMET.
  x: theo chiều dài linh kiện, y: hướng lên, z: hướng ra mặt kính.
Khi xuất, finish() đổi sang đơn vị app (1 = 100 mm) và trục Blender
(Blender x, y, z = app x, -z, y) để file glTF ra đúng trục của app.
"""
import bpy, bmesh, math, os
from mathutils import Vector, Matrix

# app (mm) -> Blender (đơn vị app)
TO_BLENDER = Matrix(((1, 0, 0, 0), (0, 0, -1, 0), (0, 1, 0, 0), (0, 0, 0, 1))) @ Matrix.Scale(0.01, 4)


def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)


def lin(h):
    h = h.lstrip('#')
    c = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    return tuple(x / 12.92 if x <= 0.04045 else ((x + 0.055) / 1.055) ** 2.4 for x in c) + (1.0,)


def mat(name, color, metal=0.0, rough=0.5, emit=None, strength=1.0, alpha=1.0):
    m = bpy.data.materials.get(name)
    if m:
        return m
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    p = m.node_tree.nodes['Principled BSDF']
    p.inputs['Base Color'].default_value = lin(color)
    p.inputs['Metallic'].default_value = metal
    p.inputs['Roughness'].default_value = rough
    if emit:
        p.inputs['Emission Color'].default_value = lin(emit)
        p.inputs['Emission Strength'].default_value = strength
    if alpha < 1:
        p.inputs['Alpha'].default_value = alpha
        m.surface_render_method = 'BLENDED'
    return m


def _obj(bm, m, name='p', smooth_angle=None):
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    o = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(o)
    me.materials.append(m)
    if smooth_angle is not None:
        for p in me.polygons:
            p.use_smooth = True
        me.set_sharp_from_angle(angle=math.radians(smooth_angle))
    return o


def box(lo, hi, m, bevel=0.0, seg=2, name='box'):
    """Hộp theo hai góc lo, hi (mm). bevel: bo cạnh (mm)."""
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0)
    for v in bm.verts:
        v.co = Vector([lo[i] + (v.co[i] + 0.5) * (hi[i] - lo[i]) for i in range(3)])
    if bevel:
        bmesh.ops.bevel(bm, geom=bm.edges[:], offset=bevel, segments=seg, affect='EDGES', profile=0.5)
    return _obj(bm, m, name, smooth_angle=35 if bevel else None)


def cbox(c, size, m, **kw):
    """Hộp theo tâm và kích thước."""
    lo = [c[i] - size[i] / 2 for i in range(3)]
    hi = [c[i] + size[i] / 2 for i in range(3)]
    return box(lo, hi, m, **kw)


_AXIS_ROT = {
    'x': Matrix.Rotation(math.radians(90), 4, 'Y'),
    'y': Matrix.Rotation(math.radians(-90), 4, 'X'),
    'z': Matrix.Identity(4),
}


def cyl(c, r, length, axis, m, seg=32, r2=None, name='cyl', smooth=True):
    """Trụ (hoặc nón cụt nếu có r2) theo trục 'x' | 'y' | 'z', tâm c."""
    bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=True, segments=seg, radius1=r, radius2=r if r2 is None else r2, depth=length)
    bmesh.ops.transform(bm, verts=bm.verts, matrix=_AXIS_ROT[axis])
    bmesh.ops.translate(bm, verts=bm.verts, vec=Vector(c))
    return _obj(bm, m, name, smooth_angle=40 if smooth else None)


def tube(c, r_out, r_in, length, axis, m, seg=32, name='tube'):
    """Ống tròn rỗng (vòng đệm, vành quạt)."""
    bm = bmesh.new()
    rings = []
    for z, r in ((-length / 2, r_out), (length / 2, r_out), (length / 2, r_in), (-length / 2, r_in)):
        rings.append([bm.verts.new((r * math.cos(a), r * math.sin(a), z))
                      for a in (i / seg * math.tau for i in range(seg))])
    for k in range(4):
        a, b = rings[k], rings[(k + 1) % 4]
        for i in range(seg):
            j = (i + 1) % seg
            bm.faces.new((a[i], a[j], b[j], b[i]))
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bmesh.ops.transform(bm, verts=bm.verts, matrix=_AXIS_ROT[axis])
    bmesh.ops.translate(bm, verts=bm.verts, vec=Vector(c))
    return _obj(bm, m, name, smooth_angle=40)


def prism(pts2d, z0, z1, m, plane='xy', name='prism'):
    """Đùn một đa giác 2D (mm) thành khối. plane='xy': đùn theo z; 'xz': đùn theo y; 'yz': đùn theo x."""
    bm = bmesh.new()

    def P(u, v, w):
        if plane == 'xy':
            return (u, v, w)
        if plane == 'xz':
            return (u, w, v)
        return (w, u, v)
    bot = [bm.verts.new(P(u, v, z0)) for u, v in pts2d]
    top = [bm.verts.new(P(u, v, z1)) for u, v in pts2d]
    bm.faces.new(bot[::-1])
    bm.faces.new(top)
    n = len(pts2d)
    for i in range(n):
        j = (i + 1) % n
        bm.faces.new((bot[i], bot[j], top[j], top[i]))
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    return _obj(bm, m, name)


def rounded_rect(w, h, r, seg=6, cx=0.0, cy=0.0):
    """Đa giác chữ nhật bo góc (mm)."""
    pts = []
    for (sx, sy, a0) in ((1, 1, 0), (-1, 1, 90), (-1, -1, 180), (1, -1, 270)):
        ox, oy = cx + sx * (w / 2 - r), cy + sy * (h / 2 - r)
        for i in range(seg + 1):
            a = math.radians(a0 + 90 * i / seg)
            pts.append((ox + r * math.cos(a), oy + r * math.sin(a)))
    return pts


def quads(rects, z, m, axis='z', name='pads'):
    """Nhiều ô vuông phẳng (pad, lỗ lưới...) trong MỘT mesh. rects: list (cx, cy, w, h).
    axis: pháp tuyến hướng theo trục nào (+/-): 'z', '-z', 'y', '-y', 'x', '-x'."""
    bm = bmesh.new()
    neg = axis.startswith('-')
    ax = axis[-1]
    for cx, cy, w, h in rects:
        corners = [(cx - w / 2, cy - h / 2), (cx + w / 2, cy - h / 2), (cx + w / 2, cy + h / 2), (cx - w / 2, cy + h / 2)]
        vs = []
        for u, v in corners:
            co = {'z': (u, v, z), 'y': (u, z, v), 'x': (z, u, v)}[ax]
            vs.append(bm.verts.new(co))
        f = bm.faces.new(vs)
        if (ax == 'z' and neg) or (ax == 'y' and not neg) or (ax == 'x' and neg):
            f.normal_flip()
    return _obj(bm, m, name)


def bezier_tube(points, r, m, seg=12, name='cable', res=12):
    """Ống mềm (dây, ống nước) đi qua các điểm (mm), mặt cắt tròn bán kính r."""
    cu = bpy.data.curves.new(name, 'CURVE')
    cu.dimensions = '3D'
    cu.bevel_depth = r
    cu.bevel_resolution = max(2, seg // 4)
    cu.resolution_u = res
    cu.use_fill_caps = True
    sp = cu.splines.new('BEZIER')
    sp.bezier_points.add(len(points) - 1)
    for bp, p in zip(sp.bezier_points, points):
        bp.co = p
        bp.handle_left_type = bp.handle_right_type = 'AUTO'
    o = bpy.data.objects.new(name, cu)
    bpy.context.scene.collection.objects.link(o)
    o.data.materials.append(m)
    bpy.context.view_layer.objects.active = o
    o.select_set(True)
    bpy.ops.object.convert(target='MESH')
    o = bpy.context.view_layer.objects.active
    for p in o.data.polygons:
        p.use_smooth = True
    o.select_set(False)
    return o


def apply_mods():
    for o in list(bpy.context.scene.objects):
        if o.type == 'MESH' and o.modifiers:
            bpy.context.view_layer.objects.active = o
            for md in list(o.modifiers):
                bpy.ops.object.modifier_apply(modifier=md.name)


def boolean(target, cutter, op='DIFFERENCE'):
    md = target.modifiers.new('bool', 'BOOLEAN')
    md.operation = op
    md.object = cutter
    md.solver = 'EXACT'
    bpy.context.view_layer.objects.active = target
    before = len(target.data.polygons)
    bpy.ops.object.modifier_apply(modifier=md.name)
    bpy.data.objects.remove(cutter, do_unlink=True)
    # Phép khoét hỏng (vật cắt tự giao nhau) thường xoá sạch hình: dừng ngay để không xuất model thiếu mặt
    if op == 'DIFFERENCE' and len(target.data.polygons) < min(6, before):
        raise RuntimeError(f'boolean xoá mất hình của {target.name} ({before} → {len(target.data.polygons)} mặt)')


def cut_each(target, boxes, m):
    """Khoét lần lượt từng khối (lo, hi). Dùng khi các khối chạm hoặc chồng lên nhau."""
    for lo, hi in boxes:
        boolean(target, box(lo, hi, m, name='cut'))


def join(objs, name):
    objs = [o for o in objs if o and o.name in bpy.context.scene.objects]
    # Gộp vật có UV với vật không có UV làm mất UV của vật đầu: cho mọi mesh cùng một lớp 'UVMap' trước khi gộp
    if any(o.type == 'MESH' and o.data.uv_layers for o in objs):
        for o in objs:
            if o.type == 'MESH' and not o.data.uv_layers:
                o.data.uv_layers.new(name='UVMap')
    bpy.ops.object.select_all(action='DESELECT')
    for o in objs:
        o.select_set(True)
    bpy.context.view_layer.objects.active = objs[0]
    if len(objs) > 1:
        bpy.ops.object.join()
    o = bpy.context.view_layer.objects.active
    o.name = o.data.name = name
    return o


def finish(path, groups=None, main='part'):
    """Đổi trục/đơn vị và xuất GLB.
    groups: {tên node: [object]} giữ riêng (ví dụ cánh quạt quay); phần còn lại gộp thành `main`."""
    apply_mods()
    groups = groups or {}
    kept = {o for objs in groups.values() for o in objs}
    rest = [o for o in bpy.context.scene.objects if o.type == 'MESH' and o not in kept]
    nodes = [join(rest, main)]
    for name, objs in groups.items():
        nodes.append(join(objs, name))
    for o in nodes:
        o.data.transform(TO_BLENDER)
        o.matrix_world = Matrix.Identity(4)
    for o in list(bpy.context.scene.objects):
        if o not in nodes:
            bpy.data.objects.remove(o, do_unlink=True)
    # Node con (bộ phận quay) đặt gốc ở tâm hình khối của nó để app xoay đúng tâm
    for o in nodes[1:]:
        pts = [Vector(v.co) for v in o.data.vertices]
        c = sum(pts, Vector()) / len(pts)
        lo = Vector([min(p[i] for p in pts) for i in range(3)])
        hi = Vector([max(p[i] for p in pts) for i in range(3)])
        c = (lo + hi) / 2
        o.data.transform(Matrix.Translation(-c))
        o.location = c
    os.makedirs(os.path.dirname(path), exist_ok=True)
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.export_scene.gltf(filepath=path, export_format='GLB', export_apply=True)
    faces = sum(len(o.data.polygons) for o in nodes)
    print(f'RESULT {os.path.basename(path)}: {faces} faces, nodes {[o.name for o in nodes]}')


def label(text, x, y, z, size, m, rot=0.0, align='CENTER', name='label'):
    """Chữ in phẳng (silkscreen) nằm trên mặt phẳng xy, mặt chữ hướng +z. size: chiều cao chữ (mm)."""
    cu = bpy.data.curves.new(name, 'FONT')
    cu.body = text
    cu.size = size
    cu.align_x = align
    cu.align_y = 'CENTER'
    cu.extrude = 0.0
    o = bpy.data.objects.new(name, cu)
    bpy.context.scene.collection.objects.link(o)
    o.location = (x, y, z)
    o.rotation_euler = (0, 0, math.radians(rot))
    o.data.materials.append(m)
    bpy.ops.object.select_all(action='DESELECT')
    bpy.context.view_layer.objects.active = o
    o.select_set(True)
    bpy.ops.object.convert(target='MESH')
    o = bpy.context.view_layer.objects.active
    o.data.transform(o.matrix_world)
    o.matrix_world = Matrix.Identity(4)
    o.select_set(False)
    return o


def pins(xs_ys, z0, z1, m, w=0.64, name='pins'):
    """Chân cắm vuông (header) tại các điểm (x, y), từ z0 lên z1."""
    bm = bmesh.new()
    for x, y in xs_ys:
        r = bmesh.ops.create_cube(bm, size=1.0)
        for v in r['verts']:
            v.co = Vector((x + v.co.x * w, y + v.co.y * w, (z0 + z1) / 2 + v.co.z * (z1 - z0)))
    return _obj(bm, m, name)


def ring(c, r_out, r_in, z, m, seg=24, name='ring'):
    """Vành phẳng (miếng đồng quanh lỗ vít) trên mặt phẳng xy tại độ cao z, mặt hướng +z."""
    bm = bmesh.new()
    a = [bm.verts.new((c[0] + r_out * math.cos(t), c[1] + r_out * math.sin(t), z)) for t in (i / seg * math.tau for i in range(seg))]
    b = [bm.verts.new((c[0] + r_in * math.cos(t), c[1] + r_in * math.sin(t), z)) for t in (i / seg * math.tau for i in range(seg))]
    for i in range(seg):
        j = (i + 1) % seg
        bm.faces.new((a[i], a[j], b[j], b[i]))
    return _obj(bm, m, name)


def disc(c, r, z, m, seg=24, name='disc'):
    bm = bmesh.new()
    bm.faces.new([bm.verts.new((c[0] + r * math.cos(t), c[1] + r * math.sin(t), z)) for t in (i / seg * math.tau for i in range(seg))])
    return _obj(bm, m, name)


def torus(c, R, r, axis, m, seg=64, rseg=8, name='torus'):
    """Vòng dây tiết diện tròn (bán kính đường tâm R, bán kính dây r) quanh trục 'x' | 'y' | 'z'."""
    bm = bmesh.new()
    rings = []
    for i in range(seg):
        a = i / seg * math.tau
        ca, sa = math.cos(a), math.sin(a)
        ring_ = []
        for j in range(rseg):
            b = j / rseg * math.tau
            rr = R + r * math.cos(b)
            ring_.append(bm.verts.new((rr * ca, rr * sa, r * math.sin(b))))
        rings.append(ring_)
    for i in range(seg):
        for j in range(rseg):
            a, b = rings[i], rings[(i + 1) % seg]
            bm.faces.new((a[j], b[j], b[(j + 1) % rseg], a[(j + 1) % rseg]))
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bmesh.ops.transform(bm, verts=bm.verts, matrix=_AXIS_ROT[axis])
    bmesh.ops.translate(bm, verts=bm.verts, vec=Vector(c))
    return _obj(bm, m, name, smooth_angle=60)


def bevel_obj(o, width, seg=2):
    """Vát cạnh sắc của một vật (bevel theo góc), áp dụng khi xuất."""
    md = o.modifiers.new('bv', 'BEVEL')
    md.width = width
    md.segments = seg
    md.limit_method = 'ANGLE'
    return o


def perforated_material(name, color, hole_d, pitch, px=96):
    """Tấm đục lỗ tròn lưới tam giác: ảnh lặp một ô (pitch × 2 hàng), lỗ trong suốt.
    Dùng với perforated_plate() để UV khớp đúng bước lỗ (mm)."""
    import numpy as np
    row = pitch * math.sqrt(3) / 2
    w, h = px, max(2, round(px * 2 * row / pitch))
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    x = (xx + 0.5) / w * pitch
    y = (yy + 0.5) / h * 2 * row
    r = hole_d / 2
    hole = np.zeros((h, w), bool)
    for cx, cy in ((0, 0), (pitch, 0), (pitch / 2, row), (0, 2 * row), (pitch, 2 * row)):
        hole |= (x - cx) ** 2 + (y - cy) ** 2 < r * r
    base = lin(color)
    pxs = np.zeros((h, w, 4), np.float32)
    for k in range(3):
        pxs[..., k] = base[k]
    pxs[..., 3] = np.where(hole, 0.0, 1.0)
    img = bpy.data.images.new(name, w, h, alpha=True)
    img.pixels.foreach_set(pxs.ravel())
    img.pack()
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    p = nt.nodes['Principled BSDF']
    p.inputs['Metallic'].default_value = 0.3
    p.inputs['Roughness'].default_value = 0.7
    t = nt.nodes.new('ShaderNodeTexImage')
    t.image = img
    t.interpolation = 'Closest'
    nt.links.new(t.outputs['Color'], p.inputs['Base Color'])
    nt.links.new(t.outputs['Alpha'], p.inputs['Alpha'])
    m.surface_render_method = 'BLENDED'
    m['tile'] = (pitch, 2 * row)
    return m


def perforated_plate(outline, y0, y1, m, name='perforated', plane='xz'):
    """Tấm mỏng theo đa giác `outline`, đùn từ y0 tới y1 theo pháp tuyến của mặt phẳng, UV lặp theo ô của
    perforated_material. plane='xz': tấm ngang (outline (x, z)); 'yz': tấm đứng pháp tuyến x (outline (y, z));
    'xy': tấm đứng pháp tuyến z (outline (x, y))."""
    tw, th = m['tile']
    o = prism(outline, y0, y1, m, plane=plane, name=name)
    me = o.data
    uv = me.uv_layers.new(name='UVMap')
    for poly in me.polygons:
        for li in poly.loop_indices:
            v = me.vertices[me.loops[li].vertex_index].co
            a, b = {'xz': (v.x, v.z), 'yz': (v.z, v.y), 'xy': (v.x, v.y)}[plane]
            uv.data[li].uv = (a / tw, b / th)
    return o
