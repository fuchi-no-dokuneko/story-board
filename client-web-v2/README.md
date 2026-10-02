# Reader v2 / 新版閱讀器 / 新版阅读器

EN: Open `https://<server-ip>:8443/?ui=v2`. The original frontend remains at `/`.
V2 follows concept A's bookshelf and reader, with concept D's mobile navigation.
The library is read only; analysis and downloads use the existing API.

繁體：開啟 `https://<server-ip>:8443/?ui=v2`；原版保留於 `/`。
新版採用 A 的書架與閱讀版面，以及 D 的手機導覽。書庫維持唯讀，分析與下載沿用既有 API。

简体：打开 `https://<server-ip>:8443/?ui=v2`；原版保留在 `/`。
新版采用 A 的书架与阅读界面，以及 D 的手机导航。书库保持只读，分析与下载沿用现有 API。

```bash
./client-web-v2/install-local.sh
./scripts/local-server.sh stop
./install.sh
./scripts/local-server.sh start
```

EN: `install-local.sh` assembles browser assets without installing packages.
The repository installer packages them into the API; restart after rebuilding.
HTTPS and its local self-signed certificate are managed by the existing API.
No backend, API contract, or security-route change is needed.

繁體：`install-local.sh` 組合瀏覽器資源，不安裝套件。儲存庫安裝器將資源封裝至 API，重建後須重新啟動。
HTTPS 與本機自簽憑證由既有 API 管理，不需修改後端、API 契約或路由。

简体：`install-local.sh` 组合浏览器资源，不安装软件包。仓库安装器将资源打包到 API，重建后须重新启动。
HTTPS 与本机自签证书由现有 API 管理，无需修改后端、API 契约或路由。

EN: Choose EN / 繁 / 简; story text is unchanged. Reading appearance and the last
chapter stay in this browser. Optional tokens stay only in page memory.
Insights offers five separate style channels, quality windows, source highlighting,
and JSON download. PDF/TXT exports include the complete selected revision.

繁體：可切換 EN／繁／简，原文不轉換。閱讀外觀與最後章節保存在瀏覽器；選用權杖只保留於本頁記憶體。
分析包含五項獨立文風距離、品質視窗、原文標記及 JSON 下載。PDF/TXT 匯出所選修訂全文。

简体：可切换 EN／繁／简，原文不转换。阅读外观与最后章节保存在浏览器；可选令牌仅保留于本页内存。
分析包含五项独立文风距离、品质窗口、原文标记及 JSON 下载。PDF/TXT 导出所选修订全文。

EN: Source: `src/js` and `src/css`; `build.py` combines v2 and the original assets
under already permitted URLs. Edit source files, then run the installer above.

繁體：來源為 `src/js` 與 `src/css`；`build.py` 將新舊資源組合於既有網址。修改來源後執行上述安裝器。

简体：源码为 `src/js` 与 `src/css`；`build.py` 将新旧资源组合到已有网址。修改源码后运行上述安装器。

[Verification / 驗證 / 验证](doc/verification.md)
