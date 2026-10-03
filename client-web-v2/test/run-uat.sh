#!/usr/bin/env bash
set -euo pipefail
umask 077
repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)
cd "$repo_dir"
source scripts/build-environment.sh
test -f server/data/server/application.jar || { echo 'Run ./install.sh first'; exit 1; }
export UAT_OUTPUT
UAT_OUTPUT=$(mktemp -d "$repo_dir/.local/frontend-v2-uat-XXXXXX")
export UAT_BASE_URL=https://127.0.0.1:${UAT_PORT:-9443}
uat_root=$UAT_OUTPUT/runtime
mkdir -p "$uat_root/server/config/content-config"
cp server/config/content-config/styles.yaml "$uat_root/server/config/content-config/"
cp -R server/style-references "$uat_root/server/"
java -Djava.net.preferIPv4Stack=true "-Dstoryblock.root=$uat_root" \
  -jar server/data/server/application.jar --policy=public "--port=${UAT_PORT:-9443}" \
  >"$UAT_OUTPUT/server.log" 2>&1 &
uat_pid=$!
cleanup() { kill "$uat_pid" 2>/dev/null || true; wait "$uat_pid" 2>/dev/null || true; }
trap cleanup EXIT
deadline=$((SECONDS + 45))
until uat_url=$(python3 scripts/server-listener.py "$uat_pid") \
  && curl -4 -kfsS --noproxy '*' --connect-timeout 1 --max-time 2 \
    "$uat_url/actuator/health" >/dev/null 2>&1; do
  kill -0 "$uat_pid" 2>/dev/null || { cat "$UAT_OUTPUT/server.log"; exit 1; }
  ((SECONDS < deadline)) || { echo "Startup timed out: $UAT_OUTPUT/server.log"; exit 1; }
  sleep .25
done
kill -0 "$uat_pid"
node client-web-v2/test/fixture-images.cjs
node client-web-v2/test/seed-reader.mjs
node client-web-v2/test/seed-catalog.mjs
node client-web-v2/test/seed-long.mjs
node client-web-v2/test/run-browser.cjs
echo "Evidence / 驗證紀錄: $UAT_OUTPUT"
