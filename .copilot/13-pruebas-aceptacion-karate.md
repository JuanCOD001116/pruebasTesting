# 13 — Pruebas de Aceptación con Karate (API end-to-end + CI + reportes)

> Agrega la **capa de aceptación** a la pirámide de pruebas del proyecto **Task Manager API**. Las pruebas existentes (unitarias, integración con MockMvc, transición de estados, caminos, rendimiento) **no se tocan**. Respeta todas las convenciones de `project-planning.md` y de los specs `00`–`12`.

## 1. Objetivo

1. Escribir las reglas de negocio acordadas en el proyecto como **escenarios ejecutables** en Gherkin, legibles por personas que no programan.
2. Ejecutarlos con **peticiones HTTP reales** contra la aplicación levantada (puerto aleatorio, H2 en memoria, migraciones Flyway reales).
3. Generar **reportes HTML, JUnit XML y Cucumber JSON**.
4. Integrarlos al **pipeline de GitHub Actions**: corren en cada push y PR, bloquean el merge si fallan y publican los reportes como artefacto.
5. Mantener intactos el Quality Gate y las métricas de SonarCloud del taller de análisis estático.

## 2. Decisiones de diseño (obligatorias)

| Decisión | Motivo |
|---|---|
| Karate corre **dentro de un test de Spring Boot** (`@SpringBootTest(webEnvironment = RANDOM_PORT)`) | Un solo comando levanta la app y ejecuta las pruebas. No hace falta Docker ni un servidor externo en CI |
| **Tarea Gradle separada** `acceptanceTest` (tag JUnit `acceptance`) | Las pruebas unitarias siguen siendo rápidas; la aceptación se ejecuta como etapa propia del pipeline |
| **Reloj fijo** (`Clock`) en `2030-01-15` zona `America/Bogota` solo durante la aceptación | Las reglas de fecha límite y urgencia deben ser deterministas. El proyecto ya inyecta `Clock` en toda la lógica de fechas (spec 09 §2.1) |
| Cada escenario **crea sus propios datos** | Independencia entre escenarios (F.I.R.S.T). No se depende del orden de ejecución |
| Ejecución **secuencial** (1 hilo) | El resumen estadístico (F3) compara conteos antes y después; con hilos en paralelo serían inestables |
| La cobertura de aceptación **no se envía a SonarCloud** | La métrica del Quality Gate sigue midiendo solo las pruebas unitarias y de integración, como en el taller. Así la aceptación no "infla" la cobertura |
| **Sin código de producción nuevo** | Solo se agregan archivos de prueba, configuración de build y CI. Si un escenario revela un bug, se corrige con su test unitario correspondiente (spec 12 §6.3) |

## 3. Dependencias y build (`build.gradle`)

Usar la **última versión 1.5.x publicada** de `io.karatelabs:karate-junit5` en Maven Central (requiere Java 17, compatible con el proyecto).

```groovy
dependencies {
    // ...existentes
    testImplementation 'io.karatelabs:karate-junit5:1.5.1'
}

sourceSets {
    test {
        resources {
            // Karate busca los .feature en el classpath de pruebas
            srcDir 'src/test/java'
            exclude '**/*.java'
        }
    }
}

tasks.named('test', Test) {
    useJUnitPlatform {
        excludeTags 'acceptance'
    }
    // ...configuración existente (finalizedBy jacocoTestReport, etc.)
}

tasks.register('acceptanceTest', Test) {
    description = 'Pruebas de aceptación end-to-end con Karate.'
    group = 'verification'
    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath
    useJUnitPlatform {
        includeTags 'acceptance'
    }
    shouldRunAfter tasks.named('test')
    systemProperty 'karate.options', System.getProperty('karate.options', '')
    outputs.dir(layout.buildDirectory.dir('karate-reports'))
    testLogging {
        events 'failed'
        exceptionFormat 'full'
    }
}

tasks.named('check') {
    dependsOn tasks.named('acceptanceTest')
}
```

Reglas:

