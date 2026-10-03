# Reader v2 / 新版閱讀器 / 新版阅读器

EN: Open `https://<server-ip>:8443/?ui=v2`; v1 remains at `/`.
V2 imports the ZIP's concept A desktop styles and concept D mobile styles.
The original screens use real API data and actions. No backend logic changes.

繁體：開啟 `https://<server-ip>:8443/?ui=v2`；v1 保留於 `/`。
新版直接採用 ZIP A 桌面及 D 手機樣式，原稿畫面接上真實資料及操作；未更改後端邏輯。

简体：打开 `https://<server-ip>:8443/?ui=v2`；v1 保留在 `/`。
新版直接采用 ZIP A 桌面及 D 手机样式，原稿界面接入真实数据及操作；未更改后端逻辑。

```bash
./client-web-v2/install-local.sh
./scripts/local-server.sh stop
./install.sh
./scripts/local-server.sh start
```

EN: Asset assembly requires Python only. Installation packages both UIs into
the API. HTTPS uses its existing locally generated self-signed certificate.

繁體：資源組合只需 Python；安裝器將兩版封裝進 API，HTTPS 沿用本機產生的自簽憑證。

简体：资源组合只需 Python；安装器将两版打包进 API，HTTPS 沿用本机生成的自签证书。

EN: EN / 繁 / 简 changes interface labels. Story text stays unchanged.
Appearance, chapter, paragraph and scroll offset remain in this browser;
optional tokens remain in page memory. Analysis and PDF/TXT/JSON exports use
the selected revision. Direct book links load independently of the catalog.

繁體：三語切換只改介面文字。外觀、章節、段落及捲動位置保存在瀏覽器；選用權杖只留於頁面記憶體。
分析及 PDF／TXT／JSON 使用所選修訂；書籍直連不等待書庫查詢。

简体：三语切换只改界面文字。外观、章节、段落及滚动位置保存在浏览器；可选令牌只留在页面内存。
分析及 PDF／TXT／JSON 使用所选修订；书籍直连不等待书库查询。

EN: Edit `src/design/*.parts` for imported references, `src/desktop` and
`src/mobile` for screens, `src/shared` for API models, and `src/runtime` for events.
`build.py` scopes each layout's CSS and assembles the existing asset URLs.
Generated files beside `.parts` directories are overwritten during installation.

繁體：原稿在 `src/design/*.parts`，畫面在 `src/desktop`、`src/mobile`，資料在 `src/shared`，事件在 `src/runtime`。
建置程式隔離兩種版面的 CSS；`.parts` 旁的產生檔會在安裝時重建。

简体：原稿在 `src/design/*.parts`，界面在 `src/desktop`、`src/mobile`，数据在 `src/shared`，事件在 `src/runtime`。
构建程序隔离两种界面的 CSS；`.parts` 旁的生成文件会在安装时重建。

[Verification / 驗證 / 验证](doc/verification.md) ·
[Deployment / 部署 / 部署](doc/deploy.md)
