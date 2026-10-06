#!/bin/sh
# usage: run_review.sh NN id   (renders must exist in design/gaming/review/<id>_*.png)
cd "$HOME/Documents/PCsmartcare"
imgs=""
for v in front iso iso2 back; do imgs="$imgs -i design/gaming/review/$2_$v.png"; done
/Applications/ChatGPT.app/Contents/Resources/codex-cli/bin/codex exec --skip-git-repo-check \
  -m gpt-6-astra -c model_reasoning_effort='"high"' -s workspace-write -C "$HOME/Documents/PCsmartcare" \
  $imgs -- "Read design/gaming/review_brief.md and follow it. Part NN=$1, id=$2. The attached images are the worker's renders of the model (front, iso, iso2, back in that order)." \
  < /dev/null > "design/gaming/review/codex_$2.log" 2>&1
echo "review $2 exit $?"
