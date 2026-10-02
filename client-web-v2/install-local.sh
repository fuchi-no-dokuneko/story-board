#!/usr/bin/env bash
set -euo pipefail
repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)
python3 "$repo_dir/scripts/assemble-sources.py"
echo 'Browser assets ready / 瀏覽器資源已建立'
