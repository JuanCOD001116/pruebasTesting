# 09 — Nuevas Funcionalidades (material real para el análisis)

## 1. Motivación y alcance

Las funcionalidades nuevas cumplen dos propósitos: (1) son coherentes con un gestor de tareas real, y (2) introducen lógica condicional que hace significativo el análisis de complejidad ciclomática y cognitiva. Todas respetan la arquitectura por capas, el DTO Pattern y la regla de no exponer entidades.

| ID | Funcionalidad | Patrón principal |
|---|---|---|
| F1 | Fecha límite (`dueDate`) + urgencia calculada | Chain of Responsibility (reglas ordenadas) |
| F2 | Búsqueda paginada con filtros combinables | Specification Pattern (Spring Data JPA) |
| F3 | Resumen estadístico de tareas | Projection + servicio dedicado (SRP) |

**Fuera de alcance:** autenticación, notificaciones, historial de cambios, borrar `dueDate` vía `PUT` (en `PUT`, `null` significa "no enviado"; se documenta como limitación conocida).

## 2. Infraestructura común

### 2.1 Reloj inyectable

`config/ClockConfig`:

```java
@Bean
public Clock clock(@Value("${app.time-zone:America/Bogota}") String zone) {
    return Clock.system(ZoneId.of(zone));
}
```

Toda lógica que dependa de "hoy" recibe `Clock` por constructor. En tests se usa `Clock.fixed(...)`. Lo usa también el `DateTimeProvider` de JPA Auditing (spec 08, H-05).

### 2.2 Métodos de dominio en `TaskStatus`

Sin renombrar los valores del enum (restricción del spec 01):

```java
public boolean isTerminal() {
    return TERMINAL.contains(this);
}

public static Set<TaskStatus> activeStatuses() {
    return EnumSet.complementOf(TERMINAL);
}

private static final Set<TaskStatus> TERMINAL = EnumSet.of(DONE, CANCELLED);
```

### 2.3 Nueva excepción

`InvalidTaskDataException extends RuntimeException` → 400 en `GlobalExceptionHandler` (datos válidos en formato pero inválidos para el negocio: fecha en el pasado, propiedad de ordenamiento no permitida).

---

## 3. F1 — Fecha límite y urgencia

### 3.1 Modelo y migración

- `Task.dueDate : LocalDate` (opcional).
- `src/main/resources/db/migration/V2__Add_due_date_to_tasks.sql`:

```sql
ALTER TABLE tasks ADD COLUMN due_date DATE;
CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date);
```

- DTOs: `dueDate` en `CreateTaskRequest`, `UpdateTaskRequest` y `TaskResponse`; además `TaskResponse.urgency : TaskUrgency` (calculado, **no** persistido).

### 3.2 Reglas de negocio

| # | Regla |
|---|---|
| R7 | Al crear, `dueDate` (si viene) no puede ser anterior a hoy → `InvalidTaskDataException`. |
| R8 | Al actualizar, si `dueDate` cambia, el nuevo valor no puede ser anterior a hoy. |
| R9 | La urgencia se calcula con las reglas de §3.3, en orden; gana la primera que aplique. |

R7/R8 se implementan en `service/validation/TaskDueDateValidator` (usa `Clock`). No se usa `@FutureOrPresent` porque Bean Validation usaría el reloj del sistema y no el `Clock` inyectado (tests no deterministas).

### 3.3 Cálculo de urgencia (`TaskUrgency`: `NONE`, `ON_TRACK`, `AT_RISK`, `OVERDUE`)

| Orden | Regla | Condición | Resultado |
|---|---|---|---|
| 1 | `TerminalStatusRule` | estado `DONE` o `CANCELLED` | `NONE` |
| 2 | `NoDueDateRule` | sin `dueDate` | `AT_RISK` si prioridad `HIGH`; si no, `ON_TRACK` |
| 3 | `OverdueRule` | `dueDate` < hoy | `OVERDUE` |
| 4 | `DueSoonRule` | `dueDate` ≤ hoy + ventana(prioridad) | `AT_RISK` |
| — | (por defecto) | ninguna aplicó | `ON_TRACK` |

