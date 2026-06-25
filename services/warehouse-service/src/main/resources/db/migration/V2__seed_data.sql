-- V2: Seed reference data for Warehouse Service

-- Insert service metadata record (for health/version checks)
INSERT INTO warehouses (
    id, warehouse_code, warehouse_name, description,
    warehouse_type, location_address, city, country_code,
    is_active, created_by, updated_by
) VALUES (
    'seed-warehouse-disabled',
    '__SEED_CHECK__',
    'Seed Verification Record',
    'Internal record to verify seed ran',
    'GENERAL', 'N/A', 'N/A', 'US',
    false,
    'system', 'system'
) ON CONFLICT (warehouse_code) DO NOTHING;

-- Clean up the seed verification record
DELETE FROM warehouses WHERE warehouse_code = '__SEED_CHECK__';
