package com.example.taskmanager.controller;

import org.junit.jupiter.api.BeforeEach;
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
    void shouldRejectInvalidCancelledTransitionThroughPatch() throws Exception {
        // Arrange
        seedTask(1L, "Patch transition task", "HIGH", "marina");

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
    void shouldRejectInvalidCancelledTransitionThroughPut() throws Exception {
        // Arrange
        seedTask(1L, "Put transition task", "MEDIUM", "diego");

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
    void shouldRejectPutWithBlankTitle() throws Exception {
        // Arrange
        seedTask(2L, "Existing title", "MEDIUM", "diego");

        // Act & Assert
        mockMvc.perform(put("/api/tasks/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("Title must not be blank")));
    }

    private void seedTask(Long id, String title, String priority, String assignedTo) {
        jdbcTemplate.update(
                "insert into tasks (id, title, description, status, priority, assigned_to, "
                    + "created_at, updated_at) values (?, ?, null, 'CANCELLED', ?, ?, "
                    + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, title, priority, assignedTo);
    }
}
