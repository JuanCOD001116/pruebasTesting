# Reporte de verificación — 2026-10-19

> **Alcance:** verificación de los specs `.copilot/00` a `.copilot/11` según el método de
> `.copilot/12-verificacion-y-correccion-claude-code.md`, más las 6 desviaciones conocidas indicadas
> por el usuario.
> **Rama:** `chore/verificacion-final` (creada desde `main` en `ad6ab04`).
> **SonarCloud:** `JuanCOD001116_pruebasTesting` · organización `juancod001116` · perfil
> *TaskManager Java Way* · gate *TaskManager Gate*.

## Resumen

| Estado | Cantidad |
|---|---|
| ✅ OK | 47 |
| 🔧 Corregido | 12 |
| ⬆️ Mejorado | 10 |
| ❌→✅ Implementado (faltaba) | 1 |
| ⚠️ Pendiente manual | 2 |

72 ítems: los de §3 del spec 12 (algunos divididos en sub-ítems "b") más las 6 desviaciones
indicadas por el usuario.

## Línea base vs. final

Línea base: `./gradlew clean build` sobre `main` (`ad6ab04`) antes de cualquier cambio.
Final: `./gradlew clean build` sobre el último commit de esta rama.

| Métrica | Antes | Después |
|---|---|---|
| Tests totales / fallidos | 132 / 0 | **164 / 0** |
| Clases de prueba | 18 | 23 |
| Cobertura global JaCoCo (instrucciones / ramas / líneas) | 96.8 % / 92.4 % / 97.0 % | **99.9 % / 97.8 % / 99.7 %** |
| Cobertura de ramas `service.validation` / `service.urgency` | 100 % / 100 % | 100 % / 100 % |
| Cobertura de ramas `service.impl` / `controller` (instr.) | 85.7 % / 75.3 % | 100 % / 100 % |
| Máx. complejidad ciclomática por método | 7 (`validate`) | 7 (`validate`) |
| `jacocoTestCoverageVerification` | Pasa | Pasa (umbrales sin cambios) |
| Quality Gate (Sonar) | OK en `main` | **OK (Passed) en el PR #3** |
| Issues abiertos (bugs / vulnerabilidades / code smells) | 0 / 0 / 0 | 0 / 0 / 0 (`new_violations` = 0) |
| Código nuevo del PR: cobertura / duplicación | — | 100 % / 0.0 % |
| Ratings de código nuevo (confiabilidad / seguridad / mantenibilidad) | — | A / A / A |
| Security Hotspots por revisar | 0 | 0 (100 % revisados) |

## Detalle por ítem

### V-05 — SonarCloud y JaCoCo

| ID | Estado | Hallazgo | Acción | Commit |
|---|---|---|---|---|
| 05.1 | ✅ | Plugin y bloque `sonar {}` completos (`projectKey`, `organization`, `host.url`, `xmlReportPaths`, `coverage.exclusions`, `qualitygate.wait`). | — | — |
| 05.1b | ⬆️ | Plugin `org.sonarqube` en 6.0.1.5171; el spec pide la última versión publicada. | Actualizado a 7.5.0.8588; `sonar → jacocoTestReport → test` verificado con `--dry-run`. | `ab32624` |
| 05.2 | ✅ | `git grep -nIiE "sonar\.token\|sonar\.login\|squ_\|sqp_"` solo encuentra los specs que lo mencionan como texto. | — | — |
| 05.3 | ✅ | `tasks.named('sonar') { dependsOn jacocoTestReport }` presente. | — | — |
| 05.4 | ✅ | Exclusiones idénticas en JaCoCo y Sonar (`TaskManagerApplication`, `config/**`, `dto/**`, `exception/*Exception`). | — | — |
| 05.5 | ✅ | `lombok.config` con `stopBubbling` y `addLombokGeneratedAnnotation`. | — | — |
| 05.6 | ✅ | Existen `html/index.html`, `jacocoTestReport.xml` y `build/jacoco/test.exec`. | — | — |
| 05.7 | ⬆️ | Workflow válido (YAML OK, `fetch-depth: 0`, temurin 17, `SONAR_TOKEN` de secrets, `build sonar`), pero con actions en Node 20 obsoleto. | `checkout@v7`, `setup-java@v6`, `cache@v6`, `setup-gradle@v5` (todas `node24`). | `8f38c79` |
| 05.8 | ✅ | 0 métodos `builder` en el XML de JaCoCo. | — | — |

