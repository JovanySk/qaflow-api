package com.qaflow.inventory.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.audit.application.DeferredAuditEventRecorder;
import com.qaflow.inventory.api.CreateInventoryMovementRequest;
import com.qaflow.inventory.api.InventoryMovementResponse;
import com.qaflow.inventory.domain.InventoryBalance;
import com.qaflow.inventory.domain.InventoryMovement;
import com.qaflow.inventory.infrastructure.InventoryBalanceRepository;
import com.qaflow.inventory.infrastructure.InventoryMovementRepository;
import com.qaflow.material.domain.Material;
import com.qaflow.material.infrastructure.MaterialRepository;
import com.qaflow.security.QaFlowPrincipal;
import com.qaflow.shared.error.BadRequestException;
import com.qaflow.shared.error.BusinessConflictException;
import com.qaflow.shared.web.RequestMetadata;
import com.qaflow.user.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class InventoryServiceTest {

    private MaterialRepository materialRepository;
    private InventoryBalanceRepository balanceRepository;
    private InventoryMovementRepository movementRepository;
    private InventoryMapper inventoryMapper;
    private AuditEventRecorder auditEventRecorder;
    private DeferredAuditEventRecorder deferredAuditEventRecorder;
    private InventoryService service;
    private QaFlowPrincipal operator;
    private RequestMetadata metadata;

    @BeforeEach
    void setUp() {
        materialRepository = mock(MaterialRepository.class);
        balanceRepository = mock(InventoryBalanceRepository.class);
        movementRepository = mock(InventoryMovementRepository.class);
        inventoryMapper = mock(InventoryMapper.class);
        auditEventRecorder = mock(AuditEventRecorder.class);
        deferredAuditEventRecorder = mock(DeferredAuditEventRecorder.class);
        service = new InventoryService(
                materialRepository,
                balanceRepository,
                movementRepository,
                inventoryMapper,
                auditEventRecorder,
                deferredAuditEventRecorder,
                Clock.fixed(Instant.parse("2026-07-20T10:00:00Z"), ZoneOffset.UTC));
        operator = new QaFlowPrincipal(2L, "operator.demo", "Demo Operator", Role.OPERATOR, 0L);
        metadata = new RequestMetadata("/api/v1/inventory/movements", "inventory-unit-001");
    }

    @Test
    void createsManualMovementAndPersistsBalanceLedgerAndAudit() {
        Material material = mock(Material.class);
        when(material.id()).thenReturn(7L);
        when(material.active()).thenReturn(true);
        InventoryBalance balance = mock(InventoryBalance.class);
        when(materialRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(material));
        when(balanceRepository.findByMaterialIdForUpdate(7L)).thenReturn(Optional.of(balance));
        doAnswer(invocation -> {
            InventoryMovement movement = invocation.getArgument(0, InventoryMovement.class);
            ReflectionTestUtils.setField(movement, "id", 15L);
            return movement;
        }).when(movementRepository).saveAndFlush(any(InventoryMovement.class));
        InventoryMovementResponse expected = mock(InventoryMovementResponse.class);
        when(inventoryMapper.toMovementResponse(any(InventoryMovement.class))).thenReturn(expected);

        service.createMovement(request("ENTRY", "5.250000"), operator, metadata);

        verify(balance).apply(any(), any(), any(), org.mockito.ArgumentMatchers.eq(2L));
        verify(balanceRepository).saveAndFlush(balance);
        verify(movementRepository).saveAndFlush(any(InventoryMovement.class));
        verify(auditEventRecorder).recordWithinBusinessTransaction(any());
    }

    @Test
    void rejectsProductionConsumptionThroughManualEndpointBeforeLockingData() {
        assertThatThrownBy(() -> service.createMovement(
                request("PRODUCTION_CONSUMPTION", "1"), operator, metadata))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("solo puede generarse");

        verify(materialRepository, never()).findAllByIdForUpdate(any());
        verify(deferredAuditEventRecorder).recordAfterCurrentTransaction(any());
    }

    @Test
    void rejectsInactiveMaterialWithoutChangingBalance() {
        Material material = mock(Material.class);
        when(material.active()).thenReturn(false);
        when(materialRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(material));

        assertThatThrownBy(() -> service.createMovement(request("ENTRY", "1"), operator, metadata))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("material inactivo");

        verify(balanceRepository, never()).findByMaterialIdForUpdate(anyLong());
        verify(movementRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsInsufficientStockWithoutPersistingPartialChanges() {
        Material material = mock(Material.class);
        when(material.active()).thenReturn(true);
        InventoryBalance balance = mock(InventoryBalance.class);
        when(materialRepository.findAllByIdForUpdate(List.of(7L))).thenReturn(List.of(material));
        when(balanceRepository.findByMaterialIdForUpdate(7L)).thenReturn(Optional.of(balance));
        doThrow(new InventoryBalance.InsufficientStockException())
                .when(balance).apply(any(), any(), any(), org.mockito.ArgumentMatchers.eq(2L));

        assertThatThrownBy(() -> service.createMovement(request("MANUAL_OUT", "9"), operator, metadata))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("no cubre");

        verify(balanceRepository, never()).saveAndFlush(any());
        verify(movementRepository, never()).saveAndFlush(any());
        verify(deferredAuditEventRecorder).recordAfterCurrentTransaction(any());
    }

    private CreateInventoryMovementRequest request(String type, String quantity) {
        return new CreateInventoryMovementRequest("7", type, quantity, "REF-UNIT", null);
    }
}
