# Local backup and restore

EN: Backup, rotation, and isolated restore stay inside this repository. The backup helper creates a local encryption key automatically. Keep keys separate from database and backup directories. Restore checks integrity, counts, and canonical replay; it never overwrites the live database.

繁體中文：備份、金鑰輪替及隔離還原均留在儲存庫。備份工具自動建立本機加密金鑰。金鑰與資料庫、備份目錄分開。還原檢查完整性、數量及標準重播，不覆寫正式資料庫。

简体中文：备份、密钥轮换及隔离还原均留在仓库。备份工具自动创建本机加密密钥。密钥与数据库、备份目录分开。还原检查完整性、数量及标准重放，不覆盖正式数据库。

[Detailed notes / 詳細筆記 / 详细笔记](backup-and-restore.notes.txt)
