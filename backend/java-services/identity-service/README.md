# identity-service

Authentication, tenants, RBAC and token issuance for NEXUS Enterprise. It is the **reference implementation** every other Java service copies (layout, error handling, tests, migrations).

## Dependencies and integration

| Direction | What | How |
| --- | --- | --- |
| Depends on | PostgreSQL `identity_db` | Flyway migrations in `src/main/resources/db/migration` |
| Depends on | `platform-common` | RFC 9457 errors, correlation IDs |
| Used by | api-gateway and every resource server | `GET /.well-known/jwks.json` to verify RS256 access tokens |
| Publishes (outbox) | `TenantCreated`, `UserRegistered`, `UserStatusChanged` | `outbox_events` → Kafka `nexus.identity.events` (relay arrives in Phase 10) |
| Consumed by (planned) | organization-service (`TenantCreated`), user-service & employee-service (`UserRegistered`), audit-service | Phase 5 |

## Endpoints

| Method | Path | Auth | Permission |
| --- | --- | --- | --- |
| POST | `/api/v1/tenants` | public (rate-limited) | — |
| POST | `/api/v1/auth/login` | public (rate-limited) | — |
| POST | `/api/v1/auth/refresh` | public (rate-limited) | — |
| POST | `/api/v1/auth/logout` | public | — |
| GET | `/api/v1/auth/me` | bearer | any |
| POST | `/api/v1/accounts` | bearer | `identity.user.create` |
| GET | `/api/v1/accounts?page&size` | bearer | `identity.user.read` |
| GET | `/api/v1/accounts/{id}` | bearer | `identity.user.read` |
| PUT | `/api/v1/accounts/{id}/status` | bearer | `identity.user.update` |
| GET | `/api/v1/roles` | bearer | `identity.role.read` |
| GET | `/.well-known/jwks.json` | public | — |
| GET | `/swagger-ui.html`, `/v3/api-docs` | public | — |
| GET | `/actuator/health`, `/actuator/prometheus` | public (internal network only in k8s) | — |

## Security behaviour

- Access token: RS256 JWT, 15 min, claims `sub, tid, email, name, roles, perms, jti`.
- Refresh token: 256-bit opaque, SHA-256 stored, 14 days, **rotated on every use**; replaying a rotated token revokes the whole login family (`REFRESH_TOKEN_REUSED`).
- Lockout: 5 bad passwords → 15 min lock. Unknown user, bad password, locked and disabled all return the same `INVALID_CREDENTIALS`.
- Disabling an account revokes all of its refresh tokens.
- In-memory fixed-window rate limit (default 20/min per IP and endpoint) on credential endpoints.
- Every query is tenant-scoped using `tid` from the verified token.

## Run locally

```bash
# from repo root: start Postgres (and the rest of the infra)
docker compose up -d postgres

cd backend/java-services
mvn -pl identity-service -am spring-boot:run -Dspring-boot.run.profiles=dev
# → http://localhost:8081/swagger-ui.html
```

The `dev` profile loads `db/seed/R__dev_seed.sql`: tenant `acme`, user `admin@acme.test` / `NexusAdmin2026!` (development only).

```bash
curl -s localhost:8081/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"tenant":"acme","email":"admin@acme.test","password":"NexusAdmin2026!"}'
```

## Configuration

| Variable | Default | Notes |
| --- | --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | local compose values | |
| `NEXUS_JWT_ISSUER` | `http://localhost:8081` | must match what resource servers expect |
| `NEXUS_JWT_PRIVATE_KEY` / `NEXUS_JWT_PUBLIC_KEY` | empty → ephemeral dev key | PKCS#8 / X.509 PEM |
| `NEXUS_JWT_KEY_ID` | `nexus-dev-key` | `kid` header and JWKS |
| `NEXUS_ACCESS_TOKEN_TTL` / `NEXUS_REFRESH_TOKEN_TTL` | `15m` / `14d` | |
| `NEXUS_AUTH_RATE_LIMIT` | `20` | requests per minute per IP per credential endpoint |
| `NEXUS_CORS_ORIGINS` | `http://localhost:4200,http://localhost:5173` | Angular / React dev servers |

Generate a key pair:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem
```

## Tests

```bash
mvn -pl identity-service -am verify   # needs Docker for Testcontainers
```

| Suite | Covers |
| --- | --- |
| `AuthFlowIT` | signup, login, `/me`, outbox + audit rows, validation problem details, uniform login errors, lockout, refresh rotation + reuse detection, logout, 401 handling, tampered tokens, JWKS |
| `RbacAndTenantIsolationIT` | permission checks (403), account creation, duplicate email / unknown role, cross-tenant 404, same email in two tenants, disabling revokes sessions, self-disable guard, roles listing, page-size limit |
| `UserAccountTest`, `FixedWindowRateLimiterTest` | domain lockout rules, email normalisation, token hashing, rate limiter windows |

## Known limitations (tracked in DEVELOPMENT_STATUS.md)

- Custom-role CRUD, password reset/change, MFA and OIDC federation are not built yet (Phase 4).
- The outbox is written but not yet relayed to Kafka (Phase 10).
- Rate limiting is per instance; the distributed limit lives in the gateway (Phase 6).
