package com.example.taskmanager.service.urgency;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskUrgency;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class TaskUrgencyEvaluator {

    private final List<UrgencyRule> rules;
    private final Clock clock;

    public TaskUrgencyEvaluator(List<UrgencyRule> rules, Clock clock) {
        this.rules = rules;
        this.clock = clock;
    }

    public TaskUrgency evaluate(Task task) {
        LocalDate today = LocalDate.now(clock);
        return rules.stream()
                .map(rule -> rule.evaluate(task, today))
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(TaskUrgency.ON_TRACK);
    }
}
