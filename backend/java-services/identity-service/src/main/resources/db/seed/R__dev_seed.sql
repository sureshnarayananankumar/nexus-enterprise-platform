-- =====================================================================
-- DEVELOPMENT-ONLY seed data. Loaded only with SPRING_PROFILES_ACTIVE=dev.
--   tenant:   acme
--   admin:    admin@acme.test / NexusAdmin2026!
-- Never enable this location in staging or production.
-- =====================================================================

INSERT INTO tenants (id, slug, name)
VALUES ('11111111-1111-1111-1111-111111111111', 'acme', 'Acme Corporation (demo)')
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_accounts (id, tenant_id, email, email_normalized, password_hash, display_name)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111',
        'admin@acme.test', 'admin@acme.test',
        '{bcrypt}$2a$12$LwB/CNuV0A31njR2oDJOq.p8vwh34tqsQwdK025nX3I2xWQbH1t26',
        'Acme Admin (demo)')
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
VALUES ('22222222-2222-2222-2222-222222222222', '00000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;
