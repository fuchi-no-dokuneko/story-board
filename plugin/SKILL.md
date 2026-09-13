---
name: storyblock-author
description: Write and edit StoryBlock novels through a standalone HTTPS CLI; discover DTOs, read chapter/scene/block slices, satisfy recent-reading guards, compare server reference styles, upload images, and export PDFs or canonical data.
---

# StoryBlock Author

EN: Run `./install-local.sh` here once, then `node scripts/storyblock-author.mjs`. This folder and Node are sufficient; no full repo or local style analyzer is needed. Use IPv4 HTTPS and accept the automatically generated self-signed certificate. Reachable clients need no credential or approval. Set the endpoint with `--base-url`.

繁體中文：先執行此目錄的 `./install-local.sh`，再用 `node scripts/storyblock-author.mjs`。只需此目錄與 Node，不需完整 repo 或本機風格分析器。採 IPv4 HTTPS，接受自動產生的自簽憑證；可連線裝置不需密鑰或核准。以 `--base-url` 指定端點。

简体中文：先运行此目录的 `./install-local.sh`，再用 `node scripts/storyblock-author.mjs`。只需此目录与 Node，不需完整 repo 或本机风格分析器。采用 IPv4 HTTPS，接受自动生成的自签证书；可连接设备不需密钥或批准。以 `--base-url` 指定端点。

EN: Register then verify persistence. The six novel/chapter/scene/block/block-version/revision IDs have five alphanumeric suffix characters. Before editing, read the target and neighbors at the same revision within 300 seconds. Insertions require both existing neighbors; read [0,0) for an empty scene. Metadata lists do not count. Follow `RECENT_READ_REQUIRED.read_requests`; inspect returned text, preview, then commit the identical request. Preserve IDs, body, timestamp and idempotency key on retries.

繁體中文：登錄後驗證持久化。六類小說／章節／場景／區塊／區塊版本／小說版本 ID 使用五位英數後綴。編輯前 300 秒內須讀取同版本目標及鄰塊；插入須讀兩側既有區塊，空場景讀 [0,0)。資料列表不算閱讀。依 `RECENT_READ_REQUIRED.read_requests` 重讀正文，預覽後提交相同請求；重試保留 ID、內文、時間與冪等鍵。

简体中文：登记后验证持久化。六类小说／章节／场景／区块／区块版本／小说版本 ID 使用五位英数后缀。编辑前 300 秒内须读取同版本目标及邻块；插入须读两侧已有区块，空场景读 [0,0)。资料列表不算阅读。依 `RECENT_READ_REQUIRED.read_requests` 重读正文，预览后提交相同请求；重试保留 ID、正文、时间与幂等键。

- [Authoring / 撰寫 / 编写](references/workflows.md)
- [Slices / 切片 / 切片](references/slices.md)
- [Styles / 風格 / 风格](references/server-styles.md)
- [Images/PDF / 圖片匯出 / 图片导出](references/images-pdf.md)
- [Endpoints / 端點 / 端点](references/endpoints.md)
- [DTOs / 契約 / 契约](references/dtos.md)
- [Errors / 錯誤 / 错误](references/error-model.md)
