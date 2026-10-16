# 12 — Verificación, Corrección y Mejora (instrucciones para Claude Code)

> **Rol:** actúas como revisor técnico senior y QA del proyecto **Task Manager API**. Tu trabajo es **verificar** que todo lo especificado en `.copilot/00` a `.copilot/11` esté implementado correctamente, **corregir** los errores que encuentres y **mejorar** lo que esté mal implementado. Siempre sigue la lógica, la arquitectura y las convenciones ya definidas. No inventes alcance nuevo.

---

## 1. Antes de tocar nada

1. Lee completos, en este orden: `project-planning.md`, `.copilot/00` … `.copilot/11`. Son la **fuente de verdad**. Si el código contradice un spec, gana el spec, salvo que el spec tenga un error técnico demostrable (ver §6.4).
2. Revisa el estado del repositorio: `git status` y `git log --oneline -30`. No pierdas trabajo sin commitear: si hay cambios pendientes, pregunta antes de seguir.
3. Crea una rama de trabajo: `git checkout -b chore/verificacion-calidad`.
4. Establece la **línea base**:

```bash
./gradlew clean build --console=plain 2>&1 | tee build/verificacion-baseline.log
```

   Anota: tests totales, fallidos, cobertura (`build/reports/jacoco/test/html/index.html`) y si `jacocoTestCoverageVerification` pasó.
5. Crea la lista de tareas con una entrada por sección de §3. Márcalas a medida que avances.

---

## 2. Método de trabajo para cada verificación

Para cada ítem de §3:

1. **Verificar** con evidencia objetiva: comando, búsqueda en el código o test. Nunca marques algo como correcto "porque parece".
2. **Clasificar** el resultado:
   - ✅ **OK**: cumple el spec.
   - 🔧 **Corregido**: había un error (no compila, falla, contradice el spec, bug) y lo arreglaste.
   - ⬆️ **Mejorado**: funcionaba pero violaba buenas prácticas, el estilo o el diseño definido, y lo mejoraste.
   - ⚠️ **Pendiente manual**: no se puede verificar desde la terminal (ej. pantallas de SonarCloud). Deja instrucciones claras para el usuario.
   - ❌ **No implementado**: falta por completo. Impleméntalo siguiendo su spec.
3. **Corregir/mejorar** con el cambio mínimo que resuelve el problema de raíz.
4. **Re-verificar** ejecutando los tests afectados.
5. **Commit** atómico con el ID del ítem: `fix(V-08-H06): ...`, `refactor(V-11): ...`, `feat(V-09-F2): ...`, `test(V-10): ...`.

---

## 3. Lista de verificación

### V-05 — SonarCloud y JaCoCo (spec 05)

| # | Verificación | Cómo |
|---|---|---|
| 05.1 | Plugin `org.sonarqube` aplicado y bloque `sonar { }` con `projectKey`, `organization`, `host.url`, `xmlReportPaths`, `coverage.exclusions`, `qualitygate.wait` | Leer `build.gradle` |
| 05.2 | **Ningún token** en el repo | `git grep -nIiE "sonar\.token\|sonar\.login\|squ_\|sqp_"` debe dar vacío. Si hay un token, elimínalo y avisa al usuario que **lo revoque** en SonarCloud (ya quedó en el historial). |
| 05.3 | `tasks.named('sonar') { dependsOn jacocoTestReport }` | Leer `build.gradle` |
| 05.4 | Exclusiones idénticas en JaCoCo (`classDirectories`) y Sonar (`sonar.coverage.exclusions`) | Comparar ambas listas |
| 05.5 | `lombok.config` con `lombok.addLombokGeneratedAnnotation = true` y `config.stopBubbling = true` | Leer el archivo |
| 05.6 | Los 3 reportes existen tras `./gradlew test` | `ls build/reports/jacoco/test/html/index.html build/reports/jacoco/test/jacocoTestReport.xml build/jacoco/test.exec` |
| 05.7 | Workflow `.github/workflows/sonarcloud.yml` válido: `fetch-depth: 0`, Java 17 temurin, `SONAR_TOKEN` desde secrets, ejecuta `build sonar` | Leer el YAML; validar sintaxis con `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/sonarcloud.yml'))"` |
| 05.8 | El JAR de Lombok no aparece como clase medida (sin métodos `@Generated` en el XML) | `grep -c 'name="builder"' build/reports/jacoco/test/jacocoTestReport.xml` debe ser 0 o casi 0 |

