Status: APPROVED

# Task management (create, update, list)

## Goal
Let users register, edit and list tasks with their scheduling metadata, so the system has a persistent catalogue of tasks to build execution on later.

## Scope
- Task entity persisted in PostgreSQL with: id, name, creationDate, modificationDate, service, description, status, script, cronExpr, maxExecutions, scheduled.
- REST API for creating, reading, updating and listing tasks.
- Server-side validation of required fields, status enum, cron expression and maxExecutions.
- Web UI: task list plus a create/edit form.

Out of scope:
- The service registry endpoint: `service` is a forward reference to it, so referential validation of `service` values is deferred.
- Status transition rules: the lifecycle state machine is deferred, status is set freely by the client.
- Deleting tasks.
- Executing scripts, scheduler runtime, execution history/counters.
- Authentication, authorization and multi-tenancy.
- Pagination, filtering, sorting options and search.
- Partial updates (PATCH).

## Behavior
1. Creating a task returns 201 with a server-generated UUID `id` and `creationDate` equal to `modificationDate` (UTC, ISO-8601).
2. `name`, `service` and `script` are required and non-blank; `description` and `cronExpr` are optional.
3. `service` is stored as free text and accepted as-is; it is not checked against the (not yet existing) service registry.
4. Omitted optionals default to `scheduled=false`, `status=CREATED`, `maxExecutions=null` (unlimited).
5. `status` accepts only `CREATED`, `RUNNING`, `COMPLETED` or `CANCELED`; any other value is rejected with 400.
6. Any of the four statuses is accepted on create and update regardless of the current one; no transition is validated.
7. When `scheduled=true`, `cronExpr` is required and must be a valid Spring 6-field cron expression; otherwise 400.
8. `maxExecutions`, when present, must be an integer >= 1; otherwise 400.
9. Client-supplied `id`, `creationDate` and `modificationDate` are ignored on create and update; update keeps `creationDate` and sets `modificationDate` to the current instant, and an unknown id returns 404.
10. `name` is unique case-insensitively; a conflicting create or update returns 409. Validation failures return one 400 payload listing every offending field.

## Acceptance criteria
- Given a payload with only name, service and script, when POST /api/tasks, then 201 with a generated id, status CREATED, scheduled false and creationDate == modificationDate.
- Given a payload with a blank name and maxExecutions=0, when POST /api/tasks, then 400 and the error list names both `name` and `maxExecutions`.
- Given a payload with scheduled=true and a missing or syntactically invalid cronExpr, when POST /api/tasks, then 400 naming the `cronExpr` field.
- Given a payload with status "PAUSED", when POST /api/tasks, then 400 naming the `status` field.
- Given a stored task named "Backup", when POST /api/tasks with name "backup", then 409 and no task is created.
- Given a stored task with status CREATED, when PUT /api/tasks/{id} with status COMPLETED and changed fields, then 200, the fields are updated, creationDate is unchanged and modificationDate is strictly greater than before.
- Given an id that does not exist, when PUT /api/tasks/{id}, then 404.
- Given three stored tasks, when GET /api/tasks, then 200 with the three tasks ordered by creationDate descending.

## API impact
- POST /api/tasks — create a task
- GET /api/tasks — list all tasks, newest first
- GET /api/tasks/{id} — fetch a single task
- PUT /api/tasks/{id} — full update of a task

## UI impact
- Task list screen (`/tasks`): table with name, service, status, scheduled and modificationDate; links to create and edit.
- Task form screen (`/tasks/new`, `/tasks/:id/edit`): all editable fields, `service` as free text, `status` as a select of the four values, cronExpr and maxExecutions enabled only when `scheduled` is checked, inline field errors from the API response.
- App shell: routing and navigation entry to the task list, replacing the scaffold landing page.