### V-06 / V-07 — Quality Profile y Quality Gate

| ID | Estado | Hallazgo | Acción | Commit |
|---|---|---|---|---|
| 06.1 | ⚠️ | El XML tiene `S3776`=10, `S1541`=10, `S134`=3, `S1067`=3, `S138`=40, `S107`=5/7 y las reglas de §4.2–4.3, **excepto** `java:S1128`, `java:S1068`, `java:S5778` y `java:S5786`, que la API confirma inactivas. | Requiere la UI de SonarCloud (pendiente manual §2). | — |
| 06.2 | ✅ | `qualityprofiles/search` → `TaskManager Java Way` (hereda de Sonar way, 317 reglas). | — | — |
| 07.1 | ✅ | `quality-gate.md` documenta todas las condiciones de §2 con justificación. | — | — |
| 07.2 | ✅ | `get_by_project` → `TaskManager Gate`; `project_status` de `main` → `OK`. | — | — |
| 07.2b | 🔧 | El primer análisis del PR #3 dejó el gate en **Failed** (`new_security_rating` = C, `new_violations` = 1) por `githubactions:S7637`: `gradle/actions/setup-gradle@v5`, action de terceros referenciada por etiqueta mutable. | Fijada al SHA completo de v5.0.2 (`0723195…`). Nuevo análisis: gate **OK**, 0 issues. | `9e2f21d` |
| 07.3 | ✅ | `hotspots/search?status=TO_REVIEW` → `total: 0`. | — | — |

### V-08 — Remediación de hallazgos

| ID | Estado | Hallazgo | Acción | Commit |
|---|---|---|---|---|
| H-01 | ✅ | Sin `demo`; `jar` deshabilitado; `build/libs` solo contiene `task-manager-api.jar`. | — | — |
| H-02 | ✅ | `application.properties` no existe. | — | — |
| H-03 | ✅ | Sin `dialect`; dev con `MODE=PostgreSQL;DB_CLOSE_DELAY=-1` y `validate`; `max-page-size: 100`. `bootRun` con perfil dev: Flyway aplica V1 y V2 y `/actuator/health` → `UP`. | — | — |
| H-04 | ⚠️ | Revisión estática OK: Actuator solo `health`, `USER app`, JAR fijo, healthcheck con `wget`. `docker build` no se pudo ejecutar (Docker Desktop apagado). | Pendiente manual §3. | — |
| H-05 | ✅ | `Task` sin `@Data`, `equals/hashCode` por `id`, auditoría con `DateTimeProvider` desde `Clock` en `JpaAuditingConfig`. | — | — |
| H-06 | 🔧 | Al levantar la app, rutas inexistentes, métodos no soportados y `Content-Type` no soportado devolvían **500** (log `error`): el handler genérico capturaba las excepciones de Spring MVC. | Handler que usa el status propio de cada excepción → 404/405/415 con cuerpo estandarizado y log `warn`. 3 tests MockMvc que fallaban antes. | `deead13` |
| H-06b | ⬆️ | El handler 500 cumplía el spec (mensaje fijo) pero no tenía ningún test; literal duplicado del mensaje de parámetro inválido. | Test unitario que prueba que `ex.getMessage()` no se filtra; constante `INVALID_PARAMETER_MESSAGE`. | `162bce4` |
| H-07 | ✅ | `findTaskOrThrow` único `orElseThrow`; sin `Collectors.toList()`; no se loguea `getTitle()`; not-found en `warn`. | — | — |
| H-08 | ✅ | Controller sin `if/else if` ni wildcard imports. (Formato: ver V-STYLE.) | — | — |
| H-09 | ✅ | `@NullOrNotBlank` + `@Size`; test de `PUT` con título en blanco → 400. | — | — |
| H-10 | 🔧 | Lombok fijado a `1.18.36` con `ext['lombok.version']` y versiones explícitas: pisaba el BOM. JUnit/Mockito sin forzar. | Lombok lo gestiona el BOM (1.18.38 con Boot 3.3.13). | `5c72be5` |
| H-11 | ✅ | `TaskControllerTest` renombrado; 404 con MockMvc; `assertTimeout(Duration.ofMillis(200))`. | — | — |
| H-12 | ✅ | Javadoc con V(G) = 7 y 7 caminos (incluye TC-18). | — | — |
| H-13 | ✅ | `TaskMapper` con `applyIfPresent`. | — | — |
| H-14 | ✅ | `application-prod.yml` sin defaults de contraseña; compose lee de `.env`; `.env.example` actualizado. | — | — |
| H-15 | 🔧 | README con estructura desactualizada (faltaban F1–F3, V2, docs), endpoints "en desarrollo" y sin conteo de tests. | Actualizado (164 pruebas / 23 clases, endpoints reales, Boot 3.3.13). | `a715a28` |

