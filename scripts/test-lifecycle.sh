#!/usr/bin/env bash
set -euo pipefail
repo_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
source "$repo_dir/scripts/build-environment.sh"
python3 "$repo_dir/scripts/test-server-arguments.py"
work=$repo_dir/.local/refactor-evidence/lifecycle
mkdir -p "$work/scripts" "$work/server" "$work/server/data"/{server,runtime,secrets}
cp "$repo_dir/scripts/"{local-server,repository-paths,server-runtime,server-process,server-control,server-arguments}.sh "$work/scripts/"
cp "$repo_dir/scripts/server-listener.py" "$work/scripts/"
cp -R "$repo_dir/server/config" "$work/server/"
cp --remove-destination "$repo_dir/server/data/server/application.jar" "$work/server/data/server/"
printf '%s\n' "$JAVA_HOME" >"$work/server/data/runtime/java-home"
printf '%s' 'isolated-lifecycle-test-pepper-thirty-two-bytes' >"$work/server/data/secrets/server-pepper"
printf '%s' 'isolated-lifecycle-test-owner-thirty-two-bytes' >"$work/server/data/secrets/owner-token"
unset STORYBLOCK_PORT_POLICY STORYBLOCK_LOCAL_PORT
export STORYBLOCK_LOCAL_PORT=19443
launcher=$work/scripts/local-server.sh
pid_file=$work/server/data/run/server.pid
cleanup() {
  "$launcher" stop >/dev/null 2>&1 || true
  [[ -z ${holder:-} ]] || kill "$holder" 2>/dev/null || true
}
trap cleanup EXIT
source "$repo_dir/scripts/test-lifecycle-cases.sh"
run_lifecycle_cases
source "$repo_dir/scripts/test-server-options-cases.sh"
run_server_options_cases
