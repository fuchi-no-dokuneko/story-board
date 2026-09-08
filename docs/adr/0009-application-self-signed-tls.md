# Application HTTPS

EN: The API terminates HTTPS and generates a local self-signed CA=false server leaf. No certificate upload, external issuer, ACME, or reverse proxy is used. Deployment defaults to IPv4 loopback with a private tunnel for remote use. All keys and runtime data stay in this repository.

繁體中文：API 內部終止 HTTPS，並產生本機 CA=false 自簽伺服器憑證。不使用憑證上傳、外部簽發者、ACME 或反向代理。部署預設 IPv4 loopback，遠端使用私有通道。密鑰與執行資料均留在儲存庫。

简体中文：API 内部终止 HTTPS，并生成本机 CA=false 自签服务器证书。不使用证书上传、外部签发者、ACME 或反向代理。部署默认 IPv4 loopback，远程使用私有通道。密钥与运行数据均留在仓库。

[Detailed notes / 詳細筆記 / 详细笔记](0009-application-self-signed-tls.notes.txt)
