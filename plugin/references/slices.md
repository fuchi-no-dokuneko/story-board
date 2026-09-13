# Context reads / 上下文閱讀 / 上下文阅读

EN: IDs identify entities; indexes locate positions. `nov_`, `ch_`, `scn_`, `blk_`, `blv_`, `rev_` use five case-sensitive alphanumeric characters. Every scene numbers live blocks from zero. Pin `revision-id` while using slices; indices change after edits. Chapter and scene lists contain metadata only. Scenes report `block_count`, `[0,N)` and `total_text_graphemes` including punctuation, not LLM tokens.

繁體中文：ID 識別實體，index 表示位置。上述六類 ID 後接五個區分大小寫的英數字元。每個場景的區塊從零編號；切片須固定版本，編輯後位置可改變。章節與場景列表只有資料，包含區塊數、[0,N) 及含標點的字素總數，不是 LLM token 數。

简体中文：ID 标识实体，index 表示位置。上述六类 ID 后接五个区分大小写的英数字符。每个场景的区块从零编号；切片须固定版本，编辑后位置可改变。章节与场景列表只有资料，包含区块数、[0,N) 及含标点的字素总数，不是 LLM token 数。

```bash
node scripts/storyblock-author.mjs chapters --novel-id nov_Ab123 --revision-id rev_Cd456 --json
node scripts/storyblock-author.mjs scenes --novel-id nov_Ab123 --revision-id rev_Cd456 --start 0 --end 1 --json
node scripts/storyblock-author.mjs blocks --novel-id nov_Ab123 --revision-id rev_Cd456 --scene-id scn_Ef789 --start 0 --end 3 --json
```

EN: `scenes` slices chapters; `blocks` slices one scene. Both use [start,end). Reading records are shared without user/IP tracking. Read the body of the edit target and immediate neighbors within 300 seconds at the edit revision. Insertions read adjacent existing blocks; empty scenes require [0,0). Metadata lists never grant reading. On HTTP 409 `RECENT_READ_REQUIRED`, read the returned `read_requests` and inspect the text before retrying. Do not silently auto-read only to bypass the rule.

繁體中文：`scenes` 切章節，`blocks` 切單一場景，均採 [start,end)。閱讀紀錄共用，不追蹤身份。編輯前 300 秒內讀取同版本目標與鄰塊正文，插入讀兩側既有塊，空場景讀 [0,0)。只有資料的列表不計。409 錯誤時依 `read_requests` 重讀並理解正文，再重試；勿自動空讀繞過規則。

简体中文：`scenes` 切章节，`blocks` 切单一场景，均采用 [start,end)。阅读记录共用，不追踪身份。编辑前 300 秒内读取同版本目标与邻块正文，插入读两侧已有块，空场景读 [0,0)。只有资料的列表不计。409 错误时依 `read_requests` 重读并理解正文，再重试；勿自动空读绕过规则。
