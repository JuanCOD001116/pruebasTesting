# Guía General — Implementación de Pruebas de Transición de Estado y Cobertura de Camino

> Este documento es la guía maestra para implementar dos técnicas de prueba de software en el proyecto **Task Manager API**: pruebas de transición de estado (caja negra) y pruebas de cobertura de camino (caja blanca), aplicadas sobre la máquina de estados del campo `status` de la entidad `Task`.

## 1. Contexto

Este trabajo complementa — **no reemplaza** — el documento `project-planning.md`, que define el stack (Java 17, Spring Boot, Gradle, PostgreSQL/H2, JUnit 5, Mockito, Lombok), la arquitectura por capas y las convenciones de código ya establecidas (Google Java Style Guide, constructor injection, patrón AAA, etc.). Antes de codificar, revisa ese documento para respetar la estructura de paquetes y las convenciones ya definidas.

El proyecto **ya está implementado** (entidad `Task`, DTOs, mapper, repository, service, controller). Este trabajo amplía la lógica de negocio existente y agrega dos suites de pruebas nuevas sobre esa ampliación.

## 2. Documentos de este paquete de trabajo

| Archivo | Contenido |
|---|---|
| `00-guia-general-para-ia.md` | Este documento: alcance, estándares, checklist |
| `01-reglas-negocio-transicion-estado.md` | Ampliación de la máquina de estados de `Task` y el componente `TaskStatusTransitionValidator` a implementar |
| `02-pruebas-transicion-estado.md` | Especificación de la suite de pruebas de caja negra (transición de estado) |
| `03-pruebas-cobertura-camino.md` | Especificación de la suite de pruebas de caja blanca (cobertura de camino / complejidad ciclomática) |

**Orden de implementación:** primero la lógica de negocio del archivo 01, luego las pruebas del 02, y finalmente las del 03 (dependen del código final del 01).

## 3. Alcance

**Sí incluye:**
- Ampliar la validación de transición de estado de `Task` (archivo 01).
- Aplicar esa validación tanto en `PUT /api/tasks/{id}` como en `PATCH /api/tasks/{id}/status`.
- Suite de pruebas unitarias de transición de estado (archivo 02).
- Suite de pruebas de cobertura de camino sobre el validador (archivo 03).
- Configuración de JaCoCo para medir cobertura de rama/camino.

**No incluye (fuera de alcance):**
- Cambios al stack tecnológico, autenticación, u otros endpoints no relacionados con `status`.
- Pruebas de performance (ya cubiertas en otra parte del plan del proyecto).
- Pruebas de integración generales no relacionadas con esta lógica.
- Nuevos campos en la entidad `Task`.

## 4. Estándares obligatorios

- **Patrón AAA** en todos los tests, con comentarios `// Arrange`, `// Act`, `// Assert` explícitos.
- **JUnit 5** + **Mockito** donde se necesiten dependencias simuladas.
- Nombres de método descriptivos en inglés (`shouldRejectTransitionFromCancelledToPending`) + `@DisplayName` en español, estilo Given/When/Then, para legibilidad de negocio.
- Ubicación de tests: `src/test/java/com/example/taskmanager/...` reflejando el paquete de la clase bajo prueba.
- Principios **F.I.R.S.T** (Fast, Independent, Repeatable, Self-validating, Thorough): cada test debe ser independiente y no depender del orden de ejecución ni de estado compartido.
- Evitar la "paradoja del pesticida": varía los datos de entrada entre tests (no repitas siempre el mismo `assignedTo`, `title`, etc.).
- Ninguna regla de negocio nueva sin trazabilidad explícita hacia una prueba (archivo 01 → archivo 02/03).

## 5. Checklist de entregables

- [ ] `TaskStatusTransitionValidator` (o nombre equivalente) implementado según el archivo 01.
- [ ] Integración del validador en el service (PUT y PATCH).
- [ ] `InvalidTaskStateException` lanzada con mensaje claro indicando estado origen/destino.
- [ ] Suite `TaskStatusTransitionValidatorTest` (pruebas de transición de estado, archivo 02).
- [ ] Al menos dos pruebas de integración a nivel de controller (archivo 02).
- [ ] Complejidad ciclomática del/los método(s) objetivo documentada (archivo 03).
- [ ] Suite de pruebas de cobertura de camino cubriendo todos los caminos independientes (archivo 03).
- [ ] JaCoCo configurado y reporte generado sin fallar el umbral definido.
- [ ] Todos los tests existentes del proyecto siguen pasando (`./gradlew test`).

## 6. Definición de "hecho"

Este trabajo se considera completo cuando:

1. `./gradlew test` pasa sin fallos.
2. `./gradlew jacocoTestCoverageVerification` pasa sin fallos según los umbrales del archivo 03.
3. La matriz de transiciones del archivo 01 está 100% cubierta por la suite del archivo 02 (una fila = al menos un test).
4. Todos los caminos independientes identificados en el archivo 03 tienen un test que los ejerce.
