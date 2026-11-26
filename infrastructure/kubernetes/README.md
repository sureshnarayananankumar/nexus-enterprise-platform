# Kubernetes (Phase 11 — not started)

Planned layout:

```text
kubernetes/
├── base/<service>/            deployment.yaml, service.yaml, hpa.yaml, pdb.yaml, networkpolicy.yaml
├── overlays/dev|staging|prod/ kustomization.yaml with image tags, replicas, resources, secrets refs
└── platform/                  ingress-nginx / ALB controller, external-secrets, prometheus-operator CRs
```

Conventions: every pod listens on 8080, exposes `/actuator/health/liveness` and `/readiness`, runs as non-root with a read-only root filesystem, and receives secrets via External Secrets Operator.
