#!/usr/bin/env bash
install_runtime() {
  for directory in data logs run runtime secrets server tls/private tls/public tmp; do
    repository_path "$local_dir/$directory" >/dev/null
    mkdir -p "$local_dir/$directory"
  done
  printf '%s\n' "$JAVA_HOME" >"$local_dir/runtime/java-home"
  mapfile -t api_jars < <(find "$repo_dir/server/target" -maxdepth 1 -type f \
    -name 'storyblock-api-*.jar' ! -name '*.original' -print)
  if ((${#api_jars[@]} != 1)); then
    echo 'Expected exactly one API jar' >&2
    return 1
  fi
  installed_jar=$local_dir/server/application.jar
  staged_jar=$local_dir/server/.application.jar.$$
  trap 'rm -f -- "$staged_jar"' EXIT
  install -m 500 -- "${api_jars[0]}" "$staged_jar"
  mv -f -- "$staged_jar" "$installed_jar"
  trap - EXIT
  for secret_name in owner-token server-pepper; do
    secret_file=$local_dir/secrets/$secret_name
    repository_path "$secret_file" >/dev/null
    if [[ ! -s $secret_file ]]; then
      od -An -N48 -tx1 /dev/urandom | tr -d ' \n' >"$secret_file"
    fi
    chmod 600 "$secret_file"
  done
}
