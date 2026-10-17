package com.example.taskmanager.service.validation;

import com.example.taskmanager.exception.InvalidTaskDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortValidatorTest {

    private SortValidator sortValidator;

    @BeforeEach
    void setUp() {
        // Arrange
        sortValidator = new SortValidator();
    }

    @Test
    @DisplayName("Dado un sort nulo o no ordenado, cuando se valida, entonces no lanza excepcion")
    void shouldAcceptNullOrUnsorted() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> sortValidator.validate(null));
        assertDoesNotThrow(() -> sortValidator.validate(Sort.unsorted()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "title", "status", "priority", "dueDate", "createdAt", "updatedAt"})
    @DisplayName("Dada una propiedad permitida en la lista blanca, cuando se valida, entonces no lanza excepcion")
    void shouldAcceptAllowedProperties(String property) {
        // Arrange
        Sort sort = Sort.by(Sort.Direction.ASC, property);

        // Act & Assert
        assertDoesNotThrow(() -> sortValidator.validate(sort));
    }

    @Test
    @DisplayName("Dado un sort compuesto con multiples propiedades validas, cuando se valida, entonces no lanza excepcion")
    void shouldAcceptMultipleValidProperties() {
        // Arrange
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("priority"));

        // Act & Assert
        assertDoesNotThrow(() -> sortValidator.validate(sort));
    }

    @ParameterizedTest
    @ValueSource(strings = {"password", "secret", "unknown", "description", "assigned_to"})
    @DisplayName("Dada una propiedad no permitida en la lista blanca, cuando se valida, entonces lanza InvalidTaskDataException")
    void shouldRejectDisallowedProperties(String property) {
        // Arrange
        Sort sort = Sort.by(Sort.Direction.ASC, property);

        // Act & Assert
        InvalidTaskDataException exception = assertThrows(
                InvalidTaskDataException.class,
                () -> sortValidator.validate(sort)
        );
        assertTrue(exception.getMessage().contains("Invalid sort property: " + property));
    }
}
