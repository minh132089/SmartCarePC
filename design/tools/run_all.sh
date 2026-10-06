#!/bin/sh
# Chạy mọi dòng trong sheets.tsv, 4 bản một lúc, bỏ qua sheet đã có PNG.
cd "$(dirname "$0")"
running=0
while IFS="$(printf '\t')" read -r name desc; do
  [ -f "$name.png" ] && continue
  ./run_sheet.sh "$name" "$desc" &
  running=$((running + 1))
  if [ "$running" -ge 4 ]; then wait; running=0; fi
done < sheets.tsv
wait
