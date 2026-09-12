#!/usr/bin/env bash
assert_listener() {
  ss -4 -ltn | rg -F "$1:$2"
  curl -4 -kfsS --noproxy '*' "https://127.0.0.1:$2/actuator/health" >"$work/options-health.json"
  "$launcher" stop
}
run_server_options_cases() {
  local config=$work/server/config/port-config.yaml
  # Configuration supplies defaults when neither CLI nor environment overrides it.
  sed -i 's/^policy:.*/policy: local/; s/^port:.*/port: 19442/' "$config"
  unset STORYBLOCK_PORT_POLICY STORYBLOCK_LOCAL_PORT
  "$launcher" start
  assert_listener 127.0.0.1 19442
  # Existing environment options still override configuration.
  STORYBLOCK_PORT_POLICY=public STORYBLOCK_LOCAL_PORT=19444 "$launcher" start
  assert_listener 0.0.0.0 19444
  # Equals-form arguments override conflicting environment and configuration.
  STORYBLOCK_PORT_POLICY=local STORYBLOCK_LOCAL_PORT=19443 \
    "$launcher" start --policy=public --port=19444
  assert_listener 0.0.0.0 19444
  # The Java fallback also defaults to public when the YAML omits policy.
  sed -i '/^policy:/d' "$config"
  "$launcher" start --port 19444
  assert_listener 0.0.0.0 19444
  cp "$repo_dir/server/config/port-config.yaml" "$config"
  echo 'PASS: config, legacy environment, CLI precedence, custom port, public fallback'
}