### V-09 — Funcionalidades nuevas

| ID | Estado | Hallazgo | Acción | Commit |
|---|---|---|---|---|
| F0.1 | ✅ | `ClockConfig` con zona configurable; ningún `LocalDate(Time).now()` sin `Clock` en `src/main`. | — | — |
| F0.2 | ✅ | `isTerminal()` y `activeStatuses()`; enum sin renombrar. | — | — |
| F1.1 | ✅ | V2 con columna e índice; V1 no se modificó desde su creación. | — | — |
| F1.2 | ✅ | `dueDate` en DTOs; `urgency` solo en `TaskResponse`, no persistida. | — | — |
| F1.3 | ✅ | `TaskDueDateValidator` aplica R7 y R8 (solo si cambia). | — | — |
| F1.4 | 🔧 | Chain of Responsibility correcta, pero el evaluador conservaba un constructor secundario `(Clock, UrgencyProperties)` que instanciaba las reglas y forzaba `@Autowired` (desviación 1). | Constructor eliminado; el evaluador depende solo de `List<UrgencyRule>`; test con `ApplicationContextRunner` del orden `@Order`. | `6a51096` |
| F1.5 | ✅ | Ventanas en `app.urgency.windows` vía `UrgencyProperties`. | — | — |
| F1.6 | ❌→✅ | Bordes cubiertos en `TaskUrgencyEvaluatorTest`, pero **faltaba** "un test unitario por regla" (spec 09 §3.5). | `TerminalStatusRuleTest`, `NoDueDateRuleTest`, `OverdueRuleTest`, `DueSoonRuleTest`. | `aba6f07` |
| F2.1 | ✅ | `JpaSpecificationExecutor`, finders eliminados, `TaskSpecifications` con constructor privado. | — | — |
| F2.1b | ⬆️ | `TaskService.getAllTasks()` tenía un cuerpo `default` que `TaskServiceImpl` sobrescribía (código muerto). | Método abstracto. | `34c630c` |
| F2.2 | ✅ | `TaskSearchCriteria` record con `isDueRangeValid()`; `@Valid @ParameterObject`. | — | — |
| F2.3 | ✅ | `PageResponse`, lista blanca de `sort` (`password` → 400), `dueFrom>dueTo` → 400 (MockMvc y `curl`). | — | — |
| F2.3b | ⬆️ | Ramas sin test: `assignedTo`/`text` en blanco y `overdue` sin fecha. | 2 tests en `TaskSpecificationsTest`. | `5e5b7bd` |
| F3.1 | ✅ | Todas las claves de enum, `overdue`, `completionRate` con 2 decimales y sin división por cero. | — | — |
| F3.2 | ✅ | Proyecciones; sin `findAll` en `TaskSummaryServiceImpl`. | — | — |
| F3.3 | ✅ | Test que confirma que `/summary` no choca con `/{id}`. | — | — |
| F.4 | ✅ | `/v3/api-docs` incluye `/api/tasks/search` y `/api/tasks/summary`. | — | — |

