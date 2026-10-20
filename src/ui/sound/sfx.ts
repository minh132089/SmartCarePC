import { create } from 'zustand'

/**
 * Hiệu ứng âm thanh của app, tổng hợp bằng Web Audio (không tải file nào): nhạc nền ở màn giới thiệu, tiếng gửi câu
 * hỏi, tiếng lách tách khi trợ lý đang nghĩ, tiếng chuông khi trả lời xong, tiếng tick khi đánh dấu xong bước lắp.
 * Trình duyệt chỉ cho phát tiếng sau lần bấm / gõ phím đầu tiên, nên AudioContext tạo lúc đó; nhạc nền được yêu cầu
 * trước đó thì đợi tới lúc ấy mới bật.
 */

const KEY = 'smartcare.sound'
const MASTER = 0.9

function loadMuted() {
  try {
    return localStorage.getItem(KEY) === 'off'
  } catch {
    return false
  }
}

export const useSound = create<{ muted: boolean; toggle: () => void }>((set, get) => ({
  muted: loadMuted(),
  toggle: () => {
    const muted = !get().muted
    try {
      localStorage.setItem(KEY, muted ? 'off' : 'on')
    } catch {
      // trình duyệt chặn lưu: vẫn tắt / bật cho phiên này
    }
    set({ muted })
    if (audio) audio.master.gain.setTargetAtTime(muted ? 0 : MASTER, audio.ctx.currentTime, 0.05)
  },
}))

let audio: { ctx: AudioContext; master: GainNode } | null = null
let unlocked = false

/** AudioContext dùng chung; null nếu người dùng chưa bấm gì (chưa được phép phát) hoặc trình duyệt không hỗ trợ */
function ctx() {
  if (!unlocked) return null
  if (!audio) {
    const Ctor = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!Ctor) return null
    const c = new Ctor()
    const master = c.createGain()
    master.gain.value = useSound.getState().muted ? 0 : MASTER
    master.connect(c.destination)
    audio = { ctx: c, master }
  }
  if (audio.ctx.state === 'suspended') void audio.ctx.resume()
  return audio
}

if (typeof window !== 'undefined') {
  const unlock = () => {
    unlocked = true
    window.removeEventListener('pointerdown', unlock, true)
    window.removeEventListener('keydown', unlock, true)
    if (ambientWanted) startAmbient()
  }
  window.addEventListener('pointerdown', unlock, true)
  window.addEventListener('keydown', unlock, true)
}

/** Một nốt ngắn: lên tiếng nhanh rồi tắt dần theo hàm mũ */
function tone(freq: number, at: number, dur: number, gain: number, type: OscillatorType = 'sine', glideTo?: number) {
  const a = ctx()
  if (!a) return
  const t = a.ctx.currentTime + at
  const osc = a.ctx.createOscillator()
  const g = a.ctx.createGain()
  osc.type = type
  osc.frequency.setValueAtTime(freq, t)
  if (glideTo) osc.frequency.exponentialRampToValueAtTime(glideTo, t + dur * 0.6)
  g.gain.setValueAtTime(0.0001, t)
  g.gain.exponentialRampToValueAtTime(gain, t + 0.008)
  g.gain.exponentialRampToValueAtTime(0.0001, t + dur)
  osc.connect(g).connect(a.master)
  osc.start(t)
  osc.stop(t + dur + 0.02)
}

/** Gửi câu hỏi: vệt "vút" đi lên */
export function playSubmit() {
  tone(340, 0, 0.18, 0.09, 'sine', 920)
  tone(1760, 0.05, 0.06, 0.025, 'triangle')
}

/** Đánh dấu xong bước lắp (done = true) hay bỏ đánh dấu */
export function playTick(done = true) {
  if (done) {
    tone(2400, 0, 0.04, 0.06, 'triangle')
    tone(1600, 0.045, 0.09, 0.05, 'sine')
  } else {
    tone(900, 0, 0.06, 0.04, 'triangle')
  }
}

/** Trợ lý trả lời xong: ba nốt chuông đi lên để người dùng ở tab khác cũng biết */
export function playDone() {
  tone(659.3, 0, 0.5, 0.08, 'sine')
  tone(987.8, 0.11, 0.55, 0.07, 'sine')
  tone(1318.5, 0.22, 0.8, 0.06, 'sine')
  tone(1318.5 * 2, 0.22, 0.35, 0.012, 'sine')
}

/** Tiếng lách tách như máy đang tính, phát khi trợ lý đang nghĩ */
let thinking: ReturnType<typeof setInterval> | null = null
const BLIPS = [1046.5, 1174.7, 1318.5, 1568, 1760, 2093]

export function startThinking() {
  if (thinking) return
  thinking = setInterval(() => {
    if (document.hidden || Math.random() < 0.35) return
    const f = BLIPS[Math.floor(Math.random() * BLIPS.length)]
    tone(f, 0, 0.07, 0.018, 'sine')
    if (Math.random() < 0.3) tone(f * 1.5, 0.08, 0.05, 0.012, 'sine')
  }, 240)
}

export function stopThinking() {
  if (thinking) clearInterval(thinking)
  thinking = null
}

/**
 * Nhạc nền màn giới thiệu: hai nốt trầm lệch nhẹ qua bộ lọc thấp, bộ lọc mở ra đóng lại thật chậm cho nghe như máy
 * đang chạy. Rất nhỏ để không át lời người dùng đọc.
 */
let ambientWanted = false
let ambient: { stop: () => void } | null = null

export function startAmbient() {
  ambientWanted = true
  const a = ctx()
  if (!a || ambient) return
  const c = a.ctx
  const t = c.currentTime
  const out = c.createGain()
  out.gain.setValueAtTime(0.0001, t)
  out.gain.exponentialRampToValueAtTime(0.05, t + 2.5)
  const filter = c.createBiquadFilter()
  filter.type = 'lowpass'
  filter.frequency.value = 520
  filter.Q.value = 6
  filter.connect(out).connect(a.master)
  const lfo = c.createOscillator()
  const lfoGain = c.createGain()
  lfo.frequency.value = 0.06
  lfoGain.gain.value = 260
  lfo.connect(lfoGain).connect(filter.frequency)
  const oscs = [
    [110, 'sawtooth', -6],
    [110, 'sawtooth', 7],
    [164.8, 'triangle', 0],
    [55, 'sine', 0],
  ].map(([f, type, detune]) => {
    const o = c.createOscillator()
    o.type = type as OscillatorType
    o.frequency.value = f as number
    o.detune.value = detune as number
    o.connect(filter)
    return o
  })
  for (const o of [lfo, ...oscs]) o.start(t)
  ambient = {
    stop: () => {
      const now = c.currentTime
      out.gain.cancelScheduledValues(now)
      out.gain.setValueAtTime(Math.max(out.gain.value, 0.0001), now)
      out.gain.exponentialRampToValueAtTime(0.0001, now + 0.8)
      for (const o of [lfo, ...oscs]) o.stop(now + 0.85)
    },
  }
}

export function stopAmbient() {
  ambientWanted = false
  ambient?.stop()
  ambient = null
}
