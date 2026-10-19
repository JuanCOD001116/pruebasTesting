package com.example.taskmanager.service.urgency;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskUrgency;

import java.time.LocalDate;
import java.util.Optional;

public interface UrgencyRule {

    Optional<TaskUrgency> evaluate(Task task, LocalDate today);
}
