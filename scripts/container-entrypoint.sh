#!/usr/bin/env bash
set -euo pipefail

load_secret() {
  local variable=$1
  local file_variable="${variable}_FILE"
  local file=${!file_variable:-}
  if [[ -n "$file" ]]; then
    if [[ ! -f "$file" ]]; then
      echo "Secret file is unavailable: $file_variable" >&2
      exit 1
    fi
    printf -v "$variable" '%s' "$(<"$file")"
    export "$variable"
    unset "$file_variable"
  fi
}

load_secret STORYBLOCK_SECURITY_OWNER_TOKEN
load_secret STORYBLOCK_SECURITY_PEPPER
load_secret STORYBLOCK_WORKER_TOKEN
load_secret STORYBLOCK_LLM_WORKER_MODEL_TOKEN

java_arguments=(-Djava.net.preferIPv4Stack=true)

if [[ ${STORYBLOCK_SELF_SIGNED_TLS_ENABLED:-false} == true ]]; then
  export STORYBLOCK_TLS_PRIVATE_DIR=${STORYBLOCK_TLS_PRIVATE_DIR:-/app/tls/private}
  export STORYBLOCK_TLS_PUBLIC_DIR=${STORYBLOCK_TLS_PUBLIC_DIR:-/app/tls/public}
  export STORYBLOCK_TLS_HOST=${STORYBLOCK_TLS_HOST:-localhost}
  /app/generate-self-signed-tls.sh

  export SERVER_ADDRESS=${SERVER_ADDRESS:-0.0.0.0}
  export SERVER_PORT=${SERVER_PORT:-8443}
  export SERVER_SSL_ENABLED=true
  export SERVER_SSL_KEY_ALIAS=${STORYBLOCK_TLS_ALIAS:-storyblock}
  export SERVER_SSL_KEY_STORE="file:$STORYBLOCK_TLS_PRIVATE_DIR/storyblock.p12"
  export SERVER_SSL_KEY_STORE_PASSWORD
  SERVER_SSL_KEY_STORE_PASSWORD=$(<"$STORYBLOCK_TLS_PRIVATE_DIR/keystore.password")
  export SERVER_SSL_KEY_STORE_TYPE=PKCS12
fi

if [[ -n ${STORYBLOCK_TLS_TRUST_STORE:-} ]]; then
  truststore_password_file=${STORYBLOCK_TLS_TRUST_STORE_PASSWORD_FILE:-}
  for _ in {1..60}; do
    if [[ -s $STORYBLOCK_TLS_TRUST_STORE \
          && -n $truststore_password_file \
          && -s $truststore_password_file ]]; then
      break
    fi
    sleep 1
  done
  if [[ ! -s $STORYBLOCK_TLS_TRUST_STORE \
        || -z $truststore_password_file \
        || ! -s $truststore_password_file ]]; then
    echo "Self-signed TLS trust store is unavailable" >&2
    exit 1
  fi
  java_arguments+=(
    "-Djavax.net.ssl.trustStore=$STORYBLOCK_TLS_TRUST_STORE"
    "-Djavax.net.ssl.trustStoreType=PKCS12"
    "-Djavax.net.ssl.trustStorePassword=$(<"$truststore_password_file")"
  )
fi

exec java "${java_arguments[@]}" -jar /app/application.jar "$@"
