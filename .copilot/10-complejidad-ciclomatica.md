# 10 — Complejidad Ciclomática (medición, análisis y control)

## 1. Concepto

La **complejidad ciclomática** de McCabe mide el número de **caminos linealmente independientes** en el grafo de flujo de control de un método:

```
V(G) = e − n + 2p        (aristas − nodos + 2·componentes)
V(G) = puntos de decisión + 1
```

Indica el **número mínimo de casos de prueba** para cubrir todos los caminos base (relación directa con el spec 03). Mide **testeabilidad**, no legibilidad: dos métodos con el mismo V(G) pueden ser muy distintos de entender (eso lo mide la complejidad cognitiva, spec 11).

## 2. Cómo la cuenta Sonar (Java)

Sonar suma **+1** por: la declaración del método, cada `if`, `for`, `while`, `do`, `case` (no `default`), operador ternario `?:`, y cada `&&` / `||`. **No** suma por `else`, `default`, `try` ni `finally`.

Dónde verla:

- *Measures → Complexity → Cyclomatic Complexity*: total por archivo/directorio (suma de sus métodos).
- Por método: Sonar no lo muestra en la UI, pero la regla `java:S1541` (spec 06) levanta un issue en cada método que supere 10.
- JaCoCo: columna **Cxty** del reporte HTML, por método (spec 05 §6.1). Úsala como contraste.

Si el valor de Sonar difiere del cálculo manual, explicar por qué en el informe (ej. cómo se cuentan las lambdas o los métodos generados).

## 3. Umbrales del proyecto

| V(G) por método | Interpretación | Acción |
|---|---|---|
| 1–5 | Simple | Ninguna |
| 6–10 | Moderada | Aceptable; revisar si crece |
| > 10 | Alta | **Issue `S1541`** → refactor obligatorio (spec 03 §4 ya lo recomendaba) |

## 4. Caso detallado: `TaskStatusTransitionValidator.validate`

```java
if (currentStatus == newStatus) { ... }                                  // d1
if (!ALLOWED.getOrDefault(...).contains(newStatus)) { ... }              // d2
if (currentStatus == PENDING                                              // d3
        && newStatus == IN_PROGRESS                                       // d4 (&&)
        && (assignedTo == null                                            // d5 (&&)
            || assignedTo.isBlank())) { ... }                             // d6 (||)
```

- Puntos de decisión: 3 `if` + 2 `&&` + 1 `||` = **6** → **V(G) = 7** (no 6 como dice hoy el Javadoc del test; ver spec 08 H-12).
- Valor esperado en Sonar para el archivo: `validate` (7) + `invalid` (1) = **8**.

Caminos base (método de McCabe: variar una decisión a la vez):

| Camino | d1 | d2 | d3 | d4 | d5 | d6 | Resultado | Caso |
|---|---|---|---|---|---|---|---|---|
| 1 | V | – | – | – | – | – | Rechaza (regla 3) | TC-01 |
| 2 | F | V | – | – | – | – | Rechaza (no permitida) | TC-04 |
| 3 | F | F | V | V | V | – | Rechaza (assignedTo null) | TC-03 |
| 4 | F | F | V | V | F | V | Rechaza (assignedTo en blanco) | TC-18 |
| 5 | F | F | V | V | F | F | Acepta | TC-02 |
| 6 | F | F | V | F | – | – | Acepta | TC-05 |
| 7 | F | F | F | – | – | – | Acepta | TC-06 |

(V = verdadero, F = falso, – = no se evalúa por cortocircuito.) Actualizar el Javadoc de `TaskStatusTransitionValidatorTest` con esta tabla.

## 5. Inventario antes / después

Valores calculados manualmente con la regla de §2. **Verificarlos contra Sonar y JaCoCo** y registrar el valor medido en el informe.

| Método | V(G) antes | Cambio | V(G) después |
|---|---|---|---|
| `TaskController.getAllTasks` | 5 (2 `if`/`else if` + 1 `else if` + `&&` + 1) | Delegación a `findTasks(criteria)` (F2) | 1 |
| `TaskMapper.toEntity(Update, Task)` | 6 | `applyIfPresent` (H-13) | 1 (+ helper 2) |
| `TaskServiceImpl.updateTask` | 4 (`if` + `&&` + `?:` + 1) | Extraer `resolveAssignee(request, task)` y `findTaskOrThrow` | 3 |
| `TaskServiceImpl.deleteTask` | 2 | — | 2 |
| `TaskStatusTransitionValidator.validate` | 7 | Opcional: extraer `requiresAssignee(...)` | 7 (o 4 + 4) |
| `TaskUrgencyEvaluator.evaluate` (versión ingenua, spec 11 §6) | 7 | Chain of Responsibility | 1 |
| Cada `UrgencyRule` (final) | — | nuevo | 2–3 |
| `TaskSpecifications.hasX(...)` | — | nuevo (ternario) | 2 |
| `TaskSearchCriteria.isDueRangeValid` | — | nuevo | 3 |
| `TaskSummaryServiceImpl.completionRate` | — | nuevo | 2 |

Totales por archivo (desde *Measures*): registrar `TaskController`, `TaskServiceImpl`, `TaskMapper`, `TaskStatusTransitionValidator` y el paquete `service.urgency` en ambas fases.

## 6. Relación con las pruebas (trazabilidad)

Para cada método con V(G) ≥ 4 en la versión final, el informe debe mostrar que existen **al menos V(G) casos de prueba** que ejercen sus caminos, y que JaCoCo reporta 100% de ramas en ese método. Métodos objetivo mínimos: `validate`, `updateTask`, `isDueRangeValid`, y cada `UrgencyRule`.

## 7. Entregable

Sección **"Complejidad ciclomática"** en `docs/analisis-estatico/informe-metricas.md` con:

1. Definición y fórmula.
2. Cálculo manual detallado de `validate` (§4) con el grafo (puede reutilizar el Mermaid del spec 03).
3. Tabla de §5 con columnas *manual*, *Sonar*, *JaCoCo*.
4. Captura de *Measures → Complexity* (evidencia `10`).
5. Conclusión: qué refactors redujeron V(G) y cómo afectó al número de tests necesarios.

## 8. Definición de "hecho"

- Javadoc del test del validador corregido (V(G) = 7, 7 caminos).
- Cero issues `java:S1541` abiertos.
- Tabla antes/después completada con valores medidos.
