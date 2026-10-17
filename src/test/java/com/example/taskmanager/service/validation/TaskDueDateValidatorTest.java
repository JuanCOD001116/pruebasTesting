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
    @DisplayName("R7: Al crear, si dueDate es null, se acepta")
    void shouldAcceptNullDueDateOnCreation() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> validator.validateDueDateForCreation(null));
    }

    @Test
    @DisplayName("R7: Al crear, si dueDate es hoy, se acepta")
    void shouldAcceptTodayDueDateOnCreation() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> validator.validateDueDateForCreation(TODAY));
    }

    @Test
    @DisplayName("R7: Al crear, si dueDate es mañana, se acepta")
    void shouldAcceptFutureDueDateOnCreation() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> validator.validateDueDateForCreation(TODAY.plusDays(1)));
    }

    @Test
    @DisplayName("R7: Al crear, si dueDate es anterior a hoy, rechaza con InvalidTaskDataException")
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
    @DisplayName("R8: Al actualizar, si newDueDate es null, se acepta")
    void shouldAcceptNullDueDateOnUpdate() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> validator.validateDueDateForUpdate(TODAY, null));
    }

    @Test
    @DisplayName("R8: Al actualizar, si dueDate no cambia aunque sea en el pasado, se acepta")
    void shouldAcceptUnchangedPastDueDateOnUpdate() {
        // Arrange
        LocalDate pastDate = TODAY.minusDays(5);

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateDueDateForUpdate(pastDate, pastDate));
    }

    @Test
    @DisplayName("R8: Al actualizar, si dueDate cambia a una fecha anterior a hoy, rechaza con InvalidTaskDataException")
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
    @DisplayName("R8: Al actualizar, si dueDate cambia a hoy o futuro, se acepta")
    void shouldAcceptWhenDueDateChangesToTodayOrFutureOnUpdate() {
        // Arrange
        LocalDate futureDate = TODAY.plusDays(3);

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateDueDateForUpdate(TODAY, futureDate));
        assertDoesNotThrow(() -> validator.validateDueDateForUpdate(TODAY.plusDays(1), TODAY));
    }
}