- `sonar.coverage.jacoco.xmlReportPaths` **sigue apuntando solo** a `jacocoTestReport.xml` (spec 05). No crear un reporte JaCoCo combinado.
- `jacocoTestReport` y `jacocoTestCoverageVerification` **siguen dependiendo solo de `test`**. Verificar que la tarea `acceptanceTest` no sobrescriba `build/jacoco/test.exec`: JaCoCo genera por defecto `build/jacoco/acceptanceTest.exec`, que es lo correcto.
- Si Karate trae versiones de librerías que chocan con las del BOM de Spring Boot (fallos al arrancar el contexto), resolverlo con `exclude` en la dependencia, documentando cuál y por qué. **No** bajar la versión de Spring Boot.

## 4. Estructura de archivos

```
src/test/java/com/example/taskmanager/acceptance/
├── AcceptanceTestRunner.java        ← levanta la app y ejecuta Karate
├── FixedClockTestConfig.java        ← Clock fijo para la aceptación
├── karate-config.js                 ← configuración global (baseUrl, fechas)
├── common/
│   ├── create-task.feature          ← helper reutilizable (@ignore)
│   ├── change-status.feature        ← helper reutilizable (@ignore)
│   └── schemas.json                 ← esquemas de respuesta
├── tasks/
│   ├── crud.feature
│   └── validation.feature
├── status/
│   └── status-transitions.feature
├── duedate/
│   ├── due-date.feature
│   └── urgency.feature
├── search/
│   └── search.feature
├── summary/
│   └── summary.feature
└── platform/
    ├── errors.feature
    └── health-and-docs.feature
```

Los `.feature` viven junto al runner, en `src/test/java`. Es la convención recomendada por Karate y la configuración de `sourceSets` de §3 los copia al classpath.

## 5. Infraestructura de prueba

### 5.1 Reloj fijo

```java
package com.example.taskmanager.acceptance;

@TestConfiguration
class FixedClockTestConfig {

    static final ZoneId ZONE = ZoneId.of("America/Bogota");
    static final LocalDate TODAY = LocalDate.of(2030, 1, 15);

    @Bean
    @Primary
    Clock acceptanceClock() {
        return Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
    }
}
```

- `@Primary` hace que este `Clock` gane frente al de `ClockConfig` sin activar la sobrescritura de beans.
- Verificar que **todos** los consumidores del reloj (validador de fecha límite, evaluador de urgencia, especificación `overdue`, resumen, auditoría JPA) lo reciben por tipo (`Clock`) y no por nombre de bean. Si alguno lo recibe por nombre, es un problema de diseño: corregirlo en ese componente.

### 5.2 Runner

```java
package com.example.taskmanager.acceptance;

@Tag("acceptance")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Import(FixedClockTestConfig.class)
class AcceptanceTestRunner {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Dado la API en ejecución, cuando se ejecutan los escenarios de aceptación, entonces todos pasan")
    void allAcceptanceScenariosPass() {
        // Arrange
        Runner.Builder<?> builder = Runner.path("classpath:com/example/taskmanager/acceptance")
                .tags("~@ignore")
                .systemProperty("karate.port", String.valueOf(port))
                .systemProperty("karate.today", FixedClockTestConfig.TODAY.toString())
                .outputHtmlReport(true)
                .outputJunitXml(true)
                .outputCucumberJson(true)
                .reportDir("build/karate-reports");

        // Act
        Results results = builder.parallel(1);

        // Assert
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}
```

- Un solo `@Test` que falla si **cualquier** escenario falla; el detalle queda en el reporte de Karate.
- `karate.options` (definido en Gradle) permite filtrar por tags desde la línea de comandos sin tocar código, por ejemplo `-Dkarate.options="--tags @smoke"`. Si `Runner.Builder` no la toma automáticamente en la versión usada, leerla con `System.getProperty` y aplicarla con `.tags(...)`.
- El perfil `dev` usa H2 en memoria con Flyway (spec 08 H-03), así que se prueban las migraciones reales.

### 5.3 `karate-config.js`

