# Read and analysis cache / 讀取與分析快取 / 读取与分析缓存

EN: The API shares `NovelReadCache` across catalog, detail, quality and style
requests. Every read checks the persisted head before reusing its revision.
Catalog metadata and canonical detail share one consistent snapshot. New books
remain visible because catalog requests still list stored novel IDs.

繁體：書庫、全文、品質及文風請求共用 `NovelReadCache`；每次讀取先檢查資料庫的目前修訂。
書庫描述及標準全文共用一致快照；書庫仍查詢已儲存小說 ID，因此新增書籍立即可見。

简体：书库、全文、品质及文风请求共用 `NovelReadCache`；每次读取先检查数据库的当前修订。
书库描述及标准全文共用一致快照；书库仍查询已存储小说 ID，因此新增书籍立即可见。

EN: Quality keys include the revision and effective quality contract, including
proper names. Quality corpus/configuration still loads at process startup.
Style keys include the revision and selected reference benchmarks in request
order. The existing reference watcher refreshes changed benchmarks. Hash and
reference availability checks run before cache lookup, including on cache hits.

繁體：品質鍵包含修訂及實際契約（包括專名）；品質語料／設定仍於啟動時載入。
文風鍵包含修訂及依請求排序的參考基準；既有監看器更新變動基準。
命中快取前仍檢查修訂雜湊及參考資料可用性。

简体：品质键包含修订及实际契约（包括专名）；品质语料／设置仍在启动时加载。
文风键包含修订及按请求排序的参考基准；现有监视器更新变动基准。
命中缓存前仍检查修订哈希及参考数据可用性。

EN: Each cache retains 128 entries using LRU eviction; restart clears them.
There is no persistent cache, schema migration or API response change. The UI's
reopen cache separately reuses detail only when the catalog head matches.

繁體：各快取以 LRU 淘汰並保留 128 筆，重啟清除。沒有永久快取、資料庫遷移或 API 回應變更。
介面另以書庫修訂相符為條件，重用已下載全文。

简体：各缓存按 LRU 淘汰并保留 128 条，重启清除。没有永久缓存、数据库迁移或 API 响应变更。
界面另以书库修订相符为条件，重用已下载全文。
