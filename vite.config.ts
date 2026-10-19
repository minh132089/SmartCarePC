import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig, type ProxyOptions } from 'vite'

/**
 * Frontend và /api cùng một địa chỉ (đi qua proxy này) nên không phải gọi khác nguồn.
 * Bỏ header Origin: mở bằng địa chỉ khác localhost (link tunnel khi demo) thì backend không chặn vì CORS.
 */
const apiProxy: ProxyOptions = {
  target: 'http://localhost:8083',
  configure: (proxy) => proxy.on('proxyReq', (req) => req.removeHeader('origin')),
}

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Frontend gọi /api/... sẽ được chuyển sang backend Spring Boot (backend/, cổng 8083)
    proxy: { '/api': apiProxy },
  },
  // Bản build chạy bằng `vite preview` (demo qua Cloudflare Tunnel) cũng chuyển /api sang backend
  preview: { proxy: { '/api': apiProxy } },
  build: {
    // three và bộ R3F tự thân đã ~750–850 kB, không chia nhỏ hơn được; code app chỉ ~100 kB
    chunkSizeWarningLimit: 900,
    rolldownOptions: {
      output: {
        // Tách thư viện ra file riêng: trình duyệt tải song song và giữ cache khi code app đổi
        codeSplitting: {
          groups: [
            { name: 'three', test: /node_modules[\\/]three[\\/]/ },
            { name: 'r3f', test: /node_modules[\\/](@react-three|@react-spring|three-stdlib|camera-controls|maath|troika)/ },
          ],
        },
      },
    },
  },
})
