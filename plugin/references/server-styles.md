# Server styles / 伺服器風格 / 服务器风格

EN: References and analysis live on the server. Run `styles`, then write a request JSON such as `{"style_ids":["lyrical","suspense"]}`. An optional `revision_id` pins an older revision. `compare-styles` obtains the revision ETag, submits, and waits up to 300 seconds for HTTP 200 on the same request. No task ID or polling exists. Each call recalculates novel scores; only reference benchmarks are retained. Generic `call` also supports these endpoints; use `--timeout-ms 300000` for long comparisons.

繁體中文：參考文字與分析在伺服器。先用 `styles`，再建立上述 JSON；可加 `revision_id` 指定版本。`compare-styles` 取得 ETag 後送出，最多等待 300 秒，計算完成於同一請求回 200。沒有 task ID 或輪詢；每次重算小說分數，僅保留參考 benchmark。通用 `call` 可加 `--timeout-ms 300000`。

简体中文：参考文字与分析在服务器。先用 `styles`，再创建上述 JSON；可加 `revision_id` 指定版本。`compare-styles` 取得 ETag 后发送，最多等待 300 秒，计算完成于同一请求返回 200。没有 task ID 或轮询；每次重算小说分数，仅保留参考 benchmark。通用 `call` 可加 `--timeout-ms 300000`。

```bash
node scripts/storyblock-author.mjs styles --base-url https://127.0.0.1:8443 --json
node scripts/storyblock-author.mjs compare-styles --novel-id nov_Ab123 --file styles.json --json
```

EN: Compare surface, grammar, rhythm, narrative and lexical distances separately. Lower means closer, not better writing or a quality percentage. TXT has no curated narrative metadata; do not claim those channels prove literary alignment. These tokenizer features are not provider billing tokens. Read `/style-math.html` for actual formulas. The server scans its YAML and source TXT files every 60 seconds; unavailable references return an actionable error.

繁體中文：分別比較表層、語法、節奏、敘事及詞彙距離；越低越接近，不是品質百分比。TXT 沒有人工敘事標註，不可宣稱分數證明文學風格完全對齊；分詞也不是供應商計費 token。公式見 `/style-math.html`。伺服器每 60 秒檢查 YAML 與 TXT，來源不可用時明確報錯。

简体中文：分别比较表层、语法、节奏、叙事及词汇距离；越低越接近，不是质量百分比。TXT 没有人工叙事标注，不可声称分数证明文学风格完全对齐；分词也不是供应商计费 token。公式见 `/style-math.html`。服务器每 60 秒检查 YAML 与 TXT，来源不可用时明确报错。
