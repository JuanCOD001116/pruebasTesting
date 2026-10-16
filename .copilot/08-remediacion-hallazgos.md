# 08 — Remediación de Hallazgos del Código Actual

## 1. Objetivo

Corregir los problemas del código existente que el análisis estático (y una revisión manual) detecta. Cada hallazgo tiene un ID para trazarlo en el informe: *hallazgo → regla Sonar (si aplica) → commit que lo corrige*.

Ejecutar esta fase **después** de capturar la línea base (spec 04 §3), para poder mostrar la reducción de issues.

## 2. Tabla resumen

| ID | Severidad | Archivo(s) | Problema | Regla Sonar relacionada |
|---|---|---|---|---|
| H-01 | Alta | `settings.gradle`, `Dockerfile` | Nombre de proyecto `demo` y JAR hardcodeado | — (bug de build) |
| H-02 | Media | `application.properties` | Configuración duplicada que contradice `application.yml` | — |
| H-03 | Alta | `application*.yml` | Dialecto PostgreSQL global aplicado a H2; Flyway y `create-drop` compitiendo | — |
| H-04 | Alta | `Dockerfile`, `docker-compose.yml` | Healthcheck a `/actuator/health` sin Actuator; `curl` inexistente en alpine; contenedor como root | `docker:S6471` (hotspot) |
| H-05 | Media | `Task.java` | `@Data` en entidad JPA; timestamps inicializados en el campo | — (buena práctica JPA) |
| H-06 | Alta | `GlobalExceptionHandler` | Duplicación; fuga de `ex.getMessage()` en el 500; errores de entrada devuelven 500 | `S1192`, duplicación |
| H-07 | Media | `TaskServiceImpl` | `findById(...).orElseThrow` repetido 3 veces; `Collectors.toList()`; log de datos del usuario; nivel de log incorrecto | `S6204`, `S1192`, log injection |
| H-08 | Media | `TaskController` | Lógica de filtrado (`if / else if`) en el controller; wildcard import | `S2208` |
| H-09 | Alta | `UpdateTaskRequest` | `PUT` permite `title` vacío (se salta la regla "title obligatorio") | — (bug de negocio) |
| H-10 | Media | `build.gradle` | Versiones forzadas que pisan el BOM de Spring Boot; dependencias redundantes | — |
| H-11 | Media | Tests de controller | Test "de integración" que no lo es; test que prueba el mock; imports/campos sin uso; performance test sin `assertTimeout` | `S1068`, `S1128`, `S2208` |
| H-12 | Media | `TaskStatusTransitionValidatorTest` | Javadoc dice V(G)=6, el valor real es 7; falta un camino base | — (documentación) |
| H-13 | Baja | `TaskMapper` | `toEntity(Update)` con 5 `if` (ciclomática 6) | `S1541` (preventivo) |
| H-14 | Alta | `application-prod.yml`, `docker-compose.yml` | Contraseñas por defecto / hardcodeadas | hotspot de credenciales |
| H-15 | Baja | `README.md` | `docker compose up -build` (debe ser `--build`); estructura y conteo de tests desactualizados | — |

## 3. Detalle y corrección esperada

### H-01 — Nombre del proyecto y JAR

- `settings.gradle`: `rootProject.name = 'task-manager-api'`.
- `build.gradle`: deshabilitar el *plain jar* y fijar el nombre del *boot jar* para no depender de la versión en el Dockerfile:

```groovy
tasks.named('jar') { enabled = false }
tasks.named('bootJar') { archiveFileName = 'task-manager-api.jar' }
```

- `Dockerfile`: `COPY --from=builder /app/build/libs/task-manager-api.jar app.jar`.

### H-02 — `application.properties` duplicado

Eliminar `src/main/resources/application.properties`. Toda la configuración vive en YAML (`spring.application.name` ya está en `application.yml`).

### H-03 — Perfiles de base de datos

- `application.yml` (común): **quitar** `hibernate.dialect` (Hibernate 6 lo detecta solo). Mantener `ddl-auto: validate` y Flyway habilitado.
- `application-dev.yml`: Flyway es la única fuente del esquema en ambos perfiles (como pide `project-planning.md` §5). Cambiar a:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:taskdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
  jpa:
    hibernate:
      ddl-auto: validate
