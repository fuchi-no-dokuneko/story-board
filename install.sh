#!/usr/bin/env bash
set -euo pipefail
umask 077

repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
local_dir=$repo_dir/.local/storyblock
start_after_install=false

usage() {
  cat <<'EOF'
Usage: ./install.sh [--start]

Build and install StoryBlock entirely below .local/storyblock in this repository.
No sudo/root access, system installation, external cache, or uploaded TLS material
is used. --start launches the installed server after a successful installation.
EOF
}

while (($#)); do
  case "$1" in
    --start) start_after_install=true ;;
    -h|--help) usage; exit 0 ;;
    *) printf 'Unknown option: %s\n' "$1" >&2; usage >&2; exit 2 ;;
  esac
  shift
done

cd "$repo_dir"
mkdir -p \
  "$local_dir/cache" \
  "$local_dir/data" \
  "$local_dir/logs" \
  "$local_dir/maven/repository" \
  "$local_dir/run" \
  "$local_dir/runtime" \
  "$local_dir/secrets" \
  "$local_dir/server" \
  "$local_dir/tls/private" \
  "$local_dir/tls/public" \
  "$local_dir/tmp"

export TMPDIR=$local_dir/tmp
export XDG_CACHE_HOME=$local_dir/cache
export MAVEN_USER_HOME=$local_dir/maven
MAVEN_OPTS="${MAVEN_OPTS:+$MAVEN_OPTS }-Djava.net.preferIPv4Stack=true -Djava.io.tmpdir=$local_dir/tmp"
export MAVEN_OPTS

if ! command -v java >/dev/null 2>&1; then
  printf 'java from Java 21 is required but was not found.\n' >&2
  exit 1
fi
java_binary=$(readlink -f "$(command -v java)")
detected_java_home=$(cd "$(dirname "$java_binary")/.." && pwd)
if [[ ! -x $detected_java_home/bin/keytool ]]; then
  printf 'keytool was not found beside the selected Java runtime.\n' >&2
  exit 1
fi
export JAVA_HOME=$detected_java_home
export PATH=$JAVA_HOME/bin:$PATH

if ! java_version=$(java -version 2>&1); then
  printf 'Java could not start:\n%s\n' "$java_version" >&2
  exit 1
fi
java_major=$(awk -F '[".]' '/version/ {print $2; exit}' <<<"$java_version")
if [[ ! $java_major =~ ^[0-9]+$ ]] || ((java_major < 21)); then
  printf 'Java 21 or newer is required; detected major version %s.\n' "$java_major" >&2
  exit 1
fi
printf '%s\n' "$JAVA_HOME" >"$local_dir/runtime/java-home"
if [[ ! -x ./mvnw ]]; then
  printf 'The tracked Maven wrapper ./mvnw is required.\n' >&2
  exit 1
fi

./mvnw --batch-mode \
  -Djava.io.tmpdir="$local_dir/tmp" \
  -Dmaven.repo.local="$local_dir/maven/repository" \
  -DskipTests clean package

mapfile -t api_jars < <(find apps/api/target -maxdepth 1 -type f \
  -name 'storyblock-api-*.jar' ! -name '*.original' -print)
if ((${#api_jars[@]} != 1)); then
  printf 'Expected exactly one API jar; found %s.\n' "${#api_jars[@]}" >&2
  exit 1
fi
cp -- "${api_jars[0]}" "$local_dir/server/application.jar"
chmod 500 "$local_dir/server/application.jar"

random_hex() {
  od -An -N48 -tx1 /dev/urandom | tr -d ' \n'
}
for secret_name in owner-token server-pepper style-worker-token llm-model-token; do
  secret_file=$local_dir/secrets/$secret_name
  if [[ ! -s $secret_file ]]; then
    random_hex >"$secret_file"
  fi
  chmod 600 "$secret_file"
done

STORYBLOCK_TLS_PRIVATE_DIR="$local_dir/tls/private" \
STORYBLOCK_TLS_PUBLIC_DIR="$local_dir/tls/public" \
STORYBLOCK_TLS_HOST="${STORYBLOCK_TLS_HOST:-localhost}" \
  "$repo_dir/scripts/generate-self-signed-tls.sh"

printf 'StoryBlock was installed inside:\n  %s\n' "$local_dir"
printf 'Owner token (not printed):\n  %s\n' "$local_dir/secrets/owner-token"
printf 'Start:  ./scripts/local-server.sh start\n'
printf 'Status: ./scripts/local-server.sh status\n'
printf 'Stop:   ./scripts/local-server.sh stop\n'
printf 'URL:    https://127.0.0.1:8443/ (self-signed certificate)\n'

if [[ $start_after_install == true ]]; then
  exec "$repo_dir/scripts/local-server.sh" start
fi
