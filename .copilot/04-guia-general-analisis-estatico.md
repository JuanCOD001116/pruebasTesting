# 04 — Guía General: Taller de Análisis de Código Estático (SonarCloud)

> Guía maestra del **Taller del 10% — Análisis de código estático** aplicado al proyecto **Task Manager API**. Complementa (no reemplaza) a `project-planning.md` y a los specs `00`–`03`. Antes de codificar, respeta las convenciones ya definidas: Java 17, Spring Boot 3.3, Gradle, arquitectura por capas, constructor injection, patrón AAA, Google Java Style Guide.

## 1. Temas que exige el taller y dónde se cubren

| # | Tema del taller | Spec que lo cubre |
|---|---|---|
| 1 | SonarCloud + ruta local para ver la cobertura con el reporte de JaCoCo | `05-sonarcloud-y-jacoco.md` |
| 2 | Quality Profile | `06-quality-profile.md` |
| 3 | Quality Gate | `07-quality-gate.md` |
| 4 | Complejidad ciclomática | `10-complejidad-ciclomatica.md` |
| 5 | Complejidad cognitiva | `11-complejidad-cognitiva.md` |
| — | Corrección de hallazgos del código actual (lo que Sonar va a reportar) | `08-remediacion-hallazgos.md` |
| — | Nuevas funcionalidades para que el análisis tenga material real | `09-nuevas-funcionalidades.md` |

## 2. Por qué hay funcionalidades nuevas

El código actual es pequeño y casi todo es CRUD lineal: la mayoría de métodos tiene complejidad ciclomática 1 y cognitiva 0. Con eso el análisis de complejidad queda pobre (solo `TaskStatusTransitionValidator.validate` y `TaskController.getAllTasks` tienen ramas). El spec `09` agrega tres funcionalidades coherentes con el dominio que sí generan lógica condicional medible:

1. **Fecha límite (`dueDate`) + urgencia calculada** (`ON_TRACK`, `AT_RISK`, `OVERDUE`, `NONE`) — incluye un caso de estudio "antes/después" para complejidad cognitiva.
2. **Búsqueda paginada con filtros combinables** usando el **Specification Pattern** de Spring Data — reemplaza la cadena `if / else if` del controller.
3. **Resumen estadístico** de tareas (conteos por estado y prioridad, vencidas, tasa de completitud).

## 3. Orden de ejecución (fases)

El orden importa: la **línea base** debe medirse ANTES de cambiar el código, para poder mostrar el "antes y después" en el informe.

| Fase | Qué se hace | Specs |
|---|---|---|
| 0 — Línea base | Configurar SonarCloud + JaCoCo y analizar el código **tal como está hoy**. Guardar capturas. | 05 |
| 1 — Estándares | Crear el Quality Profile y el Quality Gate propios; volver a analizar (aparecerán issues nuevos). | 06, 07 |
| 2 — Remediación | Corregir los hallazgos del código existente. | 08 |
| 3 — Funcionalidades | Implementar las 3 funcionalidades nuevas, incluido el caso de estudio de complejidad cognitiva (commit "ingenuo" → análisis → refactor → análisis). | 09, 11 §6 |
| 4 — Métricas | Medir y documentar complejidad ciclomática y cognitiva antes/después. | 10, 11 |

Cada fase termina con un `push` a `main` (o un PR) para que SonarCloud registre un análisis distinto en el historial. Esto deja trazabilidad en la pestaña **Activity** del proyecto.

## 4. Estándares obligatorios (heredados + nuevos)

