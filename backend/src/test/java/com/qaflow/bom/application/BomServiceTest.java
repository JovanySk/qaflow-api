package com.qaflow.bom.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.qaflow.bom.api.BomItemRequest;
import com.qaflow.bom.api.PublishBomRequest;
import com.qaflow.bom.domain.Bom;
import com.qaflow.bom.infrastructure.BomItemRepository;
import com.qaflow.bom.infrastructure.BomRepository;
import com.qaflow.material.domain.Material;
import com.qaflow.material.infrastructure.MaterialRepository;
import com.qaflow.product.domain.Product;
import com.qaflow.product.infrastructure.ProductRepository;
import com.qaflow.security.QaFlowPrincipal;
import com.qaflow.shared.error.BadRequestException;
import com.qaflow.shared.error.BusinessConflictException;
import com.qaflow.shared.error.ResourceNotFoundException;
import com.qaflow.shared.web.RequestMetadata;
import com.qaflow.user.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BomServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-20T05:00:00Z");
    private static final QaFlowPrincipal ACTOR =
            new QaFlowPrincipal(1, "admin.demo", "Administrator", Role.ADMIN, 0);
    private static final RequestMetadata METADATA =
            new RequestMetadata("/api/v1/products/7/boms", "unit-bom-001");

    private BomRepository bomRepository;
    private MaterialRepository materialRepository;
    private ProductRepository productRepository;
    private DeferredAuditEventRecorder deferredAuditEventRecorder;
    private BomService service;

    @BeforeEach
    void setUp() {
        bomRepository = mock(BomRepository.class);
        BomItemRepository bomItemRepository = mock(BomItemRepository.class);
        productRepository = mock(ProductRepository.class);
        materialRepository = mock(MaterialRepository.class);
        BomMapper bomMapper = mock(BomMapper.class);
        AuditEventRecorder auditEventRecorder = mock(AuditEventRecorder.class);
        deferredAuditEventRecorder = mock(DeferredAuditEventRecorder.class);
        service = new BomService(
                bomRepository,
                bomItemRepository,
                productRepository,
                materialRepository,
                bomMapper,
                auditEventRecorder,
                deferredAuditEventRecorder,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void rejectsDuplicateMaterialBeforeReadingOrWritingPersistence() {
        PublishBomRequest request = request(
                new BomItemRequest("10", "1.000000"),
                new BomItemRequest("10", "2.000000"));

        assertThatThrownBy(() -> service.publish("7", request, ACTOR, METADATA))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.code()).isEqualTo("DUPLICATE_BOM_MATERIAL"));

        verify(bomRepository, never()).findCurrentId(any(Long.class));
        verify(bomRepository, never()).saveAndFlush(any());
        verify(deferredAuditEventRecorder).recordAfterCurrentTransaction(any());
    }

    @Test
    void rejectsQuantityWithMoreThanSixDecimals() {
        PublishBomRequest request = request(new BomItemRequest("10", "0.0000001"));

        assertThatThrownBy(() -> service.publish("7", request, ACTOR, METADATA))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.code()).isEqualTo("VALIDATION_ERROR"));

        verify(bomRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsInactiveProductBeforeLockingMaterials() {
        Product product = mock(Product.class);
        when(product.active()).thenReturn(false);
        when(bomRepository.findCurrentId(7L)).thenReturn(Optional.empty());
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.publish("7", validRequest(), ACTOR, METADATA))
                .isInstanceOfSatisfying(BusinessConflictException.class,
                        exception -> assertThat(exception.code()).isEqualTo("RESOURCE_INACTIVE"));

        verify(materialRepository, never()).findAllByIdForUpdate(any());
        verify(bomRepository, never()).saveAndFlush(any());
        verify(deferredAuditEventRecorder).recordAfterCurrentTransaction(any());
    }

    @Test
    void rejectsMissingMaterialWithoutReplacingCurrentBom() {
        Product product = mock(Product.class);
        when(product.active()).thenReturn(true);
        when(bomRepository.findCurrentId(7L)).thenReturn(Optional.empty());
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(product));
        when(bomRepository.findCurrentForUpdate(7L)).thenReturn(Optional.empty());
        when(materialRepository.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.publish("7", validRequest(), ACTOR, METADATA))
                .isInstanceOfSatisfying(ResourceNotFoundException.class,
                        exception -> assertThat(exception.code()).isEqualTo("RESOURCE_NOT_FOUND"));

        verify(bomRepository, never()).saveAndFlush(any());
        verify(deferredAuditEventRecorder).recordAfterCurrentTransaction(any());
    }

    @Test
    void detectsChangedCurrentBomBeforeLockingMaterials() {
        Product product = mock(Product.class);
        Bom changedCurrent = mock(Bom.class);
        when(product.active()).thenReturn(true);
        when(changedCurrent.id()).thenReturn(51L);
        when(bomRepository.findCurrentId(7L)).thenReturn(Optional.of(50L));
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(product));
        when(bomRepository.findCurrentForUpdate(7L)).thenReturn(Optional.of(changedCurrent));

        assertThatThrownBy(() -> service.publish("7", validRequest(), ACTOR, METADATA))
                .isInstanceOfSatisfying(BusinessConflictException.class,
                        exception -> assertThat(exception.code()).isEqualTo("BOM_CONCURRENTLY_CHANGED"));

        verify(materialRepository, never()).findAllByIdForUpdate(any());
        verify(bomRepository, never()).saveAndFlush(any());
        verify(deferredAuditEventRecorder).recordAfterCurrentTransaction(any());
    }

    private PublishBomRequest validRequest() {
        return request(new BomItemRequest("10", "1.250000"));
    }

    private PublishBomRequest request(BomItemRequest... items) {
        return new PublishBomRequest(List.of(items));
    }
}
