# Database Architecture

> Status: `identity_db` is implemented (Flyway migrations in `backend/java-services/identity-service`). Other schemas below are the **logical design** that Phase 3 turns into migrations, one service at a time.

## 1. Stores

| Store | Used by | Why |
| --- | --- | --- |
| PostgreSQL 16 | All transactional services (one database each) | ACID, constraints, JSONB, RLS, pgvector |
| MongoDB 7 | audit-service, ai-orchestrator (conversations, prompts) | Append-heavy, flexible documents |
| Redis 7 | gateway rate limits, config cache, sessions/presence, idempotency keys | Low-latency ephemeral state |
| Vector DB (pgvector → Qdrant) | rag-service | Embedding similarity search |
| S3 / MinIO | document-service | Binary files and versions |

## 2. Conventions (all PostgreSQL schemas)

| Rule | Example |
| --- | --- |
| PK `id uuid` (app-generated) | `id uuid PRIMARY KEY` |
| Tenant column on tenant-owned tables | `tenant_id uuid NOT NULL` + index leading with `tenant_id` |
| Audit columns | `created_at timestamptz NOT NULL DEFAULT now()`, `updated_at`, `created_by uuid` |
| Optimistic lock | `version bigint NOT NULL DEFAULT 0` |
| Soft delete | `deleted_at timestamptz NULL`; unique indexes are partial `WHERE deleted_at IS NULL` |
| Enums | `varchar(32)` + `CHECK` constraint (easy to evolve; no PG enum migrations) |
| Money | `numeric(19,4)` + `currency char(3)` |
| Names | snake_case, plural tables, `fk_<table>_<ref>`, `ix_<table>_<cols>`, `uq_<table>_<cols>` |
| Outbox | every publishing service has `outbox_events` |
| Idempotent consumers | every consuming service has `processed_events(event_id PK, processed_at)` |
| Migrations | Flyway `V<n>__<description>.sql`, forward-only; dev seed data in `db/seed` enabled only by the `dev` profile |

## 3. identity_db (implemented)

```mermaid
erDiagram
    tenants ||--o{ user_accounts : contains
    tenants ||--o{ roles : "custom roles (tenant_id null = system)"
    roles ||--o{ role_permissions : grants
    permissions ||--o{ role_permissions : ""
    user_accounts ||--o{ user_roles : has
    roles ||--o{ user_roles : ""
    user_accounts ||--o{ refresh_tokens : owns
    user_accounts ||--o{ auth_audit_log : "subject of"
    tenants {
        uuid id PK
        varchar slug UK
        varchar name
        varchar status
        timestamptz created_at
    }
    user_accounts {
        uuid id PK
        uuid tenant_id FK
        varchar email
        varchar email_normalized
        varchar password_hash
        varchar display_name
        varchar status
        int failed_login_attempts
        timestamptz locked_until
        timestamptz last_login_at
        bigint version
    }
    roles {
        uuid id PK
        uuid tenant_id FK "nullable"
        varchar code
        varchar name
    }
    permissions {
        varchar code PK
        varchar description
    }
    refresh_tokens {
        uuid id PK
        uuid user_id FK
        uuid tenant_id
        uuid family_id
        char token_hash UK
        timestamptz expires_at
        timestamptz revoked_at
        varchar revoked_reason
        uuid replaced_by_id
    }
    auth_audit_log {
        bigint id PK
        uuid tenant_id
        uuid user_id
        varchar event_type
        varchar ip_address
        jsonb details
        timestamptz occurred_at
    }
```

Plus `outbox_events` (events awaiting the Phase 10 relay).

## 4. Logical schemas for remaining services

### organization_db

```mermaid
erDiagram
    organizations ||--o{ departments : has
    departments ||--o{ departments : "parent"
    organizations ||--o{ locations : has
    organizations { uuid id PK
        uuid tenant_id
        varchar name
        varchar legal_name
        varchar tax_id
        char country }
    departments { uuid id PK
        uuid tenant_id
        uuid organization_id FK
        uuid parent_id FK
        varchar code
        varchar name
        uuid head_employee_id }
    locations { uuid id PK
        uuid tenant_id
        uuid organization_id FK
        varchar name
        jsonb address
        varchar timezone }
```