### V-06 / V-07 — Quality Profile y Quality Gate (specs 06, 07)

No se configuran desde la terminal; verifica los artefactos versionados y, si el proyecto es público, consulta la API de SonarCloud (no requiere token para proyectos públicos).

| # | Verificación | Cómo |
|---|---|---|
| 06.1 | `quality/sonar/taskmanager-java-way.xml` existe y contiene `java:S3776` con threshold 10 y `java:S1541` con threshold 10 | `grep -nE "S3776\|S1541\|S134\|S1067\|S138\|S107" quality/sonar/taskmanager-java-way.xml` |
| 06.2 | El proyecto usa el perfil propio | `curl -s "https://sonarcloud.io/api/qualityprofiles/search?project=<projectKey>&language=java"` → `name` debe ser `TaskManager Java Way` |
| 07.1 | `quality/sonar/quality-gate.md` documenta todas las condiciones de spec 07 §2 con justificación | Leer el archivo |
| 07.2 | El proyecto usa el gate propio y está en `OK` | `curl -s "https://sonarcloud.io/api/qualitygates/get_by_project?project=<projectKey>&organization=<org>"` y `curl -s "https://sonarcloud.io/api/qualitygates/project_status?projectKey=<projectKey>"` |
| 07.3 | 100% de hotspots revisados | `curl -s "https://sonarcloud.io/api/hotspots/search?projectKey=<projectKey>&status=TO_REVIEW"` → `paging.total` = 0 |

Si el repo es privado o la API no responde: marca ⚠️ y deja al usuario los pasos exactos para revisarlo en la UI.

### V-08 — Remediación de hallazgos (spec 08)

| # | Verificación | Cómo |
|---|---|---|
| H-01 | Sin `demo` en build/Docker; `bootJar` con nombre fijo; `jar` deshabilitado | `git grep -nw demo -- settings.gradle build.gradle Dockerfile src` vacío; `ls build/libs/` muestra solo `task-manager-api.jar` |
| H-02 | `application.properties` eliminado | `test ! -f src/main/resources/application.properties` |
| H-03 | Sin `dialect` global; dev con `MODE=PostgreSQL;DB_CLOSE_DELAY=-1` y `ddl-auto: validate`; Flyway es la única fuente del esquema; `max-page-size: 100` | Leer los 3 YAML; levantar la app: `./gradlew bootRun --args='--spring.profiles.active=dev'` y verificar `curl -s localhost:8080/actuator/health` → `UP` (detener luego) |
| H-04 | Actuator con solo `health`; Dockerfile con `USER` no root y JAR correcto; compose con `wget` a `/actuator/health` | Leer archivos. Si Docker está disponible: `docker build -t task-manager-api:verify .` |
| H-05 | `Task` sin `@Data`; `equals/hashCode` por `id`; JPA Auditing con `DateTimeProvider` desde `Clock` en `JpaAuditingConfig` (no en la clase main) | Leer `Task.java`, `config/` |
| H-06 | `buildResponse(...)` único; handler genérico **sin** `ex.getMessage()` en la respuesta; handlers 400 para `MethodArgumentTypeMismatchException`, `HttpMessageNotReadableException`, `InvalidTaskDataException`; 4xx→`warn`, 5xx→`error` | Leer el handler; probar `GET /api/tasks?status=FOO` → 400 |
| H-07 | `findTaskOrThrow` existe y no hay `orElseThrow` duplicados; sin `Collectors.toList()`; no se loguea `getTitle()`; not-found con `warn` | `git grep -n "Collectors.toList\|getTitle()" -- src/main` (en logs debe estar vacío) |
| H-08 | Controller sin `if/else if` de filtros; sin wildcard imports | Leer `TaskController` |
| H-09 | `@NullOrNotBlank` + `@Size` en `UpdateTaskRequest`; `@Size` en `CreateTaskRequest`; test de `PUT` con título en blanco → 400 | Leer DTOs y tests |
| H-10 | Sin versiones forzadas de JUnit/Mockito; sin deps redundantes; springdoc compatible con Boot 3.3 | `./gradlew dependencies --configuration testRuntimeClasspath \| grep -E "junit-jupiter\|mockito-core"` sin conflictos `->` inesperados |
| H-11 | `TaskControllerTest` (renombrado); 404 probado vía MockMvc; sin campos/imports sin uso; performance con `assertTimeout(Duration.ofMillis(200), ...)` | Leer tests |
| H-12 | Javadoc del test del validador: V(G) = 7 y 7 caminos con la tabla del spec 10 §4 | Leer el Javadoc |
| H-13 | `TaskMapper` con `applyIfPresent` | Leer `TaskMapper` |
| H-14 | Sin contraseñas por defecto en `application-prod.yml`; compose lee de `.env`; `.env.example` actualizado | `git grep -n "password" -- src/main/resources docker-compose.yml` |
| H-15 | README con `--build`, estructura actual, conteo real de tests, sección de análisis estático y endpoints F1–F3 | Leer README; contar tests reales en `build/test-results/test/*.xml` |

