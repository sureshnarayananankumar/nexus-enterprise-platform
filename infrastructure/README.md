# infrastructure/

| Folder | Status | Contents |
| --- | --- | --- |
| `docker/` | ✅ started | Generic Java service Dockerfile, Prometheus and Grafana provisioning for `docker-compose.yml` |
| `kubernetes/` | ⏳ Phase 11 | Kustomize base + overlays (dev/staging/prod), HPA, PDB, NetworkPolicies, probes |
| `terraform/` | ⏳ Phase 14 | AWS: VPC, EKS, RDS PostgreSQL, MSK, ElastiCache, DocumentDB, S3, CloudFront, Secrets Manager |
