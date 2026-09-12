#!/usr/bin/env bash
# Sourced by repository-local build entry points.
build_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)
repo_dir=$build_root
source "$build_root/scripts/repository-paths.sh"
check_repository_state
export MAVEN_USER_HOME=$build_root/.local-tool/maven
export TMPDIR=$build_root/.local/tmp
export XDG_CACHE_HOME=$build_root/.local-tool/cache
export npm_config_update_notifier=false
export npm_config_cache=$build_root/.local-tool/npm-cache
mkdir -p "$MAVEN_USER_HOME/repository" "$TMPDIR" "$XDG_CACHE_HOME"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.net.preferIPv4Stack=true -Djava.io.tmpdir=$TMPDIR"
if [[ -z ${JAVA_HOME:-} ]]; then
  java_bin=$(readlink -f "$(command -v java)")
  export JAVA_HOME=${java_bin%/bin/java}
fi
export PATH=$JAVA_HOME/bin:$PATH
