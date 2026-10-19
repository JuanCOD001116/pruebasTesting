package com.example.taskmanager.service.impl;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.dto.request.TaskSearchCriteria;
import com.example.taskmanager.dto.response.PageResponse;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.model.TaskUrgency;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.specification.TaskSpecifications;
import com.example.taskmanager.service.TaskService;
import com.example.taskmanager.service.urgency.TaskUrgencyEvaluator;
import com.example.taskmanager.service.validation.SortValidator;
import com.example.taskmanager.service.validation.TaskDueDateValidator;
import com.example.taskmanager.service.validation.TaskStatusTransitionValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskStatusTransitionValidator taskStatusTransitionValidator;
    private final TaskDueDateValidator taskDueDateValidator;
    private final TaskUrgencyEvaluator taskUrgencyEvaluator;
    private final SortValidator sortValidator;
    private final Clock clock;

    public TaskServiceImpl(TaskRepository taskRepository,
                           TaskStatusTransitionValidator taskStatusTransitionValidator,
                           TaskDueDateValidator taskDueDateValidator,
                           TaskUrgencyEvaluator taskUrgencyEvaluator,
                           SortValidator sortValidator,
                           Clock clock) {
        this.taskRepository = taskRepository;
        this.taskStatusTransitionValidator = taskStatusTransitionValidator;
        this.taskDueDateValidator = taskDueDateValidator;
        this.taskUrgencyEvaluator = taskUrgencyEvaluator;
        this.sortValidator = sortValidator;
        this.clock = clock;
    }

    @Override
    public TaskResponse createTask(CreateTaskRequest request) {
        log.info("Creating new task");
        taskDueDateValidator.validateDueDateForCreation(request.getDueDate());
        Task task = TaskMapper.toEntity(request);
        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully with id: {}", savedTask.getId());
        return toResponse(savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        log.debug("Fetching task with id: {}", id);
        Task task = findTaskOrThrow(id);
        return toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> findTasks(TaskSearchCriteria criteria) {
        log.debug("Finding tasks with criteria: {}", criteria);
        LocalDate today = LocalDate.now(clock);
        Specification<Task> spec = TaskSpecifications.from(criteria, today);
        List<Task> tasks = taskRepository.findAll(spec);
        return tasks.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> searchTasks(TaskSearchCriteria criteria, Pageable pageable) {
        log.debug("Searching tasks with criteria: {} and pageable: {}", criteria, pageable);
        sortValidator.validate(pageable.getSort());
        LocalDate today = LocalDate.now(clock);
        Specification<Task> spec = TaskSpecifications.from(criteria, today);
        Page<Task> page = taskRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        return findTasks(null);
    }

    @Override
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        log.info("Updating task with id: {}", id);
        Task task = findTaskOrThrow(id);

        taskDueDateValidator.validateDueDateForUpdate(task.getDueDate(), request.getDueDate());

        if (request.getStatus() != null && request.getStatus() != task.getStatus()) {
            taskStatusTransitionValidator.validate(
                    task.getStatus(), request.getStatus(), resolveAssignee(request, task));
        }

        Task updatedTask = TaskMapper.toEntity(request, task);
        Task savedTask = taskRepository.save(updatedTask);
        log.info("Task updated successfully with id: {}", savedTask.getId());
        return toResponse(savedTask);
    }

    @Override
    public TaskResponse updateTaskStatus(Long id, TaskStatus newStatus) {
        log.info("Updating task status with id: {} to status: {}", id, newStatus);
        Task task = findTaskOrThrow(id);

        taskStatusTransitionValidator.validate(task.getStatus(), newStatus, task.getAssignedTo());
        task.setStatus(newStatus);
        Task savedTask = taskRepository.save(task);
        log.info("Task status updated successfully with id: {}", savedTask.getId());
        return toResponse(savedTask);
    }

    @Override
    public void deleteTask(Long id) {
        log.info("Deleting task with id: {}", id);
        if (!taskRepository.existsById(id)) {
            log.warn("Task not found with id: {}", id);
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
        log.info("Task deleted successfully with id: {}", id);
    }

    private static String resolveAssignee(UpdateTaskRequest request, Task task) {
        return request.getAssignedTo() != null ? request.getAssignedTo() : task.getAssignedTo();
    }

    private TaskResponse toResponse(Task task) {
        TaskUrgency urgency = taskUrgencyEvaluator.evaluate(task);
        return TaskMapper.toResponse(task, urgency);
    }

    private Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Task not found with id: {}", id);
                    return new TaskNotFoundException(id);
                });
    }
}
