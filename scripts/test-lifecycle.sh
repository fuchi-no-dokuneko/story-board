#!/usr/bin/env bash
set -euo pipefail
repo_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
source "$repo_dir/scripts/build-environment.sh"
work=$repo_dir/.local/refactor-evidence/lifecycle
mkdir -p "$work/scripts" "$work/server" "$work/.local/storyblock"/{server,runtime,secrets}
cp "$repo_dir/scripts/"{local-server,repository-paths,server-runtime,server-process,server-control}.sh "$work/scripts/"
cp "$repo_dir/scripts/server-listener.py" "$work/scripts/"
cp -R "$repo_dir/server/config" "$work/server/"
cp --remove-destination "$repo_dir/.local/storyblock/server/application.jar" "$work/.local/storyblock/server/"
printf '%s\n' "$JAVA_HOME" >"$work/.local/storyblock/runtime/java-home"
printf '%s' 'isolated-lifecycle-test-pepper-thirty-two-bytes' >"$work/.local/storyblock/secrets/server-pepper"
printf '%s' 'isolated-lifecycle-test-owner-thirty-two-bytes' >"$work/.local/storyblock/secrets/owner-token"
export STORYBLOCK_LOCAL_PORT=19443 STORYBLOCK_PORT_POLICY=public
launcher=$work/scripts/local-server.sh
pid_file=$work/.local/storyblock/run/server.pid
cleanup() {
  "$launcher" stop >/dev/null 2>&1 || true
  [[ -z ${holder:-} ]] || kill "$holder" 2>/dev/null || true
}
trap cleanup EXIT
source "$repo_dir/scripts/test-lifecycle-cases.sh"
run_lifecycle_cases