### V-09 — Funcionalidades nuevas (spec 09)

| # | Verificación | Cómo |
|---|---|---|
| F0.1 | `ClockConfig` con zona configurable; ningún `LocalDate.now()`/`LocalDateTime.now()` **sin** `Clock` en `src/main` | `git grep -nE "LocalDate(Time)?\.now\(\)" -- src/main` debe dar vacío |
| F0.2 | `TaskStatus.isTerminal()` y `activeStatuses()`; valores del enum sin renombrar | Leer el enum |
| F1.1 | Migración `V2__Add_due_date_to_tasks.sql` con columna e índice; `V1` **no fue modificada** | `git log --follow -p src/main/resources/db/migration/V1__Create_tasks_table.sql` (cambiar una migración aplicada rompe Flyway en prod) |
| F1.2 | `dueDate` en request/response; `urgency` en `TaskResponse` y **no** persistida | Leer DTOs y entidad |
| F1.3 | `TaskDueDateValidator` aplica R7 (crear) y R8 (solo si `dueDate` cambia en `PUT`) | Leer service + tests |
| F1.4 | Evaluador final = Chain of Responsibility: interfaz `UrgencyRule`, 4 reglas con `@Order`, evaluador sin `if` | Leer `service/urgency` |
| F1.5 | Ventanas desde `UrgencyProperties` (`HIGH=3, MEDIUM=2, LOW=1`), no hardcodeadas | `git grep -n "app.urgency" src/main/resources` |
| F1.6 | Tests de bordes: vence hoy, último día de ventana, día siguiente, cada prioridad, terminales | Leer `TaskUrgencyEvaluatorTest` |
| F2.1 | `JpaSpecificationExecutor`; finders `findByStatus/...` eliminados; `TaskSpecifications` con constructor privado | Leer repositorio |
| F2.2 | `TaskSearchCriteria` record con `isDueRangeValid()`; `@Valid @ParameterObject` en el controller | Leer |
| F2.3 | `/api/tasks/search` devuelve `PageResponse` (no `PageImpl`); lista blanca de `sort`; `sort=password` → 400; `dueFrom>dueTo` → 400 | Tests MockMvc |
| F3.1 | `/api/tasks/summary` con todas las claves de enum, `overdue`, `completionRate` con 2 decimales y sin división por cero | Tests unitarios + integración |
| F3.2 | Conteos por consultas agregadas (proyecciones), **no** `findAll()` en memoria | `git grep -n "findAll" -- src/main/java/**/TaskSummaryServiceImpl.java` vacío |
| F3.3 | `/summary` no choca con `/{id}` | Test MockMvc existente |
| F.4 | Swagger documenta los endpoints nuevos | Levantar la app y `curl -s localhost:8080/v3/api-docs \| grep -E "/api/tasks/search\|/api/tasks/summary"` |

### V-10 / V-11 — Complejidad (specs 10, 11)

| # | Verificación | Cómo |
|---|---|---|
| 10.1 | Ningún método de producción con ciclomática > 10 | Script de §4.1 sobre el XML de JaCoCo |
| 10.2 | Valores de la tabla del spec 10 §5 coinciden con los medidos (JaCoCo/Sonar); diferencias explicadas | Comparar con `docs/analisis-estatico/informe-metricas.md` |
| 11.1 | Ningún método con cognitiva > 10 | Sonar API (§4.2); si no hay acceso, revisión manual de métodos con ciclomática ≥ 5 aplicando las reglas del spec 11 §2 |
| 11.2 | Commits A (ingenuo) y B (refactor) del caso de estudio en el historial, en ese orden, y B no modificó los tests | `git log --oneline --all -- src/main/java/com/example/taskmanager/service/urgency`; `git diff <A> <B> --stat -- src/test` debe estar vacío |
| 11.3 | No queda código de la versión ingenua en `main` | Leer `TaskUrgencyEvaluator` |
| 11.4 | Issues `java:S3776` y `java:S1541` abiertos = 0 | Sonar API (§4.2) |

