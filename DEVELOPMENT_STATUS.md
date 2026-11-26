# Development Status

_Last updated: 2026-11-26_

Legend: ✅ done (implemented **and** tested in CI) · 🟡 in progress · ⏳ not started · 📐 designed (docs only)

## Phase overview

| Phase | Status | Notes |
| --- | --- | --- |
| 1. Architecture & requirements | ✅ | `docs/architecture/01–06`, ADRs, database architecture |
| 2. Repository structure | ✅ | Monorepo, compose, Java parent build, CI workflow — [CI run #3 green](https://github.com/sureshnarayananankumar/nexus-enterprise-platform/actions/runs/45775491141) |
| 3. Database architecture | 🟡 | Logical design for all services; `identity_db` migrated with Flyway. Other services' migrations pending |
| 4. Authentication & security | 🟡 | Core token model in identity-service (see below). OIDC federation, client credentials, RLS pending |
| 5. Java microservices | 🟡 | identity-service + platform-common. 16 services remaining |
| 6. Node.js services | ⏳ | |
| 7. Angular admin portal | ⏳ | |
| 8. React business portal | ⏳ | |
| 9. Generative AI services | 📐 | Design in `05-ai-architecture.md` |
| 10. Kafka event architecture | 📐 | Outbox table written by identity-service; relay pending |
| 11. Docker & Kubernetes | 🟡 | Generic Java Dockerfile + compose; Kubernetes manifests pending |
| 12. CI/CD | 🟡 | CI (build, tests, image build, compose validation); CD pending |
| 13. Automated testing | 🟡 | Unit + integration in identity-service; E2E/perf/security suites pending |
| 14. AWS deployment | ⏳ | |
| 15. Production readiness | ⏳ | |

## Component status

| Component | Status | Evidence |
| --- | --- | --- |
| platform-common (RFC 9457 errors, correlation ID filter, event envelope) | ✅ | `CorrelationIdFilterTest` |
| identity-service: tenant sign-up | ✅ | `AuthFlowIT` |
| identity-service: login, lockout, uniform errors | ✅ | `AuthFlowIT`, `UserAccountTest` |
| identity-service: RS256 access tokens + JWKS | ✅ | `AuthFlowIT.jwks…`, `tamperedTokenIsRejected` |
| identity-service: refresh rotation + reuse detection + logout | ✅ | `AuthFlowIT.refreshRotates…`, `logoutRevokes…` |
| identity-service: RBAC (permissions in JWT, @PreAuthorize) | ✅ | `RbacAndTenantIsolationIT` |
| identity-service: tenant isolation | ✅ | `RbacAndTenantIsolationIT.tenantsCannotSee…` |
| identity-service: account create/list/get/disable | ✅ | `RbacAndTenantIsolationIT` |
| identity-service: auth audit log + outbox rows | ✅ | `AuthFlowIT.signupWritesOutbox…` |
| identity-service: rate limiting (per instance) | ✅ limiter logic · 🟡 filter wiring not yet integration-tested | `FixedWindowRateLimiterTest` |
| identity-service: refresh-token retention purge | 🟡 implemented (scheduled job), not yet covered by a test | — |
| docker-compose (Postgres, MongoDB, Redis, Kafka, identity, Prometheus, Grafana) | ✅ config validated in CI · full stack not yet smoke-tested end-to-end | CI `compose` job |
| identity-service Docker image | ✅ builds in CI | CI `docker-images` job |

**Latest CI:** [run #3](https://github.com/sureshnarayananankumar/nexus-enterprise-platform/actions/runs/45775491141) — 27 tests, 0 failures, 0 skipped (platform-common 3 · identity unit 5 · `AuthFlowIT` 11 · `RbacAndTenantIsolationIT` 8), Docker image built, compose validated.

## Not yet implemented (identity scope)

- Custom role CRUD, password change/reset, email verification, MFA
- OIDC federation, client-credentials tokens, `jti` deny-list
- PostgreSQL Row-Level Security policies
- Outbox relay to Kafka

## Next steps

1. Add a compose smoke test to CI (start stack, log in through identity-service).
2. Phase 3: write Flyway migrations for organization-service and employee-service from `docs/database/database-architecture.md`.
3. Phase 5: implement organization-service (consumes `TenantCreated`) using identity-service as the template.

## Change log

| Date | Change |
| --- | --- |
| 2026-11-26 | Architecture package, monorepo initialisation, platform-common, identity-service with tests, compose, CI |
| 2026-11-26 | Testcontainers 1.21.4 + Docker API 1.44 pin (Docker Engine 29 on GitHub runners); first green CI; statuses verified |
