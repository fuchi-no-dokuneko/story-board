# Edit validation / 編輯驗證 / 编辑验证

EN: Preview the exact operation and inspect both violations and warnings. New or changed blocks containing explicit enter/exit cues require matching `presence_events` and valid evidence spans. Correct the metadata when editing such a passage; do not rewrite the prose merely to evade validation.

繁體中文：預覽完全相同的操作，檢查錯誤與警告。新增或修改的區塊若有明確出入場敘述，須提供相符的 `presence_events` 與有效證據範圍。編輯該段時補正標註，不要為了繞過驗證而改寫原文。

简体中文：预览完全相同的操作，检查错误与警告。新增或修改的区块若有明确出入场叙述，须提供相符的 `presence_events` 与有效证据范围。编辑该段时补正标注，不要为了绕过验证而改写原文。

EN: Imported text may already contain unannotated presence cues. If a block remains exactly unchanged, these existing omissions appear as warnings and do not block unrelated corrections. Other errors still block commits. A warning does not certify narrative continuity; report unresolved annotations accurately.

繁體中文：匯入文字可能已有未標註的出入場敘述。區塊完全未變更時，既有缺漏列為警告，不阻擋其他段落的修正；其他錯誤仍阻擋提交。警告不代表敘事連貫性已確認，應如實回報未補齊的標註。

简体中文：导入文字可能已有未标注的出入场叙述。区块完全未变更时，已有缺漏列为警告，不阻挡其他段落的修正；其他错误仍阻挡提交。警告不代表叙事连贯性已确认，应如实报告未补齐的标注。

EN: Hidden drafts use `hidden: true`, never the reserved extension directly. Text is NFC, stripped, 1–2000 graphemes; line breaks allowed. Meta is empty or provenance only; images are forbidden. Insert, replace, delete and move work; split, merge and extend reject hidden blocks. Replace one block with the same ID to set/remove the flag; removal must pass visible text rules. Hidden range endpoints return 422 `HIDDEN_BLOCK_NOT_RENDERABLE`. Rewrite ranges cannot cross hidden blocks.

繁體中文：隱藏草稿用 `hidden: true`；文字 NFC 去首尾空白，1–2000 字素，可換行。中繼資料只允許來源，不允許圖片。可插入、取代、刪除、移動；拆分、合併、延伸會拒絕。以相同 ID 取代單塊切換旗標，移除時須符合可見正文規則；隱藏端點及跨隱藏塊改寫會拒絕。

简体中文：隐藏草稿用 `hidden: true`；文字 NFC 去首尾空白，1–2000 字素，可换行。元数据只允许来源，不允许图片。可插入、替换、删除、移动；拆分、合并、延伸会拒绝。以相同 ID 替换单块切换旗标，移除时须符合可见正文规则；隐藏端点及跨隐藏块改写会拒绝。
