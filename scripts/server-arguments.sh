#!/usr/bin/env bash
server_usage() {
  echo 'Usage: local-server.sh {start|run} [--policy local|standard|public] [--port 1..65535]'
  echo '       local-server.sh {stop|status|logs|--help}'
  echo 'Defaults: public (0.0.0.0), port 8443; options override environment and config.'
}
server_argument_error() {
  echo "$*" >&2
  server_usage >&2
  exit 2
}
parse_server_arguments() {
  server_action=${1:-status}
  (($# == 0)) || shift
  launch_args=()
  case "$server_action" in
    -h|--help) server_usage; exit 0 ;;
    start|run) ;;
    stop|status|logs)
      (($# == 0)) || server_argument_error "$server_action does not accept options"
      return ;;
    *) server_argument_error "Unknown command: $server_action" ;;
  esac
  local server_policy=${STORYBLOCK_PORT_POLICY:-}
  local server_port=${STORYBLOCK_LOCAL_PORT:-}
  while (($#)); do
    case "$1" in
      -h|--help) server_usage; exit 0 ;;
      --policy|--port)
        (($# >= 2)) && [[ -n $2 ]] || server_argument_error "Missing value for $1"
        if [[ $1 == --policy ]]; then server_policy=$2; else server_port=$2; fi
        shift 2 ;;
      --policy=*)
        server_policy=${1#*=}
        [[ -n $server_policy ]] || server_argument_error 'Missing value for --policy'
        shift ;;
      --port=*)
        server_port=${1#*=}
        [[ -n $server_port ]] || server_argument_error 'Missing value for --port'
        shift ;;
      *) server_argument_error "Unknown option: $1" ;;
    esac
  done
  case "$server_policy" in
    ''|local|standard|public) ;;
    *) server_argument_error 'Policy must be local, standard, or public' ;;
  esac
  [[ -z $server_policy ]] || launch_args+=("--policy=$server_policy")
  if [[ -n $server_port ]]; then
    [[ $server_port =~ ^[1-9][0-9]{0,4}$ ]] && ((server_port <= 65535)) \
      || server_argument_error 'Port must be an integer from 1 to 65535'
    launch_args+=("--port=$server_port")
  fi
}
