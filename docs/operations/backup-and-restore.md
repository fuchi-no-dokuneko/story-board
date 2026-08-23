# Backup and restore

Build the CLI, create a key outside the repository and database volume, then
run the live backup command:

```bash
./mvnw -q -pl apps/cli -am package -DskipTests
umask 077
openssl rand -base64 48 > /protected/storyblock-backup.key
export STORYBLOCK_BACKUP_KEY_FILE=/protected/storyblock-backup.key
scripts/backup.sh data/storyblock.db /offsite/storyblock
```

`backup.sh` uses SQLite's online backup command, runs `integrity_check`,
compresses the snapshot, encrypts it with a PBKDF2-derived AES-256 key, and
writes a checksum plus a short manifest containing migration and canonical row
counts. Publication is atomic. The destination must be a versioned off-host
mount in production, and the script rejects a key stored beside either the live
database or backup artifacts.

Successful backups automatically apply the initial retention policy: the most
recent 48 snapshots, one daily snapshot for 30 days, and one weekly snapshot
for 12 weeks. Preview deletions without changing files with:

```bash
scripts/prune-backups.sh /offsite/storyblock
```

Run an isolated restore drill without replacing the live database:

```bash
scripts/restore-drill.sh \
  /offsite/storyblock/storyblock-YYYYMMDDTHHMMSSZ.db.zst.enc \
  artifacts/restore-drill
```

The drill decrypts into the chosen isolated directory, runs SQLite integrity
verification, compares migration/revision/operation/artifact counts with the
backup manifest, replays every novel, checks each head hash, records a
deterministic full-head render hash, and records measured RTO plus missing
artifacts in `restore-report.json`. Legacy backups without Flyway history are
migrated offline and record both source and restored versions. A reused restore
directory is rejected.
