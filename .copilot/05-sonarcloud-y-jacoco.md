# 05 — SonarCloud + Reporte de Cobertura JaCoCo (local y en la nube)

## 1. Objetivo

1. Generar el reporte de cobertura de **JaCoCo** en local (HTML para revisión humana, XML para Sonar) y saber exactamente dónde encontrarlo.
2. Conectar el repositorio de GitHub a **SonarCloud** (hoy llamado *SonarQube Cloud*) y ejecutar el análisis desde **GitHub Actions**, importando el XML de JaCoCo como fuente de cobertura.

## 2. Prerrequisito importante: el plan de SonarCloud

- En el **plan Free** (repos privados) solo existe el Quality Profile *Sonar way* y el Quality Gate *Sonar way*: **no se pueden crear perfiles ni gates propios**.
- Los repositorios **públicos** quedan en el plan *open source*, que **sí** permite Quality Gates y Quality Profiles personalizados y análisis de ramas/PR.

**Decisión:** hacer público el repositorio del proyecto (no contiene secretos; ver `.gitignore` y `.env.example`). Si el repositorio debe ser privado, las alternativas son: (a) la prueba gratuita de 14 días del plan Team, o (b) un SonarQube Community Build local con Docker (también permite perfiles y gates propios). Documenta en el informe cuál se usó.

## 3. Configuración de JaCoCo (ajustes sobre lo que ya existe en `build.gradle`)

`build.gradle` ya aplica `jacoco` (0.8.12), genera XML + HTML y tiene `jacocoTestCoverageVerification`. Ajustes requeridos:

### 3.1 Excluir código sin lógica de la medición

La cobertura debe medir lógica, no clases de configuración ni DTOs. **Las mismas exclusiones** deben aplicarse en JaCoCo (local) y en Sonar (§5) para que ambos números coincidan.

```groovy
def coverageExclusions = [
        '**/TaskManagerApplication.class',
        '**/config/**',
        '**/dto/**',
        '**/exception/*Exception.class'   // solo las excepciones simples; GlobalExceptionHandler SÍ se mide
]

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(files(classDirectories.files.collect {
        fileTree(dir: it, exclude: coverageExclusions)
    }))
}

jacocoTestCoverageVerification {
    dependsOn jacocoTestReport
    classDirectories.setFrom(jacocoTestReport.classDirectories)
    violationRules { /* mantener las reglas actuales + las del spec 09 §6 */ }
}
```

### 3.2 Excluir código generado por Lombok

Crear `lombok.config` en la raíz del proyecto:

```properties
config.stopBubbling = true
lombok.addLombokGeneratedAnnotation = true
```

JaCoCo (≥ 0.8.0) ignora los métodos anotados con `@Generated`, así los getters/setters/builders de Lombok no inflan ni bajan artificialmente la cobertura. Sonar también los respeta.

## 4. Generar el reporte en local

```bash
./gradlew clean test jacocoTestReport
```

`test` ya tiene `finalizedBy jacocoTestReport`, así que `./gradlew test` también lo genera.

## 5. Integración con SonarCloud

### 5.1 Pasos en la interfaz de SonarCloud

1. Entrar a https://sonarcloud.io con la cuenta de GitHub e **importar la organización** de GitHub.
2. **Analyze new project** → seleccionar el repositorio.
3. En *Administration → Analysis Method*: **desactivar Automatic Analysis**. Es obligatorio: el análisis automático no lee reportes de cobertura y entra en conflicto con el análisis por CI.
4. Elegir **With GitHub Actions**. SonarCloud muestra el `projectKey` (normalmente `<org>_<repo>`) y la `organization`.
5. *My Account → Security → Generate Token*. Guardarlo en GitHub: *Settings → Secrets and variables → Actions → New repository secret* con nombre `SONAR_TOKEN`.
6. En *Administration → New Code*: definir **Previous version** (se alimenta de `version` en `build.gradle`) o **Number of days = 30**. Justificarlo en el informe.

### 5.2 Plugin de Gradle

En `build.gradle` (usar la última versión publicada del plugin `org.sonarqube` en el Gradle Plugin Portal; a la fecha 7.3.x):

