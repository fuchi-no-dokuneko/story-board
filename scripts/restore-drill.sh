#!/usr/bin/env bash
set -euo pipefail
umask 077

if [[ $# -ne 2 ]]; then
  echo "Usage: scripts/restore-drill.sh <encrypted-backup> <work-directory>" >&2
  exit 2
fi

artifact=$(realpath "$1")
work=$(realpath -m "$2")
key_file=${STORYBLOCK_BACKUP_KEY_FILE:-}
manifest="$artifact.json"
checksum="$artifact.sha256"

if [[ ! -f "$artifact" ]]; then
  echo "Backup does not exist: $artifact" >&2
  exit 1
fi
if [[ -z "$key_file" || ! -f "$key_file" ]]; then
  echo "STORYBLOCK_BACKUP_KEY_FILE must name a readable key file" >&2
  exit 1
fi
if [[ ! -f "$manifest" || ! -f "$checksum" ]]; then
  echo "Backup manifest and checksum sidecar are required" >&2
  exit 1
fi
if [[ -e "$work/restored.db" ]]; then
  echo "Restore work directory is not isolated: restored.db already exists" >&2
  exit 1
fi

expected_sha=$(cut -d' ' -f1 "$checksum")
actual_sha=$(sha256sum "$artifact" | cut -d' ' -f1)
manifest_sha=$(jq -r '.encrypted_sha256' "$manifest")
if [[ "$expected_sha" != "$manifest_sha" ]]; then
  echo "Backup checksum sidecar does not match its manifest" >&2
  exit 1
fi
if [[ "$actual_sha" != "$expected_sha" ]]; then
  echo "Encrypted backup checksum does not match its manifest" >&2
  exit 1
fi

mkdir -p "$work"
started=$(date +%s%3N)
compressed="$work/restored.db.zst"
database="$work/restored.db"
report="$work/restore-report.json"

openssl enc -d -aes-256-cbc -pbkdf2 \
  -in "$artifact" -out "$compressed" -pass "file:$key_file"
zstd -q -d -f "$compressed" -o "$database"
quick_check=$(sqlite3 "$database" "PRAGMA quick_check;")
integrity_check=$(sqlite3 "$database" "PRAGMA integrity_check;")
if [[ "$quick_check" != "ok" || "$integrity_check" != "ok" ]]; then
  echo "Restore integrity check failed" >&2
  exit 1
fi

java -Djava.net.preferIPv4Stack=true \
  -jar apps/cli/target/storyblock-cli-0.1.0-SNAPSHOT.jar \
  replay-verify "$database" > "$work/replay-report.json"
if ! jq -e '.valid == true and (.novel_count == (.novels | length))' \
  "$work/replay-report.json" >/dev/null; then
  echo "Restored canonical replay verification failed" >&2
  exit 1
fi

migration_version=$(sqlite3 "$database" \
  "SELECT COALESCE(MAX(version), 'none') FROM flyway_schema_history WHERE success = 1;")
source_migration_version=$(jq -r '.migration_version' "$manifest")
revision_count=$(sqlite3 "$database" "SELECT COUNT(*) FROM revisions;")
operation_count=$(sqlite3 "$database" "SELECT COUNT(*) FROM operations;")
artifact_count=$(sqlite3 "$database" "SELECT COUNT(*) FROM artifacts;")
missing_artifacts='[]'
if [[ "$artifact_count" != "$(jq -r '.artifact_count' "$manifest")" ]]; then
  missing_artifacts='["artifact_count_mismatch"]'
fi
if [[ "$source_migration_version" != "none" \
   && "$migration_version" != "$source_migration_version" ]]; then
  echo "Restored migration version does not match the backup manifest" >&2
  exit 1
fi
if [[ "$revision_count" != "$(jq -r '.revision_count' "$manifest")" \
   || "$operation_count" != "$(jq -r '.operation_count' "$manifest")" \
   || "$missing_artifacts" != '[]' ]]; then
  echo "Restored schema or canonical counts do not match the backup manifest" >&2
  exit 1
fi

finished=$(date +%s%3N)
rto_ms=$((finished - started))
jq -n \
  --arg artifact "$(basename "$artifact")" \
  --arg checked_at "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  --arg source_migration_version "$source_migration_version" \
  --arg restored_migration_version "$migration_version" \
  --argjson revision_count "$revision_count" \
  --argjson operation_count "$operation_count" \
  --argjson artifact_count "$artifact_count" \
  --argjson missing_artifacts "$missing_artifacts" \
  --argjson rto_ms "$rto_ms" \
  '{artifact:$artifact,checked_at:$checked_at,quick_check:"ok",integrity_check:"ok",source_migration_version:$source_migration_version,restored_migration_version:$restored_migration_version,revision_count:$revision_count,operation_count:$operation_count,artifact_count:$artifact_count,missing_artifacts:$missing_artifacts,replay_report:"replay-report.json",rto_ms:$rto_ms}' \
  > "$report"
echo "$report"
