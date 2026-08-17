package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.exception.InvalidTaskStateException;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task testTask;
    private CreateTaskRequest createRequest;
    private UpdateTaskRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Arrange
        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.MEDIUM)
                .assignedTo("John Doe")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateTaskRequest.builder()
                .title("New Task")
                .description("New Description")
                .priority(TaskPriority.HIGH)
                .assignedTo("Jane Doe")
                .build();

        updateRequest = UpdateTaskRequest.builder()
                .title("Updated Task")
                .priority(TaskPriority.LOW)
                .build();
    }

    @Test
    void shouldCreateTaskSuccessfully() {
        // Arrange
        Task savedTask = Task.builder()
                .id(1L)
                .title(createRequest.getTitle())
                .description(createRequest.getDescription())
                .status(TaskStatus.PENDING)
                .priority(createRequest.getPriority())
                .assignedTo(createRequest.getAssignedTo())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        // Act
        TaskResponse response = taskService.createTask(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(savedTask.getId(), response.getId());
        assertEquals(createRequest.getTitle(), response.getTitle());
        assertEquals(TaskStatus.PENDING, response.getStatus());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void shouldGetTaskByIdSuccessfully() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        // Act
        TaskResponse response = taskService.getTaskById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(testTask.getId(), response.getId());
        assertEquals(testTask.getTitle(), response.getTitle());
        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    void shouldThrowTaskNotFoundExceptionWhenTaskDoesNotExist() {
        // Arrange
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> taskService.getTaskById(999L));
        verify(taskRepository, times(1)).findById(999L);
    }

    @Test
    void shouldGetAllTasksSuccessfully() {
        // Arrange
        when(taskRepository.findAll()).thenReturn(Arrays.asList(testTask));

        // Act
        var response = taskService.getAllTasks();

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testTask.getId(), response.get(0).getId());
        verify(taskRepository, times(1)).findAll();
    }

    @Test
    void shouldUpdateTaskSuccessfully() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // Act
        TaskResponse response = taskService.updateTask(1L, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testTask.getId(), response.getId());
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void shouldThrowTaskNotFoundExceptionWhenUpdatingNonExistentTask() {
        // Arrange
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> taskService.updateTask(999L, updateRequest));
        verify(taskRepository, times(1)).findById(999L);
    }

    @Test
    void shouldUpdateTaskStatusSuccessfully() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // Act
        TaskResponse response = taskService.updateTaskStatus(1L, TaskStatus.IN_PROGRESS);

        // Assert
        assertNotNull(response);
        assertEquals(testTask.getId(), response.getId());
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void shouldThrowInvalidTaskStateExceptionWhenChangingStatusFromCancelled() {
        // Arrange
        testTask.setStatus(TaskStatus.CANCELLED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        // Act & Assert
        assertThrows(InvalidTaskStateException.class, 
                () -> taskService.updateTaskStatus(1L, TaskStatus.PENDING));
        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    void shouldDeleteTaskSuccessfully() {
        // Arrange
        when(taskRepository.existsById(1L)).thenReturn(true);

        // Act
        taskService.deleteTask(1L);

        // Assert
        verify(taskRepository, times(1)).existsById(1L);
        verify(taskRepository, times(1)).deleteById(1L);
    }

    @Test
    void shouldThrowTaskNotFoundExceptionWhenDeletingNonExistentTask() {
        // Arrange
        when(taskRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> taskService.deleteTask(999L));
        verify(taskRepository, times(1)).existsById(999L);
    }
}
