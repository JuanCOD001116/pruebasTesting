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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        // Act & Assert
        assertTimeout(Duration.ofMillis(200), () -> {
            ResponseEntity<TaskResponse> response = taskController.createTask(createRequest);
            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
        });
        verify(taskService, times(1)).createTask(any(CreateTaskRequest.class));
    }

    @Test
    @DisplayName("should get all tasks within reasonable time")
    void shouldGetAllTasksWithinTimeThreshold() {
        // Arrange
        when(taskService.getAllTasks())
                .thenReturn(List.of(taskResponse));

        // Act & Assert
        assertTimeout(Duration.ofMillis(200), () -> {
            ResponseEntity<List<TaskResponse>> response = taskController.getAllTasks(null, null);
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        });
        verify(taskService, times(1)).getAllTasks();
    }
}
