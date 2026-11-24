-- QAFlow Phase 3 read-only validation queries.
-- Apply schema and optional test data first. Select the QAFlow database before running.
-- Queries marked "expected: zero rows" report violations only.

SET time_zone = '+00:00';

-- V-001: normalized usernames must be unique. Expected: zero rows.
SELECT username_normalized, COUNT(*) AS duplicate_count
FROM users
GROUP BY username_normalized
HAVING COUNT(*) > 1;

-- V-002: normalized SKUs must be unique. Expected: zero rows.
SELECT sku_normalized, COUNT(*) AS duplicate_count
FROM products
GROUP BY sku_normalized
HAVING COUNT(*) > 1;

-- V-003: normalized material codes must be unique. Expected: zero rows.
SELECT code_normalized, COUNT(*) AS duplicate_count
FROM materials
GROUP BY code_normalized
HAVING COUNT(*) > 1;

-- V-004: at least one active ADMIN must remain. Expected: one row, active_admins >= 1.
SELECT COUNT(*) AS active_admins
FROM users
WHERE role = 'ADMIN' AND active = 1;

-- V-005: no product may have more than one current BOM. Expected: zero rows.
SELECT product_id, COUNT(*) AS current_bom_count
FROM boms
WHERE is_current = 1
GROUP BY product_id
HAVING COUNT(*) > 1;

-- V-006: every BOM must contain at least one item. Expected: zero rows.
SELECT b.id AS empty_bom_id, b.product_id, b.revision_number
FROM boms b
LEFT JOIN bom_items bi ON bi.bom_id = b.id
GROUP BY b.id, b.product_id, b.revision_number
HAVING COUNT(bi.id) = 0;

-- V-007: current BOMs may not reference inactive products/materials. Expected: zero rows.
SELECT
    b.id AS bom_id,
    p.id AS product_id,
    p.active AS product_active,
    m.id AS material_id,
    m.active AS material_active
FROM boms b
JOIN products p ON p.id = b.product_id
JOIN bom_items bi ON bi.bom_id = b.id
JOIN materials m ON m.id = bi.material_id
WHERE b.is_current = 1
  AND (p.active = 0 OR m.active = 0);

-- V-008: stock invariants. Expected: zero rows.
SELECT material_id, on_hand_quantity, reserved_quantity, available_quantity
FROM inventory_balances
WHERE on_hand_quantity < 0
   OR reserved_quantity < 0
   OR available_quantity < 0
   OR available_quantity <> on_hand_quantity - reserved_quantity;

-- V-009: aggregate balance reservation must equal active order reservations.
-- Expected: zero rows.
WITH expected_reservations AS (
    SELECT
        pom.material_id,
        SUM(pom.active_reserved_quantity) AS expected_reserved
    FROM production_order_materials pom
    JOIN production_orders po ON po.id = pom.production_order_id
    WHERE po.status IN ('CONFIRMED', 'IN_PROGRESS')
    GROUP BY pom.material_id
)
SELECT
    ib.material_id,
    ib.reserved_quantity AS balance_reserved,
    COALESCE(er.expected_reserved, 0) AS order_reserved
FROM inventory_balances ib
LEFT JOIN expected_reservations er ON er.material_id = ib.material_id
WHERE ib.reserved_quantity <> COALESCE(er.expected_reserved, 0);

-- V-010: active orders reserve every required unit; terminal/draft orders reserve none.
-- Expected: zero rows.
SELECT
    po.order_number,
    po.status,
    pom.material_id,
    pom.required_quantity,
    pom.active_reserved_quantity
FROM production_order_materials pom
JOIN production_orders po ON po.id = pom.production_order_id
WHERE (
        po.status IN ('CONFIRMED', 'IN_PROGRESS')
        AND pom.active_reserved_quantity <> pom.required_quantity
      )
   OR (
        po.status IN ('DRAFT', 'COMPLETED', 'CANCELLED')
        AND pom.active_reserved_quantity <> 0
      );

-- V-011: current physical balance must equal signed immutable movements.
-- Expected: zero rows.
WITH movement_totals AS (
    SELECT
        material_id,
        SUM(CASE WHEN direction = 'IN' THEN quantity ELSE -quantity END) AS calculated_on_hand
    FROM inventory_movements
    GROUP BY material_id
)
SELECT
    ib.material_id,
    ib.on_hand_quantity,
    COALESCE(mt.calculated_on_hand, 0) AS movement_on_hand
