-- specs/service-registry-and-task-scoping.md: tasks.service (free text)
-- becomes a NOT NULL service_id FK. No environment holds task data worth
-- keeping, and a free-text service name cannot be mapped to a registered
-- service, so any pre-existing row is dropped before ownership becomes
-- mandatory. Without this, the NOT NULL column below fails on a database that
-- already ran V1 (a fresh Testcontainers instance never hits that path).
DELETE FROM tasks;

ALTER TABLE tasks DROP COLUMN service;

ALTER TABLE tasks
    ADD COLUMN service_id UUID NOT NULL REFERENCES services (id) ON DELETE CASCADE;

CREATE INDEX ix_tasks_service_id ON tasks (service_id);

-- name stays unique case-insensitively, but now scoped per owning service
-- instead of globally (rule 10).
DROP INDEX ux_tasks_name_ci;

CREATE UNIQUE INDEX ux_tasks_service_id_name_ci ON tasks (service_id, LOWER(name));