Ventana de alerta por prioridad, configurable (`@ConfigurationProperties(prefix = "app.urgency")`, record `UrgencyProperties` con un `Map<TaskPriority, Integer>`): `HIGH = 3`, `MEDIUM = 2`, `LOW = 1` días. Justificación: a mayor prioridad, antes hay que alertar.

### 3.4 Diseño (versión final)

Paquete `com.example.taskmanager.service.urgency`:

```java
public interface UrgencyRule {
    Optional<TaskUrgency> evaluate(Task task, LocalDate today);
}

@Component
@RequiredArgsConstructor
public class TaskUrgencyEvaluator {
    private final List<UrgencyRule> rules;   // Spring inyecta en el orden de @Order
    private final Clock clock;

    public TaskUrgency evaluate(Task task) {
        LocalDate today = LocalDate.now(clock);
        return rules.stream()
                .map(rule -> rule.evaluate(task, today))
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(TaskUrgency.ON_TRACK);
    }
}
```

- Cada regla es un `@Component` con `@Order(n)` y una sola responsabilidad (abierta a extensión, cerrada a modificación).
- Ninguna regla debe superar ciclomática 3 ni cognitiva 2.
- El mapper recibe la urgencia ya calculada: `TaskMapper.toResponse(Task task, TaskUrgency urgency)`; el service usa un helper privado `toResponse(Task)` que invoca al evaluator.

**Caso de estudio obligatorio:** antes de esta versión se implementa y analiza una versión ingenua con `if` anidados. El procedimiento y el código están en el spec 11 §6.

### 3.5 Tests de F1

- `TaskUrgencyEvaluatorTest`: `@ParameterizedTest` con `Clock.fixed` cubriendo cada fila de §3.3 + bordes: vence hoy, vence exactamente en el último día de la ventana, día siguiente a la ventana, cada prioridad.
- Un test unitario por regla (rama verdadera y falsa).
- `TaskDueDateValidatorTest`: ayer (rechaza), hoy (acepta), mañana (acepta), `null` (acepta).
- MockMvc: `POST` con `dueDate` en el pasado → 400; `GET /{id}` devuelve `urgency`.

---

## 4. F2 — Búsqueda paginada con Specification Pattern

### 4.1 Endpoints

| Método | Endpoint | Respuesta |
|---|---|---|
| GET | `/api/tasks` | `List<TaskResponse>` (compatible con lo existente; acepta los mismos filtros nuevos) |
| GET | `/api/tasks/search` | `PageResponse<TaskResponse>` paginado y ordenable |

### 4.2 Criterios (objeto de parámetros, evita `S107`)

```java
public record TaskSearchCriteria(
        TaskStatus status,
        TaskPriority priority,
        String assignedTo,     // igualdad, sin distinguir mayúsculas
        String text,           // contenido en title, sin distinguir mayúsculas
        LocalDate dueFrom,
        LocalDate dueTo,
        Boolean overdue) {

    @AssertTrue(message = "dueFrom must be before or equal to dueTo")
    public boolean isDueRangeValid() {
        return dueFrom == null || dueTo == null || !dueFrom.isAfter(dueTo);
    }
}
```

En el controller: `@Valid @ParameterObject TaskSearchCriteria criteria` (springdoc documenta cada campo como query param).

### 4.3 Specifications

- `TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task>`.
- Eliminar `findByStatus`, `findByPriority`, `findByStatusAndPriority`, `findByAssignedTo` (quedan obsoletos).
- `repository/specification/TaskSpecifications` (clase utilitaria, constructor privado): una fábrica por filtro que devuelve `null` si el parámetro es `null`:

```java
static Specification<Task> hasStatus(TaskStatus status) {
    return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
}

public static Specification<Task> from(TaskSearchCriteria c, LocalDate today) {
    return Specification.allOf(Stream.of(
                    hasStatus(c.status()),
                    hasPriority(c.priority()),
                    assignedToIgnoreCase(c.assignedTo()),
                    titleContains(c.text()),
                    dueOnOrAfter(c.dueFrom()),
                    dueOnOrBefore(c.dueTo()),
                    overdue(c.overdue(), today))
            .filter(Objects::nonNull)
            .toList());
}
```

- `overdue(true)` = `dueDate < today` y estado activo; `overdue(false)` = lo contrario; `null` = sin filtro.
- Usar el metamodelo o constantes para los nombres de atributo (evita literales repetidos, `S1192`).

