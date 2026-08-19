# task-manager

## Claude Code session

Launch Claude Code for this project with a bare, project-only configuration (no global user commands/agents, keeping authentication):

```bash
CLAUDE_CONFIG_DIR="$HOME/.claude-bare" claude --strict-mcp-config
```

## Technology stack

**Backend** (`backend/`):
- Java 25, Spring Boot 4.x
- Gradle as build tool
- PostgreSQL as database
- Docker + Docker Compose for local development (app + database)

**Frontend** (`frontend/`):
- TypeScript + React

All specs, implementations and reviews must target this stack. Do not introduce other languages, frameworks or databases without an approved spec that justifies it.

## Development commands

- JDK 25 is managed with SDKMAN: `source ~/.sdkman/bin/sdkman-init.sh` (or set `JAVA_HOME=~/.sdkman/candidates/java/current`).
- Backend tests/build: `cd backend && ./gradlew test` / `./gradlew build`. Tests use Testcontainers (real PostgreSQL in Docker); no local database needed.
- Docker runs on Colima; the Testcontainers socket override is already configured in `backend/build.gradle`.
- Frontend: `cd frontend && npm run dev` (dev server), `npm run build` (production build).
- Full local stack: `docker compose up --build` → frontend on :3000 (nginx proxies `/api/` to the backend), backend on :8080, PostgreSQL on :5432.
- Dev database only: `docker compose up postgres` (backend defaults point to `localhost:5432`, db/user/password `taskmanager`).

## Development methodology

This project works with **Spec Driven Development (SDD)** and **double-loop TDD** (ATDD). All feature work runs through the `/sdd <feature description>` pipeline:

1. **Spec** (`spec-writer`, opus) — concise spec (hard cap 80 lines) under `specs/`. Nothing else happens until the user approves it (`Status: APPROVED`).
2. **API contract** (`api-designer`, opus) — the spec's API impact becomes `api/openapi.yaml`. The contract is then FROZEN; it is what allows backend and frontend to proceed in parallel. Mock it with `npx @stoplight/prism-cli mock api/openapi.yaml`.
3. **Red acceptance tests** (`acceptance-tester`, sonnet) — failing API-level acceptance tests written from spec + contract (outer TDD loop), before any production code.
4. **Implementation** (`backend-implementer` ∥ `frontend-implementer`, sonnet) — parallel, strict inner-loop TDD, each confined to its own directory, coding against the frozen contract until acceptance tests pass. Frontend tests mock HTTP with MSW per the contract.
5. **Review** (`reviewer` ×2 in parallel, opus) — per-side review against spec and contract; corrections applied with tests kept green.
6. **E2E verification + PR** (`e2e-verifier`, sonnet) — full suites, real stack via docker compose, one Playwright smoke test on the happy path, then the PR. On failure, exactly one bounded correction round is routed back to the failing side; a second failure escalates to the user.

Rules:
- Specs live in `specs/` (English, ≤80 lines, one file per feature). Every acceptance criterion maps to an automated test.
- The pipeline degrades: phases a spec does not need (no API impact, no UI impact) are skipped — no ceremony without value.
- The orchestrator owns the `feat/<spec-name>` branch and all commits; sub-agents never run git (only `e2e-verifier` pushes and opens the PR).
- Acceptance tests and the OpenAPI contract are never weakened to make code pass; conflicts escalate as blocking questions.
- Conventional commits only; never add Co-Authored-By or AI attribution.
- Do not merge to `main` directly; all changes land via PR from the pipeline.
