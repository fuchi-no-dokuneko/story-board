#!/usr/bin/env bash
set -euo pipefail
app_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
source "$app_dir/../scripts/build-environment.sh"
shopt -s nullglob
jars=("$app_dir"/target/storyblock-cli-*.jar)
((${#jars[@]} == 1)) || { echo 'Run ./client-cli/install-local.sh first' >&2; exit 1; }
exec "$JAVA_HOME/bin/java" -jar "${jars[0]}" "$@"
