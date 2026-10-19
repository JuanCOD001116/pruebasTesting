package com.example.taskmanager.service.urgency;

import com.example.taskmanager.config.UrgencyProperties;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.model.TaskUrgency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DueSoonRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 28);

    private final DueSoonRule rule = new DueSoonRule(new UrgencyProperties(Map.of(
            TaskPriority.HIGH, 3,
            TaskPriority.MEDIUM, 2,
            TaskPriority.LOW, 1
    )));

    @ParameterizedTest
    @CsvSource({
            "HIGH, 0",
            "HIGH, 3",
            "MEDIUM, 2",
            "LOW, 1"
    })
    @DisplayName("Dada una tarea que vence dentro de la ventana de su prioridad, cuando se evalúa la regla, entonces retorna AT_RISK")
    void shouldReturnAtRiskWhenDueDateIsWithinWindow(TaskPriority priority, int daysFromToday) {
        // Arrange
        Task task = Task.builder()
                .status(TaskStatus.PENDING)
                .priority(priority)
                .dueDate(TODAY.plusDays(daysFromToday))
                .build();

        // Act
        Optional<TaskUrgency> result = rule.evaluate(task, TODAY);

        // Assert
        assertEquals(Optional.of(TaskUrgency.AT_RISK), result);
    }

    @ParameterizedTest
    @CsvSource({
            "HIGH, 4",
            "MEDIUM, 3",
            "LOW, 2",
            "LOW, 45"
    })
    @DisplayName("Dada una tarea que vence después de la ventana de su prioridad, cuando se evalúa la regla, entonces no decide")
    void shouldNotDecideWhenDueDateIsAfterWindow(TaskPriority priority, int daysFromToday) {
        // Arrange
        Task task = Task.builder()
                .status(TaskStatus.IN_PROGRESS)
                .priority(priority)
                .dueDate(TODAY.plusDays(daysFromToday))
                .build();

        // Act
        Optional<TaskUrgency> result = rule.evaluate(task, TODAY);

        // Assert
        assertTrue(result.isEmpty());
    }
}
