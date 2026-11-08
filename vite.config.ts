import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig, type ProxyOptions } from 'vite'
import { searchDealsPlugin } from './src/server/searchDealsPlugin.js'

/**
 * Frontend vÃ  /api cÃ¹ng má»™t Ä‘á»‹a chá»‰ (Ä‘i qua proxy nÃ y) nÃªn khÃ´ng pháº£i gá»i khÃ¡c nguá»“n.
 * Bá» header Origin: má»Ÿ báº±ng Ä‘á»‹a chá»‰ khÃ¡c localhost (link tunnel khi demo) thÃ¬ backend khÃ´ng cháº·n vÃ¬ CORS.
 */
const apiProxy: ProxyOptions = {
  target: 'http://localhost:8083',
  configure: (proxy) => proxy.on('proxyReq', (req) => req.removeHeader('origin')),
}

export default defineConfig({
  plugins: [searchDealsPlugin(), react(), tailwindcss()],
  server: {
    // Frontend gá»i /api/... sáº½ Ä‘Æ°á»£c chuyá»ƒn sang backend Spring Boot (backend/, cá»•ng 8083)
    proxy: {
      // /api/search-deals Ä‘Æ°á»£c xá»­ lÃ½ bá»Ÿi searchDealsPlugin, khÃ´ng forward sang Spring Boot
      '/api/chat': apiProxy,
      '/api/pc': apiProxy,
      '/api/recommend': apiProxy,
    },
  },
  // Báº£n build cháº¡y báº±ng `vite preview` (demo qua Cloudflare Tunnel) cÅ©ng chuyá»ƒn /api sang backend
  preview: { proxy: { '/api': apiProxy } },
  build: {
    // three vÃ  bá»™ R3F tá»± thÃ¢n Ä‘Ã£ ~750â€“850 kB, khÃ´ng chia nhá» hÆ¡n Ä‘Æ°á»£c; code app chá»‰ ~100 kB
    chunkSizeWarningLimit: 900,
    rolldownOptions: {
      output: {
        // TÃ¡ch thÆ° viá»‡n ra file riÃªng: trÃ¬nh duyá»‡t táº£i song song vÃ  giá»¯ cache khi code app Ä‘á»•i
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
