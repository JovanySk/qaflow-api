package com.qaflow.shared.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qaflow.shared.config.ClockConfiguration;
import com.qaflow.shared.web.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ErrorProbeController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@Import({
        ClockConfiguration.class,
        ApiErrorResponseFactory.class,
        CorrelationIdFilter.class,
        GlobalExceptionHandler.class
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsContractErrorForValidationFailure() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .header(CorrelationIdFilter.HEADER_NAME, "validation-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "validation-001"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/test/validation"))
                .andExpect(jsonPath("$.correlationId").value("validation-001"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void returnsNotFoundWithoutLeakingImplementationDetails() throws Exception {
        mockMvc.perform(get("/test/not-found")
                        .header(CorrelationIdFilter.HEADER_NAME, "not-found-001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROBE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("El recurso de prueba no existe."))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void rejectsInvalidCorrelationIdBeforeControllerExecution() throws Exception {
        mockMvc.perform(get("/test/not-found")
                        .header(CorrelationIdFilter.HEADER_NAME, "invalid correlation id"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.code").value("INVALID_CORRELATION_ID"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void sanitizesUnexpectedErrors() throws Exception {
        mockMvc.perform(get("/test/unexpected")
                        .header(CorrelationIdFilter.HEADER_NAME, "unexpected-001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(
                        "Ocurrió un error inesperado. Usa el identificador de correlación para soporte."))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("database-password"))));
    }

}
