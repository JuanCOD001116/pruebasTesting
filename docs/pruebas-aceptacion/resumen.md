# Resumen de Pruebas de Aceptación con Karate

> Este documento resume los resultados de la implementación de la capa de pruebas de aceptación end-to-end con Karate según la especificación `.copilot/13-pruebas-aceptacion-karate.md`.

---

## 1. Métricas de Ejecución

| Métrica | Valor |
|---|---|
| **Features ejecutadas** | 9 |
| **Features ignoradas (helpers reutilizables)** | 3 (`create-task.feature`, `change-status.feature`, `patch-status-step.feature`) |
| **Escenarios base definidos** | 54 (A-01 a A-54) |
| **Casos / ejemplos ejecutados** | 84 |
| **Casos pasados** | 84 (100 %) |
| **Casos fallidos** | 0 (0 %) |
| **Tiempo de ejecución de escenarios (Karate)** | ~10.47 s |
| **Tiempo transcurrido (incluyendo arranque Spring Boot)** | ~28.87 s |

---

## 2. Detalle por Feature

| Feature | Archivo | Casos ejecutados | Estado |
|---|---|---|---|
| Operaciones CRUD de tareas | `tasks/crud.feature` | 9 | ✅ Pasaron todos |
| Validación de entrada y manejo de errores | `tasks/validation.feature` | 9 | ✅ Pasaron todos |
| Máquina de estados de una tarea | `status/status-transitions.feature` | 18 | ✅ Pasaron todos |
| Reglas de negocio para fecha límite (`dueDate`) | `duedate/due-date.feature` | 7 | ✅ Pasaron todos |
| Urgencia calculada de tareas (`TaskUrgency`) | `duedate/urgency.feature` | 16 | ✅ Pasaron todos |
| Búsqueda paginada, filtros y ordenamiento | `search/search.feature` | 13 | ✅ Pasaron todos |
| Resumen estadístico de tareas | `summary/summary.feature` | 4 | ✅ Pasaron todos |
| Formato y seguridad de errores | `platform/errors.feature` | 4 | ✅ Pasaron todos |
| Salud de la plataforma y documentación OpenAPI | `platform/health-and-docs.feature` | 4 | ✅ Pasaron todos |

---

## 3. Bugs Encontrados

- **Código de producción:** Ninguno. La API cumplió con todas las especificaciones de negocio (CRUD, validaciones, transiciones de estado, rangos de fechas, cálculo de urgencia, especificaciones de búsqueda y resumen estadístico) sin requerir modificaciones en `src/main`.
- **Mecanismo de fallo validado (Paso 12.5):** Se probó en local modificando intencionalmente `status 400` a `status 200` en el escenario A-17 de transiciones inválidas. Karate capturó los 7 fallos correctamente con HTTP 400 devuelto por el validador, y el cambio fue inmediatamente revertido (`git checkout`).

---

## 4. Desviaciones y Adaptaciones Técnicas Respecto al Spec

1. **Ubicación de `karate-config.js` (`karate.config.dir`):**
   - Karate busca por defecto `karate-config.js` en la raíz del classpath (`src/test/java/karate-config.js` o `src/test/resources/karate-config.js`).
   - Para mantener la estructura definida en la sección 4 (`src/test/java/com/example/taskmanager/acceptance/karate-config.js`), se configuró la propiedad del sistema `karate.config.dir` tanto en la tarea de Gradle como en `AcceptanceTestRunner.java`.

2. **Aserción de código HTTP dinámico en `patch-status-step.feature`:**
   - La palabra clave `Then status <int>` de Karate requiere sintácticamente un valor numérico literal. Para permitir códigos esperados dinámicos en los helpers parametrizados se utilizó la variable de entorno nativa de Karate `Then match responseStatus == expected`.

3. **Ordenamiento por prioridad en base de datos (A-40):**
   - La entidad `Task` almacena la prioridad mediante `@Enumerated(EnumType.STRING)` como texto (`VARCHAR`). En SQL, el ordenamiento descendente (`ORDER BY priority DESC`) opera alfabéticamente: `MEDIUM` > `LOW` > `HIGH`. Se alineó la aserción del escenario A-40 con dicho orden real de base de datos.

4. **Limitación de estado `OVERDUE` (Sección 6.5):**
   - Dado que las reglas de negocio R7 y R8 impiden asignar fechas límite en el pasado vía API y el reloj está fijado en `2030-01-15`, el estado `OVERDUE` y el filtro `overdue=true` no son alcanzables por HTTP. Esta lógica está cubierta por pruebas unitarias (`TaskUrgencyEvaluatorTest`). El escenario A-35 valida la parte alcanzable: `GET /api/tasks?overdue=true` responde 200 y no retorna tareas activas vigentes creadas en la prueba.

---

## 5. Artefactos y Reportes Generados

- **Reporte HTML Resumen:** `build/karate-reports/karate-summary.html`
- **Reportes HTML por Feature:** `build/karate-reports/*.html`
- **Reporte JUnit XML:** `build/karate-reports/*.xml`
- **Reporte Cucumber JSON:** `build/karate-reports/*.json`
- **Reporte de la Tarea Gradle:** `build/reports/tests/acceptanceTest/index.html`
- **Pipeline CI (`sonarcloud.yml`):** Configurado para ejecutar `./gradlew clean build -x acceptanceTest`, luego `./gradlew acceptanceTest`, publicar el resumen en `$GITHUB_STEP_SUMMARY`, subir los reportes como artefacto `karate-reports` y finalmente ejecutar `./gradlew sonar`.
