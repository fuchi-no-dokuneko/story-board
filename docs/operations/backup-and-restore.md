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
writes a checksum plus a short manifest. The destination must be a versioned
off-host mount in production. The key must not share that mount.

Run an isolated restore drill without replacing the live database:

```bash
scripts/restore-drill.sh \
  /offsite/storyblock/storyblock-YYYYMMDDTHHMMSSZ.db.zst.enc \
  artifacts/restore-drill
```

The drill decrypts into the chosen isolated directory, runs SQLite integrity
verification, replays every novel, checks each head hash, and records measured
RTO in `restore-report.json`.
