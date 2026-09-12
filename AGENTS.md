# Repository operating constraints

## TLS and installation policy

- Keep inbound HTTPS termination inside the StoryBlock API process.
- Generate a self-signed server leaf locally with `CA=false`; never add an ACME
  flow, public certificate issuer, reverse proxy, or certificate-upload step.
- Never require an operator to supply or upload TLS certificate/key material.
- Default the standalone launcher to public (`0.0.0.0`); retain an explicit
  loopback option and document a private tunnel for that mode.
- Keep `install.sh` unprivileged and repository-local. It must not call `sudo`,
  require root, write to system or home directories, or place caches, secrets,
  certificates, databases, logs, or installed binaries outside this repository.
- Preserve architecture tests that enforce these constraints.

## 繁體中文

HTTPS 必須在 StoryBlock API 程序內終止。本機產生 CA=false 自簽伺服器憑證；不得加入 ACME、公用簽發者、反向代理或憑證上傳步驟，不要求操作人員提供憑證或私鑰。獨立啟動器預設監聽 public（0.0.0.0）；保留 loopback 選項及其遠端私有通道文件。install.sh 必須無特權且限於儲存庫，不得使用 sudo、要求 root，或將快取、秘密、憑證、資料庫、日誌及安裝程式寫入系統、家目錄或儲存庫以外。保留強制執行上述限制的架構測試。

## 简体中文

HTTPS 必须在 StoryBlock API 进程内终止。本机生成 CA=false 自签服务器证书；不得加入 ACME、公共签发者、反向代理或证书上传步骤，不要求操作人员提供证书或私钥。独立启动器默认监听 public（0.0.0.0）；保留 loopback 选项及其远程私有通道文档。install.sh 必须无特权且限于仓库，不得使用 sudo、要求 root，或将缓存、秘密、证书、数据库、日志及安装程序写入系统、主目录或仓库以外。保留强制执行上述限制的架构测试。
