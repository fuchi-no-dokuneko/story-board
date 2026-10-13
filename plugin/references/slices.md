# Context reads / 上下文閱讀 / 上下文阅读

EN: IDs identify entities; indexes locate positions. `nov_`, `ch_`, `scn_`, `blk_`, `blv_`, `rev_` use five case-sensitive alphanumeric characters. Pin `revision-id`: edits change indices. Scenes number all blocks from zero; `block_count` and [0,N) include hidden blocks. `total_text_graphemes` counts visible prose including punctuation, not tokens.

繁體中文：六類 ID 使用五位區分大小寫英數後綴。切片固定版本，編輯會改變 index。場景從零編號，`block_count` 與 [0,N) 包含隱藏區塊；`total_text_graphemes` 只計可見正文及標點，不是 token。

简体中文：六类 ID 使用五位区分大小写英数后缀。切片固定版本，编辑会改变 index。场景从零编号，`block_count` 与 [0,N) 包含隐藏区块；`total_text_graphemes` 只计可见正文及标点，不是 token。

```bash
node scripts/storyblock-author.mjs chapters --novel-id nov_Ab123 --revision-id rev_Cd456 --json
node scripts/storyblock-author.mjs scenes --novel-id nov_Ab123 --revision-id rev_Cd456 --start 0 --end 1 --json
node scripts/storyblock-author.mjs blocks --novel-id nov_Ab123 --revision-id rev_Cd456 --scene-id scn_Ef789 --start 0 --end 3 --json
```

EN: `scenes` slices chapters; `blocks` slices one scene, both [start,end). Records are shared without user/IP tracking. Within 300 seconds, read the edit target and immediate neighbors at its revision, including hidden neighbors. Empty scenes require [0,0). Lists never grant reading. On 409 `RECENT_READ_REQUIRED`, inspect returned `read_requests` and text before retrying; do not auto-read merely to bypass the guard.

繁體中文：`scenes` 切章節，`blocks` 切場景，均採 [start,end)。紀錄共用且不追蹤身份。編輯前 300 秒內讀取同版本目標及鄰塊（含隱藏區塊）；空場景讀 [0,0)，資料列表不算。409 時依 `read_requests` 重讀並理解正文，再重試，勿空讀繞過守衛。

简体中文：`scenes` 切章节，`blocks` 切场景，均采用 [start,end)。记录共用且不追踪身份。编辑前 300 秒内读取同版本目标及邻块（含隐藏区块）；空场景读 [0,0)，资料列表不算。409 时依 `read_requests` 重读并理解正文，再重试，勿空读绕过守卫。

EN: Slices return `extensions["storyblock.hidden"]: true`. Read a scene's hidden meaning before edits; keep new prose consistent and update notes when plot design changes. Position ties each note to its preceding passage.

繁體中文：切片會回傳隱藏旗標。編輯前閱讀場景隱藏含義，保持新正文一致，劇情變更時更新記錄；位置對應前一段。

简体中文：切片会返回隐藏旗标。编辑前阅读场景隐藏含义，保持新正文一致，剧情变更时更新记录；位置对应前一段。
