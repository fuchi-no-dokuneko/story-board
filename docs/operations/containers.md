# Containers / 容器 / 容器

EN: Containers are optional. Use an existing rootless Docker installation; the repository installer does not install a daemon or change the host. The wrapper creates repository-local writable mounts and passes the current UID/GID.

繁體中文：容器為選用功能。使用既有 rootless Docker；儲存庫安裝器不安裝常駐服務或變更主機。包裝腳本建立儲存庫內可寫掛載目錄，並傳入目前 UID/GID。

简体中文：容器为可选功能。使用既有 rootless Docker；仓库安装器不安装常驻服务或更改主机。包装脚本创建仓库内可写挂载目录，并传入当前 UID/GID。

```bash
./scripts/containers.sh build
./scripts/containers.sh up -d api
./scripts/containers.sh down
```

EN: API data and TLS stay in `server/data`. Worker mounts are separate and contain neither the API database nor its private key. The API listens on IPv4 inside the container; the published host port defaults to 0.0.0.0. HTTPS still terminates in the API. Worker profiles need their novel or model endpoint settings, with no mandatory client credential.

繁體中文：API 資料與 TLS 留在 `server/data`。工作程式掛載分開，不包含 API 資料庫或私鑰。API 在容器內監聽 IPv4，主機公布連接埠預設綁定 0.0.0.0。HTTPS 仍由 API 內部終止。工作程式設定需指定作品或模型端點，不強制客戶端密鑰。

简体中文：API 数据与 TLS 留在 `server/data`。工作程序挂载分开，不包含 API 数据库或私钥。API 在容器内监听 IPv4，主机公布端口默认绑定 0.0.0.0。HTTPS 仍由 API 内部终止。工作程序设置需指定作品或模型端点，不强制客户端密钥。
