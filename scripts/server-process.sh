#!/usr/bin/env bash
read_process() {
  pid= ticks=
  [[ ! -s $pid_file ]] || read -r pid ticks <"$pid_file"
}
process_ticks() {
  awk '{print $22}' "/proc/$1/stat" 2>/dev/null
}
is_running() {
  [[ ${pid:-} =~ ^[0-9]+$ && ${ticks:-} =~ ^[0-9]+$ ]] || return 1
  [[ $(process_ticks "$pid") == "$ticks" ]] || return 1
  [[ $(awk '{print $3}' "/proc/$pid/stat" 2>/dev/null) != Z ]] || return 1
  kill -0 "$pid" 2>/dev/null
}
is_server() {
  is_running || return 1
  python3 - "$pid" "$jar" "$repo_dir" <<'PY'
import pathlib, sys
try:
    args = pathlib.Path(f'/proc/{sys.argv[1]}/cmdline').read_bytes().split(b'\0')
    valid = sys.argv[2].encode() in args and f'-Dstoryblock.root={sys.argv[3]}'.encode() in args
    sys.exit(0 if valid else 1)
except OSError:
    sys.exit(1)
PY
}
