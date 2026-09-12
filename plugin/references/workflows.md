# Authoring / 撰寫 / 编写

EN: For a new complete manuscript, use the exact nine-field `AgentNovelRegistrationRequest`, five distinct main characters, canonical UTC time, and exact Han code-point count. Generate a fresh typed novel ID once. Validate, register, then verify. A timeout is retried with the same source and idempotency key.

繁體中文：新完整稿件使用精確九欄的 `AgentNovelRegistrationRequest`、五位不同主角、標準 UTC 時間及正確漢字碼點數。只產生一次新作品 ID。先驗證、登錄，再驗證保存結果；逾時使用相同來源與冪等鍵重試。

简体中文：新完整稿件使用精确九字段的 `AgentNovelRegistrationRequest`、五位不同主角、标准 UTC 时间及正确汉字码点数。只生成一次新作品 ID。先验证、登记，再验证保存结果；超时使用相同源码与幂等键重试。

```bash
node scripts/storyblock-author.mjs validate --dto AgentNovelRegistrationRequest --file manuscript.json
node scripts/storyblock-author.mjs register --source manuscript.json --json
node scripts/storyblock-author.mjs verify --source manuscript.json --json
```

EN: For an existing novel, read the live head and revision. Use one of the ten `OperationEnvelope` variants and live scene/block/version IDs. Preview the request, inspect violations, and commit only the identical operation, candidate ID, and time. If the head changes, read again and prepare a new operation. Export uses the exact revision ETag; package transfer includes history and image artifacts.

繁體中文：既有作品先讀取即時版本頭與修訂。採用十種 `OperationEnvelope` 之一及真實場景、區塊、版本 ID。預覽並檢查違規，僅提交相同操作、候選 ID 與時間；版本頭變更時重新讀取及建立操作。匯出使用精確修訂 ETag，封裝轉移包含歷史與圖片產物。

简体中文：既有作品先读取实时版本头与修订。采用十种 `OperationEnvelope` 之一及真实场景、区块、版本 ID。预览并检查违规，仅提交相同操作、候选 ID 与时间；版本头变更时重新读取及创建操作。导出使用精确修订 ETag，封装转移包含历史与图片产物。

```bash
node scripts/storyblock-author.mjs read --novel-id nov_UUIDV7 --json
node scripts/storyblock-author.mjs preview-edit --novel-id nov_UUIDV7 --file edit.json --json
node scripts/storyblock-author.mjs commit --novel-id nov_UUIDV7 --file edit.json --json
```

[DTOs / 綱要 / 纲要](dtos.md) · [Images / 圖片 / 图片](images-pdf.md)
