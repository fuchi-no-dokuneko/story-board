#!/usr/bin/env bash
set -euo pipefail
repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
source "$repo_dir/scripts/build-environment.sh"
maven=$MAVEN_USER_HOME/apache-maven-3.9.11/bin/mvn
if [[ ! -x $maven ]]; then
  "$repo_dir/install-local-build.sh"
fi
exec "$maven" -Dmaven.repo.local="$MAVEN_USER_HOME/repository" "$@"
