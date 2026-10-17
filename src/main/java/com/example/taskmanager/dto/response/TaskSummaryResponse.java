package com.example.taskmanager.dto.response;

import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;

import java.math.BigDecimal;
import java.util.Map;

public record TaskSummaryResponse(
        long total,
        Map<TaskStatus, Long> byStatus,
        Map<TaskPriority, Long> byPriority,
        long overdue,
        BigDecimal completionRate) {
}
