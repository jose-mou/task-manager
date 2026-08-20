-- specs/service-registry-and-task-scoping.md: tasks.service (free text)
-- becomes a NOT NULL service_id FK. The database holds no data at this point
-- in the project, so the column is replaced outright instead of migrating
-- existing rows.
ALTER TABLE tasks DROP COLUMN service;

ALTER TABLE tasks
    ADD COLUMN service_id UUID NOT NULL REFERENCES services (id) ON DELETE CASCADE;

CREATE INDEX ix_tasks_service_id ON tasks (service_id);

-- name stays unique case-insensitively, but now scoped per owning service
-- instead of globally (rule 10).
DROP INDEX ux_tasks_name_ci;

CREATE UNIQUE INDEX ux_tasks_service_id_name_ci ON tasks (service_id, LOWER(name));
