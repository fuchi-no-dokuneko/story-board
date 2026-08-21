#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$repo_dir"

MAVEN_OPTS="${MAVEN_OPTS:+$MAVEN_OPTS }-Djava.net.preferIPv4Stack=true"
export MAVEN_OPTS

if ! command -v java >/dev/null 2>&1; then
  printf 'Java 21 is required but was not found.\n' >&2
  exit 1
fi

java_major="$(java -version 2>&1 | awk -F '[".]' '/version/ {print $2; exit}')"
if [[ ! "$java_major" =~ ^[0-9]+$ ]] || (( java_major < 21 )); then
  printf 'Java 21 or newer is required; detected major version %s.\n' "$java_major" >&2
  exit 1
fi

if [[ ! -x ./mvnw ]]; then
  if ! command -v mvn >/dev/null 2>&1; then
    printf 'Bootstrap requires Maven 3.6.3 or newer; no system Maven was found.\n' >&2
    exit 1
  fi
  mvn -N wrapper:wrapper -Dmaven=3.9.11
fi

./mvnw -q -DskipTests dependency:go-offline
printf 'StoryBlock dependencies are available locally.\n'
