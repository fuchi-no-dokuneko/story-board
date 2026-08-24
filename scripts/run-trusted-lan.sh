#!/usr/bin/env bash
set -euo pipefail
umask 077

root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"

bind_address=${STORYBLOCK_LAN_BIND_ADDRESS:-127.0.0.1}
port=${STORYBLOCK_LAN_PORT:-8443}
database=${STORYBLOCK_DATABASE_PATH:-$root/data/storyblock.db}
state_dir=${STORYBLOCK_LAN_STATE_DIR:-$root/.local/trusted-lan}

if [[ ! $bind_address =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]]; then
  echo "STORYBLOCK_LAN_BIND_ADDRESS must be an IPv4 address" >&2
  exit 2
fi
IFS=. read -r first second third fourth <<<"$bind_address"
for octet in "$first" "$second" "$third" "$fourth"; do
  if ((octet < 0 || octet > 255)); then
    echo "STORYBLOCK_LAN_BIND_ADDRESS contains an invalid IPv4 octet" >&2
    exit 2
  fi
done
if ! ((first == 127
    || first == 10
    || (first == 172 && second >= 16 && second <= 31)
    || (first == 192 && second == 168)
    || (first == 100 && second >= 64 && second <= 127))); then
  echo "Refusing to bind trusted-LAN mode to a non-private IPv4 address" >&2
  exit 2
fi
if [[ ! $port =~ ^[0-9]+$ ]] || ((port < 1 || port > 65535)); then
  echo "STORYBLOCK_LAN_PORT must be between 1 and 65535" >&2
  exit 2
fi

mkdir -p "$state_dir" "$(dirname "$database")"
password_file=$state_dir/keystore.password
pepper_file=$state_dir/security.pepper
keystore=$state_dir/storyblock-lan.p12
address_file=$state_dir/certificate.address

if [[ ! -s $password_file ]]; then
  openssl rand -hex 32 >"$password_file"
fi
if [[ ! -s $pepper_file ]]; then
  openssl rand -base64 48 >"$pepper_file"
fi
password=$(<"$password_file")

if [[ ! -s $keystore || ! -s $address_file || $(<"$address_file") != "$bind_address" ]]; then
  rm -f "$keystore"
  keytool -genkeypair -noprompt \
    -alias storyblock-lan \
    -keyalg RSA -keysize 3072 -sigalg SHA256withRSA \
    -validity 825 \
    -dname "CN=StoryBlock Trusted LAN, OU=Local, O=StoryBlock" \
    -ext "SAN=ip:$bind_address,ip:127.0.0.1,dns:localhost" \
    -storetype PKCS12 \
    -keystore "$keystore" \
    -storepass "$password" \
    -keypass "$password" >/dev/null
  printf '%s\n' "$bind_address" >"$address_file"
fi

jar=$(find apps/api/target -maxdepth 1 -name 'storyblock-api-*.jar' \
  ! -name '*.original' -print -quit 2>/dev/null || true)
if [[ -z $jar ]]; then
  timeout 60s ./mvnw -q -pl apps/api -am package -DskipTests
  jar=$(find apps/api/target -maxdepth 1 -name 'storyblock-api-*.jar' \
    ! -name '*.original' -print -quit)
fi

export SPRING_PROFILES_ACTIVE=trusted-lan
export SERVER_ADDRESS=$bind_address
export SERVER_PORT=$port
export SERVER_SSL_ENABLED=true
export SERVER_SSL_KEY_ALIAS=storyblock-lan
export SERVER_SSL_KEY_STORE="file:$keystore"
export SERVER_SSL_KEY_STORE_PASSWORD=$password
export SERVER_SSL_KEY_STORE_TYPE=PKCS12
export STORYBLOCK_DATABASE_PATH=$database
export STORYBLOCK_SECURITY_PEPPER
STORYBLOCK_SECURITY_PEPPER=$(<"$pepper_file")
export STORYBLOCK_TRUSTED_LAN_ENABLED=true

echo "StoryBlock trusted LAN: https://$bind_address:$port/"
echo "Self-signed certificate: $keystore"
exec java -Djava.net.preferIPv4Stack=true -jar "$jar"
