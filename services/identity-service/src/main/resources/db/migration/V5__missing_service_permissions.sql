-- V5: close every remaining gap between the permission catalog and the
-- authorities the services actually check (@PreAuthorize hasAuthority).
--
-- Derived by diffing all hasAuthority(...) strings across the service sources
-- against the V2+V4 catalog. Notable: inventory-service authorizes stock
-- movements as stock:* (not inventory:*), and product/warehouse demand
-- fine-grained create permissions that V2 never seeded.

-- ── Product ──────────────────────────────────────────────────────────────────
INSERT INTO permissions (id, name, description, resource, action, is_active) VALUES
('perm-prod-create',     'product:create',          'Create products',              'product',   'CREATE',  TRUE),
('perm-prod-cat-create', 'product:category:create', 'Create product categories',    'product',   'CREATE',  TRUE),
('perm-prod-import',     'product:import',          'Bulk import products',         'product',   'EXECUTE', TRUE),
('perm-prod-export',     'product:export',          'Export products',              'product',   'EXECUTE', TRUE);

-- ── Inventory / stock movements ──────────────────────────────────────────────
INSERT INTO permissions (id, name, description, resource, action, is_active) VALUES
('perm-inv-count',       'inventory:count',         'Run inventory counts',         'inventory', 'EXECUTE', TRUE),
('perm-stk-in',          'stock:in',                'Stock-in goods',               'stock',     'EXECUTE', TRUE),
('perm-stk-out',         'stock:out',               'Stock-out goods',              'stock',     'EXECUTE', TRUE),
('perm-stk-adjust',      'stock:adjust',            'Adjust stock levels',          'stock',     'EXECUTE', TRUE),
('perm-stk-transfer',    'stock:transfer',          'Transfer stock',               'stock',     'EXECUTE', TRUE),
('perm-stk-reserve',     'stock:reserve',           'Reserve stock',                'stock',     'EXECUTE', TRUE);

-- ── Supplier ─────────────────────────────────────────────────────────────────
INSERT INTO permissions (id, name, description, resource, action, is_active) VALUES
('perm-sup-create',      'supplier:create',         'Create suppliers',             'supplier',  'CREATE',  TRUE),
('perm-sup-report',      'supplier:report',         'View supplier reports',        'supplier',  'READ',    TRUE);

-- ── Warehouse structure ──────────────────────────────────────────────────────
INSERT INTO permissions (id, name, description, resource, action, is_active) VALUES
('perm-wh-create',       'warehouse:create',        'Create warehouses',            'warehouse', 'CREATE',  TRUE),
('perm-wh-write',        'warehouse:write',         'Update warehouses',            'warehouse', 'UPDATE',  TRUE),
('perm-wh-report',       'warehouse:report',        'View warehouse reports',       'warehouse', 'READ',    TRUE),
('perm-wh-zone-create',  'warehouse:zone:create',   'Create warehouse zones',       'warehouse', 'CREATE',  TRUE),
('perm-wh-shelf-create', 'warehouse:shelf:create',  'Create warehouse shelves',     'warehouse', 'CREATE',  TRUE),
('perm-wh-bin-create',   'warehouse:bin:create',    'Create warehouse bins',        'warehouse', 'CREATE',  TRUE);

-- ── Role grants ──────────────────────────────────────────────────────────────
-- SYSTEM_ADMIN: everything.
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-sys-admin', 'perm-prod-create'),
('role-sys-admin', 'perm-prod-cat-create'),
('role-sys-admin', 'perm-prod-import'),
('role-sys-admin', 'perm-prod-export'),
('role-sys-admin', 'perm-inv-count'),
('role-sys-admin', 'perm-stk-in'),
('role-sys-admin', 'perm-stk-out'),
('role-sys-admin', 'perm-stk-adjust'),
('role-sys-admin', 'perm-stk-transfer'),
('role-sys-admin', 'perm-stk-reserve'),
('role-sys-admin', 'perm-sup-create'),
('role-sys-admin', 'perm-sup-report'),
('role-sys-admin', 'perm-wh-create'),
('role-sys-admin', 'perm-wh-write'),
('role-sys-admin', 'perm-wh-report'),
('role-sys-admin', 'perm-wh-zone-create'),
('role-sys-admin', 'perm-wh-shelf-create'),
('role-sys-admin', 'perm-wh-bin-create');

-- WAREHOUSE_MANAGER: warehouse structure + stock movements.
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-wh-mgr', 'perm-inv-count'),
('role-wh-mgr', 'perm-stk-in'),
('role-wh-mgr', 'perm-stk-out'),
('role-wh-mgr', 'perm-stk-adjust'),
('role-wh-mgr', 'perm-stk-transfer'),
('role-wh-mgr', 'perm-stk-reserve'),
('role-wh-mgr', 'perm-wh-create'),
('role-wh-mgr', 'perm-wh-write'),
('role-wh-mgr', 'perm-wh-report'),
('role-wh-mgr', 'perm-wh-zone-create'),
('role-wh-mgr', 'perm-wh-shelf-create'),
('role-wh-mgr', 'perm-wh-bin-create');

-- INVENTORY_OPERATOR: day-to-day stock movements.
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-inv-op', 'perm-inv-count'),
('role-inv-op', 'perm-stk-in'),
('role-inv-op', 'perm-stk-out'),
('role-inv-op', 'perm-stk-transfer');

-- SUPPLIER_MANAGER: supplier lifecycle + reporting.
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-sup-mgr', 'perm-sup-create'),
('role-sup-mgr', 'perm-sup-report');
