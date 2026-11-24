package com.qaflow.shared.error;

import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.qaflow.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ApiErrorResponseFactory errorResponseFactory;

    public GlobalExceptionHandler(ApiErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ApiErrorResponse> handleApplicationException(
            ApplicationException exception,
            HttpServletRequest request) {
        return response(exception.status(), exception.code(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<ApiFieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiFieldError(
                        error.getField(),
                        error.getCode() == null ? "INVALID" : error.getCode().toUpperCase(),
                        error.getDefaultMessage() == null ? "Valor inválido." : error.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiFieldError::field).thenComparing(ApiFieldError::code))
                .toList();
        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "La solicitud contiene datos inválidos.",
                request,
                fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<ApiFieldError> fieldErrors = exception.getConstraintViolations().stream()
                .map(violation -> new ApiFieldError(
                        violation.getPropertyPath().toString(),
                        "INVALID",
                        violation.getMessage()))
                .sorted(Comparator.comparing(ApiFieldError::field))
                .toList();
        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "La solicitud contiene datos inválidos.",
                request,
                fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        if (hasCause(exception, UnrecognizedPropertyException.class)) {
            return response(
                    HttpStatus.BAD_REQUEST,
                    "UNKNOWN_FIELD",
                    "El cuerpo JSON contiene un campo que no forma parte del contrato.",
                    request,
                    List.of());
        }
        return response(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_JSON",
                "El cuerpo JSON es inválido o no cumple el contrato esperado.",
                request,
                List.of());
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    ResponseEntity<ApiErrorResponse> handleRequestBinding(
            ServletRequestBindingException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "La solicitud no contiene todos los datos requeridos.",
                request,
                List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_QUERY_PARAMETER",
                "Un parámetro de la solicitud tiene un formato no permitido.",
                request,
                List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "El recurso solicitado no existe.",
                request,
                List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "El método HTTP no está permitido para este recurso.",
                request,
                List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "El Content-Type de la solicitud no está soportado.",
                request,
                List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        LOGGER.warn("Database constraint rejected request correlationId={}", correlationId(request));
        return response(
                HttpStatus.CONFLICT,
                "DATA_INTEGRITY_CONFLICT",
                "La operación entra en conflicto con la integridad de los datos.",
                request,
                List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        String correlationId = correlationId(request);
        LOGGER.error(
                "Unexpected request failure correlationId={} exceptionType={}",
                correlationId,
                exception.getClass().getName());
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Ocurrió un error inesperado. Usa el identificador de correlación para soporte.",
                request,
                List.of());
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<ApiFieldError> fieldErrors) {
        String correlationId = correlationId(request);
        ApiErrorResponse body = errorResponseFactory.create(
                status,
                code,
                message,
                request.getRequestURI(),
                correlationId,
                fieldErrors);
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON);
        if (status == HttpStatus.UNAUTHORIZED) {
            responseBuilder.header("WWW-Authenticate", "Bearer");
        }
        return responseBuilder.body(body);
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return value == null ? "missing-correlation-id" : value.toString();
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> expectedType) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth++ < 10) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
