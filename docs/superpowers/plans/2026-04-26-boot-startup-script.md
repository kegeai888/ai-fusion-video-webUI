# Boot Startup Script Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a boot-time shell entrypoint that can be placed under `/start.d` to automatically start MySQL, Redis, the Spring Boot API, and the frontend web process on port 7860.

**Architecture:** Keep one thin boot orchestration script responsible for startup order, readiness checks, and health verification. Reuse the existing repository startup flow for backend and frontend where possible, and keep Docker middleware startup isolated to the backend subproject’s compose file.

**Tech Stack:** Bash, Docker Compose, Spring Boot local profile, Next.js dev server, curl, mysqladmin, redis-cli

---

## File Structure

- Create: `boot-start.sh` — repository-managed boot orchestration script intended to be copied into `/start.d` and made executable.
- Modify: `start_app.sh` — align the existing app startup script with boot orchestration requirements so repeated execution is safe and predictable.
- Modify: `test_login.sh` — extend executable verification so startup validation covers dependency, login, and API health behavior after boot.
- Reference: `ai-fusion-video/docker-compose-middleware.yml` — existing MySQL and Redis startup entrypoint for local ports `43306` and `46379`.
- Reference: `ai-fusion-video/src/main/resources/application-local.yaml` — backend local profile expects MySQL on `43306` and Redis on `46379`.

## Task 1: Add boot script executable behavior

**Files:**
- Create: `boot-start.sh`
- Reference: `ai-fusion-video/docker-compose-middleware.yml`
- Reference: `start_app.sh`

- [ ] **Step 1: Write the failing shell test**

Create `boot-start.sh` with this temporary executable content so the first verification fails for the right reason:

```bash
#!/usr/bin/env bash
set -euo pipefail

echo "boot-start.sh not implemented" >&2
exit 1
```

- [ ] **Step 2: Run script to verify it fails**

Run: `bash ./boot-start.sh`
Expected: exits with status `1` and prints `boot-start.sh not implemented`

- [ ] **Step 3: Write minimal boot orchestration implementation**

Replace the placeholder with this implementation:

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MIDDLEWARE_DIR="$ROOT_DIR/ai-fusion-video"
BOOT_LOG_PREFIX="[boot-start]"

log() {
  printf '%s %s\n' "$BOOT_LOG_PREFIX" "$1"
}

