# Local HTTPS deployment

EN: Run ./install.sh, then ./scripts/local-server.sh start. The installer is unprivileged and repository-local. HTTPS terminates inside the API with an automatically generated CA=false leaf. The standalone default is 0.0.0.0:8443. Use start --policy local for loopback with a private tunnel. See the installation and port guides.

繁體中文：執行 ./install.sh，再執行 ./scripts/local-server.sh start。安裝不需特權，所有寫入留在儲存庫。HTTPS 由 API 內部終止，憑證自動產生且 CA=false。獨立啟動預設為 0.0.0.0:8443；使用 start --policy local 限定 loopback 並搭配私有通道。請參閱安裝與連接埠指南。

简体中文：执行 ./install.sh，再执行 ./scripts/local-server.sh start。安装不需特权，所有写入留在仓库。HTTPS 由 API 内部终止，证书自动生成且 CA=false。独立启动默认为 0.0.0.0:8443；使用 start --policy local 限定 loopback 并配合私有通道。请参阅安装与端口指南。

[Detailed notes / 詳細筆記 / 详细笔记](secure-deployment.notes.txt)
