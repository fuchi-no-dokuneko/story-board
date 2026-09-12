#!/usr/bin/env bash
set -euo pipefail
umask 077
source "$(dirname -- "${BASH_SOURCE[0]}")/build-environment.sh"
database=$(repository_path "${1:-$repo_dir/client-cli/target/slo/storyblock.db}")
[[ -f $database ]] || { echo 'Provide a populated StoryBlock test database' >&2; exit 2; }
work=$(mktemp -d)
trap 'rm -r -- "$work"' EXIT
fixture=$work/repository
mkdir -p "$fixture/scripts" "$fixture/client-cli/target"
cp "$repo_dir/scripts/"{backup,backup-environment,build-environment,repository-paths,restore-drill,prune-backups}.sh "$fixture/scripts/"
cp "$repo_dir"/client-cli/target/storyblock-cli-*.jar "$fixture/client-cli/target/"
sqlite3 "$database" ".backup '$fixture/input.db'"
retention_test=$repo_dir/scripts/test-backup-retention.py
cd "$fixture"
STORYBLOCK_BACKUP_PRUNE_AFTER_WRITE=false scripts/backup.sh input.db backups >first.txt &
first_pid=$!
STORYBLOCK_BACKUP_PRUNE_AFTER_WRITE=false scripts/backup.sh input.db backups >second.txt &
second_pid=$!
wait "$first_pid"
wait "$second_pid"
first=$(<first.txt)
second=$(<second.txt)
[[ $first != "$second" && $first == *.db.zst && $second == *.db.zst ]]
for backup in "$first" "$second"; do
  zstd -q -t "$backup"
  [[ $(stat -c %a "$backup") == 600 ]]
  [[ $(jq -r .sha256 "$backup.json") == "$(cut -d' ' -f1 "$backup.sha256")" ]]
  report=$(scripts/restore-drill.sh "$backup" "$fixture/restore-$(basename "$backup")")
  jq -e '.quick_check == "ok" and .integrity_check == "ok" and .missing_artifacts == []' "$report" >/dev/null
done
[[ ! -e .local/storyblock/secrets ]]
if scripts/restore-drill.sh "$first" "$fixture/restore-$(basename "$first")" >reuse.log 2>&1; then
  echo 'Reused restore directory was accepted' >&2; exit 1
fi
cp "$first" corrupt.db.zst
cp "$first.json" corrupt.db.zst.json
cp "$first.sha256" corrupt.db.zst.sha256
printf 'damaged transfer\n' >>corrupt.db.zst
if scripts/restore-drill.sh corrupt.db.zst corrupt-restore >corrupt.log 2>&1; then
  echo 'Corrupted backup was accepted' >&2; exit 1
fi
[[ ! -e corrupt-restore/restored.db ]]
python3 "$retention_test" "$first" retention
echo 'PASS: keyless concurrent backup, isolated restore, corruption rejection, retention; no secrets created'
