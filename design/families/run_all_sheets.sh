#!/bin/sh
# Chạy mọi dòng trong sheets.tsv qua run_sheet.sh, 4 bản một lúc. Bỏ qua sheet đã có PNG.
cd "$(dirname "$0")"
running=0
while IFS="$(printf '\t')" read -r path desc concept; do
  [ -f "$path.png" ] && continue
  ./run_sheet.sh "$path" "$desc" "$concept" &
  running=$((running + 1))
  if [ "$running" -ge 4 ]; then wait; running=0; fi
done < sheets.tsv
wait
