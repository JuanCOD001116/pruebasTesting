# 07 — Quality Gate: "TaskManager Gate"

## 1. Concepto

Un **Quality Gate** es un conjunto de **condiciones booleanas sobre métricas** que se evalúan al final de cada análisis. Si alguna falla, el gate queda en **Failed** y el código no debería liberarse/mergearse. Responde a *"¿este código es suficientemente bueno para avanzar?"*.

Sonar recomienda el enfoque **Clean as You Code**: las condiciones más estrictas aplican al **código nuevo** (lo que cambió dentro del periodo *New Code*, spec 05 §5.1 paso 6). Así un proyecto con deuda histórica puede mejorar sin bloquearse, pero no se permite agregar deuda nueva.

## 2. Definición del gate

Crear en *Organización → Quality Gates → Create* el gate **`TaskManager Gate`** (o *Copy* de Sonar way y ajustar).

### 2.1 Condiciones sobre código nuevo (New Code)

| Métrica | Operador | Valor | Justificación |
|---|---|---|---|
| Coverage | is less than | **80%** | Mismo umbral que `project-planning.md` §6. |
| Duplicated Lines (%) | is greater than | **3%** | Detecta copy-paste (ej. construcción repetida de `ErrorResponse`). |
| Maintainability Rating | is worse than | **A** | Cero deuda técnica relevante nueva. |
| Reliability Rating | is worse than | **A** | Cero bugs nuevos. |
| Security Rating | is worse than | **A** | Cero vulnerabilidades nuevas. |
| Security Hotspots Reviewed | is less than | **100%** | Todo hotspot (Dockerfile root, credenciales, etc.) debe revisarse. |

### 2.2 Condiciones sobre código global (Overall Code)

| Métrica | Operador | Valor | Justificación |
|---|---|---|---|
| Coverage | is less than | **75%** | Piso para todo el proyecto (un poco más bajo que new code para no castigar clases triviales). |
| Reliability Rating | is worse than | **B** | Ningún bug grave acumulado. |
| Security Rating | is worse than | **A** | Sin vulnerabilidades en ningún punto del código. |

### 2.3 ¿Cómo entra la complejidad al gate?

Sonar no ofrece una condición de gate del tipo "complejidad por método ≤ 10" (las métricas `Cyclomatic Complexity` y `Cognitive Complexity` del gate son **totales del archivo/proyecto** y crecen con el tamaño, así que no son buenos umbrales). La complejidad se controla en dos capas:

1. El **Quality Profile** (spec 06) activa `java:S1541` y `java:S3776` con umbral 10 → cada método que lo supere genera un **issue de mantenibilidad**.
2. Ese issue empeora el **Maintainability Rating** del código nuevo → el gate falla por la condición de §2.1.

Explicar esta relación perfil → issue → rating → gate en el informe; es el punto conceptual más importante del taller.

## 3. Pasos en SonarCloud

1. Crear el gate con las condiciones de §2.
2. En el proyecto: *Administration → Quality Gate* → seleccionar `TaskManager Gate` (no dejarlo como default de la organización si hay otros proyectos).
3. `sonar.qualitygate.wait=true` (spec 05 §5.2) hace que `./gradlew sonar` **falle** si el gate falla → el workflow de GitHub queda en rojo y el PR muestra el estado.
4. Documentar las condiciones en `quality/sonar/quality-gate.md` (Sonar no permite exportar gates como XML, así que la documentación versionada es la fuente de verdad).

## 4. Ciclo de evidencia Failed → Passed

Se produce de forma natural en el caso de estudio del spec 11 §6:

1. Commit con `TaskUrgencyEvaluator` ingenuo (complejidad cognitiva 15 > 10) → issue `S3776` → Maintainability Rating de new code baja → **Failed** (evidencia `07`).
2. Commit con el refactor (Chain of Responsibility) → issue cerrado → **Passed** (evidencia `09`).

## 5. Revisión de Security Hotspots

Hotspots esperados en el código actual (revisarlos en *Security Hotspots* y marcarlos *Safe* o *Fixed* con comentario):

| Hotspot esperado | Resolución (spec 08) |
|---|---|
| Contenedor ejecutándose como root (Dockerfile) | **Fixed**: usuario no privilegiado (H-04) |
| Contraseñas hardcodeadas en `docker-compose.yml` / defaults en `application-prod.yml` | **Fixed**: variables de entorno sin default (H-14) |
| Consola H2 habilitada | **Safe**: solo perfil `dev`, documentado |

## 6. Definición de "hecho"

- `TaskManager Gate` creado con las condiciones de §2 y asignado al proyecto.
- `quality/sonar/quality-gate.md` versionado con la tabla de condiciones y su justificación.
- 100% de hotspots revisados.
- Evidencias `06`, `07` y `09` capturadas; último análisis en **Passed**.
