#!/usr/bin/env bash
set -euo pipefail
repo_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
source "$repo_dir/scripts/repository-paths.sh"
for app in server client-style-worker client-llm-worker; do
  directory=$(repository_path "$repo_dir/$app/data")
  mkdir -p "$directory"
done
export STORYBLOCK_UID=$(id -u)
export STORYBLOCK_GID=$(id -g)
cd "$repo_dir"
exec docker compose "$@"
