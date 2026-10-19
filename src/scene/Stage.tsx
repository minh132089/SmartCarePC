import { Grid, MeshReflectorMaterial } from '@react-three/drei'
import { Bloom, EffectComposer, N8AO } from '@react-three/postprocessing'
import { QUALITY } from '../perf/quality'
import { useViewer } from '../store'
import { useStage } from '../theme'
import { StaticContactShadows } from './StaticContactShadows'

/**
 * Sàn studio kiểu trưng bày sản phẩm: mặt sàn bóng phản chiếu mờ, lưới mảnh nhạt dần theo khoảng cách,
 * bóng đổ mềm ngay dưới chân case. `y` là cao độ đáy chân đế của case.
 */
export function StudioFloor({ y }: { y: number }) {
  const q = QUALITY[useViewer((s) => s.quality)]
  // Màu sàn, lưới và bóng theo chế độ sáng / tối; nền và sương mù đặt ở Scene với cùng bảng màu
  const c = useStage()
  return (
    <group position={[0, y, 0]}>
      <mesh rotation-x={-Math.PI / 2} raycast={() => null}>
        <planeGeometry args={[160, 160]} />
        {q.reflector === 0 ? (
          // Mức nhẹ: sàn thường, không phải vẽ cả cảnh thêm một lần để làm phản chiếu
          <meshStandardMaterial color={c.floor} roughness={0.9} metalness={0.05} />
        ) : (
        <MeshReflectorMaterial
          key={q.reflector}
          color={c.reflector}
          resolution={q.reflector}
          blur={[400, 120]}
          mixBlur={1}
          mixStrength={0.55}
          mirror={0.18}
          roughness={0.9}
          metalness={0.05}
          depthScale={1.1}
          minDepthThreshold={0.4}
          maxDepthThreshold={1.3}
        />
        )}
      </mesh>
      <Grid
        position={[0, 0.002, 0]}
        args={[40, 40]}
        cellSize={0.5}
        cellThickness={0.55}
        cellColor={c.cell}
        sectionSize={2.5}
        sectionThickness={0.9}
        sectionColor={c.section}
        fadeDistance={24}
        fadeStrength={1.6}
        infiniteGrid
      />
      <StaticContactShadows position={[0, 0.004, 0]} scale={14} resolution={q.shadow} blur={2.6} opacity={0.5} far={5} color={c.shadow} />
    </group>
  )
}

/**
 * Hậu kỳ: bóng tối ở khe giữa các linh kiện (ambient occlusion) cho cảm giác có chiều sâu,
 * và hiệu ứng toả sáng chỉ cho vật phát sáng mạnh (viền đèn quạt, dải RGB) nhờ ngưỡng sáng 1,6.
 */
export function Effects() {
  const q = QUALITY[useViewer((s) => s.quality)]
  if (!q.ao && !q.bloom) return null // mức nhẹ: bỏ hẳn bộ hậu kỳ
  return (
    <EffectComposer key={`${q.ao}-${q.multisampling}`} multisampling={q.multisampling}>
      {q.ao ? <N8AO halfRes quality="performance" aoRadius={0.45} distanceFalloff={0.6} intensity={1.8} color="#000000" /> : <></>}
      {/* Ngưỡng 1,6: sàn và case trắng dưới đèn mạnh không vượt qua, chỉ đèn RGB (độ sáng tự phát ~2,6) mới toả quầng.
          Không thêm hiệu ứng tone mapping ở đây vì trình render đã tự làm, làm hai lần sẽ nhạt màu. */}
      {q.bloom ? <Bloom mipmapBlur luminanceThreshold={1.6} luminanceSmoothing={0.2} intensity={0.9} /> : <></>}
    </EffectComposer>
  )
}
