#!/usr/bin/env bash
set -euo pipefail
app_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
source "$app_dir/../scripts/build-environment.sh"
shopt -s nullglob
jars=("$app_dir"/target/storyblock-style-worker-*.jar)
((${#jars[@]} == 1)) || { echo 'Run ./client-style-worker/install-local.sh first / 請先建置文風工作器' >&2; exit 1; }
exec "$JAVA_HOME/bin/java" -Djava.net.preferIPv4Stack=true -jar "${jars[0]}" --quality "$@"
