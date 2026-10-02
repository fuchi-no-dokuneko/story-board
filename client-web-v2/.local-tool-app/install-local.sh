#!/usr/bin/env bash
set -euo pipefail
app_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)
repo_dir=$(cd "$app_dir/.." && pwd -P)
source "$repo_dir/scripts/build-environment.sh"
export npm_config_cache=$app_dir/.local-tool-app/npm-cache
export NODE_OPTIONS="${NODE_OPTIONS:+$NODE_OPTIONS }--dns-result-order=ipv4first"
npm install --prefix "$app_dir/.local-tool-app" --save-exact --ignore-scripts \
  --no-audit --no-fund puppeteer-core@25.12.0
