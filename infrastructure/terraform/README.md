# Terraform (Phase 14 — not started)

Planned modules: `network` (VPC, subnets across 3 AZs), `eks`, `rds-postgres` (Multi-AZ), `msk`, `elasticache-redis`, `documentdb`, `s3-documents`, `cloudfront-spa` (Angular + React), `secrets`, `observability`.
State in S3 with DynamoDB locking; one workspace per environment; applied from GitHub Actions via OIDC (no long-lived AWS keys).
