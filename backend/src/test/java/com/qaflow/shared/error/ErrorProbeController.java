package com.qaflow.shared.error;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ErrorProbeController {

    @PostMapping("/test/validation")
    Map<String, String> validate(@Valid @RequestBody ProbeRequest request) {
        return Map.of("name", request.name());
    }

    @GetMapping("/test/not-found")
    void notFound() {
        throw new ResourceNotFoundException("PROBE_NOT_FOUND", "El recurso de prueba no existe.");
    }

    @GetMapping("/test/unexpected")
    void unexpected() {
        throw new IllegalStateException("database-password=must-not-leak");
    }

    record ProbeRequest(@NotBlank(message = "El nombre es obligatorio.") String name) {
    }
}

