package com.example.taskmanager.mapper;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TaskMapperTest {

    private CreateTaskRequest createRequest;
    private UpdateTaskRequest updateRequest;
    private Task testTask;

    @BeforeEach
    void setUp() {
        // Arrange
        createRequest = CreateTaskRequest.builder()
                .title("Test Task")
                .description("Test Description")
                .priority(TaskPriority.HIGH)
                .assignedTo("John Doe")
                .dueDate(LocalDate.of(2026, 12, 31))
                .build();

        updateRequest = UpdateTaskRequest.builder()
                .title("Updated Title")
                .priority(TaskPriority.LOW)
                .dueDate(LocalDate.of(2027, 1, 15))
                .build();

        testTask = Task.builder()
                .id(1L)
                .title("Original Title")
                .description("Test Description")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .assignedTo("Jane Doe")
                .dueDate(LocalDate.of(2026, 12, 31))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Dado un CreateTaskRequest valido, cuando se mapea a entidad, entonces los campos coinciden")
    void shouldConvertCreateTaskRequestToEntity() {
        // Arrange & Act
        Task entity = TaskMapper.toEntity(createRequest);

        // Assert
        assertNotNull(entity);
        assertEquals(createRequest.getTitle(), entity.getTitle());
        assertEquals(createRequest.getDescription(), entity.getDescription());
        assertEquals(createRequest.getPriority(), entity.getPriority());
        assertEquals(createRequest.getAssignedTo(), entity.getAssignedTo());
        assertEquals(createRequest.getDueDate(), entity.getDueDate());
        assertEquals(TaskStatus.PENDING, entity.getStatus());
    }

    @Test
    @DisplayName("Dado un UpdateTaskRequest valido, cuando se actualiza la entidad, entonces los campos provistos se actualizan")
    void shouldConvertUpdateTaskRequestToEntity() {
        // Arrange & Act
        Task updatedEntity = TaskMapper.toEntity(updateRequest, testTask);

        // Assert
        assertNotNull(updatedEntity);
        assertEquals(updateRequest.getTitle(), updatedEntity.getTitle());
        assertEquals(updateRequest.getPriority(), updatedEntity.getPriority());
        assertEquals(updateRequest.getDueDate(), updatedEntity.getDueDate());
        assertEquals(testTask.getDescription(), updatedEntity.getDescription());
    }

    @Test
    @DisplayName("Dada una entidad Task, cuando se mapea a respuesta, entonces todos los campos coinciden")
    void shouldConvertTaskEntityToResponse() {
        // Arrange & Act
        TaskResponse response = TaskMapper.toResponse(testTask);

        // Assert
        assertNotNull(response);
        assertEquals(testTask.getId(), response.getId());
        assertEquals(testTask.getTitle(), response.getTitle());
        assertEquals(testTask.getDescription(), response.getDescription());
        assertEquals(testTask.getStatus(), response.getStatus());
        assertEquals(testTask.getPriority(), response.getPriority());
        assertEquals(testTask.getAssignedTo(), response.getAssignedTo());
        assertEquals(testTask.getDueDate(), response.getDueDate());
        assertEquals(testTask.getCreatedAt(), response.getCreatedAt());
        assertEquals(testTask.getUpdatedAt(), response.getUpdatedAt());
    }

    @Test
    @DisplayName("Dado un UpdateTaskRequest parcial, cuando se actualiza, entonces solo los campos presentes cambian")
    void shouldUpdateOnlyProvidedFieldsInUpdateRequest() {
        // Arrange
        UpdateTaskRequest partialUpdate = UpdateTaskRequest.builder()
                .title("New Title")
                .build();

        // Act
        Task updatedEntity = TaskMapper.toEntity(partialUpdate, testTask);

        // Assert
        assertEquals("New Title", updatedEntity.getTitle());
        assertEquals("Test Description", updatedEntity.getDescription());
        assertEquals(TaskPriority.MEDIUM, updatedEntity.getPriority());
        assertEquals("Jane Doe", updatedEntity.getAssignedTo());
        assertEquals(LocalDate.of(2026, 12, 31), updatedEntity.getDueDate());
    }
}
