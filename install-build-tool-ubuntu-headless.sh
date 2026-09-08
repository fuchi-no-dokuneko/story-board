#!/usr/bin/env bash
set -euo pipefail
# Administrator entry point; inspect before running with sudo.
if ((EUID != 0)); then
  echo 'Run with sudo / 請以 sudo 執行 / 请以 sudo 执行' >&2
  exit 1
fi
apt-get update
apt-get install -y make cmake build-essential openjdk-21-jdk-headless \
  curl unzip openssl sqlite3 zstd iproute2 util-linux jq
