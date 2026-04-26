#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/ai-fusion-video"
FRONTEND_DIR="$ROOT_DIR/ai-fusion-video-web"
BACKEND_LOG="$ROOT_DIR/backend.log"
FRONTEND_LOG="$ROOT_DIR/frontend.log"
BACKEND_PID_FILE="$ROOT_DIR/.backend.pid"
FRONTEND_PID_FILE="$ROOT_DIR/.frontend.pid"

log() {
  printf '[start_app] %s\n' "$1"
}

kill_pid_file() {
  local pid_file="$1"

  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(cat "$pid_file")"

    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
      wait "$pid" 2>/dev/null || true
    fi

    rm -f "$pid_file"
  fi
}

cleanup() {
  kill_pid_file "$BACKEND_PID_FILE"
  kill_pid_file "$FRONTEND_PID_FILE"
}

start_backend() {
  : >"$BACKEND_LOG"
  nohup bash -lc "cd '$BACKEND_DIR' && exec sh ./mvnw spring-boot:run" >"$BACKEND_LOG" 2>&1 &
  echo $! >"$BACKEND_PID_FILE"
}

start_frontend() {
  : >"$FRONTEND_LOG"
  nohup bash -lc "cd '$FRONTEND_DIR' && export NEXT_PUBLIC_API_BASE_URL='http://localhost:18080' && exec pnpm dev --hostname 0.0.0.0 --port 7860" >"$FRONTEND_LOG" 2>&1 &
  echo $! >"$FRONTEND_PID_FILE"
}

wait_for_url() {
  local url="$1"
  local label="$2"

  for _ in $(seq 1 60); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done

  log "$label did not become ready: $url"
  return 1
}

if [[ ! -d "$BACKEND_DIR" ]]; then
  echo "backend directory not found: $BACKEND_DIR" >&2
  exit 1
fi

if [[ ! -d "$FRONTEND_DIR" ]]; then
  echo "frontend directory not found: $FRONTEND_DIR" >&2
  exit 1
fi

trap cleanup EXIT INT TERM

cleanup

log "starting backend"
start_backend

log "waiting for backend"
wait_for_url "http://127.0.0.1:18080/swagger-ui.html" "backend"

log "starting frontend"
start_frontend

log "waiting for frontend"
wait_for_url "http://127.0.0.1:7860" "frontend"

log "application started"
trap - EXIT INT TERM
