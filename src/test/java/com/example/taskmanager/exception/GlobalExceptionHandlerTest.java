package com.example.taskmanager.exception;

import com.example.taskmanager.dto.response.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-14T15:30:00Z"), ZoneId.of("UTC"));

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(FIXED_CLOCK);

    @Test
    @DisplayName("Dada una excepción inesperada con detalles internos, cuando se maneja, entonces responde 500 con mensaje fijo sin filtrar el detalle")
    void shouldReturnGenericMessageWithoutLeakingExceptionDetails() {
        // Arrange
        WebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/tasks/7"));
        Exception unexpected = new IllegalStateException("password=s3cr3t at jdbc:postgresql://db:5432/taskmanager");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(unexpected, request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.getCode());
        assertEquals("Error interno del servidor", body.getMessage());
        assertFalse(body.getMessage().contains("s3cr3t"));
        assertEquals("/api/tasks/7", body.getPath());
        assertEquals(LocalDateTime.of(2026, 8, 14, 15, 30), body.getTimestamp());
    }
}
