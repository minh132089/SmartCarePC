/**
 * Vite plugin: cung cáº¥p endpoint GET /api/search-deals?q=...&max=5
 * Cháº¡y phÃ­a server (Node.js) nÃªn khÃ´ng bá»‹ CORS khi scrape Bing/DDG.
 * Thá»­ Bing trÆ°á»›c, fallback DDG náº¿u Bing tráº£ vá» Ã­t káº¿t quáº£ hÆ¡n 2.
 */
import type { Plugin } from 'vite'
import type { IncomingMessage, ServerResponse } from 'node:http'
import { searchBing } from './engines/bing.js'
import { searchDdgHtml } from './engines/ddg.js'
import type { Source } from './engines/common.js'

export interface SearchDealsResult {
  sources: Source[]
  provider: 'bing' | 'ddg' | 'combined'
  error?: string
}

export function searchDealsPlugin(): Plugin {
  return {
    name: 'search-deals-middleware',
    configureServer(server) {
      server.middlewares.use(
        '/api/search-deals',
        async (req: IncomingMessage, res: ServerResponse) => {
          // Chá»‰ nháº­n GET
          if (req.method !== 'GET') {
            res.writeHead(405, { 'Content-Type': 'application/json' })
            res.end(JSON.stringify({ error: 'Method Not Allowed' }))
            return
          }

          const urlObj = new URL(req.url ?? '/', 'http://localhost')
          const q = urlObj.searchParams.get('q') ?? ''
          const max = Math.min(Number(urlObj.searchParams.get('max') ?? '5'), 10)

          if (!q.trim()) {
            res.writeHead(400, { 'Content-Type': 'application/json' })
            res.end(JSON.stringify({ error: 'Missing query param ?q=' }))
            return
          }

          let sources: Source[] = []
          let provider: SearchDealsResult['provider'] = 'bing'

          try {
            const bingResult = await searchBing(q, max, { bingMarket: 'vi-VN' })
            sources = bingResult.sources
            provider = 'bing'

            // Náº¿u Bing Ã­t káº¿t quáº£, bá»• sung DDG
            if (sources.length < 2) {
              try {
                const ddgResult = await searchDdgHtml(q, max, { region: 'vn-vi' })
                // Merge, dedup by URL
                const seen = new Set(sources.map((s) => s.url))
                for (const s of ddgResult.sources) {
                  if (!seen.has(s.url)) {
                    sources.push(s)
                    seen.add(s.url)
                  }
                }
                provider = 'combined'
              } catch {
                // DDG fallback tháº¥t báº¡i â€” giá»¯ Bing náº¿u cÃ³
              }
            }
          } catch {
            // Bing tháº¥t báº¡i â†’ thá»­ DDG
            try {
              const ddgResult = await searchDdgHtml(q, max, { region: 'vn-vi' })
              sources = ddgResult.sources
              provider = 'ddg'
            } catch (err2) {
              res.writeHead(502, { 'Content-Type': 'application/json' })
              const msg = err2 instanceof Error ? err2.message : String(err2)
              res.end(JSON.stringify({ sources: [], provider: 'ddg', error: msg } satisfies SearchDealsResult))
              return
            }
          }

          const result: SearchDealsResult = {
            sources: sources.slice(0, max),
            provider,
          }

          res.writeHead(200, {
            'Content-Type': 'application/json',
            'Cache-Control': 'public, max-age=300', // cache 5 phÃºt
          })
          res.end(JSON.stringify(result))
        },
      )
    },
  }
}
