import { Suspense } from 'react'
import type { Build, CoolerKind } from '../data/builds'
import { boardLayout, HAS_BOARD_MODEL, ramSlotsUsed } from '../data/layout'
import { Fan } from './Fan'
import { CoolerModel, CpuModel, GpuModel, MODEL_SIZE, MotherboardModel, RamModel, SsdModel } from './Models'
import { Box, Mat, Part } from './Part'

type Layout = ReturnType<typeof boardLayout>

export function BoardAssembly({ build }: { build: Build }) {
  const L = boardLayout(build.formFactor)
  const { socket } = L
  const S = L.surface // độ cao mặt bo: bo thật dày hơn bo tự dựng
  const accent = build.accent
  const gpu = build.gpu
  const gpuThick = gpu ? MODEL_SIZE.gpu[1] * (gpu.length / MODEL_SIZE.gpu[0]) : 0
  // Máy mạnh dùng RAM có tản nhiệt, máy văn phòng / học tập dùng RAM thường
  const heatsinkRam = build.gpu !== null

  return (
    <>
      <Part id="motherboard">
        {HAS_BOARD_MODEL[build.formFactor] ? (
          <Suspense fallback={<ProceduralBoard L={L} ff={build.formFactor} />}>
            <MotherboardModel ff={build.formFactor} />
          </Suspense>
        ) : (
          <ProceduralBoard L={L} ff={build.formFactor} />
        )}
      </Part>

      <Part id="cpu" position={[socket.x, socket.y, S + 0.02]} explode={[0, 0, 0.9]} order={3}>
        <Suspense fallback={<Box size={[0.4, 0.4, 0.04]} position={[0, 0, 0.02]} color="#c9ced6" metal={0.9} rough={0.25} />}>
          <CpuModel />
        </Suspense>
      </Part>

      <Part id="cooler" position={[socket.x, socket.y, S + 0.065]} explode={[0, 0, 1.9]} order={2}>
        <Suspense fallback={<group position={[0, 0, -0.075]}><Cooler kind={build.cooler} accent={accent} /></group>}>
          <CoolerModel kind={build.cooler} />
        </Suspense>
      </Part>

      {ramSlotsUsed(build.ramSticks, L.ramSlots).map((i) => (
        <Part key={i} id="ram" position={[L.ramX(i), L.ramY, S + 0.04]} explode={[0, 0, 1.2]} order={1}>
          <Suspense
            fallback={
              <>
                <Box size={[0.07, 1.33, 0.32]} position={[0, 0, 0.16]} color="#2b2f36" metal={0.6} rough={0.4} />
                {accent && <Box size={[0.075, 1.28, 0.04]} position={[0, 0, 0.34]} color={accent} glow={accent} />}
              </>
            }
          >
            <RamModel heatsink={heatsinkRam} />
          </Suspense>
        </Part>
      ))}

      <Part id="ssd" position={[L.m2.x, L.m2.y, S + 0.005]} explode={[0, 0, 0.7]} order={4}>
        <Suspense
          fallback={
            <>
              <Box size={[0.8, 0.22, 0.03]} position={[0, 0, 0.015]} color="#1f2937" />
              <Box size={[0.5, 0.16, 0.005]} position={[0, 0, 0.033]} color="#d1d5db" metal={0} rough={0.9} />
            </>
          }
        >
          <SsdModel />
        </Suspense>
      </Part>

      {gpu && (
        // Gốc đặt ở mép chân cắm của card, thân card hướng ra phía mặt kính
        <Part
          id="gpu"
          position={[-L.w / 2 + 0.05 + gpu.length / 2, L.pcieY(0) - gpuThick / 2 + 0.08, S + 0.12]}
          explode={[0, 0, 1.5]}
          order={0}
        >
          <Suspense fallback={<group position={[0, 0, gpu.height / 2]}><Gpu {...gpu} accent={accent} /></group>}>
            <GpuModel length={gpu.length} />
          </Suspense>
        </Part>
      )}
    </>
  )
}

/** Bo mạch tự dựng từ khối đơn giản, dùng khi chưa có model thật cho cỡ bo này. */
function ProceduralBoard({ L, ff }: { L: Layout; ff: Build['formFactor'] }) {
  const { socket } = L
  return (
    <>
      <Box size={[L.w, L.h, 0.016]} color="#1c2420" rough={0.8} metal={0.1} />
      <Box
        size={[0.2, L.h * 0.5, 0.35]}
        position={[-L.w / 2 + 0.1, L.h / 2 - 0.1 - (L.h * 0.5) / 2, 0.18]}
        color="#50565f"
        metal={0.6}
      />
      <Box size={[0.62, 0.62, 0.03]} position={[socket.x, socket.y, 0.02]} color="#30343b" metal={0.5} />
      <Box size={[0.22, 0.9, 0.14]} position={[socket.x - 0.58, socket.y, 0.08]} color="#3b4048" metal={0.7} rough={0.35} />
      <Box size={[0.9, 0.22, 0.14]} position={[socket.x, socket.y + 0.58, 0.08]} color="#3b4048" metal={0.7} rough={0.35} />
      {Array.from({ length: L.ramSlots }, (_, i) => (
        <Box key={i} size={[0.08, 1.4, 0.06]} position={[L.ramX(i), socket.y, 0.04]} color="#101214" />
      ))}
      {Array.from({ length: L.pcieSlots }, (_, i) => (
        <Box
          key={i}
          size={[0.89, 0.08, 0.1]}
          position={[L.slotX, L.pcieY(i), 0.06]}
          color={i === 0 ? '#9aa1ab' : '#101214'}
          metal={i === 0 ? 0.8 : 0.1}
        />
      ))}
      <Box size={[0.84, 0.25, 0.01]} position={[L.m2.x, L.m2.y, 0.013]} color="#2a302c" />
      <Box size={[0.14, 0.55, 0.16]} position={[L.w / 2 - 0.1, socket.y - 0.25, 0.09]} color="#e5e7eb" rough={0.8} metal={0} />
      {ff !== 'ITX' && <Box size={[0.45, 0.45, 0.1]} position={[L.w / 2 - 0.55, -L.h / 2 + 0.4, 0.06]} color="#3b4048" metal={0.7} />}
    </>
  )
}