```

- Quitar `database-platform` explícitos.
- Con esto, `TaskStatusTransitionControllerIntegrationTest` ya no necesita sobreescribir `spring.datasource.url`.
- Agregar `spring.data.web.pageable.max-page-size: 100` en `application.yml` (usado por F2, spec 09).

### H-04 — Docker

- Agregar `implementation 'org.springframework.boot:spring-boot-starter-actuator'` y exponer solo `health`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
```

- Dockerfile (stage final): crear usuario sin privilegios y no copiar `gradlew.bat`:

```dockerfile
RUN addgroup -S app && adduser -S app -G app
USER app
```

- `docker-compose.yml`: el healthcheck de `app` debe usar `wget` (incluido en alpine) contra `/actuator/health`, no `curl` contra Swagger.

### H-05 — Entidad `Task`

- Reemplazar `@Data` por `@Getter @Setter` + `equals/hashCode` seguros para Hibernate:

```java
@Override
public boolean equals(Object o) {
    if (this == o) {
        return true;
    }
    if (!(o instanceof Task other)) {
        return false;
    }
    return id != null && id.equals(other.id);
}

@Override
public int hashCode() {
    return getClass().hashCode();
}
```

  `@Data` genera `equals/hashCode` sobre todos los campos mutables (incluido `id`, que cambia al persistir) y `toString` con todos los campos.
- Timestamps con **JPA Auditing** en lugar de inicializar en el campo + `@PreUpdate`:
  - `@EntityListeners(AuditingEntityListener.class)`, `@CreatedDate` (con `updatable = false`) y `@LastModifiedDate`.
  - `config/JpaAuditingConfig` con `@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")` y un `DateTimeProvider` que use el `Clock` (spec 09 §2). Va en una clase de config propia, **no** en `TaskManagerApplication`, para no romper tests de slice.

### H-06 — `GlobalExceptionHandler`

- Extraer `private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, WebRequest request)` y usarlo en todos los handlers (elimina la duplicación).
- El handler genérico (`Exception`) debe responder un mensaje fijo (`"Error interno del servidor"`), **nunca** `ex.getMessage()` (puede filtrar detalles internos). El detalle va solo al log.
- Agregar handlers 400 para entradas mal formadas, que hoy terminan en 500:
  - `MethodArgumentTypeMismatchException` (ej. `?status=FOO`).
  - `HttpMessageNotReadableException` (JSON inválido o enum inexistente en el body).
  - `InvalidTaskDataException` (nueva, spec 09).
- Niveles de log: 4xx → `warn`, 5xx → `error` (con stacktrace).
- Tests: un test MockMvc por handler nuevo, verificando código y cuerpo estandarizado.

### H-07 — `TaskServiceImpl`

- Extraer `private Task findTaskOrThrow(Long id)`; usarlo en `getTaskById`, `updateTask`, `updateTaskStatus`.
- `.collect(Collectors.toList())` → `.toList()`.
- Corregir la indentación del bloque de validación en `updateTask`.
- No registrar datos libres enviados por el usuario (`request.getTitle()`) en logs: registrar solo IDs (riesgo de *log injection* y de datos personales en logs).
- "Tarea no encontrada" es un caso de negocio esperado: `log.warn`, no `log.error`.
- Los métodos `getTasksByStatus/Priority/StatusAndPriority` se reemplazan por la búsqueda con criterios (spec 09, F2).

### H-08 — `TaskController`

- Eliminar la cadena `if / else if` de `getAllTasks`: el controller solo arma `TaskSearchCriteria` y delega (spec 09, F2). Ciclomática pasa de 5 a 1.
- Reemplazar `import org.springframework.web.bind.annotation.*;` por imports explícitos.

### H-09 — Validación de `UpdateTaskRequest`

Hoy `PUT {"title": ""}` deja una tarea sin título. Crear la anotación `@NullOrNotBlank` (paquete `dto.validation`) con su `ConstraintValidator`: `null` es válido (campo no enviado), cualquier otro valor debe tener texto.

