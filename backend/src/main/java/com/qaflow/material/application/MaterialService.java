package com.qaflow.material.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import com.qaflow.audit.application.AuditEventCommand;
import com.qaflow.audit.application.AuditEventRecorder;
import com.qaflow.audit.application.DeferredAuditEventRecorder;
import com.qaflow.audit.domain.AuditOutcome;
import com.qaflow.material.api.CreateMaterialRequest;
import com.qaflow.material.api.MaterialPageResponse;
import com.qaflow.material.api.MaterialResponse;
import com.qaflow.material.api.UpdateMaterialRequest;
import com.qaflow.material.domain.Material;
import com.qaflow.material.domain.UnitOfMeasure;
import com.qaflow.material.infrastructure.MaterialPersistenceSupport;
import com.qaflow.material.infrastructure.MaterialRepository;
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
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialPersistenceSupport persistenceSupport;
    private final MaterialMapper materialMapper;
    private final AuditEventRecorder auditEventRecorder;
    private final DeferredAuditEventRecorder deferredAuditEventRecorder;
    private final Clock clock;

    public MaterialService(
            MaterialRepository materialRepository,
            MaterialPersistenceSupport persistenceSupport,
            MaterialMapper materialMapper,
            AuditEventRecorder auditEventRecorder,
            DeferredAuditEventRecorder deferredAuditEventRecorder,
            Clock clock) {
        this.materialRepository = materialRepository;
        this.persistenceSupport = persistenceSupport;
        this.materialMapper = materialMapper;
        this.auditEventRecorder = auditEventRecorder;
        this.deferredAuditEventRecorder = deferredAuditEventRecorder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MaterialPageResponse list(
            int page,
            int size,
            String sort,
            String search,
            Boolean active,
            UnitOfMeasure unitOfMeasure) {
        Specification<Material> specification = (root, query, builder) -> builder.conjunction();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toUpperCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(root.get("codeNormalized"), pattern),
                    builder.like(builder.upper(root.get("name")), pattern)));
        }
        if (active != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("active"), active));
        }
        if (unitOfMeasure != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("unitOfMeasure"), unitOfMeasure));
        }
        Page<Material> result = materialRepository.findAll(
                specification,
                PageRequest.of(page, size, MaterialSort.parse(sort)));
        return new MaterialPageResponse(
                materialMapper.toResponses(result.getContent()),
                new PageMetadataResponse(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.isFirst(),
                        result.isLast()));
    }

    @Transactional(readOnly = true)
    public MaterialResponse get(String materialId) {
        Material material = materialRepository.findById(parseId(materialId, "materialId"))
                .orElseThrow(() -> notFound(materialId));
        return materialMapper.toResponse(material);
    }

    @Transactional
    public MaterialResponse create(
            CreateMaterialRequest request,
            QaFlowPrincipal actor,
            RequestMetadata metadata) {
        String normalizedCode = normalize(request.code());
        if (materialRepository.existsByCodeNormalized(normalizedCode)) {
            reject(actor, "MATERIAL_CREATE_REJECTED", null, "DUPLICATE_MATERIAL_CODE", metadata);
            throw duplicateCode();
        }
        Instant now = Instant.now(clock);
        Material material = Material.create(
                request.code(),
                request.name(),
                request.description(),
                request.unitOfMeasure(),
                request.activeOrDefault(),
                now,
                actor.id());
        try {
            materialRepository.saveAndFlush(material);
        } catch (DataIntegrityViolationException exception) {
            reject(actor, "MATERIAL_CREATE_REJECTED", null, "DUPLICATE_MATERIAL_CODE", metadata);
            throw duplicateCode();
        }
        persistenceSupport.createInitialBalance(material.id(), actor.id(), now);
        success(actor, "MATERIAL_CREATED", material, metadata);
        return materialMapper.toResponse(material);
    }

    @Transactional
    public MaterialResponse update(
            String materialId,
            UpdateMaterialRequest request,
            QaFlowPrincipal actor,
            RequestMetadata metadata) {
        long id = parseId(materialId, "materialId");
        Material material = materialRepository.findById(id).orElseThrow(() -> notFound(materialId));
        verifyVersion(material.version(), request.getVersion(), actor, materialId, metadata);
        if (request.hasCode()
                && materialRepository.existsByCodeNormalizedAndIdNot(normalize(request.getCode()), id)) {
            reject(actor, "MATERIAL_UPDATE_REJECTED", materialId, "DUPLICATE_MATERIAL_CODE", metadata);
            throw duplicateCode();
        }
        boolean changesUnit = request.hasUnitOfMeasure()
                && request.getUnitOfMeasure() != material.unitOfMeasure();
        if (changesUnit && persistenceSupport.hasUnitSensitiveHistory(id)) {
            reject(actor, "MATERIAL_UPDATE_REJECTED", materialId, "MATERIAL_UNIT_IMMUTABLE", metadata);
            throw new BusinessConflictException(
                    "MATERIAL_UNIT_IMMUTABLE",
                    "La unidad no puede cambiar porque el material ya tiene uso o historia asociada.");
        }

        boolean deactivated = material.active() && request.hasActive() && !request.getActive();
        if (request.hasCode()) {
            material.changeCode(request.getCode());
        }
        if (request.hasName()) {
            material.changeName(request.getName());
        }
        if (request.hasDescription()) {
            material.changeDescription(request.getDescription());
        }
        if (request.hasUnitOfMeasure()) {
            material.changeUnitOfMeasure(request.getUnitOfMeasure());
        }
        if (request.hasActive()) {
            material.changeActive(request.getActive());
        }
        material.markUpdated(Instant.now(clock), actor.id());
        try {
            materialRepository.saveAndFlush(material);
        } catch (DataIntegrityViolationException exception) {
            reject(actor, "MATERIAL_UPDATE_REJECTED", materialId, "DUPLICATE_MATERIAL_CODE", metadata);
            throw duplicateCode();
        } catch (ObjectOptimisticLockingFailureException exception) {
            reject(actor, "MATERIAL_UPDATE_REJECTED", materialId, "CONCURRENT_MODIFICATION", metadata);
            throw concurrentModification();
        }
        success(actor, deactivated ? "MATERIAL_DEACTIVATED" : "MATERIAL_UPDATED", material, metadata);
        return materialMapper.toResponse(material);
    }

    private void verifyVersion(
            long currentVersion,
            String requestedVersion,
            QaFlowPrincipal actor,
            String resourceId,
            RequestMetadata metadata) {
        if (currentVersion != parseVersion(requestedVersion)) {
            reject(actor, "MATERIAL_UPDATE_REJECTED", resourceId, "CONCURRENT_MODIFICATION", metadata);
            throw concurrentModification();
        }
    }

    private void success(
            QaFlowPrincipal actor,
            String action,
            Material material,
            RequestMetadata metadata) {
        auditEventRecorder.recordWithinBusinessTransaction(new AuditEventCommand(
                actor.id(),
                actor.username(),
                actor.role().name(),
                action,
                "MATERIAL",
                Long.toString(material.id()),
                AuditOutcome.SUCCESS,
                Map.of(
                        "code", material.code(),
                        "unitOfMeasure", material.unitOfMeasure().name(),
                        "active", material.active()),
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
                "MATERIAL",
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

    private ResourceNotFoundException notFound(String materialId) {
        return new ResourceNotFoundException(
                "RESOURCE_NOT_FOUND",
                "No existe un material con id " + materialId + ".");
    }

    private BusinessConflictException duplicateCode() {
        return new BusinessConflictException(
                "DUPLICATE_MATERIAL_CODE",
                "Ya existe un material con el código indicado.");
    }

    private BusinessConflictException concurrentModification() {
        return new BusinessConflictException(
                "CONCURRENT_MODIFICATION",
                "El recurso fue modificado por otra operación. Actualiza los datos e intenta de nuevo.");
    }
}
