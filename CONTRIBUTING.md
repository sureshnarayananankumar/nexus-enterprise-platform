# Contributing to NEXUS Enterprise

## Workflow

1. Check [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md) and the [roadmap](docs/architecture/06-roadmap.md).
2. Before starting a new module, add a **"Dependencies and integration"** section to its README: what it calls, what it publishes/consumes, which permissions it defines.
3. Branch from `main`: `feat/<service>-<topic>`, `fix/...`, `docs/...`.
4. Keep changes PR-sized. CI must be green before merge.
5. Update `DEVELOPMENT_STATUS.md` in the same PR. A feature is only "done" when it is implemented **and** covered by passing tests.

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/): `feat(identity): rotate refresh tokens`, `fix(gateway): ...`, `docs: ...`, `test: ...`, `chore(ci): ...`.

## Code standards

| Area | Rules |
| --- | --- |
| Java | Java 21, Spring Boot 3; layers `api / application / domain / infrastructure`; constructor injection; no field injection; records for DTOs; Bean Validation on every request DTO; errors via `ApiException` + `platform-common` |
| Data | Flyway forward-only migrations; never edit an applied migration; every tenant-owned table has `tenant_id` and an index leading with it |
| Security | Tenant from JWT only; permission checks with `@PreAuthorize("hasAuthority('<service>.<resource>.<action>')")`; no secrets in code, logs or tests |
| Tests | Unit tests for domain rules; Testcontainers integration tests for repositories and APIs; at least one cross-tenant isolation test per service |
| API | `/api/v1/...`, RFC 9457 problem details, pagination limits, OpenAPI annotations |

## Running checks locally

```bash
cd backend/java-services && mvn verify      # requires Docker (Testcontainers)
docker compose config -q                     # from repo root
```
