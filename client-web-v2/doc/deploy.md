# Existing server / 既有伺服器 / 现有服务器

EN: From the existing server checkout, update to the tested commit on `main`.
Use the same checkout to preserve its SQLite database, styles and local TLS material.
The target port for this deployment is 8444.

繁體：在正式站既有儲存庫更新至 `main` 的已驗證提交；使用同一目錄，以保留 SQLite、風格資料及本機 TLS。
本次部署使用 8444 埠。

简体：在正式站现有仓库更新至 `main` 的已验证提交；使用同一目录，保留 SQLite、风格数据及本机 TLS。
此次部署使用 8444 端口。

```bash
git pull --ff-only
./scripts/local-server.sh stop
./install.sh
./scripts/local-server.sh start --policy public --port 8444
./scripts/local-server.sh status
```

EN: If installation fails, inspect its error before restarting. The installer
uses repository-local tools and files, without root or an external TLS proxy.

繁體：安裝失敗時，先檢查錯誤再重新啟動。工具及檔案均在儲存庫內，不需 root 或外部 TLS 代理。

简体：安装失败时，先检查错误再重新启动。工具及文件均在仓库内，不需要 root 或外部 TLS 代理。

EN: Verify both `/` and `/?ui=v2`, a direct book link, mobile reading,
style/quality analysis and a download. A successful local build does not prove
that the remote server has been updated.

繁體：檢查兩版首頁、書籍直連、手機閱讀、文風／品質分析及下載。本機建置通過不代表正式站已更新。

简体：检查两版首页、书籍直连、手机阅读、文风／品质分析及下载。本机构建通过不代表正式站已更新。
