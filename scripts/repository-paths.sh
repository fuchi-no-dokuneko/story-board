#!/usr/bin/env bash
repository_path() {
  local resolved
  resolved=$(realpath -m -- "$1")
  case "$resolved" in
    "$repo_dir"|"$repo_dir"/*) printf '%s\n' "$resolved" ;;
    *) echo 'Path must remain inside this repository' >&2; return 1 ;;
  esac
}
check_repository_state() {
  repository_path "$repo_dir/.local" >/dev/null
  repository_path "$repo_dir/.local-tool" >/dev/null
}
