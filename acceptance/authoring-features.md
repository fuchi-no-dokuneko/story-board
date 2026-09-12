# Authoring features / 撰寫功能 / 编写功能

EN: After installation, start the API and register a novel using the standalone Skill. Keep at least two ready styles. This real Chromium check opens the novel, switches between one and two styles, downloads PDF/TXT, checks the four general statistics and verifies UTF-8 text against the selected revision, and visits the math page. Every run creates a fresh output directory so old downloads cannot make a test pass. Inspect the PDF pages visually for layout and glyph coverage.

繁體中文：安裝後啟動 API，以獨立 Skill 登錄小說，保留至少兩種可用風格。此真正 Chromium 測試開啟小說、切換單一／兩種風格、下載 PDF／TXT、驗證四個一般統計欄位與下載全文，再檢查公式頁。每次使用新目錄，避免舊下載造成假通過；另須實際查看 PDF 排版與字形。

简体中文：安装后启动 API，以独立 Skill 登记小说，保留至少两种可用风格。此真正 Chromium 测试打开小说、切换单一／两种风格、下载 PDF／TXT、验证四个通用统计字段与下载全文，再检查公式页。每次使用新目录，避免旧下载造成假通过；另须实际查看 PDF 排版与字形。

```bash
node acceptance/authoring-features.cjs https://127.0.0.1:8443 nov_Ab123 server/data/uat
```