```javascript
function fn() {
  var port = karate.properties['karate.port'] || '8080';
  var today = karate.properties['karate.today'];
  var LocalDate = Java.type('java.time.LocalDate');
  var base = LocalDate.parse(today);

  var config = {
    baseUrl: 'http://localhost:' + port,
    apiUrl: 'http://localhost:' + port + '/api/tasks',
    today: today,
    plusDays: function (n) { return base.plusDays(n).toString(); },
    uniqueTitle: function (prefix) { return prefix + ' ' + java.util.UUID.randomUUID(); }
  };

  karate.configure('connectTimeout', 5000);
  karate.configure('readTimeout', 10000);
  karate.configure('logPrettyRequest', true);
  karate.configure('logPrettyResponse', true);
  return config;
}
```

`uniqueTitle` evita choques entre escenarios en la búsqueda por texto.

### 5.4 Helpers reutilizables

`common/create-task.feature`:

```gherkin
@ignore
Feature: Helper — crear una tarea

  Scenario:
    * def defaults = { title: '#(uniqueTitle("Tarea"))', priority: 'MEDIUM' }
    * def body = karate.merge(defaults, __arg)
    Given url apiUrl
    And request body
    When method post
    Then status 201
    * def task = response
```

Uso: `* def created = call read('classpath:com/example/taskmanager/acceptance/common/create-task.feature') { priority: 'HIGH', dueDate: '#(plusDays(2))' }` y luego `created.task.id`.

`common/change-status.feature` hace lo mismo con `PATCH /api/tasks/{id}/status`. Recibe `id`, `status` y opcionalmente `expectedStatus` (por defecto 200).

### 5.5 Esquemas (`common/schemas.json`)

Antes de escribirlos, **leer** `TaskResponse`, `ErrorResponse`, `PageResponse` y `TaskSummaryResponse` y usar sus campos reales. Ejemplo orientativo:

```json
{
  "task": {
    "id": "#number",
    "title": "#string",
    "description": "##string",
    "status": "#regex (PENDING|IN_PROGRESS|DONE|CANCELLED)",
    "priority": "#regex (LOW|MEDIUM|HIGH)",
    "assignedTo": "##string",
    "dueDate": "##regex \\d{4}-\\d{2}-\\d{2}",
    "urgency": "#regex (NONE|ON_TRACK|AT_RISK|OVERDUE)",
    "createdAt": "#string",
    "updatedAt": "#string"
  },
  "error": {
    "status": "#number",
    "message": "#string"
  }
}
```

`##` significa "opcional o nulo" en Karate. Ajustar los nombres y agregar los campos que falten (`timestamp`, `path`, `error`…).

## 6. Catálogo de escenarios

Cada escenario lleva **tags**:

- Por funcionalidad: `@crud`, `@validation`, `@status`, `@duedate`, `@urgency`, `@search`, `@summary`, `@errors`, `@platform`.
- `@smoke` en los críticos (un subconjunto rápido para verificar que la API está viva).
- La **regla de negocio** que valida, como `@R1`… `@R9`, o el hallazgo, como `@H-09`.

Estilo: títulos en español, en forma de comportamiento ("Una tarea cancelada no se puede reabrir"), Given/When/Then explícitos, y `Scenario Outline` con `Examples` para variar datos (paradoja del pesticida).

### 6.1 `tasks/crud.feature` — CRUD

| # | Escenario | Esperado | Tags |
|---|---|---|---|
| A-01 | Crear una tarea con datos mínimos | 201, `status = PENDING`, coincide con el esquema `task` | `@smoke @crud` |
| A-02 | Crear con todos los campos (descripción, prioridad, responsable, fecha límite) | 201, todos los valores devueltos iguales a los enviados | `@crud` |
| A-03 | Consultar por id una tarea recién creada | 200, mismo contenido | `@smoke @crud` |
| A-04 | Consultar un id inexistente | 404 con cuerpo de error estandarizado | `@crud @errors` |
| A-05 | Actualizar título y prioridad con `PUT` | 200 y cambio persistido (verificado con un `GET` posterior) | `@crud` |
| A-06 | `PUT` parcial: solo se envía la descripción | Los demás campos no cambian | `@crud` |
| A-07 | Eliminar una tarea | 204; un `GET` posterior devuelve 404 | `@crud` |
| A-08 | Eliminar un id inexistente | 404 | `@crud @errors` |
| A-09 | Listar tareas (`GET /api/tasks`) | 200, arreglo que incluye la tarea creada | `@crud` |

