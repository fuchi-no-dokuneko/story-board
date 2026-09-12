# Ports / 連接埠 / 端口

EN: Edit `server/config/port-config.yaml`. The only policies are `local` (127.0.0.1), `standard` (active IPv4 addresses on wg* and tails* interfaces), and `public` (0.0.0.0). Standard fails clearly when no matching interface exists. Every listener uses the API's self-signed HTTPS.

繁體中文：編輯 `server/config/port-config.yaml`。策略僅有 `local`（127.0.0.1）、`standard`（wg* 與 tails* 介面的啟用 IPv4 位址）及 `public`（0.0.0.0）。沒有符合介面時，standard 會明確失敗。所有監聽均使用 API 自簽 HTTPS。

简体中文：编辑 `server/config/port-config.yaml`。策略仅有 `local`（127.0.0.1）、`standard`（wg* 与 tails* 接口的启用 IPv4 地址）及 `public`（0.0.0.0）。没有匹配接口时，standard 会明确失败。所有监听均使用 API 自签 HTTPS。

```yaml
port: 8443
purpose: StoryBlock HTTPS API and library
policy: public
enable_whitelist: false
whitelist: ""
```

EN: The standalone launcher defaults to public (`0.0.0.0:8443`). Both `start` and foreground `run` accept `--policy public` / `--policy=public` and `--port 9443` / `--port=9443`. Options override legacy `STORYBLOCK_PORT_POLICY` / `STORYBLOCK_LOCAL_PORT`, then YAML. Stop and start to apply changes to a running server. Unknown options and invalid values fail before launch; `--help` lists usage.

繁體中文：獨立啟動器預設 public（`0.0.0.0:8443`）。`start` 與前景 `run` 均接受 `--policy public` / `--policy=public` 及 `--port 9443` / `--port=9443`。優先順序為參數、舊環境變數、YAML。變更後須停止再啟動；未知選項或無效值會在啟動前失敗，`--help` 顯示用法。

简体中文：独立启动器默认 public（`0.0.0.0:8443`）。`start` 与前台 `run` 均接受 `--policy public` / `--policy=public` 及 `--port 9443` / `--port=9443`。优先顺序为参数、旧环境变量、YAML。更改后须停止再启动；未知选项或无效值会在启动前失败，`--help` 显示用法。

```bash
./scripts/local-server.sh start
./scripts/local-server.sh stop
./scripts/local-server.sh start --policy local --port 8443
# On your computer / 在你的電腦 / 在你的电脑
ssh -4 -N -L 8443:127.0.0.1:8443 operator@private-host
```

EN: For public mode browse to `https://<server-ip>:8443/`; for local mode use the private SSH tunnel above and browse to `https://127.0.0.1:8443/`. The optional whitelist checks literal IPv4 socket peers, not forwarded headers.

繁體中文：public 模式使用 `https://<server-ip>:8443/`；local 模式使用上述私有 SSH 通道與 `https://127.0.0.1:8443/`。選用白名單檢查 IPv4 socket 對端，不信任轉送標頭。

简体中文：public 模式使用 `https://<server-ip>:8443/`；local 模式使用上述私有 SSH 通道与 `https://127.0.0.1:8443/`。可选白名单检查 IPv4 socket 对端，不信任转发标头。
