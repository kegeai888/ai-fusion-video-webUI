#!/usr/bin/env bash
set -euo pipefail

USERNAME=${1:-admin}
PASSWORD=${2:-admin123}
FRONTEND_BASE="http://localhost:7860"
BACKEND_BASE="http://localhost:18080"
SWAGGER_URL="$BACKEND_BASE/swagger-ui.html"
LOGIN_URL="$BACKEND_BASE/auth/login"
USER_INFO_URL="$BACKEND_BASE/auth/user-info"
HOST_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "localhost")

check_frontend() {
  curl -fsS "$FRONTEND_BASE" >/dev/null
}

check_backend() {
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" "$SWAGGER_URL" 2>/dev/null || echo "000")
  [[ "$status" == "200" || "$status" == "302" || "$status" == "401" ]]
}

echo "=========================================="
echo "融光 API 登录测试"
echo "后端地址: $BACKEND_BASE"
echo "Swagger 地址: $SWAGGER_URL"
echo "外网前端地址: http://$HOST_IP:7860"
echo "用户名: $USERNAME"
echo "=========================================="

echo ""
echo "[1/5] 检查前端连通性..."
check_frontend
echo "  ✓ 前端可达"

echo ""
echo "[2/5] 检查后端连通性..."
check_backend
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$SWAGGER_URL" 2>&1)
if [ "$HTTP_CODE" = "302" ] || [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "401" ]; then
  echo "  ✓ 后端可达 (HTTP $HTTP_CODE)"
else
  echo "  ✗ 后端不可达 (HTTP $HTTP_CODE)"
  exit 1
fi

echo ""
echo "[3/5] 验证认证拦截..."
AUTH_HTTP_CODE=$(curl -s -o /tmp/auth_check.json -w "%{http_code}" "$USER_INFO_URL" 2>&1)
if [ "$AUTH_HTTP_CODE" = "401" ] || [ "$AUTH_HTTP_CODE" = "403" ]; then
  echo "  ✓ 未登录状态正确被拦截 (HTTP $AUTH_HTTP_CODE)"
else
  echo "  - HTTP $AUTH_HTTP_CODE，响应: $(head -c 200 /tmp/auth_check.json)"
fi

echo ""
echo "[4/5] 执行登录请求..."
export USERNAME
export PASSWORD
LOGIN_PAYLOAD=$(python3 -c 'import json, os; print(json.dumps({"username": os.environ["USERNAME"], "password": os.environ["PASSWORD"]}, ensure_ascii=False))')
LOGIN_RESP=$(curl -s -X POST "$LOGIN_URL" \
  -H "Content-Type: application/json" \
  -d "$LOGIN_PAYLOAD" 2>&1)

echo "  响应: $(echo "$LOGIN_RESP" | head -c 500)"

ACCESS_TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('accessToken',''))" 2>/dev/null)

if [ -z "$ACCESS_TOKEN" ]; then
  echo "  ✗ 登录失败，未获取到 accessToken"
  echo "  完整响应: $LOGIN_RESP"
  exit 1
fi
echo "  ✓ 登录成功，获取到 accessToken"

echo ""
echo "[5/5] 验证 Token 有效性..."
ME_RESP=$(curl -s "$USER_INFO_URL" \
  -H "Authorization: Bearer $ACCESS_TOKEN" 2>&1)

echo "  响应: $(echo "$ME_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(json.dumps(d.get('data',{}), ensure_ascii=False))" 2>/dev/null || echo "$ME_RESP" | head -c 300)"

if echo "$ME_RESP" | grep -qi "username\|nickname"; then
  echo "  ✓ Token 有效，接口返回用户信息"
else
  echo "  ✗ Token 验证失败"
  exit 1
fi

echo ""
echo "=========================================="
echo "测试通过"
echo "=========================================="
