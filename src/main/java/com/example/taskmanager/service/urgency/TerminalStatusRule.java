package com.example.taskmanager.service.urgency;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskUrgency;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@Order(1)
public class TerminalStatusRule implements UrgencyRule {

    @Override
    public Optional<TaskUrgency> evaluate(Task task, LocalDate today) {
        if (task.getStatus().isTerminal()) {
            return Optional.of(TaskUrgency.NONE);
        }
        return Optional.empty();
    }
}
