package com.qaflow.inventory.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.qaflow.audit.application.AuditEventCommand;
import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.audit.application.DeferredAuditEventRecorder;
import com.qaflow.audit.domain.AuditOutcome;
import com.qaflow.inventory.api.CreateInventoryMovementRequest;
import com.qaflow.inventory.api.InventoryBalancePageResponse;
import com.qaflow.inventory.api.InventoryBalanceResponse;
import com.qaflow.inventory.api.InventoryMovementPageResponse;
import com.qaflow.inventory.api.InventoryMovementResponse;
import com.qaflow.inventory.domain.InventoryBalance;
import com.qaflow.inventory.domain.InventoryMovement;
import com.qaflow.inventory.domain.InventoryMovementType;
import com.qaflow.inventory.infrastructure.InventoryBalanceRepository;
import com.qaflow.inventory.infrastructure.InventoryMovementRepository;
import com.qaflow.material.domain.Material;
import com.qaflow.material.infrastructure.MaterialRepository;
import com.qaflow.security.QaFlowPrincipal;
import com.qaflow.shared.api.PageMetadataResponse;
import com.qaflow.shared.error.BadRequestException;
import com.qaflow.shared.error.BusinessConflictException;
import com.qaflow.shared.error.ResourceNotFoundException;
import com.qaflow.shared.validation.ExactQuantityParser;
import com.qaflow.shared.web.RequestMetadata;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final MaterialRepository materialRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryMapper inventoryMapper;
    private final AuditEventRecorder auditEventRecorder;
    private final DeferredAuditEventRecorder deferredAuditEventRecorder;
    private final Clock clock;

    public InventoryService(
            MaterialRepository materialRepository,
            InventoryBalanceRepository balanceRepository,
            InventoryMovementRepository movementRepository,
            InventoryMapper inventoryMapper,
            AuditEventRecorder auditEventRecorder,
            DeferredAuditEventRecorder deferredAuditEventRecorder,
            Clock clock) {
        this.materialRepository = materialRepository;
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
        this.inventoryMapper = inventoryMapper;
        this.auditEventRecorder = auditEventRecorder;
        this.deferredAuditEventRecorder = deferredAuditEventRecorder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public InventoryBalancePageResponse listBalances(
            int page,
            int size,
            String sort,
            String search,
            Boolean active,
            Boolean outOfStock) {
        Specification<InventoryBalance> specification = (root, query, builder) -> builder.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toUpperCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(root.join("material").get("codeNormalized"), pattern),
                    builder.like(builder.upper(root.join("material").get("name")), pattern)));
        }
        if (active != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.join("material").get("active"), active));
        }
        if (outOfStock != null) {
            specification = specification.and((root, query, builder) -> outOfStock
                    ? builder.equal(root.get("availableQuantity"), BigDecimal.ZERO)
                    : builder.greaterThan(root.get("availableQuantity"), BigDecimal.ZERO));
        }
        Page<InventoryBalance> result = balanceRepository.findAll(
                specification,
                PageRequest.of(page, size, InventoryBalanceSort.parse(sort)));
        return new InventoryBalancePageResponse(
                inventoryMapper.toBalanceResponses(result.getContent()),
                pageMetadata(result));
    }

    @Transactional(readOnly = true)
    public InventoryBalanceResponse getBalance(String materialId) {
        long id = parseId(materialId, "materialId");
        InventoryBalance balance = balanceRepository.findDetailByMaterialId(id)
                .orElseThrow(() -> materialNotFound(materialId));
        return inventoryMapper.toBalanceResponse(balance);
    }

    @Transactional(readOnly = true)
    public InventoryMovementPageResponse listMovements(
            int page,
            int size,
            String sort,
            String materialId,
            InventoryMovementType type,
            String productionOrderId,
            String performedBy,
            Instant from,
            Instant to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new BadRequestException(
                    "INVALID_DATE_RANGE",
                    "from debe ser anterior a to; from es inclusivo y to exclusivo.");
        }
        Specification<InventoryMovement> specification = (root, query, builder) -> builder.conjunction();
        if (materialId != null) {
            long id = parseId(materialId, "materialId");
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("material").get("id"), id));
        }
        if (type != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("movementType"), type));
        }
        if (productionOrderId != null) {
            long id = parseId(productionOrderId, "productionOrderId");
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("productionOrderId"), id));
        }
        if (performedBy != null) {
            long id = parseId(performedBy, "performedBy");
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("performedBy"), id));
        }
        if (from != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThan(root.get("occurredAt"), to));
        }
        Page<InventoryMovement> result = movementRepository.findAll(
                specification,
                PageRequest.of(page, size, InventoryMovementSort.parse(sort)));
        return new InventoryMovementPageResponse(
                inventoryMapper.toMovementResponses(result.getContent()),
                pageMetadata(result));
    }

    @Transactional(readOnly = true)
    public InventoryMovementResponse getMovement(String movementId) {
        long id = parseId(movementId, "movementId");
        InventoryMovement movement = movementRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RESOURCE_NOT_FOUND",
                        "No existe un movimiento de inventario con id " + movementId + "."));
        return inventoryMapper.toMovementResponse(movement);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InventoryMovementResponse createMovement(
            CreateInventoryMovementRequest request,
            QaFlowPrincipal actor,
            RequestMetadata metadata) {
        long materialId = parseId(request.materialId(), "materialId");
        BigDecimal quantity = ExactQuantityParser.parsePositive(request.quantity(), "quantity");
        InventoryMovementType type = InventoryMovementType.valueOf(request.type());
        if (!type.manual()) {
            reject(actor, Long.toString(materialId), "PRODUCTION_MOVEMENT_NOT_MANUAL", metadata);
            throw new BadRequestException(
                    "VALIDATION_ERROR",
                    "PRODUCTION_CONSUMPTION solo puede generarse al completar una orden de producción.");
        }

        try {
            Material material = materialRepository.findAllByIdForUpdate(List.of(materialId)).stream()
                    .findFirst()
                    .orElse(null);
            if (material == null) {
                reject(actor, Long.toString(materialId), "RESOURCE_NOT_FOUND", metadata);
                throw materialNotFound(request.materialId());
            }
            if (!material.active()) {
                reject(actor, Long.toString(materialId), "RESOURCE_INACTIVE", metadata);
                throw new BusinessConflictException(
                        "RESOURCE_INACTIVE",
                        "No pueden registrarse movimientos para un material inactivo.");
            }
            InventoryBalance balance = balanceRepository.findByMaterialIdForUpdate(materialId).orElse(null);
            if (balance == null) {
                reject(actor, Long.toString(materialId), "INVENTORY_BALANCE_MISSING", metadata);
                throw new BusinessConflictException(
                        "INVENTORY_BALANCE_MISSING",
                        "El material no tiene un balance de inventario inicial.");
            }
            Instant now = Instant.now(clock);
            try {
                balance.apply(type, quantity, now, actor.id());
            } catch (InventoryBalance.InsufficientStockException exception) {
                reject(actor, Long.toString(materialId), "INSUFFICIENT_STOCK", metadata);
                throw new BusinessConflictException(
                        "INSUFFICIENT_STOCK",
                        "El inventario disponible no cubre la salida solicitada.");
            } catch (InventoryBalance.QuantityOverflowException exception) {
                reject(actor, Long.toString(materialId), "QUANTITY_OVERFLOW", metadata);
                throw new BusinessConflictException(
                        "QUANTITY_OVERFLOW",
                        "La entrada excede la capacidad máxima del saldo DECIMAL(19,6).");
            }

            balanceRepository.saveAndFlush(balance);
            InventoryMovement movement = InventoryMovement.manual(
                    material,
                    type,
                    quantity,
                    balance,
                    request.referenceCode(),
                    request.reason(),
                    actor.id(),
                    now,
                    metadata.correlationId());
            movementRepository.saveAndFlush(movement);
            success(actor, movement, metadata);
            return inventoryMapper.toMovementResponse(movement);
        } catch (PessimisticLockingFailureException exception) {
            reject(actor, Long.toString(materialId), "CONCURRENT_MODIFICATION", metadata);
            throw new BusinessConflictException(
                    "CONCURRENT_MODIFICATION",
                    "Otro proceso está modificando el inventario; vuelve a intentar.");
        } catch (DataIntegrityViolationException exception) {
            reject(actor, Long.toString(materialId), "NEGATIVE_INVENTORY_CONFLICT", metadata);
            throw new BusinessConflictException(
                    "NEGATIVE_INVENTORY_CONFLICT",
                    "La operación fue rechazada para conservar la integridad del inventario.");
        }
    }

    private PageMetadataResponse pageMetadata(Page<?> page) {
        return new PageMetadataResponse(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    private void success(QaFlowPrincipal actor, InventoryMovement movement, RequestMetadata metadata) {
        auditEventRecorder.recordWithinBusinessTransaction(new AuditEventCommand(
                actor.id(),
                actor.username(),
                actor.role().name(),
                "INVENTORY_MOVEMENT_CREATED",
                "INVENTORY_MOVEMENT",
                Long.toString(movement.id()),
                AuditOutcome.SUCCESS,
                Map.of(
                        "materialId", Long.toString(movement.material().id()),
                        "type", movement.movementType().name(),
                        "quantity", movement.quantity().toPlainString()),
                metadata.path(),
                metadata.correlationId()));
    }

    private void reject(QaFlowPrincipal actor, String resourceId, String reason, RequestMetadata metadata) {
        deferredAuditEventRecorder.recordAfterCurrentTransaction(new AuditEventCommand(
                actor.id(),
                actor.username(),
                actor.role().name(),
                "INVENTORY_MOVEMENT_REJECTED",
                "INVENTORY",
                resourceId,
                AuditOutcome.REJECTED,
                Map.of("reason", reason),
                metadata.path(),
                metadata.correlationId()));
    }

    private long parseId(String value, String field) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new BadRequestException(
                    "INVALID_QUERY_PARAMETER",
                    field + " debe ser un entero positivo.");
        }
    }

    private ResourceNotFoundException materialNotFound(String materialId) {
        return new ResourceNotFoundException(
                "RESOURCE_NOT_FOUND",
                "No existe un material con id " + materialId + ".");
    }
}
