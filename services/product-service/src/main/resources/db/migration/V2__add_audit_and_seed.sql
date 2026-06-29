-- Product Service — V2: Audit logs + seed data

-- ── Audit Logs ────────────────────────────────────────────────────────────────

CREATE TABLE product_audit_logs (
    id             VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    event_type     VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(100) NOT NULL DEFAULT 'PRODUCT',
    entity_id      VARCHAR(36)  NOT NULL,
    actor_id       VARCHAR(36)  NOT NULL,
    action_type    VARCHAR(50)  NOT NULL,
    old_values     JSONB,
    new_values     JSONB,
    change_summary TEXT,
    timestamp      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id VARCHAR(36),
    request_id     VARCHAR(36),
    CONSTRAINT ck_action_type CHECK (
        action_type IN ('CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE',
                        'DISCONTINUE', 'RESTORE', 'BULK_IMPORT')
    )
);

CREATE INDEX idx_audit_entity    ON product_audit_logs(entity_id, timestamp DESC);
CREATE INDEX idx_audit_actor     ON product_audit_logs(actor_id);
CREATE INDEX idx_audit_event     ON product_audit_logs(event_type);
CREATE INDEX idx_audit_timestamp ON product_audit_logs(timestamp DESC);

-- Immutability: prevent updates and deletes on audit rows
CREATE OR REPLACE RULE product_audit_no_update AS ON UPDATE TO product_audit_logs DO INSTEAD NOTHING;
CREATE OR REPLACE RULE product_audit_no_delete AS ON DELETE TO product_audit_logs DO INSTEAD NOTHING;

-- ── Seed: root categories ─────────────────────────────────────────────────────

INSERT INTO categories (id, category_name, description, category_level, sort_order, created_by, updated_by)
VALUES
    ('cat-electronics',  'Electronics',         'Electronic devices and components',  0, 1, 'system', 'system'),
    ('cat-hardware',     'Hardware & Tools',     'Physical tools and hardware items',  0, 2, 'system', 'system'),
    ('cat-consumables',  'Consumables',          'Perishable and consumable items',    0, 3, 'system', 'system'),
    ('cat-raw-material', 'Raw Materials',        'Unprocessed source materials',       0, 4, 'system', 'system'),
    ('cat-packaging',    'Packaging',            'Packaging and shipping materials',   0, 5, 'system', 'system')
ON CONFLICT (category_name) DO NOTHING;