- Patrón **AAA** con comentarios `// Arrange`, `// Act`, `// Assert` en todos los tests.
- Métodos de test en inglés descriptivo + `@DisplayName` en español estilo Given/When/Then.
- **F.I.R.S.T** y evitar la paradoja del pesticida (variar datos de prueba).
- Ningún método nuevo con complejidad ciclomática > 10 ni cognitiva > 10 (umbral del Quality Profile, spec 06).
- Cero issues nuevos de severidad *Blocker*/*High* y 100% de Security Hotspots revisados (Quality Gate, spec 07).
- Tiempo inyectable con `java.time.Clock` para que toda lógica que dependa de "hoy" sea determinista en los tests.
- Ningún secreto (token de Sonar, contraseñas) en el repositorio. Siempre por variables de entorno / GitHub Secrets.

## 5. Artefactos que deben quedar en el repositorio

```
.github/workflows/sonarcloud.yml          ← análisis automático en cada push/PR (spec 05)
lombok.config                              ← excluye código generado de la cobertura (spec 05)
quality/sonar/taskmanager-java-way.xml    ← backup del Quality Profile (spec 06)
quality/sonar/quality-gate.md             ← definición documentada del Quality Gate (spec 07)
docs/analisis-estatico/informe-metricas.md← tablas antes/después de complejidad y cobertura (specs 10, 11)
docs/analisis-estatico/evidencias/        ← capturas de pantalla numeradas
```

## 6. Evidencias para el informe del taller

Guarda las capturas en `docs/analisis-estatico/evidencias/` con este nombre y orden:

| Archivo | Qué debe mostrar |
|---|---|
| `01-jacoco-local-index.png` | `build/reports/jacoco/test/html/index.html` abierto en el navegador (con la ruta visible en la barra de direcciones) |
| `02-jacoco-validator-ramas.png` | Detalle de `TaskStatusTransitionValidator` en JaCoCo con los rombos de rama en verde |
| `03-sonar-overview-linea-base.png` | Overview de SonarCloud del primer análisis (Fase 0) |
| `04-quality-profile-reglas.png` | Quality Profile propio con las reglas activadas y sus parámetros |
| `05-quality-profile-changelog.png` | Changelog del perfil (muestra qué se cambió respecto a Sonar way) |
| `06-quality-gate-condiciones.png` | Condiciones del Quality Gate propio |
| `07-quality-gate-failed.png` | Gate en **Failed** por el commit ingenuo del caso de estudio (spec 11 §6) |
| `08-issue-s3776.png` | Issue de complejidad cognitiva detectado en `TaskUrgencyEvaluator` |
| `09-quality-gate-passed.png` | Gate en **Passed** después del refactor |
| `10-measures-complexity.png` | Pestaña *Measures → Complexity* con ciclomática y cognitiva por archivo |
| `11-activity-historial.png` | Pestaña *Activity* mostrando la evolución entre fases |

## 7. Checklist de entregables

- [ ] SonarCloud conectado al repositorio y analizando desde GitHub Actions (spec 05).
- [ ] Reporte JaCoCo XML importado por Sonar (la cobertura en Sonar coincide con la local ±1%).
- [ ] Ruta local del reporte JaCoCo documentada y capturada (spec 05 §6).
- [ ] Quality Profile propio activo para Java, con backup XML versionado (spec 06).
- [ ] Quality Gate propio asignado al proyecto y documentado (spec 07).
- [ ] Todos los hallazgos `H-01` … `H-15` resueltos o justificados (spec 08).
- [ ] Funcionalidades F1, F2, F3 implementadas con tests (spec 09).
- [ ] Caso de estudio de complejidad cognitiva ejecutado con evidencias antes/después (spec 11 §6).
- [ ] `informe-metricas.md` con tablas de complejidad ciclomática y cognitiva antes/después (specs 10, 11).
- [ ] `./gradlew clean build` pasa (incluye `jacocoTestCoverageVerification`).
- [ ] Último análisis de SonarCloud con Quality Gate en **Passed**.

## 8. Definición de "hecho"

1. `./gradlew clean build` pasa sin fallos.
2. El workflow `sonarcloud.yml` termina en verde y el Quality Gate está en **Passed**.
3. Ningún método del código de producción supera complejidad ciclomática 10 ni cognitiva 10 (cero issues abiertos de `java:S1541` y `java:S3776`).
4. Cobertura global ≥ 80% en código nuevo y en la capa de servicio.
5. El informe de métricas y las evidencias están en `docs/analisis-estatico/`.