### V-10 / V-11 — Complejidad

| ID | Estado | Hallazgo | Acción | Commit |
|---|---|---|---|---|
| 10.1 | ✅ | Script §4.1: máximo V(G) por método = 7 (`validate`); ninguno > 10. | — | — |
| 10.2 | 🔧 | Diferencias con la tabla del spec 10 §5: `updateTask` medía 4 (esperado 3, faltaba `resolveAssignee`) y `completionRate` no existía como método (estaba dentro de `getSummary`, V(G) 4). | Extraídos `resolveAssignee` (+3 tests de rama) y `completionRate`; ahora 3 y 2. Tabla medida en `informe-metricas.md` §5.3. | `4d6be2f`, `446b345` |
| 11.1 | ✅ | Sonar por archivo: ningún método supera cognitiva 10 (el mayor total por archivo, `TaskSpecifications` = 13, se reparte en 8 métodos de ≤ 3). | — | — |
| 11.2 | ✅ | `fbc0df1` (A) → `b31c43a` (B) en orden; `git show --stat b31c43a -- src/test` vacío. (El único cambio en `src/test` entre A y B es `2f412c3`, que quitó un import sin uso.) | No se reescribió el historial. | — |
| 11.3 | ✅ | `TaskUrgencyEvaluator.evaluate` es el despachador por stream; no queda código ingenuo. | — | — |
| 11.4 | ✅ | `issues/search?rules=java:S3776,java:S1541&resolved=false` → 0. | — | — |

### V-TEST — Calidad de la suite

| ID | Estado | Hallazgo | Acción | Commit |
|---|---|---|---|---|
| T.1 | 🔧 | 22 tests con etiquetas combinadas (`// Arrange & Act & Assert`, `// Arrange & Act`, `// Arrange - Act - Assert`). | Separadas en `// Arrange:` + `// Act` / `// Act & Assert`. Script: 0 incompletos. | `52bd481` |
| T.2 | 🔧 | 12 tests sin `@DisplayName` y 22 en inglés o sin forma Dado/cuando/entonces. | Todos en español Given/When/Then. | `a94b062`, `bdbeaac`, `52bd481` |
| T.3 | ✅ | Ningún test sin aserciones (script). Ningún test prueba solo un mock. | — | — |
| T.3b | ⬆️ | `TaskController.updateTask/updateTaskStatus` con 0 % de cobertura: solo se probaban los rechazos. | Tests MockMvc de transiciones válidas por PATCH y PUT. | `a94b062` |
| T.4 | 🔧 | `TaskSpecificationsTest` ejecutada sola fallaba 9/9 (`NULL not allowed for column CREATED_AT`): el `@DataJpaTest` no cargaba la auditoría. Reproducido en `origin/main`. | `@Import({JpaAuditingConfig, ClockConfig})`. Luego: las 23 clases pasan por separado y la suite pasa 2 veces en orden aleatorio (clases y métodos). | `493f5cd` |
| T.5 | ✅ | Datos variados (`@MethodSource`, `@CsvSource`, nombres/fechas distintos por clase). | — | — |
| T.6 | ✅ | `LocalDate(Time).now()` en tests solo para `createdAt/updatedAt` de fixtures y rangos de `TaskSearchCriteriaTest` cuyo resultado no depende de la fecha. La lógica temporal usa `Clock.fixed`. | Justificado. | — |

