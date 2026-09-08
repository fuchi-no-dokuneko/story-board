# Verification / 驗證 / 验证

EN: ADR-311 preserves the existing HTTP routes and DTO shapes. The original OpenAPI document and eight database migrations are unchanged. The final Java run passed 182 tests across 19 modules; the author tool passed 173 tests and browser assets passed 4 tests.

繁體中文：ADR-311 保留既有 HTTP 路徑及 DTO 結構。原始 OpenAPI 與八份資料庫遷移未變更。最終 Java 執行通過 19 個模組中的 182 項測試；作者工具通過 173 項，瀏覽器資源通過 4 項。

简体中文：ADR-311 保留既有 HTTP 路径及 DTO 结构。原始 OpenAPI 与八份数据库迁移未更改。最终 Java 执行通过 19 个模块中的 182 项测试；作者工具通过 173 项，浏览器资源通过 4 项。

EN: Real Chromium passed 9 scenarios and 85 steps against an isolated HTTPS/SQLite installation. Additional live checks covered image upload, preview, commit replay, deterministic PDF, package import into another database, concurrent startup, restart persistence, port conflicts, PID ownership, concurrent backups and restore. Coverage and mocks are listed in the root inventory.

繁體中文：真正 Chromium 對隔離 HTTPS／SQLite 安裝通過 9 情境、85 步驟。另完成圖片上傳、預覽、提交重播、確定性 PDF、另一資料庫封裝匯入、併發啟動、重啟保存、連接埠衝突、PID 身分、併發備份與還原。根目錄清單列明覆蓋範圍與替身。

简体中文：真正 Chromium 对隔离 HTTPS／SQLite 安装通过 9 场景、85 步骤。另完成图片上传、预览、提交重放、确定性 PDF、另一数据库封装导入、并发启动、重启保存、端口冲突、PID 身份、并发备份与恢复。根目录清单列明覆盖范围与替身。

EN: Container checks are static only: Docker is absent and user namespaces are denied on this PC. GitHub push exceeded the required five-second deadline; it was not retried. Evidence remains locally in `.local/refactor-evidence` and `build/reports/acceptance/uat`.

繁體中文：容器僅完成靜態檢查；此電腦沒有 Docker，使用者命名空間遭拒。GitHub 推送超過五秒期限，未重試。證據留在上述儲存庫內目錄。

简体中文：容器仅完成静态检查；此电脑没有 Docker，用户命名空间遭拒。GitHub 推送超过五秒期限，未重试。证据保留在上述仓库内目录。

[Coverage / 覆蓋 / 覆盖](../../text-coverage.txt) · [Pending / 待辦 / 待办](../../todo.txt)
