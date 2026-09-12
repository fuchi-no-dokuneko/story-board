# StoryBlock Author

EN: This standalone Node CLI bundles all 39 programmatic routes and 140 DTO schemas, with no runtime package dependencies. Run its local installer before first use. Online commands use IPv4 HTTPS and accept the local self-signed certificate; offline discovery and validation need no server.

繁體中文：此獨立 Node CLI 包含全部三十九個程式化路由與一百四十個 DTO 綱要，沒有執行期套件依賴。首次使用前執行本機安裝器。線上命令使用 IPv4 HTTPS 並接受本機自簽憑證；離線查詢與驗證不需伺服器。

简体中文：此独立 Node CLI 包含全部三十九个程序化路由与一百四十个 DTO 纲要，没有运行期软件包依赖。首次使用前执行本机安装器。在线命令使用 IPv4 HTTPS 并接受本机自签证书；离线查询与验证不需服务器。

```bash
./install-local.sh
node scripts/storyblock-author.mjs endpoints
node scripts/storyblock-author.mjs register --source manuscript.json --json
node scripts/storyblock-author.mjs verify --source manuscript.json --json
npm test
```

EN: The default origin is `https://127.0.0.1:8443`. Override it with `STORYBLOCK_BASE_URL` or `--base-url`. Credentials are optional for the default trusted server. The whole folder can be copied independently; run its installer after copying. Outputs refuse overwriting unless `--force` is explicit.

繁體中文：預設來源為 `https://127.0.0.1:8443`，可用 `STORYBLOCK_BASE_URL` 或 `--base-url` 覆寫。預設信任模式不需密鑰。整個目錄可獨立複製，複製後執行安裝器。除非明確指定 `--force`，輸出不覆寫既有檔案。

简体中文：默认来源为 `https://127.0.0.1:8443`，可用 `STORYBLOCK_BASE_URL` 或 `--base-url` 覆盖。默认信任模式不需密钥。整个目录可独立复制，复制后执行安装器。除非明确指定 `--force`，输出不覆盖既有文件。

[Skill / 技能 / 技能](SKILL.md) · [Workflows / 流程 / 流程](references/workflows.md)