### V-TEST — Calidad de la suite de pruebas (aplica a todo)

| # | Verificación | Cómo |
|---|---|---|
| T.1 | Todos los tests con `// Arrange`, `// Act`, `// Assert` (o `// Act & Assert`) | Script: para cada `@Test`/`@ParameterizedTest`, verificar los comentarios en el cuerpo |
| T.2 | Nombres en inglés + `@DisplayName` en español Given/When/Then en los tests nuevos | Revisión |
| T.3 | Ningún test sin aserciones; ningún test que solo pruebe un mock | Revisión |
| T.4 | Independencia: los tests pasan en orden aleatorio | Ejecutar una vez con `junit.jupiter.testmethod.order.default=org.junit.jupiter.api.MethodOrderer$Random` (`./gradlew test -Djunit.jupiter.testmethod.order.default=...` o vía `systemProperty` temporal) y revertir la configuración |
| T.5 | Datos variados (paradoja del pesticida) | Revisión de `@MethodSource` y fixtures |
| T.6 | Tests dependientes de fecha usan `Clock.fixed` | `git grep -n "LocalDate.now()" -- src/test` vacío o justificado |

### V-STYLE — Código limpio y convenciones (aplica a todo `src/`)

```bash
git grep -nE "^import .*\.\*;" -- 'src/**/*.java'          # wildcard imports → vacío
git grep -nE "@Autowired" -- src/main                       # field injection → vacío
git grep -nE "System\.(out|err)\.print|printStackTrace" -- src/main   # → vacío
git grep -nE "Collectors\.toList\(\)" -- src                # → vacío
git grep -nE "@Data" -- src/main/java/com/example/taskmanager/model   # → vacío
git grep -nE "TODO|FIXME" -- src                           # justificar o resolver
```

Además revisar: ninguna entidad expuesta en controllers, sin lógica de negocio en controllers, sin literales duplicados, métodos ≤ 40 líneas, ≤ 5 parámetros, formato Google Java Style.

---

## 4. Scripts de apoyo

### 4.1 Métodos con alta complejidad ciclomática (JaCoCo XML)

Guárdalo como `build/tmp/complexity.py` (no se commitea):

```python
import xml.etree.ElementTree as ET

THRESHOLD = 10
root = ET.parse("build/reports/jacoco/test/jacocoTestReport.xml").getroot()
rows = []
for cls in root.iter("class"):
    for m in cls.iter("method"):
        for c in m.iter("counter"):
            if c.get("type") == "COMPLEXITY":
                cc = int(c.get("missed")) + int(c.get("covered"))
                rows.append((cc, cls.get("name").replace("/", "."), m.get("name")))
rows.sort(reverse=True)
print("Top 15 por complejidad ciclomática:")
for cc, cls, name in rows[:15]:
    flag = "  <-- SUPERA UMBRAL" if cc > THRESHOLD else ""
    print(f"{cc:>3}  {cls}.{name}{flag}")
```

```bash
python3 build/tmp/complexity.py
```

### 4.2 Consultas a SonarCloud (proyecto público)

```bash
KEY=<projectKey>
curl -s "https://sonarcloud.io/api/qualitygates/project_status?projectKey=$KEY"
curl -s "https://sonarcloud.io/api/measures/component?component=$KEY&metricKeys=coverage,complexity,cognitive_complexity,duplicated_lines_density,code_smells,bugs,vulnerabilities"
curl -s "https://sonarcloud.io/api/issues/search?componentKeys=$KEY&rules=java:S3776,java:S1541&resolved=false"
curl -s "https://sonarcloud.io/api/measures/component_tree?component=$KEY&metricKeys=complexity,cognitive_complexity&qualifiers=FIL&ps=100"
```

Si el shell no tiene salida a `sonarcloud.io`, marca ⚠️ y deja estos comandos en el reporte para que el usuario los ejecute.

---

## 5. Verificación final

Después de todas las correcciones:

```bash
./gradlew clean build --console=plain                 # tests + jacocoTestCoverageVerification
python3 build/tmp/complexity.py                       # ningún método > 10
```

