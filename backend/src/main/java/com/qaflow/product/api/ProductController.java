package com.qaflow.product.api;

import java.net.URI;

import com.qaflow.product.application.ProductService;
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
@RequestMapping(path = "/api/v1/products", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Products")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Listar productos")
    public ProductPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) Boolean active) {
        return productService.list(page, size, sort, search, active);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear producto")
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal QaFlowPrincipal actor,
            HttpServletRequest servletRequest) {
        ProductResponse response = productService.create(
                request,
                actor,
                RequestMetadata.from(servletRequest));
        return ResponseEntity.created(URI.create("/api/v1/products/" + response.id())).body(response);
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Consultar producto")
    public ProductResponse get(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String productId) {
        return productService.get(productId);
    }

    @PatchMapping(path = "/{productId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar o desactivar producto")
    public ProductResponse update(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String productId,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal QaFlowPrincipal actor,
            HttpServletRequest servletRequest) {
        return productService.update(
                productId,
                request,
                actor,
                RequestMetadata.from(servletRequest));
    }
}
