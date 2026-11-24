package com.qaflow.bom.api;

import java.net.URI;

import com.qaflow.bom.application.BomService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(path = "/api/v1/products/{productId}/boms", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "BOM")
@SecurityRequirement(name = "bearerAuth")
public class BomController {

    private final BomService bomService;

    public BomController(BomService bomService) {
        this.bomService = bomService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Listar revisiones de la BOM de un producto")
    public BomSummaryPageResponse list(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "revisionNumber,desc") String sort) {
        return bomService.list(productId, page, size, sort);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publicar una nueva revisión de BOM")
    public ResponseEntity<BomResponse> publish(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String productId,
            @Valid @RequestBody PublishBomRequest request,
            @AuthenticationPrincipal QaFlowPrincipal actor,
            HttpServletRequest servletRequest) {
        BomResponse response = bomService.publish(
                productId,
                request,
                actor,
                RequestMetadata.from(servletRequest));
        URI location = URI.create(
                "/api/v1/products/" + productId + "/boms/" + response.revisionNumber());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Consultar la BOM vigente")
    public BomResponse getCurrent(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String productId) {
        return bomService.getCurrent(productId);
    }

    @GetMapping("/{revisionNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Consultar una revisión histórica de BOM")
    public BomResponse getRevision(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String productId,
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String revisionNumber) {
        return bomService.getRevision(productId, revisionNumber);
    }
}
