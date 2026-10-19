package com.example.taskmanager.service.validation;

import com.example.taskmanager.exception.InvalidTaskDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskDueDateValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 16);
    private TaskDueDateValidator validator;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-16T10:00:00Z"), ZoneId.of("UTC"));
        validator = new TaskDueDateValidator(fixedClock);
    }

    @Test
    @DisplayName("R7: Dado un dueDate null, cuando se valida al crear, entonces se acepta")
    void shouldAcceptNullDueDateOnCreation() {
        // Arrange: validator con Clock fijo en setUp()

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateDueDateForCreation(null));
    }

    @Test
    @DisplayName("R7: Dado un dueDate igual a hoy, cuando se valida al crear, entonces se acepta")
    void shouldAcceptTodayDueDateOnCreation() {
        // Arrange: validator con Clock fijo en setUp()

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateDueDateForCreation(TODAY));
    }

    @Test
    @DisplayName("R7: Dado un dueDate de mañana, cuando se valida al crear, entonces se acepta")
    void shouldAcceptFutureDueDateOnCreation() {
        // Arrange: validator con Clock fijo en setUp()

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateDueDateForCreation(TODAY.plusDays(1)));
    }

    @Test
    @DisplayName("R7: Dado un dueDate anterior a hoy, cuando se valida al crear, entonces lanza InvalidTaskDataException")
    void shouldRejectPastDueDateOnCreation() {
        // Arrange
        LocalDate yesterday = TODAY.minusDays(1);

        // Act & Assert
        InvalidTaskDataException ex = assertThrows(
                InvalidTaskDataException.class,
                () -> validator.validateDueDateForCreation(yesterday)
        );
        assertEquals("Due date cannot be in the past", ex.getMessage());
    }

    @Test
    @DisplayName("R8: Dado un nuevo dueDate null, cuando se valida al actualizar, entonces se acepta")
    void shouldAcceptNullDueDateOnUpdate() {
        // Arrange: validator con Clock fijo en setUp()

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateDueDateForUpdate(TODAY, null));
    }

    @Test
    @DisplayName("R8: Dado un dueDate pasado que no cambia, cuando se valida al actualizar, entonces se acepta")
    void shouldAcceptUnchangedPastDueDateOnUpdate() {
        // Arrange
        LocalDate pastDate = TODAY.minusDays(5);

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateDueDateForUpdate(pastDate, pastDate));
    }

    @Test
    @DisplayName("R8: Dado un dueDate que cambia a una fecha pasada, cuando se valida al actualizar, entonces lanza InvalidTaskDataException")
    void shouldRejectWhenDueDateChangesToPastDateOnUpdate() {
        // Arrange
        LocalDate pastDate = TODAY.minusDays(1);

        // Act & Assert
        InvalidTaskDataException ex = assertThrows(
                InvalidTaskDataException.class,
                () -> validator.validateDueDateForUpdate(TODAY, pastDate)
        );
        assertEquals("Due date cannot be in the past", ex.getMessage());
    }

    @Test
    @DisplayName("R8: Dado un dueDate que cambia a hoy o a una fecha futura, cuando se valida al actualizar, entonces se acepta")
    void shouldAcceptWhenDueDateChangesToTodayOrFutureOnUpdate() {
        // Arrange
        LocalDate futureDate = TODAY.plusDays(3);

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateDueDateForUpdate(TODAY, futureDate));
        assertDoesNotThrow(() -> validator.validateDueDateForUpdate(TODAY.plusDays(1), TODAY));
    }
}
