# Task Manager API

API REST para gestionar tareas con Spring Boot 3.3.0, Java 17 y soporte para H2 en desarrollo y PostgreSQL en producción.

## Descripción del proyecto

Esta aplicación expone CRUD para tareas con validación, manejo de errores centralizado, perfiles de ejecución y pruebas automatizadas.

Se diseñó para practicar:
- Arquitectura en capas
- Spring Data JPA
- DTOs, validaciones y mapeo
- Excepciones y manejo global
- Testing con JUnit 5 + Mockito
- Pruebas de rendimiento
- Swagger/OpenAPI

## Stack tecnológico

- Java 17
- Spring Boot 3.3.0
- Spring Web
- Spring Data JPA
- Validation
- Flyway
- PostgreSQL
- H2 Database
- Springdoc OpenAPI UI
- JUnit 5
- Mockito
- Gradle

## Estructura del proyecto

```text
demo/
├── src/
│   ├── main/
│   │   ├── java/com/example/taskmanager/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   │   └── impl/
│   │   │   └── TaskManagerApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   └── test/
│       └── java/com/example/taskmanager/
│           ├── controller/
│           ├── mapper/
│           └── service/
├── build.gradle
├── gradlew
├── settings.gradle
├── Dockerfile
├── docker-compose.yml
├── README.md
└── .env.example
```

## Modelo de dominio

La entidad principal es `Task` con estos campos:

- id
- title
- description
- status
- priority
- assignedTo
- createdAt
- updatedAt

Estados soportados:
- PENDING
- IN_PROGRESS
- DONE
- CANCELLED

Prioridades soportadas:
- LOW
- MEDIUM
- HIGH

Reglas de negocio aplicadas:
- El título es obligatorio.
- No se puede cambiar el estado de una tarea cancelada.
- La fecha de actualización se actualiza automáticamente en cada modificación.

## Endpoints

Base URL:

```text
http://localhost:8080/api/tasks
```

### Crear tarea

```http
POST /api/tasks
Content-Type: application/json
```

Ejemplo:

```json
{
  "title": "Revisar pull request",
  "description": "Comprobar cambios antes del merge",
  "priority": "HIGH",
  "assignedTo": "Juan"
}
```

### Listar tareas

```http
GET /api/tasks
```

Con filtros opcionales:

```http
GET /api/tasks?status=PENDING
GET /api/tasks?priority=HIGH
GET /api/tasks?status=PENDING&priority=HIGH
```

### Obtener por id

```http
GET /api/tasks/{id}
```

### Actualizar completa

```http
PUT /api/tasks/{id}
```

### Actualizar estado

```http
PATCH /api/tasks/{id}/status
```

Ejemplo:

```json
{
  "status": "IN_PROGRESS"
}
```

### Eliminar tarea

```http
DELETE /api/tasks/{id}
```

## Ejecución del proyecto

### Requisitos

- Java 17
- Gradle
- Docker (opcional)

### Ejecutar en desarrollo (H2)

```bash
cd demo
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew bootRun --args='--spring.profiles.active=dev'
```

La app queda disponible en:

```text
http://localhost:8080
```

También puedes consultar H2 en:

```text
http://localhost:8080/h2-console
```

### Ejecutar en producción (PostgreSQL)

1. Levantar PostgreSQL con Docker Compose:

```bash
cd demo
docker-compose up -d
```

2. Ejecutar la aplicación con perfil prod:

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## Swagger / OpenAPI

Cuando la app está levantada, la documentación interactiva está en:

```text
http://localhost:8080/swagger-ui.html
```

La especificación OpenAPI queda en:

```text
http://localhost:8080/v3/api-docs
```

## Ejecutar tests

### Todos los tests

```bash
./gradlew test
```

### Solo unit tests

```bash
./gradlew test --tests "*TaskServiceTest" --tests "*TaskMapperTest"
```

### Tests de controlador

```bash
./gradlew test --tests "*TaskController*"
```

## Cómo funcionan los tests

### 1. Unit tests

Los tests unitarios se centran en piezas pequeñas del sistema sin levantar el contexto completo de Spring.

Ejemplo real en [demo/src/test/java/com/example/taskmanager/service/TaskServiceTest.java](demo/src/test/java/com/example/taskmanager/service/TaskServiceTest.java):

- Se mockea el repositorio con Mockito.
- Se prepara un objeto de prueba (`Task` o `CreateTaskRequest`).
- Se invoca el método del servicio.
- Se validan resultados con `assertEquals`, `assertNotNull`, `assertThrows`, etc.

Esto permite comprobar lógica de negocio como:
- creación correcta de una tarea
- búsqueda por id
- actualización de estado
- error cuando la tarea no existe
- excepción si una tarea cancelada intenta cambiar de estado

### 2. Tests de performance

Los tests de rendimiento se enfocan en comprobar que una operación no tarda demasiado.

Ejemplo en [demo/src/test/java/com/example/taskmanager/controller/TaskControllerPerformanceTest.java](demo/src/test/java/com/example/taskmanager/controller/TaskControllerPerformanceTest.java):

