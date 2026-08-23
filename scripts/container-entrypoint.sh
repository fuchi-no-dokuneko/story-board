#!/usr/bin/env bash
set -euo pipefail

load_secret() {
  local variable=$1
  local file_variable="${variable}_FILE"
  local file=${!file_variable:-}
  if [[ -n "$file" ]]; then
    if [[ ! -f "$file" ]]; then
      echo "Secret file is unavailable: $file_variable" >&2
      exit 1
    fi
    printf -v "$variable" '%s' "$(<"$file")"
    export "$variable"
    unset "$file_variable"
  fi
}

load_secret STORYBLOCK_SECURITY_OWNER_TOKEN
load_secret STORYBLOCK_SECURITY_PEPPER
load_secret STORYBLOCK_WORKER_TOKEN
load_secret STORYBLOCK_LLM_WORKER_MODEL_TOKEN

exec java -Djava.net.preferIPv4Stack=true -jar /app/application.jar "$@"
