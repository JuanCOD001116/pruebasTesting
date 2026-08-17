package com.example.taskmanager.service.impl;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.exception.InvalidTaskStateException;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    @Override
    public TaskResponse createTask(CreateTaskRequest request) {
        log.info("Creating task with title: {}", request.getTitle());
        Task task = TaskMapper.toEntity(request);
        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully with id: {}", savedTask.getId());
        return TaskMapper.toResponse(savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        log.debug("Fetching task with id: {}", id);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Task not found with id: {}", id);
                    return new TaskNotFoundException(id);
                });
        return TaskMapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        log.debug("Fetching all tasks");
        List<Task> tasks = taskRepository.findAll();
        return tasks.stream()
                .map(TaskMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByStatus(TaskStatus status) {
        log.debug("Fetching tasks with status: {}", status);
        List<Task> tasks = taskRepository.findByStatus(status);
        return tasks.stream()
                .map(TaskMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByPriority(TaskPriority priority) {
        log.debug("Fetching tasks with priority: {}", priority);
        List<Task> tasks = taskRepository.findByPriority(priority);
        return tasks.stream()
                .map(TaskMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByStatusAndPriority(TaskStatus status, TaskPriority priority) {
        log.debug("Fetching tasks with status: {} and priority: {}", status, priority);
        List<Task> tasks = taskRepository.findByStatusAndPriority(status, priority);
        return tasks.stream()
                .map(TaskMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        log.info("Updating task with id: {}", id);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Task not found with id: {}", id);
                    return new TaskNotFoundException(id);
                });
        
        Task updatedTask = TaskMapper.toEntity(request, task);
        Task savedTask = taskRepository.save(updatedTask);
        log.info("Task updated successfully with id: {}", savedTask.getId());
        return TaskMapper.toResponse(savedTask);
    }

    @Override
    public TaskResponse updateTaskStatus(Long id, TaskStatus newStatus) {
        log.info("Updating task status with id: {} to status: {}", id, newStatus);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Task not found with id: {}", id);
                    return new TaskNotFoundException(id);
                });
        
        // Business rule: cannot change status from CANCELLED to another status
        if (task.getStatus() == TaskStatus.CANCELLED && newStatus != TaskStatus.CANCELLED) {
            log.error("Cannot change status from CANCELLED to {} for task id: {}", newStatus, id);
            throw new InvalidTaskStateException("Cannot change status of a cancelled task");
        }
        
        task.setStatus(newStatus);
        Task savedTask = taskRepository.save(task);
        log.info("Task status updated successfully with id: {}", savedTask.getId());
        return TaskMapper.toResponse(savedTask);
    }

    @Override
    public void deleteTask(Long id) {
        log.info("Deleting task with id: {}", id);
        if (!taskRepository.existsById(id)) {
            log.error("Task not found with id: {}", id);
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
        log.info("Task deleted successfully with id: {}", id);
    }
}
