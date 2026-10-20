import { create } from 'zustand'

/**
 * Hiệu ứng âm thanh của app, tổng hợp bằng Web Audio (không tải file nào): tiếng gửi câu hỏi, tiếng lách tách khi
 * trợ lý bắt đầu nghĩ, tiếng chuông khi trả lời xong, tiếng tick khi đánh dấu xong bước lắp. Trình duyệt chỉ cho phát
 * tiếng sau lần bấm / gõ phím đầu tiên, nên AudioContext tạo lúc đó.
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

/**
 * Trợ lý bắt đầu nghĩ: một chuỗi lách tách ngắn như máy đang tính, phát đúng một lần rồi thôi (không lặp suốt lúc chờ)
 */
export function playThinking() {
  const notes = [1046.5, 1568, 1318.5, 2093, 1760]
  notes.forEach((f, i) => tone(f, 0.32 + i * 0.075, 0.06, 0.03, 'sine'))
}
