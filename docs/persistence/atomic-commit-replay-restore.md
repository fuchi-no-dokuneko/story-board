# Atomic persistence

EN: A commit appends operation, revision, projection, checkpoint, audit, and head change in one SQLite transaction. Idempotency and stale-head checks preserve concurrency semantics. Replay verifies ordered history; restore appends a new revision without deleting history.

繁體中文：提交在同一 SQLite 交易中寫入操作、修訂、投影、檢查點、稽核及版本頭。冪等與過期版本頭檢查保留併發語意。重播驗證有序歷史；還原新增修訂而不刪除歷史。

简体中文：提交在同一 SQLite 事务中写入操作、修订、投影、检查点、审计及版本头。幂等与过期版本头检查保留并发语义。重放验证有序历史；还原新增修订而不删除历史。

[Detailed notes / 詳細筆記 / 详细笔记](atomic-commit-replay-restore.notes.txt)
