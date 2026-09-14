# 02 — Pruebas de Transición de Estado (Caja Negra)

## 1. Fundamento

Esta es una técnica de **caja negra**: no importa cómo esté implementado el validador por dentro, solo importa que el comportamiento observable (aceptar/rechazar cada transición) coincida con la máquina de estados definida en `01-reglas-negocio-transicion-estado.md`. El modelo de referencia es la matriz de transiciones de ese documento.

## 2. Objetivo de la suite

Cubrir el 100% de las celdas de la matriz de transiciones (16 combinaciones de estado + 1 variante adicional por la condición de `assignedTo` en `PENDING → IN_PROGRESS` = 17 casos), verificando tanto los casos que deben aceptarse como los que deben rechazarse con `InvalidTaskStateException`.

## 3. Matriz de casos de prueba

| ID | Desde | Hacia | assignedTo | Resultado esperado |
|---|---|---|---|---|
| TC-01 | PENDING | PENDING | "juan" | Rechaza (Regla 3) |
| TC-02 | PENDING | IN_PROGRESS | "juan" | Acepta |
| TC-03 | PENDING | IN_PROGRESS | null | Rechaza (Regla 5) |
| TC-04 | PENDING | DONE | "juan" | Rechaza (Regla 4) |
| TC-05 | PENDING | CANCELLED | "juan" | Acepta |
| TC-06 | IN_PROGRESS | PENDING | "juan" | Acepta |
| TC-07 | IN_PROGRESS | IN_PROGRESS | "juan" | Rechaza (Regla 3) |
| TC-08 | IN_PROGRESS | DONE | "juan" | Acepta |
| TC-09 | IN_PROGRESS | CANCELLED | "juan" | Acepta |
| TC-10 | DONE | PENDING | "juan" | Rechaza (Regla 2) |
| TC-11 | DONE | IN_PROGRESS | "juan" | Rechaza (Regla 2) |
| TC-12 | DONE | DONE | "juan" | Rechaza (Regla 2/3) |
| TC-13 | DONE | CANCELLED | "juan" | Rechaza (Regla 2) |
| TC-14 | CANCELLED | PENDING | "juan" | Rechaza (Regla 1) |
| TC-15 | CANCELLED | IN_PROGRESS | "juan" | Rechaza (Regla 1) |
| TC-16 | CANCELLED | DONE | "juan" | Rechaza (Regla 1) |
| TC-17 | CANCELLED | CANCELLED | "juan" | Rechaza (Regla 1/3) |

## 4. Estructura recomendada del test

Clase: `TaskStatusTransitionValidatorTest` en `src/test/java/com/example/taskmanager/service/validation/`.

Usa `@ParameterizedTest` + `@MethodSource` para no repetir 17 métodos casi idénticos, pero mantén la estructura AAA visible dentro del método parametrizado:

```java
@ParameterizedTest(name = "{index}: {0} -> {1} (assignedTo={2}) => {3}")
@MethodSource("transitionCases")
@DisplayName("Dado un estado origen, cuando se solicita una transición, entonces el resultado coincide con la regla de negocio")
void shouldRespectTransitionMatrix(TaskStatus from, TaskStatus to, String assignedTo, boolean expectedValid) {
    // Arrange
    // (el propio MethodSource entrega los datos de entrada)

    // Act & Assert
    if (expectedValid) {
        assertDoesNotThrow(() -> validator.validate(from, to, assignedTo));
    } else {
        assertThrows(InvalidTaskStateException.class,
                () -> validator.validate(from, to, assignedTo));
    }
}
```

El `@MethodSource` debe construirse directamente a partir de la tabla de la sección 3 — cada fila es un `Arguments.of(...)`.

Además de la suite parametrizada, agrega **al menos 2 tests individuales no parametrizados** para los casos con mayor valor de negocio, con nombres descriptivos y `@DisplayName` completo estilo Given/When/Then, por ejemplo:

```java
@Test
@DisplayName("Dado que una tarea está CANCELLED, cuando se intenta reabrirla, entonces se rechaza con mensaje claro")
void shouldRejectReopeningCancelledTask() {
    // Arrange
    // Act
    // Assert — además de assertThrows, verifica el contenido del mensaje de la excepción
}
```

## 5. Prueba de integración complementaria

Agrega al menos dos tests a nivel de controller (`@SpringBootTest` + `MockMvc`, como ya está definido para pruebas de integración en `project-planning.md`) que confirmen el comportamiento end-to-end:

1. `PATCH /api/tasks/{id}/status` con una transición inválida (ej. `CANCELLED → PENDING`) debe responder con el código HTTP y el cuerpo de error estandarizado definidos en la sección de manejo de errores del proyecto.
2. `PUT /api/tasks/{id}` con un cambio de `status` inválido también debe ser rechazado — esto verifica el punto de integración obligatorio #2 del archivo 01 (que `PUT` no pueda saltarse la validación que sí aplica en `PATCH`).

## 6. Principios de pruebas aplicados (referencia)

- **Cobertura "exhaustiva" viable:** normalmente las pruebas exhaustivas son inviables por la explosión combinatoria, pero aquí el dominio es finito y pequeño (4 estados × 4 estados + 1 condición), así que cubrir el 100% de la matriz sí es razonable y es el objetivo de esta suite.
- **Paradoja del pesticida:** no reutilices siempre el mismo `assignedTo` o el mismo `title` en los datos de prueba; varía los valores entre casos para no acostumbrar la suite a un único patrón de datos.
- **F.I.R.S.T:** cada caso de la matriz debe poder ejecutarse de forma aislada y en cualquier orden.

## 7. Definición de "hecho" de esta suite

- Las 17 filas de la matriz están cubiertas.
- Al menos 2 tests no parametrizados con `@DisplayName` de negocio.
- Al menos 2 tests de integración (uno vía PATCH, uno vía PUT).
- Todos los tests pasan y son independientes entre sí.
