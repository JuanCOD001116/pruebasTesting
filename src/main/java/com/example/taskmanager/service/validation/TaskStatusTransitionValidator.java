package com.example.taskmanager.service.validation;

import com.example.taskmanager.exception.InvalidTaskStateException;
import com.example.taskmanager.model.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class TaskStatusTransitionValidator {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            TaskStatus.PENDING, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED),
            TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.PENDING, TaskStatus.DONE, TaskStatus.CANCELLED),
            TaskStatus.DONE, EnumSet.noneOf(TaskStatus.class),
            TaskStatus.CANCELLED, EnumSet.noneOf(TaskStatus.class));

    public void validate(TaskStatus currentStatus, TaskStatus newStatus, String assignedTo) {
        if (currentStatus == newStatus) {
            throw invalid(currentStatus, newStatus, "una tarea no puede mantener el mismo estado");
        }

        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus)) {
            throw invalid(currentStatus, newStatus, "la transición no está permitida");
        }

        if (currentStatus == TaskStatus.PENDING
                && newStatus == TaskStatus.IN_PROGRESS
                && (assignedTo == null || assignedTo.isBlank())) {
            throw invalid(currentStatus, newStatus, "falta assignedTo para iniciar la tarea");
        }
    }

    private InvalidTaskStateException invalid(
            TaskStatus currentStatus, TaskStatus newStatus, String reason) {
        return new InvalidTaskStateException(
                "Transición de estado inválida de " + currentStatus + " a " + newStatus + ": " + reason);
    }
}
