package com.example.taskmanager.dto.request;

import com.example.taskmanager.dto.validation.NullOrNotBlank;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UpdateTaskRequest {

    @NullOrNotBlank(message = "Title must not be blank")
    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String description;

    private TaskPriority priority;

    private TaskStatus status;

    @Size(max = 255)
    private String assignedTo;

    private LocalDate dueDate;

    public UpdateTaskRequest() {
    }

    public UpdateTaskRequest(String title, String description, TaskPriority priority, TaskStatus status,
                             String assignedTo, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.assignedTo = assignedTo;
        this.dueDate = dueDate;
    }
}
