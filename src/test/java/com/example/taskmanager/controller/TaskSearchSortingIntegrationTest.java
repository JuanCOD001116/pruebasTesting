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

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class TaskSearchSortingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        // Arrange
        taskRepository.deleteAll();
        taskRepository.saveAll(List.of(
                task("Ordenar reunión", TaskStatus.CANCELLED, TaskPriority.MEDIUM),
                task("Revisar contrato", TaskStatus.PENDING, TaskPriority.LOW),
                task("Corregir incidente", TaskStatus.IN_PROGRESS, TaskPriority.HIGH),
                task("Publicar versión", TaskStatus.DONE, TaskPriority.MEDIUM)
        ));
    }

    @Test
    @DisplayName("Dado tareas de todas las prioridades, cuando se busca con sort=priority,desc, entonces el orden es HIGH, MEDIUM, MEDIUM, LOW y no alfabético")
    void shouldSortByPriorityRankDescending() throws Exception {
        // Arrange: tareas sembradas en setUp()

        // Act & Assert
        mockMvc.perform(get("/api/tasks/search")
                        .param("sort", "priority,desc")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].priority").value(contains("HIGH", "MEDIUM", "MEDIUM", "LOW")))
                .andExpect(jsonPath("$.content[*].title").value(contains(
                        "Corregir incidente", "Ordenar reunión", "Publicar versión", "Revisar contrato")));
    }

    @Test
    @DisplayName("Dado tareas de todas las prioridades, cuando se busca con sort=priority,asc, entonces el orden es LOW, MEDIUM, MEDIUM, HIGH")
    void shouldSortByPriorityRankAscending() throws Exception {
        // Arrange: tareas sembradas en setUp()

        // Act & Assert
        mockMvc.perform(get("/api/tasks/search")
                        .param("sort", "priority,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].priority").value(contains("LOW", "MEDIUM", "MEDIUM", "HIGH")));
    }

    @Test
    @DisplayName("Dado tareas en todos los estados, cuando se busca con sort=status,asc, entonces sigue el ciclo de vida PENDING, IN_PROGRESS, DONE, CANCELLED")
    void shouldSortByStatusLifecycleOrder() throws Exception {
        // Arrange: tareas sembradas en setUp()

        // Act & Assert
        mockMvc.perform(get("/api/tasks/search")
                        .param("sort", "status,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status")
                        .value(contains("PENDING", "IN_PROGRESS", "DONE", "CANCELLED")));
    }

    @Test
    @DisplayName("Dado una búsqueda paginada ordenada por prioridad, cuando se pide la segunda página, entonces los totales no cambian por el ordenamiento")
    void shouldKeepPaginationTotalsWhenSortingByPriority() throws Exception {
        // Arrange: tareas sembradas en setUp()

        // Act & Assert
        mockMvc.perform(get("/api/tasks/search")
                        .param("sort", "priority,desc")
                        .param("sort", "title,asc")
                        .param("page", "1")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[*].title").value(contains("Revisar contrato")));
    }

    private static Task task(String title, TaskStatus status, TaskPriority priority) {
        return Task.builder().title(title).status(status).priority(priority).build();
    }
}
