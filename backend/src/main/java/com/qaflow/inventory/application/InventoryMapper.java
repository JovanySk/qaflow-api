package com.qaflow.inventory.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.qaflow.auth.api.ActorSummaryResponse;
import com.qaflow.auth.application.ActorSummaryResolver;
import com.qaflow.inventory.api.InventoryBalanceResponse;
import com.qaflow.inventory.api.InventoryMovementResponse;
import com.qaflow.inventory.domain.InventoryBalance;
import com.qaflow.inventory.domain.InventoryMovement;
import com.qaflow.material.api.MaterialSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    private final ActorSummaryResolver actorSummaryResolver;

    public InventoryMapper(ActorSummaryResolver actorSummaryResolver) {
        this.actorSummaryResolver = actorSummaryResolver;
    }

    public InventoryBalanceResponse toBalanceResponse(InventoryBalance balance) {
        return toBalanceResponses(List.of(balance)).get(0);
    }

    public List<InventoryBalanceResponse> toBalanceResponses(List<InventoryBalance> balances) {
        HashSet<Long> actorIds = new HashSet<>();
        balances.forEach(balance -> actorIds.add(balance.updatedBy()));
        Map<Long, ActorSummaryResponse> actors = actorSummaryResolver.resolve(actorIds);
        List<InventoryBalanceResponse> responses = new ArrayList<>(balances.size());
        for (InventoryBalance balance : balances) {
            responses.add(new InventoryBalanceResponse(
                    MaterialSummaryResponse.from(balance.material()),
                    balance.onHandQuantity().toPlainString(),
                    balance.reservedQuantity().toPlainString(),
                    balance.availableQuantity().toPlainString(),
                    Long.toString(balance.version()),
                    balance.updatedAt(),
                    actors.get(balance.updatedBy())));
        }
        return List.copyOf(responses);
    }

    public InventoryMovementResponse toMovementResponse(InventoryMovement movement) {
        return toMovementResponses(List.of(movement)).get(0);
    }

    public List<InventoryMovementResponse> toMovementResponses(List<InventoryMovement> movements) {
        HashSet<Long> actorIds = new HashSet<>();
        movements.forEach(movement -> actorIds.add(movement.performedBy()));
        Map<Long, ActorSummaryResponse> actors = actorSummaryResolver.resolve(actorIds);
        List<InventoryMovementResponse> responses = new ArrayList<>(movements.size());
        for (InventoryMovement movement : movements) {
            responses.add(new InventoryMovementResponse(
                    Long.toString(movement.id()),
                    MaterialSummaryResponse.from(movement.material()),
                    movement.movementType(),
                    movement.direction(),
                    movement.quantity().toPlainString(),
                    movement.onHandAfter().toPlainString(),
                    movement.reservedAfter().toPlainString(),
                    movement.productionOrderId() == null ? null : Long.toString(movement.productionOrderId()),
                    movement.referenceCode(),
                    movement.reason(),
                    actors.get(movement.performedBy()),
                    movement.occurredAt(),
                    movement.correlationId()));
        }
        return List.copyOf(responses);
    }
}
