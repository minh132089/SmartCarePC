import { useFrame, useThree } from '@react-three/fiber'
import { useEffect, useMemo, useRef } from 'react'
import {
  Color,
  Group,
  Mesh,
  MeshDepthMaterial,
  OrthographicCamera,
  PlaneGeometry,
  ShaderMaterial,
  WebGLRenderTarget,
  type Object3D,
  type Scene,
  type WebGLRenderer,
} from 'three'
import { HorizontalBlurShader } from 'three/addons/shaders/HorizontalBlurShader.js'
import { VerticalBlurShader } from 'three/addons/shaders/VerticalBlurShader.js'

/**
 * Bóng đổ mềm dưới chân máy, cùng cách vẽ với ContactShadows của drei, nhưng chỉ vẽ lại khi cảnh đổi:
 * linh kiện dịch chuyển (tách / lắp, đổi máy), model vừa tải xong hoặc ẩn / hiện.
 * ContactShadows của drei vẽ lại cả cảnh mỗi khung, chiếm gần nửa công vẽ kể cả khi chỉ xoay camera.
 * Cánh quạt đang quay (nhóm có userData.spinner) không tính là đổi để máy bật vẫn không phải vẽ bóng liên tục.
 */
export function StaticContactShadows({
  position,
  scale = 10,
  resolution = 512,
  blur = 1,
  opacity = 1,
  far = 10,
  color = '#000000',
}: {
  position?: [number, number, number]
  scale?: number
  resolution?: number
  blur?: number
  opacity?: number
  far?: number
  color?: string
}) {
  const scene = useThree((s) => s.scene)
  const gl = useThree((s) => s.gl)
  const group = useRef<Group>(null)
  const camera = useRef<OrthographicCamera>(null)
  const last = useRef(NaN)

  const res = useMemo(() => {
    const target = new WebGLRenderTarget(resolution, resolution)
    const targetBlur = new WebGLRenderTarget(resolution, resolution)
    target.texture.generateMipmaps = targetBlur.texture.generateMipmaps = false
    const plane = new PlaneGeometry(scale, scale).rotateX(Math.PI / 2)
    const blurPlane = new Mesh(plane)
    const depth = new MeshDepthMaterial()
    depth.depthTest = depth.depthWrite = false
    depth.onBeforeCompile = (shader) => {
      shader.uniforms.ucolor = { value: new Color(color) }
      shader.fragmentShader = shader.fragmentShader
        .replace('void main() {', 'uniform vec3 ucolor;\nvoid main() {')
        .replace('vec4( vec3( 1.0 - fragCoordZ ), opacity );', 'vec4( ucolor * fragCoordZ * 2.0, ( 1.0 - fragCoordZ ) * 1.0 );')
    }
    const hBlur = new ShaderMaterial(HorizontalBlurShader)
    const vBlur = new ShaderMaterial(VerticalBlurShader)
    hBlur.depthTest = vBlur.depthTest = false
    return { target, targetBlur, plane, blurPlane, depth, hBlur, vBlur }
  }, [resolution, scale, color])

  useEffect(() => {
    last.current = NaN // tài nguyên mới (đổi độ phân giải, màu theo giao diện): vẽ lại
    return () => {
      res.target.dispose()
      res.targetBlur.dispose()
      res.plane.dispose()
      res.depth.dispose()
      res.hBlur.dispose()
      res.vBlur.dispose()
    }
  }, [res])

  useFrame(() => {
    const g = group.current
    if (!g || !camera.current) return
    scene.updateMatrixWorld()
    const sig = signature(scene, g)
    if (sig === last.current) return
    last.current = sig
    drawShadow(gl, scene, g, camera.current, res, blur)
  })

  const h = scale / 2
  return (
    <group ref={group} position={position} rotation-x={Math.PI / 2}>
      <mesh geometry={res.plane} scale={[1, -1, 1]} rotation={[-Math.PI / 2, 0, 0]} raycast={() => null}>
        <meshBasicMaterial transparent map={res.target.texture} opacity={opacity} depthWrite={false} />
      </mesh>
      <orthographicCamera ref={camera} args={[-h, h, h, -h, 0, far]} />
    </group>
  )
}

type Resources = {
  target: WebGLRenderTarget
  targetBlur: WebGLRenderTarget
  blurPlane: Mesh
  depth: MeshDepthMaterial
  hBlur: ShaderMaterial
  vBlur: ShaderMaterial
}

/** Vẽ độ sâu của cả cảnh nhìn từ dưới sàn lên vào `target`, rồi làm mờ hai lần cho bóng mềm. */
function drawShadow(gl: WebGLRenderer, scene: Scene, self: Object3D, camera: OrthographicCamera, r: Resources, blur: number) {
  const bg = scene.background
  const override = scene.overrideMaterial
  self.visible = false
  scene.background = null
  scene.overrideMaterial = r.depth
  gl.setRenderTarget(r.target)
  // Xoá trước khi vẽ: bộ hậu kỳ tắt autoClear, không xoá thì bóng cộng dồn mỗi lần vẽ, đậm và loang dần theo thời gian
  gl.clear()
  gl.render(scene, camera)
  blurOnce(gl, camera, r, blur)
  blurOnce(gl, camera, r, blur * 0.4)
  gl.setRenderTarget(null)
  self.visible = true
  scene.overrideMaterial = override
  scene.background = bg
}

function blurOnce(gl: WebGLRenderer, camera: OrthographicCamera, r: Resources, amount: number) {
  r.blurPlane.visible = true
  r.blurPlane.material = r.hBlur
  r.hBlur.uniforms.tDiffuse.value = r.target.texture
  r.hBlur.uniforms.h.value = amount / 256
  gl.setRenderTarget(r.targetBlur)
  gl.render(r.blurPlane, camera)
  r.blurPlane.material = r.vBlur
  r.vBlur.uniforms.tDiffuse.value = r.targetBlur.texture
  r.vBlur.uniforms.v.value = amount / 256
  gl.setRenderTarget(r.target)
  gl.render(r.blurPlane, camera)
  r.blurPlane.visible = false
}

/** Dấu vân tay rẻ của vị trí, cỡ và số lượng các mesh đang hiện (vài trăm mesh, dưới 0,1 ms). */
function signature(root: Object3D, skip: Object3D): number {
  let sum = 0
  let n = 0
  const visit = (o: Object3D) => {
    if (!o.visible || o === skip) return
    if ((o as Mesh).isMesh) {
      const e = o.matrixWorld.elements
      n++
      sum += (e[12] * 1.31 + e[13] * 2.71 + e[14] * 4.13 + e[0] + e[5] + e[10]) * ((n % 89) + 1)
    }
    if (o.userData.spinner) return
    for (const c of o.children) visit(c)
  }
  visit(root)
  return sum + n * 1e7
}
