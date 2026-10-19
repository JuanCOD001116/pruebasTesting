package com.example.taskmanager.repository.specification;

import com.example.taskmanager.dto.request.TaskSearchCriteria;
import com.example.taskmanager.model.Task;
import com.example.taskmanager.model.TaskPriority;
import com.example.taskmanager.model.TaskStatus;
import com.example.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("dev")
class TaskSpecificationsTest {

    @Autowired
    private TaskRepository taskRepository;

    private final LocalDate today = LocalDate.of(2026, 10, 16);

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();

        Task t1 = Task.builder()
                .title("Implement Authentication Module")
                .description("JWT security")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH)
                .assignedTo("alice")
                .dueDate(LocalDate.of(2026, 10, 10)) // Overdue (active + past)
                .build();

        Task t2 = Task.builder()
                .title("Create Database Migration")
                .description("Flyway scripts")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.MEDIUM)
                .assignedTo("Bob")
                .dueDate(LocalDate.of(2026, 10, 20)) // Future
                .build();

        Task t3 = Task.builder()
                .title("Write Documentation")
                .description("Swagger specs")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.LOW)
                .assignedTo("Alice")
                .dueDate(LocalDate.of(2026, 10, 5)) // Terminal (DONE), not considered overdue
                .build();

        Task t4 = Task.builder()
                .title("Fix Security Bug")
                .description("Urgent patch")
                .status(TaskStatus.CANCELLED)
                .priority(TaskPriority.HIGH)
                .assignedTo("Charlie")
                .dueDate(null)
                .build();

        taskRepository.saveAll(List.of(t1, t2, t3, t4));
    }

    @Test
    @DisplayName("Dado un filtro por status, cuando se ejecuta la consulta, entonces retorna solo las tareas con ese status")
    void shouldFilterByStatus() {
        // Arrange
        Specification<Task> spec = TaskSpecifications.hasStatus(TaskStatus.PENDING);

        // Act
        List<Task> result = taskRepository.findAll(spec);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Implement Authentication Module", result.get(0).getTitle());
    }

    @Test
    @DisplayName("Dado un filtro por prioridad, cuando se ejecuta la consulta, entonces retorna solo las tareas con esa prioridad")
    void shouldFilterByPriority() {
        // Arrange
        Specification<Task> spec = TaskSpecifications.hasPriority(TaskPriority.HIGH);

        // Act
        List<Task> result = taskRepository.findAll(spec);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.getPriority() == TaskPriority.HIGH));
    }

    @Test
    @DisplayName("Dado un filtro por assignedTo, cuando se busca sin distinguir mayúsculas, entonces retorna coincidencias exactas ignorando caso")
    void shouldFilterByAssignedToCaseInsensitive() {
        // Arrange
        Specification<Task> spec = TaskSpecifications.assignedToIgnoreCase("ALICE");

        // Act
        List<Task> result = taskRepository.findAll(spec);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.getAssignedTo().equalsIgnoreCase("alice")));
    }

    @Test
    @DisplayName("Dado un filtro de texto, cuando se busca parte del titulo sin distinguir mayúsculas, entonces retorna las tareas coincidentes")
    void shouldFilterByTitleContainingTextCaseInsensitive() {
        // Arrange
        Specification<Task> spec = TaskSpecifications.titleContains("mOdUlE");

        // Act
        List<Task> result = taskRepository.findAll(spec);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Implement Authentication Module", result.get(0).getTitle());
    }

    @Test
    @DisplayName("Dado un filtro por rango de fechas (dueFrom y dueTo), cuando se consulta, entonces retorna tareas dentro del intervalo")
    void shouldFilterByDueDateRange() {
        // Arrange
        Specification<Task> spec = Specification.where(TaskSpecifications.dueOnOrAfter(LocalDate.of(2026, 10, 8)))
                .and(TaskSpecifications.dueOnOrBefore(LocalDate.of(2026, 10, 15)));

        // Act
        List<Task> result = taskRepository.findAll(spec);

        // Assert
        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2026, 10, 10), result.get(0).getDueDate());
    }

    @Test
    @DisplayName("Dado overdue=true, cuando se ejecuta la consulta, entonces retorna solo tareas activas con fecha anterior a hoy")
    void shouldFilterOverdueTasks() {
        // Arrange
        Specification<Task> spec = TaskSpecifications.overdue(true, today);

        // Act
        List<Task> result = taskRepository.findAll(spec);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Implement Authentication Module", result.get(0).getTitle());
        assertTrue(result.get(0).getDueDate().isBefore(today));
        assertTrue(TaskStatus.activeStatuses().contains(result.get(0).getStatus()));
    }

    @Test
    @DisplayName("Dado overdue=false, cuando se ejecuta la consulta, entonces retorna tareas no vencidas o terminales")
    void shouldFilterNonOverdueTasks() {
        // Arrange
        Specification<Task> spec = TaskSpecifications.overdue(false, today);

        // Act
        List<Task> result = taskRepository.findAll(spec);

        // Assert
        assertEquals(3, result.size());
        assertFalse(result.stream().anyMatch(t -> t.getTitle().equals("Implement Authentication Module")));
    }

    @Test
    @DisplayName("Dado un objeto TaskSearchCriteria combinado con multiples filtros, cuando se consulta, entonces aplica todos conjuntamente")
    void shouldFilterWithCombinedCriteria() {
        // Arrange
        TaskSearchCriteria criteria = new TaskSearchCriteria(
                TaskStatus.IN_PROGRESS,
                TaskPriority.MEDIUM,
                "bob",
                "database",
                null,
                null,
                false
        );
        Specification<Task> spec = TaskSpecifications.from(criteria, today);

        // Act
        List<Task> result = taskRepository.findAll(spec);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Create Database Migration", result.get(0).getTitle());
    }

    @Test
    @DisplayName("Dado un criterio vacio o nulo, cuando se consulta, entonces retorna todas las tareas existentes")
    void shouldReturnAllTasksWhenCriteriaIsEmpty() {
        // Arrange
        TaskSearchCriteria emptyCriteria = new TaskSearchCriteria(null, null, null, null, null, null, null);
        Specification<Task> spec = TaskSpecifications.from(emptyCriteria, today);

        // Act
        List<Task> result = taskRepository.findAll(spec);

        // Assert
        assertEquals(4, result.size());
    }

    @Test
    @DisplayName("Dado assignedTo y text en blanco, cuando se consulta, entonces se ignoran como filtros y retorna todas las tareas")
    void shouldIgnoreBlankTextFilters() {
        // Arrange
        TaskSearchCriteria blankCriteria = new TaskSearchCriteria(null, null, "   ", "\t", null, null, null);
        Specification<Task> spec = TaskSpecifications.from(blankCriteria, today);

        // Act
        List<Task> result = taskRepository.findAll(spec);

        // Assert
        assertEquals(4, result.size());
    }

    @Test
    @DisplayName("Dado el filtro overdue sin fecha de referencia, cuando se arma la especificación, entonces no se aplica ningún filtro")
    void shouldNotBuildOverdueFilterWithoutReferenceDate() {
        // Arrange
        Boolean overdue = Boolean.TRUE;

        // Act
        Specification<Task> spec = TaskSpecifications.overdue(overdue, null);

        // Assert
        assertNull(spec);
    }
}
