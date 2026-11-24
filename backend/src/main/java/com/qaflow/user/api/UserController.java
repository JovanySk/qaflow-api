package com.qaflow.user.api;

import java.net.URI;

import com.qaflow.security.QaFlowPrincipal;
import com.qaflow.shared.web.RequestMetadata;
import com.qaflow.user.application.UserService;
import com.qaflow.user.domain.Role;
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
@RequestMapping(path = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios")
    public UserPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active) {
        return userService.list(page, size, sort, search, role, active);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear usuario")
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal QaFlowPrincipal actor,
            HttpServletRequest servletRequest) {
        UserResponse response = userService.create(request, actor, RequestMetadata.from(servletRequest));
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Consultar usuario")
    public UserResponse get(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String userId) {
        return userService.get(userId);
    }

    @PatchMapping(path = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar usuario")
    public UserResponse update(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String userId,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal QaFlowPrincipal actor,
            HttpServletRequest servletRequest) {
        return userService.update(userId, request, actor, RequestMetadata.from(servletRequest));
    }

    @PatchMapping(path = "/{userId}/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cambiar contraseña")
    public ResponseEntity<Void> changePassword(
            @PathVariable @Pattern(regexp = "^[1-9][0-9]*$") String userId,
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal QaFlowPrincipal actor,
            HttpServletRequest servletRequest) {
        userService.changePassword(userId, request, actor, RequestMetadata.from(servletRequest));
        return ResponseEntity.noContent().build();
    }
}
