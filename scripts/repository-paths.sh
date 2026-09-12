#!/usr/bin/env bash
repository_path() {
  local resolved logical
  logical=$(realpath -ms -- "$1")
  resolved=$(realpath -m -- "$1")
  local application
  for application in server client-cli client-style-worker client-llm-worker plugin; do
    case "$logical" in
      "$repo_dir/$application/data"|"$repo_dir/$application/data"/*) printf '%s\n' "$resolved"; return ;;
    esac
  done
  case "$resolved" in
    "$repo_dir"|"$repo_dir"/*) printf '%s\n' "$resolved" ;;
    *) echo 'Path must remain inside this repository' >&2; return 1 ;;
  esac
}
check_repository_state() {
  repository_path "$repo_dir/.local" >/dev/null
  repository_path "$repo_dir/.local-tool" >/dev/null
}
