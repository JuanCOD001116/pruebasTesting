package com.example.taskmanager.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskStatusTest {

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"DONE", "CANCELLED"})
    @DisplayName("Dado un estado DONE o CANCELLED, cuando se consulta isTerminal, entonces retorna verdadero")
    void shouldReturnTrueWhenStatusIsTerminal(TaskStatus status) {
        // Arrange & Act
        boolean result = status.isTerminal();

        // Assert
        assertTrue(result);
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"PENDING", "IN_PROGRESS"})
    @DisplayName("Dado un estado PENDING o IN_PROGRESS, cuando se consulta isTerminal, entonces retorna falso")
    void shouldReturnFalseWhenStatusIsNotTerminal(TaskStatus status) {
        // Arrange & Act
        boolean result = status.isTerminal();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Dado el enum TaskStatus, cuando se obtienen los activeStatuses, entonces contiene PENDING e IN_PROGRESS")
    void shouldReturnActiveStatusesExcludingTerminal() {
        // Arrange & Act
        Set<TaskStatus> activeStatuses = TaskStatus.activeStatuses();

        // Assert
        assertEquals(Set.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS), activeStatuses);
        assertFalse(activeStatuses.contains(TaskStatus.DONE));
        assertFalse(activeStatuses.contains(TaskStatus.CANCELLED));
    }
}
