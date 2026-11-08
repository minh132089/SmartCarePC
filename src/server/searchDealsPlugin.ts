/**
 * Vite plugin: cung cấp endpoint GET /api/search-deals?q=...&max=5
 * Chạy phía server (Node.js) nên không bị CORS khi scrape.
 * Ưu tiên DuckDuckGo (chuẩn kết quả Việt Nam, MemoryZone, GearVN, Shopee),
 * fallback sang DDG Lite hoặc Bing nếu cần.
 */
import type { Plugin } from 'vite'
import type { IncomingMessage, ServerResponse } from 'node:http'
import { searchDdgHtml, searchDdgLite } from './engines/ddg.js'
import { searchBing } from './engines/bing.js'
import type { Source } from './engines/common.js'

export interface SearchDealsResult {
  sources: Source[]
  provider: 'ddg' | 'ddg-lite' | 'bing'
  error?: string
}

export function searchDealsPlugin(): Plugin {
  return {
    name: 'search-deals-middleware',
    configureServer(server) {
      server.middlewares.use(
        '/api/search-deals',
        async (req: IncomingMessage, res: ServerResponse) => {
          if (req.method !== 'GET') {
            res.writeHead(405, { 'Content-Type': 'application/json' })
            res.end(JSON.stringify({ error: 'Method Not Allowed' }))
            return
          }

          const urlObj = new URL(req.url ?? '/', 'http://localhost')
          const q = urlObj.searchParams.get('q') ?? ''
          const max = Math.min(Number(urlObj.searchParams.get('max') ?? '4'), 10)

          if (!q.trim()) {
            res.writeHead(400, { 'Content-Type': 'application/json' })
            res.end(JSON.stringify({ error: 'Missing query param ?q=' }))
            return
          }

          let sources: Source[] = []
          let provider: SearchDealsResult['provider'] = 'ddg'

          // 1. Thử DuckDuckGo HTML trước
          try {
            const ddgRes = await searchDdgHtml(q, max, { region: 'vn-vi' })
            sources = ddgRes.sources
            provider = 'ddg'
          } catch {
            sources = []
          }

          // 2. Nếu DDG HTML ít kết quả hoặc lỗi, thử DDG Lite
          if (sources.length < 2) {
            try {
              const liteRes = await searchDdgLite(q, max, { region: 'vn-vi' })
              if (liteRes.sources.length > 0) {
                sources = liteRes.sources
                provider = 'ddg-lite'
              }
            } catch {
              // Bỏ qua lỗi lite
            }
          }

          // 3. Nếu vẫn không có kết quả, thử Bing
          if (sources.length === 0) {
            try {
              const bingRes = await searchBing(q, max)
              sources = bingRes.sources
              provider = 'bing'
            } catch (err) {
              const msg = err instanceof Error ? err.message : String(err)
              res.writeHead(502, { 'Content-Type': 'application/json' })
              res.end(JSON.stringify({ sources: [], provider: 'bing', error: msg } satisfies SearchDealsResult))
              return
            }
          }

          const result: SearchDealsResult = {
            sources: sources.slice(0, max),
            provider,
          }

          res.writeHead(200, {
            'Content-Type': 'application/json; charset=utf-8',
            'Cache-Control': 'public, max-age=300',
          })
          res.end(JSON.stringify(result))
        },
      )
    },
  }
}
