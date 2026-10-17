package com.example.taskmanager.service.impl;

import com.example.taskmanager.dto.response.TaskSummaryResponse;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.service.TaskSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TaskSummaryServiceImpl implements TaskSummaryService {

    private final TaskRepository taskRepository;
    private final Clock clock;

    @Override
    public TaskSummaryResponse getSummary() {
        log.debug("Calculating tasks summary statistics");

        Map<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            byStatus.put(status, 0L);
        }
        taskRepository.countByStatus().forEach(sc -> byStatus.put(sc.getStatus(), sc.getTotal()));

        Map<TaskPriority, Long> byPriority = new EnumMap<>(TaskPriority.class);
        for (TaskPriority priority : TaskPriority.values()) {
            byPriority.put(priority, 0L);
        }
        taskRepository.countByPriority().forEach(pc -> byPriority.put(pc.getPriority(), pc.getTotal()));

        long total = taskRepository.count();

        LocalDate today = LocalDate.now(clock);
        long overdue = taskRepository.countByDueDateBeforeAndStatusIn(today, TaskStatus.activeStatuses());

        long doneCount = byStatus.get(TaskStatus.DONE);
        long cancelledCount = byStatus.get(TaskStatus.CANCELLED);
        long denominator = total - cancelledCount;

        BigDecimal completionRate;
        if (denominator <= 0) {
            completionRate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            completionRate = BigDecimal.valueOf(doneCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
        }

        return new TaskSummaryResponse(total, byStatus, byPriority, overdue, completionRate);
    }
}
