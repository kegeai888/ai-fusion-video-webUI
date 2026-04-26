#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT=${1:-7860}
BACKEND_PORT=18080
HOST_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "localhost")
BACKEND_LOG="$SCRIPT_DIR/backend.log"
FRONTEND_LOG="$SCRIPT_DIR/frontend.log"
BACKEND_DIR="$SCRIPT_DIR/ai-fusion-video"
FRONTEND_DIR="$SCRIPT_DIR/ai-fusion-video-web"
BACKEND_JAR="$BACKEND_DIR/target/ai-fusion-video-0.2.1.jar"
BACKEND_SWAGGER_URL="http://127.0.0.1:$BACKEND_PORT/swagger-ui.html"
FRONTEND_URL="http://127.0.0.1:$PORT"

is_port_open() {
  local port="$1"
  lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
}

is_frontend_process() {
  local args="$1"
  echo "$args" | grep -Eq 'next-server|next dev|pnpm dev|node.*next|/next@|ai-fusion-video-web'
}

kill_matching_processes() {
  local port="$1"

  while read -r pid; do
    PROC_NAME=$(ps -p "$pid" -o comm= 2>/dev/null || true)
    if echo "$PROC_NAME" | grep -qi "jupyter"; then
      echo "跳过 jupyterlab 进程 (PID: $pid)"
      continue
    fi
    PROC_ARGS=$(ps -p "$pid" -o args= 2>/dev/null || true)
    if is_frontend_process "$PROC_ARGS"; then
      echo "终止进程 PID: $pid ($PROC_NAME)"
      kill "$pid" 2>/dev/null || true
    else
      echo "跳过未知进程 PID: $pid ($PROC_NAME)"
    fi
  done < <(lsof -t -i:"$port" 2>/dev/null || true)
}

kill_process_tree() {
  local pid="$1"
  local signal="$2"
  local child

  while read -r child; do
    [[ -n "$child" ]] || continue
    kill_process_tree "$child" "$signal"
  done < <(pgrep -P "$pid" || true)

  kill "$signal" "$pid" 2>/dev/null || true
}

force_kill_matching_processes() {
  local port="$1"

  while read -r pid; do
    PROC_NAME=$(ps -p "$pid" -o comm= 2>/dev/null || true)
    PROC_ARGS=$(ps -p "$pid" -o args= 2>/dev/null || true)
    if echo "$PROC_NAME" | grep -qi "jupyter"; then
      echo "跳过 jupyterlab 进程 (PID: $pid)"
      continue
    fi
    if is_frontend_process "$PROC_ARGS"; then
      echo "强制终止进程树 PID: $pid ($PROC_NAME)"
      kill_process_tree "$pid" -9
    fi
  done < <(lsof -t -i:"$port" 2>/dev/null || true)
}

wait_for_http() {
  local url="$1"

  for _ in $(seq 1 30); do
    local status
    status=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    if [[ "$status" == "200" || "$status" == "302" || "$status" == "401" ]]; then
      return 0
    fi
    sleep 2
  done

  return 1
}

build_backend_env() {
  BACKEND_ENV=(env)

  if [[ -n "${SPRING_DATASOURCE_URL:-}" ]]; then
    BACKEND_ENV+=("SPRING_DATASOURCE_URL=$SPRING_DATASOURCE_URL")
  fi
  if [[ -n "${SPRING_DATA_REDIS_HOST:-}" ]]; then
    BACKEND_ENV+=("SPRING_DATA_REDIS_HOST=$SPRING_DATA_REDIS_HOST")
  fi
  if [[ -n "${SPRING_DATA_REDIS_PORT:-}" ]]; then
    BACKEND_ENV+=("SPRING_DATA_REDIS_PORT=$SPRING_DATA_REDIS_PORT")
  fi
  if [[ -n "${SPRING_DATA_REDIS_PASSWORD:-}" ]]; then
    BACKEND_ENV+=("SPRING_DATA_REDIS_PASSWORD=$SPRING_DATA_REDIS_PASSWORD")
  fi
}

