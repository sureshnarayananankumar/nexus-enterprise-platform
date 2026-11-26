-- Permissions owned by identity-service and the platform's system roles.
-- Other services add their permissions in their own identity-registration migrations later (Phase 5).

INSERT INTO permissions (code, description) VALUES
    ('identity.tenant.read',   'View tenant details'),
    ('identity.tenant.update', 'Update tenant settings'),
    ('identity.user.create',   'Create user accounts in the tenant'),
    ('identity.user.read',     'List and view user accounts'),
    ('identity.user.update',   'Enable/disable user accounts and change their roles'),
    ('identity.role.read',     'View roles and permissions'),
    ('identity.role.manage',   'Create and edit custom roles'),
    ('identity.audit.read',    'Read the authentication audit log');

INSERT INTO roles (id, tenant_id, code, name, description) VALUES
    ('00000000-0000-0000-0000-000000000001', NULL, 'TENANT_ADMIN', 'Tenant administrator', 'Full administrative access within a tenant'),
    ('00000000-0000-0000-0000-000000000002', NULL, 'MANAGER',      'Manager',              'Manages teams, projects and approvals'),
    ('00000000-0000-0000-0000-000000000003', NULL, 'EMPLOYEE',     'Employee',             'Standard internal user'),
    ('00000000-0000-0000-0000-000000000004', NULL, 'CUSTOMER',     'Customer',             'External customer portal user'),
    ('00000000-0000-0000-0000-000000000005', NULL, 'AUDITOR',      'Auditor',              'Read-only compliance access');

-- TENANT_ADMIN gets every identity permission
INSERT INTO role_permissions (role_id, permission_code)
SELECT '00000000-0000-0000-0000-000000000001', code FROM permissions;

INSERT INTO role_permissions (role_id, permission_code) VALUES
    ('00000000-0000-0000-0000-000000000002', 'identity.user.read'),
    ('00000000-0000-0000-0000-000000000002', 'identity.role.read'),
    ('00000000-0000-0000-0000-000000000005', 'identity.user.read'),
    ('00000000-0000-0000-0000-000000000005', 'identity.role.read'),
    ('00000000-0000-0000-0000-000000000005', 'identity.audit.read'),
    ('00000000-0000-0000-0000-000000000005', 'identity.tenant.read');
