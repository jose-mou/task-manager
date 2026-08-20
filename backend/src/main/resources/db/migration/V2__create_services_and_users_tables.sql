-- Service registry: machine identities that own tasks and authenticate with
-- generated API credentials (specs/service-registry-and-task-scoping.md).
-- apiKey is stored as-is (it is the public HTTP Basic username, looked up
-- directly on every request); apiSecret is never stored, only a hex-encoded
-- SHA-256 hash of it, verified with a constant-time comparison at the
-- application layer.
CREATE TABLE services (
    id                 UUID PRIMARY KEY,
    name               TEXT                     NOT NULL,
    api_key            TEXT                     NOT NULL,
    api_secret_hash    TEXT                     NOT NULL,
    creation_date      TIMESTAMP WITH TIME ZONE NOT NULL,
    modification_date  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- A service name identifies exactly one service (it is how tasks and the
-- ADMIN-facing API reference a service), so it must be unique.
CREATE UNIQUE INDEX ux_services_name_ci ON services (LOWER(name));

-- apiKey is the HTTP Basic username: it must resolve to exactly one service.
CREATE UNIQUE INDEX ux_services_api_key ON services (api_key);

-- UI users, independent from services. Column names match the fixture
-- expectations of TaskAnonymousAndRoleAccessAcceptanceTest, which seeds a
-- USER-role row directly (there is no user-creation endpoint - user
-- self-registration is explicitly out of scope of the spec).
CREATE TABLE users (
    id                 UUID PRIMARY KEY,
    username           TEXT                     NOT NULL,
    password_hash      TEXT                     NOT NULL,
    role               VARCHAR(20)              NOT NULL,
    creation_date      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX ux_users_username_ci ON users (LOWER(username));
