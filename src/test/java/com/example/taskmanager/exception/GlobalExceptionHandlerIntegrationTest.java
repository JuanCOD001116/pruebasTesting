package com.example.taskmanager.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Dado un parámetro de tipo enum inválido en query, cuando se consulta GET /api/tasks?status=FOO, entonces responde 400 estandarizado")
    void shouldReturn400WhenMethodArgumentTypeMismatch() throws Exception {
        // Arrange & Act & Assert
        mockMvc.perform(get("/api/tasks")
                        .param("status", "INVALID_ENUM_VALUE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Parámetro inválido 'status'")))
                .andExpect(jsonPath("$.path").value("/api/tasks"));
    }

    @Test
    @DisplayName("Dado un JSON mal formado en el body, cuando se envía POST /api/tasks, entonces responde 400 estandarizado")
    void shouldReturn400WhenHttpMessageNotReadable() throws Exception {
        // Arrange
        String malformedJson = "{ invalid json content ";

        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Cuerpo de solicitud mal formado o inválido"))
                .andExpect(jsonPath("$.path").value("/api/tasks"));
    }

    @Test
    @DisplayName("Dado un ID que no existe, cuando se consulta GET /api/tasks/{id}, entonces responde 404 estandarizado")
    void shouldReturn404WhenTaskNotFound() throws Exception {
        // Arrange & Act & Assert
        mockMvc.perform(get("/api/tasks/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(containsString("Task not found with id: 999999")))
                .andExpect(jsonPath("$.path").value("/api/tasks/999999"));
    }

    @Test
    @DisplayName("Dado un path variable inválido, cuando se consulta GET /api/tasks/{id} con texto, entonces responde 400 por type mismatch")
    void shouldReturn400WhenPathVariableTypeMismatch() throws Exception {
        // Arrange & Act & Assert
        mockMvc.perform(get("/api/tasks/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Parámetro inválido 'id'")))
                .andExpect(jsonPath("$.path").value("/api/tasks/not-a-number"));
    }
}
