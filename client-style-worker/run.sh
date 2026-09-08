#!/usr/bin/env bash
set -euo pipefail
app_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repo_dir=$(cd "$app_dir/.." && pwd)
source "$repo_dir/scripts/build-environment.sh"
shopt -s nullglob
jars=("$app_dir"/target/storyblock-*.jar)
((${#jars[@]} == 1)) || { echo 'Run ./client-style-worker/install-local.sh first' >&2; exit 1; }
exec "$JAVA_HOME/bin/java" -jar "${jars[0]}" \
  "--spring.config.additional-location=file:$app_dir/config/config.yaml" "$@"
