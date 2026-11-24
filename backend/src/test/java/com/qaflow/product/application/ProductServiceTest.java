package com.qaflow.product.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.audit.application.DeferredAuditEventRecorder;
import com.qaflow.product.api.CreateProductRequest;
import com.qaflow.product.api.UpdateProductRequest;
import com.qaflow.product.domain.Product;
import com.qaflow.product.infrastructure.ProductRepository;
import com.qaflow.security.QaFlowPrincipal;
import com.qaflow.shared.error.BusinessConflictException;
import com.qaflow.shared.web.RequestMetadata;
import com.qaflow.user.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-20T04:00:00Z");
    private static final QaFlowPrincipal ACTOR =
            new QaFlowPrincipal(1, "admin.demo", "Administrator", Role.ADMIN, 0);
    private static final RequestMetadata METADATA =
            new RequestMetadata("/api/v1/products", "unit-product-001");

    private ProductRepository repository;
    private ProductMapper mapper;
    private AuditEventRecorder auditEventRecorder;
    private DeferredAuditEventRecorder deferredAuditEventRecorder;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        mapper = mock(ProductMapper.class);
        auditEventRecorder = mock(AuditEventRecorder.class);
        deferredAuditEventRecorder = mock(DeferredAuditEventRecorder.class);
        service = new ProductService(
                repository,
                mapper,
                auditEventRecorder,
                deferredAuditEventRecorder,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void rejectsNormalizedDuplicateSkuBeforePersistence() {
        when(repository.existsBySkuNormalized("FG-PANEL-01")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreateProductRequest(" fg-panel-01 ", "Panel", null, null),
                ACTOR,
                METADATA))
                .isInstanceOfSatisfying(BusinessConflictException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.code())
                                .isEqualTo("DUPLICATE_SKU"));

        verify(repository, never()).saveAndFlush(any());
        verify(deferredAuditEventRecorder).recordAfterCurrentTransaction(any());
    }

    @Test
    void updateDistinguishesExplicitNullDescriptionAndLogicalDeactivation() {
        Product product = mock(Product.class);
        when(product.id()).thenReturn(9L);
        when(product.version()).thenReturn(2L);
        when(product.active()).thenReturn(true, false);
        when(product.sku()).thenReturn("FG-PANEL-01");
        when(repository.findById(9L)).thenReturn(Optional.of(product));
        UpdateProductRequest request = new UpdateProductRequest();
        request.setVersion("2");
        request.setDescription(null);
        request.setActive(false);

        service.update("9", request, ACTOR, METADATA);

        verify(product).changeDescription(null);
        verify(product).changeActive(false);
        verify(product).markUpdated(NOW, ACTOR.id());
        verify(repository).saveAndFlush(product);
        verify(auditEventRecorder).recordWithinBusinessTransaction(any());
    }

    @Test
    void rejectsStaleVersionWithoutMutatingProduct() {
        Product product = mock(Product.class);
        when(product.version()).thenReturn(4L);
        when(repository.findById(7L)).thenReturn(Optional.of(product));
        UpdateProductRequest request = new UpdateProductRequest();
        request.setVersion("3");
        request.setName("Stale name");

        assertThatThrownBy(() -> service.update("7", request, ACTOR, METADATA))
                .isInstanceOfSatisfying(BusinessConflictException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.code())
                                .isEqualTo("CONCURRENT_MODIFICATION"));

        verify(product, never()).changeName(any());
        verify(repository, never()).saveAndFlush(any());
    }
}
