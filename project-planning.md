# Planificación del Proyecto: Task Manager API

> Este documento define el alcance y las especificaciones técnicas del proyecto para que GitHub Copilot lo use como guía de codificación.

## 1. Descripción general

- **Nombre del proyecto:** Task Manager API
- **Propósito:** API REST simple y escalable para la gestión de tareas (CRUD + reglas de negocio básicas), pensada como base de ejemplo para practicar unit testing, performance testing y el patrón AAA (Arrange-Act-Assert).
- **groupId / artifactId:** `com.example.taskmanager`

## 2. Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Build tool | Gradle |
| Framework | Spring Boot |
| Base de datos principal | PostgreSQL |
| Base de datos fallback | H2 (embebida, en memoria) |
| Documentación de API | Swagger / OpenAPI (springdoc-openapi) |
| Autenticación | Ninguna (API abierta) |
| Testing | JUnit 5 |
| Contenerización | Docker (Dockerfile) |
| Reducción de boilerplate | Lombok |
| Logging | SLF4J |

## 3. Arquitectura

**Arquitectura por capas (Layered Architecture):**

```
Controller  →  Service  →  Repository  →  Database
                 ↓
              Domain / Model
                 ↓
             DTO / Mapper
```

- **Controller:** expone los endpoints REST, valida entrada, delega en el service.
- **Service:** contiene la lógica de negocio.
- **Repository:** acceso a datos (Spring Data JPA).
- **Model/Domain:** entidades JPA.
- **DTO:** objetos de transferencia, separados de las entidades (no exponer entidades directamente en la API).
- **Mapper:** conversión entre entidad y DTO.
- **Exception:** excepciones custom + manejo centralizado.
- **Config:** configuración de Spring (Swagger, perfiles de BD, etc.).

### Estructura de paquetes propuesta

```
com.example.taskmanager
├── controller
├── service
│   └── impl
├── repository
├── model
├── dto
│   ├── request
│   └── response
├── mapper
├── exception
├── config
└── TaskManagerApplication.java
```

## 4. Dominio del negocio: Gestión de Tareas

### Entidad principal: `Task`

| Campo | Tipo | Notas |
|---|---|---|
| id | Long | autogenerado |
| title | String | obligatorio |
| description | String | opcional |
| status | Enum (`PENDING`, `IN_PROGRESS`, `DONE`, `CANCELLED`) | obligatorio, default `PENDING` |
| priority | Enum (`LOW`, `MEDIUM`, `HIGH`) | obligatorio, default `MEDIUM` |
| assignedTo | String | opcional (nombre/usuario asignado, sin entidad de usuario separada) |
| createdAt | LocalDateTime | autogenerado |
| updatedAt | LocalDateTime | autogenerado |

### Endpoints (CRUD + filtros básicos)

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/tasks` | Crear tarea |
| GET | `/api/tasks` | Listar tareas (con filtros opcionales por `status` y `priority`) |
| GET | `/api/tasks/{id}` | Obtener tarea por ID |
| PUT | `/api/tasks/{id}` | Actualizar tarea |
| PATCH | `/api/tasks/{id}/status` | Cambiar estado de una tarea |
| DELETE | `/api/tasks/{id}` | Eliminar tarea |

### Reglas de negocio mínimas

- No se puede crear una tarea sin `title`.
- No se puede cambiar el estado de una tarea `CANCELLED` a otro estado (regla simple para justificar lógica en el service).
- Al actualizar, `updatedAt` se refresca automáticamente.

## 5. Base de datos

- **Principal:** PostgreSQL, configurado vía variables de entorno (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).
- **Fallback:** H2 embebido en memoria, activo por defecto si no hay variables de PostgreSQL configuradas.
- **Perfiles de Spring:**
  - `application.yml` (config común)
  - `application-dev.yml` → H2
  - `application-prod.yml` → PostgreSQL
- Migraciones: usar **Flyway** para versionar el esquema (recomendado, aplica a ambos perfiles).

## 6. Testing

### Requisitos generales

- Framework: **JUnit 5**
- Todos los tests deben seguir el **patrón AAA** (Arrange, Act, Assert), separado con comentarios o bloques claros:

```java
@Test
void shouldCreateTaskSuccessfully() {
    // Arrange
    ...

    // Act
    ...

    // Assert
    ...
}
```

### Tipos de test a implementar

| Tipo | Alcance | Herramienta |
|---|---|---|
| Unitarios | Service, Mapper (lógica de negocio aislada, con Mockito para dependencias) | JUnit 5 + Mockito |
| Integración | Repository (con H2), Controller (`@SpringBootTest` + `MockMvc`) | JUnit 5 + Spring Test |
| Performance | Tiempos de respuesta de operaciones clave (ej. creación y listado de tareas) | JUnit 5 puro con `assertTimeout` / `assertTimeoutPreemptively` |

- Los performance tests deben validar que operaciones críticas no excedan un umbral de tiempo razonable (a definir por endpoint, ej. < 200ms en condiciones locales/mock).
- Cobertura mínima esperada: **80%** en capa de servicio.

## 7. Documentación de API

- Integrar **springdoc-openapi** para generar Swagger UI automáticamente (`/swagger-ui.html`).
- Documentar cada endpoint con anotaciones (`@Operation`, `@ApiResponse`, etc.).

## 8. Manejo de errores

- Excepciones custom: `TaskNotFoundException`, `InvalidTaskStateException`, etc.
- Manejo centralizado con `@RestControllerAdvice`.
- Respuestas de error estandarizadas (código, mensaje, timestamp).

## 9. Validación

- Bean Validation (`jakarta.validation`) en los DTOs de entrada (`@NotBlank`, `@NotNull`, etc.).

## 10. Patrones de diseño y buenas prácticas

- **Repository Pattern** (Spring Data JPA)
- **DTO Pattern** (separación entidad/API)
- **Builder Pattern** (construcción de objetos, vía Lombok `@Builder`)
- **Dependency Injection** (constructor injection, no `@Autowired` en campos)
- **Strategy Pattern** si se requiere lógica condicional extensible (ej. validaciones de transición de estado)
- Principios SOLID y Clean Code en general
- Estilo de código: Google Java Style Guide

## 11. Contenerización

- **Dockerfile** multi-stage:
  1. Stage de build con Gradle (compilar el jar)
  2. Stage final liviano (ej. `eclipse-temurin:17-jre-alpine`) copiando solo el jar generado
- Exponer el puerto configurado de Spring Boot (por defecto `8080`)
- Variables de entorno para configuración de base de datos inyectables en runtime

## 12. Entregables esperados de Copilot

1. Proyecto Gradle inicial con estructura de paquetes definida.
2. Entidad `Task`, DTOs, mapper, repository, service e implementación, controller.
3. Configuración de perfiles `dev` (H2) y `prod` (PostgreSQL) con Flyway.
4. Integración de Swagger/OpenAPI.
5. Manejo global de excepciones.
6. Tests unitarios, de integración y de performance siguiendo AAA.
7. Dockerfile funcional.
8. README con instrucciones de build, ejecución y pruebas.
