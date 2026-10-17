package com.example.taskmanager.repository.projection;

import com.example.taskmanager.model.TaskStatus;

public interface StatusCount {
    TaskStatus getStatus();
    long getTotal();
}
