#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: scripts/restore-drill.sh <encrypted-backup> <work-directory>" >&2
  exit 2
fi

artifact=$(realpath "$1")
work=$(realpath -m "$2")
key_file=${STORYBLOCK_BACKUP_KEY_FILE:-}

if [[ ! -f "$artifact" ]]; then
  echo "Backup does not exist: $artifact" >&2
  exit 1
fi
if [[ -z "$key_file" || ! -f "$key_file" ]]; then
  echo "STORYBLOCK_BACKUP_KEY_FILE must name a readable key file" >&2
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
integrity=$(sqlite3 "$database" "PRAGMA integrity_check;")
if [[ "$integrity" != "ok" ]]; then
  echo "Restore integrity check failed: $integrity" >&2
  exit 1
fi

java -Djava.net.preferIPv4Stack=true \
  -jar apps/cli/target/storyblock-cli-0.1.0-SNAPSHOT.jar \
  replay-verify "$database" > "$work/replay-report.json"
finished=$(date +%s%3N)
rto_ms=$((finished - started))
printf '{"artifact":"%s","checked_at":"%s","integrity":"ok","replay_report":"replay-report.json","rto_ms":%d}\n' \
  "$(basename "$artifact")" \
  "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  "$rto_ms" > "$report"
echo "$report"