### 4.4 Paginación y ordenamiento

- `PageResponse<T>` (record: `content`, `page`, `size`, `totalElements`, `totalPages`) con `static <T> PageResponse<T> from(Page<T> page)`. No se serializa `PageImpl` directamente (Spring Data desaconseja exponerlo: su JSON no es estable).
- Tamaño máximo 100 (`spring.data.web.pageable.max-page-size`, spec 08 H-03). Por defecto `size=20`, `sort=createdAt,desc`.
- **Lista blanca de ordenamiento:** `id, title, status, priority, dueDate, createdAt, updatedAt`. Cualquier otra propiedad → `InvalidTaskDataException` (400). Validarlo en un componente `SortValidator` llamado desde el service. Sin esto, un `sort` desconocido produce un 500.

### 4.5 Service

`TaskService` reemplaza los tres métodos `getTasksBy...` por:

```java
List<TaskResponse> findTasks(TaskSearchCriteria criteria);
PageResponse<TaskResponse> searchTasks(TaskSearchCriteria criteria, Pageable pageable);
```

`getAllTasks()` se elimina o delega a `findTasks` con criterios vacíos. Actualizar `TaskServiceTest` y `TaskControllerTest`.

### 4.6 Tests de F2

- `@DataJpaTest` de `TaskSpecifications`: cada filtro solo, combinaciones de 2–3 filtros, criterios vacíos (devuelve todo), `text` sin distinguir mayúsculas.
- MockMvc: paginación (`totalElements`, `totalPages`), `sort` no permitido → 400, `dueFrom > dueTo` → 400, `status=FOO` → 400.

---

## 5. F3 — Resumen estadístico

### 5.1 Endpoint

`GET /api/tasks/summary` → `TaskSummaryResponse`:

```json
{
  "total": 12,
  "byStatus":   { "PENDING": 4, "IN_PROGRESS": 3, "DONE": 4, "CANCELLED": 1 },
  "byPriority": { "LOW": 2, "MEDIUM": 6, "HIGH": 4 },
  "overdue": 2,
  "completionRate": 36.36
}
```

### 5.2 Reglas

- `byStatus` y `byPriority` incluyen **todas** las claves del enum (con 0 si no hay tareas), usando `EnumMap`.
- `overdue` = tareas activas con `dueDate < hoy`.
- `completionRate` = `DONE / (total − CANCELLED) × 100`, `BigDecimal` con 2 decimales `HALF_UP`; `0.00` si el denominador es 0 (sin división por cero).

### 5.3 Diseño

- `service/TaskSummaryService` + `impl/TaskSummaryServiceImpl` (SRP: no inflar `TaskServiceImpl`). El endpoint vive en `TaskController`, que delega.
- Repositorio con proyecciones (una sola consulta por agrupación, nada de traer todas las tareas a memoria):

```java
interface StatusCount { TaskStatus getStatus(); long getTotal(); }

@Query("select t.status as status, count(t) as total from Task t group by t.status")
List<StatusCount> countByStatus();

long countByDueDateBeforeAndStatusIn(LocalDate date, Collection<TaskStatus> statuses);
```

- Ruta literal `/summary` antes de `/{id}` no genera conflicto (Spring prioriza rutas literales), pero agregar un test que lo confirme.

### 5.4 Tests de F3

- Unitarios con mocks: sin tareas (todo en 0, `completionRate = 0.00`), solo canceladas (denominador 0), caso mixto con redondeo.
- Integración (`@SpringBootTest` + MockMvc + H2) sembrando datos y verificando el JSON completo.

---

## 6. Cobertura (agregar a `jacocoTestCoverageVerification`)

```groovy
rule {
    element = 'CLASS'
    includes = ['com.example.taskmanager.service.urgency.*']
    limit {
        counter = 'BRANCH'
        minimum = 0.90
    }
}
```

## 7. Definición de "hecho"

- F1, F2 y F3 implementadas con migración `V2`, Swagger documentado (`@Operation`, `@ApiResponse`) y tests AAA.
- Ningún método nuevo supera ciclomática 10 / cognitiva 10 en SonarCloud.
- README actualizado con los endpoints nuevos.
- `./gradlew clean build` y el Quality Gate en **Passed**.
