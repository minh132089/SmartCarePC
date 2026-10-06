#!/bin/sh
# usage: run_part.sh NN id "description"
cd "$HOME/Documents/PCsmartcare"
/Applications/ChatGPT.app/Contents/Resources/codex-cli/bin/codex exec --skip-git-repo-check \
  -m gpt-6-astra -c model_reasoning_effort='"high"' -s workspace-write -C "$HOME/Documents/PCsmartcare" \
  "Read design/gaming/brief_parts.md and design/gaming/00_blueprint.md. Your task: part NN=$1, id=$2. Draw: $3. Save design/gaming/$1_$2.png and design/gaming/$1_$2.md." \
  < /dev/null > "design/gaming/codex_$1_$2.log" 2>&1
echo "$1_$2 exit $?"
