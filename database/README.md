# database/

| Folder | Contents |
| --- | --- |
| `schemas/` | Cluster-level bootstrap (`00-create-databases.sql` creates one database per service) and, from Phase 3, read-only DDL snapshots per service for review |
| `migrations/` | Cross-service operational scripts (analytics views for the NL→SQL assistant, RLS policies) — Phase 3/9 |
| `seed-data/` | Larger demo datasets for portfolio demos — Phase 3 |

**Service schemas are owned by the services.** Each Java service keeps its Flyway migrations in
`backend/java-services/<service>/src/main/resources/db/migration` and applies them on start-up.
Development-only seed data lives in `db/seed` and is loaded only with the `dev` Spring profile.

Implemented so far: `identity_db` — see `backend/java-services/identity-service` and
[docs/database/database-architecture.md](../docs/database/database-architecture.md).
