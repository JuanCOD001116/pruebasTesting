package com.example.taskmanager.repository.projection;

import com.example.taskmanager.model.TaskPriority;

public interface PriorityCount {
    TaskPriority getPriority();
    long getTotal();
}
