#!/usr/bin/env bash
set -euo pipefail
umask 077

if [[ $# -ne 2 ]]; then
  echo "Usage: scripts/rotate-backup-key.sh <encrypted-backup> <new-encrypted-backup>" >&2
  exit 2
fi

source_artifact=$(realpath "$1")
destination_artifact=$(realpath -m "$2")
old_key=${STORYBLOCK_BACKUP_KEY_FILE:-}
new_key=${STORYBLOCK_NEW_BACKUP_KEY_FILE:-}
source_manifest="$source_artifact.json"
source_checksum="$source_artifact.sha256"

for required in "$source_artifact" "$source_manifest" "$source_checksum" "$old_key" "$new_key"; do
  if [[ -z "$required" || ! -f "$required" ]]; then
    echo "Backup, sidecars, and both backup key files are required" >&2
    exit 1
  fi
done
if [[ -e "$destination_artifact" || "$source_artifact" == "$destination_artifact" ]]; then
  echo "Rotation destination must be a new path" >&2
  exit 1
fi
if [[ "$(sha256sum "$old_key" | cut -d' ' -f1)" == \
      "$(sha256sum "$new_key" | cut -d' ' -f1)" ]]; then
  echo "New backup key must differ from the old key" >&2
  exit 1
fi

expected_sha=$(cut -d' ' -f1 "$source_checksum")
actual_sha=$(sha256sum "$source_artifact" | cut -d' ' -f1)
manifest_sha=$(jq -r '.encrypted_sha256' "$source_manifest")
if [[ "$expected_sha" != "$actual_sha" || "$expected_sha" != "$manifest_sha" ]]; then
  echo "Source backup checksum validation failed" >&2
  exit 1
fi

mkdir -p "$(dirname "$destination_artifact")"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
compressed="$work/source.db.zst"
verification="$work/verification.db.zst"
pending="$destination_artifact.pending"

openssl enc -d -aes-256-cbc -pbkdf2 \
  -in "$source_artifact" -out "$compressed" -pass "file:$old_key"
zstd -q -t "$compressed"
openssl enc -aes-256-cbc -pbkdf2 -salt \
  -in "$compressed" -out "$pending" -pass "file:$new_key"
openssl enc -d -aes-256-cbc -pbkdf2 \
  -in "$pending" -out "$verification" -pass "file:$new_key"
cmp "$compressed" "$verification"

new_sha=$(sha256sum "$pending" | cut -d' ' -f1)
checksum_pending="$destination_artifact.sha256.pending"
manifest_pending="$destination_artifact.json.pending"
printf '%s  %s\n' "$new_sha" "$(basename "$destination_artifact")" \
  > "$checksum_pending"
jq \
  --arg artifact "$(basename "$destination_artifact")" \
  --arg encrypted_sha256 "$new_sha" \
  --arg rotated_at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  --arg rotation_source_sha256 "$expected_sha" \
  '.artifact=$artifact
   | .encrypted_sha256=$encrypted_sha256
   | .rotated_at=$rotated_at
   | .rotation_source_sha256=$rotation_source_sha256' \
  "$source_manifest" > "$manifest_pending"

mv "$pending" "$destination_artifact"
mv "$checksum_pending" "$destination_artifact.sha256"
mv "$manifest_pending" "$destination_artifact.json"
chmod 600 "$destination_artifact" "$destination_artifact.sha256" \
  "$destination_artifact.json"
echo "$destination_artifact"
