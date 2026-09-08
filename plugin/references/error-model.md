# Errors / 錯誤 / 错误

EN: Use `--json` for machine-readable diagnostics. HTTP problems retain status, type, title, code, detail, request ID, and available validation issues. Retry mutations only with the exact original payload and idempotency key. A stale head requires a fresh read and preview.

繁體中文：使用 `--json` 取得機器可讀診斷。HTTP 問題保留狀態、類型、標題、代碼、詳情、請求 ID 及可用驗證問題。變更重試只能使用原始內容與冪等鍵；版本頭過期時重新讀取及預覽。

简体中文：使用 `--json` 获取机器可读诊断。HTTP 问题保留状态、类型、标题、代码、详情、请求 ID 及可用验证问题。变更重试只能使用原始内容与幂等键；版本头过期时重新读取及预览。

| Exit / 結束碼 / 退出码 | Meaning / 意義 / 含义 |
| --- | --- |
| 0 | Success / 成功 / 成功 |
| 1 | Unexpected local failure / 非預期本機錯誤 / 非预期本机错误 |
| 2 | Command usage / 命令用法 / 命令用法 |
| 3 | Validation or verification / 驗證失敗 / 验证失败 |
| 4 | HTTP 401 or 403 / 授權失敗 / 授权失败 |
| 5 | Other HTTP error / 其他 HTTP 錯誤 / 其他 HTTP 错误 |
| 6 | Network or TLS / 網路或 TLS / 网络或 TLS |
| 7 | Unknown endpoint or DTO / 未知端點或 DTO / 未知端点或 DTO |

EN: Common statuses: 409 changed retry content; 410 expired artifact; 412 stale head; 413 oversized body; 422 rejected edit; 428 missing mutation headers; 429 rate limit. Preserve `If-Match` and `Idempotency-Key`. Scoped cross-novel reads normally return 404.

繁體中文：常見狀態：409 重試內容變更、410 產物過期、412 版本頭過期、413 內容過大、422 編輯拒絕、428 缺少變更標頭、429 速率限制。保留 `If-Match` 與 `Idempotency-Key`；範圍模式跨作品讀取通常回傳 404。

简体中文：常见状态：409 重试内容变更、410 产物过期、412 版本头过期、413 内容过大、422 编辑拒绝、428 缺少变更标头、429 速率限制。保留 `If-Match` 与 `Idempotency-Key`；范围模式跨作品读取通常返回 404。
