#!/usr/bin/env bash
backup_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
source "$backup_root/scripts/build-environment.sh"
lock_backup_directory() {
  mkdir -p "$1"
  exec 7>"$(repository_path "$1/.backup.lock")"
  flock 7
}
