# Browser acceptance / 瀏覽器驗收 / 浏览器验收

[Run workflows / 執行流程 / 执行流程](workflows.md)

EN: Browser UAT follows installation and backend verification. Use an isolated database and a registered fixture. The dependency-free browser runner communicates with real Chromium over its private debugging pipe; it does not open an HTTP control service. Screenshots and results stay in the repository.

繁體中文：安裝與後端驗證後執行瀏覽器驗收，使用隔離資料庫與已登錄範例。無套件依賴的瀏覽器測試器透過私有除錯管道操作真正 Chromium，不開啟 HTTP 控制服務。截圖與結果留在儲存庫。

简体中文：安装与后端验证后执行浏览器验收，使用隔离数据库与已登记示例。无软件包依赖的浏览器测试器通过私有调试管道操作真正 Chromium，不开启 HTTP 控制服务。截图与结果留在仓库。

EN: The existing Cucumber daily checklist and English/Cantonese demo scripts remain available. Install their fixed dependencies with `./acceptance/install-local.sh`; app-only packages and cache stay in `.local-tool-app`. A dry run checks bindings and is not a successful GUI milestone. Full results belong in the shared monthly milestone log only after a real browser run.

繁體中文：既有 Cucumber 每日清單及英文／粵語示範腳本仍可使用。以 `./acceptance/install-local.sh` 安裝固定依賴，專用套件與快取留在 `.local-tool-app`。乾跑僅檢查步驟綁定，不算 GUI 成功里程碑。完成真正瀏覽器執行後，才將完整結果記入共用月份里程碑。

简体中文：既有 Cucumber 每日清单及英文／粤语示范脚本仍可使用。以 `./acceptance/install-local.sh` 安装固定依赖，专用软件包与缓存留在 `.local-tool-app`。干跑仅检查步骤绑定，不算 GUI 成功里程碑。完成真正浏览器执行后，才将完整结果记入共享月份里程碑。
