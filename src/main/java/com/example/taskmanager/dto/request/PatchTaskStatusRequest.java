package com.example.taskmanager.dto.request;

import com.example.taskmanager.model.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatchTaskStatusRequest {

    @NotNull(message = "Status is required")
    private TaskStatus status;

    public PatchTaskStatusRequest() {
    }

    public PatchTaskStatusRequest(TaskStatus status) {
        this.status = status;
    }
}