### V-STYLE — Código limpio y convenciones

| ID | Estado | Hallazgo | Acción | Commit |
|---|---|---|---|---|
| S.1 | ✅ | Sin wildcard imports, `System.out`, `printStackTrace`, `Collectors.toList()`, `@Data` en `model`, `TODO/FIXME`. | — | — |
| S.2 | 🔧 | `@Autowired` en `TaskUrgencyEvaluator` (solo para elegir entre dos constructores). | Desaparece al quitar el constructor secundario. | `6a51096` |
| S.3 | ⬆️ | Métodos de `TaskController` indentados a 8/16/24 espacios; 25 líneas de `src/main` > 100 columnas. | Reindentado y envuelto sin cambiar tokens (verificado). | `704a624` |
| S.4 | ⬆️ | 3 `@SuppressWarnings("unchecked")` en `TaskServiceTest`. | Matcher tipado `ArgumentMatchers.<Specification<Task>>any()`. | `bdbeaac` |
| S.5 | ✅ | Sin entidades expuestas en controllers ni lógica de negocio en ellos; métodos ≤ 40 líneas; ≤ 5 parámetros (constructores ≤ 7). | — | — |

### Desviaciones conocidas indicadas por el usuario

| # | Estado | Evaluación | Acción | Commit |
|---|---|---|---|---|
| D1 | 🔧 | Constructor secundario de `TaskUrgencyEvaluator`. | Ver F1.4. | `6a51096` |
| D2 | ✅ | **Spring Boot nunca se actualizó a 3.4**: `build.gradle` declara `3.3.0` en todo el historial (`git log -G`) y se resolvía 3.3.0. La afirmación venía de la desviación 7 del informe, que era incorrecta. Mantener la línea 3.3 es lo correcto (specs 04 y 08 H-10; springdoc 2.6.x). | Se sube al último parche, 3.3.13 (seguridad), y se corrige el informe. | `5c72be5`, `e95b412` |
| D3 | ⬆️ | Plugin `org.sonarqube` 6.0.1. | Ver 05.1b. | `ab32624` |
| D4 | ⬆️ | Actions en Node 20. | Ver 05.7. | `8f38c79` |
| D5 | ✅ | `3a0a002` cambió `@RequiredArgsConstructor` por constructores explícitos **solo en servicios/controller** (≤ 6 parámetros): es constructor injection válida, no afecta métricas y evita el problema del IDE. Se mantiene. `Task` y DTOs ya habían vuelto a Lombok en `80a2fb8` porque un constructor manual de 9 parámetros viola `S107`. Lo que **no** conviene es fijar Lombok a mano (H-10). | Versión fija de Lombok eliminada; IDE resuelto por configuración de usuario (D6). | `5c72be5` |
| D6 | 🔧 | `.vscode/settings.json` tenía `java.jdt.ls.java.home` y `java.configuration.runtimes` con `C:\Program Files\Java\jdk-21`: en otra máquina el Java Language Server no arranca. | Quitadas del archivo versionado; README explica cómo ponerlas en el `settings.json` de usuario. | `605b5b4`, `a715a28` |

## Desviaciones respecto a los specs

