-- V2__seed_data.sql
-- Seed default permissions, roles, role-permission mappings, and system admin user
-- Per ADR-0005 role hierarchy: SYSTEM_ADMIN, WAREHOUSE_MANAGER, INVENTORY_OPERATOR,
--   SUPPLIER_MANAGER, REPORTER, AUDITOR

-- ============================================================
-- DEFAULT PERMISSIONS
-- ============================================================
INSERT INTO permissions (id, name, description, resource, action, is_active) VALUES
-- User management
('perm-usr-read',    'user:read',         'Read user profiles',          'user',      'READ',       TRUE),
('perm-usr-create',  'user:create',       'Create users',                'user',      'CREATE',     TRUE),
('perm-usr-update',  'user:update',       'Update user profiles',        'user',      'UPDATE',     TRUE),
('perm-usr-delete',  'user:delete',       'Deactivate/delete users',     'user',      'DELETE',     TRUE),
('perm-usr-admin',   'user:admin:write',  'Admin-level user management', 'user',      'MANAGE',     TRUE),
('perm-usr-radmin',  'user:admin:read',   'Admin-level user listing',    'user',      'READ',       TRUE),

-- Role management
('perm-role-read',   'role:read',         'Read roles',                  'role',      'READ',       TRUE),
('perm-role-create', 'role:admin:create', 'Create roles',                'role',      'CREATE',     TRUE),
('perm-role-assign', 'role:assign',       'Assign roles to users',       'role',      'MANAGE',     TRUE),
('perm-role-revoke', 'role:revoke',       'Revoke user roles',           'role',      'MANAGE',     TRUE),

-- Inventory
('perm-inv-read',    'inventory:read',    'Read inventory data',         'inventory', 'READ',       TRUE),
('perm-inv-write',   'inventory:write',   'Write inventory data',        'inventory', 'READ_WRITE', TRUE),
('perm-inv-adjust',  'inventory:adjust',  'Adjust stock counts',         'inventory', 'EXECUTE',    TRUE),
('perm-inv-audit',   'inventory:audit',   'Audit inventory',             'inventory', 'READ',       TRUE),

-- Warehouse
('perm-wh-manage',   'warehouse:manage',  'Manage warehouse operations', 'warehouse', 'MANAGE',     TRUE),
('perm-wh-read',     'warehouse:read',    'Read warehouse data',         'warehouse', 'READ',       TRUE),

-- Supplier
('perm-sup-read',    'supplier:read',     'Read supplier data',          'supplier',  'READ',       TRUE),
('perm-sup-write',   'supplier:write',    'Manage suppliers',            'supplier',  'READ_WRITE', TRUE),

-- Product
('perm-prod-read',   'product:read',      'Read product catalog',        'product',   'READ',       TRUE),
('perm-prod-write',  'product:write',     'Manage product catalog',      'product',   'READ_WRITE', TRUE),

-- Order
('perm-ord-create',  'order:create',      'Create purchase/sales orders','order',     'CREATE',     TRUE),
('perm-ord-read',    'order:read',        'Read orders',                 'order',     'READ',       TRUE),

-- Reports
('perm-rep-view',    'report:view',       'View reports',                'report',    'READ',       TRUE),
('perm-rep-gen',     'report:generate',   'Generate reports',            'report',    'EXECUTE',    TRUE),
('perm-rep-export',  'report:export',     'Export report data',          'report',    'EXECUTE',    TRUE),

-- Audit
('perm-aud-read',    'audit:read',        'Read audit logs',             'audit',     'READ',       TRUE),

-- System
('perm-sys-admin',   'system:admin',      'Full system administration',  'system',    'MANAGE',     TRUE),
('perm-sys-config',  'system:config',     'System configuration',        'system',    'MANAGE',     TRUE);

-- ============================================================
-- DEFAULT ROLES  (per ADR-0005 hierarchy)
-- ============================================================
INSERT INTO roles (id, name, description, is_system_role, is_active) VALUES
('role-sys-admin', 'SYSTEM_ADMIN',       'Full system access',                        TRUE,  TRUE),
('role-wh-mgr',    'WAREHOUSE_MANAGER',  'Warehouse operations and staff management', FALSE, TRUE),
('role-inv-op',    'INVENTORY_OPERATOR', 'Stock operations',                          FALSE, TRUE),
('role-sup-mgr',   'SUPPLIER_MANAGER',   'Supplier and purchase order management',    FALSE, TRUE),
('role-reporter',  'REPORTER',           'Read-only analytics and reporting',         FALSE, TRUE),
('role-auditor',   'AUDITOR',            'Audit log access only',                     FALSE, TRUE);

