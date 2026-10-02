# Results / 驗證結果 / 验证结果

EN: Verified on 2026-10-02: 225 Java tests, 17 browser logic tests, and seven
Firefox 153 acceptance groups passed. The packaged API served both frontends.
The inspected story's revision stayed unchanged throughout browser acceptance.

繁體：2026-10-02 驗證：225 項 Java、17 項前端邏輯測試及七組 Firefox 153 驗收通過。
封裝後的 API 同時提供新舊前端；瀏覽器驗收期間，受測小說修訂未變更。

简体：2026-10-02 验证：225 项 Java、17 项前端逻辑测试及七组 Firefox 153 验收通过。
打包后的 API 同时提供新旧前端；浏览器验收期间，受测小说修订未更改。

| Group / 組別 / 组别 | Evidence / 證據 / 证据 |
| --- | --- |
| Library / 書庫 / 书库 | 14 stories; paging, search, language filter, empty result / 14 部、分頁、搜尋、篩選、空結果 / 14 部、分页、搜索、筛选、空结果 |
| Reading / 閱讀 / 阅读 | Chapters, resume, three UI languages, PNG/JPEG, metadata / 章節、續讀、三語、圖片、資料 / 章节、续读、三语、图片、信息 |
| Analysis / 分析 / 分析 | Five independent channels, tied minima, windows, exact source highlight / 五項獨立距離、並列最小值、視窗、原文標記 / 五项独立距离、并列最小值、窗口、原文标记 |
| Downloads / 下載 / 下载 | Browser-saved PDF, complete TXT and matching JSON / 瀏覽器下載 PDF、完整 TXT、吻合的 JSON / 浏览器下载 PDF、完整 TXT、吻合的 JSON |
| Layout / 版面 / 布局 | 320–1920 px; portrait/landscape, 2× DPI, 28 px text, night mode / 直橫向、高解析、放大文字、夜間 / 竖横向、高分辨率、放大文字、夜间 |
| Original / 原版 / 原版 | Reading, images, analysis, console, mobile / 閱讀、圖片、分析、主控台、手機 / 阅读、图片、分析、控制台、手机 |
| Recovery / 重試 / 重试 | Read/image failures recover; optional token stays in memory / 讀取與圖片重試、權杖不落地 / 读取与图片重试、令牌不落地 |

EN: API, SQLite, analysis, rendering, downloads and Firefox are real. Only the
recovery group's temporary HTTP failures are intercepted. Unit tests inject
transport/DOM/storage fixtures. No calibration corpus was invented.

繁體：API、SQLite、分析、渲染、下載及 Firefox 均為實際執行；僅重試情境攔截暫時 HTTP 失敗。
單元測試注入傳輸、DOM、儲存範例；未虛構校準語料。

简体：API、SQLite、分析、渲染、下载及 Firefox 均实际运行；仅重试场景拦截临时 HTTP 失败。
单元测试注入传输、DOM、存储示例；未虚构校准语料。