function Cooler({ kind, accent }: { kind: CoolerKind; accent: string | null }) {
  if (kind === 'aio') {
    return (
      <mesh position={[0, 0, 0.29]} rotation={[Math.PI / 2, 0, 0]}>
        <cylinderGeometry args={[0.35, 0.35, 0.42, 40]} />
        <Mat color="#1c1d22" metal={0.55} rough={0.35} />
      </mesh>
    )
  }
  if (kind === 'lowprofile') {
    return (
      <>
        <Box size={[0.7, 0.7, 0.05]} position={[0, 0, 0.075]} color="#b8bec7" metal={0.8} rough={0.3} />
        <Box size={[0.9, 0.9, 0.2]} position={[0, 0, 0.2]} color="#9aa1ab" metal={0.7} rough={0.35} />
        <mesh position={[0, 0, 0.36]} rotation={[Math.PI / 2, 0, 0]}>
          <cylinderGeometry args={[0.42, 0.42, 0.1, 32]} />
          <Mat color="#23262c" />
        </mesh>
      </>
    )
  }
  if (kind === 'stock') {
    return (
      <>
        <mesh position={[0, 0, 0.22]} rotation={[Math.PI / 2, 0, 0]}>
          <cylinderGeometry args={[0.5, 0.5, 0.35, 40]} />
          <Mat color="#8f969f" metal={0.8} rough={0.35} />
        </mesh>
        <mesh position={[0, 0, 0.44]} rotation={[Math.PI / 2, 0, 0]}>
          <cylinderGeometry args={[0.46, 0.46, 0.1, 40]} />
          <Mat color="#1b1d22" />
        </mesh>
      </>
    )
  }
  // Tản tháp: đế, ống đồng, chồng lá nhôm và một quạt thổi từ trước ra sau.
  return (
    <>
      <Box size={[0.45, 0.45, 0.08]} position={[0, 0, 0.09]} color="#c9ced6" metal={0.9} rough={0.25} />
      {[-0.12, -0.04, 0.04, 0.12].map((x) => (
        <mesh key={x} position={[x, 0, 0.25]} rotation={[Math.PI / 2, 0, 0]}>
          <cylinderGeometry args={[0.03, 0.03, 0.3, 12]} />
          <Mat color="#b87333" metal={0.9} rough={0.3} />
        </mesh>
      ))}
      {Array.from({ length: 21 }, (_, i) => (
        <Box key={i} size={[0.5, 1.25, 0.02]} position={[0, 0, 0.35 + i * 0.06]} color="#aeb4bd" metal={0.8} rough={0.3} />
      ))}
      <Box size={[0.52, 1.27, 0.04]} position={[0, 0, 1.58]} color="#1b1d22" />
      <Fan size={1.2} position={[0.38, 0, 0.95]} accent={accent} />
    </>
  )
}

function Gpu({
  length,
  height,
  thick,
  fans,
  accent,
}: {
  length: number
  height: number
  thick: number
  fans: number
  accent: string | null
}) {
  const r = Math.min(height * 0.4, length / (fans * 2.2))
  return (
    <>
      <Box size={[length, thick, height]} color="#1a1c21" metal={0.5} rough={0.45} />
      {/* Tấm lưng */}
      <Box size={[length - 0.05, 0.02, height - 0.05]} position={[0, thick / 2 + 0.01, 0]} color="#2d3139" metal={0.7} rough={0.35} />
      {/* Dải trang trí trên mặt hướng ra kính */}
      <Box
        size={[length * 0.8, 0.04, 0.01]}
        position={[0, 0, height / 2 + 0.006]}
        color={accent ?? '#9aa1ab'}
        glow={accent ?? undefined}
        metal={accent ? 0.2 : 0.8}
      />
      {/* Quạt ở mặt dưới */}
      {Array.from({ length: fans }, (_, i) => (
        <mesh key={i} position={[(i - (fans - 1) / 2) * (length / fans), -thick / 2 - 0.01, 0]}>
          <cylinderGeometry args={[r, r, 0.02, 32]} />
          <Mat color="#2a2d33" />
        </mesh>
      ))}
      {/* Nẹp kim loại ở đuôi card */}
      <Box size={[0.02, thick + 0.1, height + 0.1]} position={[-length / 2 - 0.01, 0, 0]} color="#9aa1ab" metal={0.8} rough={0.3} />
    </>
  )
}
