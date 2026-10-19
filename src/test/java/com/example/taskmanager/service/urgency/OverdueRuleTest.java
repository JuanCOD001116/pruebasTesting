package com.example.taskmanager.service.urgency;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.model.TaskUrgency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverdueRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 2);

    private final OverdueRule rule = new OverdueRule();

    @Test
    @DisplayName("Dada una tarea cuyo dueDate fue ayer, cuando se evalúa la regla, entonces retorna OVERDUE")
    void shouldReturnOverdueWhenDueDateIsBeforeToday() {
        // Arrange
        Task task = Task.builder()
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.LOW)
                .dueDate(TODAY.minusDays(1))
                .build();

        // Act
        Optional<TaskUrgency> result = rule.evaluate(task, TODAY);

        // Assert
        assertEquals(Optional.of(TaskUrgency.OVERDUE), result);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 30})
    @DisplayName("Dada una tarea que vence hoy o después, cuando se evalúa la regla, entonces no decide y delega a la siguiente")
    void shouldNotDecideWhenDueDateIsTodayOrLater(int daysFromToday) {
        // Arrange
        Task task = Task.builder()
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .dueDate(TODAY.plusDays(daysFromToday))
                .build();

        // Act
        Optional<TaskUrgency> result = rule.evaluate(task, TODAY);

        // Assert
        assertTrue(result.isEmpty());
    }
}
