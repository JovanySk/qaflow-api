-- QAFlow Fase 11 - validaciones de integridad del inventario (MySQL 8).
-- Cada consulta marcada como "esperado: 0 filas" señala una violación cuando devuelve datos.

-- V-INV-001: todo material debe tener exactamente un saldo y no debe haber saldos huérfanos.
-- Esperado: 0 filas.
SELECT m.id AS material_id, COUNT(ib.material_id) AS balance_count
FROM materials m
LEFT JOIN inventory_balances ib ON ib.material_id = m.id
GROUP BY m.id
HAVING COUNT(ib.material_id) <> 1;

-- V-INV-002: cantidades del saldo no negativas y fórmula disponible consistente.
-- Esperado: 0 filas.
SELECT material_id, on_hand_quantity, reserved_quantity, available_quantity
FROM inventory_balances
WHERE on_hand_quantity < 0
   OR reserved_quantity < 0
   OR available_quantity < 0
   OR on_hand_quantity < reserved_quantity
   OR available_quantity <> on_hand_quantity - reserved_quantity;

-- V-INV-003: el saldo físico debe reconciliarse con la suma firmada del ledger.
-- Esperado: 0 filas.
SELECT ib.material_id,
       ib.on_hand_quantity,
       COALESCE(SUM(CASE im.direction WHEN 'IN' THEN im.quantity ELSE -im.quantity END), 0) AS ledger_quantity
FROM inventory_balances ib
LEFT JOIN inventory_movements im ON im.material_id = ib.material_id
GROUP BY ib.material_id, ib.on_hand_quantity
HAVING ib.on_hand_quantity <> ledger_quantity;

-- V-INV-004: la fotografía física de cada movimiento debe coincidir con el acumulado cronológico.
-- Esperado: 0 filas.
WITH movement_running_balance AS (
    SELECT im.id,
           im.material_id,
           im.on_hand_after,
           SUM(CASE im.direction WHEN 'IN' THEN im.quantity ELSE -im.quantity END)
               OVER (PARTITION BY im.material_id ORDER BY im.occurred_at, im.id) AS calculated_on_hand
    FROM inventory_movements im
)
SELECT id, material_id, on_hand_after, calculated_on_hand
FROM movement_running_balance
WHERE on_hand_after <> calculated_on_hand;

-- V-INV-005: tipo, dirección, cantidad, referencias y fotografías válidas.
-- Esperado: 0 filas.
SELECT id, material_id, movement_type, direction, quantity
FROM inventory_movements
WHERE quantity <= 0
   OR on_hand_after < 0
   OR reserved_after < 0
   OR on_hand_after < reserved_after
   OR (movement_type IN ('ENTRY', 'ADJUSTMENT_IN') AND direction <> 'IN')
   OR (movement_type IN ('MANUAL_OUT', 'ADJUSTMENT_OUT', 'PRODUCTION_CONSUMPTION') AND direction <> 'OUT')
   OR (movement_type = 'PRODUCTION_CONSUMPTION' AND production_order_id IS NULL)
   OR (movement_type <> 'PRODUCTION_CONSUMPTION' AND production_order_id IS NOT NULL)
   OR (movement_type <> 'PRODUCTION_CONSUMPTION'
       AND CHAR_LENGTH(TRIM(COALESCE(reference_code, ''))) = 0
       AND CHAR_LENGTH(TRIM(COALESCE(reason, ''))) = 0);

-- V-INV-006: no debe haber referencias huérfanas aun si las FK fueran deshabilitadas temporalmente.
-- Esperado: 0 filas.
SELECT im.id, im.material_id, im.performed_by, im.production_order_id
FROM inventory_movements im
LEFT JOIN materials m ON m.id = im.material_id
LEFT JOIN users u ON u.id = im.performed_by
LEFT JOIN production_orders po ON po.id = im.production_order_id
WHERE m.id IS NULL
   OR u.id IS NULL
   OR (im.production_order_id IS NOT NULL AND po.id IS NULL);

-- V-INV-007: las reservas del saldo deben reconciliarse con reservas activas de órdenes.
-- Esperado: 0 filas. En la Fase 11 ambas cantidades son cero; la Fase 12 poblará las reservas.
SELECT ib.material_id,
       ib.reserved_quantity,
       COALESCE(SUM(pom.active_reserved_quantity), 0) AS order_reserved_quantity
FROM inventory_balances ib
LEFT JOIN production_order_materials pom ON pom.material_id = ib.material_id
GROUP BY ib.material_id, ib.reserved_quantity
HAVING ib.reserved_quantity <> order_reserved_quantity;

-- V-INV-008: cada movimiento confirmado por la API debe tener auditoría exitosa correlacionada.
-- Esperado: 0 filas.
SELECT im.id, im.correlation_id
FROM inventory_movements im
LEFT JOIN audit_events ae
       ON ae.correlation_id = im.correlation_id
      AND ae.action = 'INVENTORY_MOVEMENT_CREATED'
      AND ae.resource_type = 'INVENTORY_MOVEMENT'
      AND ae.resource_id = CAST(im.id AS CHAR)
      AND ae.outcome = 'SUCCESS'
WHERE ae.id IS NULL;

-- V-INV-009: deben existir los dos triggers que hacen inmutable el ledger.
-- Esperado: trigger_count = 2.
SELECT COUNT(*) AS trigger_count
FROM information_schema.triggers
WHERE trigger_schema = DATABASE()
  AND trigger_name IN ('trg_inventory_movements_no_update', 'trg_inventory_movements_no_delete');

-- V-INV-010: vista operativa para revisar los últimos movimientos y sus saldos posteriores.
SELECT im.id,
       m.code AS material_code,
       im.movement_type,
       im.direction,
       im.quantity,
       im.on_hand_after,
       im.reserved_after,
       im.performed_by,
       im.occurred_at,
       im.correlation_id
FROM inventory_movements im
JOIN materials m ON m.id = im.material_id
ORDER BY im.occurred_at DESC, im.id DESC
LIMIT 100;
