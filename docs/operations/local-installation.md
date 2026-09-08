# Local installation / 本機安裝 / 本机安装

EN: Review `install-build-tool-ubuntu-headless.sh` before an administrator runs it. The coding agent does not run it. Python and nvm follow the existing Useful_shell_script setup. `install-local-build.sh` installs fixed Maven tooling in `.local-tool`; it creates missing local directories without replacing data.

繁體中文：管理員執行前先審查 `install-build-tool-ubuntu-headless.sh`；程式代理不執行。Python 與 nvm 沿用既有 Useful_shell_script 安裝。`install-local-build.sh` 將固定 Maven 工具放在 `.local-tool`，只建立缺少的目錄，不取代資料。

简体中文：管理员执行前先审查 `install-build-tool-ubuntu-headless.sh`；程序代理不执行。Python 与 nvm 沿用既有 Useful_shell_script 安装。`install-local-build.sh` 将固定 Maven 工具放在 `.local-tool`，只创建缺少的目录，不替换数据。

```bash
./install-local-build.sh
./install.sh
./scripts/local-server.sh start
./scripts/local-server.sh status
./scripts/local-server.sh stop
```

EN: Installation assembles source sections, builds the apps, atomically installs the API jar, and generates its local self-signed leaf. State stays under `.local/storyblock`: `data`, `secrets`, `tls`, `logs`, and `run`. Existing database and secret files are retained. Stop and start after installing an update. Use `logs` to follow the append-only runtime log.

繁體中文：安裝組合來源區段、建置應用、原子替換 API jar，並產生本機自簽憑證。狀態留在 `.local/storyblock` 的 `data`、`secrets`、`tls`、`logs` 與 `run`；保留既有資料庫及秘密。更新安裝後停止並重新啟動，使用 `logs` 追蹤追加式日誌。

简体中文：安装组合源码区段、构建应用、原子替换 API jar，并生成本机自签证书。状态留在 `.local/storyblock` 的 `data`、`secrets`、`tls`、`logs` 与 `run`；保留既有数据库及秘密。更新安装后停止并重新启动，使用 `logs` 跟踪追加式日志。
