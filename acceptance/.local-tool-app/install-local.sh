#!/usr/bin/env bash
set -euo pipefail
app_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
repo_dir=$(cd "$app_dir/.." && pwd)
source "$repo_dir/scripts/build-environment.sh"
python3 "$repo_dir/scripts/assemble-sources.py"
export npm_config_cache=$app_dir/.local-tool-app/npm-cache
export NODE_OPTIONS="${NODE_OPTIONS:+$NODE_OPTIONS }--dns-result-order=ipv4first"
cp "$app_dir/package.json" "$app_dir/package-lock.json" "$app_dir/.local-tool-app/"
cd "$app_dir/.local-tool-app"
timeout 60s npm ci --ignore-scripts --no-audit --no-fund
ln -sfn .local-tool-app/node_modules "$app_dir/node_modules"
