/**
 * Vite plugin: cung cấp endpoint GET /api/search-deals?q=...&max=4
 * Chạy phía server (Node.js) nên không bị CORS khi tìm kiếm.
 * Tích hợp từ dsh-free-web-search:
 * 1. searchAnysearch (API tìm kiếm miễn phí, không cần key, không bao giờ bị captcha 202)
 * 2. Fallback sang DuckDuckGo / Bing nếu cần.
 */
import type { Plugin } from 'vite'
import type { IncomingMessage, ServerResponse } from 'node:http'
import { searchAnysearch } from './engines/anysearch.js'
import { searchDdgHtml, searchDdgLite } from './engines/ddg.js'
import { searchBing } from './engines/bing.js'
import type { Source } from './engines/common.js'

export interface SearchDealsResult {
  sources: Source[]
  provider: 'anysearch' | 'ddg' | 'ddg-lite' | 'bing'
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
          let provider: SearchDealsResult['provider'] = 'anysearch'

          // 1. Thử Anysearch trước (miễn phí, nhanh, không bị anti-bot)
          try {
            const anyRes = await searchAnysearch(q, max)
            if (anyRes.sources.length > 0) {
              sources = anyRes.sources
              provider = 'anysearch'
            }
          } catch {
            sources = []
          }

          // 2. Nếu Anysearch ít kết quả, thử DDG
          if (sources.length === 0) {
            try {
              const ddgRes = await searchDdgHtml(q, max, { region: 'vn-vi' })
              if (ddgRes.sources.length > 0) {
                sources = ddgRes.sources
                provider = 'ddg'
              }
            } catch {
              try {
                const liteRes = await searchDdgLite(q, max, { region: 'vn-vi' })
                if (liteRes.sources.length > 0) {
                  sources = liteRes.sources
                  provider = 'ddg-lite'
                }
              } catch {
                // Ignore
              }
            }
          }

          // 3. Fallback Bing
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


          // Lọc kết quả: loại bỏ các link mạng xã hội, wikipedia, link category chung chung
          const validSources = sources.filter((s) => {
            const u = s.url.toLowerCase();
            if (u.includes('facebook.com') || u.includes('wikipedia.org') || u.includes('ebay.com') || u.includes('reddit.com')) {
              return false;
            }
            if (u.endsWith('/c/') || u.includes('/list/') || u.includes('?q=') || u.includes('/search')) {
              return false;
            }
            return true;
          });
          const finalSources = validSources.length >= 2 ? validSources : sources;

          const result: SearchDealsResult = {
            sources: finalSources.slice(0, max),
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
