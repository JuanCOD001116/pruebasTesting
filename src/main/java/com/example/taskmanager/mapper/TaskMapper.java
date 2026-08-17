package com.example.taskmanager.mapper;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.model.Task;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TaskMapper {

    public Task toEntity(CreateTaskRequest request) {
        return Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .assignedTo(request.getAssignedTo())
                .build();
    }

    public Task toEntity(UpdateTaskRequest request, Task existingTask) {
        if (request.getTitle() != null) {
            existingTask.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            existingTask.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            existingTask.setPriority(request.getPriority());
        }
        if (request.getAssignedTo() != null) {
            existingTask.setAssignedTo(request.getAssignedTo());
        }
        return existingTask;
    }

    public TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .assignedTo(task.getAssignedTo())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
