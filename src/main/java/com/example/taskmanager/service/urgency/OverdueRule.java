package com.example.taskmanager.service.urgency;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskUrgency;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@Order(3)
public class OverdueRule implements UrgencyRule {

    @Override
    public Optional<TaskUrgency> evaluate(Task task, LocalDate today) {
        if (task.getDueDate().isBefore(today)) {
            return Optional.of(TaskUrgency.OVERDUE);
        }
        return Optional.empty();
    }
}
