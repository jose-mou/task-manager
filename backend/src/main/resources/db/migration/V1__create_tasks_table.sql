-- The contract (api/openapi.yaml) puts no maximum length on name, service,
-- script or cronExpr, so they are stored as TEXT: an arbitrary-length value is a
-- valid request and must not fail as a database error.
CREATE TABLE tasks (
    id                 UUID PRIMARY KEY,
    name               TEXT                     NOT NULL,
    creation_date      TIMESTAMP WITH TIME ZONE NOT NULL,
    modification_date  TIMESTAMP WITH TIME ZONE NOT NULL,
    service            TEXT                     NOT NULL,
    description        TEXT,
    status             VARCHAR(20)              NOT NULL,
    script             TEXT                     NOT NULL,
    cron_expr          TEXT,
    max_executions     INTEGER,
    scheduled          BOOLEAN                  NOT NULL DEFAULT FALSE
);

-- Case-insensitive uniqueness on name, enforced at the database level as the
-- final safety net behind the application-level pre-check.
CREATE UNIQUE INDEX ux_tasks_name_ci ON tasks (LOWER(name));

CREATE INDEX ix_tasks_creation_date ON tasks (creation_date);
