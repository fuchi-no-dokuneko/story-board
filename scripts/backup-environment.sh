#!/usr/bin/env bash
backup_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
source "$backup_root/scripts/build-environment.sh"
backup_secret_dir=$(repository_path "$repo_dir/.local/storyblock/secrets")
mkdir -p "$(repository_path "$backup_secret_dir/backup-artifacts")"
backup_key() {
  local path
  path=$(repository_path "$1")
  (
    umask 077
    exec 8>"$(repository_path "$path.lock")"
    flock 8
    if [[ ! -s $path ]]; then
      openssl rand -base64 48 >"$(repository_path "$path.pending")"
      mv "$path.pending" "$path"
    fi
  )
  printf '%s\n' "$path"
}
lock_backup_directory() {
  mkdir -p "$1"
  exec 7>"$(repository_path "$1/.backup.lock")"
  flock 7
}
key_for_artifact() {
  local stored=$backup_secret_dir/backup-artifacts/$(basename -- "$1").key
  if [[ -s $stored ]]; then repository_path "$stored"
  else repository_path "$backup_secret_dir/backup.key"; fi
}