### employee_db

```mermaid
erDiagram
    employees ||--o{ employment_contracts : has
    employees ||--o{ leave_requests : submits
    positions ||--o{ employees : holds
    employees { uuid id PK
        uuid tenant_id
        uuid user_id "nullable link to identity"
        varchar employee_no
        varchar first_name
        varchar last_name
        uuid department_id
        uuid position_id FK
        uuid manager_id FK
        date hire_date
        varchar status }
    positions { uuid id PK
        uuid tenant_id
        varchar title
        varchar grade }
    employment_contracts { uuid id PK
        uuid employee_id FK
        varchar type
        date start_date
        date end_date
        numeric salary
        char currency }
    leave_requests { uuid id PK
        uuid employee_id FK
        varchar type
        date from_date
        date to_date
        varchar status }
```

### customer_db (CRM)

```mermaid
erDiagram
    accounts ||--o{ contacts : has
    accounts ||--o{ opportunities : has
    leads }o--|| accounts : "converted to"
    accounts ||--o{ activities : logs
    accounts { uuid id PK
        uuid tenant_id
        varchar name
        varchar industry
        varchar tier
        uuid owner_employee_id }
    contacts { uuid id PK
        uuid account_id FK
        varchar name
        varchar email
        varchar phone }
    leads { uuid id PK
        uuid tenant_id
        varchar source
        varchar status
        int score }
    opportunities { uuid id PK
        uuid account_id FK
        varchar stage
        numeric amount
        char currency
        date expected_close }
    activities { uuid id PK
        uuid account_id FK
        varchar type
        text notes
        timestamptz occurred_at }
```

### project_db / task_db

```mermaid
erDiagram
    projects ||--o{ milestones : has
    projects ||--o{ project_members : has
    projects ||--o{ project_risks : has
    tasks ||--o{ tasks : subtasks
    tasks ||--o{ task_comments : has
    tasks ||--o{ time_logs : has
    projects { uuid id PK
        uuid tenant_id
        varchar code
        varchar name
        varchar status
        date start_date
        date due_date
        numeric budget }
    milestones { uuid id PK
        uuid project_id FK
        varchar name
        date due_date
        varchar status }
    project_members { uuid project_id FK
        uuid user_id
        varchar project_role }
    project_risks { uuid id PK
        uuid project_id FK
        varchar source "MANUAL|AI"
        int probability
        int impact
        text mitigation }
    tasks { uuid id PK
        uuid tenant_id
        uuid project_id "ref only"
        uuid parent_id FK
        varchar title
        varchar status
        varchar priority
        uuid assignee_id
        timestamptz due_at }
    task_comments { uuid id PK
        uuid task_id FK
        uuid author_id
        text body }
    time_logs { uuid id PK
        uuid task_id FK
        uuid user_id
        int minutes
        date work_date }
```

### order_db / inventory_db / vendor_db / finance_db

