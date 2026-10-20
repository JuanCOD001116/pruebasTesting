# Resumen de Pruebas de Aceptación con Karate

> Este documento resume los resultados de la implementación de la capa de pruebas de aceptación end-to-end con Karate según la especificación `.copilot/13-pruebas-aceptacion-karate.md`.

---

## 1. Métricas de Ejecución

Medidas con `gradlew.bat clean build` (incluye `acceptanceTest`) en la rama `test/karate-acceptance`.

| Métrica | Valor |
|---|---|
| **Features ejecutadas** | 9 |
| **Features ignoradas (helpers reutilizables)** | 3 (`create-task.feature`, `change-status.feature`, `patch-status-step.feature`) |
| **Escenarios base definidos** | 54 (A-01 a A-54) |
| **Casos / ejemplos ejecutados** | 86 |
| **Casos pasados** | 86 (100 %) |
| **Casos fallidos** | 0 (0 %) |
| **Tiempo transcurrido de Karate** | ~30 s |
| **Tiempo de `gradlew.bat clean build` completo** | ~2 min |

Cada feature pasa también ejecutado por separado (`-Dkarate.options="classpath:.../<feature>.feature"`), lo que confirma que ningún escenario depende de datos de otro.

---

## 2. Detalle por Feature

| Feature | Archivo | Casos ejecutados | Estado |
|---|---|---|---|
| Operaciones CRUD de tareas | `tasks/crud.feature` | 9 | ✅ Pasaron todos |
| Validación de entrada y manejo de errores | `tasks/validation.feature` | 9 | ✅ Pasaron todos |
| Máquina de estados de una tarea | `status/status-transitions.feature` | 20 | ✅ Pasaron todos |
| Reglas de negocio para fecha límite (`dueDate`) | `duedate/due-date.feature` | 7 | ✅ Pasaron todos |
| Urgencia calculada de tareas (`TaskUrgency`) | `duedate/urgency.feature` | 16 | ✅ Pasaron todos |
| Búsqueda paginada, filtros y ordenamiento | `search/search.feature` | 13 | ✅ Pasaron todos |
| Resumen estadístico de tareas | `summary/summary.feature` | 4 | ✅ Pasaron todos |
| Formato y seguridad de errores | `platform/errors.feature` | 4 | ✅ Pasaron todos |
| Salud de la plataforma y documentación OpenAPI | `platform/health-and-docs.feature` | 4 | ✅ Pasaron todos |

---

## 3. Bugs Encontrados

| Bug | Escenario | Test que lo reproduce | Corrección |
|---|---|---|---|
| `GET /api/tasks/search?sort=priority,desc` devolvía `MEDIUM, LOW, HIGH`: como los enums se guardan como texto (`EnumType.STRING`), la base ordenaba alfabéticamente. Lo mismo ocurría con `status` (`CANCELLED, DONE, IN_PROGRESS, PENDING`). | A-40 | `TaskSearchSortingIntegrationTest` (4 tests; fallaban antes del cambio) | `d40e615` — `TaskSpecifications.orderedBy(Sort)` ordena los enums por su posición declarada (`LOW < MEDIUM < HIGH`; `PENDING → IN_PROGRESS → DONE → CANCELLED`). |

En la primera versión de esta rama, el escenario A-40 se había ajustado al orden alfabético de la base, es decir, documentaba el bug en lugar de detectarlo. La verificación lo corrigió (`4413afa`): ahora exige `HIGH, MEDIUM, LOW`.

**Mecanismo de fallo validado (§12.5):** se cambió en local `Then status 400` por `Then status 200` en A-17; Karate reportó los 7 casos como fallidos y el cambio se revirtió con `git checkout`.

---

## 4. Desviaciones y Adaptaciones Técnicas Respecto al Spec

1. **Ubicación de `karate-config.js`:**
   - Karate busca `karate-config.js` por defecto en la raíz del classpath. Para mantener la estructura de §4 (`acceptance/karate-config.js`), el runner usa `Runner.Builder.configDir("classpath:com/example/taskmanager/acceptance")`, que no depende del directorio de trabajo.

2. **Filtro por tags (`-Dkarate.options`):**
   - Karate 1.5.1 lee `karate.options` por sí mismo y reemplaza los tags del runner, así que no hace falta leerla a mano (§5.2). La tarea Gradle solo reenvía la propiedad al proceso de pruebas. Verificado: `--tags @smoke` ejecuta 10 casos.

3. **Helper adicional `patch-status-step.feature`:**
   - `change-status.feature` recorre una lista de estados (`path`, §6.9); cada paso individual vive en `common/patch-status-step.feature` (también `@ignore`). Como `Then status <n>` exige un literal, el código esperado dinámico se valida con `match responseStatus == expected`.

4. **Escenario A-18 ampliado:**
   - Además de `PENDING→PENDING` e `IN_PROGRESS→IN_PROGRESS` (catálogo), cubre `DONE→DONE` y `CANCELLED→CANCELLED`, como exige §13 ("todos los estados") y la matriz del spec 01 (TC-12, TC-17). Por eso hay 86 casos y no 84.

5. **Limitación de estado `OVERDUE` (§6.5):**
   - Las reglas R7 y R8 impiden asignar fechas límite pasadas vía API y el reloj está fijo en `2030-01-15`, así que `OVERDUE` y `overdue=true` no son alcanzables por HTTP. La lógica está cubierta por pruebas unitarias (`TaskUrgencyEvaluatorTest`, `OverdueRuleTest`, `TaskSpecificationsTest`). A-35 valida la parte alcanzable: `GET /api/tasks?overdue=true` responde 200 con una lista vacía. Documentado al inicio de `urgency.feature`.

6. **Código de producción:**
   - El spec prohíbe código de producción *para facilitar las pruebas*. El único cambio en `src/main` es la corrección del bug de §3, hecha según §12.4 (test que lo reproduce + commit `fix`).

---

## 5. Artefactos y Reportes Generados

- **Reporte HTML Resumen:** `build/karate-reports/karate-summary.html`
- **Reportes HTML por Feature:** `build/karate-reports/*.html`
- **Reporte JUnit XML:** `build/karate-reports/*.xml`
- **Reporte Cucumber JSON:** `build/karate-reports/*.json`
- **Reporte de la Tarea Gradle:** `build/reports/tests/acceptanceTest/index.html`
- **Pipeline CI (`sonarcloud.yml`):** ejecuta `./gradlew clean build -x acceptanceTest`, luego `./gradlew acceptanceTest`, publica el resumen en `$GITHUB_STEP_SUMMARY`, sube los reportes como artefacto `karate-reports` (`actions/upload-artifact@v7`) y finalmente ejecuta `./gradlew sonar`.

## 6. Cobertura y SonarCloud

La cobertura enviada a SonarCloud sigue saliendo solo de `jacocoTestReport.xml` (ejecución de `test`). La aceptación escribe su propio `build/jacoco/acceptanceTest.exec`, que no se usa en ningún reporte. Verificado comparando el XML de `clean test` con el de `clean build` (que sí ejecuta Karate): son **idénticos** (99.9 % de instrucciones, 98.0 % de ramas).
