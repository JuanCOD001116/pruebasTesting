package com.example.taskmanager.dto.request;

import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {

    private String title;

    private String description;

    private TaskPriority priority;

    private TaskStatus status;

    private String assignedTo;
}
