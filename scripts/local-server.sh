#!/usr/bin/env bash
set -euo pipefail
umask 077

repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
local_dir=$repo_dir/.local/storyblock
jar=$local_dir/server/application.jar
pid_file=$local_dir/run/server.pid
log_file=$local_dir/logs/server.log
bind_address=${STORYBLOCK_LOCAL_BIND_ADDRESS:-127.0.0.1}
port=${STORYBLOCK_LOCAL_PORT:-8443}
tls_host=${STORYBLOCK_TLS_HOST:-localhost}

read_pid() {
  if [[ -s $pid_file ]]; then
    cat "$pid_file"
  fi
}

is_running() {
  local pid=${1:-}
  [[ $pid =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null
}

run_server() {
  if [[ ! -r $jar ]]; then
    printf 'Server is not installed. Run ./install.sh first.\n' >&2
    exit 1
  fi
  mkdir -p "$local_dir/data" "$local_dir/logs" "$local_dir/run" "$local_dir/tmp"
  if [[ ! -s $local_dir/runtime/java-home ]]; then
    printf 'Installed Java runtime metadata is missing. Run ./install.sh again.\n' >&2
    exit 1
  fi
  export JAVA_HOME
  JAVA_HOME=$(<"$local_dir/runtime/java-home")
  if [[ ! -x $JAVA_HOME/bin/java || ! -x $JAVA_HOME/bin/keytool ]]; then
    printf 'The Java runtime used during installation is unavailable.\n' >&2
    exit 1
  fi
  export PATH=$JAVA_HOME/bin:$PATH
  STORYBLOCK_TLS_PRIVATE_DIR="$local_dir/tls/private" \
  STORYBLOCK_TLS_PUBLIC_DIR="$local_dir/tls/public" \
  STORYBLOCK_TLS_HOST="$tls_host" \
    "$repo_dir/scripts/generate-self-signed-tls.sh" >/dev/null

  export SERVER_ADDRESS=$bind_address
  export SERVER_PORT=$port
  export SERVER_SSL_ENABLED=true
  export SERVER_SSL_KEY_ALIAS=storyblock
  export SERVER_SSL_KEY_STORE="file:$local_dir/tls/private/storyblock.p12"
  export SERVER_SSL_KEY_STORE_PASSWORD
  SERVER_SSL_KEY_STORE_PASSWORD=$(<"$local_dir/tls/private/keystore.password")
  export SERVER_SSL_KEY_STORE_TYPE=PKCS12
  export STORYBLOCK_DATABASE_PATH=$local_dir/data/storyblock.db
  export STORYBLOCK_SECURITY_OWNER_TOKEN
  STORYBLOCK_SECURITY_OWNER_TOKEN=$(<"$local_dir/secrets/owner-token")
  export STORYBLOCK_SECURITY_PEPPER
  STORYBLOCK_SECURITY_PEPPER=$(<"$local_dir/secrets/server-pepper")
  export TMPDIR=$local_dir/tmp

  exec "$JAVA_HOME/bin/java" \
    -Djava.net.preferIPv4Stack=true \
    -Djava.io.tmpdir="$local_dir/tmp" \
    -jar "$jar"
}

command_name=${1:-status}
case "$command_name" in
  run)
    run_server
    ;;
  start)
    pid=$(read_pid || true)
    if is_running "$pid"; then
      printf 'StoryBlock is already running with PID %s.\n' "$pid"
      exit 0
    fi
    rm -f -- "$pid_file"
    mkdir -p "$local_dir/logs" "$local_dir/run"
    nohup "$0" run >>"$log_file" 2>&1 &
    pid=$!
    printf '%s\n' "$pid" >"$pid_file"
    for _ in {1..20}; do
      if ! is_running "$pid"; then
        printf 'StoryBlock failed to start; inspect %s.\n' "$log_file" >&2
        exit 1
      fi
      if grep -q 'Started StoryBlockApiApplication' "$log_file" 2>/dev/null; then
        printf 'StoryBlock started with PID %s at https://%s:%s/.\n' \
          "$pid" "$bind_address" "$port"
        exit 0
      fi
      sleep 0.5
    done
    printf 'StoryBlock is still starting with PID %s; inspect %s.\n' "$pid" "$log_file"
    ;;
  stop)
    pid=$(read_pid || true)
    if ! is_running "$pid"; then
      rm -f -- "$pid_file"
      printf 'StoryBlock is not running.\n'
      exit 0
    fi
    process_command=$(ps -p "$pid" -o args= 2>/dev/null || true)
    if [[ $process_command != *application.jar* && $process_command != *local-server.sh* ]]; then
      printf 'Refusing to stop unrelated PID %s.\n' "$pid" >&2
      exit 1
    fi
    kill "$pid"
    for _ in {1..20}; do
      if ! is_running "$pid"; then
        rm -f -- "$pid_file"
        printf 'StoryBlock stopped.\n'
        exit 0
      fi
      sleep 0.25
    done
    printf 'StoryBlock did not stop cleanly; PID %s remains.\n' "$pid" >&2
    exit 1
    ;;
  status)
    pid=$(read_pid || true)
    if is_running "$pid"; then
      printf 'StoryBlock is running with PID %s at https://%s:%s/.\n' \
        "$pid" "$bind_address" "$port"
    else
      printf 'StoryBlock is not running.\n'
      exit 1
    fi
    ;;
  logs)
    if [[ ! -f $log_file ]]; then
      printf 'No server log exists yet.\n' >&2
      exit 1
    fi
    exec tail -n 200 -f "$log_file"
    ;;
  *)
    printf 'Usage: %s {start|stop|status|logs|run}\n' "$0" >&2
    exit 2
    ;;
esac
