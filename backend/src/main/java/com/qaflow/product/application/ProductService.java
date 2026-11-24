package com.qaflow.product.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import com.qaflow.audit.application.AuditEventCommand;
import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.audit.application.DeferredAuditEventRecorder;
import com.qaflow.audit.domain.AuditOutcome;
import com.qaflow.product.api.CreateProductRequest;
import com.qaflow.product.api.ProductPageResponse;
import com.qaflow.product.api.ProductResponse;
import com.qaflow.product.api.UpdateProductRequest;
import com.qaflow.product.domain.Product;
import com.qaflow.product.infrastructure.ProductRepository;
import com.qaflow.security.QaFlowPrincipal;
import com.qaflow.shared.api.PageMetadataResponse;
import com.qaflow.shared.error.BadRequestException;
import com.qaflow.shared.error.BusinessConflictException;
import com.qaflow.shared.error.ResourceNotFoundException;
import com.qaflow.shared.web.RequestMetadata;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final AuditEventRecorder auditEventRecorder;
    private final DeferredAuditEventRecorder deferredAuditEventRecorder;
    private final Clock clock;

    public ProductService(
            ProductRepository productRepository,
            ProductMapper productMapper,
            AuditEventRecorder auditEventRecorder,
            DeferredAuditEventRecorder deferredAuditEventRecorder,
            Clock clock) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.auditEventRecorder = auditEventRecorder;
        this.deferredAuditEventRecorder = deferredAuditEventRecorder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProductPageResponse list(int page, int size, String sort, String search, Boolean active) {
        Specification<Product> specification = (root, query, builder) -> builder.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toUpperCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(root.get("skuNormalized"), pattern),
                    builder.like(builder.upper(root.get("name")), pattern)));
        }
        if (active != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("active"), active));
        }
        Page<Product> result = productRepository.findAll(
                specification,
                PageRequest.of(page, size, ProductSort.parse(sort)));
        return new ProductPageResponse(
                productMapper.toResponses(result.getContent()),
                new PageMetadataResponse(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.isFirst(),
                        result.isLast()));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(String productId) {
        Product product = productRepository.findById(parseId(productId, "productId"))
                .orElseThrow(() -> notFound(productId));
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse create(
            CreateProductRequest request,
            QaFlowPrincipal actor,
            RequestMetadata metadata) {
        String normalizedSku = normalize(request.sku());
        if (productRepository.existsBySkuNormalized(normalizedSku)) {
            reject(actor, "PRODUCT_CREATE_REJECTED", null, "DUPLICATE_SKU", metadata);
            throw duplicateSku();
        }
        Product product = Product.create(
                request.sku(),
                request.name(),
                request.description(),
                request.activeOrDefault(),
                Instant.now(clock),
                actor.id());
        try {
            productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException exception) {
            reject(actor, "PRODUCT_CREATE_REJECTED", null, "DUPLICATE_SKU", metadata);
            throw duplicateSku();
        }
        success(actor, "PRODUCT_CREATED", product, metadata);
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse update(
            String productId,
            UpdateProductRequest request,
            QaFlowPrincipal actor,
            RequestMetadata metadata) {
        long id = parseId(productId, "productId");
        Product product = productRepository.findById(id).orElseThrow(() -> notFound(productId));
        verifyVersion(product.version(), request.getVersion(), actor, productId, metadata);
        if (request.hasSku()
                && productRepository.existsBySkuNormalizedAndIdNot(normalize(request.getSku()), id)) {
            reject(actor, "PRODUCT_UPDATE_REJECTED", productId, "DUPLICATE_SKU", metadata);
            throw duplicateSku();
        }

        boolean deactivated = product.active() && request.hasActive() && !request.getActive();
        if (request.hasSku()) {
            product.changeSku(request.getSku());
        }
        if (request.hasName()) {
            product.changeName(request.getName());
        }
        if (request.hasDescription()) {
            product.changeDescription(request.getDescription());
        }
        if (request.hasActive()) {
            product.changeActive(request.getActive());
        }
        product.markUpdated(Instant.now(clock), actor.id());
        try {
            productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException exception) {
            reject(actor, "PRODUCT_UPDATE_REJECTED", productId, "DUPLICATE_SKU", metadata);
            throw duplicateSku();
        } catch (ObjectOptimisticLockingFailureException exception) {
            reject(actor, "PRODUCT_UPDATE_REJECTED", productId, "CONCURRENT_MODIFICATION", metadata);
            throw concurrentModification();
        }
        success(actor, deactivated ? "PRODUCT_DEACTIVATED" : "PRODUCT_UPDATED", product, metadata);
        return productMapper.toResponse(product);
    }

    private void verifyVersion(
            long currentVersion,
            String requestedVersion,
            QaFlowPrincipal actor,
            String resourceId,
            RequestMetadata metadata) {
        if (currentVersion != parseVersion(requestedVersion)) {
            reject(actor, "PRODUCT_UPDATE_REJECTED", resourceId, "CONCURRENT_MODIFICATION", metadata);
            throw concurrentModification();
        }
    }

    private void success(
            QaFlowPrincipal actor,
            String action,
            Product product,
            RequestMetadata metadata) {
        auditEventRecorder.recordWithinBusinessTransaction(new AuditEventCommand(
                actor.id(),
                actor.username(),
                actor.role().name(),
                action,
                "PRODUCT",
                Long.toString(product.id()),
                AuditOutcome.SUCCESS,
                Map.of("sku", product.sku(), "active", product.active()),
                metadata.path(),
                metadata.correlationId()));
    }

    private void reject(
            QaFlowPrincipal actor,
            String action,
            String resourceId,
            String reason,
            RequestMetadata metadata) {
        deferredAuditEventRecorder.recordAfterCurrentTransaction(new AuditEventCommand(
                actor.id(),
                actor.username(),
                actor.role().name(),
                action,
                "PRODUCT",
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
            throw new BadRequestException("INVALID_PATH_PARAMETER", field + " debe ser un entero positivo.");
        }
    }

    private long parseVersion(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new BadRequestException("VALIDATION_ERROR", "La versión debe ser un entero no negativo.");
        }
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private ResourceNotFoundException notFound(String productId) {
        return new ResourceNotFoundException(
                "RESOURCE_NOT_FOUND",
                "No existe un producto con id " + productId + ".");
    }

    private BusinessConflictException duplicateSku() {
        return new BusinessConflictException("DUPLICATE_SKU", "Ya existe un producto con el SKU indicado.");
    }

    private BusinessConflictException concurrentModification() {
        return new BusinessConflictException(
                "CONCURRENT_MODIFICATION",
                "El recurso fue modificado por otra operación. Actualiza los datos e intenta de nuevo.");
    }
}
