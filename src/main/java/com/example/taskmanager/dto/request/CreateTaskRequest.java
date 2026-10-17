package com.example.taskmanager.dto.request;

import com.example.taskmanager.model.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String description;

    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Size(max = 255)
    private String assignedTo;

    private LocalDate dueDate;

    public CreateTaskRequest() {
        // @Builder.Default moves the field initializer into the builder, so set it here as well
        this.priority = TaskPriority.MEDIUM;
    }

    public CreateTaskRequest(String title, String description, TaskPriority priority, String assignedTo,
                             LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.assignedTo = assignedTo;
        this.dueDate = dueDate;
    }
}
