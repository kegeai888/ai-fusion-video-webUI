# 2026-04-26 Boot Startup Design

## Goal
Enable the full application stack to start automatically at boot on this host by placing one executable shell script under `/start.d`.

## Constraints
- This environment treats `/start.d` as the boot-time startup hook.
- Services should be started through shell scripts, not systemd.
- The stack includes MySQL, Redis, backend API, and frontend web on port 7860.
- The solution should fit the current repository structure and reuse existing startup logic where practical.

## Recommended Approach
Use one boot entry script under `/start.d` whose only responsibility is to ensure the whole stack is running.

The script should:
1. Confirm required runtime prerequisites are available.
2. Start infrastructure dependencies first.
3. Wait for MySQL and Redis readiness.
4. Start backend and frontend through the existing repository startup flow.
5. Run lightweight health checks for backend and frontend.
6. Exit successfully if services are already running instead of failing on repeated execution.

## Startup Flow
### 1. Infrastructure startup
The boot script should start the dependency layer first. If MySQL and Redis are already managed through Docker in this environment, the script should invoke the existing Docker Compose command that starts those services. If they are already up, this step should be a no-op.

### 2. Dependency readiness
After starting infrastructure, the script should wait until:
- MySQL accepts connections on its configured local port.
- Redis responds on its configured local port.

This avoids starting Spring Boot before Flyway can connect.

### 3. Application startup
After dependencies are ready, the script should call the repository’s existing startup entrypoint, currently `start_app.sh`, so the host-specific process cleanup and startup behavior remain in one place.

### 4. Health checks
After application startup, the script should verify:
- Backend: `http://localhost:18080/api/swagger-ui.html`
- Frontend: local access on port `7860`

The script should log a clear success or failure message for each check.

## Script Boundaries
The new boot script should not duplicate the full backend/frontend startup logic that already exists in `start_app.sh` unless current behavior makes reuse impossible. It should act as a thin orchestration layer for boot-time execution.

## Idempotency Requirements
The script must be safe to run more than once.
- Starting already-running containers must not be treated as an error.
- Calling the application startup path repeatedly must either reuse existing cleanup logic or skip duplicate launches cleanly.
- Failures should be explicit in logs rather than silent.

## Logging
The script should print short, readable status lines for each phase:
- starting dependencies
- waiting for MySQL
- waiting for Redis
- starting application
- checking backend
- checking frontend

If repository logging already writes to `backend.log` and `frontend.log`, this design should preserve that behavior.

## Testing Strategy
Because this is boot orchestration, validation should focus on executable behavior rather than unit tests.

Required verification:
1. Dependencies are started when not already running.
2. Running the boot script a second time does not fail because of existing services.
3. Backend health check passes.
4. Frontend port 7860 responds.

This validation can be implemented as script-level checks and manual execution during verification.

## Out of Scope
- Migrating startup to systemd.
- Re-architecting the stack into a new container topology.
- Changing frontend/backend application behavior unrelated to startup.
- Adding unrelated deployment features.
