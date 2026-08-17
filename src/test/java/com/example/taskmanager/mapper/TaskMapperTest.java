package com.example.taskmanager.mapper;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

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
                .build();

        updateRequest = UpdateTaskRequest.builder()
                .title("Updated Title")
                .priority(TaskPriority.LOW)
                .build();

        testTask = Task.builder()
                .id(1L)
                .title("Original Title")
                .description("Test Description")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .assignedTo("Jane Doe")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldConvertCreateTaskRequestToEntity() {
        // Arrange & Act
        Task entity = TaskMapper.toEntity(createRequest);

        // Assert
        assertNotNull(entity);
        assertEquals(createRequest.getTitle(), entity.getTitle());
        assertEquals(createRequest.getDescription(), entity.getDescription());
        assertEquals(createRequest.getPriority(), entity.getPriority());
        assertEquals(createRequest.getAssignedTo(), entity.getAssignedTo());
        assertEquals(TaskStatus.PENDING, entity.getStatus());
    }

    @Test
    void shouldConvertUpdateTaskRequestToEntity() {
        // Arrange & Act
        Task updatedEntity = TaskMapper.toEntity(updateRequest, testTask);

        // Assert
        assertNotNull(updatedEntity);
        assertEquals(updateRequest.getTitle(), updatedEntity.getTitle());
        assertEquals(updateRequest.getPriority(), updatedEntity.getPriority());
        assertEquals(testTask.getDescription(), updatedEntity.getDescription());
    }

    @Test
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
        assertEquals(testTask.getCreatedAt(), response.getCreatedAt());
        assertEquals(testTask.getUpdatedAt(), response.getUpdatedAt());
    }

    @Test
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
    }
}
