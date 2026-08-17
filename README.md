# Task Manager API

API REST para gestionar tareas con Spring Boot 3.3.0, Java 17, PostgreSQL y soporte Docker.

## Descripcion

Aplicacion CRUD para tareas con validacion, manejo de errores centralizado, perfiles de ejecucion y pruebas automatizadas. Disenada para practicar:

- Arquitectura en capas (Controller / Service / Repository)
- Spring Data JPA + Flyway
- DTOs, validaciones y mapeo
- Excepciones y manejo global
- Testing con JUnit 5 + Mockito
- Pruebas de rendimiento
- Swagger/OpenAPI
- Docker multi-stage build

## Stack tecnologico

| Categoria | Tecnologia |
|-----------|------------|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.3.0 |
| Build | Gradle 8.12 |
| ORM | Spring Data JPA / Hibernate |
| Migraciones | Flyway |
| BD produccion | PostgreSQL 15 |
| BD desarrollo | H2 Database (en memoria) |
| Documentacion | Springdoc OpenAPI 2.2.0 |
| Testing | JUnit 5 + Mockito |
| Contenedor | Docker multi-stage |

## Estructura del proyecto

```text
pruebasTesting/
├── src/
│   ├── main/
│   │   ├── java/com/example/taskmanager/
│   │   │   ├── TaskManagerApplication.java
│   │   │   ├── config/
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── model/
│   │   │   │   ├── Task.java
│   │   │   │   ├── TaskStatus.java
│   │   │   │   └── TaskPriority.java
│   │   │   ├── repository/
│   │   │   │   └── TaskRepository.java
│   │   │   ├── service/
│   │   │   │   ├── TaskService.java
│   │   │   │   └── impl/
│   │   │   │       └── TaskServiceImpl.java
│   │   │   ├── controller/
│   │   │   │   └── TaskController.java
│   │   │   ├── mapper/
│   │   │   │   └── TaskMapper.java
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   │   ├── CreateTaskRequest.java
│   │   │   │   │   ├── UpdateTaskRequest.java
│   │   │   │   │   └── PatchTaskStatusRequest.java
│   │   │   │   └── response/
│   │   │   │       ├── TaskResponse.java
│   │   │   │       └── ErrorResponse.java
│   │   │   └── exception/
│   │   │       ├── TaskNotFoundException.java
│   │   │       ├── InvalidTaskStateException.java
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           └── V1__Create_tasks_table.sql
│   └── test/
│       └── java/com/example/taskmanager/
│           ├── service/
│           │   └── TaskServiceTest.java
│           ├── mapper/
│           │   └── TaskMapperTest.java
│           └── controller/
│               ├── TaskControllerIntegrationTest.java
│               └── TaskControllerPerformanceTest.java
├── build.gradle
├── settings.gradle
├── gradlew / gradlew.bat
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── .env.example
└── README.md
```

## Modelo de dominio

Entidad principal: `Task`

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| id | Long | Identificador auto-generado |
| title | String | Obligatorio, max 255 caracteres |
| description | String | Opcional, max 1000 caracteres |
| status | TaskStatus | Estado actual de la tarea |
| priority | TaskPriority | Prioridad de la tarea |
| assignedTo | String | Opcional, nombre del responsable |
| createdAt | LocalDateTime | Se establece automaticamente al crear |
| updatedAt | LocalDateTime | Se actualiza automaticamente en cada modificacion |

**Estados soportados:** `PENDING`, `IN_PROGRESS`, `DONE`, `CANCELLED`

**Prioridades soportadas:** `LOW`, `MEDIUM`, `HIGH`

**Reglas de negocio:**
- El titulo es obligatorio.
- No se puede cambiar el estado de una tarea `CANCELLED` a otro estado.
- La fecha de actualizacion se actualiza automaticamente via `@PreUpdate`.

## Endpoints

Base URL: `http://localhost:8080/api/tasks`

### Crear tarea

```http
POST /api/tasks
Content-Type: application/json
```

```json
{
  "title": "Revisar pull request",
  "description": "Comprobar cambios antes del merge",
  "priority": "HIGH",
  "assignedTo": "Juan"
}
```

Respuesta: `201 Created`

### Listar tareas

```http
GET /api/tasks
GET /api/tasks?status=PENDING
GET /api/tasks?priority=HIGH
GET /api/tasks?status=PENDING&priority=HIGH
```

### Obtener tarea por ID

```http
GET /api/tasks/{id}
```

### Actualizar tarea (parcial)

```http
PUT /api/tasks/{id}
Content-Type: application/json
```

```json
{
  "title": "Nuevo titulo",
  "priority": "LOW"
}
```

Solo los campos enviados se actualizan; los demas permanecen sin cambios.