FROM inventory_balances ib
LEFT JOIN movement_totals mt ON mt.material_id = ib.material_id
WHERE ib.on_hand_quantity <> COALESCE(mt.calculated_on_hand, 0);

-- V-012: latest movement physical-stock snapshot must match current on-hand.
-- Reserved stock may change later without a movement, so it is not compared here.
-- Expected: zero rows for materials that have movements.
WITH ranked_movements AS (
    SELECT
        im.*,
        ROW_NUMBER() OVER (
            PARTITION BY im.material_id
            ORDER BY im.occurred_at DESC, im.id DESC
        ) AS row_rank
    FROM inventory_movements im
)
SELECT
    ib.material_id,
    ib.on_hand_quantity,
    ib.reserved_quantity AS current_reserved,
    rm.on_hand_after,
    rm.reserved_after AS reserved_when_movement_occurred
FROM inventory_balances ib
JOIN ranked_movements rm
  ON rm.material_id = ib.material_id
 AND rm.row_rank = 1
WHERE ib.on_hand_quantity <> rm.on_hand_after;

-- V-013: completed orders require exactly one matching consumption per material.
-- Expected: zero rows.
SELECT
    po.order_number,
    pom.material_id,
    pom.required_quantity,
    COUNT(im.id) AS movement_count,
    COALESCE(SUM(im.quantity), 0) AS consumed_quantity
FROM production_orders po
JOIN production_order_materials pom ON pom.production_order_id = po.id
LEFT JOIN inventory_movements im
  ON im.production_order_id = po.id
 AND im.material_id = pom.material_id
 AND im.movement_type = 'PRODUCTION_CONSUMPTION'
WHERE po.status = 'COMPLETED'
GROUP BY po.id, po.order_number, pom.material_id, pom.required_quantity
HAVING COUNT(im.id) <> 1
    OR COALESCE(SUM(im.quantity), 0) <> pom.required_quantity;

-- V-014: non-completed orders may not have production consumption. Expected: zero rows.
SELECT po.order_number, po.status, im.id AS movement_id
FROM production_orders po
JOIN inventory_movements im ON im.production_order_id = po.id
WHERE im.movement_type = 'PRODUCTION_CONSUMPTION'
  AND po.status <> 'COMPLETED';

-- V-015: order snapshot required quantity must match order quantity x unit quantity.
-- Expected: zero rows; comparison uses the approved scale of six decimals.
SELECT
    po.order_number,
    pom.material_id,
    pom.required_quantity,
    ROUND(po.quantity * pom.quantity_per_unit, 6) AS calculated_required
FROM production_orders po
JOIN production_order_materials pom ON pom.production_order_id = po.id
WHERE pom.required_quantity <> ROUND(po.quantity * pom.quantity_per_unit, 6);

-- V-016: DRAFT has no snapshot; confirmed/in-progress/completed has one or more.
-- CANCELLED has a snapshot exactly when it was confirmed before cancellation.
-- Expected: zero rows.
SELECT
    po.id,
    po.order_number,
    po.status,
    COUNT(pom.id) AS snapshot_line_count
