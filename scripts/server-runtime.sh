#!/usr/bin/env bash
run_server() {
  [[ -r $jar ]] || { echo 'Run ./install.sh first' >&2; exit 1; }
  [[ -s $local_dir/runtime/java-home ]] || { echo 'Installed Java metadata missing' >&2; exit 1; }
  export JAVA_HOME
  JAVA_HOME=$(<"$local_dir/runtime/java-home")
  [[ -x $JAVA_HOME/bin/java ]] || { echo 'Installed Java runtime unavailable' >&2; exit 1; }
  export TMPDIR=$repo_dir/.local/tmp
  mkdir -p "$TMPDIR"
  export STORYBLOCK_DATABASE_PATH=${STORYBLOCK_DATABASE_PATH:-$local_dir/data/storyblock.db}
  repository_path "$STORYBLOCK_DATABASE_PATH" >/dev/null
  export STORYBLOCK_TRUSTED_LAN_ENABLED=${STORYBLOCK_TRUSTED_LAN_ENABLED:-true}
  export STORYBLOCK_SECURITY_PEPPER
  STORYBLOCK_SECURITY_PEPPER=$(<"$local_dir/secrets/server-pepper")
  export STORYBLOCK_SECURITY_OWNER_TOKEN
  STORYBLOCK_SECURITY_OWNER_TOKEN=$(<"$local_dir/secrets/owner-token")
  args=("--spring.config.additional-location=file:$repo_dir/server/config/config.yaml,file:$repo_dir/server/config/port-config.yaml,file:$repo_dir/server/config/content-config/logging.yaml")
  [[ -z ${STORYBLOCK_PORT_POLICY:-} ]] || args+=("--policy=$STORYBLOCK_PORT_POLICY")
  [[ -z ${STORYBLOCK_LOCAL_PORT:-} ]] || args+=("--port=$STORYBLOCK_LOCAL_PORT")
  cd "$repo_dir"
  exec "$JAVA_HOME/bin/java" -Djava.net.preferIPv4Stack=true \
    "-Dstoryblock.root=$repo_dir" "-Djava.io.tmpdir=$TMPDIR" -jar "$jar" "${args[@]}"
}
