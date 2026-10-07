import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Frontend gọi /api/... sẽ được chuyển sang backend Spring Boot (backend/, cổng 8083)
    proxy: { '/api': 'http://localhost:8083' },
  },
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
