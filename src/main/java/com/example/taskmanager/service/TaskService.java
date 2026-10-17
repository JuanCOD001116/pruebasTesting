package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.model.TaskStatus;

import com.example.taskmanager.dto.request.TaskSearchCriteria;
import com.example.taskmanager.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskService {

    TaskResponse createTask(CreateTaskRequest request);

    TaskResponse getTaskById(Long id);

    List<TaskResponse> findTasks(TaskSearchCriteria criteria);

    PageResponse<TaskResponse> searchTasks(TaskSearchCriteria criteria, Pageable pageable);

    default List<TaskResponse> getAllTasks() {
        return findTasks(null);
    }

    TaskResponse updateTask(Long id, UpdateTaskRequest request);

    TaskResponse updateTaskStatus(Long id, TaskStatus newStatus);

    void deleteTask(Long id);
}
