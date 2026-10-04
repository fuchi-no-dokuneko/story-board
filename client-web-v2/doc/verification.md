# Verification / 驗證 / 验证

EN: The original UI at `/` and v2 at `/?ui=v2` share the existing API.
Java read/analysis caches preserve response contracts, database schema and
listener behavior. V2 keeps the existing asset URLs and CSP.

繁體：兩版共用既有 API；Java 讀取／分析快取保留回應契約、資料庫結構及監聽行為。
新版沿用資源網址及 CSP。

简体：两版共用现有 API；Java 读取／分析缓存保留响应契约、数据库结构及监听行为。
新版沿用资源网址及 CSP。

```bash
./client-web-v2/install-local.sh
node --test server/src/test/js/*.test.cjs client-web-v2/test/*.test.cjs
./mvnw --batch-mode verify
./install.sh
./client-web-v2/test/run-uat.sh
```

EN: UAT starts the installed API with a fresh SQLite database and public HTTPS
on port 9443. Set `UAT_PORT` to change it. It creates 16 stories, including a
30-chapter, 6,000-block novel and a real 714-window analysis fixture,
and PNG/JPEG illustrations through existing endpoints. Evidence stays in the printed
`.local/frontend-v2-uat-*` directory. The script stops its API and Firefox.

繁體：UAT 使用全新 SQLite 資料庫，以 public HTTPS 9443 啟動已安裝的 API；可用 `UAT_PORT` 改埠。
建立 16 部小說（含 6,000 段長篇及真實 714 視窗範例）與 PNG／JPEG 圖片。
紀錄留在輸出的 `.local/frontend-v2-uat-*`；結束後關閉 API 與 Firefox。

简体：UAT 使用全新 SQLite 数据库，以 public HTTPS 9443 启动已安装的 API；可用 `UAT_PORT` 改端口。
创建 16 部小说（含 6,000 段长篇及真实 714 窗口范例）与 PNG／JPEG 图片。
记录保留在输出的 `.local/frontend-v2-uat-*`；结束后关闭 API 与 Firefox。

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
