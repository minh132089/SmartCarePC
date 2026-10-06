#!/bin/sh
# Runs every line of parts.tsv through run_part.sh, 4 at a time.
cd "$(dirname "$0")"
running=0
while IFS="$(printf '\t')" read -r nn id desc; do
  ./run_part.sh "$nn" "$id" "$desc" &
  running=$((running + 1))
  if [ "$running" -ge 4 ]; then wait; running=0; fi
done < parts.tsv
wait