wait_for_mysql() {
  for _ in $(seq 1 30); do
    if mysqladmin ping -h 127.0.0.1 -P 43306 -uroot -p123456 --silent >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

wait_for_redis() {
  for _ in $(seq 1 30); do
    if redis-cli -h 127.0.0.1 -p 46379 -a 123456 ping >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

check_backend() {
  curl -fsS http://127.0.0.1:18080/api/swagger-ui.html >/dev/null
}

check_frontend() {
  curl -fsS http://127.0.0.1:7860 >/dev/null
}

log "starting dependencies"
docker compose -f "$MIDDLEWARE_DIR/docker-compose-middleware.yml" up -d

log "waiting for MySQL"
wait_for_mysql

log "waiting for Redis"
wait_for_redis

log "starting application"
bash "$ROOT_DIR/start_app.sh"

log "checking backend"
check_backend

log "checking frontend"
check_frontend

log "startup complete"
```

- [ ] **Step 4: Run script to verify the base flow passes**

Run: `bash ./boot-start.sh`
Expected: prints the six phase messages, exits `0`, and leaves dependencies plus application startup invoked.

- [ ] **Step 5: Commit**

```bash
git add boot-start.sh
git commit -m "feat: add boot startup script"
```

## Task 2: Make repeated startup safe in the existing app launcher

**Files:**
- Modify: `start_app.sh`
- Test: `boot-start.sh`

- [ ] **Step 1: Write the failing idempotency test**

Append this temporary verification block near the end of `boot-start.sh` immediately before `log "startup complete"`:

```bash
bash "$ROOT_DIR/start_app.sh"
```

This intentionally calls the launcher twice so the current behavior exposes duplicate-start problems.

- [ ] **Step 2: Run script to verify it fails or behaves incorrectly**

Run: `bash ./boot-start.sh`
Expected: the second application launch fails, collides on ports, or leaves duplicate processes. Capture the exact failure mode before changing `start_app.sh`.

- [ ] **Step 3: Write minimal idempotent startup logic in `start_app.sh`**

Update `start_app.sh` so repeated execution cleans up only this project’s previous backend and frontend processes, then starts both services exactly once. The script should follow this shape:

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/ai-fusion-video"
FRONTEND_DIR="$ROOT_DIR/ai-fusion-video-web"
BACKEND_LOG="$ROOT_DIR/backend.log"
FRONTEND_LOG="$ROOT_DIR/frontend.log"

pkill -f "$BACKEND_DIR/.mvn/wrapper/maven-wrapper.jar" 2>/dev/null || true
pkill -f "$FRONTEND_DIR" 2>/dev/null || true
pkill -f "next-server" 2>/dev/null || true

nohup bash -lc "cd '$BACKEND_DIR' && ./mvnw spring-boot:run" >"$BACKEND_LOG" 2>&1 &
nohup bash -lc "cd '$FRONTEND_DIR' && pnpm dev --hostname 0.0.0.0 --port 7860" >"$FRONTEND_LOG" 2>&1 &
```

Keep any existing health-check loop that already matches this repository, but remove duplicate startup branches and keep the script deterministic.

- [ ] **Step 4: Run boot script twice to verify idempotency passes**

Run: `bash ./boot-start.sh && bash ./boot-start.sh`
Expected: both invocations exit `0`, with one reachable backend and one reachable frontend after the second run.

- [ ] **Step 5: Commit**

```bash
git add start_app.sh boot-start.sh
git commit -m "fix: make startup idempotent"
```

## Task 3: Add executable verification for post-boot health

**Files:**
- Modify: `test_login.sh`
- Test: `boot-start.sh`

- [ ] **Step 1: Write the failing verification update**

Add this assertion near the top of `test_login.sh` before any login flow:

```bash
curl -fsS http://127.0.0.1:7860 >/dev/null
```

This should fail until the script reports frontend availability as part of the startup contract.

- [ ] **Step 2: Run verification to watch it fail for the expected reason**

Run: `bash ./test_login.sh`
Expected: fails on the new frontend availability command if the frontend is not reachable.

- [ ] **Step 3: Write minimal verification output and health checks**

Update `test_login.sh` to include these exact helpers near the top of the file:

```bash
#!/usr/bin/env bash
set -euo pipefail

check_frontend() {
  curl -fsS http://127.0.0.1:7860 >/dev/null
}

check_backend() {
  curl -fsS http://127.0.0.1:18080/api/swagger-ui.html >/dev/null
}

check_frontend
check_backend
```

Keep the existing login assertions after these checks so the script validates both startup health and authentication flow.

- [ ] **Step 4: Run end-to-end verification to confirm it passes**

Run: `bash ./boot-start.sh && bash ./test_login.sh`
Expected: boot startup exits `0`, frontend health passes, backend health passes, and login verification passes.

- [ ] **Step 5: Commit**

```bash
git add test_login.sh
git commit -m "test: verify boot startup health"
```

## Task 4: Prepare the script for `/start.d` installation

**Files:**
- Modify: `boot-start.sh`

- [ ] **Step 1: Write the failing installation-path check**

Temporarily add this guard near the top of `boot-start.sh` after `ROOT_DIR` is set:

```bash
if [[ ! -x "$ROOT_DIR/start_app.sh" ]]; then
  echo "start_app.sh must be executable" >&2
  exit 1
fi
```

This fails until installation prerequisites are explicit.

- [ ] **Step 2: Run script to verify the prerequisite check behaves correctly**

Run: `chmod -x ./start_app.sh && bash ./boot-start.sh`
Expected: exits `1` with `start_app.sh must be executable`

Then restore permissions:

Run: `chmod +x ./start_app.sh`
Expected: no output

- [ ] **Step 3: Write minimal install-ready header and usage note in script**

Keep the executable guard and add this comment block at the top of `boot-start.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

# Copy this script to /start.d and keep it executable.
# It expects to run from the repository root or a copied deployment bundle
# that still contains start_app.sh and the ai-fusion-video subdirectory.
```

Do not add extra branching for unrelated deployment modes.

- [ ] **Step 4: Run final verification**

Run: `bash ./boot-start.sh && bash ./test_login.sh`
Expected: all checks pass with `start_app.sh` executable.

- [ ] **Step 5: Commit**

```bash
git add boot-start.sh start_app.sh test_login.sh
git commit -m "docs: prepare boot startup install flow"
```

## Self-Review

- Spec coverage: startup order, MySQL readiness, Redis readiness, backend health, frontend health, and repeated execution safety are each covered by Tasks 1-4.
- Placeholder scan: no `TODO`, `TBD`, or vague “handle appropriately” language remains.
- Type consistency: all tasks consistently use `boot-start.sh`, `start_app.sh`, MySQL `43306`, Redis `46379`, backend `18080`, and frontend `7860`.
