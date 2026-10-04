# Memory cache / 記憶體快取 / 内存缓存

EN: `MemoCache` stores completed values in access order. Each service retains
up to 128 entries; concurrent requests for one key share one load. Unrelated
keys load independently. Failed loads are removed and can retry. In-flight
entries can temporarily exceed the limit. Nothing is written to disk; restart
clears all entries. There is no TTL or external cache service.

繁體：`MemoCache` 按存取順序保留結果，各服務最多保留 128 筆。同一鍵的並行請求共用一次載入，
不同鍵可獨立載入。失敗結果移除後可重試；尚未完成的載入可能暫時超過上限。
快取不寫入磁碟，重啟全部清除；沒有 TTL 或外部快取服務。

简体：`MemoCache` 按访问顺序保留结果，各服务最多保留 128 条。同一键的并发请求共用一次加载，
不同键可独立加载。失败结果移除后可重试；尚未完成的加载可能暂时超过上限。
缓存不写入磁盘，重启全部清除；没有 TTL 或外部缓存服务。

EN: `snapshot()` exposes local hit/load/entry counts for tests. The API adds no
cache endpoint. See [server integration](../../server/doc/cache.md).

繁體：`snapshot()` 提供本機命中／載入／筆數供測試，API 未新增快取端點。整合方式見上方連結。

简体：`snapshot()` 提供本机命中／加载／条数供测试，API 未新增缓存端点。集成方式见上方链接。
