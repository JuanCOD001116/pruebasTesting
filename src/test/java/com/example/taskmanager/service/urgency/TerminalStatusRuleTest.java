package com.example.taskmanager.service.urgency;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.model.TaskUrgency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalStatusRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 9);

    private final TerminalStatusRule rule = new TerminalStatusRule();

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"DONE", "CANCELLED"})
    @DisplayName("Dada una tarea en estado terminal, cuando se evalúa la regla, entonces retorna NONE")
    void shouldReturnNoneWhenStatusIsTerminal(TaskStatus status) {
        // Arrange
        Task task = Task.builder()
                .status(status)
                .priority(TaskPriority.HIGH)
                .dueDate(TODAY.minusDays(4))
                .build();

        // Act
        Optional<TaskUrgency> result = rule.evaluate(task, TODAY);

        // Assert
        assertEquals(Optional.of(TaskUrgency.NONE), result);
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"PENDING", "IN_PROGRESS"})
    @DisplayName("Dada una tarea activa, cuando se evalúa la regla, entonces no decide y delega a la siguiente")
    void shouldNotDecideWhenStatusIsActive(TaskStatus status) {
        // Arrange
        Task task = Task.builder()
                .status(status)
                .priority(TaskPriority.LOW)
                .dueDate(TODAY.plusDays(7))
                .build();

        // Act
        Optional<TaskUrgency> result = rule.evaluate(task, TODAY);

        // Assert
        assertTrue(result.isEmpty());
    }
}
