-- QAFlow Phase 3 deterministic test/demo fixture.
-- Apply only after phase-3-initial-schema.sql to an empty schema.
-- Never load this file in a production profile.
-- Test-only credentials for all three users: password
-- The stored value is a known BCrypt test hash with the {bcrypt} id prefix.
-- Source example: https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html

SET time_zone = '+00:00';
START TRANSACTION;

INSERT INTO users (
    id, username, display_name, password_hash, role, active,
    auth_version, version, created_at, created_by, updated_at, updated_by
) VALUES (
    1, 'admin', 'QAFlow Administrator',
    '{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'ADMIN', 1, 0, 0,
    '2026-07-19 07:00:00.000000', NULL,
    '2026-07-19 07:00:00.000000', NULL
);

INSERT INTO users (
    id, username, display_name, password_hash, role, active,
    auth_version, version, created_at, created_by, updated_at, updated_by
) VALUES
    (
        2, 'operator', 'QAFlow Operator',
        '{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
        'OPERATOR', 1, 0, 0,
        '2026-07-19 07:01:00.000000', 1,
        '2026-07-19 07:01:00.000000', 1
    ),
    (
        3, 'inspector', 'QAFlow Quality Inspector',
        '{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
        'QUALITY_INSPECTOR', 1, 0, 0,
        '2026-07-19 07:02:00.000000', 1,
        '2026-07-19 07:02:00.000000', 1
    );

INSERT INTO products (
    id, sku, name, description, active, version,
    created_at, created_by, updated_at, updated_by
) VALUES
    (
        1, 'P-FRAME-001', 'Industrial Frame',
        'Welded frame used by the QAFlow demo flow.', 1, 0,
        '2026-07-19 07:10:00.000000', 1,
        '2026-07-19 07:10:00.000000', 1
    ),
    (
        2, 'P-PANEL-001', 'Control Panel',
        'Compact electrical control panel for test scenarios.', 1, 0,
        '2026-07-19 07:11:00.000000', 1,
        '2026-07-19 07:11:00.000000', 1
    );

INSERT INTO materials (
    id, code, name, description, unit_of_measure, active, version,
    created_at, created_by, updated_at, updated_by
) VALUES
    (
        1, 'MAT-STL-001', 'Steel Sheet', 'Carbon steel sheet stock.',
        'KILOGRAM', 1, 0,
        '2026-07-19 07:12:00.000000', 1,
        '2026-07-19 07:12:00.000000', 1
    ),
    (
        2, 'MAT-BLT-001', 'M8 Bolt', 'M8 fastening bolt.',
        'PIECE', 1, 0,
        '2026-07-19 07:13:00.000000', 1,
        '2026-07-19 07:13:00.000000', 1
    ),
    (
        3, 'MAT-PNT-001', 'Protective Paint', 'Industrial protective coating.',
        'LITER', 1, 0,
        '2026-07-19 07:14:00.000000', 1,
        '2026-07-19 07:14:00.000000', 1
    ),
    (
        4, 'MAT-CAB-001', 'Control Cable', 'Flexible control cable.',
        'METER', 1, 0,
        '2026-07-19 07:15:00.000000', 1,
        '2026-07-19 07:15:00.000000', 1
    );

INSERT INTO boms (
    id, product_id, revision_number, is_current,
    created_at, created_by, superseded_at, superseded_by
) VALUES
    (1, 1, 1, 1, '2026-07-19 07:20:00.000000', 1, NULL, NULL),
    (2, 2, 1, 1, '2026-07-19 07:21:00.000000', 1, NULL, NULL);

INSERT INTO bom_items (
    id, bom_id, material_id, quantity_per_unit, created_at
) VALUES
    (1, 1, 1, 2.500000, '2026-07-19 07:20:00.000000'),
    (2, 1, 2, 8.000000, '2026-07-19 07:20:00.000000'),
    (3, 1, 3, 0.250000, '2026-07-19 07:20:00.000000'),
    (4, 2, 1, 1.200000, '2026-07-19 07:21:00.000000'),
    (5, 2, 2, 4.000000, '2026-07-19 07:21:00.000000'),
    (6, 2, 4, 3.000000, '2026-07-19 07:21:00.000000');

INSERT INTO production_orders (
    id, order_number, product_id, bom_id, quantity, status, version,
    created_at, created_by, updated_at, updated_by
) VALUES
    (
        1, 'PO-20260719-0001', 1, NULL, 10.000000, 'DRAFT', 0,
        '2026-07-19 10:00:00.000000', 2,
        '2026-07-19 10:00:00.000000', 2
    ),
    (
        2, 'PO-20260719-0002', 1, 1, 5.000000, 'CONFIRMED', 1,
        '2026-07-19 08:30:00.000000', 2,
        '2026-07-19 08:35:00.000000', 2
    ),
    (
        3, 'PO-20260719-0003', 2, 2, 4.000000, 'IN_PROGRESS', 2,
        '2026-07-19 08:40:00.000000', 2,
        '2026-07-19 08:50:00.000000', 2
    ),
    (
        4, 'PO-20260719-0004', 1, 1, 2.000000, 'COMPLETED', 3,
        '2026-07-19 09:00:00.000000', 2,
        '2026-07-19 09:15:00.000000', 2
    ),
    (
        5, 'PO-20260719-0005', 1, 1, 1.000000, 'CANCELLED', 2,
        '2026-07-19 09:40:00.000000', 2,
        '2026-07-19 09:50:00.000000', 2
    ),
    (
        6, 'PO-20260719-0006', 2, 2, 1.000000, 'COMPLETED', 3,
        '2026-07-19 09:20:00.000000', 2,
        '2026-07-19 09:35:00.000000', 2
    );

