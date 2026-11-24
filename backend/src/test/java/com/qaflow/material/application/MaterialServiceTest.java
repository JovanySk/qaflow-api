package com.qaflow.material.application;

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
import com.qaflow.material.api.CreateMaterialRequest;
import com.qaflow.material.api.UpdateMaterialRequest;
import com.qaflow.material.domain.Material;
import com.qaflow.material.domain.UnitOfMeasure;
import com.qaflow.material.infrastructure.MaterialPersistenceSupport;
import com.qaflow.material.infrastructure.MaterialRepository;
import com.qaflow.security.QaFlowPrincipal;
import com.qaflow.shared.error.BusinessConflictException;
import com.qaflow.shared.web.RequestMetadata;
import com.qaflow.user.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MaterialServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-20T04:00:00Z");
    private static final QaFlowPrincipal ACTOR =
            new QaFlowPrincipal(1, "admin.demo", "Administrator", Role.ADMIN, 0);
    private static final RequestMetadata METADATA =
            new RequestMetadata("/api/v1/materials", "unit-material-001");

    private MaterialRepository repository;
    private MaterialPersistenceSupport persistenceSupport;
    private MaterialMapper mapper;
    private AuditEventRecorder auditEventRecorder;
    private DeferredAuditEventRecorder deferredAuditEventRecorder;
    private MaterialService service;

    @BeforeEach
    void setUp() {
        repository = mock(MaterialRepository.class);
        persistenceSupport = mock(MaterialPersistenceSupport.class);
        mapper = mock(MaterialMapper.class);
        auditEventRecorder = mock(AuditEventRecorder.class);
        deferredAuditEventRecorder = mock(DeferredAuditEventRecorder.class);
        service = new MaterialService(
                repository,
                persistenceSupport,
                mapper,
                auditEventRecorder,
                deferredAuditEventRecorder,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsMaterialAndZeroBalanceInsideServiceTransaction() {
        when(repository.saveAndFlush(any(Material.class))).thenAnswer(invocation -> {
            Material material = invocation.getArgument(0);
            ReflectionTestUtils.setField(material, "id", 12L);
            return material;
        });

        service.create(
                new CreateMaterialRequest(
                        "MAT-AL-01", "Aluminum", null, UnitOfMeasure.KILOGRAM, true),
                ACTOR,
                METADATA);

        verify(persistenceSupport).createInitialBalance(12L, ACTOR.id(), NOW);
        verify(auditEventRecorder).recordWithinBusinessTransaction(any());
    }

    @Test
    void blocksUnitChangeWhenMaterialHasHistory() {
        Material material = mock(Material.class);
        when(material.version()).thenReturn(1L);
        when(material.unitOfMeasure()).thenReturn(UnitOfMeasure.KILOGRAM);
        when(repository.findById(8L)).thenReturn(Optional.of(material));
        when(persistenceSupport.hasUnitSensitiveHistory(8L)).thenReturn(true);
        UpdateMaterialRequest request = new UpdateMaterialRequest();
        request.setVersion("1");
        request.setUnitOfMeasure(UnitOfMeasure.GRAM);

        assertThatThrownBy(() -> service.update("8", request, ACTOR, METADATA))
                .isInstanceOfSatisfying(BusinessConflictException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.code())
                                .isEqualTo("MATERIAL_UNIT_IMMUTABLE"));

        verify(material, never()).changeUnitOfMeasure(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void permitsUnitChangeBeforeAnyUsageHistory() {
        Material material = mock(Material.class);
        when(material.id()).thenReturn(8L);
        when(material.version()).thenReturn(1L);
        when(material.unitOfMeasure()).thenReturn(UnitOfMeasure.KILOGRAM, UnitOfMeasure.GRAM);
        when(material.active()).thenReturn(true);
        when(material.code()).thenReturn("MAT-AL-01");
        when(repository.findById(8L)).thenReturn(Optional.of(material));
        when(persistenceSupport.hasUnitSensitiveHistory(8L)).thenReturn(false);
        UpdateMaterialRequest request = new UpdateMaterialRequest();
        request.setVersion("1");
        request.setUnitOfMeasure(UnitOfMeasure.GRAM);

        service.update("8", request, ACTOR, METADATA);

        verify(material).changeUnitOfMeasure(UnitOfMeasure.GRAM);
        verify(material).markUpdated(NOW, ACTOR.id());
        verify(repository).saveAndFlush(material);
    }
}
