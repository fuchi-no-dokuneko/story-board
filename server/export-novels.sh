#!/usr/bin/env bash
set -euo pipefail
repo_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
export PYTHONDONTWRITEBYTECODE=1
exec python3 "$repo_dir/server/scripts/export_novels.py" "$@"
