#!/usr/bin/env bash
set -euo pipefail
umask 077
repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
source "$repo_dir/scripts/build-environment.sh"
for tool in java javac keytool curl unzip python3; do
  command -v "$tool" >/dev/null || { echo "Missing build tool: $tool" >&2; exit 1; }
done
maven_dir=$MAVEN_USER_HOME/apache-maven-3.9.11
if [[ ! -x $maven_dir/bin/mvn ]]; then
  archive=$MAVEN_USER_HOME/apache-maven-3.9.11-bin.zip
  trap 'rm -f -- "$archive"' EXIT
  curl -4 --fail --location --retry 0 --connect-timeout 15 --max-time 120 \
    https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip \
    -o "$archive"
  unzip -q -o "$archive" -d "$MAVEN_USER_HOME"
fi
echo 'Local build tools ready / 本機建置工具已備妥 / 本机构建工具已就绪'
