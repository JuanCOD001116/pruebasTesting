# Task Manager API

API REST para gestionar tareas construida con Spring Boot 3.3.13, Java 17, PostgreSQL y soporte Docker.

## Descripción

Aplicación para la gestión del ciclo de vida de tareas con validación estricta, manejo de errores centralizado, auditoría JPA, perfiles de ejecución y pruebas automatizadas. Diseñada para aplicar:

- Arquitectura en capas limpia (Controller / Service / Repository)
- Spring Data JPA + Flyway (esquema unificado y validado con `ddl-auto: validate`)
- DTOs inmutables, validaciones personalizadas Bean Validation y mapeo
- Excepciones de negocio y manejo global seguro (`GlobalExceptionHandler`)
- Testing exhaustivo con JUnit 5 + Mockito siguiendo el patrón AAA
- Pruebas de rendimiento con límites de tiempo (`assertTimeout`)
- Documentación viva con Swagger / OpenAPI
- Contenedores Docker multi-stage y ejecución sin privilegios de root
- Análisis estático continuo con SonarCloud y cobertura de código con JaCoCo

## Stack tecnológico

| Categoría | Tecnología |
|-----------|------------|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.3.13 |
| Build Tool | Gradle 8.12 |
| ORM | Spring Data JPA / Hibernate 6 |
| Migraciones | Flyway |
| BD producción | PostgreSQL 15 |
| BD desarrollo | H2 Database (modo compatibilidad PostgreSQL) |
| Documentación | Springdoc OpenAPI 2.6.0 |
| Métricas / Salud | Spring Boot Starter Actuator |
| Testing | JUnit 5 + Mockito + MockMvc |
| Calidad de Código | SonarCloud + JaCoCo 0.8.12 |
| Contenedores | Docker (Alpine, usuario sin privilegios) + Docker Compose |

## Estructura del proyecto

```text
pruebasTesting/
├── .github/
│   └── workflows/
│       └── sonarcloud.yml             # Workflow de CI con análisis de SonarCloud
├── .vscode/
│   └── settings.json                  # Ajustes portables del IDE (sin rutas locales)
├── docs/
│   └── analisis-estatico/
│       ├── informe-metricas.md        # Métricas antes/después (complejidad, cobertura)
│       ├── reporte-verificacion.md    # Reporte de verificación final (spec 12)
│       └── evidencias/                # Capturas de SonarCloud y JaCoCo
├── gradle/
│   └── gradle-daemon-jvm.properties   # Fija el daemon de Gradle en JDK 17
├── quality/
│   └── sonar/
│       ├── quality-gate.md            # Definición documentada del Quality Gate
│       └── taskmanager-java-way.xml   # Backup del Quality Profile
├── src/
│   ├── main/
│   │   ├── java/com/example/taskmanager/
│   │   │   ├── TaskManagerApplication.java
│   │   │   ├── config/
│   │   │   │   ├── ClockConfig.java           # Reloj inyectable para lógica temporal determinista
│   │   │   │   ├── JpaAuditingConfig.java     # Auditoría JPA con DateTimeProvider basado en Clock
│   │   │   │   ├── OpenApiConfig.java         # Configuración Swagger / OpenAPI
│   │   │   │   └── UrgencyProperties.java     # Ventanas de urgencia por prioridad (app.urgency)
│   │   │   ├── controller/
│   │   │   │   └── TaskController.java
│   │   │   ├── dto/
│   │   │   │   ├── request/                   # CreateTaskRequest, UpdateTaskRequest,
│   │   │   │   │                              # PatchTaskStatusRequest, TaskSearchCriteria
│   │   │   │   ├── response/                  # TaskResponse, PageResponse, TaskSummaryResponse,
│   │   │   │   │                              # ErrorResponse
│   │   │   │   └── validation/                # @NullOrNotBlank y su validador
│   │   │   ├── exception/                     # Excepciones de negocio + GlobalExceptionHandler
│   │   │   ├── mapper/
│   │   │   │   └── TaskMapper.java
│   │   │   ├── model/                         # Task, TaskStatus, TaskPriority, TaskUrgency
│   │   │   ├── repository/
│   │   │   │   ├── TaskRepository.java        # JpaRepository + JpaSpecificationExecutor
│   │   │   │   ├── projection/                # StatusCount, PriorityCount (F3)
│   │   │   │   └── specification/
│   │   │   │       └── TaskSpecifications.java    # Filtros combinables (F2)
│   │   │   └── service/
│   │   │       ├── TaskService.java
│   │   │       ├── TaskSummaryService.java
│   │   │       ├── impl/                      # TaskServiceImpl, TaskSummaryServiceImpl
│   │   │       ├── urgency/                   # Chain of Responsibility de urgencia (F1):
│   │   │       │                              # UrgencyRule, 4 reglas y TaskUrgencyEvaluator
│   │   │       └── validation/                # TaskStatusTransitionValidator,
│   │   │                                      # TaskDueDateValidator, SortValidator
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           ├── V1__Create_tasks_table.sql
│   │           └── V2__Add_due_date_to_tasks.sql
│   └── test/
│       └── java/com/example/taskmanager/      # 23 clases de prueba, mismo paquete que lo probado
│           ├── controller/                    # MockMvc, unitarios y de rendimiento
│           ├── dto/                           # TaskSearchCriteria y @NullOrNotBlank
│           ├── exception/                     # Handler: unitario e integración
│           ├── mapper/
│           ├── model/
│           ├── repository/specification/      # @DataJpaTest de las Specifications
│           └── service/                       # Servicios, reglas de urgencia y validadores
├── build.gradle
├── settings.gradle
├── lombok.config                       # Exclusión de código generado por Lombok de JaCoCo
├── Dockerfile                         # Multi-stage build con usuario app
├── docker-compose.yml
├── .env.example
└── README.md
```

