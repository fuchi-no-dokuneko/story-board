# Connection / 連線 / 连接

EN: The default installation trusts every device that can reach its listening port. No client secret or manual client approval is required. Transport always uses IPv4 HTTPS and accepts the server's locally generated self-signed certificate. Never supply or upload TLS key material. Keep deployment on loopback and use a private tunnel remotely.

繁體中文：預設安裝信任所有能連到監聽連接埠的裝置，不需客戶端秘密或人工核准。傳輸一律使用 IPv4 HTTPS，接受伺服器本機自簽憑證。不提供或上傳 TLS 私鑰材料。部署保持 loopback，遠端使用私有通道。

简体中文：默认安装信任所有能连到监听端口的设备，不需客户端秘密或人工批准。传输一律使用 IPv4 HTTPS，接受服务器本机自签证书。不提供或上传 TLS 私钥材料。部署保持 loopback，远程使用私有通道。

```bash
ssh -4 -N -L 8443:127.0.0.1:8443 operator@private-host
STORYBLOCK_BASE_URL=https://127.0.0.1:8443 node scripts/storyblock-author.mjs health
```

EN: Existing bearer-key endpoints remain compatible with explicitly enabled scoped mode. An optional `STORYBLOCK_ACCESS_KEY` is kept out of logs and output. Scoped keys bind one novel, actor, expiry, and permissions. Rewrite-proposal reads now enforce the stored novel boundary. `STORYBLOCK_TIMEOUT_MS` and `STORYBLOCK_USER_AGENT` override client startup defaults.

繁體中文：既有 bearer 密鑰端點與明確啟用的範圍模式保持相容。選填 `STORYBLOCK_ACCESS_KEY` 不進入日誌或輸出。範圍密鑰綁定單一作品、使用者、期限及權限；改寫提案讀取也檢查已儲存作品邊界。`STORYBLOCK_TIMEOUT_MS` 與 `STORYBLOCK_USER_AGENT` 可覆寫客戶端啟動預設值。

简体中文：既有 bearer 密钥端点与明确启用的范围模式保持兼容。选填 `STORYBLOCK_ACCESS_KEY` 不进入日志或输出。范围密钥绑定单一作品、用户、期限及权限；改写提案读取也检查已保存作品边界。`STORYBLOCK_TIMEOUT_MS` 与 `STORYBLOCK_USER_AGENT` 可覆盖客户端启动默认值。
