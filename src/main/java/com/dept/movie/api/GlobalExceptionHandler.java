package com.dept.movie.api;

import com.dept.movie.api.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> handleValidation(HandlerMethodValidationException ex) {
        return response(HttpStatus.BAD_REQUEST, "Validation error", ex.getMessage());
    }

    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    ResponseEntity<ErrorResponse> handleExternalNotFound(HttpClientErrorException.NotFound ex) {
        return response(
                HttpStatus.NOT_FOUND,
                "Movie not found",
                "Movie was not found in external datasource"
        );
    }

    @ExceptionHandler({
            HttpServerErrorException.class,
            org.springframework.web.client.ResourceAccessException.class,
            io.github.resilience4j.circuitbreaker.CallNotPermittedException.class,
            io.github.resilience4j.ratelimiter.RequestNotPermitted.class,
            io.github.resilience4j.bulkhead.BulkheadFullException.class
    })
    ResponseEntity<ErrorResponse> handleExternalUnavailable(RuntimeException ex) {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "External provider unavailable",
                "Movie data provider is temporarily unavailable"
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error",
                "Unexpected server error"
        );
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String error, String message) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        Instant.now(),
                        status.value(),
                        error,
                        message
                ));
    }
}