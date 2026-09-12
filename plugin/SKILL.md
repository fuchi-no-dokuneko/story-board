---
name: storyblock-author
description: Discover StoryBlock HTTPS endpoints and DTOs, register and verify manuscripts, preview and commit typed edits, upload images, render PDFs, and transfer canonical data using the bundled standalone CLI.
---

# StoryBlock Author

EN: Run `./install-local.sh` in this folder once to assemble bundled sources. Use `node scripts/storyblock-author.mjs`. Discover with `endpoints`, `describe`, and `dtos`; validate the exact DTO before a mutation. Use only HTTPS over IPv4 and accept the server's local self-signed leaf. Never request or upload TLS material. The default reachable-client mode needs no credential or approval; remote access uses a private tunnel to loopback.

繁體中文：先在此目錄執行一次 `./install-local.sh` 組合隨附來源，再使用 `node scripts/storyblock-author.mjs`。以 `endpoints`、`describe`、`dtos` 查詢，變更前驗證精確 DTO。僅用 IPv4 HTTPS，接受本機自簽憑證，不要求或上傳 TLS 材料。預設可連線客戶端不需密鑰或核准，遠端透過私有通道連接 loopback。

简体中文：先在此目录执行一次 `./install-local.sh` 组合随附源码，再使用 `node scripts/storyblock-author.mjs`。以 `endpoints`、`describe`、`dtos` 查询，变更前验证精确 DTO。仅用 IPv4 HTTPS，接受本机自签证书，不要求或上传 TLS 材料。默认可连接客户端不需密钥或批准，远程通过私有通道连接 loopback。

EN: Register a complete manuscript, then verify persistence before claiming completion. For edits, read the live head, preview, and commit the unchanged request. Retries preserve body, IDs, timestamp, and idempotency key. Never infer an undocumented field. Image/PDF and transfer operations use their exact schemas; existing canonical hashes and schema fields remain protocol data.

繁體中文：完整稿件先登錄，再驗證持久化後才宣告完成。編輯時讀取即時版本頭，預覽後提交不變的請求。重試保留內文、ID、時間及冪等鍵，不推測未記錄欄位。圖片、PDF 與轉移採精確綱要；既有標準雜湊與綱要欄位仍為協定資料。

简体中文：完整稿件先登记，再验证持久化后才宣告完成。编辑时读取实时版本头，预览后提交不变的请求。重试保留正文、ID、时间及幂等键，不推测未记录字段。图片、PDF 与转移采用精确纲要；既有标准哈希与纲要字段仍为协议数据。

- [Workflows / 流程 / 流程](references/workflows.md)
- [Images and PDF / 圖片與 PDF / 图片与 PDF](references/images-pdf.md)
- [Access / 存取 / 访问](references/authentication.md)
- [Errors / 錯誤 / 错误](references/error-model.md)
- [Endpoints / 端點 / 端点](references/endpoints.md)
- [DTOs / 資料型別 / 数据类型](references/dtos.md)
