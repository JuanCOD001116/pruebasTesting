# Quality Gate: "TaskManager Gate"

> **Proyecto:** Task Manager API (`JuanCOD001116_pruebasTesting`)  
> **Organización:** `juancod001116`  
> **Fuente de verdad:** `.copilot/07-quality-gate.md`, `.copilot/04-guia-general-analisis-estatico.md` y mediciones en SonarCloud  

---

## 1. Concepto y Enfoque

El **Quality Gate** es el conjunto de condiciones booleanas evaluadas al final de cada análisis de SonarCloud. Si alguna condición falla, el análisis se marca como **FAILED** y el pipeline de CI (`./gradlew clean build sonar`) se bloquea mediante `sonar.qualitygate.wait=true`.

Este gate sigue el principio **Clean as You Code**:
- Aplica las reglas más exigentes sobre el **código nuevo (New Code)** para impedir que entre deuda técnica, bugs o vulnerabilidades a la base de código.
- Mantiene umbrales de seguridad y confiabilidad sobre el **código global (Overall Code)** sin bloquear el avance por deuda histórica menor.

---

## 2. Condiciones del Quality Gate

### 2.1 Condiciones sobre Código Nuevo (New Code)

| Métrica | Identificador Sonar | Operador | Umbral de fallo | Severidad / Impacto | Justificación |
|---|---|---|---|---|---|
| **Issues / Violaciones** | `new_violations` | is greater than | **> 0** | Error | Tolerancia cero a cualquier issue detectado por las reglas del Quality Profile en código nuevo. |
| **Coverage** | `new_coverage` | is less than | **< 80.0%** | Error | Coincide con el estándar mínimo definido en `project-planning.md` §6 y spec 04. |
| **Duplicated Lines (%)** | `new_duplicated_lines_density` | is greater than | **> 3.0%** | Error | Previene la duplicación y el copy-paste en DTOs, controllers y lógica de excepciones. |
| **Security Hotspots Reviewed** | `new_security_hotspots_reviewed` | is less than | **< 100%** | Error | Auditoría obligatoria de todos los puntos sensibles (permisos Docker, credenciales, consola H2). |
| **Maintainability Rating** | `new_maintainability_rating` | is worse than | **peor que A** | Error | Garantiza cero deuda técnica no justificada en nuevas funcionalidades y refactors. |
| **Reliability Rating** | `new_reliability_rating` | is worse than | **peor que A** | Error | Cero bugs introducidos en cambios recientes. |
| **Security Rating** | `new_security_rating` | is worse than | **peor que A** | Error | Tolerancia cero a vulnerabilidades nuevas. |
| **Security Review Rating** | `new_security_review_rating` | is worse than | **peor que A** | Error | Auditoría exhaustiva de seguridad; ningún hotspot nuevo puede quedar sin revisar. |

### 2.2 Condiciones sobre Código Global (Overall Code)

| Métrica | Identificador Sonar | Operador | Umbral de fallo | Severidad / Impacto | Justificación |
|---|---|---|---|---|---|
| **Coverage** | `coverage` | is less than | **< 75.0%** | Error | Piso de calidad para toda la aplicación, permitiendo un margen para clases utilitarias o de configuración. |
| **Reliability Rating** | `reliability_rating` | is worse than | **peor que B** | Error | Asegura que no se acumulen bugs críticos en el historial del repositorio. |
| **Security Rating** | `security_rating` | is worse than | **peor que A** | Error | Cero vulnerabilidades en cualquier parte del código fuente. |

---

## 3. Relación Técnica: Quality Profile → Issue → Condición `Issues > 0` → Gate

SonarCloud no ofrece una condición genérica del tipo *"complejidad por método ≤ 10"* directamente en el Quality Gate, ya que las métricas globales de complejidad en el gate son agregadas.

El control estricto de complejidad, diseño y estilo se logra articulando el **Quality Profile** con el **Quality Gate**:

```
[1. Quality Profile: "TaskManager Java Way"]
     Define reglas activas y umbrales específicos:
     - java:S3776 (Cognitive Complexity > 10)
     - java:S1541 (Cyclomatic Complexity > 10)
     - java:S1192 (Literales duplicados)
     - java:S107  (Parámetros en métodos > 5, constructores > 7)
     - java:S2208, S6204, S6813, S134, S1067, S138, S2699
          ↓
[2. Detección de Violaciones en Código Nuevo]
     Cualquier método o clase nueva que viole una regla genera un Issue (violación).
     Ejemplo: TaskUrgencyEvaluator.evaluate con cognitiva 15 generó 1 issue java:S3776.
          ↓
[3. Evaluación Inmediata en el Quality Gate]
     La condición 'new_violations > 0' se dispara con 1 solo issue en código nuevo,
     y simultáneamente degrada el 'new_maintainability_rating' (peor que A).
          ↓
[4. Bloqueo en CI/CD (Quality Gate FAILED)]
     El pipeline de GitHub Actions se detiene (exit code != 0), 
     bloqueando el Pull Request hasta que se ejecute la remediación o refactor.
```

Esta relación técnica vincula directamente el diseño de código limpio con la aprobación del pipeline en CI/CD.

---

## 4. Auditoría de Security Hotspots

Los siguientes Security Hotspots identificados en la línea base son gestionados y trazados en la remediación (spec 08):

1. **Ejecución como root en Docker (`Dockerfile`):**  
   - *Resolución:* Creación y uso de usuario no privilegiado `app` (H-04).
2. **Credenciales en `docker-compose.yml` y defaults en `application-prod.yml`:**  
   - *Resolución:* Parametrización exclusiva por variables de entorno sin defaults inseguros (H-14).
3. **Consola H2 web habilitada en perfil `dev`:**  
   - *Resolución:* Marcado como **Safe** justificado para entorno de desarrollo local aislado.

---

## 5. Artefactos Asociados

- **Definición del Quality Gate:** `quality/sonar/quality-gate.md` (este documento).
- **Backup del Quality Profile:** `quality/sonar/taskmanager-java-way.xml` (exportado tras extender *Sonar way core* en SonarCloud).
