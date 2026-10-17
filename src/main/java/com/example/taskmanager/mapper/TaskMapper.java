package com.example.taskmanager.mapper;

import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.model.Task;
import lombok.experimental.UtilityClass;

import java.util.function.Consumer;

@UtilityClass
public class TaskMapper {

    public Task toEntity(CreateTaskRequest request) {
        return Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .assignedTo(request.getAssignedTo())
                .dueDate(request.getDueDate())
                .build();
    }

    public Task toEntity(UpdateTaskRequest request, Task existingTask) {
        applyIfPresent(request.getTitle(), existingTask::setTitle);
        applyIfPresent(request.getDescription(), existingTask::setDescription);
        applyIfPresent(request.getPriority(), existingTask::setPriority);
        applyIfPresent(request.getStatus(), existingTask::setStatus);
        applyIfPresent(request.getAssignedTo(), existingTask::setAssignedTo);
        applyIfPresent(request.getDueDate(), existingTask::setDueDate);
        return existingTask;
    }

    private static <T> void applyIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    public TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .assignedTo(task.getAssignedTo())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
