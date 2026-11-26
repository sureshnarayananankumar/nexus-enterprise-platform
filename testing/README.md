# testing/

Service-level unit and integration tests live **inside each service**. This folder holds cross-service suites:

| Folder | Phase | Tooling |
| --- | --- | --- |
| `integration/` | 6+ | End-to-end API flows through the gateway (REST Assured / Playwright API) against `docker compose` |
| `performance/` | 13 | k6 scripts with thresholds (p95 latency, error rate) |
| `security/` | 13 | OWASP ZAP baseline scans, dependency and container scanning configs |

Current automated coverage: identity-service (unit + Testcontainers integration tests), platform-common (unit tests) — run in CI on every push.
