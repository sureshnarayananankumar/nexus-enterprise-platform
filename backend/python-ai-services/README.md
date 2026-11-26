# Python / FastAPI AI services (Phase 9 — not started)

`ai-orchestrator`, `rag-service`, `document-intelligence`, `nl-sql-service`.
Design: [docs/architecture/05-ai-architecture.md](../../docs/architecture/05-ai-architecture.md).
AI services call business APIs **with the end user's token**, so they can never exceed the user's permissions.
