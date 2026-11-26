# Architecture Decision Records

Short-form ADRs. New decisions are appended; superseded ones are marked, never deleted.

## ADR-0001 — Monorepo

- **Status:** Accepted
- **Context:** ~25 deployables across Java, Node, Python, Angular, React; one developer/team; portfolio visibility matters.
- **Decision:** Single monorepo with path-filtered CI jobs per area.
- **Consequences:** Atomic cross-service changes and shared docs; CI must use path filters to stay fast.

## ADR-0002 — Database per service, shared PostgreSQL instance locally

- **Status:** Accepted
- **Decision:** Each service owns one logical database (`identity_db`, `order_db`, …) and its Flyway migrations. Locally one Postgres container hosts all databases; in AWS, RDS instances are grouped by criticality.
- **Consequences:** No cross-service joins; reporting uses event-fed read models.

## ADR-0003 — RS256 JWT with JWKS, opaque rotating refresh tokens

- **Status:** Accepted
- **Decision:** identity-service signs short-lived access tokens with RSA; resource servers verify using JWKS. Refresh tokens are random, stored hashed, rotated on every use, with family-based reuse detection.
- **Alternatives:** HS256 shared secret (rejected: every service could mint tokens); full Keycloak (deferred: heavier to operate, less learning value; may be adopted for federation later).

## ADR-0004 — Shared-schema multi-tenancy with `tenant_id`

- **Status:** Accepted
- **Decision:** Discriminator column + tenant from JWT; add PostgreSQL RLS in Phase 4 as defence in depth.
- **Consequences:** Cheapest to operate; requires discipline and tests on every repository method.

## ADR-0005 — Transactional outbox for events

- **Status:** Accepted
- **Decision:** Services write events to an `outbox_events` table in the same transaction as the state change; a relay publishes to Kafka.
- **Consequences:** No lost/phantom events; consumers must be idempotent.

## ADR-0006 — Permission-based authorisation

- **Status:** Accepted
- **Decision:** Endpoints check permission strings (`service.resource.action`), not role names. Roles are bundles of permissions, editable per tenant.

## ADR-0007 — Gateway in NestJS rather than Spring Cloud Gateway

- **Status:** Accepted
- **Decision:** The brief requires Node.js for the gateway, real-time and integrations; co-locating the GraphQL BFF and WebSocket auth there keeps edge concerns in one runtime.
- **Consequences:** Java services must not rely on gateway-only security (they re-validate tokens).

## ADR-0008 — pgvector first, Qdrant optional

- **Status:** Proposed
- **Decision:** Start RAG on PostgreSQL + pgvector (one fewer system, transactional metadata filters). Keep a `VectorStore` port so Qdrant can be swapped in when collection size or filtering performance requires it.
