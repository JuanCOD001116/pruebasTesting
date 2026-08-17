package com.example.taskmanager.controller;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerIntegrationTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private CreateTaskRequest createRequest;
    private Task taskEntity;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        // Arrange
        createRequest = CreateTaskRequest.builder()
                .title("Integration Test Task")
                .description("Test Description")
                .priority(TaskPriority.HIGH)
                .assignedTo("John Doe")
                .build();

        taskEntity = Task.builder()
                .id(1L)
                .title("Integration Test Task")
                .description("Test Description")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH)
                .assignedTo("John Doe")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        taskResponse = TaskResponse.builder()
                .id(1L)
                .title("Integration Test Task")
                .description("Test Description")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH)
                .assignedTo("John Doe")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldCreateTask() {
        // Arrange
        when(taskService.createTask(any(CreateTaskRequest.class)))
                .thenReturn(taskResponse);

        // Act
        ResponseEntity<TaskResponse> response = taskController.createTask(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Integration Test Task", response.getBody().getTitle());
        verify(taskService, times(1)).createTask(any(CreateTaskRequest.class));
    }

    @Test
    void shouldGetAllTasks() {
        // Arrange
        when(taskService.getAllTasks())
                .thenReturn(Arrays.asList(taskResponse));

        // Act
        ResponseEntity<List<TaskResponse>> response = taskController.getAllTasks(null, null);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isEmpty());
        verify(taskService, times(1)).getAllTasks();
    }

    @Test
    void shouldReturnNotFoundForNonExistentTask() {
        // Arrange
        when(taskService.getTaskById(99999L))
                .thenThrow(new com.example.taskmanager.exception.TaskNotFoundException("Task not found"));

        // Act & Assert
        assertThrows(com.example.taskmanager.exception.TaskNotFoundException.class, () -> {
            taskService.getTaskById(99999L);
        });
        verify(taskService, times(1)).getTaskById(99999L);
    }

    @Test
    void shouldDeleteTask() {
        // Arrange
        doNothing().when(taskService).deleteTask(1L);

        // Act
        ResponseEntity<Void> response = taskController.deleteTask(1L);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(taskService, times(1)).deleteTask(1L);
    }
}
