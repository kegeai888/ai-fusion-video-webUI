#!/usr/bin/env bash
set -euo pipefail

# Copy this script to /start.d and keep it executable.
# It expects to run from the repository root or a copied deployment bundle
# that still contains start_app.sh and the ai-fusion-video subdirectory.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MIDDLEWARE_DIR="$ROOT_DIR/ai-fusion-video"
BOOT_LOG_PREFIX="[boot-start]"
MYSQL_PORT="${MYSQL_PORT:-43306}"
MYSQL_LOCAL_PORT="${MYSQL_LOCAL_PORT:-3306}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-123456}"
REDIS_PORT="${REDIS_PORT:-46379}"
REDIS_LOCAL_PORT="${REDIS_LOCAL_PORT:-6379}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PASSWORD="${REDIS_PASSWORD:-123456}"

log() {
  printf '%s %s\n' "$BOOT_LOG_PREFIX" "$1"
}

is_port_open() {
  local host="$1"
  local port="$2"

  python3 - "$host" "$port" <<'PY'
import socket
import sys

host = sys.argv[1]
port = int(sys.argv[2])
with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
    sock.settimeout(1)
    sys.exit(0 if sock.connect_ex((host, port)) == 0 else 1)
PY
}

wait_for_mysql() {
  local host="$1"
  local port="$2"

  for _ in $(seq 1 30); do
    if mysqladmin ping -h "$host" -P "$port" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --silent >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done

  log "MySQL did not become ready on ${host}:${port}"
  return 1
}

wait_for_redis() {
  local host="$1"
  local port="$2"

  for _ in $(seq 1 30); do
    if redis-cli -h "$host" -p "$port" ping >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done

  log "Redis did not become ready on ${host}:${port}"
  return 1
}

start_local_mysql() {
  if command -v service >/dev/null 2>&1; then
    service mysql start >/dev/null 2>&1 || service mysqld start >/dev/null 2>&1 || true
  fi
}

start_local_redis() {
  if command -v service >/dev/null 2>&1; then
    service redis-server start >/dev/null 2>&1 || service redis start >/dev/null 2>&1 || true
  fi
}

ensure_mysql() {
  if is_port_open "$MYSQL_HOST" "$MYSQL_PORT"; then
    wait_for_mysql "$MYSQL_HOST" "$MYSQL_PORT"
    export SPRING_DATASOURCE_URL="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/ai_fusion_video?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
    return 0
  fi

  if is_port_open "$MYSQL_HOST" "$MYSQL_LOCAL_PORT"; then
    log "using local MySQL on ${MYSQL_HOST}:${MYSQL_LOCAL_PORT}"
    wait_for_mysql "$MYSQL_HOST" "$MYSQL_LOCAL_PORT"
    export SPRING_DATASOURCE_URL="jdbc:mysql://${MYSQL_HOST}:${MYSQL_LOCAL_PORT}/ai_fusion_video?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
    return 0
  fi

  start_local_mysql

  if is_port_open "$MYSQL_HOST" "$MYSQL_LOCAL_PORT"; then
    log "started local MySQL on ${MYSQL_HOST}:${MYSQL_LOCAL_PORT}"
    wait_for_mysql "$MYSQL_HOST" "$MYSQL_LOCAL_PORT"
    export SPRING_DATASOURCE_URL="jdbc:mysql://${MYSQL_HOST}:${MYSQL_LOCAL_PORT}/ai_fusion_video?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
    return 0
  fi

  if command -v docker >/dev/null 2>&1; then
    log "starting MySQL via Docker Compose"
    docker compose -f "$MIDDLEWARE_DIR/docker-compose-middleware.yml" up -d mysql
    wait_for_mysql "$MYSQL_HOST" "$MYSQL_PORT"
    export SPRING_DATASOURCE_URL="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/ai_fusion_video?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
    return 0
  fi

  log "MySQL is unavailable. Start a local MySQL service on ${MYSQL_LOCAL_PORT} or install Docker for ${MYSQL_PORT}."
  return 1
}

ensure_redis() {
  if is_port_open "$REDIS_HOST" "$REDIS_PORT"; then
    wait_for_redis "$REDIS_HOST" "$REDIS_PORT"
    export SPRING_DATA_REDIS_HOST="$REDIS_HOST"
    export SPRING_DATA_REDIS_PORT="$REDIS_PORT"
    return 0
  fi

  if is_port_open "$REDIS_HOST" "$REDIS_LOCAL_PORT"; then
    log "using local Redis on ${REDIS_HOST}:${REDIS_LOCAL_PORT}"
    wait_for_redis "$REDIS_HOST" "$REDIS_LOCAL_PORT"
    export SPRING_DATA_REDIS_HOST="$REDIS_HOST"
    export SPRING_DATA_REDIS_PORT="$REDIS_LOCAL_PORT"
    return 0
  fi

  start_local_redis

  if is_port_open "$REDIS_HOST" "$REDIS_LOCAL_PORT"; then
    log "started local Redis on ${REDIS_HOST}:${REDIS_LOCAL_PORT}"
    wait_for_redis "$REDIS_HOST" "$REDIS_LOCAL_PORT"
    export SPRING_DATA_REDIS_HOST="$REDIS_HOST"
    export SPRING_DATA_REDIS_PORT="$REDIS_LOCAL_PORT"
    return 0
  fi

  if command -v docker >/dev/null 2>&1; then
    log "starting Redis via Docker Compose"
    docker compose -f "$MIDDLEWARE_DIR/docker-compose-middleware.yml" up -d redis
    wait_for_redis "$REDIS_HOST" "$REDIS_PORT"
    export SPRING_DATA_REDIS_HOST="$REDIS_HOST"
    export SPRING_DATA_REDIS_PORT="$REDIS_PORT"
    return 0
  fi

  log "Redis is unavailable. Start a local Redis service on ${REDIS_LOCAL_PORT} or install Docker for ${REDIS_PORT}."
  return 1
}

check_backend() {
  curl -fsS http://127.0.0.1:18080/swagger-ui.html >/dev/null
}

check_frontend() {
  curl -fsS http://127.0.0.1:7860 >/dev/null
}

if [[ ! -x "$ROOT_DIR/start_app.sh" ]]; then
  echo "start_app.sh must be executable" >&2
  exit 1
fi

log "ensuring MySQL"
ensure_mysql

log "ensuring Redis"
ensure_redis

log "starting application"
bash "$ROOT_DIR/start_app.sh"

log "checking backend"
check_backend

log "checking frontend"
check_frontend

log "startup complete"
