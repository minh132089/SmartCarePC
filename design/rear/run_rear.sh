#!/bin/sh
# usage: run_rear.sh <sheet name> "<which case>"
cd "$HOME/Documents/PCsmartcare"
mkdir -p design/rear/logs
/Applications/ChatGPT.app/Contents/Resources/codex-cli/bin/codex exec --skip-git-repo-check \
  -m gpt-6-astra -c model_reasoning_effort='"high"' -s workspace-write -C "$HOME/Documents/PCsmartcare" \
  -i design/rear/ref_rear.png -i design/rear/current_standard_rear.png \
  -- "Read design/rear/brief_rear.md and follow it. Sheet: design/rear/$1 (write $1.png and $1.md under design/rear/). Case: $2" \
  < /dev/null > "design/rear/logs/$1.log" 2>&1
echo "$1 exit $?"
