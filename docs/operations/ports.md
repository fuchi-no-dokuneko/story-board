# Ports / 連接埠 / 端口

EN: Edit `server/config/port-config.yaml`. The only policies are `local` (127.0.0.1), `standard` (active IPv4 addresses on wg* and tails* interfaces), and `public` (0.0.0.0). Standard fails clearly when no matching interface exists. Every listener uses the API's self-signed HTTPS.

繁體中文：編輯 `server/config/port-config.yaml`。策略僅有 `local`（127.0.0.1）、`standard`（wg* 與 tails* 介面的啟用 IPv4 位址）及 `public`（0.0.0.0）。沒有符合介面時，standard 會明確失敗。所有監聽均使用 API 自簽 HTTPS。

简体中文：编辑 `server/config/port-config.yaml`。策略仅有 `local`（127.0.0.1）、`standard`（wg* 与 tails* 接口的启用 IPv4 地址）及 `public`（0.0.0.0）。没有匹配接口时，standard 会明确失败。所有监听均使用 API 自签 HTTPS。

```yaml
port: 8443
purpose: StoryBlock HTTPS API and library
policy: local
enable_whitelist: false
whitelist: ""
```

EN: Deployment defaults to loopback. During development use the command below; the whitelist remains disabled. An explicitly enabled whitelist accepts comma-separated literal IPv4 addresses and checks the socket peer, not forwarded headers. No public/private address category is blocked by default.

繁體中文：部署預設 loopback。開發時使用下列命令，白名單維持停用。明確啟用的白名單接受逗號分隔 IPv4 位址，檢查 socket 對端，不信任轉送標頭。預設不按公用或私有位址類型封鎖。

简体中文：部署默认 loopback。开发时使用下列命令，白名单保持停用。明确启用的白名单接受逗号分隔 IPv4 地址，检查 socket 对端，不信任转发标头。默认不按公网或私有地址类型封锁。

```bash
STORYBLOCK_PORT_POLICY=public ./scripts/local-server.sh run
ssh -4 -N -L 8443:127.0.0.1:8443 operator@private-host
```