1. **Rama de trabajo:** el spec 12 §1.3 indica `chore/verificacion-calidad`; por instrucción del usuario se usó `chore/verificacion-final` desde `main`, porque la otra ya está mergeada.
2. **Trabajo previo sin commitear:** al empezar, `docs/informe-metricas` tenía cambios pendientes (§1.2). Durante la lectura el usuario los commiteó (`14653df`). Se trajeron a esta rama con `cherry-pick` (`8911936`), sin modificar su contenido, porque el reporte y el informe viven en esa carpeta.
3. **Plugin `org.sonarqube` 7.5.0.8588** en lugar de 7.3.x: el spec pide "la última versión publicada"; 7.3.x era la de la fecha de redacción.
4. **Versiones de GitHub Actions:** el spec muestra `@v4` pero pide "la versión mayor vigente". Se usan v7/v6/v6 para checkout/setup-java/cache. `gradle/actions/setup-gradle` queda en **v5** (Node 24) y no en v6, porque v6 mueve el caché a un componente propietario y obliga a aceptar los *Terms of Use* de Gradle; esa decisión es del dueño del repositorio. Además se referencia por **SHA completo** (v5.0.2) y no por etiqueta, que es lo que exige `githubactions:S7637` para actions de terceros.
5. **Constructores explícitos** en lugar de `@RequiredArgsConstructor` (spec 09 §3.4) en servicios y controller: equivalentes en diseño (constructor injection), se conservan por compatibilidad con IDEs (D5).
6. **`spring-boot-configuration-processor`** (`2f412c3`, ya en `main`): no lo pide ningún spec. Solo genera metadatos para que el IDE reconozca `app.urgency.*`; no aporta código ejecutable. Se mantiene y se documenta aquí.
7. **Handler 4xx de Spring MVC (H-06):** el spec enumera tres handlers 400. Se agregó un cuarto para 404/405/415 porque la misma clase de defecto ("entradas inválidas que terminan en 500") aparecía con rutas y métodos inexistentes.

## Pendientes manuales para el usuario

1. **Merge del PR #3 y análisis de `main`.** El gate del PR ya está en **Passed** (verificado por API). Tras hacer merge, comprobar que el análisis de `main` también queda en OK:
   ```bash
   curl -s "https://sonarcloud.io/api/qualitygates/project_status?projectKey=JuanCOD001116_pruebasTesting"
   ```
2. **Activar 4 reglas faltantes del Quality Profile (spec 06 §4.2–4.3).**
   1. SonarCloud → organización `juancod001116` → *Quality Profiles* → *Java* → **TaskManager Java Way**.
   2. *Activate More* → buscar y activar con la severidad por defecto: `java:S1128` (*Unnecessary imports should be removed*), `java:S1068` (*Unused "private" fields should be removed*), `java:S5778` (*Only one method invocation is expected when testing runtime exceptions*), `java:S5786` (*JUnit5 test classes and methods should have default package visibility*).
   3. ⚙ → *Back up* → reemplazar `quality/sonar/taskmanager-java-way.xml` y commitearlo.
   4. Volver a analizar (push a `main` o al PR). Si aparecen issues en código existente, corregirlos; no desactivar las reglas.
3. **Verificar la imagen Docker (H-04).** Con Docker Desktop encendido:
   ```bash
   docker build -t task-manager-api:verify .
   docker run --rm task-manager-api:verify id    # debe mostrar uid de "app", no root
   ```
4. **Configuración del IDE (D6).** En el `settings.json` de usuario de Antigravity (no en el del proyecto) agregar `"java.jdt.ls.java.home": "C:\\Program Files\\Java\\jdk-21"` si vuelven los falsos positivos de Lombok con el JDK 27 del language server.
5. **`gradle/actions` v6 (opcional):** si se aceptan los *Terms of Use* de Gradle, cambiar `setup-gradle@v5` por `@v6` en el workflow.

## Evidencias faltantes (spec 04 §6)

`docs/analisis-estatico/evidencias/` solo contiene `.gitkeep`. Faltan las 11 capturas:
`01-jacoco-local-index.png`, `02-jacoco-validator-ramas.png`, `03-sonar-overview-linea-base.png`,
`04-quality-profile-reglas.png`, `05-quality-profile-changelog.png`, `06-quality-gate-condiciones.png`,
`07-quality-gate-failed.png`, `08-issue-s3776.png`, `09-quality-gate-passed.png`,
`10-measures-complexity.png` y `11-activity-historial.png`. Se toman desde el navegador (JaCoCo:
`build/reports/jacoco/test/html/index.html`; resto: SonarCloud, PR #2 para 07–09).
