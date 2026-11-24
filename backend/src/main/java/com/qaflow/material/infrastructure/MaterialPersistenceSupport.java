package com.qaflow.material.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MaterialPersistenceSupport {

    private final JdbcTemplate jdbcTemplate;

    public MaterialPersistenceSupport(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createInitialBalance(long materialId, long actorUserId, Instant occurredAt) {
        jdbcTemplate.update(
                "INSERT INTO inventory_balances "
                        + "(material_id, on_hand_quantity, reserved_quantity, version, updated_at, updated_by) "
                        + "VALUES (?, 0, 0, 0, ?, ?)",
                materialId,
                Timestamp.from(occurredAt),
                actorUserId);
    }

    public boolean hasUnitSensitiveHistory(long materialId) {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT CASE WHEN "
                        + "EXISTS (SELECT 1 FROM bom_items WHERE material_id = ?) "
                        + "OR EXISTS (SELECT 1 FROM inventory_movements WHERE material_id = ?) "
                        + "OR EXISTS (SELECT 1 FROM production_order_materials WHERE material_id = ?) "
                        + "THEN 1 ELSE 0 END",
                Integer.class,
                materialId,
                materialId,
                materialId);
        return Integer.valueOf(1).equals(result);
    }
}
