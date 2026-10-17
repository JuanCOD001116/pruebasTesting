package com.example.taskmanager.service.urgency;

import com.example.taskmanager.config.UrgencyProperties;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.model.TaskUrgency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskUrgencyEvaluatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 16);
    private TaskUrgencyEvaluator evaluator;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-16T10:00:00Z"), ZoneId.of("UTC"));
        UrgencyProperties properties = new UrgencyProperties(Map.of(
                TaskPriority.HIGH, 3,
                TaskPriority.MEDIUM, 2,
                TaskPriority.LOW, 1
        ));
        evaluator = new TaskUrgencyEvaluator(fixedClock, properties);
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"DONE", "CANCELLED"})
    @DisplayName("Regla 1 (Terminal): Tarea en estado terminal siempre retorna NONE independientemente de dueDate o prioridad")
    void shouldReturnNoneForTerminalStatuses(TaskStatus status) {
        // Arrange
        Task taskWithDueDate = Task.builder()
                .status(status)
                .priority(TaskPriority.HIGH)
                .dueDate(TODAY.minusDays(5))
                .build();

        Task taskWithoutDueDate = Task.builder()
                .status(status)
                .priority(TaskPriority.LOW)
                .dueDate(null)
                .build();

        // Act & Assert
        assertEquals(TaskUrgency.NONE, evaluator.evaluate(taskWithDueDate));
        assertEquals(TaskUrgency.NONE, evaluator.evaluate(taskWithoutDueDate));
    }

    @Test
    @DisplayName("Regla 2 (No Due Date - HIGH): Tarea activa sin fecha y prioridad HIGH retorna AT_RISK")
    void shouldReturnAtRiskForActiveTaskWithoutDueDateAndHighPriority() {
        // Arrange
        Task task = Task.builder()
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH)
                .dueDate(null)
                .build();

        // Act
        TaskUrgency result = evaluator.evaluate(task);

        // Assert
        assertEquals(TaskUrgency.AT_RISK, result);
    }

    @ParameterizedTest
    @EnumSource(value = TaskPriority.class, names = {"MEDIUM", "LOW"})
    @DisplayName("Regla 2 (No Due Date - MEDIUM/LOW): Tarea activa sin fecha y prioridad no alta retorna ON_TRACK")
    void shouldReturnOnTrackForActiveTaskWithoutDueDateAndMediumOrLowPriority(TaskPriority priority) {
        // Arrange
        Task task = Task.builder()
                .status(TaskStatus.IN_PROGRESS)
                .priority(priority)
                .dueDate(null)
                .build();

        // Act
        TaskUrgency result = evaluator.evaluate(task);

        // Assert
        assertEquals(TaskUrgency.ON_TRACK, result);
    }

    @Test
    @DisplayName("Regla 3 (Overdue): Tarea activa con fecha anterior a hoy retorna OVERDUE")
    void shouldReturnOverdueForPastDueDate() {
        // Arrange
        Task task = Task.builder()
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .dueDate(TODAY.minusDays(1))
                .build();

        // Act
        TaskUrgency result = evaluator.evaluate(task);

        // Assert
        assertEquals(TaskUrgency.OVERDUE, result);
    }

    @ParameterizedTest
    @EnumSource(TaskPriority.class)
    @DisplayName("Regla 4 (Due Soon - Hoy): Tarea activa que vence hoy siempre retorna AT_RISK")
    void shouldReturnAtRiskWhenDueDateIsToday(TaskPriority priority) {
        // Arrange
        Task task = Task.builder()
                .status(TaskStatus.IN_PROGRESS)
                .priority(priority)
                .dueDate(TODAY)
                .build();

        // Act
        TaskUrgency result = evaluator.evaluate(task);

        // Assert
        assertEquals(TaskUrgency.AT_RISK, result);
    }

    @ParameterizedTest
    @CsvSource({
            "HIGH, 3, AT_RISK",
            "HIGH, 4, ON_TRACK",
            "MEDIUM, 2, AT_RISK",
            "MEDIUM, 3, ON_TRACK",
            "LOW, 1, AT_RISK",
            "LOW, 2, ON_TRACK"
    })
    @DisplayName("Regla 4 y Default (Ventanas de Alerta): Verifica límites exactos y días posteriores para cada prioridad")
    void shouldEvaluateUrgencyAccordingToWindowLimits(TaskPriority priority, int daysFromToday, TaskUrgency expected) {
        // Arrange
        Task task = Task.builder()
                .status(TaskStatus.PENDING)
                .priority(priority)
                .dueDate(TODAY.plusDays(daysFromToday))
                .build();

        // Act
        TaskUrgency result = evaluator.evaluate(task);

        // Assert
        assertEquals(expected, result);
    }
}
