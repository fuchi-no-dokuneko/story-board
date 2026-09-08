# Examples / 範例 / 示例

EN: `minimal-novel.json` validates offline. Generate a fresh novel ID before a new registration. Edit and render templates contain illustrative revision, scene, block, version, and hash values: replace them with the live head's values. Retries reuse the exact body and idempotency key.

繁體中文：`minimal-novel.json` 可離線驗證。新登錄前產生新作品 ID。編輯與渲染範本包含示意修訂、場景、區塊、版本與雜湊，須替換為即時版本頭數值。重試保留精確內文與冪等鍵。

简体中文：`minimal-novel.json` 可离线验证。新登记前生成新作品 ID。编辑与渲染模板包含示意修订、场景、区块、版本与哈希，须替换为实时版本头数值。重试保留精确正文与幂等键。

EN: For `image-block-draft.json`, use the complete `block_image` returned by upload; its text is the caption. `pdf-render-request.json` selects the immutable revision. Character-reference examples contain five identities, each with one initial image and plain-background variants. Validate templates before sending.

繁體中文：`image-block-draft.json` 使用上傳回傳的完整 `block_image`，文字為圖說。`pdf-render-request.json` 選擇不可變修訂。角色參照範例包含五個身分，各有初始圖與純色背景變體。送出前驗證範本。

简体中文：`image-block-draft.json` 使用上传返回的完整 `block_image`，文字为图注。`pdf-render-request.json` 选择不可变修订。角色引用示例包含五个身份，各有初始图与纯色背景变体。发送前验证模板。

```bash
node scripts/storyblock-author.mjs validate --dto BlockDraft --file examples/image-block-draft.json
node scripts/storyblock-author.mjs validate --dto PdfRenderRequest --file examples/pdf-render-request.json
```
