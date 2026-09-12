# Authoring verification / 撰寫驗收 / 编写验收

EN: Verified 2026-09-12 using the installed HTTPS build. All 13 requirements are implemented for fresh installation. VPS untouched.

繁體中文：2026-09-12 已對安裝後的 HTTPS 版本完成 13 項撰寫／風格需求驗收。採全新安裝，不提供舊資料遷移；未部署或修改既有 VPS。

简体中文：2026-09-12 已对安装后的 HTTPS 版本完成 13 项编写／风格需求验收。采用全新安装，不提供旧数据迁移；未部署或修改现有 VPS。

| Scope / 範圍 / 范围 | Evidence / 證據 / 证据 |
|---|---|
| IDs, retries / 短識別與重試 / 短标识与重试 | `ShortIdPersistenceTest`, `ShortIdOwnershipTest`; live restart replay |
| Slices, counts, reads / 切片與閱讀 / 切片与阅读 | `NarrativeFeatureHttpTest`, `RecentReadLedgerTest`; live insert/delete with stable IDs |
| Sources / 參考來源 / 参考来源 | `StyleLibraryRefreshTest`; live 60-second external-directory refresh |
| Async, no stored results / 非同步不存結果 / 异步不存结果 | Eight concurrent HTTPS comparisons returned 200; database job/profile counts stayed zero |
| GUI / 介面 / 界面 | 9 scenarios, 85 steps; separate comparison, PDF/TXT and formula-page browser run |
| Installation / 安裝 / 安装 | Real lifecycle, keyless backup/restore, external data-symlink boot; architecture checks |

EN: Passed: Java 195 + 2 reference tests; CLI 187; browser assets 4. Read expiry uses a controlled clock; MockMvc tests simulate servlet transport. GUI uses real Chromium, HTTPS and SQLite; only degraded-health/offline cases are simulated.

繁體中文：Java 全套 195 項加參考文字 2 項、CLI 187 項、介面資源 4 項均通過。期限用受控時鐘，MockMvc 模擬傳輸；GUI 使用真正 Chromium、HTTPS、SQLite，僅降級與斷線為模擬。

简体中文：Java 全套 195 项加参考文字 2 项、CLI 187 项、界面资源 4 项均通过。期限用受控时钟，MockMvc 模拟传输；GUI 使用真实 Chromium、HTTPS、SQLite，仅降级与断线为模拟。

- [Run GUI / 執行介面驗收 / 执行界面验收](../../acceptance/authoring-features.md)
- [Configure TXT / 設定文字 / 配置文本](../../server/doc/styles.md)
- [Standalone Skill / 獨立技能 / 独立技能](../../plugin/SKILL.md)

EN: Local receipts/logs: `.local/authoring-work/`; downloads/screenshots: `server/data/uat/`. Database: `server/data/storyblock.db`; `server/data` supports operator-created symlinks. PDF pages are raster images; TXT is UTF-8. Docker execution was unavailable; container checks were static.

繁體中文：上述路徑保存本機證據與資料庫，data 支援使用者建立的連結。PDF 為頁面影像，TXT 為 UTF-8；Docker 未執行，僅靜態檢查。

简体中文：上述路径保存本机证据与数据库，data 支持用户创建的链接。PDF 为页面图像，TXT 为 UTF-8；Docker 未执行，仅静态检查。
