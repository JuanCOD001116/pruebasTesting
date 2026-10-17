package com.example.taskmanager.dto.request;

import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskSearchCriteriaTest {

    @Test
    @DisplayName("Dado dueFrom anterior a dueTo, cuando se valida el rango, entonces es valido")
    void shouldBeValidWhenDueFromIsBeforeDueTo() {
        // Arrange
        TaskSearchCriteria criteria = new TaskSearchCriteria(
                TaskStatus.PENDING, TaskPriority.HIGH, "user1", "report",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), false
        );

        // Act & Assert
        assertTrue(criteria.isDueRangeValid());
    }

    @Test
    @DisplayName("Dado dueFrom igual a dueTo, cuando se valida el rango, entonces es valido")
    void shouldBeValidWhenDueFromEqualsDueTo() {
        // Arrange
        LocalDate sameDate = LocalDate.of(2026, 10, 15);
        TaskSearchCriteria criteria = new TaskSearchCriteria(
                null, null, null, null, sameDate, sameDate, null
        );

        // Act & Assert
        assertTrue(criteria.isDueRangeValid());
    }

    @Test
    @DisplayName("Dado fechas nulas en el criterio, cuando se valida el rango, entonces es valido")
    void shouldBeValidWhenDatesAreNull() {
        // Arrange
        TaskSearchCriteria bothNull = new TaskSearchCriteria(null, null, null, null, null, null, null);
        TaskSearchCriteria fromOnly = new TaskSearchCriteria(null, null, null, null, LocalDate.now(), null, null);
        TaskSearchCriteria toOnly = new TaskSearchCriteria(null, null, null, null, null, LocalDate.now(), null);

        // Act & Assert
        assertTrue(bothNull.isDueRangeValid());
        assertTrue(fromOnly.isDueRangeValid());
        assertTrue(toOnly.isDueRangeValid());
    }

    @Test
    @DisplayName("Dado dueFrom posterior a dueTo, cuando se valida el rango, entonces es invalido")
    void shouldBeInvalidWhenDueFromIsAfterDueTo() {
        // Arrange
        TaskSearchCriteria criteria = new TaskSearchCriteria(
                null, null, null, null,
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 10),
                null
        );

        // Act & Assert
        assertFalse(criteria.isDueRangeValid());
    }
}
