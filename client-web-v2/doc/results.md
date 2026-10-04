# Results / 驗證結果 / 验证结果

EN: 2026-10-04: 231 Java tests, 22 JavaScript tests and 12 Firefox 156
acceptance groups passed. All 27 supplied ZIP assertions and 15 additional
checks passed with real HTTPS/API/SQLite data; no uncaught browser errors.

繁體：231 項 Java、22 項 JavaScript、12 組 Firefox 驗收通過。
ZIP 的 27 項及額外 15 項檢查全部使用真實 HTTPS／API／SQLite，無未捕捉瀏覽器錯誤。

简体：231 项 Java、22 项 JavaScript、12 组 Firefox 验收通过。
ZIP 的 27 项及额外 15 项检查全部使用真实 HTTPS／API／SQLite，无未捕获浏览器错误。

| Evidence / 證據 / 证据 | Scope / 範圍 / 范围 |
| --- | --- |
| ZIP CSS | Original styles preserved verbatim / 原稿樣式完整保留 / 原稿样式完整保留 |
| F1 | 714 real windows, chart, keys, drag, evidence, bands, paging / 真實視窗、圖表、鍵盤、拖曳、證據、標示、分頁 / 真实窗口、图表、键盘、拖曳、证据、标示、分页 |
| F2–F5 | Empty facts, appearance, metric labels, plurals, upright digits / 空欄位、外觀、標籤、複數、直排數字 / 空字段、外观、标签、复数、直排数字 |
| F6 | Reopen performs one novel GET / 重開只下載一次 / 重开只下载一次 |
| Cache / 快取 / 缓存 | Concurrent load sharing; revision/name/reference refresh; retry / 並行共用、輸入更新、重試 / 并发共用、输入更新、重试 |
| Regression / 回歸 / 回归 | v1, images, exports, 320–1920 px, 6000 blocks / 圖片、匯出、多尺寸、長篇 / 图片、导出、多尺寸、长篇 |

EN: On a local 16-novel fixture, first request → median of 5 repeats (ms):
catalog 1113 → 10.8; detail 104.9 → 40.7; quality 662.3 → 39.2;
style 642.5 → 5.0. All repeated responses were identical; 12 concurrent
HTTP readers also matched. These timings are local measurements.

繁體：本機 16 部小說的首次／重複五次中位數如上，回應均相同；12 個並行 HTTP 讀取也一致。

简体：本机 16 部小说的首次／重复五次中位数如上，响应均相同；12 个并发 HTTP 读取也一致。

EN: Quality fixtures are synthetic test texts, not reference corpora. Faults
and delays are simulated. VPS deployment is separate; local success does not
update the remote process. See [deployment](deploy.md) and [cache](../../server/doc/cache.md).

繁體：品質範例為合成測試文字，不是參考語料；故障／延遲使用替身。本機通過不代表 VPS 已部署。

简体：品质范例为合成测试文字，不是参考语料；故障／延迟使用替身。本机通过不代表 VPS 已部署。
