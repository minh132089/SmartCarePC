#!/bin/sh
# usage: run_sheet.sh <relative sheet path without extension, e.g. 04_mini_sff/01_case> "<what to draw>" [concept image]
cd "$HOME/Documents/PCsmartcare"
log="design/families/logs/$(echo "$1" | tr '/' '_').log"
mkdir -p design/families/logs "design/families/$(dirname "$1")"
img=""
[ -n "$3" ] && img="-i $3"
/Applications/ChatGPT.app/Contents/Resources/codex-cli/bin/codex exec --skip-git-repo-check \
  -m gpt-6-astra -c model_reasoning_effort='"high"' -s workspace-write -C "$HOME/Documents/PCsmartcare" \
  $img -- "Read design/families/brief_sheet.md and follow it. Sheet: design/families/$1 (write $1.png and $1.md under design/families/). Draw: $2" \
  < /dev/null > "$log" 2>&1
echo "$1 exit $?"
