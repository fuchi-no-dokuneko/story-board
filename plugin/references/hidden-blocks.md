# Hidden meaning / 隱藏含義 / 隐藏含义

EN: These notes are public to anyone who can read the novel. They remain in scene order, canonical revisions, history and transfers. Prose rendering, PDF/HTML/TXT books, style, quality, detection, monitoring and rewrites exclude them.

繁體中文：可閱讀小說的人皆可讀取註記。註記保留場景順序、正式版本、歷史及轉移資料；正文渲染、PDF／HTML／TXT 書籍及各項文字分析會略過。

简体中文：可阅读小说的人皆可读取注记。注记保留场景顺序、正式版本、历史及转移数据；正文渲染、PDF／HTML／TXT 书籍及各项文字分析会略过。

EN: Insert a draft with the normal preview and commit workflow. Place it immediately after its passage. Record subtext, foreshadowing and payoff chapter/scene, symbolism, motive or plot purpose. Read the scene's notes before editing or continuing; align prose and update changed plot design.

繁體中文：循正常預覽及提交流程，在原段後插入草稿。記錄潛台詞、伏筆與回收章節／場景、象徵、動機或劇情作用；編輯或續寫前閱讀，保持正文一致，劇情變更時更新。

简体中文：循正常预览及提交流程，在原段后插入草稿。记录潜台词、伏笔与回收章节／场景、象征、动机或剧情作用；编辑或续写前阅读，保持正文一致，剧情变更时更新。

```json
{"id":"blk_Ab123","text":"The cracked teacup foreshadows Lin's break with her brother in chapter 9, scene 2.","meta":{},"hidden":true}
```

EN: Drafts use `hidden: true`; stored blocks and slices use `extensions["storyblock.hidden"]: true`. Use no other truthy value. Text allows 1–2000 NFC, stripped graphemes and line breaks; metadata allows only provenance; images cannot coexist. Split/merge/extend reject notes. Replace the same ID to toggle the flag; visible rules apply on removal.

繁體中文：草稿用 `hidden: true`，保存及切片用隱藏 extension。文字限 1–2000 個 NFC 去首尾空白字素，可換行；中繼資料只允許來源，不可附圖片。不得拆分、合併、延伸；以相同 ID 取代可切換旗標，移除時適用可見規則。

简体中文：草稿用 `hidden: true`，保存及切片用隐藏 extension。文字限 1–2000 个 NFC 去首尾空白字素，可换行；元数据只允许来源，不可附图片。不得拆分、合并、延伸；以相同 ID 替换可切换旗标，移除时适用可见规则。

Codes / 驗證碼 / 验证码: `HIDDEN_BLOCK_TEXT_INVALID`, `HIDDEN_BLOCK_META_NOT_ALLOWED`, `HIDDEN_BLOCK_UNSUPPORTED`. Hidden prose range endpoints return HTTP 422 `HIDDEN_BLOCK_NOT_RENDERABLE`.
