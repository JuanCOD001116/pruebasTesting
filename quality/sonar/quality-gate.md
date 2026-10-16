# Quality Gate: "TaskManager Gate"

> **Proyecto:** Task Manager API (`JuanCOD001116_pruebasTesting`)  
> **Organización:** `juancod001116`  
> **Fuente de verdad:** `.copilot/07-quality-gate.md` y `.copilot/04-guia-general-analisis-estatico.md`  

---

## 1. Concepto y Enfoque

El **Quality Gate** es el conjunto de condiciones booleanas evaluadas al final de cada análisis de SonarCloud. Si alguna condición falla, el análisis se marca como **FAILED** y el pipeline de CI (`./gradlew clean build sonar`) se bloquea mediante `sonar.qualitygate.wait=true`.

Este gate sigue el principio **Clean as You Code**:
- Aplica las reglas más exigentes sobre el **código nuevo (New Code)** para impedir que entre deuda técnica, bugs o vulnerabilidades a la base de código.
- Mantiene umbrales de seguridad y confiabilidad sobre el **código global (Overall Code)** sin bloquear el avance por deuda histórica menor.

---

## 2. Condiciones del Quality Gate

### 2.1 Condiciones sobre Código Nuevo (New Code)

| Métrica | Operador | Umbral de fallo | Severidad / Impacto | Justificación |
|---|---|---|---|---|
| **Coverage** | is less than | **< 80.0%** | Error | Coincide con el estándar mínimo definido en `project-planning.md` §6 y spec 04. |
| **Duplicated Lines (%)** | is greater than | **> 3.0%** | Error | Previene la duplicación y el copy-paste en DTOs, controllers y lógica de excepciones. |
| **Maintainability Rating** | is worse than | **peor que A** | Error | Garantiza cero deuda técnica no justificada en nuevas funcionalidades y refactors. |
| **Reliability Rating** | is worse than | **peor que A** | Error | Cero bugs introducidos en cambios recientes. |
| **Security Rating** | is worse than | **peor que A** | Error | Tolerancia cero a vulnerabilidades nuevas. |
| **Security Hotspots Reviewed** | is less than | **< 100%** | Error | Auditoría obligatoria de todos los puntos sensibles (permisos Docker, credenciales, consola H2). |

### 2.2 Condiciones sobre Código Global (Overall Code)

| Métrica | Operador | Umbral de fallo | Severidad / Impacto | Justificación |
|---|---|---|---|---|
| **Coverage** | is less than | **< 75.0%** | Error | Piso de calidad para toda la aplicación, permitiendo un margen para clases utilitarias o triviales. |
| **Reliability Rating** | is worse than | **peor que B** | Error | Asegura que no se acumulen bugs críticos en el historial del repositorio. |
| **Security Rating** | is worse than | **peor que A** | Error | Cero vulnerabilidades en cualquier parte del código fuente. |

---

## 3. ¿Cómo controla el Quality Gate la Complejidad Ciclomática y Cognitiva?

SonarCloud no ofrece una métrica de gate del tipo *"complejidad por método ≤ 10"*, ya que las métricas directas de complejidad en el gate son acumulativas para todo el proyecto.

Por ello, el control se realiza mediante una **estrategia de tres capas**:

```
[1. Quality Profile] 
     Reglas java:S1541 (ciclomática > 10) y java:S3776 (cognitiva > 10) activas.
         ↓
[2. Análisis de Código Nuevo] 
     Si un método nuevo supera 10 puntos, genera un Issue de Mantenibilidad (Code Smell).
         ↓
[3. Quality Gate Evaluation] 
     El issue degrada el "Maintainability Rating on New Code" a 'B' o inferior, 
     haciendo fallar la condición: "Maintainability Rating is worse than A".
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
- **Backup del Quality Profile:** `quality/sonar/taskmanager-java-way.xml` (exportado tras extender *Sonar way* en SonarCloud).
