package com.example.taskmanager.service.validation;

import com.example.taskmanager.exception.InvalidTaskDataException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Component
public class TaskDueDateValidator {

    private final Clock clock;

    public TaskDueDateValidator(Clock clock) {
        this.clock = clock;
    }

    public void validateDueDateForCreation(LocalDate dueDate) {
        if (dueDate == null) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        if (dueDate.isBefore(today)) {
            throw new InvalidTaskDataException("Due date cannot be in the past");
        }
    }

    public void validateDueDateForUpdate(LocalDate existingDueDate, LocalDate newDueDate) {
        if (newDueDate == null || Objects.equals(existingDueDate, newDueDate)) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        if (newDueDate.isBefore(today)) {
            throw new InvalidTaskDataException("Due date cannot be in the past");
        }
    }
}
