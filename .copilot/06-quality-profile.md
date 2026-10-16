# 06 — Quality Profile: "TaskManager Java Way"

## 1. Concepto

Un **Quality Profile** es el conjunto de **reglas** (y sus parámetros) que Sonar aplica a un lenguaje durante el análisis. Responde a *"¿qué se considera un problema en este código?"*. Cada issue que aparece en SonarCloud existe porque una regla activa del perfil lo detectó.

Diferencia clave con el Quality Gate (spec 07): el **perfil decide qué se detecta**; el **gate decide si el resultado es aceptable** para liberar.

## 2. Estrategia: extender, no copiar

Crear el perfil **extendiendo** *Sonar way* (Java), no copiándolo:

- *Extend*: hereda todas las reglas de Sonar way y **recibe automáticamente** las reglas nuevas que Sonar publique. Solo se agregan/ajustan las propias.
- *Copy*: congela el estado actual; las reglas nuevas de Sonar no llegan.

Para un proyecto vivo, extender es la opción mantenible. Justificarlo en el informe.

## 3. Pasos en SonarCloud

1. *Organización → Quality Profiles → Java → Sonar way → ⚙ → Extend*. Nombre: **`TaskManager Java Way`**.
2. *Activate More* → buscar cada regla de §4 por clave o título → activar con la severidad y los parámetros indicados.
3. En el proyecto: *Administration → Quality Profiles → Java* → seleccionar `TaskManager Java Way`.
4. Volver a ejecutar el análisis (push a `main`).
5. *Quality Profiles → TaskManager Java Way → ⚙ → Back up*: guardar el XML en `quality/sonar/taskmanager-java-way.xml` y versionarlo. Así el perfil es reproducible y revisable en Git.

> Las claves de regla pueden variar entre versiones del analizador. Si una clave no aparece, busca por el título indicado; si la regla ya no existe, documenta el reemplazo.

## 4. Reglas a activar o ajustar

### 4.1 Complejidad y tamaño (núcleo del taller)

| Clave | Título | Parámetro | Valor Sonar way | Valor propio | Justificación |
|---|---|---|---|---|---|
| `java:S3776` | Cognitive Complexity of methods should not be too high | `Threshold` | 15 | **10** | API pequeña y didáctica; métodos de negocio deben ser fáciles de leer. |
| `java:S1541` | Methods should not be too complex (cyclomatic) | `Threshold` | inactiva | **10** | Umbral clásico de McCabe; coincide con la recomendación del spec 03 §4. |
| `java:S134` | Control flow statements should not be nested too deeply | `max` | inactiva | **3** | Penaliza el anidamiento, principal fuente de complejidad cognitiva. |
| `java:S1067` | Expressions should not be too complex | `max` | inactiva | **3** | Evita que condiciones como la de `validate()` crezcan sin control. |
| `java:S138` | Methods should not have too many lines | `max` | inactiva | **40** | Métodos cortos = una responsabilidad. |
| `java:S107` | Methods should not have too many parameters | `max` | 7 | **5** | Fuerza objetos de parámetros (ej. `TaskSearchCriteria`, spec 09). |

### 4.2 Mantenibilidad y estilo

| Clave | Título | Justificación |
|---|---|---|
| `java:S2208` | Wildcard imports should not be used | Google Java Style lo prohíbe; hoy hay `import ...annotation.*` y `static ...Assertions.*`. |
| `java:S6204` | `Stream.toList()` should be used instead of `collectors` | Java 17: `collect(Collectors.toList())` aparece en `TaskServiceImpl`. |
| `java:S1192` | String literals should not be duplicated | Mensajes repetidos en logs/excepciones. |
| `java:S1128` | Unnecessary imports should be removed | Hay imports sin uso en tests. |
| `java:S1068` | Unused "private" fields should be removed | `taskEntity` sin uso en el test de controller. |
| `java:S6813` | Field dependency injection should be avoided | Refuerza la convención de constructor injection (`project-planning.md` §10). |

### 4.3 Confiabilidad, seguridad y tests

| Clave | Título | Justificación |
|---|---|---|
| `java:S2699` | Tests should include assertions | Garantiza que ningún test pase "vacío". |
| `java:S5778` | Only one method invocation is expected when testing runtime exceptions | Lambdas de `assertThrows` limpias. |
| `java:S5786` | JUnit5 test classes and methods should have default package visibility | Convención JUnit 5 ya usada en el proyecto. |
| `java:S4684` | Persistent entities should not be used as arguments of `@RequestMapping` methods | Protege el DTO Pattern (nunca exponer `Task` en la API). |
| `java:S2077` | Formatting SQL queries is security-sensitive | Hotspot ante cualquier SQL concatenado en el futuro. |

Las reglas de seguridad (vulnerabilidades y hotspots) ya activas en Sonar way **no se desactivan**.

## 5. Reglas que NO se desactivan y por qué

No desactivar reglas para "pasar" el gate. Si un issue es falso positivo, marcarlo en SonarCloud como **False Positive** o **Accepted** con un comentario que lo justifique. Desactivar la regla esconde el problema para todo el código futuro.

## 6. Evidencia del efecto del perfil

Tras activar el perfil (Fase 1), el número de issues aumentará respecto a la línea base. Documentar en el informe:

| Métrica | Línea base (Sonar way) | Con TaskManager Java Way |
|---|---|---|
| Issues totales | … | … |
| Issues de mantenibilidad | … | … |
| Issues `S3776` / `S1541` | … | … |

Esperado con el código actual: issues de `S2208` (wildcard imports), `S6204` (`Collectors.toList`), `S1068`/`S1128` en tests. `validate()` (ciclomática 7, cognitiva 5) **no** debería disparar `S1541`/`S3776`; eso se verifica en los specs 10 y 11.

## 7. Definición de "hecho"

- Perfil `TaskManager Java Way` creado por extensión y asignado al proyecto.
- Las reglas de §4 activas con los parámetros indicados.
- `quality/sonar/taskmanager-java-way.xml` versionado.
- Tabla de §6 completada en `docs/analisis-estatico/informe-metricas.md`.
- Evidencias `04` y `05` capturadas.
