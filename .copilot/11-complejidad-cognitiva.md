# 11 — Complejidad Cognitiva (medición, caso de estudio y refactor)

## 1. Concepto

La **complejidad cognitiva** (definida por SonarSource) mide **qué tan difícil es para una persona entender** el flujo de un método. A diferencia de la ciclomática (spec 10), que mide caminos para probar, la cognitiva castiga lo que realmente cansa al leer: **el anidamiento** y las rupturas del flujo lineal.

## 2. Reglas de cálculo

| Tipo | Qué suma | Ejemplos |
|---|---|---|
| Incremento estructural | +1 | `if`, `else if`, `else`, `switch`, `for`, `while`, `do`, `catch`, ternario `?:`, `goto`/`break` o `continue` con etiqueta, recursión |
| Incremento por anidamiento | + nivel de anidamiento actual | un `if` dentro de otro `if` suma 1 + 1 = 2; uno más adentro suma 1 + 2 = 3 |
| Secuencias de operadores lógicos | +1 por cada **secuencia** de operadores iguales | `a && b && c` → +1; `a && b \|\| c` → +2 |
| No suma | 0 | `else` no suma anidamiento; `try`, `finally`, `default`, métodos y lambdas en sí mismos (pero sí aumentan el nivel de anidamiento de lo que contienen) |

Sonar la reporta por archivo en *Measures → Complexity → Cognitive Complexity* y por método mediante la regla `java:S3776` (umbral 10 en el perfil, spec 06).

## 3. Ciclomática vs. cognitiva (idea central del informe)

| | Ciclomática | Cognitiva |
|---|---|---|
| Pregunta | ¿Cuántos casos de prueba necesito? | ¿Cuánto cuesta entenderlo? |
| `else` | 0 | +1 |
| Anidamiento | No importa | Multiplica |
| `a && b && c` | +2 | +1 |
| `switch` con 5 `case` | +5 | +1 |

El caso de estudio de §6 muestra **dos métodos con la misma V(G) = 7 y cognitiva 5 vs 15**.

## 4. Inventario del código actual

| Método | Cálculo | Cognitiva |
|---|---|---|
| `TaskStatusTransitionValidator.validate` | `if` +1, `if` +1, `if` +1, secuencia `&&` +1, `\|\|` +1 | **5** |
| `TaskController.getAllTasks` | `if` +1, `&&` +1, `else if` +1, `else if` +1, `else` +1 | **5** |
| `TaskMapper.toEntity(Update, Task)` | 5 × `if` +1 | **5** |
| `TaskServiceImpl.updateTask` | `if` +1, `&&` +1, ternario anidado dentro del `if` +2 | **4** |
| `TaskServiceImpl.deleteTask` | `if` +1 | **1** |
| Resto (CRUD, mapper `toResponse`, handlers) | sin ramas | 0 |

Ninguno supera el umbral 10 → con el código actual **no** aparece `S3776`. Por eso el caso de estudio de §6 es necesario para demostrar la regla en acción.

## 5. Técnicas de reducción a aplicar

1. **Retornos tempranos (guard clauses)** en lugar de `if/else` anidados.
2. **Extraer método** con nombre de negocio (`requiresAssignee`, `resolveAssignee`).
3. **Polimorfismo / Chain of Responsibility / Strategy** en lugar de cadenas de condiciones (F1).
4. **Estructuras de datos** en lugar de condiciones (mapa de transiciones en el validador, `EnumSet` en `TaskStatus.isTerminal()`, `EnumMap` de ventanas de urgencia).
5. **Specification Pattern** para combinar filtros sin `if` (F2).

## 6. Caso de estudio obligatorio: `TaskUrgencyEvaluator`

### 6.1 Procedimiento

1. **Commit A (ingenuo):** implementar `TaskUrgencyEvaluator.evaluate` con la versión de §6.2 **y sus tests** (los mismos tests de F1, spec 09 §3.5). Mensaje: `feat(F1): urgency evaluator - naive version for cognitive complexity study`.
2. Push → SonarCloud reporta `java:S3776` en `evaluate` (15 > 10) → Quality Gate **Failed**. Capturar evidencias `07` y `08`.
3. **Commit B (refactor):** reemplazar por la versión Chain of Responsibility (spec 09 §3.4) **sin tocar los tests**. Que los mismos tests pasen demuestra que el refactor preserva el comportamiento.
4. Push → issue cerrado → Gate **Passed**. Capturar evidencia `09`.