### 6.2 `tasks/validation.feature` — Validación de entrada

| # | Escenario | Esperado | Tags |
|---|---|---|---|
| A-10 | Outline: crear sin título, con título vacío o con título de solo espacios | 400 | `@validation @R1` |
| A-11 | Título de 256 caracteres | 400 (`@Size max 255`) | `@validation` |
| A-12 | Descripción de 1001 caracteres | 400 | `@validation` |
| A-13 | `PUT` con título en blanco | 400 | `@validation @H-09` |
| A-14 | Outline: prioridad inexistente en el body (`URGENT`) / estado inexistente en `PATCH` (`ARCHIVED`) | 400, no 500 | `@validation @H-06` |
| A-15 | JSON mal formado | 400, no 500 | `@validation @H-06` |

### 6.3 `status/status-transitions.feature` — Máquina de estados

| # | Escenario | Esperado | Tags |
|---|---|---|---|
| A-16 | Outline **transiciones válidas**: `PENDING→IN_PROGRESS` (con responsable), `PENDING→CANCELLED`, `IN_PROGRESS→PENDING`, `IN_PROGRESS→DONE`, `IN_PROGRESS→CANCELLED` | 200 y el estado nuevo persistido | `@smoke @status` |
| A-17 | Outline **transiciones inválidas**: `PENDING→DONE`, `DONE→PENDING`, `DONE→IN_PROGRESS`, `DONE→CANCELLED`, `CANCELLED→PENDING`, `CANCELLED→IN_PROGRESS`, `CANCELLED→DONE` | 400 y el estado **no cambia** (verificado con `GET`) | `@status` |
| A-18 | Outline mismo estado: `PENDING→PENDING`, `IN_PROGRESS→IN_PROGRESS` | 400 | `@status` |
| A-19 | `PENDING→IN_PROGRESS` sin responsable | 400 | `@status` |
| A-20 | `PENDING→IN_PROGRESS` con responsable en blanco (`"   "`) | 400 | `@status` |
| A-21 | Ciclo de vida completo: crear → iniciar → volver a pendiente → iniciar → terminar | Todos 200; estado final `DONE` y urgencia `NONE` | `@status @urgency` |
| A-22 | Cambiar el estado de un id inexistente | 404 | `@status @errors` |

Para llegar a los estados de origen `IN_PROGRESS`, `DONE` y `CANCELLED`, usar el helper `change-status.feature` con transiciones válidas. Nunca se insertan datos directamente en la base.

Las reglas de transición deben coincidir **exactamente** con el spec 01 y con `ALLOWED_TRANSITIONS` del validador. Si encuentras una diferencia, detente y repórtala (spec 12 §6.5).

### 6.4 `duedate/due-date.feature` — Fecha límite (R7, R8)

Fecha fija del sistema: **2030-01-15** (§5.1).

| # | Escenario | Esperado | Tags |
|---|---|---|---|
| A-23 | Crear con fecha límite de ayer (`plusDays(-1)`) | 400 | `@duedate @R7` |
| A-24 | Outline crear con fecha límite hoy / mañana / dentro de 30 días | 201 | `@duedate @R7` |
| A-25 | Crear sin fecha límite | 201 y `dueDate` nulo | `@duedate` |
| A-26 | `PUT` que cambia la fecha límite a una pasada | 400 | `@duedate @R8` |
| A-27 | `PUT` que cambia la fecha límite a una futura | 200 | `@duedate @R8` |

### 6.5 `duedate/urgency.feature` — Urgencia calculada (R9)

Ventanas: `HIGH = 3`, `MEDIUM = 2`, `LOW = 1` días (`UrgencyProperties`).