-- ============================================================
-- ROLE → PERMISSION ASSIGNMENTS  (composite PK: role_id, permission_id)
-- ============================================================

-- SYSTEM_ADMIN: all permissions
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-sys-admin', 'perm-usr-read'),
('role-sys-admin', 'perm-usr-create'),
('role-sys-admin', 'perm-usr-update'),
('role-sys-admin', 'perm-usr-delete'),
('role-sys-admin', 'perm-usr-admin'),
('role-sys-admin', 'perm-usr-radmin'),
('role-sys-admin', 'perm-role-read'),
('role-sys-admin', 'perm-role-create'),
('role-sys-admin', 'perm-role-assign'),
('role-sys-admin', 'perm-role-revoke'),
('role-sys-admin', 'perm-inv-read'),
('role-sys-admin', 'perm-inv-write'),
('role-sys-admin', 'perm-inv-adjust'),
('role-sys-admin', 'perm-inv-audit'),
('role-sys-admin', 'perm-wh-manage'),
('role-sys-admin', 'perm-wh-read'),
('role-sys-admin', 'perm-sup-read'),
('role-sys-admin', 'perm-sup-write'),
('role-sys-admin', 'perm-prod-read'),
('role-sys-admin', 'perm-prod-write'),
('role-sys-admin', 'perm-ord-create'),
('role-sys-admin', 'perm-ord-read'),
('role-sys-admin', 'perm-rep-view'),
('role-sys-admin', 'perm-rep-gen'),
('role-sys-admin', 'perm-rep-export'),
('role-sys-admin', 'perm-aud-read'),
('role-sys-admin', 'perm-sys-admin'),
('role-sys-admin', 'perm-sys-config');

-- WAREHOUSE_MANAGER: warehouse + inventory read/write + reports
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-wh-mgr', 'perm-inv-read'),
('role-wh-mgr', 'perm-inv-write'),
('role-wh-mgr', 'perm-inv-adjust'),
('role-wh-mgr', 'perm-wh-manage'),
('role-wh-mgr', 'perm-wh-read'),
('role-wh-mgr', 'perm-prod-read'),
('role-wh-mgr', 'perm-rep-view'),
('role-wh-mgr', 'perm-usr-read');

-- INVENTORY_OPERATOR: stock operations
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-inv-op', 'perm-inv-read'),
('role-inv-op', 'perm-inv-write'),
('role-inv-op', 'perm-inv-adjust'),
('role-inv-op', 'perm-wh-read'),
('role-inv-op', 'perm-prod-read');

-- SUPPLIER_MANAGER: supplier + purchase orders
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-sup-mgr', 'perm-sup-read'),
('role-sup-mgr', 'perm-sup-write'),
('role-sup-mgr', 'perm-ord-create'),
('role-sup-mgr', 'perm-ord-read'),
('role-sup-mgr', 'perm-rep-view'),
('role-sup-mgr', 'perm-prod-read');

-- REPORTER: read-only across data
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-reporter', 'perm-inv-read'),
('role-reporter', 'perm-wh-read'),
('role-reporter', 'perm-sup-read'),
('role-reporter', 'perm-prod-read'),
('role-reporter', 'perm-ord-read'),
('role-reporter', 'perm-rep-view'),
('role-reporter', 'perm-rep-gen'),
('role-reporter', 'perm-rep-export');

-- AUDITOR: audit logs only
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-auditor', 'perm-aud-read'),
('role-auditor', 'perm-rep-view');

-- ============================================================
-- DEFAULT SYSTEM ADMIN USER
-- Password: Admin@SmartStock2026! (BCrypt cost 12)
-- Change immediately in production via the API.
-- ============================================================
INSERT INTO users (
    id, username, email, password_hash,
    first_name, last_name,
    is_active, email_verified,
    password_changed_at, created_at, updated_at
) VALUES (
    'user-system-admin',
    'system.admin',
    'admin@smartstock.local',
    '$2a$12$XD4r5u8z7ZA7fWlC5L4BOuiNRs0bBcCHV5eH7f.W3b8rVMN3qGC7G',
    'System', 'Administrator',
    TRUE, TRUE,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- Assign SYSTEM_ADMIN role  (composite PK: user_id, role_id)
INSERT INTO user_roles (user_id, role_id, assigned_at)
VALUES ('user-system-admin', 'role-sys-admin', CURRENT_TIMESTAMP);
