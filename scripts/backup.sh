#!/usr/bin/env bash
set -euo pipefail
umask 077

if [[ $# -ne 2 ]]; then
  echo "Usage: scripts/backup.sh <database> <offsite-directory>" >&2
  exit 2
fi

database=$(realpath "$1")
destination=$(realpath -m "$2")
key_file=${STORYBLOCK_BACKUP_KEY_FILE:-}

if [[ ! -f "$database" ]]; then
  echo "Database does not exist: $database" >&2
  exit 1
fi
if [[ -z "$key_file" || ! -f "$key_file" ]]; then
  echo "STORYBLOCK_BACKUP_KEY_FILE must name a readable key file" >&2
  exit 1
fi
key_file=$(realpath "$key_file")
database_directory=$(dirname "$database")
case "$key_file" in
  "$destination"/*|"$database_directory"/*)
    echo "Backup key must be separate from the database and backup destination" >&2
    exit 1
    ;;
esac

mkdir -p "$destination"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
stamp=$(date -u +%Y%m%dT%H%M%SZ)
plain="$work/storyblock-$stamp.db"
compressed="$plain.zst"
artifact="$destination/storyblock-$stamp.db.zst.enc"
pending="$destination/.storyblock-$stamp.db.zst.enc.pending"

sqlite3 "$database" ".timeout 5000" ".backup '$plain'"
quick_check=$(sqlite3 "$plain" "PRAGMA quick_check;")
integrity_check=$(sqlite3 "$plain" "PRAGMA integrity_check;")
if [[ "$quick_check" != "ok" || "$integrity_check" != "ok" ]]; then
  echo "Backup integrity check failed" >&2
  exit 1
fi
zstd -q -T0 -19 "$plain" -o "$compressed"
openssl enc -aes-256-cbc -pbkdf2 -salt \
  -in "$compressed" -out "$pending" -pass "file:$key_file"
mv "$pending" "$artifact"
chmod 600 "$artifact"
sha256sum "$artifact" > "$artifact.sha256"
migration_version=$(sqlite3 "$plain" \
  "SELECT COALESCE(MAX(version), 'none') FROM flyway_schema_history WHERE success = 1;" \
  2>/dev/null || printf 'none')
revision_count=$(sqlite3 "$plain" \
  "SELECT COUNT(*) FROM revisions;" 2>/dev/null || printf '0')
operation_count=$(sqlite3 "$plain" \
  "SELECT COUNT(*) FROM operations;" 2>/dev/null || printf '0')
manifest="$artifact.json"
manifest_pending="$manifest.pending"
printf '{"artifact":"%s","created_at":"%s","quick_check":"ok","integrity_check":"ok","encrypted_sha256":"%s","migration_version":"%s","revision_count":%d,"operation_count":%d}\n' \
  "$(basename "$artifact")" \
  "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  "$(cut -d' ' -f1 "$artifact.sha256")" \
  "$migration_version" \
  "$revision_count" \
  "$operation_count" > "$manifest_pending"
mv "$manifest_pending" "$manifest"

if [[ ${STORYBLOCK_BACKUP_PRUNE_AFTER_WRITE:-true} == true ]]; then
  "$(dirname "$0")/prune-backups.sh" "$destination" --apply >/dev/null
fi
echo "$artifact"