| # | Escenario (Outline) | Prioridad | Fecha límite | Esperado | Tags |
|---|---|---|---|---|---|
| A-28 | Sin fecha límite, prioridad alta | HIGH | — | `AT_RISK` | `@urgency` |
| A-29 | Sin fecha límite, prioridad media o baja | MEDIUM / LOW | — | `ON_TRACK` | `@urgency` |
| A-30 | Vence hoy | HIGH / MEDIUM / LOW | +0 | `AT_RISK` | `@urgency` |
| A-31 | Vence en el **último día** de la ventana | HIGH +3 / MEDIUM +2 / LOW +1 | — | `AT_RISK` | `@urgency` |
| A-32 | Vence el **día siguiente** a la ventana | HIGH +4 / MEDIUM +3 / LOW +2 | — | `ON_TRACK` | `@urgency` |
| A-33 | Tarea terminada o cancelada con fecha límite cercana | HIGH, +1 | — | `NONE` | `@urgency` |
| A-34 | La urgencia aparece en `GET /{id}`, en el listado y en la búsqueda paginada | — | — | Presente y consistente en las tres respuestas | `@urgency` |

**Limitación documentada — `OVERDUE`:** con la API no se puede crear ni actualizar una tarea con fecha pasada (R7, R8), y el reloj está fijo, así que el estado `OVERDUE` y el filtro `overdue=true` no son alcanzables por HTTP. Esa rama ya está cubierta por las pruebas unitarias (`TaskUrgencyEvaluatorTest`, `OverdueRule`, tests de `TaskSpecifications`). Agregar un escenario A-35 que verifique la parte alcanzable: `GET /api/tasks?overdue=true` responde 200 y **no** contiene las tareas creadas en la ejecución. Escribir esta limitación en un comentario al inicio del `.feature` y en el informe. **No** agregar endpoints ni puertas traseras de prueba en el código de producción para poder "viajar en el tiempo".

### 6.6 `search/search.feature` — Búsqueda paginada (F2)

Cada escenario crea un grupo de tareas con un prefijo único (`uniqueTitle`) y filtra por `text=<prefijo>`, para aislarse de los datos de otros escenarios.

| # | Escenario | Esperado | Tags |
|---|---|---|---|
| A-36 | Outline filtro único: por estado, por prioridad, por responsable sin distinguir mayúsculas, por texto sin distinguir mayúsculas | Solo devuelve las tareas que cumplen | `@search` |
| A-37 | Filtros combinados: estado + prioridad + texto | Intersección correcta | `@search` |
| A-38 | Rango de fecha límite (`dueFrom`, `dueTo`, inclusivos) | Incluye los extremos | `@search` |
| A-39 | Paginación: 5 tareas, `size=2` | `totalElements = 5`, `totalPages = 3`; la página 2 tiene 1 elemento | `@smoke @search` |
| A-40 | Ordenamiento `sort=priority,desc` y `sort=dueDate,asc` | Orden correcto | `@search` |
| A-41 | Ordenamiento por propiedad no permitida (`sort=password`) | 400 | `@search @errors` |
| A-42 | `dueFrom` posterior a `dueTo` | 400 | `@search @errors` |
| A-43 | `status=FOO` en la consulta | 400, no 500 | `@search @errors @H-06` |
| A-44 | `size=500` | El tamaño de página aplicado no supera 100 | `@search` |
| A-45 | La respuesta paginada cumple el esquema (`content`, `page`, `size`, `totalElements`, `totalPages`) y **no** expone campos internos de `PageImpl` (`pageable`, `sort`…) | Esquema exacto | `@search` |

### 6.7 `summary/summary.feature` — Resumen estadístico (F3)

Como la base es compartida durante la ejecución, el resumen se valida por **diferencias**: se lee antes, se crean datos y se lee después.

