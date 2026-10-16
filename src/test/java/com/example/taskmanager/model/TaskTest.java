package com.example.taskmanager.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TaskTest {

    @Test
    @DisplayName("Dado dos tareas con el mismo id, cuando se comparan con equals, entonces son iguales")
    void shouldBeEqualWhenSameId() {
        // Arrange
        Task task1 = Task.builder().id(1L).title("Task 1").build();
        Task task2 = Task.builder().id(1L).title("Task 2").build();

        // Act & Assert
        assertEquals(task1, task2);
        assertEquals(task1.hashCode(), task2.hashCode());
    }

    @Test
    @DisplayName("Dado dos tareas con diferente id, cuando se comparan con equals, entonces no son iguales")
    void shouldNotBeEqualWhenDifferentId() {
        // Arrange
        Task task1 = Task.builder().id(1L).title("Task 1").build();
        Task task2 = Task.builder().id(2L).title("Task 1").build();

        // Act & Assert
        assertNotEquals(task1, task2);
    }

    @Test
    @DisplayName("Dado una tarea sin id persistido, cuando se compara con otra tarea sin id, entonces no son iguales por id nulo")
    void shouldNotBeEqualWhenIdIsNull() {
        // Arrange
        Task task1 = Task.builder().title("Task 1").build();
        Task task2 = Task.builder().title("Task 1").build();

        // Act & Assert
        assertNotEquals(task1, task2);
    }

    @Test
    @DisplayName("Dado una tarea y un objeto de otro tipo o nulo, cuando se comparan con equals, entonces no son iguales")
    void shouldNotBeEqualWithNullOrDifferentClass() {
        // Arrange
        Task task = Task.builder().id(1L).build();

        // Act & Assert
        assertNotEquals(null, task);
        assertNotEquals("string", task);
        assertEquals(task, task);
    }
}
