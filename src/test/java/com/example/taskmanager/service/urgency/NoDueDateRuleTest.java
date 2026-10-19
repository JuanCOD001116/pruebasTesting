package com.example.taskmanager.service.urgency;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.model.TaskUrgency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoDueDateRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 21);

    private final NoDueDateRule rule = new NoDueDateRule();

    @Test
    @DisplayName("Dada una tarea sin dueDate y prioridad HIGH, cuando se evalúa la regla, entonces retorna AT_RISK")
    void shouldReturnAtRiskWhenNoDueDateAndHighPriority() {
        // Arrange
        Task task = Task.builder()
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .dueDate(null)
                .build();

        // Act
        Optional<TaskUrgency> result = rule.evaluate(task, TODAY);

        // Assert
        assertEquals(Optional.of(TaskUrgency.AT_RISK), result);
    }

    @ParameterizedTest
    @EnumSource(value = TaskPriority.class, names = {"MEDIUM", "LOW"})
    @DisplayName("Dada una tarea sin dueDate y prioridad no alta, cuando se evalúa la regla, entonces retorna ON_TRACK")
    void shouldReturnOnTrackWhenNoDueDateAndNotHighPriority(TaskPriority priority) {
        // Arrange
        Task task = Task.builder()
                .status(TaskStatus.PENDING)
                .priority(priority)
                .dueDate(null)
                .build();

        // Act
        Optional<TaskUrgency> result = rule.evaluate(task, TODAY);

        // Assert
        assertEquals(Optional.of(TaskUrgency.ON_TRACK), result);
    }

    @Test
    @DisplayName("Dada una tarea con dueDate, cuando se evalúa la regla, entonces no decide y delega a la siguiente")
    void shouldNotDecideWhenDueDateIsPresent() {
        // Arrange
        Task task = Task.builder()
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH)
                .dueDate(TODAY.plusDays(12))
                .build();

        // Act
        Optional<TaskUrgency> result = rule.evaluate(task, TODAY);

        // Assert
        assertTrue(result.isEmpty());
    }
}
