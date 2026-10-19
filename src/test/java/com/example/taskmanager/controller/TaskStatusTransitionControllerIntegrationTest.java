package com.example.taskmanager.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class TaskStatusTransitionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Arrange
        jdbcTemplate.update("delete from tasks");
    }

    @Test
    @DisplayName("Dada una tarea CANCELLED, cuando se envía PATCH a PENDING, entonces responde 400 con el error estandarizado")
    void shouldRejectInvalidCancelledTransitionThroughPatch() throws Exception {
        // Arrange
        seedTask(1L, "Patch transition task", "CANCELLED", "HIGH", "marina");

        // Act & Assert
        mockMvc.perform(patch("/api/tasks/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("CANCELLED")))
                .andExpect(jsonPath("$.message", containsString("PENDING")));
    }

    @Test
    @DisplayName("Dada una tarea CANCELLED, cuando se envía PUT con status PENDING, entonces responde 400 porque PUT no salta la validación")
    void shouldRejectInvalidCancelledTransitionThroughPut() throws Exception {
        // Arrange
        seedTask(1L, "Put transition task", "CANCELLED", "MEDIUM", "diego");

        // Act & Assert
        mockMvc.perform(put("/api/tasks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated title\",\"status\":\"PENDING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("CANCELLED")))
                .andExpect(jsonPath("$.message", containsString("PENDING")));
    }

    @Test
    @DisplayName("Dada una tarea existente, cuando se envía PUT con título en blanco, entonces responde 400 por validación")
    void shouldRejectPutWithBlankTitle() throws Exception {
        // Arrange
        seedTask(2L, "Existing title", "CANCELLED", "MEDIUM", "diego");

        // Act & Assert
        mockMvc.perform(put("/api/tasks/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("Title must not be blank")));
    }

    @Test
    @DisplayName("Dada una tarea PENDING asignada, cuando se envía PATCH a IN_PROGRESS, entonces responde 200 con el nuevo estado")
    void shouldAcceptValidTransitionThroughPatch() throws Exception {
        // Arrange
        seedTask(3L, "Preparar demo del sprint", "PENDING", "LOW", "valentina");

        // Act & Assert
        mockMvc.perform(patch("/api/tasks/{id}/status", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignedTo").value("valentina"));
    }

    @Test
    @DisplayName("Dada una tarea IN_PROGRESS, cuando se envía PUT con status DONE y nuevo título, entonces responde 200 con los cambios aplicados")
    void shouldAcceptValidTransitionThroughPut() throws Exception {
        // Arrange
        seedTask(4L, "Revisar pull request", "IN_PROGRESS", "HIGH", "tomás");

        // Act & Assert
        mockMvc.perform(put("/api/tasks/{id}", 4L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Revisar y aprobar pull request\",\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.title").value("Revisar y aprobar pull request"))
                .andExpect(jsonPath("$.urgency").value("NONE"));
    }

    private void seedTask(Long id, String title, String status, String priority, String assignedTo) {
        jdbcTemplate.update(
                "insert into tasks (id, title, description, status, priority, assigned_to, "
                    + "created_at, updated_at) values (?, ?, null, ?, ?, ?, "
                    + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, title, status, priority, assignedTo);
    }
}
