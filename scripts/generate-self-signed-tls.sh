#!/usr/bin/env bash
set -euo pipefail
umask 077
repo_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
source "$repo_dir/scripts/build-environment.sh"
jar=$repo_dir/server/data/server/application.jar
[[ -r $jar ]] || { echo 'Run ./install.sh first' >&2; exit 1; }
exec "$JAVA_HOME/bin/java" -Djava.net.preferIPv4Stack=true \
  "-Djava.io.tmpdir=$TMPDIR" "-Dstoryblock.root=$repo_dir" \
  -Dloader.main=dev.storyblock.api.runtime.CertificateSetup \
  -cp "$jar" org.springframework.boot.loader.launch.PropertiesLauncher