INSERT INTO production_order_materials (
    id, production_order_id, material_id, quantity_per_unit,
    required_quantity, active_reserved_quantity, created_at
) VALUES
    (1, 2, 1, 2.500000, 12.500000, 12.500000, '2026-07-19 08:35:00.000000'),
    (2, 2, 2, 8.000000, 40.000000, 40.000000, '2026-07-19 08:35:00.000000'),
    (3, 2, 3, 0.250000, 1.250000, 1.250000, '2026-07-19 08:35:00.000000'),
    (4, 3, 1, 1.200000, 4.800000, 4.800000, '2026-07-19 08:45:00.000000'),
    (5, 3, 2, 4.000000, 16.000000, 16.000000, '2026-07-19 08:45:00.000000'),
    (6, 3, 4, 3.000000, 12.000000, 12.000000, '2026-07-19 08:45:00.000000'),
    (7, 4, 1, 2.500000, 5.000000, 0.000000, '2026-07-19 09:05:00.000000'),
    (8, 4, 2, 8.000000, 16.000000, 0.000000, '2026-07-19 09:05:00.000000'),
    (9, 4, 3, 0.250000, 0.500000, 0.000000, '2026-07-19 09:05:00.000000'),
    (10, 5, 1, 2.500000, 2.500000, 0.000000, '2026-07-19 09:45:00.000000'),
    (11, 5, 2, 8.000000, 8.000000, 0.000000, '2026-07-19 09:45:00.000000'),
    (12, 5, 3, 0.250000, 0.250000, 0.000000, '2026-07-19 09:45:00.000000'),
    (13, 6, 1, 1.200000, 1.200000, 0.000000, '2026-07-19 09:25:00.000000'),
    (14, 6, 2, 4.000000, 4.000000, 0.000000, '2026-07-19 09:25:00.000000'),
    (15, 6, 4, 3.000000, 3.000000, 0.000000, '2026-07-19 09:25:00.000000');

INSERT INTO inventory_balances (
    material_id, on_hand_quantity, reserved_quantity,
    version, updated_at, updated_by
) VALUES
    (1, 93.800000, 17.300000, 8, '2026-07-19 09:50:00.000000', 2),
    (2, 480.000000, 56.000000, 8, '2026-07-19 09:50:00.000000', 2),
    (3, 29.500000, 1.250000, 5, '2026-07-19 09:50:00.000000', 2),
    (4, 97.000000, 12.000000, 4, '2026-07-19 09:35:00.000000', 2);

INSERT INTO inventory_movements (
    id, material_id, movement_type, direction, quantity,
    on_hand_after, reserved_after, production_order_id,
    reference_code, reason, performed_by, occurred_at, correlation_id
) VALUES
    (1, 1, 'ENTRY', 'IN', 100.000000, 100.000000, 0.000000, NULL,
     'SEED-RECEIPT-001', 'Initial test stock', 2,
     '2026-07-19 08:00:00.000000', 'seed-inventory-steel'),
    (2, 2, 'ENTRY', 'IN', 500.000000, 500.000000, 0.000000, NULL,
     'SEED-RECEIPT-002', 'Initial test stock', 2,
     '2026-07-19 08:01:00.000000', 'seed-inventory-bolts'),
    (3, 3, 'ENTRY', 'IN', 30.000000, 30.000000, 0.000000, NULL,
     'SEED-RECEIPT-003', 'Initial test stock', 2,
     '2026-07-19 08:02:00.000000', 'seed-inventory-paint'),
    (4, 4, 'ENTRY', 'IN', 100.000000, 100.000000, 0.000000, NULL,
     'SEED-RECEIPT-004', 'Initial test stock', 2,
     '2026-07-19 08:03:00.000000', 'seed-inventory-cable'),
    (5, 1, 'PRODUCTION_CONSUMPTION', 'OUT', 5.000000, 95.000000, 17.300000, 4,
     'PO-20260719-0004', 'Production order consumption', 2,
     '2026-07-19 09:15:00.000000', 'seed-order-0004-complete'),
    (6, 2, 'PRODUCTION_CONSUMPTION', 'OUT', 16.000000, 484.000000, 56.000000, 4,
     'PO-20260719-0004', 'Production order consumption', 2,
     '2026-07-19 09:15:00.000000', 'seed-order-0004-complete'),
    (7, 3, 'PRODUCTION_CONSUMPTION', 'OUT', 0.500000, 29.500000, 1.250000, 4,
     'PO-20260719-0004', 'Production order consumption', 2,
     '2026-07-19 09:15:00.000000', 'seed-order-0004-complete'),
    (8, 1, 'PRODUCTION_CONSUMPTION', 'OUT', 1.200000, 93.800000, 17.300000, 6,
     'PO-20260719-0006', 'Production order consumption', 2,
     '2026-07-19 09:35:00.000000', 'seed-order-0006-complete'),
    (9, 2, 'PRODUCTION_CONSUMPTION', 'OUT', 4.000000, 480.000000, 56.000000, 6,
     'PO-20260719-0006', 'Production order consumption', 2,
     '2026-07-19 09:35:00.000000', 'seed-order-0006-complete'),
    (10, 4, 'PRODUCTION_CONSUMPTION', 'OUT', 3.000000, 97.000000, 12.000000, 6,
     'PO-20260719-0006', 'Production order consumption', 2,
     '2026-07-19 09:35:00.000000', 'seed-order-0006-complete');

