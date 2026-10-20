# Informe de Métricas de Análisis Estático de Código

> **Proyecto:** Task Manager API (`com.example.taskmanager`)  
> **Repositorio:** [JuanCOD001116/pruebasTesting](https://github.com/JuanCOD001116/pruebasTesting)  
> **Organización SonarCloud:** `juancod001116`  
> **Project Key:** `JuanCOD001116_pruebasTesting`  
> **Fecha:** Octubre 2026  
> **Estándar:** Taller de Análisis Estático de Código — Specs 04, 05, 06, 07, 08, 09, 10 y 11  

---

## 1. Introducción y Herramientas

El análisis estático de código es una práctica de ingeniería de software orientada a evaluar la calidad interna, mantenibilidad, confiabilidad y seguridad de un sistema sin necesidad de ejecutarlo. En este proyecto, se implementó un pipeline automatizado de análisis estático con el objetivo de prevenir la degradación de la arquitectura por capas, controlar la complejidad estructural y cognitiva, e imponer el principio **Clean as You Code**.

### 1.1 Ecosistema de Herramientas

| Herramienta | Versión | Rol en el Pipeline |
|---|---|---|
| **SonarCloud** | SaaS (Plan Free OSS) | Plataforma centralizada de análisis de código estático, gestión de Quality Profiles, evaluación de Quality Gates y auditoría de Security Hotspots. |
| **JaCoCo** | `0.8.12` | Agente de cobertura de código para Java en tiempo de ejecución de pruebas unitarias e integración (`jacocoTestReport` y `jacocoTestCoverageVerification`). |
| **GitHub Actions** | Hosted Runners (`ubuntu-latest`) | Orquestador de Integración Continua (CI). Ejecuta `./gradlew clean build sonar` en cada `push` a ramas principales y en cada `pull_request`. |
| **Gradle** | `8.12` (Plugin `org.sonarqube: 7.5.0.8588`) | Build tool que compila, ejecuta la suite de pruebas JUnit 5, recolecta binarios de instrumentación `.exec` y transmite el reporte XML a SonarCloud. |

---

## 2. Reporte de Cobertura Local con JaCoCo

El reporte local de cobertura se genera durante la tarea `./gradlew test` o `./gradlew build` a través del plugin oficial `jacoco`.

### 2.1 Rutas de los Artefactos Generados

- **Reporte Visual Navegable (HTML):**  
  `build/reports/jacoco/test/html/index.html`  
  Permite una inspección jerárquica a nivel de proyecto, paquete, clase, método y línea de código fuente.
- **Reporte para Motores de CI y Sonar (XML):**  
  `build/reports/jacoco/test/jacocoTestReport.xml`  
  Documento estructurado consumido por SonarCloud mediante la propiedad `sonar.coverage.jacoco.xmlReportPaths`.
- **Binario de Ejecución:**  
  `build/jacoco/test.exec`  
  Volcado binario crudo generado por la JVM que registra las instrucciones de bytecode y ramas cubiertas durante la sesión de pruebas.

### 2.2 Guía de Lectura del Reporte Local

Al abrir `index.html` en un navegador web:
1. **Métrica de Instrucciones (Instruction Coverage):** Mide la cantidad de bytecode de Java ejecutado frente al total. Proporciona una métrica objetiva independiente del formateo visual del código.
2. **Métrica de Ramas (Branch Coverage):** Mide la cobertura en estructuras condicionales (`if`, `switch`, ternarios). Se representa en el código fuente con rombos coloreados al costado izquierdo:
   - 🟢 **Rombo verde:** Todas las bifurcaciones de la decisión fueron evaluadas (100% de caminos cubiertos).
   - 🟡 **Rombo amarillo:** Cobertura parcial (se ejecutó la condición verdadera pero nunca la falsa, o viceversa).
   - 🔴 **Rombo rojo:** Ninguna rama de la decisión fue ejercitada.
3. **Métrica de Complejidad (Cxty):** Refleja la complejidad ciclomática de McCabe $V(G)$ por método y clase, permitiendo contrastar la cantidad de pruebas implementadas contra los caminos teóricos independientes.
4. **Exclusiones Configuradas:** Mediante `build.gradle` y `lombok.config` (`lombok.addLombokGeneratedAnnotation = true`), se excluyen del reporte las clases de configuración, DTOs, excepciones simples y el código generado automáticamente (getters, setters, builders).

---

## 3. Quality Profile: "TaskManager Java Way"

Para garantizar un estándar riguroso superior al perfil genérico, se creó el perfil **TaskManager Java Way** para el lenguaje Java en la organización de SonarCloud.

### 3.1 Herencia de Perfiles

- **Perfil Padre:** `Sonar way core` (perfil por defecto de SonarCloud para proyectos Java).
- **Herencia:** Extiende todas las reglas activas de *Sonar way core* e introduce personalizaciones y reglas adicionales específicas para el dominio de Task Manager API.

### 3.2 Tabla de Reglas Personalizadas y Activadas

La siguiente tabla refleja la configuración exacta exportada en `quality/sonar/taskmanager-java-way.xml`:

| Clave de Regla | Nombre / Propósito | Parámetros Configurados | Severidad | Justificación |
|---|---|---|---|---|
| **`java:S3776`** | Cognitive Complexity of methods | `Threshold = 10` | Critical | Límite estricto de entendimiento cognitivo. Evita anidamientos profundos y bifurcaciones complejas. |
| **`java:S1541`** | Cyclomatic Complexity of methods | `Threshold = 10` | Critical | Límite de caminos de ejecución por método. Asegura alta testeabilidad y bajo acoplamiento. |
| **`java:S107`** | Methods should not have too many parameters | `max = 5`<br>`constructorMax = 7` | Major | Previene firmas infladas. `constructorMax = 7` acomoda la inyección de dependencias de Spring. |
| **`java:S134`** | Control flow statements "if, for, while, switch" should not be nested too deeply | `max = 3` | Critical | Prohíbe anidamientos de control de flujo mayores a 3 niveles. |
| **`java:S1067`** | Expressions should not be too complex | `max = 3` | Critical | Limita el número de operadores lógicos condicionales consecutivos en una sola expresión. |
| **`java:S138`** | Methods should not have too many lines of code | `max = 40` | Major | Fuerza la modularización y el principio de responsabilidad única (SRP). |
| **`java:S2208`** | Wildcard imports should not be used | Activada por defecto | Critical | Evita colisiones de nombres y ambigüedad en imports (`import java.util.*`). |
| **`java:S6204`** | Stream.toList() should be used instead of Collectors.toList() | Activada | Major | Impone modismos modernos e inmutables de Java 16+. |
| **`java:S6813`** | Field dependency injection should not be used | Activada | Major | Prohíbe `@Autowired` en atributos; exige inyección por constructor. |
| **`java:S1192`** | String literals should not be duplicated | `threshold = 3`<br>`minimalLength = 5` | Critical | Obliga a extraer constantes reutilizables para mensajes repetidos. |
| **`java:S2699`** | Tests should include assertions | Activada | Blocker | Garantiza que ningún método `@Test` carezca de aserciones válidas (evita pruebas superficiales). |
| **`java:S1128`** | Unnecessary imports should be removed | Activada | Minor | Imports sin uso (spec 06 §4.2). Activada en la verificación final. |
| **`java:S1068`** | Unused "private" fields should be removed | Activada | Major | Campos privados muertos (spec 06 §4.2). Activada en la verificación final. |
| **`java:S5778`** | Only one method invocation is expected when testing runtime exceptions | Activada | Major | Lambdas de `assertThrows` limpias (spec 06 §4.3). Activada en la verificación final. |
| **`java:S5786`** | JUnit5 test classes and methods should have default package visibility | Activada | Info | Convención JUnit 5 (spec 06 §4.3). Activada en la verificación final. |

Total de reglas activas en el perfil: **321** (317 antes de la verificación final).

---

## 4. Quality Gate: "TaskManager Gate"

El **Quality Gate** es la barrera de control que determina si una versión de código puede ser aceptada e integrada en la rama principal.

### 4.1 Condiciones del Gate

#### A. Condiciones sobre Código Nuevo (New Code)

| Métrica | Condición | Umbral | Justificación |
|---|---|---|---|
| **Issues / Violaciones** (`new_violations`) | is greater than | `> 0` | Tolerancia cero a nuevos code smells, bugs o vulnerabilidades. |
| **Cobertura** (`new_coverage`) | is less than | `< 80.0%` | Garantiza que todo código agregado esté probado rigurosamente. |
| **Líneas Duplicadas** (`new_duplicated_lines_density`) | is greater than | `> 3.0%` | Evita la replicación de bloques de código en nuevas clases. |
| **Security Hotspots Revisados** (`new_security_hotspots_reviewed`) | is less than | `< 100%` | Todo hotspot nuevo debe ser auditado antes de mergear. |
| **Maintainability Rating** (`new_maintainability_rating`) | is worse than | `peor que A` | Deuda técnica nula en el incremento analizado. |
| **Reliability Rating** (`new_reliability_rating`) | is worse than | `peor que A` | Ningún bug nuevo introducido. |
| **Security Rating** (`new_security_rating`) | is worse than | `peor que A` | Cero vulnerabilidades de seguridad nuevas. |
| **Security Review Rating** (`new_security_review_rating`) | is worse than | `peor que A` | Evaluación integral de la postura de seguridad nueva. |

#### B. Condiciones sobre Código Global (Overall Code)

| Métrica | Condición | Umbral | Justificación |
|---|---|---|---|
| **Cobertura Global** (`coverage`) | is less than | `< 75.0%` | Piso base de cobertura para todo el repositorio. |
| **Reliability Rating Global** (`reliability_rating`) | is worse than | `peor que B` | Previene la permanencia de defectos mayores en el core. |
| **Security Rating Global** (`security_rating`) | is worse than | `peor que A` | Cero vulnerabilidades toleradas en toda la base de código. |

### 4.2 Relación Técnica: Quality Profile → Issue → Condición `Issues > 0` → Gate

La complejidad ciclomática y cognitiva no se configuran como un número escalar plano en el Quality Gate. Su control estricto se logra a través de una cadena de responsabilidad automatizada:

```
[1. Quality Profile: "TaskManager Java Way"]
     Reglas java:S3776 (Cognitive Complexity > 10) y java:S1541 (Cyclomatic Complexity > 10) activas.
          ↓
[2. Análisis de Código en PR o Commit]
     Si un método nuevo o modificado excede 10 puntos, SonarCloud levanta un Issue.
          ↓
[3. Evaluación del Quality Gate]
     La condición 'new_violations > 0' detecta de inmediato el issue reportado.
     Simultáneamente, 'new_maintainability_rating' decae a B o peor.
          ↓
[4. Resultado en CI/CD]
     El Quality Gate transiciona a FAILED y el pipeline en GitHub Actions falla (exit code 1),
     bloqueando la integración en main hasta refactorizar el código.
```

---

## 5. Complejidad Ciclomática (Spec 10)

La complejidad ciclomática de McCabe $V(G)$ representa el número de caminos linealmente independientes a través del grafo de flujo de control de un método:

$$V(G) = e - n + 2p = \text{puntos de decisión} + 1$$

Indica el número mínimo de casos de prueba requeridos para alcanzar cobertura completa de caminos independientes.

### 5.1 Caso Detallado: `TaskStatusTransitionValidator.validate`

El método que gobierna las transiciones de estado de una tarea (`TaskStatusTransitionValidator.validate`) posee la siguiente estructura lógica:

```java
// Punto de decisión 1: Estados idénticos
if (currentStatus == newStatus) { ... }                              // d1: if (+1)

// Punto de decisión 2: Transición no permitida en matriz
if (!ALLOWED.getOrDefault(...).contains(newStatus)) { ... }          // d2: if (+1)

// Puntos de decisión 3, 4, 5, 6: Obligatoriedad de asignación
if (currentStatus == PENDING                                         // d3: if (+1)
        && newStatus == IN_PROGRESS                                  // d4: && (+1)
        && (assignedTo == null                                       // d5: && (+1)
            || assignedTo.isBlank())) { ... }                        // d6: || (+1)
```

- **Puntos de decisión:** 3 `if` + 2 `&&` + 1 `||` = **6**.
- **Complejidad del método:** $V(G) = 6 + 1 = \mathbf{7}$.
- **Complejidad total del archivo en Sonar:** `validate` (7) + método privado `invalid` (1) = **8**.

#### Grafo y Tabla de Caminos Independientes de McCabe

| Camino | d1 | d2 | d3 | d4 | d5 | d6 | Resultado | Caso de Prueba Asociado |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|---|---|
| **1** | **V** | – | – | – | – | – | Rechaza (mismo estado) | `TC-01` (`validate_SameStatus_ThrowsException`) |
| **2** | **F** | **V** | – | – | – | – | Rechaza (transición inválida) | `TC-04` (`validate_CancelledToPending_ThrowsException`) |
| **3** | **F** | **F** | **V** | **V** | **V** | – | Rechaza (`assignedTo` es null) | `TC-03` (`validate_PendingToInProgress_NullAssignee_ThrowsException`) |
| **4** | **F** | **F** | **V** | **V** | **F** | **V** | Rechaza (`assignedTo` en blanco) | `TC-18` (`validate_PendingToInProgress_BlankAssignee_ThrowsException`) |
| **5** | **F** | **F** | **V** | **V** | **F** | **F** | Acepta (`PENDING` a `IN_PROGRESS` con usuario) | `TC-02` (`validate_PendingToInProgress_Valid_Success`) |
| **6** | **F** | **F** | **V** | **F** | – | – | Acepta (otra transición válida, ej. `PENDING` a `CANCELLED`) | `TC-05` (`validate_PendingToCancelled_Success`) |
| **7** | **F** | **F** | **F** | – | – | – | Acepta (`IN_PROGRESS` a `DONE`) | `TC-06` (`validate_InProgressToDone_Success`) |

*Nota: V = Verdadero, F = Falso, – = No evaluado por cortocircuito de operadores lógicos.*

### 5.2 Inventario de Métodos y Archivos (Antes vs. Después)

| Archivo / Componente | V(G) Línea Base (522e931) | V(G) Main Final (ad6ab04) | Observación del Cambio |
|---|:---:|:---:|---|
| `TaskController` | 10 | 8 | Se eliminó la cadena `if/else if` de filtros delegando en `TaskSearchCriteria`. |
| `TaskMapper` | 8 | 5 | Refactor con helper `applyIfPresent` (H-13). |
| `TaskServiceImpl` | 16 | 14 | Modularización con `findTaskOrThrow` y separación de `TaskSummaryService`. |
| `TaskStatusTransitionValidator` | 8 | 8 | Coincide con el cálculo manual de 7 (`validate`) + 1 (`invalid`). |
| `GlobalExceptionHandler` | 5 | 11 | Aumenta por los nuevos handlers HTTP 400 requeridos por spec 08 (H-06). |
| `TaskSpecifications` (nuevo) | — | 29 | Descompuesto en especificaciones atómicas de $V(G) \le 2$ cada una. |
| `TaskUrgencyEvaluator` (Commit A vs B) | 9 (versión ingenua) | 4 (versión refactorizada) | Refactor a Chain of Responsibility. |

### 5.3 Valores medidos por método tras la verificación final (spec 12)

Medidos con JaCoCo (columna *Cxty* y ramas) sobre la rama `chore/verificacion-final`. Sonar no
expone la complejidad por método; la regla `java:S1541` (umbral 10) no tiene issues abiertos.

| Método | V(G) spec 10 §5 (manual) | V(G) JaCoCo | Ramas cubiertas | Comentario |
|---|:---:|:---:|:---:|---|
| `TaskController.getAllTasks` | 1 | 1 | — | Delegación a `findTasks(criteria)`. |
| `TaskMapper.toEntity(Update, Task)` | 1 (+ helper 2) | 1 (+ `applyIfPresent` 2) | 2/2 | Coincide. |
| `TaskServiceImpl.updateTask` | 3 | **3** | 4/4 | Antes medía 4: faltaba extraer `resolveAssignee` (corregido). |
| `TaskServiceImpl.resolveAssignee` | — | 2 | 2/2 | Nuevo (extraído de `updateTask`). |
| `TaskServiceImpl.deleteTask` | 2 | 2 | 2/2 | Coincide. |
| `TaskStatusTransitionValidator.validate` | 7 | 7 | 12/12 | Coincide con el cálculo de §5.1. |
| `TaskUrgencyEvaluator.evaluate` | 1 | 1 | — | Ya no tiene constructor secundario. |
| `TerminalStatusRule` / `OverdueRule` / `DueSoonRule` | 2 | 2 | 2/2 | Una prueba unitaria por regla. |
| `NoDueDateRule` | 3 | 3 | 4/4 | Guard clause + ternario. |
| `TaskSpecifications.hasX(...)` | 2 | 2 | 2/2 | `overdue` mide 4 (guarda `null` + `if`). |
| `TaskSummaryServiceImpl.completionRate` | 2 | **2** | 2/2 | Antes estaba dentro de `getSummary` (V(G) 4); se extrajo. `getSummary` queda en 3 (2 `for` + 1). |

Máxima complejidad ciclomática por método en producción: **7** (`validate`). Cobertura JaCoCo
final: 99.9 % de instrucciones y 97.8 % de ramas (164 pruebas).

### 5.4 Cambios de métricas en el cierre del proyecto

| Métrica | Tras spec 12 (PR #3) | Tras Karate (PR #4, `b7786e1`) | Cierre final | Qué cambió y por qué |
|---|:---:|:---:|:---:|---|
| Pruebas unitarias/integración | 164 | 168 | 168 | +4 de `TaskSearchSortingIntegrationTest` (bug de ordenamiento hallado por Karate, `d40e615`). |
| Casos de aceptación (Karate) | — | 86 | 86 | Capa nueva del spec 13; no aporta cobertura a Sonar. |
| Cobertura JaCoCo instr. / ramas | 99.9 % / 97.8 % | 99.9 % / 98.0 % | 99.9 % / 98.0 % | Ramas 92 → 98: las 6 nuevas de `toOrder` (ternario) y `sortKey` (`if` + `for`), todas cubiertas. |
| Instrucciones medidas por JaCoCo | 1 417 | 1 510 | 1 444 | Los 7 constructores de inyección vuelven a ser de Lombok (`e0ebf4d`) y JaCoCo excluye el código `@Generated`; el porcentaje no cambia. |
| Complejidad ciclomática total (Sonar, `main`) | 142 | 151 | ≈ 144 (el análisis de un PR solo mide los archivos cambiados; el total se confirma en `main` tras el merge) | +9 por el ordenamiento nuevo (`orderedBy`, `toOrder`, `sortKey`) del PR #4; −7 al eliminar los 7 constructores escritos a mano (cada uno suma 1 en Sonar). |
| Máx. V(G) por método | 7 | 7 | 7 | Sin cambios (`TaskStatusTransitionValidator.validate`). |

---

## 6. Complejidad Cognitiva (Spec 11) y Caso de Estudio

La **complejidad cognitiva** mide el esfuerzo mental necesario para que un desarrollador comprenda el flujo de control de un método. A diferencia de la ciclomática, penaliza severamente el **anidamiento** y las rupturas de linealidad.

### 6.1 Ciclomática vs. Cognitiva

| Aspecto | Complejidad Ciclomática $V(G)$ | Complejidad Cognitiva |
|---|---|---|
| **Pregunta clave** | ¿Cuántos casos de prueba se necesitan? | ¿Qué tan difícil es comprender el código? |
| **Estructuras `else / else if`** | No suman (0 puntos) | Suman incremento (+1) |
| **Anidamiento** | No penaliza el nivel | Multiplica el costo (+ nivel actual de nesting) |
| **Secuencias `a && b && c`** | Suma por cada operador (+2) | Suma por secuencia de operadores iguales (+1) |
| **Estructuras `switch`** | Suma por cada `case` | Suma un único punto por todo el `switch` |

### 6.2 Caso de Estudio: `TaskUrgencyEvaluator`

Para validar empíricamente la efectividad del estándar y la detección en CI, se ejecutó un caso de estudio en dos etapas en el Pull Request #2.

#### Etapa 1: Commit A — Implementación Ingenua (`fbc0df1`)
Se implementó `TaskUrgencyEvaluator.evaluate` mediante condicionales `if/else` profundamente anidados:

```java
public TaskUrgency evaluate(Task task) {
    LocalDate today = LocalDate.now(clock);
    if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CANCELLED) {   // +1 (if), +1 (||)
        return TaskUrgency.NONE;
    } else {                                                                                   // +1 (else)
        if (task.getDueDate() == null) {                                                       // +2 (if, nesting 1)
            if (task.getPriority() == TaskPriority.HIGH) {                                     // +3 (if, nesting 2)
                return TaskUrgency.AT_RISK;
            } else {                                                                           // +1 (else)
                return TaskUrgency.ON_TRACK;
            }
        } else {                                                                               // +1 (else)
            if (task.getDueDate().isBefore(today)) {                                           // +3 (if, nesting 2)
                return TaskUrgency.OVERDUE;
            } else if (!task.getDueDate().isAfter(today.plusDays(windowFor(task.getPriority())))) { // +1 (else if)
                return TaskUrgency.AT_RISK;
            } else {                                                                           // +1 (else)
                return TaskUrgency.ON_TRACK;
            }
        }
    }
}
```

- **Incrementos reportados por SonarCloud:**  
  `+1, +1, +1, +2 (nesting 1), +3 (nesting 2), +1, +1, +3 (nesting 2), +1, +1 = 15`.
- **Resultado del análisis:**  
  - Complejidad Cognitiva: **15** (Supera el umbral de 10).
  - Issue generado: `java:S3776` (*"Refactor this method to reduce its Cognitive Complexity from 15 to the 10 allowed"*).
  - Estado del Quality Gate: **FAILED** (2/4 checks fallidos en el PR).

#### Etapa 2: Commit B — Refactorización con Chain of Responsibility (`b31c43a`)
Se desacopló la lógica en la interfaz `UrgencyRule` y cuatro componentes ordenados con `@Order`, dejando a `TaskUrgencyEvaluator` como un despachador lineal funcional:

```java
public TaskUrgency evaluate(Task task) {
    LocalDate today = LocalDate.now(clock);
    return rules.stream()
            .map(rule -> rule.evaluate(task, today))
            .flatMap(Optional::stream)
            .findFirst()
            .orElse(TaskUrgency.ON_TRACK);
}
```

- **Desglose de Complejidad tras el Refactor:**

| Componente | Complejidad Cognitiva | Complejidad Ciclomática | Regla Aplicada |
|---|:---:|:---:|---|
| `TaskUrgencyEvaluator.evaluate` | **0** | 1 | Despachador funcional con Stream |
| `TerminalStatusRule` | **1** | 2 | `task.getStatus().isTerminal()` |
| `NoDueDateRule` | **2** | 3 | Guard clause + operador ternario plano |
| `OverdueRule` | **1** | 2 | Condicional plano `isBefore(today)` |
| `DueSoonRule` | **1** | 2 | Evaluación con ventana de `UrgencyProperties` |

- **Resultado del análisis:**  
  - Complejidad Cognitiva en `TaskUrgencyEvaluator`: **0**.
  - Ninguna regla supera complejidad cognitiva 2 (máximo permitido en el diseño: 2).
  - Totales del PR: Cognitiva disminuyó de 40 a 30; ciclomática redistribuida de 81 a 86.
  - Issue `java:S3776`: **Cerrado**.
  - Estado del Quality Gate: **PASSED** (4/4 checks en verde).
  - **Integridad de pruebas:** Ningún archivo de `src/test` fue modificado, comprobando que el refactor preservó al 100% el comportamiento funcional.

---

## 7. Historial y Evolución de los Análisis

A lo largo del taller se completaron 6 hitos formales de análisis registrados en la pestaña **Activity** de SonarCloud:

| # | Hito / Evento | Commit | Perfil Utilizado | Cobertura | Issues | Ciclomática | Cognitiva | Líneas | Estado Gate |
|---|---|:---:|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **1** | Análisis Inicial Automático | `9813bb8` | Sonar way core | 0.0% | 0 | 53 | 20 | 606 | PASSED (sin reglas custom) |
| **2** | Línea Base (Fase 0) | `522e931` | TaskManager Java Way | 73.3% | 3 | 53 | 20 | 606 | **FAILED** (`coverage < 75%`) |
| **3** | Versión 1.0.0 en main | `893d42f` | TaskManager Java Way | 93.9% | 0 | 109 | 41 | 979 | **PASSED** ("Green (was Red)") |
| **4** | PR #2 — Commit A (Caso Estudio) | `fbc0df1` | TaskManager Java Way | 100% (New) | 1 | 9 (file) | 15 (file) | +542 | **FAILED** (`java:S3776` en evaluate) |
| **5** | PR #2 — Commit B (Refactor) | `b31c43a` | TaskManager Java Way | 100% (New) | 0 | 86 (PR) | 30 (PR) | +132 | **PASSED** (0 issues, 4/4 checks) |
| **6** | Merge de PR #2 en main | `ad6ab04` | TaskManager Java Way | **95.9%** | **0** | **142** | **55** | **1,215** | **PASSED** |

### 7.1 Comparativa por Archivo (Línea Base `522e931` vs. Main `893d42f`)

Formato de métricas: `Ciclomática / Cognitiva / Cobertura`

```
- TaskController:                10 / 5 / 46.2%   ──>    8 / 0 / 62.5%
- TaskMapper:                     8 / 5 / 81.1%   ──>    5 / 1 / 100.0%
- TaskServiceImpl:               16 / 5 / 94.3%   ──>   14 / 5 / 94.8%
- TaskStatusTransitionValidator:  8 / 5 / 100.0%  ──>    8 / 5 / 100.0%
- GlobalExceptionHandler:         5 / 0 / 25.6%   ──>   11 / 3 / 91.4%
- TaskSpecifications (Nuevo F2):        —         ──>   29 / 13 / 94.7%
```

---

## 8. Desviaciones Respecto a los Specs

Durante el despliegue del pipeline y la auditoría de calidad se presentaron las siguientes desviaciones técnicas justificadas:

1. **Nombre del Perfil Base de Java:**  
   En la versión actual de SonarCloud el perfil por defecto se denomina `Sonar way core` (el spec 06 lo referenciaba como `Sonar way`).
2. **Ausencia de `java:S3776` en el perfil base:**  
   `Sonar way core` no mantiene activa la regla de complejidad cognitiva `java:S3776`. Sin la creación del perfil propio `TaskManager Java Way`, el issue del caso de estudio nunca se habría detectado.
3. **Corrección de regla de pruebas unitarias (`S2698` vs `S2699`):**  
   Inicialmente se activó por error `java:S2698` (que exige aserciones específicas de ciertas librerías), disparando 69 issues falsos positivos. Se corrigió activando `java:S2699` (aserciones genéricas presentes en el test), reduciendo los hallazgos reales de la línea base a 3.
4. **Condición de Duplicación en el Gate:**  
   Se eliminó la condición absoluta `new_duplicated_lines > 3`, introducida por error tipográfico, conservando la condición porcentual estándar `new_duplicated_lines_density > 3.0%`.
5. **Recreación de Organización bajo Plan Free OSS:**  
   La organización inicial bajo el plan Free genérico no permitía la asignación de Quality Profiles ni Quality Gates personalizados como predeterminados. Se recreó bajo el plan Free para proyectos de código abierto (Open Source), desbloqueando la vinculación de perfiles propios.
6. **Simulación de Código Nuevo mediante Pull Request:**  
   Debido a que SonarCloud calcula el *New Code* basado en la fecha de los commits y no en la fecha del análisis, re-analizar commits antiguos en `main` no creaba una ventana de código nuevo adecuada. El ciclo deliberado **Failed → Passed** se ejecutó mediante el Pull Request #2.
7. **Versiones de Framework y Plugins:**  
   El proyecto nunca pasó a Spring Boot 3.4: `build.gradle` siempre declaró `3.3.0` (verificado en todo el historial). En la verificación final se actualizó al último parche de la línea que fijan los specs, `3.3.13` (correcciones de seguridad de Spring Framework 6.1.21 y Tomcat 10.1.42), y el plugin `org.sonarqube` a `7.5.0.8588`, la última versión publicada que pide el spec 05 §5.2.
8. **Compatibilidad de Constructores con IDEs y Regla `S107`:**  
   `Task` y los DTO conservan `@NoArgsConstructor`/`@AllArgsConstructor` de Lombok: escribir a mano el constructor de 9 parámetros de `Task` violaría `java:S107` (`constructorMax = 7`), mientras que el código generado por Lombok no se analiza. En el cierre del proyecto los 7 componentes inyectados (controller, handler, servicios, `TaskUrgencyEvaluator`, `DueSoonRule`, `TaskDueDateValidator`) volvieron a `@RequiredArgsConstructor` (`e0ebf4d`): el falso positivo del IDE que motivó `3a0a002` se resuelve por configuración de usuario (README), y así todo el proyecto sigue la convención Lombok del spec 09. Lombok dejó de fijarse en `1.18.36`: lo gestiona el BOM de Spring Boot (`1.18.38` con 3.3.13), como exige H-10.
9. **Constructor Secundario de Compatibilidad en `TaskUrgencyEvaluator` (resuelto):**  
   Durante el Commit B, `TaskUrgencyEvaluator` tuvo un constructor secundario que instanciaba internamente las 4 reglas para no modificar `src/test/`. Cerrado el caso de estudio, se eliminó: el evaluador depende solo de `List<UrgencyRule>` y los tests construyen la lista explícitamente; un test con `ApplicationContextRunner` verifica que Spring respeta el orden de `@Order`.

---

## 9. Conclusiones

1. **Eficacia del Principio Clean as You Code:**  
   Configurar condiciones inflexibles sobre código nuevo (`new_violations > 0` y `new_coverage < 80%`) evitó la entrada de deuda técnica y garantizó que cada incremento aumentara la cobertura global de 73.3% a 95.9%.
2. **Desacoplamiento mediante Patrones de Diseño:**  
   La sustitución de estructuras condicionales anidadas por el patrón **Chain of Responsibility** (`TaskUrgencyEvaluator`) demostró cómo la arquitectura impacta directamente las métricas de calidad: la complejidad cognitiva bajó de 15 a 0 sin perder cobertura funcional.
3. **Complementariedad entre Métricas:**  
   La complejidad ciclomática guió el diseño de casos de prueba en las transiciones de estado ($V(G) = 7 \implies 7\text{ pruebas independientes}$), mientras que la complejidad cognitiva guió la refactorización para la legibilidad humana.
4. **Automatización Determinista:**  
   El uso de un reloj inyectable (`Clock.fixed`), la verificación en CI con GitHub Actions y el bloqueo automático ante violaciones de Quality Gate consolidan un estándar industrial reproducible.

---

## 10. Tabla de Evidencias

A continuación se listan las 11 evidencias documentales correspondientes al spec 04 §6:

| # | Archivo de Evidencia | Qué Demuestra | Estado / Enlace |
|:---:|---|---|:---:|
| **01** | `01-jacoco-local-index.png` | `build/reports/jacoco/test/html/index.html` abierto en navegador con ruta visible | [01-jacoco-local-index.png](evidencias/01-jacoco-local-index.png) (pendiente) |
| **02** | `02-jacoco-validator-ramas.png` | Detalle de `TaskStatusTransitionValidator` en JaCoCo con rombos de rama en verde | [02-jacoco-validator-ramas.png](evidencias/02-jacoco-validator-ramas.png) (pendiente) |
| **03** | `03-sonar-overview-linea-base.png` | Overview de SonarCloud del análisis de Línea Base en commit `522e931` (Gate Failed) | [03-sonar-overview-linea-base.png](evidencias/03-sonar-overview-linea-base.png) (pendiente) |
| **04** | `04-quality-profile-reglas.png` | Perfil *TaskManager Java Way* con reglas personalizadas y parámetros | [04-quality-profile-reglas.png](evidencias/04-quality-profile-reglas.png) (pendiente) |
| **05** | `05-quality-profile-changelog.png` | Changelog del perfil mostrando las reglas añadidas sobre *Sonar way core* | [05-quality-profile-changelog.png](evidencias/05-quality-profile-changelog.png) (pendiente) |
| **06** | `06-quality-gate-condiciones.png` | Condiciones configuradas en el Quality Gate *TaskManager Gate* | [06-quality-gate-condiciones.png](evidencias/06-quality-gate-condiciones.png) (pendiente) |
| **07** | `07-quality-gate-failed.png` | Quality Gate en **Failed** en el PR #2 tras el Commit A ingenuo | [07-quality-gate-failed.png](evidencias/07-quality-gate-failed.png) (pendiente) |
| **08** | `08-issue-s3776.png` | Detalle del issue `java:S3776` (Cognitive Complexity 15 > 10) en `TaskUrgencyEvaluator` | [08-issue-s3776.png](evidencias/08-issue-s3776.png) (pendiente) |
| **09** | `09-quality-gate-passed.png` | Quality Gate en **Passed** en el PR #2 tras el Commit B refactorizado | [09-quality-gate-passed.png](evidencias/09-quality-gate-passed.png) (pendiente) |
| **10** | `10-measures-complexity.png` | Pestaña *Measures → Complexity* en SonarCloud mostrando ciclomática y cognitiva | [10-measures-complexity.png](evidencias/10-measures-complexity.png) (pendiente) |
| **11** | `11-activity-historial.png` | Pestaña *Activity* exhibiendo los 6 análisis y la transición histórica de métricas | [11-activity-historial.png](evidencias/11-activity-historial.png) (pendiente) |

*Nota: Las capturas pueden ser depositadas directamente en el directorio `docs/analisis-estatico/evidencias/` para su visualización vinculada.*
