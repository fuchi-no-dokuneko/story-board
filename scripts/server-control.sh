#!/usr/bin/env bash
server_status() {
  read_process
  if is_server; then
    echo "StoryBlock running / 執行中 / 运行中: PID $pid"
  else
    echo 'StoryBlock stopped / 已停止 / 已停止'
    return 1
  fi
}
server_start() {
  read_process
  if is_server; then server_status; return; fi
  rm -f -- "$pid_file"
  nohup "$repo_dir/scripts/local-server.sh" run >>"$log_file" 2>&1 9>&- &
  pid=$!
  ticks=$(process_ticks "$pid")
  printf '%s %s\n' "$pid" "$ticks" >"$pid_file"
  deadline=$((SECONDS + 60))
  while ((SECONDS < deadline)); do
    if ! is_running; then
      rm -f -- "$pid_file"
      echo "Startup failed: $log_file" >&2
      return 1
    fi
    if is_server && url=$(python3 "$repo_dir/scripts/server-listener.py" "$pid") \
      && curl -4 -kfsS --noproxy '*' --connect-timeout 1 --max-time 2 \
        "$url/actuator/health" >/dev/null 2>&1; then
      echo "StoryBlock ready / 已就緒 / 已就绪: $url"
      return
    fi
    sleep 0.25
  done
  echo "Startup timed out: $log_file" >&2
  server_stop
  return 1
}
server_stop() {
  read_process
  if ! is_running; then rm -f -- "$pid_file"; return; fi
  if ! is_server; then
    echo "Refusing to stop unrelated PID $pid" >&2
    return 1
  fi
  kill "$pid"
  deadline=$((SECONDS + 30))
  while ((SECONDS < deadline)); do
    if ! is_running; then
      rm -f -- "$pid_file"
      echo 'StoryBlock stopped / 已停止 / 已停止'
      return
    fi
    sleep 0.25
  done
  echo "Shutdown timed out: PID $pid" >&2
  return 1
}
