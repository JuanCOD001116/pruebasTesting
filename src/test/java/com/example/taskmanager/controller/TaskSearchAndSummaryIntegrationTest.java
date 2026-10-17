package com.example.taskmanager.controller;

import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class TaskSearchAndSummaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();

        // 12 tasks total: 4 DONE, 3 IN_PROGRESS, 4 PENDING, 1 CANCELLED
        // Denominator = 12 - 1 = 11. Completion rate = 4 / 11 * 100 = 36.36%
        Task t1 = Task.builder().title("Task A").status(TaskStatus.DONE).priority(TaskPriority.LOW).build();
        Task t2 = Task.builder().title("Task B").status(TaskStatus.DONE).priority(TaskPriority.LOW).build();
        Task t3 = Task.builder().title("Task C").status(TaskStatus.DONE).priority(TaskPriority.MEDIUM).build();
        Task t4 = Task.builder().title("Task D").status(TaskStatus.DONE).priority(TaskPriority.MEDIUM).build();

        Task t5 = Task.builder().title("Task E").status(TaskStatus.IN_PROGRESS).priority(TaskPriority.MEDIUM).assignedTo("dev1").build();
        Task t6 = Task.builder().title("Task F").status(TaskStatus.IN_PROGRESS).priority(TaskPriority.MEDIUM).assignedTo("dev2").build();
        Task t7 = Task.builder().title("Task G").status(TaskStatus.IN_PROGRESS).priority(TaskPriority.HIGH).assignedTo("dev3").build();

        Task t8 = Task.builder().title("Task H").status(TaskStatus.PENDING).priority(TaskPriority.HIGH).dueDate(LocalDate.of(2020, 1, 1)).build();
        Task t9 = Task.builder().title("Task I").status(TaskStatus.PENDING).priority(TaskPriority.HIGH).dueDate(LocalDate.of(2020, 1, 2)).build();
        Task t10 = Task.builder().title("Task J").status(TaskStatus.PENDING).priority(TaskPriority.HIGH).build();
        Task t11 = Task.builder().title("Task K").status(TaskStatus.PENDING).priority(TaskPriority.MEDIUM).build();

        Task t12 = Task.builder().title("Task L").status(TaskStatus.CANCELLED).priority(TaskPriority.MEDIUM).build();

        taskRepository.saveAll(List.of(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12));
    }

    @Test
    @DisplayName("Dado tareas existentes, cuando se consulta GET /api/tasks/summary, entonces retorna estadísticas correctas con tasa de completitud 36.36%")
    void shouldReturnSummaryWithCorrectStatisticsAndCompletionRate() throws Exception {
        // Arrange & Act & Assert
        mockMvc.perform(get("/api/tasks/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.completionRate").value(36.36))
                .andExpect(jsonPath("$.byStatus.DONE").value(4))
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(3))
                .andExpect(jsonPath("$.byStatus.PENDING").value(4))
                .andExpect(jsonPath("$.byStatus.CANCELLED").value(1))
                .andExpect(jsonPath("$.byPriority.LOW").value(2))
                .andExpect(jsonPath("$.byPriority.MEDIUM").value(6))
                .andExpect(jsonPath("$.byPriority.HIGH").value(4))
                .andExpect(jsonPath("$.overdue").value(2));
    }

    @Test
    @DisplayName("Dado el endpoint /api/tasks/summary, cuando se consulta, entonces no colisiona con GET /api/tasks/{id}")
    void shouldNotConfuseSummaryEndpointWithGetById() throws Exception {
        // Arrange & Act & Assert
        mockMvc.perform(get("/api/tasks/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    @DisplayName("Dado parametros de paginacion y ordenamiento, cuando se consulta GET /api/tasks/search, entonces retorna PageResponse ordenado")
    void shouldSearchTasksWithPaginationAndSorting() throws Exception {
        // Arrange & Act & Assert
        mockMvc.perform(get("/api/tasks/search")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value("Task A"))
                .andExpect(jsonPath("$.content[1].title").value("Task B"))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(6))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    @DisplayName("Dada una propiedad de ordenamiento no permitida, cuando se consulta GET /api/tasks/search, entonces retorna 400 Bad Request")
    void shouldRejectInvalidSortPropertyWith400() throws Exception {
        // Arrange & Act & Assert
        mockMvc.perform(get("/api/tasks/search")
                        .param("sort", "password,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort property: password")));
    }

    @Test
    @DisplayName("Dado dueFrom mayor que dueTo, cuando se consulta GET /api/tasks/search, entonces retorna 400 por validación de rango")
    void shouldRejectInvalidDueRangeWith400() throws Exception {
        // Arrange & Act & Assert
        mockMvc.perform(get("/api/tasks/search")
                        .param("dueFrom", "2026-12-31")
                        .param("dueTo", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
