package com.example.taskmanager.dto.response;

import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.model.TaskUrgency;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TaskResponse {

    private Long id;

    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private String assignedTo;

    private LocalDate dueDate;

    private TaskUrgency urgency;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public TaskResponse() {
    }

    public TaskResponse(Long id, String title, String description, TaskStatus status, TaskPriority priority,
                        String assignedTo, LocalDate dueDate, TaskUrgency urgency,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.assignedTo = assignedTo;
        this.dueDate = dueDate;
        this.urgency = urgency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
