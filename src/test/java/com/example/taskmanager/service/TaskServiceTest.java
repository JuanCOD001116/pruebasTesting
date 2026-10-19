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
import com.example.taskmanager.service.validation.TaskStatusTransitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.taskmanager.dto.request.TaskSearchCriteria;
import com.example.taskmanager.dto.response.PageResponse;
import com.example.taskmanager.service.validation.SortValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.taskmanager.model.TaskUrgency;
import com.example.taskmanager.service.urgency.TaskUrgencyEvaluator;
import com.example.taskmanager.service.validation.TaskDueDateValidator;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskStatusTransitionValidator taskStatusTransitionValidator;

    @Mock
    private TaskDueDateValidator taskDueDateValidator;

    @Mock
    private TaskUrgencyEvaluator taskUrgencyEvaluator;

    @Spy
    private SortValidator sortValidator = new SortValidator();

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-10-16T10:00:00Z"), ZoneId.of("UTC"));

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

        lenient().when(taskUrgencyEvaluator.evaluate(any())).thenReturn(TaskUrgency.ON_TRACK);
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
        assertEquals(TaskUrgency.ON_TRACK, response.getUrgency());
        verify(taskDueDateValidator, times(1)).validateDueDateForCreation(createRequest.getDueDate());
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
    @DisplayName("Dado un criterio de busqueda, cuando se ejecuta findTasks, entonces retorna la lista mapeada a respuesta")
    @SuppressWarnings("unchecked")
    void shouldFindTasksSuccessfullyWithCriteria() {
        // Arrange
        TaskSearchCriteria criteria = new TaskSearchCriteria(
                TaskStatus.PENDING, TaskPriority.MEDIUM, null, null, null, null, null
        );
        when(taskRepository.findAll(any(Specification.class))).thenReturn(List.of(testTask));

        // Act
        List<TaskResponse> response = taskService.findTasks(criteria);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testTask.getId(), response.get(0).getId());
        verify(taskRepository, times(1)).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Dado un criterio y paginacion valida, cuando se ejecuta searchTasks, entonces retorna PageResponse")
    @SuppressWarnings("unchecked")
    void shouldSearchTasksWithPaginationSuccessfully() {
        // Arrange
        TaskSearchCriteria criteria = new TaskSearchCriteria(null, null, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of(testTask), pageable, 1);
        when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        PageResponse<TaskResponse> response = taskService.searchTasks(criteria, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(1L, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(0, response.page());
        assertEquals(10, response.size());
        verify(taskRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Dado getAllTasks, cuando se invoca, entonces delega a findTasks con criterio nulo")
    @SuppressWarnings("unchecked")
    void shouldGetAllTasksDelegatingToFindTasks() {
        // Arrange
        when(taskRepository.findAll(any(Specification.class))).thenReturn(List.of(testTask));

        // Act
        List<TaskResponse> response = taskService.getAllTasks();

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        verify(taskRepository, times(1)).findAll(any(Specification.class));
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
        assertEquals(TaskUrgency.ON_TRACK, response.getUrgency());
        verify(taskDueDateValidator, times(1)).validateDueDateForUpdate(testTask.getDueDate(), updateRequest.getDueDate());
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("Dado un PUT que cambia el estado y trae assignedTo, cuando se actualiza, entonces valida la transición con el assignedTo del request")
    void shouldValidateTransitionWithRequestAssigneeWhenStatusChanges() {
        // Arrange
        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .status(TaskStatus.IN_PROGRESS)
                .assignedTo("Lucía Pérez")
                .build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // Act
        taskService.updateTask(1L, request);

        // Assert
        verify(taskStatusTransitionValidator, times(1))
                .validate(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, "Lucía Pérez");
    }

    @Test
    @DisplayName("Dado un PUT que cambia el estado sin assignedTo, cuando se actualiza, entonces valida la transición con el assignedTo actual de la tarea")
    void shouldValidateTransitionWithCurrentAssigneeWhenRequestHasNone() {
        // Arrange
        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .status(TaskStatus.CANCELLED)
                .build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // Act
        taskService.updateTask(1L, request);

        // Assert
        verify(taskStatusTransitionValidator, times(1))
                .validate(TaskStatus.PENDING, TaskStatus.CANCELLED, "John Doe");
    }

    @Test
    @DisplayName("Dado un PUT con el mismo estado actual, cuando se actualiza, entonces no invoca el validador de transición")
    void shouldSkipTransitionValidationWhenStatusIsUnchanged() {
        // Arrange
        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Mismo estado, nuevo título")
                .status(TaskStatus.PENDING)
                .build();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // Act
        taskService.updateTask(1L, request);

        // Assert
        verify(taskStatusTransitionValidator, never()).validate(any(), any(), any());
    }

    @Test
    @DisplayName("Dado un dueDate inválido al crear, cuando se invoca createTask, entonces lanza InvalidTaskDataException")
    void shouldThrowInvalidTaskDataExceptionWhenCreatingTaskWithInvalidDueDate() {
        // Arrange
        doThrow(new com.example.taskmanager.exception.InvalidTaskDataException("Due date cannot be in the past"))
                .when(taskDueDateValidator)
                .validateDueDateForCreation(createRequest.getDueDate());

        // Act & Assert
        assertThrows(com.example.taskmanager.exception.InvalidTaskDataException.class,
                () -> taskService.createTask(createRequest));
        verify(taskRepository, times(0)).save(any(Task.class));
    }

    @Test
    @DisplayName("Dado un dueDate inválido al actualizar, cuando se invoca updateTask, entonces lanza InvalidTaskDataException")
    void shouldThrowInvalidTaskDataExceptionWhenUpdatingTaskWithInvalidDueDate() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        doThrow(new com.example.taskmanager.exception.InvalidTaskDataException("Due date cannot be in the past"))
                .when(taskDueDateValidator)
                .validateDueDateForUpdate(testTask.getDueDate(), updateRequest.getDueDate());

        // Act & Assert
        assertThrows(com.example.taskmanager.exception.InvalidTaskDataException.class,
                () -> taskService.updateTask(1L, updateRequest));
        verify(taskRepository, times(0)).save(any(Task.class));
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
        verify(taskStatusTransitionValidator, times(1))
            .validate(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, "John Doe");
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void shouldThrowInvalidTaskStateExceptionWhenChangingStatusFromCancelled() {
        // Arrange
        testTask.setStatus(TaskStatus.CANCELLED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        doThrow(new InvalidTaskStateException("invalid transition"))
            .when(taskStatusTransitionValidator)
            .validate(TaskStatus.CANCELLED, TaskStatus.PENDING, "John Doe");

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
