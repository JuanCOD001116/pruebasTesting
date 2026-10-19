package com.example.taskmanager.service.urgency;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskUrgency;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@Order(2)
public class NoDueDateRule implements UrgencyRule {

    @Override
    public Optional<TaskUrgency> evaluate(Task task, LocalDate today) {
        if (task.getDueDate() != null) {
            return Optional.empty();
        }
        return Optional.of(task.getPriority() == TaskPriority.HIGH
                ? TaskUrgency.AT_RISK
                : TaskUrgency.ON_TRACK);
    }
}
