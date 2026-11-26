# Node.js / NestJS services (Phase 6 — not started)

| Service | Responsibility |
| --- | --- |
| `api-gateway` | Routing, JWKS verification, Redis rate limiting, CORS, GraphQL BFF |
| `realtime-service` | Socket.IO gateway, presence, Kafka → WebSocket bridge |
| `integration-service` | Connectors, signed outbound webhooks, inbound webhooks |
| `worker-service` | BullMQ jobs: email, report rendering, workflow timers |

Integration contract with existing code: tokens are verified against identity-service `GET /.well-known/jwks.json`; permissions come from the `perms` claim; tenant from `tid`. See [docs/architecture/02-microservices.md](../../docs/architecture/02-microservices.md).