FROM production_orders po
LEFT JOIN production_order_materials pom ON pom.production_order_id = po.id
GROUP BY po.id, po.order_number, po.status
HAVING (po.status = 'DRAFT' AND COUNT(pom.id) <> 0)
    OR (po.status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED') AND COUNT(pom.id) = 0)
    OR (
        po.status = 'CANCELLED'
        AND (
            (po.bom_id IS NULL AND COUNT(pom.id) <> 0)
            OR (po.bom_id IS NOT NULL AND COUNT(pom.id) = 0)
        )
    );

-- V-017: current order status must equal its latest immutable history status.
-- Expected: zero rows.
WITH latest_history AS (
    SELECT
        h.*,
        ROW_NUMBER() OVER (
            PARTITION BY h.production_order_id
            ORDER BY h.sequence_number DESC
        ) AS row_rank
    FROM production_order_status_history h
)
SELECT
    po.order_number,
    po.status AS current_status,
    lh.to_status AS history_status
FROM production_orders po
LEFT JOIN latest_history lh
  ON lh.production_order_id = po.id
 AND lh.row_rank = 1
WHERE lh.id IS NULL OR po.status <> lh.to_status;

-- V-018: status-history sequence must start at one and have no gaps.
-- Expected: zero rows.
SELECT
    production_order_id,
    MIN(sequence_number) AS minimum_sequence,
    MAX(sequence_number) AS maximum_sequence,
    COUNT(*) AS history_count
FROM production_order_status_history
GROUP BY production_order_id
HAVING MIN(sequence_number) <> 1
    OR MAX(sequence_number) <> COUNT(*);

-- V-019: an inspection may reference only a completed order. Expected: zero rows.
SELECT qi.id AS inspection_id, po.order_number, po.status
FROM quality_inspections qi
JOIN production_orders po ON po.id = qi.production_order_id
WHERE po.status <> 'COMPLETED';

-- V-020: rejected inspection requires non-blank observations. Expected: zero rows.
SELECT id, production_order_id, observations
FROM quality_inspections
WHERE result = 'REJECTED'
  AND CHAR_LENGTH(TRIM(COALESCE(observations, ''))) = 0;

-- V-021: inspection actor should currently have QUALITY_INSPECTOR role.
-- Historical role changes must ultimately be interpreted through audit snapshots.
-- Expected for seed: zero rows.
SELECT qi.id, qi.inspected_by, u.role
FROM quality_inspections qi
JOIN users u ON u.id = qi.inspected_by
WHERE u.role <> 'QUALITY_INSPECTOR';

-- V-022: fixture operator-owned actions should currently belong to OPERATOR.
-- Expected for seed: zero rows.
SELECT DISTINCT actor_id, role, action_source
FROM (
    SELECT im.performed_by AS actor_id, u.role, 'INVENTORY_MOVEMENT' AS action_source
    FROM inventory_movements im
    JOIN users u ON u.id = im.performed_by
    UNION ALL
    SELECT h.changed_by, u.role, 'ORDER_TRANSITION'
    FROM production_order_status_history h
    JOIN users u ON u.id = h.changed_by
) actor_actions
WHERE role <> 'OPERATOR';

-- V-023: audit records require core trace fields. Expected: zero rows.
SELECT id, action, resource_type, outcome, occurred_at, correlation_id
FROM audit_events
WHERE CHAR_LENGTH(TRIM(action)) = 0
   OR CHAR_LENGTH(TRIM(resource_type)) = 0
   OR CHAR_LENGTH(TRIM(correlation_id)) = 0
   OR occurred_at IS NULL;

-- V-024: expected immutable triggers must exist. Expected: one row with count = 10.
SELECT COUNT(*) AS immutable_trigger_count
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA = DATABASE()
  AND TRIGGER_NAME IN (
      'trg_bom_items_no_update',
      'trg_bom_items_no_delete',
      'trg_inventory_movements_no_update',
      'trg_inventory_movements_no_delete',
      'trg_order_status_history_no_update',
      'trg_order_status_history_no_delete',
      'trg_quality_inspections_no_update',
      'trg_quality_inspections_no_delete',
      'trg_audit_events_no_update',
      'trg_audit_events_no_delete'
  );

-- V-025: fixture summary. Expected after phase-3-test-data.sql:
-- users=3, products=2, materials=4, boms=2, orders=6,
-- movements=10, inspections=2, audit_events=1.
SELECT
    (SELECT COUNT(*) FROM users) AS users,
    (SELECT COUNT(*) FROM products) AS products,
    (SELECT COUNT(*) FROM materials) AS materials,
    (SELECT COUNT(*) FROM boms) AS boms,
    (SELECT COUNT(*) FROM production_orders) AS orders,
    (SELECT COUNT(*) FROM inventory_movements) AS movements,
    (SELECT COUNT(*) FROM quality_inspections) AS inspections,
    (SELECT COUNT(*) FROM audit_events) AS audit_events;

-- V-026: expected final fixture balances.
SELECT
    m.code,
    ib.on_hand_quantity,
    ib.reserved_quantity,
    ib.available_quantity
FROM inventory_balances ib
JOIN materials m ON m.id = ib.material_id
ORDER BY m.code;

-- Expected rows:
-- MAT-BLT-001 | 480.000000 | 56.000000 | 424.000000
-- MAT-CAB-001 |  97.000000 | 12.000000 |  85.000000
-- MAT-PNT-001 |  29.500000 |  1.250000 |  28.250000
-- MAT-STL-001 |  93.800000 | 17.300000 |  76.500000

-- V-027: operational view of orders and quality result.
SELECT
    po.order_number,
    p.sku,
    po.quantity,
    po.status,
    qi.result AS inspection_result
FROM production_orders po
JOIN products p ON p.id = po.product_id
LEFT JOIN quality_inspections qi ON qi.production_order_id = po.id
ORDER BY po.order_number;
