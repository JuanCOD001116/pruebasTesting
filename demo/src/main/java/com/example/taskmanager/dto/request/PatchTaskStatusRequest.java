package com.example.taskmanager.dto.request;

import com.example.taskmanager.model.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatchTaskStatusRequest {

    @NotNull(message = "Status is required")
    private TaskStatus status;
}
