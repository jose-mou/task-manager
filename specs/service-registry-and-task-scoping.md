Status: APPROVED

# Service registry, API credentials and UI authentication

## Goal
Give machines and humans separate identities: services authenticate with generated API credentials and may only write their own tasks, while ADMIN users authenticate with a JWT from the UI and may write tasks for any service. Reading tasks stays open to everyone.

## Scope
- Service entity in PostgreSQL: id, name, apiKey, apiSecretHash (SHA-256), creationDate, modificationDate.
- User entity, independent from services: id, username, passwordHash (BCrypt, kept slow on purpose for low-entropy human passwords), role (ADMIN | USER), creationDate.
- Generated machine credentials: `apiKey` + `apiSecret`, plus a rotation endpoint that replaces the pair.
- Service credentials are only ever transmitted over TLS.
- The `Authorization` header value is never written to application logs.
- JWT login endpoint for UI users (stateless HS256, 8-hour expiry, no refresh token) and a self-service password change.
- Bootstrap flow: the seeded ADMIN registers the services; from then on each service manages its own tasks with its API credentials.
- Spring Security added to `backend/build.gradle`, wiring both authentication filters and the authorization rules.
- Task ownership: `tasks.service` free text becomes a NOT NULL `service_id` FK; the schema is recreated from scratch (the database holds no data).
- Web UI: public read-only task views, user login, password change, admin-only service screens.

Out of scope:
- Roles beyond ADMIN and USER; per-service granular permissions; user self-registration and user CRUD endpoints.
- Password reset by e-mail, refresh tokens, token revocation lists, account lockout, rate limiting.
- Task deletion endpoint (tasks are removed only as a side effect of deleting their service).
- Status transition rules, scheduler runtime, pagination, sorting options.

## Behavior
1. `POST /api/services` requires an ADMIN JWT and returns 201 with id, name and a one-time `apiKey` + `apiSecret`, each 256 bits from a cryptographically secure RNG and base64url-encoded without padding; the store keeps only a hex-encoded SHA-256 hash of the secret, verified by constant-time comparison, and no later response ever exposes the secret.
2. `POST /api/services/{id}/credentials` returns a fresh pair and invalidates the previous one; it is allowed to an ADMIN JWT or to that same service's own credentials, and returns 403 for another service's credentials.
3. Services authenticate with HTTP Basic carrying `apiKey` as username and `apiSecret` as password; users authenticate with `Authorization: Bearer <JWT>` obtained from `POST /api/auth/login` (200 with token and expiry, 401 on wrong credentials).
4. On first startup, when the `users` table is empty, an ADMIN user `admin` / `admin` is seeded; both values are overridable through configuration properties (environment-injectable) and the defaults apply when unset.
5. `POST /api/users/me/password` lets any authenticated user change their own password: `currentPassword` must match (400 naming `currentPassword`) and `newPassword` must be at least 8 characters and different from the current one (400); success returns 204 and previously issued JWTs stay valid until they expire. No password change is forced at any point.
6. The `USER` role grants nothing beyond anonymous read in this feature; it exists so that a non-ADMIN token is explicitly rejected on writes.
7. `GET /api/tasks` and `GET /api/tasks/{id}` are anonymous and unrestricted: every task is readable whoever the owner is, credentials are ignored when present, and an unknown id returns 404. The list accepts an optional `?service=<name>` filter.
8. `POST /api/tasks` and `PUT /api/tasks/{id}` reject anonymous callers with 401 and USER-role tokens with 403.
9. With service credentials the owner is derived from the credentials, any `service` field in the payload is ignored, and updating a task owned by another service returns 403 with the task unchanged.
10. With an ADMIN JWT the payload's `service` field is required and must name a registered service (400 naming `service` otherwise); task `name` stays unique case-insensitively per owning service (409) and every other rule from `specs/task-management.md` is unchanged.

## Acceptance criteria
- Given an ADMIN JWT, when POST /api/services with a name, then 201 with non-empty apiKey and apiSecret, a subsequent GET /api/services returns the service without either value, and neither the captured log output nor any later response contains the raw apiSecret; given service A's credentials for the same call, then 403.
- Given a registered service, when POST /api/services/{id}/credentials with its own credentials, then 200 with a different pair and the previous secret is refused with 401 on the next task write.
- Given a freshly started system with an empty users table, when POST /api/auth/login with `admin` / `admin`, then 200 with a JWT.
- Given the seeded admin authenticated, when POST /api/users/me/password with the correct current password and a valid new one, then 204, a login with the old password returns 401 and a login with the new password returns 200 with a JWT.
- Given no Authorization header, when GET /api/tasks and GET /api/tasks/{id} for a task owned by any service, then 200 on both; when POST /api/tasks, then 401; and with a USER-role JWT, then 403.
- Given service A's credentials, when POST /api/tasks with a payload whose `service` says "B", then 201 and the created task's `service` is A.
- Given a task owned by service B, when service A's credentials PUT it, then 403 and the task is unchanged.
- Given an ADMIN JWT, when POST /api/tasks with `service` naming a registered service, then 201 owned by that service; when `service` names an unregistered one, then 400 naming `service`.

## API impact
- POST /api/auth/login — exchange username and password for a JWT (anonymous)
- POST /api/users/me/password — change the authenticated user's own password (any authenticated user)
- POST /api/services — register a service, returns one-time credentials (ADMIN)
- GET /api/services — list services, never their credentials (ADMIN)
- PUT /api/services/{id} — rename a service (ADMIN, or that service's own credentials)
- DELETE /api/services/{id} — delete a service and its tasks (ADMIN)
- POST /api/services/{id}/credentials — rotate credentials (ADMIN, or that service's own credentials)
- GET /api/tasks, GET /api/tasks/{id} — unchanged shape, now anonymous; list gains an optional `service` query filter
- POST /api/tasks, PUT /api/tasks/{id} — ADMIN JWT (payload `service` required) or service credentials (payload `service` ignored)

## UI impact
- Task list and task detail (`/tasks`, `/tasks/:id`): public, no login required; keep the `service` column and add a filter by service.
- Login screen (`/login`): username and password, stores the JWT in session storage and sends it as a Bearer header; 401/403 on writes redirect here.
- Change-password screen (`/account/password`): current and new password, reachable from the app shell by any logged-in user, never forced.
- Task form (`/tasks/new`, `/tasks/:id/edit`): ADMIN-only, `service` becomes a select fed by GET /api/services; create/edit actions are hidden for anonymous visitors.
- Admin services screen (`/admin/services`): ADMIN-only list with register, rename, delete and rotate-credentials actions.
- One-time credential dialog: shown once after register and after rotation, with a copy action and a warning that the secret cannot be retrieved again.
- App shell: login/logout entry, account menu with the password change, admin navigation visible only for ADMIN tokens.
