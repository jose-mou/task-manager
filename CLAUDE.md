# task-manager

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

This project works with **Spec Driven Development (SDD)** and **strict TDD**. All feature work follows the three-phase pipeline, started with the `/sdd <feature description>` command:

1. **Specification** — the `spec-writer` agent turns the user prompt into a simple spec under `specs/`. Nothing is implemented until the user approves the spec (`Status: APPROVED`).
2. **Implementation** — the `implementer` agent implements the approved spec on a `feat/<spec-name>` branch using strict TDD (red → green → refactor; no production code without a failing test first).
3. **Review + PR** — the `reviewer` agent reviews the implementation against the spec, applies corrections keeping tests green, and opens a PR to `main` with `gh`.

Rules:
- Specs live in `specs/`, written in English, one file per feature.
- Every acceptance criterion in a spec must map to an automated test.
- Conventional commits only; never add Co-Authored-By or AI attribution.
- Do not merge to `main` directly; all changes land via PR from the pipeline.
