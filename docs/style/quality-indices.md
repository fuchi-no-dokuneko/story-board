# Quality indices / 品質指標 / 品质指标

English: In the reader, select **Analyze quality**. Proper names are optional and local to that analysis. Save the JSON report or inspect sentence-aligned windows and source evidence. Existing style comparisons and rewrite decisions keep their prior behavior.

繁體：在閱讀頁選「分析品質」。可填專名，只影響本次分析；可儲存 JSON、選擇句界對齊視窗並查看原文證據。既有文風比較與改寫判定維持原行為。

简体：在阅读页选「分析品质」。可填专名，仅影响本次分析；可保存 JSON、选择句界对齐窗口并查看原文证据。已有文风比较与改写判断维持原行为。

```sh
./client-style-worker/install-local.sh
./client-style-worker/quality.sh input.txt fan-out/quality.json corpus.json
```

English: Omit `corpus.json` for uncalibrated analysis. The API reads `server/config/content-config/quality.json` at startup; override with `storyblock.quality.corpus`. Format: `{"sources":[{"id":"author-a","kind":"HUMAN","texts":["..."]}]}`. Use separate IDs for independent authors/models; `kind` is HUMAN or AI. A source may contain multiple texts. Optional `contract` contains all fields emitted in a report. Restart after changing the file. No external downloads are needed.

繁體：省略語料檔即可執行未校準分析。API 啟動時讀取上述設定；不同作者／模型使用不同 ID，同一來源可含多篇文本。可選的 `contract` 使用報告中完整欄位。修改後重新啟動，不需下載外部資產。

简体：省略语料文件即可执行未校准分析。API 启动时读取上述设置；不同作者／模型使用不同 ID，同一来源可含多篇文本。可选的 `contract` 使用报告中完整字段。修改后重新启动，无需下载外部资产。

English: POST `/v1/novels/{novelId}/quality-reports` with `If-Match`, `Idempotency-Key` and `{ "revision_id": "rev_XXXXX", "proper_names": [] }`. Response includes revision identity, block spans and `quality_report`. Offsets use UTF-16 into block texts joined with one newline. Missing calibration yields null percentiles; rhythm and formulaic bias also report UNCALIBRATED when needed. Short samples report INSUFFICIENT_DATA. These are text-pattern measurements, not authorship probabilities.

繁體：API 回傳修訂識別、區塊位置及獨立報告。位置以區塊文字加單一換行後的 UTF-16 計算；缺校準時分位數為空，相關指標標示未校準，短樣本標示不足。指標量度文本模式，不代表作者身分機率。

简体：API 返回修订标识、区块位置及独立报告。位置按区块文本加单个换行后的 UTF-16 计算；缺少校准时分位数为空，相关指标标示未校准，短样本标示不足。指标衡量文本模式，不代表作者身份概率。

[Formula contract / 計算契約 / 计算契约](quality-formulas.md)
