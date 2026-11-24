-- QAFlow Manufacturing Testing Portfolio
-- Phase 3 design artifact: initial MySQL schema.
-- Target: MySQL 8.0.16+ (CHECK constraints enforced).
-- Apply once to an empty, already selected schema. Flyway packaging happens later.

SET time_zone = '+00:00';

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    username_normalized VARCHAR(100)
        GENERATED ALWAYS AS (UPPER(TRIM(username))) STORED,
    display_name VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    auth_version BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BIGINT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_username_normalized UNIQUE (username_normalized),
    CONSTRAINT ck_users_username_not_blank
        CHECK (CHAR_LENGTH(TRIM(username)) BETWEEN 3 AND 100),
    CONSTRAINT ck_users_display_name_not_blank
        CHECK (CHAR_LENGTH(TRIM(display_name)) BETWEEN 1 AND 150),
    CONSTRAINT ck_users_password_hash_length
        CHECK (CHAR_LENGTH(password_hash) BETWEEN 60 AND 255),
    CONSTRAINT ck_users_role
        CHECK (role IN ('ADMIN', 'OPERATOR', 'QUALITY_INSPECTOR')),
    CONSTRAINT ck_users_active CHECK (active IN (0, 1)),
    CONSTRAINT ck_users_auth_version CHECK (auth_version >= 0),
    CONSTRAINT ck_users_version CHECK (version >= 0),
    CONSTRAINT fk_users_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_users_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_users_role_active (role, active),
    INDEX ix_users_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku VARCHAR(64) NOT NULL,
    sku_normalized VARCHAR(64)
        GENERATED ALWAYS AS (UPPER(TRIM(sku))) STORED,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_products_sku_normalized UNIQUE (sku_normalized),
    CONSTRAINT ck_products_sku_not_blank
        CHECK (CHAR_LENGTH(TRIM(sku)) BETWEEN 1 AND 64),
    CONSTRAINT ck_products_name_not_blank
        CHECK (CHAR_LENGTH(TRIM(name)) BETWEEN 1 AND 150),
    CONSTRAINT ck_products_active CHECK (active IN (0, 1)),
    CONSTRAINT ck_products_version CHECK (version >= 0),
    CONSTRAINT fk_products_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_products_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_products_active_name (active, name),
    INDEX ix_products_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE materials (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    code_normalized VARCHAR(64)
        GENERATED ALWAYS AS (UPPER(TRIM(code))) STORED,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    unit_of_measure VARCHAR(32) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_materials_code_normalized UNIQUE (code_normalized),
    CONSTRAINT ck_materials_code_not_blank
        CHECK (CHAR_LENGTH(TRIM(code)) BETWEEN 1 AND 64),
    CONSTRAINT ck_materials_name_not_blank
        CHECK (CHAR_LENGTH(TRIM(name)) BETWEEN 1 AND 150),
    CONSTRAINT ck_materials_unit_of_measure
        CHECK (unit_of_measure IN (
            'PIECE', 'KILOGRAM', 'GRAM',
            'LITER', 'MILLILITER', 'METER', 'CENTIMETER'
        )),
    CONSTRAINT ck_materials_active CHECK (active IN (0, 1)),
    CONSTRAINT ck_materials_version CHECK (version >= 0),
    CONSTRAINT fk_materials_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_materials_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_materials_active_name (active, name),
    INDEX ix_materials_unit (unit_of_measure),
    INDEX ix_materials_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE boms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    revision_number INT NOT NULL,
    is_current TINYINT(1) NOT NULL DEFAULT 1,
    current_product_id BIGINT
        GENERATED ALWAYS AS (
            CASE WHEN is_current = 1 THEN product_id ELSE NULL END
        ) STORED,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BIGINT NOT NULL,
    superseded_at DATETIME(6) NULL,
    superseded_by BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_boms_product_revision UNIQUE (product_id, revision_number),
    CONSTRAINT uq_boms_one_current_per_product UNIQUE (current_product_id),
    CONSTRAINT ck_boms_revision_positive CHECK (revision_number > 0),
    CONSTRAINT ck_boms_is_current CHECK (is_current IN (0, 1)),
    CONSTRAINT ck_boms_superseded_state CHECK (
        (is_current = 1 AND superseded_at IS NULL AND superseded_by IS NULL)
        OR
        (is_current = 0 AND superseded_at IS NOT NULL AND superseded_by IS NOT NULL)
    ),
    CONSTRAINT fk_boms_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_boms_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_boms_superseded_by
        FOREIGN KEY (superseded_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_boms_product_current (product_id, is_current),
    INDEX ix_boms_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE bom_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bom_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    quantity_per_unit DECIMAL(19, 6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_bom_items_bom_material UNIQUE (bom_id, material_id),
    CONSTRAINT ck_bom_items_quantity_positive
        CHECK (quantity_per_unit > 0),
    CONSTRAINT fk_bom_items_bom
        FOREIGN KEY (bom_id) REFERENCES boms (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_bom_items_material
        FOREIGN KEY (material_id) REFERENCES materials (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_bom_items_material (material_id),
    INDEX ix_bom_items_bom (bom_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE inventory_balances (
    material_id BIGINT NOT NULL,
    on_hand_quantity DECIMAL(19, 6) NOT NULL DEFAULT 0,
    reserved_quantity DECIMAL(19, 6) NOT NULL DEFAULT 0,
    available_quantity DECIMAL(19, 6)
        GENERATED ALWAYS AS (on_hand_quantity - reserved_quantity) STORED,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by BIGINT NOT NULL,
    PRIMARY KEY (material_id),
    CONSTRAINT ck_inventory_balances_on_hand_non_negative
        CHECK (on_hand_quantity >= 0),
    CONSTRAINT ck_inventory_balances_reserved_non_negative
        CHECK (reserved_quantity >= 0),
    CONSTRAINT ck_inventory_balances_available_non_negative
        CHECK (on_hand_quantity >= reserved_quantity),
    CONSTRAINT ck_inventory_balances_version CHECK (version >= 0),
    CONSTRAINT fk_inventory_balances_material
        FOREIGN KEY (material_id) REFERENCES materials (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_balances_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_inventory_balances_available (available_quantity),
    INDEX ix_inventory_balances_updated_at (updated_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE production_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(40) NOT NULL,
    order_number_normalized VARCHAR(40)
        GENERATED ALWAYS AS (UPPER(TRIM(order_number))) STORED,
    product_id BIGINT NOT NULL,
    bom_id BIGINT NULL,
    quantity DECIMAL(19, 6) NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_production_orders_number UNIQUE (order_number_normalized),
    CONSTRAINT ck_production_orders_number_not_blank
        CHECK (CHAR_LENGTH(TRIM(order_number)) BETWEEN 1 AND 40),
    CONSTRAINT ck_production_orders_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_production_orders_status CHECK (
        status IN ('DRAFT', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT ck_production_orders_bom_by_status CHECK (
        (status = 'DRAFT' AND bom_id IS NULL)
        OR status = 'CANCELLED'
        OR (status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED') AND bom_id IS NOT NULL)
    ),
    CONSTRAINT ck_production_orders_version CHECK (version >= 0),
    CONSTRAINT fk_production_orders_product
        FOREIGN KEY (product_id) REFERENCES products (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_production_orders_bom
        FOREIGN KEY (bom_id) REFERENCES boms (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_production_orders_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_production_orders_updated_by
        FOREIGN KEY (updated_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_production_orders_status_created (status, created_at),
    INDEX ix_production_orders_product_status (product_id, status),
    INDEX ix_production_orders_bom (bom_id),
    INDEX ix_production_orders_created_by (created_by, created_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE production_order_materials (
    id BIGINT NOT NULL AUTO_INCREMENT,
    production_order_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    quantity_per_unit DECIMAL(19, 6) NOT NULL,
    required_quantity DECIMAL(19, 6) NOT NULL,
    active_reserved_quantity DECIMAL(19, 6) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_order_materials_order_material
        UNIQUE (production_order_id, material_id),
    CONSTRAINT ck_order_materials_quantity_per_unit_positive
        CHECK (quantity_per_unit > 0),
    CONSTRAINT ck_order_materials_required_positive
        CHECK (required_quantity > 0),
    CONSTRAINT ck_order_materials_active_reserved_range
        CHECK (
            active_reserved_quantity >= 0
            AND active_reserved_quantity <= required_quantity
        ),
    CONSTRAINT fk_order_materials_order
        FOREIGN KEY (production_order_id) REFERENCES production_orders (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_order_materials_material
        FOREIGN KEY (material_id) REFERENCES materials (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_order_materials_material (material_id),
    INDEX ix_order_materials_order (production_order_id),
    INDEX ix_order_materials_active_reservation
        (material_id, active_reserved_quantity)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE inventory_movements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    movement_type VARCHAR(32) NOT NULL,
    direction VARCHAR(3) NOT NULL,
    quantity DECIMAL(19, 6) NOT NULL,
    on_hand_after DECIMAL(19, 6) NOT NULL,
    reserved_after DECIMAL(19, 6) NOT NULL,
    production_order_id BIGINT NULL,
    reference_code VARCHAR(120) NULL,
    reason VARCHAR(500) NULL,
    performed_by BIGINT NOT NULL,
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    correlation_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_inventory_movements_order_material_type
        UNIQUE (production_order_id, material_id, movement_type),
    CONSTRAINT ck_inventory_movements_type CHECK (
        movement_type IN (
            'ENTRY', 'MANUAL_OUT', 'ADJUSTMENT_IN',
            'ADJUSTMENT_OUT', 'PRODUCTION_CONSUMPTION'
        )
    ),
    CONSTRAINT ck_inventory_movements_direction CHECK (direction IN ('IN', 'OUT')),
    CONSTRAINT ck_inventory_movements_type_direction CHECK (
        (movement_type IN ('ENTRY', 'ADJUSTMENT_IN') AND direction = 'IN')
        OR
        (movement_type IN ('MANUAL_OUT', 'ADJUSTMENT_OUT', 'PRODUCTION_CONSUMPTION')
         AND direction = 'OUT')
    ),
    CONSTRAINT ck_inventory_movements_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_inventory_movements_after_non_negative CHECK (
        on_hand_after >= 0
        AND reserved_after >= 0
        AND on_hand_after >= reserved_after
    ),
    CONSTRAINT ck_inventory_movements_production_reference CHECK (
        (movement_type = 'PRODUCTION_CONSUMPTION' AND production_order_id IS NOT NULL)
        OR
        (movement_type <> 'PRODUCTION_CONSUMPTION' AND production_order_id IS NULL)
    ),
    CONSTRAINT ck_inventory_movements_manual_reference CHECK (
        movement_type = 'PRODUCTION_CONSUMPTION'
        OR CHAR_LENGTH(TRIM(COALESCE(reference_code, ''))) > 0
        OR CHAR_LENGTH(TRIM(COALESCE(reason, ''))) > 0
    ),
    CONSTRAINT ck_inventory_movements_correlation_not_blank
        CHECK (CHAR_LENGTH(TRIM(correlation_id)) BETWEEN 1 AND 64),
    CONSTRAINT fk_inventory_movements_material
        FOREIGN KEY (material_id) REFERENCES materials (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_movements_order
        FOREIGN KEY (production_order_id) REFERENCES production_orders (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_movements_performed_by
        FOREIGN KEY (performed_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_inventory_movements_material_time (material_id, occurred_at),
    INDEX ix_inventory_movements_type_time (movement_type, occurred_at),
    INDEX ix_inventory_movements_order (production_order_id),
    INDEX ix_inventory_movements_actor_time (performed_by, occurred_at),
    INDEX ix_inventory_movements_correlation (correlation_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE production_order_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    production_order_id BIGINT NOT NULL,
    sequence_number INT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    changed_by BIGINT NOT NULL,
    changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    correlation_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_order_status_history_sequence
        UNIQUE (production_order_id, sequence_number),
    CONSTRAINT ck_order_status_history_sequence_positive
        CHECK (sequence_number > 0),
    CONSTRAINT ck_order_status_history_transition CHECK (
        (from_status IS NULL AND to_status = 'DRAFT')
        OR (from_status = 'DRAFT' AND to_status IN ('CONFIRMED', 'CANCELLED'))
        OR (from_status = 'CONFIRMED' AND to_status IN ('IN_PROGRESS', 'CANCELLED'))
        OR (from_status = 'IN_PROGRESS' AND to_status IN ('COMPLETED', 'CANCELLED'))
    ),
    CONSTRAINT ck_order_status_history_correlation_not_blank
        CHECK (CHAR_LENGTH(TRIM(correlation_id)) BETWEEN 1 AND 64),
    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (production_order_id) REFERENCES production_orders (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_order_status_history_changed_by
        FOREIGN KEY (changed_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_order_status_history_order_time (production_order_id, changed_at),
    INDEX ix_order_status_history_to_status_time (to_status, changed_at),
    INDEX ix_order_status_history_actor_time (changed_by, changed_at),
    INDEX ix_order_status_history_correlation (correlation_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE quality_inspections (
    id BIGINT NOT NULL AUTO_INCREMENT,
    production_order_id BIGINT NOT NULL,
    result VARCHAR(16) NOT NULL,
    observations VARCHAR(1000) NULL,
    inspected_by BIGINT NOT NULL,
    inspected_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    correlation_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_quality_inspections_order UNIQUE (production_order_id),
    CONSTRAINT ck_quality_inspections_result
        CHECK (result IN ('APPROVED', 'REJECTED')),
    CONSTRAINT ck_quality_inspections_rejection_observation CHECK (
        result = 'APPROVED'
        OR (
            result = 'REJECTED'
            AND CHAR_LENGTH(TRIM(COALESCE(observations, ''))) > 0
        )
    ),
    CONSTRAINT ck_quality_inspections_correlation_not_blank
        CHECK (CHAR_LENGTH(TRIM(correlation_id)) BETWEEN 1 AND 64),
    CONSTRAINT fk_quality_inspections_order
        FOREIGN KEY (production_order_id) REFERENCES production_orders (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_quality_inspections_inspector
        FOREIGN KEY (inspected_by) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_quality_inspections_result_time (result, inspected_at),
    INDEX ix_quality_inspections_inspector_time (inspected_by, inspected_at)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    actor_username_snapshot VARCHAR(100) NULL,
    actor_role_snapshot VARCHAR(32) NULL,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NULL,
    outcome VARCHAR(16) NOT NULL,
    summary JSON NULL,
    request_path VARCHAR(255) NULL,
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    correlation_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_audit_events_action_not_blank
        CHECK (CHAR_LENGTH(TRIM(action)) BETWEEN 1 AND 64),
    CONSTRAINT ck_audit_events_resource_type_not_blank
        CHECK (CHAR_LENGTH(TRIM(resource_type)) BETWEEN 1 AND 64),
    CONSTRAINT ck_audit_events_actor_role CHECK (
        actor_role_snapshot IS NULL
        OR actor_role_snapshot IN ('ADMIN', 'OPERATOR', 'QUALITY_INSPECTOR')
    ),
    CONSTRAINT ck_audit_events_outcome
        CHECK (outcome IN ('SUCCESS', 'REJECTED', 'FAILURE')),
    CONSTRAINT ck_audit_events_correlation_not_blank
        CHECK (CHAR_LENGTH(TRIM(correlation_id)) BETWEEN 1 AND 64),
    CONSTRAINT fk_audit_events_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_audit_events_occurred_at (occurred_at),
    INDEX ix_audit_events_actor_time (actor_user_id, occurred_at),
    INDEX ix_audit_events_resource_time (resource_type, resource_id, occurred_at),
    INDEX ix_audit_events_action_outcome_time (action, outcome, occurred_at),
    INDEX ix_audit_events_correlation (correlation_id)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- Immutable-history safeguards. Integration tests use ephemeral schemas;
-- normal application corrections are compensating records, not mutations.
DELIMITER $$

CREATE TRIGGER trg_bom_items_no_update
BEFORE UPDATE ON bom_items
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'BOM items are immutable';
END$$

CREATE TRIGGER trg_bom_items_no_delete
BEFORE DELETE ON bom_items
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'BOM items cannot be deleted';
END$$

CREATE TRIGGER trg_inventory_movements_no_update
BEFORE UPDATE ON inventory_movements
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Inventory movements are immutable';
END$$

CREATE TRIGGER trg_inventory_movements_no_delete
BEFORE DELETE ON inventory_movements
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Inventory movements cannot be deleted';
END$$

CREATE TRIGGER trg_order_status_history_no_update
BEFORE UPDATE ON production_order_status_history
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Order status history is immutable';
END$$

CREATE TRIGGER trg_order_status_history_no_delete
BEFORE DELETE ON production_order_status_history
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Order status history cannot be deleted';
END$$

CREATE TRIGGER trg_quality_inspections_no_update
BEFORE UPDATE ON quality_inspections
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Quality inspections are immutable';
END$$

CREATE TRIGGER trg_quality_inspections_no_delete
BEFORE DELETE ON quality_inspections
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Quality inspections cannot be deleted';
END$$

CREATE TRIGGER trg_audit_events_no_update
BEFORE UPDATE ON audit_events
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Audit events are immutable';
END$$

CREATE TRIGGER trg_audit_events_no_delete
BEFORE DELETE ON audit_events
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Audit events cannot be deleted';
END$$

DELIMITER ;
