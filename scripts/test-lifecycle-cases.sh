#!/usr/bin/env bash
run_lifecycle_cases() {
  "$launcher" start >"$work/first-start.txt" & first=$!
  "$launcher" start >"$work/second-start.txt" & second=$!
  wait "$first"; wait "$second"
  read -r initial_pid initial_ticks <"$pid_file"
  curl -4 -kfsS --noproxy '*' https://127.0.0.1:19443/v1/admin/novels >"$work/catalog.json"
  STORYBLOCK_BASE_URL=https://127.0.0.1:19443 node "$repo_dir/plugin/scripts/storyblock-author.mjs" \
    register --source "$repo_dir/plugin/examples/minimal-novel.json" --json >"$work/registration.json"
  "$launcher" stop
  "$launcher" start
  STORYBLOCK_BASE_URL=https://127.0.0.1:19443 node "$repo_dir/plugin/scripts/storyblock-author.mjs" \
    verify --source "$repo_dir/plugin/examples/minimal-novel.json" --json >"$work/verification.json"
  "$launcher" stop
  # A stale success line cannot make an occupied-port startup succeed.
  printf '\nStarted StoryBlockApiApplication (old run)\n' >>"$work/.local/storyblock/logs/server.log"
  python3 -u - "$work/port-ready" <<'PY' &
import pathlib, socket, sys, time
with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as listener:
    listener.bind(('127.0.0.1', 19443))
    listener.listen()
    pathlib.Path(sys.argv[1]).touch()
    time.sleep(90)
PY
  holder=$!
  for _ in {1..40}; do [[ ! -f $work/port-ready ]] || break; sleep .05; done
  if "$launcher" start >"$work/occupied-port.txt" 2>&1; then
    echo 'Occupied-port startup incorrectly reported success' >&2; return 1
  fi
  [[ ! -s $pid_file ]]
  kill "$holder"; wait "$holder" 2>/dev/null || true
  holder=
  rm -f "$work/port-ready"
  sleep 90 & holder=$!
  ticks=$(awk '{print $22}' "/proc/$holder/stat")
  printf '%s %s\n' "$holder" "$ticks" >"$pid_file"
  if "$launcher" stop >"$work/unrelated-pid.txt" 2>&1; then
    echo 'Unrelated process identity was accepted' >&2; return 1
  fi
  kill -0 "$holder"
  kill "$holder"; wait "$holder" 2>/dev/null || true
  holder=
  rm -f "$pid_file"
  export STORYBLOCK_PORT_POLICY=local
  "$launcher" start
  read -r pid ticks <"$pid_file"
  python3 "$work/scripts/server-listener.py" "$pid"
  ss -4 -ltn | rg '127\.0\.0\.1:19443'
  "$launcher" stop
  echo 'PASS: concurrent start, restart persistence, stale logs, port collision, PID ownership, loopback'
}
