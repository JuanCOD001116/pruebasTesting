package com.example.taskmanager.service.urgency;

import com.example.taskmanager.config.UrgencyProperties;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.model.TaskUrgency;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class TaskUrgencyEvaluator {

    private final Clock clock;
    private final UrgencyProperties urgencyProperties;

    public TaskUrgencyEvaluator(Clock clock, UrgencyProperties urgencyProperties) {
        this.clock = clock;
        this.urgencyProperties = urgencyProperties;
    }

    public TaskUrgency evaluate(Task task) {
        LocalDate today = LocalDate.now(clock);
        if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CANCELLED) {
            return TaskUrgency.NONE;
        } else {
            if (task.getDueDate() == null) {
                if (task.getPriority() == TaskPriority.HIGH) {
                    return TaskUrgency.AT_RISK;
                } else {
                    return TaskUrgency.ON_TRACK;
                }
            } else {
                if (task.getDueDate().isBefore(today)) {
                    return TaskUrgency.OVERDUE;
                } else if (!task.getDueDate().isAfter(today.plusDays(windowFor(task.getPriority())))) {
                    return TaskUrgency.AT_RISK;
                } else {
                    return TaskUrgency.ON_TRACK;
                }
            }
        }
    }

    private int windowFor(TaskPriority priority) {
        return urgencyProperties.getWindowFor(priority);
    }
}
