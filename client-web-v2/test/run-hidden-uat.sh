#!/usr/bin/env bash
set -euo pipefail
umask 077
repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)
cd "$repo_dir"
source scripts/build-environment.sh
export UAT_OUTPUT
UAT_OUTPUT=$(mktemp -d "$repo_dir/.local/hidden-uat-XXXXXX")
export UAT_BASE_URL=https://127.0.0.1:${UAT_PORT:-9445}
uat_root=$UAT_OUTPUT/runtime
mkdir -p "$uat_root/server/config/content-config" "$UAT_OUTPUT/downloads"
cp server/config/content-config/styles.yaml "$uat_root/server/config/content-config/"
cp -R server/style-references "$uat_root/server/"
java -Djava.net.preferIPv4Stack=true "-Dstoryblock.root=$uat_root" \
  -jar server/target/storyblock-api-0.1.0-SNAPSHOT.jar --policy=public "--port=${UAT_PORT:-9445}" \
  >"$UAT_OUTPUT/server.log" 2>&1 &
uat_pid=$!
cleanup() { kill "$uat_pid" 2>/dev/null || true; wait "$uat_pid" 2>/dev/null || true; }
trap cleanup EXIT
deadline=$((SECONDS + 45))
until curl -4 -kfsS --noproxy '*' --connect-timeout 1 --max-time 2 \
  "$UAT_BASE_URL/actuator/health" >/dev/null 2>&1; do
  kill -0 "$uat_pid" 2>/dev/null || { cat "$UAT_OUTPUT/server.log"; exit 1; }
  ((SECONDS < deadline)) || { echo "Startup timed out: $UAT_OUTPUT/server.log"; exit 1; }
  sleep .25
done
node client-web-v2/test/seed-hidden.mjs
node client-web-v2/test/run-hidden-browser.cjs
echo "Evidence / 驗證紀錄: $UAT_OUTPUT"