INSERT INTO production_order_status_history (
    id, production_order_id, sequence_number, from_status, to_status,
    changed_by, changed_at, correlation_id
) VALUES
    (1, 1, 1, NULL, 'DRAFT', 2, '2026-07-19 10:00:00.000000', 'seed-order-0001-create'),
    (2, 2, 1, NULL, 'DRAFT', 2, '2026-07-19 08:30:00.000000', 'seed-order-0002-create'),
    (3, 2, 2, 'DRAFT', 'CONFIRMED', 2, '2026-07-19 08:35:00.000000', 'seed-order-0002-confirm'),
    (4, 3, 1, NULL, 'DRAFT', 2, '2026-07-19 08:40:00.000000', 'seed-order-0003-create'),
    (5, 3, 2, 'DRAFT', 'CONFIRMED', 2, '2026-07-19 08:45:00.000000', 'seed-order-0003-confirm'),
    (6, 3, 3, 'CONFIRMED', 'IN_PROGRESS', 2, '2026-07-19 08:50:00.000000', 'seed-order-0003-start'),
    (7, 4, 1, NULL, 'DRAFT', 2, '2026-07-19 09:00:00.000000', 'seed-order-0004-create'),
    (8, 4, 2, 'DRAFT', 'CONFIRMED', 2, '2026-07-19 09:05:00.000000', 'seed-order-0004-confirm'),
    (9, 4, 3, 'CONFIRMED', 'IN_PROGRESS', 2, '2026-07-19 09:10:00.000000', 'seed-order-0004-start'),
    (10, 4, 4, 'IN_PROGRESS', 'COMPLETED', 2, '2026-07-19 09:15:00.000000', 'seed-order-0004-complete'),
    (11, 5, 1, NULL, 'DRAFT', 2, '2026-07-19 09:40:00.000000', 'seed-order-0005-create'),
    (12, 5, 2, 'DRAFT', 'CONFIRMED', 2, '2026-07-19 09:45:00.000000', 'seed-order-0005-confirm'),
    (13, 5, 3, 'CONFIRMED', 'CANCELLED', 2, '2026-07-19 09:50:00.000000', 'seed-order-0005-cancel'),
    (14, 6, 1, NULL, 'DRAFT', 2, '2026-07-19 09:20:00.000000', 'seed-order-0006-create'),
    (15, 6, 2, 'DRAFT', 'CONFIRMED', 2, '2026-07-19 09:25:00.000000', 'seed-order-0006-confirm'),
    (16, 6, 3, 'CONFIRMED', 'IN_PROGRESS', 2, '2026-07-19 09:30:00.000000', 'seed-order-0006-start'),
    (17, 6, 4, 'IN_PROGRESS', 'COMPLETED', 2, '2026-07-19 09:35:00.000000', 'seed-order-0006-complete');

INSERT INTO quality_inspections (
    id, production_order_id, result, observations,
    inspected_by, inspected_at, correlation_id
) VALUES
    (1, 4, 'APPROVED', 'Dimensions and finish within tolerance.',
     3, '2026-07-19 09:18:00.000000', 'seed-inspection-0004'),
    (2, 6, 'REJECTED', 'Cable routing is outside the approved tolerance.',
     3, '2026-07-19 09:38:00.000000', 'seed-inspection-0006');

-- Bulk fixture loading is represented as one audit event. It is not evidence
-- that the future application audit pipeline has been implemented or tested.
INSERT INTO audit_events (
    id, actor_user_id, actor_username_snapshot, actor_role_snapshot,
    action, resource_type, resource_id, outcome, summary,
    request_path, occurred_at, correlation_id
) VALUES (
    1, 1, 'admin', 'ADMIN',
    'DEMO_DATA_LOADED', 'SYSTEM', 'phase-3', 'SUCCESS',
    JSON_OBJECT(
        'users', 3,
        'products', 2,
        'materials', 4,
        'orders', 6,
        'inspections', 2
    ),
    NULL, '2026-07-19 10:05:00.000000', 'seed-phase-3-load'
);

COMMIT;
