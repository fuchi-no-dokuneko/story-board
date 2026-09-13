# Local backup and restore / 本機備份還原 / 本机备份恢复

EN: Backups are unencrypted Zstandard-compressed SQLite snapshots (`.db.zst`). No backup key, key configuration, or rotation is used. Files stay inside the repository with owner-only permissions. SHA-256 sidecars detect corruption. Restore checks database integrity, counts, and canonical replay in a separate directory.

繁體中文：備份為未加密的 Zstandard 壓縮 SQLite 快照（`.db.zst`），不使用備份金鑰、金鑰設定或輪替。檔案留在儲存庫，僅擁有者可讀寫。SHA-256 附檔檢查損壞；還原在獨立目錄檢查資料庫完整性、數量與標準重播。

简体中文：备份为未加密的 Zstandard 压缩 SQLite 快照（`.db.zst`），不使用备份密钥、密钥配置或轮换。文件保留在仓库，仅所有者可读写。SHA-256 附件检查损坏；恢复在独立目录检查数据库完整性、数量与标准重放。

```bash
./scripts/backup.sh server/data/storyblock.db server/data/backups
./scripts/restore-drill.sh server/data/backups/storyblock-YYYYMMDDTHHMMSSZ.db.zst .local/restore-drill
./scripts/prune-backups.sh server/data/backups
```

EN: Retention keeps the newest 48 snapshots, one per day for 30 days, and one per week for 12 weeks. Pruning previews removals unless `--apply` is supplied. Concurrent backups are serialized and receive distinct filenames. The regression command is `./scripts/test-backup.sh <test-database>`.

繁體中文：保留最新 48 份、30 天內每日一份及 12 週內每週一份。清理預設只預覽，指定 `--apply` 才刪除。併發備份依序執行並使用不同檔名。回歸命令為 `./scripts/test-backup.sh <測試資料庫>`。

简体中文：保留最新 48 份、30 天内每日一份及 12 周内每周一份。清理默认只预览，指定 `--apply` 才删除。并发备份依次执行并使用不同文件名。回归命令为 `./scripts/test-backup.sh <测试数据库>`。

[Detailed notes / 詳細筆記 / 详细笔记](backup-and-restore.notes.txt)
