CREATE TABLE tasks (
    id                 UUID PRIMARY KEY,
    name               VARCHAR(255)             NOT NULL,
    creation_date      TIMESTAMP WITH TIME ZONE NOT NULL,
    modification_date  TIMESTAMP WITH TIME ZONE NOT NULL,
    service            VARCHAR(255)             NOT NULL,
    description        TEXT,
    status             VARCHAR(20)              NOT NULL,
    script             VARCHAR(1024)            NOT NULL,
    cron_expr          VARCHAR(100),
    max_executions     INTEGER,
    scheduled          BOOLEAN                  NOT NULL DEFAULT FALSE
);

-- Case-insensitive uniqueness on name, enforced at the database level as the
-- final safety net behind the application-level pre-check.
CREATE UNIQUE INDEX ux_tasks_name_ci ON tasks (LOWER(name));

CREATE INDEX ix_tasks_creation_date ON tasks (creation_date);
