#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: scripts/prune-backups.sh <backup-directory> [--apply]" >&2
  exit 2
fi

directory=$(realpath "$1")
apply=${2:-}
if [[ "$apply" != "" && "$apply" != "--apply" ]]; then
  echo "Second argument must be --apply" >&2
  exit 2
fi

mapfile -t artifacts < <(
  find "$directory" -maxdepth 1 -type f \
    -name 'storyblock-????????T??????Z.db.zst.enc' -printf '%f\n' | sort -r
)

declare -A keep=()
declare -A daily=()
declare -A weekly=()
now=$(date -u +%s)

for index in "${!artifacts[@]}"; do
  artifact=${artifacts[$index]}
  stamp=${artifact#storyblock-}
  stamp=${stamp%.db.zst.enc}
  epoch=$(date -u -d "${stamp:0:8} ${stamp:9:2}:${stamp:11:2}:${stamp:13:2}" +%s)
  age_days=$(((now - epoch) / 86400))

  if ((index < 48)); then
    keep[$artifact]=1
  fi
  day=${stamp:0:8}
  if ((age_days < 30)) && [[ -z ${daily[$day]:-} ]]; then
    daily[$day]=1
    keep[$artifact]=1
  fi
  week=$(date -u -d "@${epoch}" +%G-%V)
  if ((age_days < 84)) && [[ -z ${weekly[$week]:-} ]]; then
    weekly[$week]=1
    keep[$artifact]=1
  fi
done

for artifact in "${artifacts[@]}"; do
  if [[ -z ${keep[$artifact]:-} ]]; then
    printf '%s\n' "$artifact"
    if [[ "$apply" == "--apply" ]]; then
      rm -f -- \
        "$directory/$artifact" \
        "$directory/$artifact.sha256" \
        "$directory/$artifact.json"
    fi
  fi
done
