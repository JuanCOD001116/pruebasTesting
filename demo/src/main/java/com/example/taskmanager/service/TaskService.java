package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;

import java.util.List;

public interface TaskService {

    TaskResponse createTask(CreateTaskRequest request);

    TaskResponse getTaskById(Long id);

    List<TaskResponse> getAllTasks();

    List<TaskResponse> getTasksByStatus(TaskStatus status);

    List<TaskResponse> getTasksByPriority(TaskPriority priority);

    List<TaskResponse> getTasksByStatusAndPriority(TaskStatus status, TaskPriority priority);

    TaskResponse updateTask(Long id, UpdateTaskRequest request);

    TaskResponse updateTaskStatus(Long id, TaskStatus newStatus);

    void deleteTask(Long id);
}
