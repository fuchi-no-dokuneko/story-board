# SQLite persistence

EN: SQLite remains the local canonical store. Connections use WAL, FULL synchronization, foreign keys, a bounded busy timeout, and explicit read-only transactions. Flyway migrations and checkpoint/replay semantics remain unchanged.

繁體中文：SQLite 仍為本機正式資料庫。連線使用 WAL、FULL 同步、外鍵、有界忙碌逾時及明確唯讀交易。Flyway 遷移與檢查點重播語意均保持不變。

简体中文：SQLite 仍为本机正式数据库。连接使用 WAL、FULL 同步、外键、有界忙碌超时及明确只读事务。Flyway 迁移与检查点重放语义均保持不变。

[Detailed notes / 詳細筆記 / 详细笔记](0003-sqlite-jdbc-persistence.notes.txt)
