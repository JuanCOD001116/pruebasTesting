# 03 — Pruebas de Cobertura de Camino (Caja Blanca)

## 1. Fundamento

A diferencia de las pruebas de transición de estado (que se diseñan desde el comportamiento observable), la cobertura de camino es una técnica de **caja blanca**: se diseña mirando la estructura interna del código — el grafo de flujo de control del método — para asegurar que cada camino independiente se ejecute al menos una vez.

La métrica de referencia es el **número ciclomático** de McCabe:

```
V(G) = e - n + 2p
```

Donde `e` = número de aristas, `n` = número de nodos, y `p` = número de componentes conexas del grafo (normalmente 1). `V(G)` también indica el número mínimo de casos de prueba necesarios para cubrir todos los caminos independientes (cobertura de decisión).

## 2. Método(s) objetivo

El objetivo principal es `TaskStatusTransitionValidator.validate(...)` (o el método equivalente que implemente las 6 reglas del archivo 01), por ser el método con más lógica condicional del cambio propuesto. Si al implementarlo terminas repartiendo la lógica en más de un método (por ejemplo, un método privado por regla), incluye también esos métodos si tienen ramas propias.

## 3. Paso 1 — Construir el grafo de flujo de control

Con el código ya implementado (archivo 01), documenta el grafo de flujo de control del método objetivo, ya sea como comentario Javadoc en la clase de test o como diagrama Mermaid (`flowchart TD`) en este mismo repositorio, indicando:

- Cada nodo = una sentencia o bloque de sentencias sin bifurcación.
- Cada arista = una posible transferencia de control (incluye las ramas `if`/`else`, cada `case`, cada condición de bucle).

## 4. Paso 2 — Calcular la complejidad ciclomática

Aplica la fórmula `V(G) = e - n + 2p` (o, de forma equivalente, cuenta los puntos de decisión + 1: cada `if`, `else if`, `&&`/`||` dentro de una condición, y cada `case` suma un punto de decisión).

Con las 6 reglas del archivo 01, una implementación típica de `validate(...)` ronda **V(G) ≈ 7–9**, dependiendo de cómo se estructure el código (por ejemplo, si se usa un `switch` sobre `currentStatus` con condiciones anidadas, o un mapa de transiciones válidas más una condición aparte para la regla 5). **Calcula el valor real sobre tu implementación final y documéntalo** — no asumas el valor de ejemplo de este documento.

Si `V(G)` supera 10, es una señal de que el método debería descomponerse en métodos más pequeños antes de continuar con el diseño de caminos.

## 5. Paso 3 — Identificar los caminos base (basis paths)

Enumera los caminos independientes del grafo (método de McCabe: parte de un camino base y genera los siguientes cambiando una decisión a la vez). Documenta la lista como comentario en la clase de test, con este formato orientativo:

```
Camino 1: currentStatus == newStatus                                    → rechaza (Regla 3)
Camino 2: currentStatus == CANCELLED                                    → rechaza (Regla 1)
Camino 3: currentStatus == DONE                                         → rechaza (Regla 2)
Camino 4: currentStatus == PENDING, newStatus == DONE                   → rechaza (Regla 4)
Camino 5: currentStatus == PENDING, newStatus == IN_PROGRESS, sin assignedTo → rechaza (Regla 5)
Camino 6: currentStatus == PENDING, newStatus == IN_PROGRESS, con assignedTo → acepta
Camino 7: currentStatus == PENDING, newStatus == CANCELLED               → acepta
Camino 8: currentStatus == IN_PROGRESS, newStatus en {PENDING, DONE, CANCELLED} → acepta
```

Ajusta esta lista a los caminos reales que resulten de tu implementación; el número de caminos debe ser igual o muy cercano a `V(G)`.

## 6. Paso 4 — Diseñar un caso de prueba por camino

Para cada camino identificado, agrega (o reutiliza, si ya existe en el archivo 02) un caso de prueba que fuerce específicamente ese camino. Usa una tabla de trazabilidad en el Javadoc de la clase de test:

| Camino | Test que lo cubre |
|---|---|
| Camino 1 | `shouldRespectTransitionMatrix` fila TC-01 (o equivalente) |
| Camino 5 | `shouldRespectTransitionMatrix` fila TC-03 |
| ... | ... |

En la mayoría de los casos, la suite parametrizada del archivo 02 ya cubrirá todos los caminos, porque la matriz de transiciones fue diseñada a partir de las mismas reglas. El valor de este ejercicio es **verificarlo explícitamente** y detectar caminos que la matriz de negocio no haya revelado (por ejemplo, una combinación de condiciones internas que no sea visible desde la tabla de transiciones).

## 7. Paso 5 — Configurar JaCoCo (medición de cobertura)

Agrega el plugin de JaCoCo en `build.gradle` para verificar objetivamente la cobertura de rama (la aproximación práctica a "cobertura de camino" que ofrece la mayoría de herramientas):

```groovy
plugins {
    id 'jacoco'
}

jacoco {
    toolVersion = "0.8.12"
}

test {
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            element = 'CLASS'
            includes = ['com.example.taskmanager.service.validation.*']
            limit {
                counter = 'BRANCH'
                minimum = 0.90
            }
        }
        rule {
            limit {
                counter = 'INSTRUCTION'
                minimum = 0.80
            }
        }
    }
}

check.dependsOn jacocoTestCoverageVerification
```

Umbral de rama (`BRANCH`) del 90% específicamente para el validador (por ser el foco de este ejercicio), y el 80% general de instrucción ya definido en `project-planning.md` para la capa de servicio.

## 8. Paso 6 — Verificar

Ejecuta:

```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

Revisa el reporte HTML (`build/reports/jacoco/test/html/index.html`) y confirma visualmente que cada rama del método objetivo aparece cubierta (verde), no solo cada línea.

## 9. Por qué ambas suites son necesarias (no son redundantes)

- La suite de **transición de estado** (archivo 02) valida el comportamiento desde la perspectiva del negocio: ¿el sistema hace lo correcto según las reglas?
- La suite de **cobertura de camino** (este archivo) valida la estructura del código: ¿realmente se ejecutó cada rama de la implementación, o hay código muerto / caminos sin probar que la matriz de negocio no reveló?

Es común que la primera técnica encuentre errores de lógica de negocio, y la segunda encuentre errores de codificación (ramas mal implementadas, condiciones invertidas, código inalcanzable) que no siempre son visibles desde la tabla de transiciones.

## 10. Definición de "hecho" de esta suite

- Número ciclomático documentado para el/los método(s) objetivo.
- Lista de caminos base documentada.
- Tabla de trazabilidad camino → test.
- JaCoCo configurado y `jacocoTestCoverageVerification` pasa.
- Reporte HTML revisado manualmente al menos una vez.
