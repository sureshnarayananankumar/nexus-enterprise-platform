-- =====================================================================
-- identity-service schema (identity_db)
-- Conventions: docs/database/database-architecture.md
-- =====================================================================

CREATE TABLE tenants (
    id          uuid         PRIMARY KEY,
    slug        varchar(63)  NOT NULL,
    name        varchar(200) NOT NULL,
    status      varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    version     bigint       NOT NULL DEFAULT 0,
    CONSTRAINT uq_tenants_slug UNIQUE (slug),
    CONSTRAINT ck_tenants_slug CHECK (slug ~ '^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$'),
    CONSTRAINT ck_tenants_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE TABLE permissions (
    code        varchar(100) PRIMARY KEY,
    description varchar(255) NOT NULL,
    CONSTRAINT ck_permissions_code CHECK (code ~ '^[a-z]+\.[a-z-]+\.[a-z-]+$')
);

-- tenant_id NULL = system role shipped with the platform; otherwise a tenant-defined custom role
CREATE TABLE roles (
    id          uuid         PRIMARY KEY,
    tenant_id   uuid         NULL REFERENCES tenants (id) ON DELETE CASCADE,
    code        varchar(50)  NOT NULL,
    name        varchar(100) NOT NULL,
    description varchar(255),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_roles_code CHECK (code ~ '^[A-Z][A-Z0-9_]{1,49}$')
);
CREATE UNIQUE INDEX uq_roles_system_code ON roles (code) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX uq_roles_tenant_code ON roles (tenant_id, code) WHERE tenant_id IS NOT NULL;

CREATE TABLE role_permissions (
    role_id         uuid         NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_code varchar(100) NOT NULL REFERENCES permissions (code) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_code)
);

CREATE TABLE user_accounts (
    id                    uuid         PRIMARY KEY,
    tenant_id             uuid         NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    email                 varchar(254) NOT NULL,
    email_normalized      varchar(254) NOT NULL,
    password_hash         varchar(255) NOT NULL,
    display_name          varchar(120) NOT NULL,
    status                varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts integer      NOT NULL DEFAULT 0,
    locked_until          timestamptz,
    last_login_at         timestamptz,
    created_at            timestamptz  NOT NULL DEFAULT now(),
    updated_at            timestamptz  NOT NULL DEFAULT now(),
    version               bigint       NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_accounts_tenant_email UNIQUE (tenant_id, email_normalized),
    CONSTRAINT ck_user_accounts_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_user_accounts_attempts CHECK (failed_login_attempts >= 0)
);
CREATE INDEX ix_user_accounts_tenant_created ON user_accounts (tenant_id, created_at DESC);

CREATE TABLE user_roles (
    user_id     uuid        NOT NULL REFERENCES user_accounts (id) ON DELETE CASCADE,
    role_id     uuid        NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX ix_user_roles_role ON user_roles (role_id);

-- Only the SHA-256 hash of the opaque refresh token is stored.
CREATE TABLE refresh_tokens (
    id             uuid         PRIMARY KEY,
    user_id        uuid         NOT NULL REFERENCES user_accounts (id) ON DELETE CASCADE,
    tenant_id      uuid         NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    family_id      uuid         NOT NULL,
    token_hash     varchar(64)  NOT NULL,
    issued_at      timestamptz  NOT NULL,
    expires_at     timestamptz  NOT NULL,
    revoked_at     timestamptz,
    revoked_reason varchar(32),
    replaced_by_id uuid,
    created_ip     varchar(45),
    user_agent     varchar(255),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_refresh_tokens_reason CHECK (revoked_reason IS NULL OR revoked_reason IN
        ('ROTATED', 'LOGOUT', 'REUSE_DETECTED', 'ACCOUNT_DISABLED')),
    CONSTRAINT ck_refresh_tokens_expiry CHECK (expires_at > issued_at)
);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_expires ON refresh_tokens (expires_at);

CREATE TABLE auth_audit_log (
    id          bigserial    PRIMARY KEY,
    tenant_id   uuid,
    user_id     uuid,
    event_type  varchar(40)  NOT NULL,
    ip_address  varchar(45),
    user_agent  varchar(255),
    details     jsonb,
    occurred_at timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX ix_auth_audit_tenant_time ON auth_audit_log (tenant_id, occurred_at DESC);
CREATE INDEX ix_auth_audit_user_time ON auth_audit_log (user_id, occurred_at DESC);

-- Transactional outbox; published to Kafka by the relay introduced in Phase 10.
CREATE TABLE outbox_events (
    id             uuid         PRIMARY KEY,
    tenant_id      uuid,
    aggregate_type varchar(60)  NOT NULL,
    aggregate_id   varchar(64)  NOT NULL,
    event_type     varchar(80)  NOT NULL,
    event_version  integer      NOT NULL DEFAULT 1,
    payload        jsonb        NOT NULL,
    correlation_id varchar(64),
    occurred_at    timestamptz  NOT NULL DEFAULT now(),
    published_at   timestamptz
);
CREATE INDEX ix_outbox_unpublished ON outbox_events (occurred_at) WHERE published_at IS NULL;
