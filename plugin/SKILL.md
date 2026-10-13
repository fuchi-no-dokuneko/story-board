---
name: storyblock-author
description: Write and edit StoryBlock novels through a standalone HTTPS CLI; discover DTOs, read slices and hidden meaning, satisfy reading guards, export books or canonical data.
---

# StoryBlock Author

EN: Run `./install-local.sh`, then `node scripts/storyblock-author.mjs`. Set `--base-url` to IPv4 HTTPS; accept the generated self-signed certificate. Reachable devices need no credentials.

繁體中文：先執行 `./install-local.sh`，再用 `node scripts/storyblock-author.mjs`。只需此目錄與 Node。`--base-url` 指定 IPv4 HTTPS，接受自動產生的自簽憑證；可連線裝置不需密鑰或核准。

简体中文：先运行 `./install-local.sh`，再用 `node scripts/storyblock-author.mjs`。只需此目录与 Node。`--base-url` 指定 IPv4 HTTPS，接受自动生成的自签证书；可连接设备不需密钥或批准。

EN: Register and verify. IDs use five alphanumeric suffix characters. Read targets and neighbors at the same revision within 300 seconds; read [0,0) for an empty scene. Lists do not count. Inspect `RECENT_READ_REQUIRED.read_requests`, preview and commit the identical request. Preserve IDs, body, time and idempotency key on retries.

繁體中文：登錄後驗證保存結果。六類 ID 使用五位英數後綴。編輯前 300 秒內須讀取同版本目標及鄰塊；空場景讀 [0,0)，資料列表不算。依 `RECENT_READ_REQUIRED.read_requests` 重讀正文，預覽後提交相同請求；重試保留 ID、內文、時間與冪等鍵。

简体中文：登记后验证保存结果。六类 ID 使用五位英数后缀。编辑前 300 秒内须读取同版本目标及邻块；空场景读 [0,0)，资料列表不算。依 `RECENT_READ_REQUIRED.read_requests` 重读正文，预览后提交相同请求；重试保留 ID、正文、时间与幂等键。

EN: Read a scene's hidden blocks before editing or continuing it. Place `hidden: true` immediately after its passage to record subtext, foreshadowing/payoff chapter and scene, symbolism, motive or plot purpose. Align prose; update changed plot design.

繁體中文：編輯或續寫前閱讀隱藏區塊。以 `hidden: true` 緊接原段記錄潛台詞、伏筆及回收章節／場景、象徵、動機或劇情作用；保持正文一致，劇情變更時更新記錄。

简体中文：编辑或续写前阅读隐藏区块。以 `hidden: true` 紧接原段记录潜台词、伏笔及回收章节／场景、象征、动机或剧情作用；保持正文一致，剧情变更时更新记录。

- [Authoring / 撰寫 / 编写](references/workflows.md)
- [Slices / 切片 / 切片](references/slices.md)
- [Validation / 驗證 / 验证](references/edit-validation.md)
- [Styles / 風格 / 风格](references/server-styles.md)
- [Images/PDF / 圖片匯出 / 图片导出](references/images-pdf.md)
- [Endpoints / 端點 / 端点](references/endpoints.md)
- [DTOs / 契約 / 契约](references/dtos.md)
- [Hidden / 隱藏 / 隐藏](references/hidden-blocks.md)
