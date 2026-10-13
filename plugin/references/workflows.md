# Authoring / 撰寫 / 编写

EN: Register a complete manuscript with the seven-field `AgentNovelRegistrationRequest`, distinct main characters (empty allowed), canonical UTC time, exact Han code-point count and one fresh novel ID. Validate, register and verify; retry timeouts with identical source and idempotency key.

繁體中文：完整稿件使用七欄登錄請求、主角列表（可空）、標準 UTC 時間、正確漢字碼點數及新作品 ID。驗證、登錄、確認保存；逾時以相同來源及冪等鍵重試。

简体中文：完整稿件使用七字段登记请求、主角列表（可空）、标准 UTC 时间、正确汉字码点数及新作品 ID。验证、登记、确认保存；超时以相同来源及幂等键重试。

```bash
node scripts/storyblock-author.mjs validate --dto AgentNovelRegistrationRequest --file manuscript.json
node scripts/storyblock-author.mjs register --source manuscript.json --json
node scripts/storyblock-author.mjs verify --source manuscript.json --json
```

EN: Read an existing novel's live head, target and neighbors. Use a normal `OperationEnvelope` with live IDs; preview, inspect violations and commit identical operation, candidate ID and time. If the head changes, reread and prepare again. Export pins the revision ETag; transfers keep history, images and hidden blocks.

繁體中文：既有作品先讀版本頭、目標及鄰塊。使用正常編輯請求與真實 ID，預覽並檢查違規，提交相同操作、候選 ID 與時間；版本頭變更時重讀及重建。匯出固定版本 ETag，轉移保留歷史、圖片及隱藏區塊。

简体中文：已有作品先读版本头、目标及邻块。使用正常编辑请求与真实 ID，预览并检查违规，提交相同操作、候选 ID 与时间；版本头变更时重读及重建。导出固定版本 ETag，转移保留历史、图片及隐藏区块。

```bash
node scripts/storyblock-author.mjs read --novel-id nov_Ab123 --json
node scripts/storyblock-author.mjs preview-edit --novel-id nov_Ab123 --file edit.json --json
node scripts/storyblock-author.mjs commit --novel-id nov_Ab123 --file edit.json --json
```

EN: Insert `hidden: true` after the passage it explains. Record what readers are not told: subtext, foreshadowing and payoff chapter/scene, symbolism, motive and scene purpose. Read these notes before editing/continuing; align new prose and update changed plot design.

繁體中文：在原段後插入 `hidden: true`，記錄讀者未獲告知的潛台詞、伏筆與回收章節／場景、象徵、動機及場景作用。編輯／續寫前閱讀，保持正文一致，劇情變更時同步更新。

简体中文：在原段后插入 `hidden: true`，记录读者未获告知的潜台词、伏笔与回收章节／场景、象征、动机及场景作用。编辑／续写前阅读，保持正文一致，剧情变更时同步更新。

[DTOs / 綱要 / 纲要](dtos.md) · [Validation / 驗證 / 验证](edit-validation.md)