## Modelo de dominio

Entidad principal: `Task`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | Long | Identificador auto-generado |
| title | String | Título (1-255 caracteres, obligatorio) |
| description | String | Descripción (hasta 1000 caracteres, opcional) |
| status | TaskStatus | `PENDING`, `IN_PROGRESS`, `DONE`, `CANCELLED` |
| priority | TaskPriority | `LOW`, `MEDIUM`, `HIGH` (default: `MEDIUM`) |
| assignedTo | String | Usuario asignado (opcional, obligatorio para pasar a `IN_PROGRESS`) |
| dueDate | LocalDate | Fecha límite (opcional, no puede ser anterior a hoy al crear o cambiarla) |
| createdAt | LocalDateTime | Fecha y hora de creación (gestionada por auditoría JPA) |
| updatedAt | LocalDateTime | Fecha y hora de última modificación (gestionada por auditoría JPA) |

## Endpoints de la API

| Método | Endpoint | Descripción | Códigos HTTP |
|--------|----------|-------------|--------------|
| POST | `/api/tasks` | Crear una nueva tarea | 201, 400 |
| GET | `/api/tasks` | Listar tareas con filtros opcionales (F2) | 200, 400 |
| GET | `/api/tasks/search` | Búsqueda paginada y ordenable (F2) | 200, 400 |
| GET | `/api/tasks/summary` | Resumen estadístico (F3) | 200 |
| GET | `/api/tasks/{id}` | Obtener una tarea por ID | 200, 404 |
| PUT | `/api/tasks/{id}` | Actualizar datos y/o estado de la tarea | 200, 400, 404 |
| PATCH | `/api/tasks/{id}/status` | Cambiar únicamente el estado de la tarea | 200, 400, 404 |
| DELETE | `/api/tasks/{id}` | Eliminar una tarea | 204, 404 |
| GET | `/actuator/health` | Estado de salud de la aplicación | 200 |

Rutas inexistentes, métodos no soportados y `Content-Type` no soportados responden 404, 405 y 415
con el mismo cuerpo de error estandarizado (`code`, `message`, `timestamp`, `path`).

### Funcionalidades F1 – F3

- **F1 — Fecha límite y urgencia:**
  - `dueDate` en `CreateTaskRequest`, `UpdateTaskRequest` y `TaskResponse`.
  - Atributo calculado `urgency` (`NONE`, `ON_TRACK`, `AT_RISK`, `OVERDUE`) mediante una cadena de
    reglas (`TerminalStatusRule` → `NoDueDateRule` → `OverdueRule` → `DueSoonRule`). Ventanas de
    alerta configurables en `app.urgency.windows` (`HIGH=3`, `MEDIUM=2`, `LOW=1` días).
