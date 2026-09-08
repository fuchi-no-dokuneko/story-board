#!/usr/bin/env bash
set -euo pipefail
repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)
export STORYBLOCK_TRUSTED_LAN_ENABLED=true
exec "$repo_dir/scripts/local-server.sh" run "$@"
