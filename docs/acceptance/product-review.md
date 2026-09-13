# Product review / 產品驗收 / 产品验收

EN: ADR-311 removes Zombies/TNT from registration, catalog, UI and Skill contracts. Ordinary novels accept any distinct cast size, including zero, and non-Han prose. English sentence spaces survive registration. Unchanged imported presence omissions remain warnings; new/changed cues still require annotations. PDF paragraphs/captions retain their fonts after page breaks.

繁體中文：移除登錄、書庫、介面及 Skill 的 Zombies／TNT 欄位；解除固定五位角色與必須含漢字限制，保留英文句間空格。未修改原文的既有出入場缺漏列為警告；新增／修改仍須標註。修正 PDF 段落及圖說換頁後字體縮小。

简体中文：移除登记、书库、界面及 Skill 的 Zombies／TNT 字段；解除固定五位角色与必须含汉字限制，保留英文句间空格。未修改原文的已有出入场缺漏列为警告；新增／修改仍须标注。修正 PDF 段落及图说换页后字体缩小。

EN: Registered one new ordinary novel, `nov_x2Zx6` (末班車之前): 535 Han, two characters, two chapters/scenes, 14 blocks. Real Chromium checks compare the complete reader text with the source, search/navigate, switch one/two styles, inspect five distance columns and formulas, and download fresh PDF/TXT. Desktop/mobile screenshots and PDF body pages were inspected. A real edit and restoration retained block IDs; an unread revision returned `RECENT_READ_REQUIRED`, then reading enabled commit and idempotent replay.

繁體中文：以新小說《末班車之前》驗證全文、統計、搜尋、章節、單一／兩種風格與五欄距離、公式及下載，實際查看桌面／手機畫面及 PDF 正文。編輯後還原原文；未讀新版被拒絕，閱讀後可提交，同一請求重試成功。

简体中文：以新小说《末班车之前》验证全文、统计、搜索、章节、单一／两种风格与五列距离、公式及下载，实际查看桌面／手机画面及 PDF 正文。编辑后还原原文；未读新版被拒绝，阅读后可提交，同一请求重试成功。

EN: Local receipts: `.local/product-review/`; browser output: `server/data/uat/product-review/`. Installed HTTPS build verified locally; VPS not deployed. Database: `server/data/storyblock.db`. See [coverage](../../text-coverage.txt) for counts and simulated boundaries.

繁體中文：上述路徑保存本機證據；驗收安裝後的 HTTPS 版本，未部署 VPS。測試數量與模擬界線見覆蓋清單。

简体中文：上述路径保存本机证据；验收安装后的 HTTPS 版本，未部署 VPS。测试数量与模拟边界见覆盖清单。
