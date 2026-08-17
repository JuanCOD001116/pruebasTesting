package com.example.taskmanager.controller;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerPerformanceTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private CreateTaskRequest createRequest;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        // Arrange
        createRequest = CreateTaskRequest.builder()
                .title("Performance Test Task")
                .description("Test Description")
                .priority(TaskPriority.MEDIUM)
                .assignedTo("Test User")
                .build();

        taskResponse = TaskResponse.builder()
                .id(1L)
                .title("Performance Test Task")
                .description("Test Description")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .assignedTo("Test User")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("should create task within reasonable time")
    void shouldCreateTaskWithinTimeThreshold() {
        // Arrange
        when(taskService.createTask(any(CreateTaskRequest.class)))
                .thenReturn(taskResponse);

        // Act
        long startTime = System.currentTimeMillis();
        ResponseEntity<TaskResponse> response = taskController.createTask(createRequest);
        long endTime = System.currentTimeMillis();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        long executionTime = endTime - startTime;
        assertTrue(executionTime < 5000, "Create task took " + executionTime + "ms");
    }

    @Test
    @DisplayName("should get all tasks within reasonable time")
    void shouldGetAllTasksWithinTimeThreshold() {
        // Arrange
        when(taskService.getAllTasks())
                .thenReturn(Arrays.asList(taskResponse));

        // Act
        long startTime = System.currentTimeMillis();
        ResponseEntity<java.util.List<TaskResponse>> response = taskController.getAllTasks(null, null);
        long endTime = System.currentTimeMillis();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        long executionTime = endTime - startTime;
        assertTrue(executionTime < 5000, "Get all tasks took " + executionTime + "ms");
    }
}
