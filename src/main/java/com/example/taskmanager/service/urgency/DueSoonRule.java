package com.example.taskmanager.service.urgency;

import com.example.taskmanager.config.UrgencyProperties;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskUrgency;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@Order(4)
@RequiredArgsConstructor
public class DueSoonRule implements UrgencyRule {

    private final UrgencyProperties urgencyProperties;

    @Override
    public Optional<TaskUrgency> evaluate(Task task, LocalDate today) {
        LocalDate threshold = today.plusDays(urgencyProperties.getWindowFor(task.getPriority()));
        if (!task.getDueDate().isAfter(threshold)) {
            return Optional.of(TaskUrgency.AT_RISK);
        }
        return Optional.empty();
    }
}
