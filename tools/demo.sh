#!/bin/sh
# Chạy demo cho người khác mở qua mạng: MySQL, backend, bản build, một link https tạm (Cloudflare Tunnel)
# và cập nhật link ngắn cố định trên Cloudflare Pages (giao diện ở Pages, /api chuyển về máy này qua tunnel).
# Cách dùng: sh tools/demo.sh   — Ctrl+C để tắt hết. Cần đăng nhập một lần: npx wrangler@4 login
# Máy phải bật và không ngủ suốt buổi demo (script tự giữ máy thức).
cd "$(dirname "$0")/.." || exit 1
LOG="${TMPDIR:-/tmp}/pcsmartcare-demo"
mkdir -p "$LOG"
PIDS=""
MYSQL_STARTED=""
DATADIR="$(brew --prefix)/var/mysql"
PAGES_PROJECT=smartcarepc
WRANGLER="npx --yes wrangler@4.151.1"

say() { printf '\n== %s\n' "$1"; }
port_up() { lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1; }
wait_port() { i=0; while ! port_up "$1"; do i=$((i + 1)); [ $i -gt "$2" ] && return 1; sleep 1; done; }

cleanup() {
  trap - INT TERM EXIT
  say "Đang tắt demo..."
  kill $PIDS 2>/dev/null
  lsof -ti tcp:8083 -sTCP:LISTEN | xargs kill 2>/dev/null
  lsof -ti tcp:4173 -sTCP:LISTEN | xargs kill 2>/dev/null
  [ -n "$MYSQL_STARTED" ] && kill "$(cat "$DATADIR"/*.pid 2>/dev/null)" 2>/dev/null
  echo "Đã tắt."
}
trap cleanup INT TERM EXIT

for p in 8083 4173; do
  if port_up $p; then echo "Cổng $p đang bị chiếm (app cũ còn chạy?). Tắt nó trước rồi chạy lại."; exit 1; fi
done

say "1/6 MySQL"
if port_up 3306; then echo "Đã chạy sẵn."; else
  "$(brew --prefix)/opt/mysql/bin/mysqld_safe" --datadir="$DATADIR" >"$LOG/mysql.log" 2>&1 &
  PIDS="$PIDS $!"; MYSQL_STARTED=1
  wait_port 3306 30 || { echo "MySQL không lên, xem $LOG/mysql.log"; exit 1; }
fi

say "2/6 Backend (cổng 8083)"
(cd backend && exec sh mvnw -q spring-boot:run) >"$LOG/backend.log" 2>&1 &
PIDS="$PIDS $!"

say "3/6 Build giao diện"
npm run build >"$LOG/build.log" 2>&1 || { echo "Build lỗi, xem $LOG/build.log"; exit 1; }

say "4/6 Chạy bản build (cổng 4173)"
npx vite preview --port 4173 --strictPort >"$LOG/preview.log" 2>&1 &
PIDS="$PIDS $!"
wait_port 4173 30 || { echo "Không chạy được bản build, xem $LOG/preview.log"; exit 1; }
wait_port 8083 120 || { echo "Backend không lên, xem $LOG/backend.log"; exit 1; }

say "5/6 Mở link công khai"
: >"$LOG/tunnel.log"
cloudflared tunnel --no-autoupdate --url http://localhost:4173 --http-host-header localhost:4173 >"$LOG/tunnel.log" 2>&1 &
PIDS="$PIDS $!"
URL=""
i=0
while [ -z "$URL" ] && [ $i -lt 40 ]; do
  URL=$(grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' "$LOG/tunnel.log" | head -1)
  i=$((i + 1)); sleep 1
done
[ -n "$URL" ] || { echo "Không lấy được link, xem $LOG/tunnel.log"; exit 1; }

say "6/6 Cập nhật link ngắn trên Cloudflare Pages"
printf "// Ghi lại bởi tools/demo.sh, không commit\nexport const BACKEND = '%s'\n" "$URL" >functions/_backend.js
SHORT=""
if $WRANGLER pages deploy dist --project-name "$PAGES_PROJECT" --branch main --commit-dirty=true >"$LOG/pages.log" 2>&1; then
  SHORT="https://$PAGES_PROJECT.pages.dev"
else
  echo "Không cập nhật được link ngắn (xem $LOG/pages.log). Vẫn dùng được link tạm bên dưới."
fi

# Giữ máy thức (không ngủ, không tắt màn hình) cho tới khi tắt demo
caffeinate -dims &
PIDS="$PIDS $!"

printf '\n============================================\n'
[ -n "$SHORT" ] && printf '  Link demo:  %s\n' "$SHORT"
printf '  Link dự phòng: %s\n' "$URL"
printf '  (đợi ~10 giây để link sẵn sàng; Ctrl+C để tắt)\n'
printf '============================================\n'
wait
