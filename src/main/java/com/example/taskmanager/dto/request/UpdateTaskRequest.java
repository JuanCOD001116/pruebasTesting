package com.example.taskmanager.dto.request;

import com.example.taskmanager.dto.validation.NullOrNotBlank;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
