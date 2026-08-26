#!/usr/bin/env bash
set -euo pipefail
umask 077

private_dir=${STORYBLOCK_TLS_PRIVATE_DIR:-/app/tls/private}
public_dir=${STORYBLOCK_TLS_PUBLIC_DIR:-/app/tls/public}
tls_host=${STORYBLOCK_TLS_HOST:-localhost}
alias_name=${STORYBLOCK_TLS_ALIAS:-storyblock}
keystore=$private_dir/storyblock.p12
password_file=$private_dir/keystore.password
host_file=$private_dir/certificate.host
certificate=$public_dir/storyblock.crt
truststore=$public_dir/storyblock-truststore.p12
truststore_password_file=$public_dir/truststore.password
truststore_password=changeit

if ! command -v keytool >/dev/null 2>&1; then
  printf 'keytool is required to generate local self-signed TLS.\n' >&2
  exit 1
fi
if [[ ! $tls_host =~ ^[A-Za-z0-9][A-Za-z0-9.-]*$ ]]; then
  printf 'STORYBLOCK_TLS_HOST must be a hostname or IPv4 address.\n' >&2
  exit 2
fi

mkdir -p "$private_dir" "$public_dir"
if [[ ! -s $password_file ]]; then
  od -An -N32 -tx1 /dev/urandom | tr -d ' \n' >"$password_file"
fi
password=$(<"$password_file")

san='dns:localhost,dns:api,ip:127.0.0.1'
if [[ $tls_host =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  IFS=. read -r first second third fourth <<<"$tls_host"
  for octet in "$first" "$second" "$third" "$fourth"; do
    if ((octet < 0 || octet > 255)); then
      printf 'STORYBLOCK_TLS_HOST contains an invalid IPv4 octet.\n' >&2
      exit 2
    fi
  done
  if [[ $tls_host != 127.0.0.1 ]]; then
    san+=",ip:$tls_host"
  fi
elif [[ $tls_host != localhost && $tls_host != api ]]; then
  san+=",dns:$tls_host"
fi

if [[ ! -s $keystore || ! -s $host_file || $(<"$host_file") != "$tls_host" ]]; then
  rm -f -- "$keystore" "$host_file"
  keytool -genkeypair -noprompt \
    -alias "$alias_name" \
    -keyalg RSA -keysize 3072 -sigalg SHA256withRSA \
    -validity 825 \
    -dname "CN=$tls_host,OU=Local,O=StoryBlock" \
    -ext "SAN=$san" \
    -ext 'BC=ca:false' \
    -ext 'KU=digitalSignature,keyEncipherment' \
    -ext 'EKU=serverAuth' \
    -storetype PKCS12 \
    -keystore "$keystore" \
    -storepass "$password" \
    -keypass "$password" >/dev/null
  printf '%s\n' "$tls_host" >"$host_file"
fi

certificate_tmp=$public_dir/storyblock.crt.tmp
truststore_tmp=$public_dir/storyblock-truststore.p12.tmp
rm -f -- "$certificate_tmp" "$truststore_tmp"
keytool -exportcert -rfc \
  -alias "$alias_name" \
  -keystore "$keystore" \
  -storepass "$password" \
  -file "$certificate_tmp" >/dev/null
keytool -importcert -noprompt \
  -alias "$alias_name" \
  -file "$certificate_tmp" \
  -storetype PKCS12 \
  -keystore "$truststore_tmp" \
  -storepass "$truststore_password" >/dev/null
mv -f -- "$certificate_tmp" "$certificate"
mv -f -- "$truststore_tmp" "$truststore"
printf '%s\n' "$truststore_password" >"$truststore_password_file"
chmod 600 \
  "$keystore" "$password_file" "$host_file" \
  "$certificate" "$truststore" "$truststore_password_file"

printf 'Self-signed StoryBlock leaf certificate is ready for %s.\n' "$tls_host"
