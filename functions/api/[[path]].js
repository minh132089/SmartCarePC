// Cloudflare Pages Function: chuyển mọi request /api/... về backend chạy trên máy demo qua Cloudflare Tunnel.
// Địa chỉ tunnel đổi mỗi lần chạy nên tools/demo.sh ghi lại file _backend.js rồi deploy lại.
import { BACKEND } from '../_backend.js'

export async function onRequest({ request }) {
  if (!BACKEND) return offline()
  const url = new URL(request.url)
  const headers = new Headers(request.headers)
  // Cùng nguồn với trang nên không cần CORS; bỏ Origin để backend (chỉ cho phép localhost) không chặn
  headers.delete('origin')
  headers.delete('host')
  try {
    const res = await fetch(BACKEND + url.pathname + url.search, {
      method: request.method,
      headers,
      body: request.method === 'GET' || request.method === 'HEAD' ? undefined : request.body,
      redirect: 'manual',
    })
    // Tunnel đã tắt: Cloudflare trả trang lỗi HTML 530 / 502 / 1033. Lỗi JSON là của backend (vd dịch vụ AI lỗi): chuyển nguyên
    const json = (res.headers.get('content-type') ?? '').includes('json')
    if ((res.status === 530 || res.status === 502) && !json) return offline()
    return new Response(res.body, res)
  } catch {
    return offline()
  }
}

function offline() {
  return Response.json(
    { message: 'Máy chủ demo đang tắt nên trợ lý và feedback tạm thời không dùng được. Phần xem mô hình 3D vẫn dùng bình thường.' },
    { status: 503 },
  )
}