| # | Escenario | Esperado | Tags |
|---|---|---|---|
| A-46 | El resumen tiene **todas** las claves de `TaskStatus` y `TaskPriority` | Claves completas aunque el conteo sea 0 | `@smoke @summary` |
| A-47 | Crear 2 HIGH PENDING y 1 LOW DONE | `total` +3, `byStatus.PENDING` +2, `byStatus.DONE` +1, `byPriority.HIGH` +2, `byPriority.LOW` +1 | `@summary` |
| A-48 | `completionRate` | Número con a lo sumo 2 decimales, entre 0 y 100, y consistente con la fórmula `DONE / (total − CANCELLED) × 100` aplicada a los valores del propio resumen | `@summary` |
| A-49 | `GET /api/tasks/summary` no se confunde con `GET /api/tasks/{id}` | 200 con el esquema del resumen, no 400 ni 404 | `@summary` |

### 6.8 `platform/errors.feature` y `platform/health-and-docs.feature`

| # | Escenario | Esperado | Tags |
|---|---|---|---|
| A-50 | Outline: todos los errores (400, 404) tienen el mismo formato | Coinciden con el esquema `error` | `@errors @H-06` |
| A-51 | Un error nunca expone detalles internos | El cuerpo no contiene `Exception`, `at com.example`, `org.hibernate` ni `SQL` | `@errors @H-06` |
| A-52 | `GET /actuator/health` | 200 y `status = UP` | `@smoke @platform @H-04` |
| A-53 | Actuator solo expone `health` | `GET /actuator/env` y `GET /actuator/beans` → 404 | `@platform @H-04` |
| A-54 | `GET /v3/api-docs` documenta los endpoints | Contiene `/api/tasks`, `/api/tasks/search`, `/api/tasks/summary` y `/api/tasks/{id}/status` | `@platform` |

**Total: 54 escenarios definidos.** Con los `Scenario Outline` se ejecutan aproximadamente **90 casos** en el reporte.

### 6.9 Ejemplo completo de referencia (`status/status-transitions.feature`)

```gherkin
@status
Feature: Máquina de estados de una tarea
  Como usuario del gestor de tareas
  quiero que el sistema solo permita cambios de estado válidos
  para que el ciclo de vida de una tarea sea consistente.

  Background:
    * url apiUrl
    * def createTask = read('classpath:com/example/taskmanager/acceptance/common/create-task.feature')
    * def changeStatus = read('classpath:com/example/taskmanager/acceptance/common/change-status.feature')

  @smoke
  Scenario Outline: Transición permitida de <desde> a <hacia>
    Given def created = call createTask { assignedTo: 'ana' }
    And def id = created.task.id
    And call changeStatus { id: '#(id)', path: <ruta> }
    When path id, 'status'
    And request { status: '<hacia>' }
    And method patch
    Then status 200
    And match response.status == '<hacia>'

    Examples:
      | desde       | hacia       | ruta                |
      | PENDING     | IN_PROGRESS | []                  |
      | PENDING     | CANCELLED   | []                  |
      | IN_PROGRESS | PENDING     | ['IN_PROGRESS']     |
      | IN_PROGRESS | DONE        | ['IN_PROGRESS']     |
      | IN_PROGRESS | CANCELLED   | ['IN_PROGRESS']     |

  Scenario Outline: Transición prohibida de <desde> a <hacia>
    Given def created = call createTask { assignedTo: 'ana' }
    And def id = created.task.id
    And call changeStatus { id: '#(id)', path: <ruta> }
    When path id, 'status'
    And request { status: '<hacia>' }
    And method patch
    Then status 400
    And match response contains { message: '#string' }
    Given path id
    When method get
    Then match response.status == '<desde>'

    Examples:
      | desde     | hacia       | ruta                     |
      | PENDING   | DONE        | []                       |
      | DONE      | PENDING     | ['IN_PROGRESS', 'DONE']  |
      | DONE      | IN_PROGRESS | ['IN_PROGRESS', 'DONE']  |
      | DONE      | CANCELLED   | ['IN_PROGRESS', 'DONE']  |
      | CANCELLED | PENDING     | ['CANCELLED']            |
      | CANCELLED | IN_PROGRESS | ['CANCELLED']            |
      | CANCELLED | DONE        | ['CANCELLED']            |
```

En este ejemplo `change-status.feature` recibe `path`, una lista de estados que se recorren en orden con transiciones válidas para llegar al estado de origen. Ajustar el helper a ese contrato.

## 7. Integración continua (`.github/workflows/sonarcloud.yml`)