### Cambiar estado

```http
PATCH /api/tasks/{id}/status
Content-Type: application/json
```

```json
{
  "status": "IN_PROGRESS"
}
```

### Eliminar tarea

```http
DELETE /api/tasks/{id}
```

Respuesta: `204 No Content`

## Formato de errores

Todos los errores retornan una respuesta JSON estandarizada:

```json
{
  "code": 404,
  "message": "Task not found with id: 1",
  "timestamp": "2026-08-17T10:30:45",
  "path": "/api/tasks/1"
}
```

| HTTP Status | Significado |
|-------------|-------------|
| 200 | Exito |
| 201 | Recurso creado |
| 204 | Eliminacion exitosa |
| 400 | Input invalido o regla de negocio violada |
| 404 | Recurso no encontrado |
| 500 | Error interno del servidor |

## Ejecucion

### Requisitos

- Java 17
- Gradle (incluido via wrapper)
- Docker + Docker Compose (opcional)

### Desarrollo local (H2 en memoria)

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

La app queda disponible en `http://localhost:8080`.

Consola H2 disponible en `http://localhost:8080/h2-console`.

### Produccion con Docker Compose

Levanta PostgreSQL y la app juntos:

```bash
docker compose up -build
```

La app queda en `http://localhost:8080` y PostgreSQL en el puerto `5432`.

Para detener:

```bash
docker compose down
```

Para eliminar la base de datos:

```bash
docker compose down -v
```

### Solo la imagen Docker

```bash
docker build -t task-manager-api:1.0.0 .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/taskmanager \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  task-manager-api:1.0.0
```

### Variables de entorno

Ver [.env.example](.env.example) para las variables disponibles.

## Swagger / OpenAPI

Con la app levantada:

- UI: `http://localhost:8080/swagger-ui.html`
- Spec JSON: `http://localhost:8080/v3/api-docs`

## Ejecutar tests

```bash
# Todos los tests
./gradlew test

# Solo unit tests (servicio + mapper)
./gradlew test --tests "*TaskServiceTest" --tests "*TaskMapperTest"

# Tests de controlador
./gradlew test --tests "*TaskControllerIntegrationTest"

# Tests de rendimiento
./gradlew test --tests "*TaskControllerPerformanceTest"
```

## Como funcionan los tests

Se siguen dos tipos de pruebas, ambas bajo el patron **AAA (Arrange-Act-Assert)**.

### Unit tests

Se centran en piezas pequenas del sistema sin levantar el contexto de Spring.

**`TaskServiceTest`** (10 tests) - Mockea el repositorio con Mockito y valida:
- Creacion correcta de tareas
- Busqueda por ID
- Actualizacion de estado
- Error cuando la tarea no existe
- Excepcion si una tarea cancelada intenta cambiar de estado
- Eliminacion exitosa y manejo de ID inexistente

**`TaskMapperTest`** (4 tests) - Valida la conversion DTO <-> Entity:
- CreateTaskRequest a entity (status default PENDING)
- UpdateTaskRequest aplica solo campos no nulos
- Task entity a TaskResponse

### Tests de integracion del controlador

**`TaskControllerIntegrationTest`** (4 tests) - Mockea el servicio y valida el comportamiento HTTP:
- POST retorna 201 con body correcto
- GET retorna 200 con lista
- GET con ID inexistente propagna excepcion
- DELETE retorna 204

### Tests de rendimiento

**`TaskControllerPerformanceTest`** (2 tests) - Mide tiempo de ejecucion de operaciones criticas y valida que no excedan umbrales razonables (< 5000ms).

## Base de datos

### Perfil dev

- H2 en memoria
- Esquema creado automaticamente con `ddl-auto: create-drop`
- Consola H2 habilitada

### Perfil prod

- PostgreSQL
- Migraciones via Flyway (`db/migration/V1__Create_tasks_table.sql`)
- Esquema validado con `ddl-auto: validate`

## Arquitectura

El proyecto sigue un patron de arquitectura en capas:

```
Controller (recibe HTTP)
    ↓
Service (logica de negocio)
    ↓
Repository (acceso a datos via JPA)
    ↓
Database (PostgreSQL / H2)
```

**Patrones de diseno aplicados:**
- **DTO** - Separacion de modelos de entrada/salida de la entidad JPA
- **Repository** - Abstraccion del acceso a datos
- **Dependency Injection** - Inyeccion via constructor con Lombok `@RequiredArgsConstructor`
- **Global Exception Handler** - Manejo centralizado de errores via `@RestControllerAdvice`
- **Mapper** - Conversion entre DTOs y entidades
- **Transactional** - Transacciones gestionadas en la capa de servicio

## License

Apache License 2.0
