package com.example.taskmanager.dto.response;

import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Long id;

    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private String assignedTo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