Modificar el workflow existente. **No** crear uno nuevo, para que exista un solo pipeline y un solo check de build.

```yaml
      - name: Build, unit & integration tests
        run: ./gradlew clean build -x acceptanceTest

      - name: Acceptance tests (Karate)
        run: ./gradlew acceptanceTest

      - name: Publish Karate summary
        if: always()
        run: |
          if [ -f build/karate-reports/karate-summary-json.txt ]; then
            echo "## Pruebas de aceptación (Karate)" >> "$GITHUB_STEP_SUMMARY"
            python3 - <<'EOF' >> "$GITHUB_STEP_SUMMARY"
          import json
          d = json.load(open('build/karate-reports/karate-summary-json.txt'))
          print(f"| Features | Escenarios | Pasaron | Fallaron |")
          print(f"|---|---|---|---|")
          print(f"| {d.get('featuresPassed',0)+d.get('featuresFailed',0)} | {d.get('scenariosPassed',0)+d.get('scenariosfailed',0)} | {d.get('scenariosPassed',0)} | {d.get('scenariosfailed',0)} |")
          EOF
          fi

      - name: Upload Karate reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: karate-reports
          path: build/karate-reports/
          retention-days: 14

      - name: SonarCloud analysis
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: ./gradlew sonar
```

Reglas:

- **Orden:** build y pruebas unitarias → aceptación → Sonar. Si la aceptación falla, el job queda en rojo y el PR no se puede mergear. Sonar no se ejecuta, lo cual es correcto: no se analiza un build roto.
- Los nombres exactos de los campos del resumen JSON de Karate dependen de la versión. **Verificarlos** abriendo `karate-summary-json.txt` tras una ejecución local y ajustar el script. Si el archivo no existe, el paso no debe fallar.
- Usar la versión mayor vigente de `actions/upload-artifact` (y aprovechar para revisar los avisos de Node 20 pendientes del spec 12).
- `sonar` ya no ejecuta `build`, pero **sí** necesita `jacocoTestReport`, que se mantiene por el `dependsOn` del spec 05.

## 8. Reportes

| Reporte | Ruta | Uso |
|---|---|---|
| **HTML resumen** | `build/karate-reports/karate-summary.html` | Evidencia visual: features, escenarios, pasos, tiempos |
| HTML por feature | `build/karate-reports/*.html` | Detalle de cada escenario con petición y respuesta |
| JUnit XML | `build/karate-reports/*.xml` | Integración con CI |
| Cucumber JSON | `build/karate-reports/*.json` | Compatible con herramientas de reporte de Cucumber |
| Reporte Gradle de la tarea | `build/reports/tests/acceptanceTest/index.html` | Resultado del runner JUnit |
| Artefacto en GitHub | Pestaña *Actions* → ejecución → *Artifacts* → `karate-reports` | Descargar los reportes de CI |

Abrir el reporte local en Windows: `start build\karate-reports\karate-summary.html`.

## 9. Relación con SonarCloud y el Quality Gate

- El runner Java y los helpers son código de prueba; Sonar no los cuenta como código de producción. Los `.feature` y `.js` de prueba no deben generar issues; si `karate-config.js` genera alguno, corregirlo (no excluirlo).
- La cobertura del gate **no cambia**: sigue calculándose solo con `jacocoTestReport.xml` (§2).
- El PR de esta funcionalidad debe cumplir el `TaskManager Gate` como cualquier otro: 0 issues nuevos.

## 10. Comandos

```bat
:: Solo pruebas unitarias e integración (rápidas)
gradlew.bat test

:: Solo aceptación
gradlew.bat acceptanceTest

:: Solo un grupo de escenarios por tag
gradlew.bat acceptanceTest -Dkarate.options="--tags @smoke"
gradlew.bat acceptanceTest -Dkarate.options="--tags @status"

:: Todo (check incluye acceptanceTest)
gradlew.bat clean build

:: Abrir el reporte
start build\karate-reports\karate-summary.html
```

## 11. Evidencias para el informe

Guardarlas en `docs/pruebas-aceptacion/evidencias/`:

| Archivo | Qué debe mostrar |
|---|---|
| `01-karate-summary.png` | `karate-summary.html` con la ruta visible en la barra del navegador: features, escenarios pasados y tiempos |
| `02-karate-feature-detalle.png` | Un feature abierto (`status-transitions`) con los escenarios en verde |
| `03-karate-request-response.png` | Detalle de un paso: petición y respuesta JSON |
| `04-karate-escenario-fallido.png` | Un escenario fallando a propósito en local (ver §12.3) y cómo lo muestra el reporte. **No** se commitea el cambio |
| `05-actions-acceptance-step.png` | El pipeline con el paso *Acceptance tests (Karate)* en verde |
| `06-actions-job-summary.png` | El resumen de Karate en el *Job Summary* de GitHub |
| `07-actions-artifact.png` | El artefacto `karate-reports` disponible para descargar |
| `08-pr-checks-passed.png` | El PR con todos los checks en verde y el Quality Gate en *Passed* |

## 12. Procedimiento de implementación (para el agente)

1. Crear la rama `test/karate-acceptance` desde `main` actualizado.
2. **Commit 1** — `build(karate): agregar dependencia, tarea acceptanceTest y runner`. Incluye §3, §5.1, §5.2, §5.3 y un feature mínimo (A-52, salud). Verificar con `gradlew.bat acceptanceTest` que la app arranca y el escenario pasa.
3. **Commit 2 en adelante** — un commit por feature, en el orden de §6: `test(karate): escenarios CRUD`, `test(karate): validación de entrada`… Ejecutar la tarea después de cada uno.
4. **Si un escenario falla:**
   - Si el escenario contradice un spec, el error está en el escenario: corrígelo.
   - Si el escenario sigue el spec y la API no lo cumple, es un **bug real**. Escribe primero un test unitario o de integración que lo reproduzca, corrige el código de producción en un commit `fix(...)` y vuelve a ejecutar. Registra el bug en el reporte final (§14).
   - **Nunca** marques un escenario con `@ignore` para que pase.
5. **Prueba del mecanismo de fallo (solo local):** cambia temporalmente un `Then status 400` por `Then status 200` en A-17, ejecuta, toma la captura 04 y **revierte** el cambio (`git checkout -- <archivo>`). Verifica con `git status` que no quedó nada pendiente.
6. **Commit final** — `ci(karate): ejecutar aceptación en el pipeline y publicar reportes` (§7).
7. Ejecutar `gradlew.bat clean build` completo y **no hacer push**: el usuario revisa y sube.

## 13. Reglas (además de spec 12 §6)

- **No** modificar ni eliminar pruebas existentes.
- **No** agregar código de producción solo para facilitar las pruebas (endpoints de prueba, perfiles con lógica especial, cambiar fechas por API).
- **No** usar `Thread.sleep` ni esperas fijas. La app está lista cuando arranca el `@SpringBootTest`.
- **No** depender del orden de los escenarios ni de datos creados por otro escenario.
- **No** poner datos sensibles en los `.feature`.
- Los `Scenario Outline` deben cubrir todos los valores relevantes del dominio (todas las prioridades, todos los estados), no solo uno.

## 14. Definición de "hecho"

1. Los 54 escenarios de §6 implementados y en verde; ningún `@ignore` salvo en los helpers.
2. `gradlew.bat clean build` pasa (incluye `acceptanceTest`) y `gradlew.bat test` sigue sin ejecutar Karate.
3. Los reportes de §8 se generan en local y se publican como artefacto en CI.
4. El pipeline ejecuta la aceptación antes de Sonar y queda en rojo si un escenario falla.
5. La cobertura reportada en SonarCloud es la misma que antes de este cambio (±1 %), y el Quality Gate está en *Passed*.
6. Limitación de `OVERDUE` documentada en el feature.
7. Un reporte corto en `docs/pruebas-aceptacion/resumen.md` con: número de features, escenarios y casos ejecutados; tiempo total; bugs encontrados (si los hubo) con su commit de corrección; y desviaciones respecto a este spec.
