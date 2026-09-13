#!/usr/bin/env bash
set -euo pipefail
umask 077
cd /workspace
app=${STORYBLOCK_CONTAINER_APP:-api}
case "$app" in
  api) data_dir=/workspace/server/data ;;
  style-worker) data_dir=/workspace/client-style-worker/data ;;
  llm-worker) data_dir=/workspace/client-llm-worker/data ;;
  *) exit 2 ;;
esac
mkdir -p "$data_dir/tmp"
args=()
if [[ ${STORYBLOCK_CONTAINER_APP:-api} == api ]]; then
  mkdir -p server/data/secrets
  pepper=server/data/secrets/server-pepper
  if [[ ! -s $pepper ]]; then
    od -An -N48 -tx1 /dev/urandom | tr -d ' \n' >"$pepper"
  fi
  export STORYBLOCK_SECURITY_PEPPER
  STORYBLOCK_SECURITY_PEPPER=$(<"$pepper")
  args+=(--spring.config.additional-location=file:/workspace/server/config/config.yaml,file:/workspace/server/config/port-config.yaml,file:/workspace/server/config/content-config/logging.yaml)
  args+=("--policy=${POLICY:-public}" "--port=${PORT:-8443}")
fi
exec java "-Duser.home=$data_dir" "-Djava.io.tmpdir=$data_dir/tmp" -jar /workspace/application.jar "${args[@]}" "$@"
