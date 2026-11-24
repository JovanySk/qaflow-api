package com.qaflow.inventory.domain;

public enum InventoryMovementType {
    ENTRY(InventoryDirection.IN, true),
    MANUAL_OUT(InventoryDirection.OUT, true),
    ADJUSTMENT_IN(InventoryDirection.IN, true),
    ADJUSTMENT_OUT(InventoryDirection.OUT, true),
    PRODUCTION_CONSUMPTION(InventoryDirection.OUT, false);

    private final InventoryDirection direction;
    private final boolean manual;

    InventoryMovementType(InventoryDirection direction, boolean manual) {
        this.direction = direction;
        this.manual = manual;
    }

    public InventoryDirection direction() {
        return direction;
    }

    public boolean manual() {
        return manual;
    }
}
