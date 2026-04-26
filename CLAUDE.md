# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
融光 (AI Fusion Video) — AI-driven video creation platform. Monorepo: Java/Spring Boot backend + Next.js frontend.

## Commands

### Backend (Java 21 + Maven)

### Frontend (Next.js 16 + pnpm)

### Docker (full stack)
Access at `http://localhost:8080`.

## Architecture

### Backend
- **Entry point**: `ai-fusion-video/src/main/java/com/stonewu/fusion/FusionVideoApplication.java`
- **Server**: port 18080, context path `/api`, Spring Boot 3.5.13, virtual threads enabled
- **Profiles**: default `local`, Docker uses `docker`
- **Swagger**: `http://localhost:18080/api/swagger-ui.html`
- **Database**: MySQL 8 (Flyway migrations at `db/migration/`), Redis 7 (token cache)
- **ORM**: MyBatis-Plus 3.5.12, soft delete (`deleted` field), auto-fill timestamps
- **Security**: Stateless JWT via `TokenAuthenticationFilter` (tokens stored in Redis). Public endpoints: `/auth/**`, `/system/init/**`, Swagger paths, `/media/**`, `/art-styles/**`. Password encoder: BCrypt.
- **AI Stack**: Spring AI (OpenAI, Anthropic, DashScope, Ollama, Vertex AI Gemini, Google GenAI) + AgentScope 1.0.11 + Anthropic official SDK
- **Storage**: AWS S3 SDK v2 (compatible with OSS/COS/MinIO), local fallback at `./data/media`

### Frontend
- **App Router** with route groups: `(auth)` — `/login`, `/setup`; `(dashboard)` — `/dashboard`, `/projects/[id]`, `/settings`, `/assets`
- **API client**: `lib/api/client.ts` — axios instance, auto Bearer token, automatic token refresh queue, redirects to `/login` on failure. Base URL from `NEXT_PUBLIC_API_BASE_URL` (default `http://localhost:18080/api`)
- **State**: Zustand stores at `lib/store/auth-store.ts` (persisted to localStorage) and `lib/store/pipeline-store.ts`
- **UI**: Tailwind CSS v4, shadcn/ui, Ant Design X, Radix UI, Framer Motion, @dnd-kit
- **No middleware.ts** — auth is client-side via zustand store
- **Config**: `next.config.ts` — standalone output, port 7860

## Configuration
| File | Purpose |
|---|---|
| `ai-fusion-video/src/main/resources/application.yaml` | Base: port, context, Flyway, MyBatis-Plus |
| `ai-fusion-video/src/main/resources/application-local.yaml` | Local dev: DB conn, Redis, DashScope key |
| `ai-fusion-video/src/main/resources/application-docker.yaml` | Docker: reads env vars for services |
| `.env.example` → `.env` | Docker Compose env template |
| `ai-fusion-video-web/.env.local` | Frontend: `NEXT_PUBLIC_API_BASE_URL` |
