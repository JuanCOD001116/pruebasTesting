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
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskUrgencyEvaluatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 16);
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-16T10:00:00Z"), ZoneId.of("UTC"));
    private static final UrgencyProperties PROPERTIES = new UrgencyProperties(Map.of(
            TaskPriority.HIGH, 3,
            TaskPriority.MEDIUM, 2,
            TaskPriority.LOW, 1
    ));

    private TaskUrgencyEvaluator evaluator;

    @BeforeEach
    void setUp() {
        // Arrange
        List<UrgencyRule> rules = List.of(
                new TerminalStatusRule(),
                new NoDueDateRule(),
                new OverdueRule(),
                new DueSoonRule(PROPERTIES)
        );
        evaluator = new TaskUrgencyEvaluator(rules, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Dado el contexto de Spring con las reglas registradas en desorden, cuando se evalúa, entonces se aplican en el orden de @Order")
    void shouldApplyRulesInOrderDefinedBySpringContext() {
        // Arrange
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withBean(Clock.class, () -> FIXED_CLOCK)
                .withBean(UrgencyProperties.class, () -> PROPERTIES)
                .withUserConfiguration(DueSoonRule.class, OverdueRule.class, NoDueDateRule.class,
                        TerminalStatusRule.class, TaskUrgencyEvaluator.class);
        Task cancelledAndOverdue = Task.builder()
                .status(TaskStatus.CANCELLED)
                .priority(TaskPriority.LOW)
                .dueDate(TODAY.minusDays(2))
                .build();
        Task activeWithoutDueDate = Task.builder()
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH)
                .dueDate(null)
                .build();

        // Act & Assert
        contextRunner.run(context -> {
            TaskUrgencyEvaluator contextEvaluator = context.getBean(TaskUrgencyEvaluator.class);
            assertEquals(TaskUrgency.NONE, contextEvaluator.evaluate(cancelledAndOverdue));
            assertEquals(TaskUrgency.AT_RISK, contextEvaluator.evaluate(activeWithoutDueDate));
        });
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"DONE", "CANCELLED"})
    @DisplayName("Regla 1 (Terminal): Dada una tarea en estado terminal, cuando se evalúa la urgencia, entonces retorna NONE sin importar dueDate ni prioridad")
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
    @DisplayName("Regla 2 (NoDueDate): Dada una tarea activa sin dueDate y prioridad HIGH, cuando se evalúa la urgencia, entonces retorna AT_RISK")
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
    @DisplayName("Regla 2 (NoDueDate): Dada una tarea activa sin dueDate y prioridad MEDIUM o LOW, cuando se evalúa la urgencia, entonces retorna ON_TRACK")
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
    @DisplayName("Regla 3 (Overdue): Dada una tarea activa con dueDate anterior a hoy, cuando se evalúa la urgencia, entonces retorna OVERDUE")
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
    @DisplayName("Regla 4 (DueSoon): Dada una tarea activa que vence hoy, cuando se evalúa la urgencia, entonces retorna AT_RISK para cualquier prioridad")
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
    @DisplayName("Regla 4 y default: Dada una tarea con dueDate en el borde o después de su ventana, cuando se evalúa la urgencia, entonces retorna AT_RISK u ON_TRACK")
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
