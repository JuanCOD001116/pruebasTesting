package com.example.taskmanager.service.validation;

import com.example.taskmanager.exception.InvalidTaskStateException;
import com.example.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basis-path documentation for TaskStatusTransitionValidator.validate.
 *
 * <p>Decision points: 3 if + 2 &amp;&amp; + 1 || = 6 decision points -&gt; V(G) = 6 + 1 = 7.
 *
 * <p>Basis paths and traceability (McCabe method):
 *
 * <ul>
 *   <li>Path 1 (d1=T): same status -&gt; rejects (rule 3) -&gt; TC-01, TC-07, TC-12, TC-17.
 *   <li>Path 2 (d1=F, d2=T): disallowed transition -&gt; rejects (not allowed) -&gt; TC-04, TC-10, TC-14.
 *   <li>Path 3 (d1=F, d2=F, d3=T, d4=T, d5=T): PENDING to IN_PROGRESS with assignedTo null -&gt; rejects -&gt; TC-03.
 *   <li>Path 4 (d1=F, d2=F, d3=T, d4=T, d5=F, d6=T): PENDING to IN_PROGRESS with assignedTo blank -&gt; rejects -&gt; TC-18.
 *   <li>Path 5 (d1=F, d2=F, d3=T, d4=T, d5=F, d6=F): PENDING to IN_PROGRESS with valid assignedTo -&gt; accepts -&gt; TC-02.
 *   <li>Path 6 (d1=F, d2=F, d3=T, d4=F): allowed transition, current == PENDING, new != IN_PROGRESS -&gt; accepts -&gt; TC-05 (PENDING -&gt; CANCELLED).
 *   <li>Path 7 (d1=F, d2=F, d3=F): allowed transition, current != PENDING (short-circuit) -&gt; accepts -&gt; TC-06, TC-08, TC-09.
 * </ul>
 */
class TaskStatusTransitionValidatorTest {

    @ParameterizedTest(name = "{index}: {0} -> {1} (assignedTo={2}) => {3}")
    @MethodSource("transitionCases")
    @DisplayName("Dado un estado origen, cuando se solicita una transición, entonces el resultado coincide con la regla de negocio")
    void shouldRespectTransitionMatrix(
            TaskStatus from, TaskStatus to, String assignedTo, boolean expectedValid) {
        // Arrange
        TaskStatusTransitionValidator validator = new TaskStatusTransitionValidator();

        // Act & Assert
        if (expectedValid) {
            assertDoesNotThrow(() -> validator.validate(from, to, assignedTo));
        } else {
            assertThrows(InvalidTaskStateException.class,
                    () -> validator.validate(from, to, assignedTo));
        }
    }

    @Test
    @DisplayName("Dado que una tarea está CANCELLED, cuando se intenta reabrirla, entonces se rechaza con mensaje claro")
    void shouldRejectReopeningCancelledTask() {
        // Arrange
        TaskStatus currentStatus = TaskStatus.CANCELLED;
        TaskStatus newStatus = TaskStatus.PENDING;
        TaskStatusTransitionValidator validator = new TaskStatusTransitionValidator();

        // Act
        InvalidTaskStateException exception = assertThrows(
                InvalidTaskStateException.class,
                () -> validator.validate(currentStatus, newStatus, "lucia"));

        // Assert
        assertTrue(exception.getMessage().contains("CANCELLED"));
        assertTrue(exception.getMessage().contains("PENDING"));
    }

    @Test
    @DisplayName("Dado que una tarea está PENDING sin asignación, cuando se inicia, entonces se rechaza indicando la causa")
    void shouldRejectStartingPendingTaskWithoutAssignment() {
        // Arrange
        TaskStatus currentStatus = TaskStatus.PENDING;
        TaskStatus newStatus = TaskStatus.IN_PROGRESS;
        TaskStatusTransitionValidator validator = new TaskStatusTransitionValidator();

        // Act
        InvalidTaskStateException exception = assertThrows(
                InvalidTaskStateException.class,
                () -> validator.validate(currentStatus, newStatus, null));

        // Assert
        assertTrue(exception.getMessage().contains("assignedTo"));
        assertTrue(exception.getMessage().contains("PENDING"));
        assertTrue(exception.getMessage().contains("IN_PROGRESS"));
    }

    private static Stream<Arguments> transitionCases() {
        return Stream.of(
                Arguments.of(TaskStatus.PENDING, TaskStatus.PENDING, "juan", false),
                Arguments.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, "marina", true),
                Arguments.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, null, false),
                Arguments.of(TaskStatus.PENDING, TaskStatus.DONE, "oscar", false),
                Arguments.of(TaskStatus.PENDING, TaskStatus.CANCELLED, "ines", true),
                Arguments.of(TaskStatus.IN_PROGRESS, TaskStatus.PENDING, "diego", true),
                Arguments.of(TaskStatus.IN_PROGRESS, TaskStatus.IN_PROGRESS, "sofia", false),
                Arguments.of(TaskStatus.IN_PROGRESS, TaskStatus.DONE, "raul", true),
                Arguments.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED, "elena", true),
                Arguments.of(TaskStatus.DONE, TaskStatus.PENDING, "bruno", false),
                Arguments.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS, "carla", false),
                Arguments.of(TaskStatus.DONE, TaskStatus.DONE, "mateo", false),
                Arguments.of(TaskStatus.DONE, TaskStatus.CANCELLED, "valeria", false),
                Arguments.of(TaskStatus.CANCELLED, TaskStatus.PENDING, "nicolas", false),
                Arguments.of(TaskStatus.CANCELLED, TaskStatus.IN_PROGRESS, "paula", false),
                Arguments.of(TaskStatus.CANCELLED, TaskStatus.DONE, "tomas", false),
                Arguments.of(TaskStatus.CANCELLED, TaskStatus.CANCELLED, "ines", false),
                Arguments.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, "   ", false));
    }
}
