package com.example.taskmanager.dto.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NullOrNotBlankValidatorTest {

    private NullOrNotBlankValidator validator;

    @BeforeEach
    void setUp() {
        // Arrange
        validator = new NullOrNotBlankValidator();
    }

    @Test
    @DisplayName("Dado un valor nulo, cuando se valida, entonces retorna verdadero (es valido)")
    void shouldReturnTrueWhenValueIsNull() {
        // Arrange
        String value = null;

        // Act
        boolean result = validator.isValid(value, null);

        // Assert
        assertTrue(result, "Null should be considered valid");
    }

    @Test
    @DisplayName("Dado un texto con caracteres validos, cuando se valida, entonces retorna verdadero")
    void shouldReturnTrueWhenValueHasText() {
        // Arrange
        String value = "Valid Task Title";

        // Act
        boolean result = validator.isValid(value, null);

        // Assert
        assertTrue(result, "Text with non-whitespace characters should be valid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    @DisplayName("Dado un texto vacio o solo espacios, cuando se valida, entonces retorna falso")
    void shouldReturnFalseWhenValueIsBlank(String blankValue) {
        // Arrange: valor en blanco provisto por @ValueSource

        // Act
        boolean result = validator.isValid(blankValue, null);

        // Assert
        assertFalse(result, "Blank string should be invalid");
    }
}