```java
@NullOrNotBlank(message = "Title must not be blank")
@Size(max = 255)
private String title;

@Size(max = 1000)
private String description;

@Size(max = 255)
private String assignedTo;
```

Aplicar también `@Size` a `CreateTaskRequest` (coherente con las columnas del V1). Tests: unitarios del validador + MockMvc de `PUT` con título en blanco → 400.

### H-10 — `build.gradle`

- Quitar versiones explícitas de `junit-jupiter` y `mockito-*` (el BOM de Spring Boot ya las gestiona; forzarlas genera conflictos).
- Quitar `spring-boot-test-autoconfigure` y `spring-boot-starter-logging` (vienen transitivamente).
- Actualizar `springdoc-openapi-starter-webmvc-ui` a la línea compatible con Spring Boot 3.3 (2.6.x).

### H-11 — Tests de controller

- `TaskControllerIntegrationTest` usa mocks y llama métodos directamente: **renombrar** a `TaskControllerTest` (es unitario). Las pruebas de integración reales son las de MockMvc.
- `shouldReturnNotFoundForNonExistentTask` llama `taskService.getTaskById` directamente (prueba el mock, no el controller). Reescribir con MockMvc verificando 404 y cuerpo de error.
- Eliminar el campo `taskEntity` y los imports sin uso (`WebRequest`, `eq`, `Task`).
- Reemplazar imports comodín (`Assertions.*`, `Mockito.*`) por imports explícitos.
- `TaskControllerPerformanceTest`: usar `assertTimeout(Duration.ofMillis(200), () -> ...)` como pide `project-planning.md` §6, en lugar de `System.currentTimeMillis()` con 5000 ms.

### H-12 — Documentación de caminos base del validador

El Javadoc enumera **6 puntos de decisión** (3 `if` + `&&`, `&&`, `||`) y concluye `V(G) = 6`. Error: `V(G) = puntos de decisión + 1 = 7`. Con 7 caminos, el camino 6 actual mezcla dos recorridos distintos. Corregir así:

| Camino | Recorrido | Caso |
|---|---|---|
| 6 | Permitida, `current == PENDING` verdadero, `new == IN_PROGRESS` falso | TC-05 (PENDING → CANCELLED) |
| 7 | Permitida, `current == PENDING` falso (cortocircuito) | TC-06, TC-08, TC-09 |

Detalle del cálculo en el spec 10 §4.

### H-13 — `TaskMapper.toEntity(UpdateTaskRequest, Task)`

Cinco `if (x != null)` → ciclomática 6, cognitiva 5. Refactor a un helper genérico sin ramas en el método público:

```java
private static <T> void applyIfPresent(T value, Consumer<T> setter) {
    if (value != null) {
        setter.accept(value);
    }
}
// uso
applyIfPresent(request.getTitle(), existingTask::setTitle);
```

`toEntity` queda en ciclomática 1 / cognitiva 0; el helper en 2 / 1. Los tests de `TaskMapperTest` deben seguir pasando sin cambios.

> Nota: con F1, el estado ya no se asigna en el mapper sin validación previa; mantener el orden actual del service (validar transición → aplicar cambios).

### H-14 — Credenciales

- `application-prod.yml`: `password: ${SPRING_DATASOURCE_PASSWORD}` **sin** valor por defecto (si falta, la app no arranca: *fail fast*). Igual para URL y usuario.
- `docker-compose.yml`: leer `POSTGRES_PASSWORD` y `SPRING_DATASOURCE_PASSWORD` desde `.env` (`${POSTGRES_PASSWORD}`), actualizar `.env.example`.

### H-15 — README

Corregir el comando, la estructura de carpetas, los conteos de tests y agregar secciones de: análisis estático (cómo correr `sonar`), ruta del reporte JaCoCo, y los nuevos endpoints de F1–F3.

## 4. Definición de "hecho"

- Los 15 hallazgos resueltos, cada uno en un commit con el ID en el mensaje (`fix(H-06): ...`).
- `./gradlew clean build` pasa; los tests existentes siguen pasando (adaptados donde el spec lo indica).
- En el informe: tabla "issues antes / después de la remediación" por tipo (bugs, vulnerabilidades, code smells, hotspots).
