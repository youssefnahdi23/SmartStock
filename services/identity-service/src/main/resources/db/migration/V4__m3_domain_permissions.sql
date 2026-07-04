-- V4: permissions for the M-3 domain services (customer, purchase-order, sales-order).
--
-- The V2 catalog predates the M-3 split and only carries the generic order:*
-- permissions, but customer-service and the two order services authorize with
-- fine-grained authorities (PERMISSION_customer:read, PERMISSION_purchase-order:confirm, …).
-- Without these rows even SYSTEM_ADMIN is denied on those APIs.

-- ── Customer management ─────────────────────────────────────────────────────
INSERT INTO permissions (id, name, description, resource, action, is_active) VALUES
('perm-cust-read',    'customer:read',          'Read customers',                 'customer',       'READ',    TRUE),
('perm-cust-create',  'customer:create',        'Create customers',               'customer',       'CREATE',  TRUE),
('perm-cust-write',   'customer:write',         'Update customers',               'customer',       'UPDATE',  TRUE);

-- ── Purchase orders (procurement) ────────────────────────────────────────────
INSERT INTO permissions (id, name, description, resource, action, is_active) VALUES
('perm-po-read',      'purchase-order:read',    'Read purchase orders',           'purchase-order', 'READ',    TRUE),
('perm-po-create',    'purchase-order:create',  'Create purchase orders',         'purchase-order', 'CREATE',  TRUE),
('perm-po-write',     'purchase-order:write',   'Update purchase orders',         'purchase-order', 'UPDATE',  TRUE),
('perm-po-confirm',   'purchase-order:confirm', 'Confirm purchase orders',        'purchase-order', 'EXECUTE', TRUE),
('perm-po-receive',   'purchase-order:receive', 'Receive purchase order goods',   'purchase-order', 'EXECUTE', TRUE),
('perm-po-quality',   'purchase-order:quality', 'Record quality inspection',      'purchase-order', 'EXECUTE', TRUE);

-- ── Sales orders (fulfilment) ────────────────────────────────────────────────
INSERT INTO permissions (id, name, description, resource, action, is_active) VALUES
('perm-so-read',      'sales-order:read',       'Read sales orders',              'sales-order',    'READ',    TRUE),
('perm-so-create',    'sales-order:create',     'Create sales orders',            'sales-order',    'CREATE',  TRUE),
('perm-so-write',     'sales-order:write',      'Update sales orders',            'sales-order',    'UPDATE',  TRUE),
('perm-so-confirm',   'sales-order:confirm',    'Confirm sales orders',           'sales-order',    'EXECUTE', TRUE),
('perm-so-pick',      'sales-order:pick',       'Pick sales order stock',         'sales-order',    'EXECUTE',    TRUE),
('perm-so-ship',      'sales-order:ship',       'Ship sales orders',              'sales-order',    'EXECUTE',    TRUE),
('perm-so-deliver',   'sales-order:deliver',    'Mark sales orders delivered',    'sales-order',    'EXECUTE', TRUE);

-- ── Role grants ──────────────────────────────────────────────────────────────
-- SYSTEM_ADMIN: everything (mirrors the V2 convention).
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-sys-admin', 'perm-cust-read'),
('role-sys-admin', 'perm-cust-create'),
('role-sys-admin', 'perm-cust-write'),
('role-sys-admin', 'perm-po-read'),
('role-sys-admin', 'perm-po-create'),
('role-sys-admin', 'perm-po-write'),
('role-sys-admin', 'perm-po-confirm'),
('role-sys-admin', 'perm-po-receive'),
('role-sys-admin', 'perm-po-quality'),
('role-sys-admin', 'perm-so-read'),
('role-sys-admin', 'perm-so-create'),
('role-sys-admin', 'perm-so-write'),
('role-sys-admin', 'perm-so-confirm'),
('role-sys-admin', 'perm-so-pick'),
('role-sys-admin', 'perm-so-ship'),
('role-sys-admin', 'perm-so-deliver');

-- SUPPLIER_MANAGER: procurement flow (had generic order:create/read in V2).
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-sup-mgr', 'perm-po-read'),
('role-sup-mgr', 'perm-po-create'),
('role-sup-mgr', 'perm-po-write'),
('role-sup-mgr', 'perm-po-confirm');

-- WAREHOUSE_MANAGER: goods receipt + fulfilment operations.
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-wh-mgr', 'perm-po-receive'),
('role-wh-mgr', 'perm-so-read'),
('role-wh-mgr', 'perm-so-pick'),
('role-wh-mgr', 'perm-so-ship'),
('role-wh-mgr', 'perm-so-deliver');