```mermaid
erDiagram
    orders ||--o{ order_lines : contains
    products ||--o{ stock_levels : "per warehouse"
    warehouses ||--o{ stock_levels : holds
    products ||--o{ stock_movements : moves
    stock_reservations }o--|| products : reserves
    vendors ||--o{ purchase_orders : receives
    purchase_orders ||--o{ purchase_order_lines : contains
    purchase_orders ||--o{ goods_receipts : fulfilled_by
    invoices ||--o{ invoice_lines : contains
    invoices ||--o{ payments : settled_by
    accounts_chart ||--o{ journal_lines : posts
    journal_entries ||--o{ journal_lines : contains
    orders { uuid id PK
        uuid tenant_id
        varchar order_no
        uuid customer_account_id
        varchar status
        numeric total
        char currency }
    order_lines { uuid id PK
        uuid order_id FK
        uuid product_id
        int quantity
        numeric unit_price }
    products { uuid id PK
        uuid tenant_id
        varchar sku
        varchar name
        numeric list_price }
    warehouses { uuid id PK
        uuid tenant_id
        varchar code }
    stock_levels { uuid product_id FK
        uuid warehouse_id FK
        int on_hand
        int reserved
        int reorder_point }
    stock_movements { uuid id PK
        uuid product_id FK
        varchar type
        int quantity
        varchar reference }
    stock_reservations { uuid id PK
        uuid order_id
        uuid product_id FK
        int quantity
        varchar status }
    vendors { uuid id PK
        uuid tenant_id
        varchar name
        numeric rating }
    purchase_orders { uuid id PK
        uuid vendor_id FK
        varchar status
        numeric total }
    purchase_order_lines { uuid id PK
        uuid purchase_order_id FK
        uuid product_id
        int quantity }
    goods_receipts { uuid id PK
        uuid purchase_order_id FK
        timestamptz received_at }
    invoices { uuid id PK
        uuid tenant_id
        varchar invoice_no
        uuid source_order_id
        varchar status
        numeric total
        date due_date }
    invoice_lines { uuid id PK
        uuid invoice_id FK
        varchar description
        numeric amount
        numeric tax }
    payments { uuid id PK
        uuid invoice_id FK
        numeric amount
        varchar method
        timestamptz paid_at }
    accounts_chart { uuid id PK
        uuid tenant_id
        varchar code
        varchar type }
    journal_entries { uuid id PK
        uuid tenant_id
        date entry_date
        varchar reference }
    journal_lines { uuid id PK
        uuid entry_id FK
        uuid account_id FK
        numeric debit
        numeric credit }
```

Finance invariant enforced by a deferred constraint trigger: `SUM(debit) = SUM(credit)` per journal entry.

### Other services (summary)

| Database | Core tables |
| --- | --- |
| workflow_db | `workflow_definitions(versioned JSON)`, `workflow_instances`, `workflow_steps`, `approvals`, `sla_timers` |
| document_db | `folders`, `documents`, `document_versions(storage_key, checksum, size)`, `document_shares`, `retention_labels` |
| reporting_db | `report_definitions`, `report_runs`, `kpi_daily(tenant_id, metric, date, value)`, projection tables |
| notification_db | `templates`, `notifications`, `delivery_attempts`, `user_channel_preferences` |
| config_db | `tenant_settings(key, value jsonb)`, `feature_flags`, `system_parameters` |
| integration (Mongo/PG) | `connectors`, `webhook_subscriptions(secret_hash)`, `webhook_deliveries` |
| audit (MongoDB) | `audit_events` — immutable, `{eventId, tenantId, actor, action, resource, before, after, ip, at}`; TTL per tenant policy |
| ai (MongoDB) | `conversations`, `messages`, `prompt_templates`, `feedback`, `eval_runs` |
| vectors | `chunks(id, tenant_id, document_id, version, page, acl[], embedding vector(1536), text)` with HNSW index |

## 5. Indexing strategy

- Every tenant-owned query path has a composite index **leading with `tenant_id`** (e.g. `ix_tasks_tenant_assignee_status (tenant_id, assignee_id, status)`).
- Partial unique indexes for soft-deleted rows.
- `GIN` on JSONB attributes that are filtered; `pg_trgm` GIN for fuzzy name search.
- Time-series tables (`auth_audit_log`, `stock_movements`, `kpi_daily`) are range-partitioned by month once they exceed ~50 M rows.

## 6. Data retention policies

| Data | Retention | Mechanism |
| --- | --- | --- |
| Refresh tokens (expired/revoked) | 30 days after expiry | Scheduled purge job in identity-service |
| Auth audit log | 1 year hot, 7 years archived (S3 Glacier) | Monthly partitions detached → export |
| Business audit events (Mongo) | Tenant-configurable, default 7 years | TTL index + archive export |
| Soft-deleted business records | 90 days, then hard delete or anonymise | Retention job per service |
| Outbox rows | 7 days after publish | Relay cleanup |
| AI conversations | 180 days default, tenant-configurable; PII redacted in logs | TTL index |
| Uploaded documents | Per retention label (e.g. contracts 10 years) | document-service lifecycle + S3 lifecycle rules |
| Application logs (ELK) | 30 days hot, 90 days warm | ILM policy |

GDPR "right to erasure": each service exposes an internal `ErasureRequested` consumer that anonymises personal data for a user ID while preserving financial records required by law.
