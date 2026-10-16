package com.example.taskmanager.controller;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.exception.GlobalExceptionHandler;
import com.example.taskmanager.exception.TaskNotFoundException;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;
    private CreateTaskRequest createRequest;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        // Arrange
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        createRequest = CreateTaskRequest.builder()
                .title("Integration Test Task")
                .description("Test Description")
                .priority(TaskPriority.HIGH)
                .assignedTo("John Doe")
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
    @DisplayName("should create task")
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
    @DisplayName("should get all tasks")
    void shouldGetAllTasks() {
        // Arrange
        when(taskService.getAllTasks())
                .thenReturn(List.of(taskResponse));

        // Act
        ResponseEntity<List<TaskResponse>> response = taskController.getAllTasks(null, null);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isEmpty());
        verify(taskService, times(1)).getAllTasks();
    }

    @Test
    @DisplayName("should return 404 when task is not found")
    void shouldReturnNotFoundForNonExistentTask() throws Exception {
        // Arrange
        when(taskService.getTaskById(99999L))
                .thenThrow(new TaskNotFoundException("Task not found with id: 99999"));

        // Act & Assert
        mockMvc.perform(get("/api/tasks/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Task not found with id: 99999"));
        verify(taskService, times(1)).getTaskById(99999L);
    }

    @Test
    @DisplayName("should delete task")
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
