#!/usr/bin/env bash
set -euo pipefail
umask 077
repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)
local_dir=$repo_dir/.local/storyblock
start_after_install=false
case "${1:-}" in
  --start) start_after_install=true ;;
  -h|--help)
    echo 'Usage: ./install.sh [--start] — repository-local installation'
    echo '儲存庫內安裝；無需管理員權限。 / 仓库内安装；无需管理员权限。'
    exit 0 ;;
  '') ;;
  *) echo 'Usage: ./install.sh [--start]' >&2; exit 2 ;;
esac
(($# <= 1)) || exit 2
cd "$repo_dir"
source scripts/build-environment.sh
repository_path "$local_dir" >/dev/null
mkdir -p "$local_dir"
exec 9>"$local_dir/install.lock"
flock 9
./install-local-build.sh
./mvnw --batch-mode -DskipTests clean package
source scripts/install-runtime.sh
install_runtime
"$repo_dir/scripts/generate-self-signed-tls.sh"
echo 'Installed / 安裝完成 / 安装完成'
echo 'Start: ./scripts/local-server.sh start'
echo 'URL: https://127.0.0.1:8443/ (self-signed / 自簽 / 自签)'
flock -u 9
exec 9>&-
if [[ $start_after_install == true ]]; then
  exec "$repo_dir/scripts/local-server.sh" start
fi