- se mide el tiempo antes y después de ejecutar el método
- se ejecuta la operación sobre el controlador mockeado
- se valida que el tiempo esté por debajo de un umbral razonable

Se usan marcas temporales con `System.currentTimeMillis()` y luego una aserción tipo:

```java
assertTrue(executionTime < 5000, "Create task took " + executionTime + "ms");
```

Esto sirve para detectar regresiones de rendimiento sin convertir los tests en pruebas frágiles.

### 3. Patrón AAA

El patrón AAA significa:
- Arrange: preparar datos y mocks
- Act: ejecutar la acción bajo prueba
- Assert: comprobar la salida esperada

Se usa de forma consistente en la suite. Por ejemplo:

```java
@BeforeEach
void setUp() {
    // Arrange
    createRequest = CreateTaskRequest.builder()
            .title("Test Task")
            .description("Test Description")
            .priority(TaskPriority.HIGH)
            .assignedTo("John Doe")
            .build();
}

@Test
void shouldCreateTaskSuccessfully() {
    // Arrange
    when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

    // Act
    TaskResponse response = taskService.createTask(createRequest);

    // Assert
    assertNotNull(response);
    assertEquals(savedTask.getId(), response.getId());
    assertEquals(createRequest.getTitle(), response.getTitle());
}
```

Este patrón ayuda a que los tests sean más legibles, más mantenibles y más fáciles de depurar cuando falla uno.

## Base de datos

### Perfil dev

- H2 en memoria
- Se crea el esquema de forma automática
- Útil para desarrollar y probar sin levantar PostgreSQL

### Perfil prod

- PostgreSQL
- Se usa Flyway para migraciones

## Docker

Construir imagen:

```bash
docker build -t task-manager-api:1.0.0 .
```

Levantar con docker-compose:

```bash
docker-compose up --build
```

## Observaciones finales

Este proyecto sirve como ejemplo claro de una API REST con capas bien separadas, validación, manejo de errores y una batería de tests que cubre lógica, integración y rendimiento.

Si quieres avanzar el siguiente nivel, el siguiente paso natural es añadir:
- más tests de casos límite
- cobertura de Jacoco
- autenticación JWT
- paginación de resultados
- CI con GitHub Actions


### Run with Docker:

```bash
# With H2 (dev profile)
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev task-manager-api:1.0.0

# With PostgreSQL (prod profile)
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/taskmanager \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  task-manager-api:1.0.0
```

## Example API Usage

### Create a Task

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Complete project documentation",
    "description": "Write comprehensive API documentation",
    "priority": "HIGH",
    "assignedTo": "John Doe"
  }'
```

### Get All Tasks

```bash
curl http://localhost:8080/api/tasks
```

### Get Tasks by Status

```bash
curl http://localhost:8080/api/tasks?status=PENDING
```

### Get a Task by ID

```bash
curl http://localhost:8080/api/tasks/1
```

### Update a Task

```bash
curl -X PUT http://localhost:8080/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated title",
    "priority": "MEDIUM"
  }'
```

### Update Task Status

```bash
curl -X PATCH http://localhost:8080/api/tasks/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'
```

### Delete a Task

```bash
curl -X DELETE http://localhost:8080/api/tasks/1
```

## Error Handling

All errors return standardized JSON responses:

```json
{
  "code": 404,
  "message": "Task not found with id: 1",
  "timestamp": "2026-08-17T10:30:45",
  "path": "/api/tasks/1"
}
```

### Common HTTP Status Codes

- `200 OK` - Successful request
- `201 Created` - Resource created successfully
- `204 No Content` - Successful deletion
- `400 Bad Request` - Invalid input or business rule violation
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Unexpected server error

## Testing Patterns

All tests follow the **AAA (Arrange-Act-Assert)** pattern:

```java
@Test
void shouldCreateTaskSuccessfully() {
    // Arrange
    CreateTaskRequest request = CreateTaskRequest.builder()
        .title("New Task")
        .build();
    
    // Act
    TaskResponse response = taskService.createTask(request);
    
    // Assert
    assertNotNull(response);
    assertEquals("New Task", response.getTitle());
}
```

## Performance Benchmarks

Performance tests validate that critical operations complete within acceptable timeframes:

- **Create Task**: < 500ms
- **Get Single Task**: < 200ms
- **Get All Tasks**: < 500ms

These thresholds are tested in `TaskControllerPerformanceTest`

## Troubleshooting

### Gradle Build Issues

```bash
./gradlew clean
./gradlew build
```

### Port Already in Use

```bash
# Change port in application.yml
server:
  port: 8081
```

### H2 Console Not Accessible

Ensure H2 is enabled in `application-dev.yml`:
```yaml
spring:
  h2:
    console:
      enabled: true
```

### PostgreSQL Connection Errors

1. Verify PostgreSQL is running
2. Check credentials in environment variables
3. Ensure database `taskmanager` exists

## Contributing

When contributing code:
1. Follow Google Java Style Guide
2. Write tests following AAA pattern
3. Maintain > 80% test coverage in service layer
4. Document new endpoints in Swagger annotations

## License

Apache License 2.0

## Support

For issues, questions, or suggestions, please refer to the project documentation or open an issue in the repository.
