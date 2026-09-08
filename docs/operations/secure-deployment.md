# Local HTTPS deployment

EN: Run ./install.sh, then ./scripts/local-server.sh start. The installer is unprivileged and repository-local. HTTPS terminates inside the API with an automatically generated CA=false leaf. The host default is 127.0.0.1:8443; remote use goes through a private tunnel. See the current installation and port guides.

繁體中文：執行 ./install.sh，再執行 ./scripts/local-server.sh start。安裝不需特權，所有寫入留在儲存庫。HTTPS 由 API 內部終止，憑證自動產生且 CA=false。主機預設為 127.0.0.1:8443，遠端使用私有通道。請參閱目前安裝與連接埠指南。

简体中文：执行 ./install.sh，再执行 ./scripts/local-server.sh start。安装不需特权，所有写入留在仓库。HTTPS 由 API 内部终止，证书自动生成且 CA=false。主机默认为 127.0.0.1:8443，远程使用私有通道。请参阅当前安装与端口指南。

[Detailed notes / 詳細筆記 / 详细笔记](secure-deployment.notes.txt)
