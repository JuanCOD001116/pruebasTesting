package com.example.taskmanager.dto.request;

import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;

public record TaskSearchCriteria(
        TaskStatus status,
        TaskPriority priority,
        String assignedTo,
        String text,
        LocalDate dueFrom,
        LocalDate dueTo,
        Boolean overdue) {

    @AssertTrue(message = "dueFrom must be before or equal to dueTo")
    public boolean isDueRangeValid() {
        return dueFrom == null || dueTo == null || !dueFrom.isAfter(dueTo);
    }
}
