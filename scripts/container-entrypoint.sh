#!/usr/bin/env bash
set -euo pipefail
umask 077
cd /workspace
mkdir -p .local/tmp
args=()
if [[ ${STORYBLOCK_CONTAINER_APP:-api} == api ]]; then
  mkdir -p .local/storyblock/{data,secrets}
  pepper=.local/storyblock/secrets/server-pepper
  if [[ ! -s $pepper ]]; then
    od -An -N48 -tx1 /dev/urandom | tr -d ' \n' >"$pepper"
  fi
  export STORYBLOCK_SECURITY_PEPPER
  STORYBLOCK_SECURITY_PEPPER=$(<"$pepper")
  args+=(--spring.config.additional-location=file:/workspace/server/config/config.yaml,file:/workspace/server/config/port-config.yaml)
  args+=("--policy=${POLICY:-public}" "--port=${PORT:-8443}")
fi
exec java -jar /workspace/application.jar "${args[@]}" "$@"
