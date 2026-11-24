package com.qaflow.inventory.api;

import java.net.URI;
import java.time.Instant;

import com.qaflow.inventory.application.InventoryService;
import com.qaflow.inventory.domain.InventoryMovementType;
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
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping(path = "/api/v1/inventory", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Inventory")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/balances")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Listar saldos de inventario")
    public InventoryBalancePageResponse listBalances(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "materialCode,asc") String sort,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean outOfStock) {
        return inventoryService.listBalances(page, size, sort, search, active, outOfStock);
    }

    @GetMapping("/balances/{materialId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Consultar saldo por material")
    public InventoryBalanceResponse getBalance(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String materialId) {
        return inventoryService.getBalance(materialId);
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Listar el historial inmutable de movimientos")
    public InventoryMovementPageResponse listMovements(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "occurredAt,desc") String sort,
            @RequestParam(required = false) @Pattern(regexp = "^[1-9][0-9]*$") String materialId,
            @RequestParam(required = false) InventoryMovementType type,
            @RequestParam(required = false) @Pattern(regexp = "^[1-9][0-9]*$") String productionOrderId,
            @RequestParam(required = false) @Pattern(regexp = "^[1-9][0-9]*$") String performedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return inventoryService.listMovements(
                page, size, sort, materialId, type, productionOrderId, performedBy, from, to);
    }

    @PostMapping(path = "/movements", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "Registrar un movimiento manual de inventario")
    public ResponseEntity<InventoryMovementResponse> createMovement(
            @Valid @RequestBody CreateInventoryMovementRequest request,
            @AuthenticationPrincipal QaFlowPrincipal actor,
            HttpServletRequest servletRequest) {
        InventoryMovementResponse response = inventoryService.createMovement(
                request,
                actor,
                RequestMetadata.from(servletRequest));
        return ResponseEntity.created(URI.create("/api/v1/inventory/movements/" + response.id())).body(response);
    }

    @GetMapping("/movements/{movementId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Consultar un movimiento inmutable")
    public InventoryMovementResponse getMovement(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String movementId) {
        return inventoryService.getMovement(movementId);
    }
}
