# Verification / 驗證 / 验证

EN: The original UI at `/` and v2 at `/?ui=v2` share the existing API. No Java,
API schema, database migration, or listener logic changed. V2 packages through
the existing asset URLs and preserves the original CSP.

繁體：原版 `/` 與新版 `/?ui=v2` 共用既有 API。未變更 Java、API 結構、資料庫遷移或監聽邏輯。
新版使用既有資源網址封裝，保留原有 CSP。

简体：原版 `/` 与新版 `/?ui=v2` 共用现有 API。未更改 Java、API 结构、数据库迁移或监听逻辑。
新版使用现有资源网址打包，保留原有 CSP。

```bash
./client-web-v2/install-local.sh
node --test server/src/test/js/*.test.cjs client-web-v2/test/*.test.cjs
./mvnw --batch-mode test
./install.sh
./client-web-v2/test/run-uat.sh
```

EN: UAT starts the installed API with a fresh SQLite database and public HTTPS
on port 9443. Set `UAT_PORT` to change that port. It creates real stories and
PNG/JPEG illustrations through existing endpoints. Evidence stays in the printed
`.local/frontend-v2-uat-*` directory. The script stops its API and Firefox.

繁體：UAT 使用全新 SQLite 資料庫，以 public HTTPS 9443 啟動已安裝的 API；可用 `UAT_PORT` 改埠。
測試透過既有端點建立小說及 PNG／JPEG 圖片。紀錄留在輸出的 `.local/frontend-v2-uat-*` 目錄；結束後關閉測試 API 與 Firefox。

简体：UAT 使用全新 SQLite 数据库，以 public HTTPS 9443 启动已安装的 API；可用 `UAT_PORT` 改端口。
测试通过现有端点创建小说及 PNG／JPEG 图片。记录保留在输出的 `.local/frontend-v2-uat-*` 目录；结束后关闭测试 API 与 Firefox。

EN: Firefox defaults to `~/UAT-firefox/firefox/firefox`; override with `FIREFOX_BINARY`.
Puppeteer uses this app's local tools or the existing `~/UAT-firefox/automation`
installation. `PUPPETEER_MODULE` can select another installed copy. If needed:

繁體：Firefox 預設路徑如上，可用 `FIREFOX_BINARY` 指定。
Puppeteer 使用本應用工具或既有 UAT 安裝，可用 `PUPPETEER_MODULE` 指定；需要時執行：

简体：Firefox 默认路径如上，可用 `FIREFOX_BINARY` 指定。
Puppeteer 使用本应用工具或现有 UAT 安装，可用 `PUPPETEER_MODULE` 指定；需要时运行：

```bash
./client-web-v2/.local-tool-app/install-local.sh
```

[Results / 結果 / 结果](results.md)
