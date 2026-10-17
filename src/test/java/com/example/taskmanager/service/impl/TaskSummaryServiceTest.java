package com.example.taskmanager.service.impl;

import com.example.taskmanager.dto.response.TaskSummaryResponse;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.projection.PriorityCount;
import com.example.taskmanager.repository.projection.StatusCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskSummaryServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private Clock fixedClock;
    private TaskSummaryServiceImpl taskSummaryService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-10-16T10:00:00Z"), ZoneId.of("UTC"));
        taskSummaryService = new TaskSummaryServiceImpl(taskRepository, fixedClock);
    }

    @Test
    @DisplayName("Dado un repositorio sin tareas, cuando se calcula el resumen, entonces todos los contadores estan en 0 y completionRate es 0.00")
    void shouldReturnZeroSummaryWhenNoTasksExist() {
        // Arrange
        when(taskRepository.countByStatus()).thenReturn(List.of());
        when(taskRepository.countByPriority()).thenReturn(List.of());
        when(taskRepository.count()).thenReturn(0L);
        when(taskRepository.countByDueDateBeforeAndStatusIn(any(LocalDate.class), any())).thenReturn(0L);

        // Act
        TaskSummaryResponse response = taskSummaryService.getSummary();

        // Assert
        assertNotNull(response);
        assertEquals(0L, response.total());
        assertEquals(0L, response.overdue());
        assertEquals(new BigDecimal("0.00"), response.completionRate());

        // Validate all enum keys exist in map with 0
        for (TaskStatus status : TaskStatus.values()) {
            assertEquals(0L, response.byStatus().get(status));
        }
        for (TaskPriority priority : TaskPriority.values()) {
            assertEquals(0L, response.byPriority().get(priority));
        }
    }

    @Test
    @DisplayName("Dado solo tareas canceladas (denominador cero), cuando se calcula el resumen, entonces completionRate no falla por division por cero y es 0.00")
    void shouldHandleZeroDenominatorWhenOnlyCancelledTasksExist() {
        // Arrange
        StatusCount cancelledCount = mock(StatusCount.class);
        when(cancelledCount.getStatus()).thenReturn(TaskStatus.CANCELLED);
        when(cancelledCount.getTotal()).thenReturn(5L);

        when(taskRepository.countByStatus()).thenReturn(List.of(cancelledCount));
        when(taskRepository.countByPriority()).thenReturn(List.of());
        when(taskRepository.count()).thenReturn(5L);
        when(taskRepository.countByDueDateBeforeAndStatusIn(any(LocalDate.class), any())).thenReturn(0L);

        // Act
        TaskSummaryResponse response = taskSummaryService.getSummary();

        // Assert
        assertEquals(5L, response.total());
        assertEquals(5L, response.byStatus().get(TaskStatus.CANCELLED));
        assertEquals(0L, response.byStatus().get(TaskStatus.DONE));
        assertEquals(new BigDecimal("0.00"), response.completionRate());
    }

    @Test
    @DisplayName("Dado un conjunto mixto de tareas, cuando se calcula el resumen, entonces redondea completionRate a 2 decimales con HALF_UP")
    void shouldCalculateMixedSummaryWithHalfUpRounding() {
        // Arrange
        // Total = 12, CANCELLED = 1 => Denominador = 11. DONE = 4 => 4 / 11 * 100 = 36.3636... -> 36.36
        StatusCount pending = mockStatusCount(TaskStatus.PENDING, 4L);
        StatusCount inProgress = mockStatusCount(TaskStatus.IN_PROGRESS, 3L);
        StatusCount done = mockStatusCount(TaskStatus.DONE, 4L);
        StatusCount cancelled = mockStatusCount(TaskStatus.CANCELLED, 1L);

        PriorityCount low = mockPriorityCount(TaskPriority.LOW, 2L);
        PriorityCount medium = mockPriorityCount(TaskPriority.MEDIUM, 6L);
        PriorityCount high = mockPriorityCount(TaskPriority.HIGH, 4L);

        when(taskRepository.countByStatus()).thenReturn(List.of(pending, inProgress, done, cancelled));
        when(taskRepository.countByPriority()).thenReturn(List.of(low, medium, high));
        when(taskRepository.count()).thenReturn(12L);
        when(taskRepository.countByDueDateBeforeAndStatusIn(eq(LocalDate.now(fixedClock)), any()))
                .thenReturn(2L);

        // Act
        TaskSummaryResponse response = taskSummaryService.getSummary();

        // Assert
        assertEquals(12L, response.total());
        assertEquals(2L, response.overdue());
        assertEquals(new BigDecimal("36.36"), response.completionRate());

        assertEquals(4L, response.byStatus().get(TaskStatus.PENDING));
        assertEquals(3L, response.byStatus().get(TaskStatus.IN_PROGRESS));
        assertEquals(4L, response.byStatus().get(TaskStatus.DONE));
        assertEquals(1L, response.byStatus().get(TaskStatus.CANCELLED));

        assertEquals(2L, response.byPriority().get(TaskPriority.LOW));
        assertEquals(6L, response.byPriority().get(TaskPriority.MEDIUM));
        assertEquals(4L, response.byPriority().get(TaskPriority.HIGH));
    }

    private StatusCount mockStatusCount(TaskStatus status, long total) {
        StatusCount count = mock(StatusCount.class);
        when(count.getStatus()).thenReturn(status);
        when(count.getTotal()).thenReturn(total);
        return count;
    }

    private PriorityCount mockPriorityCount(TaskPriority priority, long total) {
        PriorityCount count = mock(PriorityCount.class);
        when(count.getPriority()).thenReturn(priority);
        when(count.getTotal()).thenReturn(total);
        return count;
    }
}