Esto es honesto metodológicamente: el código "malo" solo existe en el historial como objeto de estudio; el código final es limpio.

### 6.2 Versión ingenua (Commit A)

```java
public TaskUrgency evaluate(Task task) {
    LocalDate today = LocalDate.now(clock);
    if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CANCELLED) {   // +1 if, +1 ||
        return TaskUrgency.NONE;
    } else {                                                                                   // +1
        if (task.getDueDate() == null) {                                                       // +2 (anidado 1)
            if (task.getPriority() == TaskPriority.HIGH) {                                     // +3 (anidado 2)
                return TaskUrgency.AT_RISK;
            } else {                                                                           // +1
                return TaskUrgency.ON_TRACK;
            }
        } else {                                                                               // +1
            if (task.getDueDate().isBefore(today)) {                                           // +3 (anidado 2)
                return TaskUrgency.OVERDUE;
            } else if (!task.getDueDate().isAfter(today.plusDays(windowFor(task.getPriority())))) { // +1
                return TaskUrgency.AT_RISK;
            } else {                                                                           // +1
                return TaskUrgency.ON_TRACK;
            }
        }
    }
}
```

| Métrica | Cálculo | Valor |
|---|---|---|
| Cognitiva | 1+1+1+2+3+1+1+3+1+1 | **15** |
| Ciclomática | 1 + 4 `if` + 1 `else if` + 1 `\|\|` | **7** |

### 6.3 Versión refactorizada (Commit B)

| Elemento | Cognitiva | Ciclomática |
|---|---|---|
| `TaskUrgencyEvaluator.evaluate` (stream + `findFirst`) | 0 | 1 |
| `TerminalStatusRule` (`task.getStatus().isTerminal()`) | 1 (un ternario o `if`) | 2 |
| `NoDueDateRule` (guard + ternario) | 2 | 3 |
| `OverdueRule` | 1 | 2 |
| `DueSoonRule` | 1 | 2 |

Ejemplo de una regla:

```java
@Component
@Order(2)
public class NoDueDateRule implements UrgencyRule {

    @Override
    public Optional<TaskUrgency> evaluate(Task task, LocalDate today) {
        if (task.getDueDate() != null) {
            return Optional.empty();
        }
        return Optional.of(task.getPriority() == TaskPriority.HIGH
                ? TaskUrgency.AT_RISK
                : TaskUrgency.ON_TRACK);
    }
}
```

Conclusión esperada para el informe: la complejidad **total** apenas cambia (se reparte), pero la **máxima por método** baja de 15 a 2, cada pieza se prueba aislada y agregar una regla nueva no modifica las existentes (OCP).

## 7. Inventario objetivo después de todas las fases

| Método | Cognitiva antes | Cognitiva después | Técnica |
|---|---|---|---|
| `TaskController.getAllTasks` | 5 | 0 | Specification (F2) |
| `TaskMapper.toEntity(Update, Task)` | 5 | 0 (+1 helper) | Extraer método (H-13) |
| `TaskServiceImpl.updateTask` | 4 | 2 | Extraer `resolveAssignee` |
| `TaskStatusTransitionValidator.validate` | 5 | 5 (o 3 con `requiresAssignee`) | Extraer método (opcional) |
| `TaskUrgencyEvaluator.evaluate` | 15 (Commit A) | 0 | Chain of Responsibility |

Completar con los valores **medidos** en SonarCloud (por archivo) y explicar las diferencias.

## 8. Entregable

Sección **"Complejidad cognitiva"** en `docs/analisis-estatico/informe-metricas.md` con:

1. Definición y reglas de cálculo (§2).
2. Tabla comparativa ciclomática vs cognitiva (§3).
3. Cálculo línea por línea de la versión ingenua y de la final (§6).
4. Evidencias `07`, `08`, `09` y la tabla de §7.
5. Conclusión: por qué el umbral se fijó en 10 y qué técnicas funcionaron mejor.

## 9. Definición de "hecho"

- Commits A y B en el historial, con sus análisis en *Activity* de SonarCloud.
- Cero issues `java:S3776` abiertos al final.
- Ningún método de producción con cognitiva > 10.
- Sección del informe completada con valores medidos.
