#!/usr/bin/env bash
set -euo pipefail
umask 077
repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)
local_dir=$repo_dir/.local/storyblock
source "$repo_dir/scripts/repository-paths.sh"
check_repository_state
repository_path "$local_dir" >/dev/null
jar=$local_dir/server/application.jar
pid_file=$local_dir/run/server.pid
log_file=$local_dir/logs/server.log
mkdir -p "$local_dir/run" "$local_dir/logs"
source "$repo_dir/scripts/server-process.sh"
case "${1:-status}" in
  run) source "$repo_dir/scripts/server-runtime.sh"; run_server ;;
  start|stop|status)
    exec 9>"$local_dir/run/control.lock"
    flock 9
    source "$repo_dir/scripts/server-control.sh"
    "server_${1:-status}" ;;
  logs) exec tail -n 200 -f "$log_file" ;;
  *) echo 'Usage: local-server.sh {start|stop|status|logs|run}' >&2; exit 2 ;;
esac
