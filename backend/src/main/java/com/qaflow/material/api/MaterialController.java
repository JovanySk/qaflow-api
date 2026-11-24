package com.qaflow.material.api;

import java.net.URI;

import com.qaflow.material.application.MaterialService;
import com.qaflow.material.domain.UnitOfMeasure;
import com.qaflow.security.QaFlowPrincipal;
import com.qaflow.shared.web.RequestMetadata;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(path = "/api/v1/materials", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Materials")
@SecurityRequirement(name = "bearerAuth")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Listar materiales")
    public MaterialPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) UnitOfMeasure unitOfMeasure) {
        return materialService.list(page, size, sort, search, active, unitOfMeasure);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear material y su balance inicial en cero")
    public ResponseEntity<MaterialResponse> create(
            @Valid @RequestBody CreateMaterialRequest request,
            @AuthenticationPrincipal QaFlowPrincipal actor,
            HttpServletRequest servletRequest) {
        MaterialResponse response = materialService.create(
                request,
                actor,
                RequestMetadata.from(servletRequest));
        return ResponseEntity.created(URI.create("/api/v1/materials/" + response.id())).body(response);
    }

    @GetMapping("/{materialId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Consultar material")
    public MaterialResponse get(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String materialId) {
        return materialService.get(materialId);
    }

    @PatchMapping(path = "/{materialId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar o desactivar material")
    public MaterialResponse update(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String materialId,
            @Valid @RequestBody UpdateMaterialRequest request,
            @AuthenticationPrincipal QaFlowPrincipal actor,
            HttpServletRequest servletRequest) {
        return materialService.update(
                materialId,
                request,
                actor,
                RequestMetadata.from(servletRequest));
    }
}
