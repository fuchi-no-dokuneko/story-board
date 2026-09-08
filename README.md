# StoryBlock

EN: A local narrative engine with immutable revisions, typed edits, image blocks, deterministic rendering, and a browser library. All inbound web traffic uses self-signed HTTPS inside the API. Reachable clients are trusted by default.

繁體中文：本機敘事引擎，提供不可變修訂、型別化編輯、圖片區塊、確定性渲染及瀏覽器書庫。所有入站網頁流量由 API 內部使用自簽 HTTPS；預設信任可連線客戶端。

简体中文：本机叙事引擎，提供不可变修订、类型化编辑、图片区块、确定性渲染及浏览器书库。所有入站网页流量由 API 内部使用自签 HTTPS；默认信任可连接客户端。

```bash
./install-local-build.sh
./install.sh
./scripts/local-server.sh start
```

EN: Open `https://127.0.0.1:8443/` and accept the local certificate warning. Installation needs no root access or supplied keys. Java 21, Python, Node through nvm, and ordinary build utilities must already be available. The administrative prerequisite script is provided for an operator to review and run separately.

繁體中文：開啟 `https://127.0.0.1:8443/` 並接受本機憑證提示。安裝不需 root 或提供密鑰。電腦須已有 Java 21、Python、nvm 管理的 Node 及一般建置工具。管理員前置安裝腳本供操作人員另行審查與執行。

简体中文：打开 `https://127.0.0.1:8443/` 并接受本机证书提示。安装不需 root 或提供密钥。电脑须已有 Java 21、Python、nvm 管理的 Node 及普通构建工具。管理员前置安装脚本供操作人员另行审查与执行。

- [Install / 安裝 / 安装](docs/operations/local-installation.md)
- [Ports and tunnel / 連接埠與通道 / 端口与通道](docs/operations/ports.md)
- [Applications and sources / 應用與來源 / 应用与源码](docs/operations/source-layout.md)
- [Containers / 容器 / 容器](docs/operations/containers.md)
- [Author tool / 作者工具 / 作者工具](plugin/README.md)
- [Tests / 測試 / 测试](docs/operations/integration-security-tests.md)
- [Coverage / 覆蓋範圍 / 覆盖范围](text-coverage.txt)
- [Verification / 驗證紀錄 / 验证记录](docs/operations/verification.md)