- **F2 — Búsqueda con Specification Pattern:**
  - `GET /api/tasks?status=...&priority=...&assignedTo=...&text=...&dueFrom=...&dueTo=...&overdue=...`
  - `GET /api/tasks/search?page=0&size=20&sort=createdAt,desc` → `PageResponse<TaskResponse>`.
    Tamaño máximo 100; `sort` solo admite `id, title, status, priority, dueDate, createdAt, updatedAt`.
- **F3 — Resumen estadístico:**
  - `GET /api/tasks/summary` → `TaskSummaryResponse` con total, desglose por estado/prioridad,
    tareas vencidas y tasa de completitud (2 decimales).

## Ejecución del proyecto

### Requisitos

- Java 17 (JDK). Gradle usa un JDK 17 instalado para su daemon (`gradle/gradle-daemon-jvm.properties`) aunque `JAVA_HOME` apunte a una versión más nueva.
- Docker y Docker Compose (para ejecución en contenedores)

> **VS Code / Antigravity:** `.vscode/settings.json` solo contiene ajustes portables. Si tu IDE
> arranca el Java Language Server con un JDK muy nuevo (p. ej. 27) y muestra falsos positivos con
> Lombok, fija el JDK del language server en tu `settings.json` **de usuario** (no en el del
> proyecto), con la ruta de tu máquina:
>
> ```json
> "java.jdt.ls.java.home": "C:\\Program Files\\Java\\jdk-21"
> ```

### Desarrollo local (H2 en memoria)

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

- API disponible en: `http://localhost:8080`
- Consola H2: `http://localhost:8080/h2-console`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Healthcheck: `http://localhost:8080/actuator/health`

### Producción con Docker Compose

1. Configurar variables de entorno desde la plantilla:
```bash
cp .env.example .env
# Editar contraseñas seguras en .env
```

2. Levantar los servicios:
```bash
docker compose up --build
```

Para detener los contenedores:
```bash
docker compose down
```

Para eliminar el volumen de datos persistente:
```bash
docker compose down -v
```

## Pruebas automatizadas

El proyecto cuenta con **164 pruebas en 23 clases** (unitarias, de integración con MockMvc/H2,
`@DataJpaTest` y de rendimiento), todas con el patrón **AAA (Arrange-Act-Assert)** y
`@DisplayName` en estilo Dado/cuando/entonces. Son independientes entre sí: pasan ejecutadas por
clase y en orden aleatorio.

```bash
# Ejecutar todos los tests y generar reporte de cobertura
./gradlew test

# Ejecutar tests unitarios específicos
./gradlew test --tests "*TaskServiceTest"
./gradlew test --tests "*TaskMapperTest"
./gradlew test --tests "*TaskStatusTransitionValidatorTest"

# Ejecutar pruebas de integración de controladores
./gradlew test --tests "*TaskControllerTest"
./gradlew test --tests "*TaskStatusTransitionControllerIntegrationTest"

# Ejecutar pruebas de rendimiento
./gradlew test --tests "*TaskControllerPerformanceTest"
```

## Análisis estático y cobertura de código

### Reporte de cobertura JaCoCo

La tarea `test` finaliza automáticamente con `jacocoTestReport`. Para visualizar el reporte HTML:

```text
build/reports/jacoco/test/html/index.html
```

### Ejecutar análisis de SonarCloud

Para lanzar el análisis localmente hacia SonarCloud:

```bash
# En Bash / macOS / Linux:
export SONAR_TOKEN="tu-token-sonarcloud"
./gradlew sonar

# En PowerShell (Windows):
$env:SONAR_TOKEN="tu-token-sonarcloud"
./gradlew sonar
```

El análisis evalúa:
- Cobertura de código en código nuevo (umbral mínimo del Quality Gate: ≥ 80%).
- Complejidad ciclomática y cognitiva (máximo 10 por método).
- 0 bugs, 0 vulnerabilidades, 0 code smells de severidad alta/bloqueante.
- 100% de Security Hotspots revisados.

## Licencia

Apache License 2.0
