#!/usr/bin/env bash
set -euo pipefail
source "$(dirname -- "${BASH_SOURCE[0]}")/backup-environment.sh"
umask 077

if [[ $# -ne 2 ]]; then
  echo "Usage: scripts/backup.sh <database> <repository-backup-directory>" >&2
  exit 2
fi

database=$(repository_path "$1")
destination=$(repository_path "$2")

if [[ ! -f "$database" ]]; then
  echo "Database does not exist: $database" >&2
  exit 1
fi
lock_backup_directory "$destination"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
stamp=$(date -u +%Y%m%dT%H%M%SZ)
while [[ -e "$destination/storyblock-$stamp.db.zst" ]]; do
  sleep 1
  stamp=$(date -u +%Y%m%dT%H%M%SZ)
done
plain="$work/storyblock-$stamp.db"
artifact="$destination/storyblock-$stamp.db.zst"
pending="$destination/.storyblock-$stamp.db.zst.pending"

sqlite3 "$database" ".timeout 5000" ".backup '$plain'"
quick_check=$(sqlite3 "$plain" "PRAGMA quick_check;")
integrity_check=$(sqlite3 "$plain" "PRAGMA integrity_check;")
if [[ "$quick_check" != "ok" || "$integrity_check" != "ok" ]]; then
  echo "Backup integrity check failed" >&2
  exit 1
fi
zstd -q -T0 -19 "$plain" -o "$pending"
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
artifact_count=$(sqlite3 "$plain" \
  "SELECT COUNT(*) FROM artifacts;" 2>/dev/null || printf '0')
manifest="$artifact.json"
manifest_pending="$manifest.pending"
printf '{"artifact":"%s","created_at":"%s","quick_check":"ok","integrity_check":"ok","sha256":"%s","migration_version":"%s","revision_count":%d,"operation_count":%d,"artifact_count":%d}\n' \
  "$(basename "$artifact")" \
  "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  "$(cut -d' ' -f1 "$artifact.sha256")" \
  "$migration_version" \
  "$revision_count" \
  "$operation_count" \
  "$artifact_count" > "$manifest_pending"
mv "$manifest_pending" "$manifest"

if [[ ${STORYBLOCK_BACKUP_PRUNE_AFTER_WRITE:-true} == true ]]; then
  "$(dirname "$0")/prune-backups.sh" "$destination" --apply >/dev/null
fi
echo "$artifact"