echo "=========================================="
echo "融光 AI 视频创作平台启动脚本"
echo "前端端口: $PORT"
echo "后端端口: $BACKEND_PORT"
echo "后端地址: http://$HOST_IP:$BACKEND_PORT"
echo "=========================================="

echo "[1/6] 清理前端端口 $PORT ..."
rm -f "$FRONTEND_DIR/.next/dev/lock" 2>/dev/null || true
kill_matching_processes "$PORT"

for _ in $(seq 1 10); do
  if ! is_port_open "$PORT"; then
    break
  fi
  sleep 1
done

if is_port_open "$PORT"; then
  force_kill_matching_processes "$PORT"
fi

for _ in $(seq 1 10); do
  if ! is_port_open "$PORT"; then
    break
  fi
  sleep 1
done

if is_port_open "$PORT"; then
  echo "前端端口 $PORT 仍被占用" >&2
  exit 1
fi

echo "前端端口 $PORT 已清理"

echo "[2/6] 清理后端端口 $BACKEND_PORT ..."
while read -r pid; do
  PROC_NAME=$(ps -p "$pid" -o comm= 2>/dev/null || true)
  if echo "$PROC_NAME" | grep -qi "jupyter"; then
    echo "跳过 jupyterlab 进程 (PID: $pid)"
    continue
  fi
  PROC_ARGS=$(ps -p "$pid" -o args= 2>/dev/null || true)
  if echo "$PROC_ARGS" | grep -q "$BACKEND_DIR" || echo "$PROC_ARGS" | grep -q "$BACKEND_JAR"; then
    echo "终止进程 PID: $pid ($PROC_NAME)"
    kill -9 "$pid" 2>/dev/null || true
  else
    echo "跳过未知进程 PID: $pid ($PROC_NAME)"
  fi
done < <(lsof -t -i:"$BACKEND_PORT" 2>/dev/null || true)
sleep 2
echo "后端端口 $BACKEND_PORT 已清理"

echo "[3/6] 启动后端..."
: > "$BACKEND_LOG"
build_backend_env
if [[ -f "$BACKEND_JAR" ]]; then
  nohup "${BACKEND_ENV[@]}" bash -lc "cd '$BACKEND_DIR' && java -Xmx512m -jar '$BACKEND_JAR'" >"$BACKEND_LOG" 2>&1 &
else
  nohup "${BACKEND_ENV[@]}" bash -lc "cd '$BACKEND_DIR' && ./mvnw spring-boot:run" >"$BACKEND_LOG" 2>&1 &
fi
BACKEND_PID=$!
echo "后端已启动 PID: $BACKEND_PID"

echo "[4/6] 启动前端..."
: > "$FRONTEND_LOG"
nohup env PORT="$PORT" NEXT_PUBLIC_API_BASE_URL="/api" BACKEND_API_BASE_URL="http://localhost:$BACKEND_PORT/api" bash -lc "cd '$FRONTEND_DIR' && pnpm dev --hostname 0.0.0.0 --port '$PORT'" >"$FRONTEND_LOG" 2>&1 &
FRONTEND_PID=$!
echo "前端已启动 PID: $FRONTEND_PID"

for _ in $(seq 1 10); do
  if ps -p "$FRONTEND_PID" >/dev/null 2>&1; then
    sleep 1
    continue
  fi
  echo "前端进程已提前退出，检查日志: $FRONTEND_LOG" >&2
  exit 1
done

echo "[5/6] 等待服务就绪..."
wait_for_http "$BACKEND_SWAGGER_URL"
wait_for_http "$FRONTEND_URL"

echo "[6/6] 检查服务状态..."
FRONTEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$FRONTEND_URL" 2>/dev/null || echo "000")
BACKEND_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BACKEND_SWAGGER_URL" 2>/dev/null || echo "000")

echo ""
echo "=========================================="
echo "服务状态:"
echo "  前端 (http://localhost:$PORT): $FRONTEND_STATUS"
echo "  后端 (http://$HOST_IP:$BACKEND_PORT): $BACKEND_STATUS"
echo "  API文档: http://localhost:$BACKEND_PORT/swagger-ui.html"
echo "  后端日志: $BACKEND_LOG"
echo "  前端日志: $FRONTEND_LOG"
echo "=========================================="
