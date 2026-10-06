#!/bin/sh
# usage: run_sheet.sh <sheet base name, e.g. 01_screwdriver> "<what to draw>"
cd "$HOME/Documents/PCsmartcare"
mkdir -p design/tools/logs
/Applications/ChatGPT.app/Contents/Resources/codex-cli/bin/codex exec --skip-git-repo-check \
  -m gpt-6-astra -c model_reasoning_effort='"high"' -s workspace-write -C "$HOME/Documents/PCsmartcare" \
  -- "Read design/tools/brief_tool_sheet.md and follow it. Sheet: design/tools/$1 (write $1.png and $1.md under design/tools/). Draw: $2" \
  < /dev/null > "design/tools/logs/$1.log" 2>&1
echo "$1 exit $?"