```groovy
plugins {
    // ...existentes
    id 'org.sonarqube' version '7.3.0.8198'
}

sonar {
    properties {
        property 'sonar.projectKey', '<org>_<repo>'          // reemplazar
        property 'sonar.organization', '<org>'                // reemplazar
        property 'sonar.host.url', 'https://sonarcloud.io'
        property 'sonar.projectName', 'Task Manager API'
        property 'sonar.java.source', '17'
        property 'sonar.coverage.jacoco.xmlReportPaths',
                "${layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml"
        property 'sonar.coverage.exclusions',
                '**/TaskManagerApplication.java,**/config/**,**/dto/**,**/exception/*Exception.java'
        property 'sonar.qualitygate.wait', 'true'          // el build falla si el Quality Gate falla
    }
}

tasks.named('sonar') {
    dependsOn jacocoTestReport   // las versiones recientes del plugin ya no dependen de test automáticamente
}
```

**Nunca** poner `sonar.token` en `build.gradle`: el plugin lo lee de la variable de entorno `SONAR_TOKEN`.

### 5.3 Workflow de GitHub Actions

Crear `.github/workflows/sonarcloud.yml` (usar la versión mayor vigente de cada action):

```yaml
name: Build & SonarCloud

on:
  push:
    branches: [ main ]
  pull_request:
    types: [ opened, synchronize, reopened ]

jobs:
  build-and-analyze:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0              # historial completo: necesario para "new code" y blame

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - uses: gradle/actions/setup-gradle@v4

      - name: Cache SonarCloud packages
        uses: actions/cache@v4
        with:
          path: ~/.sonar/cache
          key: ${{ runner.os }}-sonar
          restore-keys: ${{ runner.os }}-sonar

      - name: Build, test, coverage and analyze
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: ./gradlew clean build sonar --info
```

### 5.4 Ejecutar el análisis desde la máquina local (opcional)

```bash
# Linux / macOS
export SONAR_TOKEN=<tu_token>
./gradlew clean build sonar

# Windows PowerShell
$env:SONAR_TOKEN="<tu_token>"
.\gradlew.bat clean build sonar
```

## 6. Rutas locales de los reportes (tema #1 del taller)

Todas relativas a la raíz del proyecto, después de `./gradlew test`:

| Reporte | Ruta | Para qué |
|---|---|---|
| **JaCoCo HTML** | `build/reports/jacoco/test/html/index.html` | Revisión visual de cobertura por paquete, clase, método, línea y rama |
| JaCoCo XML | `build/reports/jacoco/test/jacocoTestReport.xml` | Lo que importa SonarCloud (`sonar.coverage.jacoco.xmlReportPaths`) |
| JaCoCo binario | `build/jacoco/test.exec` | Datos crudos de ejecución (no se abre directamente) |
| Resultados de tests | `build/reports/tests/test/index.html` | Tests pasados/fallidos y tiempos |

Abrir el reporte HTML:

```bash
# Linux
xdg-open build/reports/jacoco/test/html/index.html
# macOS
open build/reports/jacoco/test/html/index.html
# Windows (PowerShell)
start build\reports\jacoco\test\html\index.html
```

### 6.1 Cómo leer el reporte (incluir en el informe)

- **Missed Instructions / Cov.**: porcentaje de instrucciones bytecode ejecutadas.
- **Missed Branches / Cov.**: porcentaje de ramas (`if`, `&&`, `||`, `?:`, `case`) ejecutadas en ambos sentidos. Es la métrica más cercana a la cobertura de camino del spec 03.
- **Cxty**: la complejidad ciclomática que calcula JaCoCo por método/clase. Compárala con la de Sonar (spec 10): deberían ser iguales o casi.
- En la vista de código fuente: línea **verde** = cubierta, **amarilla** = rama parcialmente cubierta (el rombo indica cuántas de N ramas), **roja** = no ejecutada.

### 6.2 Validación cruzada local ↔ Sonar

Después del primer análisis, comparar la cobertura global de `index.html` con la de *Measures → Coverage* en SonarCloud. Deben coincidir con ±1%. Si no coinciden, revisar que las exclusiones de §3.1 y §5.2 sean equivalentes y que el XML exista antes de ejecutar `sonar`.

## 7. Definición de "hecho"

- `lombok.config` creado; exclusiones idénticas en JaCoCo y Sonar.
- `./gradlew test` genera los 3 reportes en las rutas de §6.
- Workflow en verde y el proyecto en SonarCloud muestra cobertura (no "—").
- Automatic Analysis desactivado; `SONAR_TOKEN` solo en GitHub Secrets.
- Evidencias `01`, `02` y `03` capturadas (spec 04 §6).