Y, si hay acceso, ejecuta el análisis (`SONAR_TOKEN` en el entorno) o haz push de la rama y abre un PR para que el workflow corra. Confirma el Quality Gate en `OK`.

Compara contra la línea base de §1.4: los tests no pueden ser menos (salvo eliminaciones justificadas de tests inútiles) y la cobertura no puede bajar.

---

## 6. Reglas al corregir (obligatorias)

### 6.1 Prohibido "hacer trampa" para que todo pase

- **No** bajar umbrales de JaCoCo, del Quality Gate ni del Quality Profile.
- **No** desactivar reglas, agregar `@SuppressWarnings`, `//NOSONAR`, `@Disabled` ni excluir clases con lógica de la cobertura para ocultar un problema.
- **No** debilitar aserciones ni borrar tests que fallan: si un test falla, determina si el error está en el código o en el test, y corrige el que esté mal.
- **No** modificar migraciones Flyway ya existentes (`V1`, `V2`): los cambios de esquema van en una migración nueva.

### 6.2 Respetar el diseño existente

- Arquitectura por capas, DTO Pattern, constructor injection, `Clock` inyectado, patrones definidos (Strategy en transiciones, Chain of Responsibility en urgencia, Specification en búsqueda).
- No cambiar reglas de negocio (specs 01 y 09) ni el contrato de los endpoints sin documentarlo.
- No renombrar los valores de `TaskStatus` / `TaskPriority`.
- No agregar dependencias nuevas salvo que un spec las pida.

### 6.3 Calidad de cada cambio

- Cada corrección de bug va acompañada de un test que falle antes y pase después.
- Cambio mínimo y de raíz; nada de refactors masivos no pedidos.
- Commits atómicos con el ID del ítem.

### 6.4 Si un spec tiene un error

Si encuentras que un spec pide algo técnicamente incorrecto (API inexistente, regla de Sonar renombrada, versión incompatible), **no lo implementes a ciegas**: aplica la alternativa correcta más cercana a la intención del spec, y regístralo en el reporte en la sección "Desviaciones respecto a los specs" con la razón.

### 6.5 Cuándo detenerte y preguntar

- Un cambio requeriría borrar datos, reescribir historial de Git (`push --force`, `rebase` de commits publicados) o tocar secretos.
- Dos specs se contradicen y no está claro cuál debe prevalecer.
- Corregir algo exige cambiar una regla de negocio.

---

## 7. Entregable: reporte de verificación

Crear `docs/analisis-estatico/reporte-verificacion.md` con esta estructura:

```markdown
# Reporte de verificación — <fecha>

## Resumen
| Estado | Cantidad |
|---|---|
| ✅ OK | … |
| 🔧 Corregido | … |
| ⬆️ Mejorado | … |
| ❌→✅ Implementado (faltaba) | … |
| ⚠️ Pendiente manual | … |

## Línea base vs. final
| Métrica | Antes | Después |
|---|---|---|
| Tests totales / fallidos | | |
| Cobertura global (JaCoCo) | | |
| Cobertura de ramas `service.validation` / `service.urgency` | | |
| Máx. complejidad ciclomática por método | | |
| Quality Gate (Sonar) | | |
| Issues abiertos (bugs / vulnerabilidades / code smells) | | |

## Detalle por ítem
| ID | Estado | Hallazgo | Acción | Commit |
|---|---|---|---|---|
| H-06 | 🔧 | El handler genérico devolvía `ex.getMessage()` | Mensaje fijo + log con stacktrace | `abc1234` |

## Desviaciones respecto a los specs
…

## Pendientes manuales para el usuario
1. … (con pasos exactos en la UI de SonarCloud)

## Evidencias faltantes (spec 04 §6)
…
```

Actualiza también `docs/analisis-estatico/informe-metricas.md` si los valores medidos cambiaron con tus correcciones.

---

## 8. Definición de "hecho"

1. Todos los ítems de §3 están en ✅, 🔧, ⬆️ o ⚠️ (ninguno en ❌).
2. `./gradlew clean build` pasa, sin bajar umbrales ni deshabilitar tests.
3. Ningún método de producción supera complejidad ciclomática 10 ni cognitiva 10.
4. Quality Gate en `OK` (o ⚠️ con instrucciones si no hay acceso).
5. `reporte-verificacion.md` completo y commiteado en la rama `chore/verificacion-calidad`, lista para PR a `main`.
