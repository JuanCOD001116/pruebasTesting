package com.example.taskmanager.config;

import com.example.taskmanager.model.TaskPriority;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.urgency")
public record UrgencyProperties(Map<TaskPriority, Integer> windows) {

    private static final Map<TaskPriority, Integer> DEFAULT_WINDOWS = Map.of(
            TaskPriority.HIGH, 3,
            TaskPriority.MEDIUM, 2,
            TaskPriority.LOW, 1
    );

    public UrgencyProperties(Map<TaskPriority, Integer> windows) {
        if (windows == null || windows.isEmpty()) {
            this.windows = DEFAULT_WINDOWS;
        } else {
            Map<TaskPriority, Integer> merged = new EnumMap<>(TaskPriority.class);
            merged.putAll(DEFAULT_WINDOWS);
            merged.putAll(windows);
            this.windows = Map.copyOf(merged);
        }
    }

    public int getWindowFor(TaskPriority priority) {
        if (priority == null) {
            return 1;
        }
        return windows.getOrDefault(priority, 1);
    }
}
