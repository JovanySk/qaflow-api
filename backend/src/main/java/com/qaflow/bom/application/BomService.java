package com.qaflow.bom.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.qaflow.audit.application.AuditEventCommand;
import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.audit.application.DeferredAuditEventRecorder;
import com.qaflow.audit.domain.AuditOutcome;
import com.qaflow.bom.api.BomItemRequest;
import com.qaflow.bom.api.BomResponse;
import com.qaflow.bom.api.BomSummaryPageResponse;
import com.qaflow.bom.api.PublishBomRequest;
import com.qaflow.bom.domain.Bom;
import com.qaflow.bom.infrastructure.BomItemRepository;
import com.qaflow.bom.infrastructure.BomRepository;
import com.qaflow.material.domain.Material;
import com.qaflow.material.infrastructure.MaterialRepository;
import com.qaflow.product.domain.Product;
import com.qaflow.product.infrastructure.ProductRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BomService {

    private final BomRepository bomRepository;
    private final BomItemRepository bomItemRepository;
    private final ProductRepository productRepository;
    private final MaterialRepository materialRepository;
    private final BomMapper bomMapper;
    private final AuditEventRecorder auditEventRecorder;
    private final DeferredAuditEventRecorder deferredAuditEventRecorder;
    private final Clock clock;

    public BomService(
            BomRepository bomRepository,
            BomItemRepository bomItemRepository,
            ProductRepository productRepository,
            MaterialRepository materialRepository,
            BomMapper bomMapper,
            AuditEventRecorder auditEventRecorder,
            DeferredAuditEventRecorder deferredAuditEventRecorder,
            Clock clock) {
        this.bomRepository = bomRepository;
        this.bomItemRepository = bomItemRepository;
        this.productRepository = productRepository;
        this.materialRepository = materialRepository;
        this.bomMapper = bomMapper;
        this.auditEventRecorder = auditEventRecorder;
        this.deferredAuditEventRecorder = deferredAuditEventRecorder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public BomSummaryPageResponse list(String productId, int page, int size, String sort) {
        long id = parsePositiveLong(productId, "productId");
        requireProduct(id, productId);
        Page<Bom> result = bomRepository.findAllByProduct_Id(
                id,
                PageRequest.of(page, size, BomSort.parse(sort)));
        Map<Long, Long> itemCounts = itemCounts(result.getContent());
        return new BomSummaryPageResponse(
                bomMapper.toSummaryResponses(result.getContent(), itemCounts),
                new PageMetadataResponse(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.isFirst(),
                        result.isLast()));
    }

    @Transactional(readOnly = true)
    public BomResponse getCurrent(String productId) {
        long id = parsePositiveLong(productId, "productId");
        requireProduct(id, productId);
        Bom bom = bomRepository.findCurrentDetail(id).orElseThrow(() -> new ResourceNotFoundException(
                "CURRENT_BOM_NOT_FOUND",
                "El producto no tiene una BOM vigente."));
        return bomMapper.toResponse(bom);
    }

    @Transactional(readOnly = true)
    public BomResponse getRevision(String productId, String revisionNumber) {
        long id = parsePositiveLong(productId, "productId");
        int revision = parsePositiveInt(revisionNumber, "revisionNumber");
        requireProduct(id, productId);
        Bom bom = bomRepository.findRevisionDetail(id, revision).orElseThrow(() ->
                new ResourceNotFoundException(
                        "RESOURCE_NOT_FOUND",
                        "No existe la revisión " + revisionNumber + " para el producto " + productId + "."));
        return bomMapper.toResponse(bom);
    }

    @Transactional
    public BomResponse publish(
            String productId,
            PublishBomRequest request,
            QaFlowPrincipal actor,
            RequestMetadata metadata) {
        long id = parsePositiveLong(productId, "productId");
        List<RequestedItem> requestedItems = parseItems(request.items(), actor, productId, metadata);
        Optional<Long> observedCurrentId = bomRepository.findCurrentId(id);
        try {
            Product product = productRepository.findByIdForUpdate(id).orElseThrow(() -> {
                reject(actor, productId, "PRODUCT_NOT_FOUND", metadata);
                return productNotFound(productId);
            });
            if (!product.active()) {
                reject(actor, productId, "PRODUCT_INACTIVE", metadata);
                throw inactiveResource("El producto debe estar activo para publicar una BOM.");
            }

            Optional<Bom> lockedCurrent = bomRepository.findCurrentForUpdate(id);
            Long lockedCurrentId = lockedCurrent.map(Bom::id).orElse(null);
            if (!Objects.equals(observedCurrentId.orElse(null), lockedCurrentId)) {
                reject(actor, productId, "BOM_CONCURRENTLY_CHANGED", metadata);
                throw concurrentChange();
            }

            List<Long> sortedMaterialIds = requestedItems.stream()
                    .map(RequestedItem::materialId)
                    .sorted()
                    .toList();
            Map<Long, Material> materials = new LinkedHashMap<>();
            materialRepository.findAllByIdForUpdate(sortedMaterialIds)
                    .forEach(material -> materials.put(material.id(), material));
            for (RequestedItem requestedItem : requestedItems) {
                Material material = materials.get(requestedItem.materialId());
                if (material == null) {
                    reject(actor, productId, "MATERIAL_NOT_FOUND", metadata);
                    throw new ResourceNotFoundException(
                            "RESOURCE_NOT_FOUND",
                            "No existe un material con id " + requestedItem.materialId() + ".");
                }
                if (!material.active()) {
                    reject(actor, productId, "MATERIAL_INACTIVE", metadata);
                    throw inactiveResource(
                            "El material " + requestedItem.materialId() + " debe estar activo para publicar la BOM.");
                }
            }

            int maxRevision = bomRepository.findMaxRevisionNumber(id);
            if (maxRevision == Integer.MAX_VALUE) {
                reject(actor, productId, "BOM_REVISION_LIMIT_REACHED", metadata);
                throw new BusinessConflictException(
                        "BOM_REVISION_LIMIT_REACHED",
                        "El producto alcanzó el límite de revisiones permitido.");
            }
            Instant now = Instant.now(clock);
            lockedCurrent.ifPresent(current -> {
                current.supersede(now, actor.id());
                bomRepository.saveAndFlush(current);
            });

            Bom bom = Bom.create(product, maxRevision + 1, now, actor.id());
            requestedItems.forEach(item -> bom.addItem(
                    materials.get(item.materialId()),
                    item.quantityPerUnit(),
                    now));
            bomRepository.saveAndFlush(bom);
            success(actor, bom, metadata);
            return bomMapper.toResponse(bom);
        } catch (DataIntegrityViolationException | PessimisticLockingFailureException exception) {
            reject(actor, productId, "BOM_CONCURRENTLY_CHANGED", metadata);
            throw concurrentChange();
        }
    }

    private List<RequestedItem> parseItems(
            List<BomItemRequest> items,
            QaFlowPrincipal actor,
            String productId,
            RequestMetadata metadata) {
        if (items == null || items.isEmpty() || items.size() > 100) {
            reject(actor, productId, "INVALID_BOM_SIZE", metadata);
            throw new BadRequestException(
                    "VALIDATION_ERROR",
                    "La BOM debe contener entre 1 y 100 materiales.");
        }
        HashSet<Long> uniqueMaterialIds = new HashSet<>();
        return items.stream().map(item -> {
            long materialId = parsePositiveLong(item.materialId(), "materialId");
            if (!uniqueMaterialIds.add(materialId)) {
                reject(actor, productId, "DUPLICATE_BOM_MATERIAL", metadata);
                throw new BadRequestException(
                        "DUPLICATE_BOM_MATERIAL",
                        "Un material no puede repetirse dentro de la misma BOM.");
            }
            BigDecimal quantity = ExactQuantityParser.parsePositive(
                    item.quantityPerUnit(),
                    "quantityPerUnit");
            return new RequestedItem(materialId, quantity);
        }).toList();
    }

    private Map<Long, Long> itemCounts(List<Bom> boms) {
        if (boms.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = boms.stream().map(Bom::id).toList();
        HashMap<Long, Long> counts = new HashMap<>();
        for (Object[] row : bomItemRepository.countItemsByBomIds(ids)) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return Map.copyOf(counts);
    }

    private Product requireProduct(long id, String originalId) {
        return productRepository.findById(id).orElseThrow(() -> productNotFound(originalId));
    }

    private void success(QaFlowPrincipal actor, Bom bom, RequestMetadata metadata) {
        auditEventRecorder.recordWithinBusinessTransaction(new AuditEventCommand(
                actor.id(),
                actor.username(),
                actor.role().name(),
                "BOM_PUBLISHED",
                "BOM",
                Long.toString(bom.id()),
                AuditOutcome.SUCCESS,
                Map.of(
                        "productId", Long.toString(bom.product().id()),
                        "revisionNumber", bom.revisionNumber(),
                        "itemCount", bom.items().size()),
                metadata.path(),
                metadata.correlationId()));
    }

    private void reject(
            QaFlowPrincipal actor,
            String productId,
            String reason,
            RequestMetadata metadata) {
        deferredAuditEventRecorder.recordAfterCurrentTransaction(new AuditEventCommand(
                actor.id(),
                actor.username(),
                actor.role().name(),
                "BOM_PUBLISH_REJECTED",
                "BOM",
                productId,
                AuditOutcome.REJECTED,
                Map.of("reason", reason),
                metadata.path(),
                metadata.correlationId()));
    }

    private long parsePositiveLong(String value, String field) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new NumberFormatException("not positive");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new BadRequestException(
                    "INVALID_PATH_PARAMETER",
                    field + " debe ser un entero positivo.");
        }
    }

    private int parsePositiveInt(String value, String field) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException("not positive");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new BadRequestException(
                    "INVALID_PATH_PARAMETER",
                    field + " debe ser un entero positivo.");
        }
    }

    private ResourceNotFoundException productNotFound(String productId) {
        return new ResourceNotFoundException(
                "RESOURCE_NOT_FOUND",
                "No existe un producto con id " + productId + ".");
    }

    private BusinessConflictException inactiveResource(String message) {
        return new BusinessConflictException("RESOURCE_INACTIVE", message);
    }

    private BusinessConflictException concurrentChange() {
        return new BusinessConflictException(
                "BOM_CONCURRENTLY_CHANGED",
                "La BOM vigente cambió durante la publicación. Consulta la revisión actual e intenta de nuevo.");
    }

    private record RequestedItem(long materialId, BigDecimal quantityPerUnit) {
    }
}
