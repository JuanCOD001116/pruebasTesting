<#
.SYNOPSIS
    Verifica el proyecto Task Manager API en un solo paso.

.DESCRIPTION
    1. Comprueba requisitos (JDK 17+, raiz del repo, estado de git).
    2. Ejecuta las pruebas unitarias/integracion con cobertura y las de aceptacion (Karate).
    3. Muestra un resumen: pruebas, cobertura, complejidad, Karate y SonarCloud (API publica).
    4. Abre los reportes en el navegador y guarda el resumen en build\verificacion.

    Nunca ejecuta el analisis de SonarCloud ni usa tokens: solo consulta la API publica.

.PARAMETER Rapido
    Solo pruebas unitarias e integracion (sin Karate).

.PARAMETER SoloKarate
    Solo pruebas de aceptacion con Karate.

.PARAMETER SinNavegador
    No abre ningun reporte en el navegador.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\verificar-todo.ps1 -Rapido -SinNavegador
#>
[CmdletBinding()]
param(
    [switch]$Rapido,
    [switch]$SoloKarate,
    [switch]$SinNavegador
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

$SonarProjectKey = 'JuanCOD001116_pruebasTesting'
$SonarApi = 'https://sonarcloud.io/api'
$SonarDashboard = "https://sonarcloud.io/summary/overall?id=$SonarProjectKey"
$UmbralComplejidad = 10
# Gradle 8.12 puede ejecutarse con Java 17 a 23
$JavaMinimo = 17
$JavaMaximoGradle = 23

try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { Write-Verbose 'Consola sin soporte UTF-8' }

$script:Resumen = New-Object System.Collections.Generic.List[string]
$script:Fallos = New-Object System.Collections.Generic.List[string]
$script:Avisos = New-Object System.Collections.Generic.List[string]
$script:Etapas = New-Object System.Collections.Generic.List[object]

# ---------------------------------------------------------------- utilidades de salida

function Write-Linea {
    param([string]$Texto = '', [string]$Color = 'Gray', [switch]$SinResumen)
    Write-Host $Texto -ForegroundColor $Color
    if (-not $SinResumen) { $script:Resumen.Add($Texto) }
}

function Write-Titulo {
    param([string]$Texto)
    Write-Linea ''
    Write-Linea ('=' * 72) 'Cyan'
    Write-Linea "  $Texto" 'Cyan'
    Write-Linea ('=' * 72) 'Cyan'
}

function Write-Ok { param([string]$Texto) Write-Linea "  [OK]    $Texto" 'Green' }

function Write-Aviso {
    param([string]$Texto)
    Write-Linea "  [AVISO] $Texto" 'Yellow'
    $script:Avisos.Add($Texto)
}

function Write-Fallo {
    param([string]$Texto)
    Write-Linea "  [FALLO] $Texto" 'Red'
    $script:Fallos.Add($Texto)
}

function Format-Duracion {
    param([TimeSpan]$Duracion)
    if ($Duracion.TotalMinutes -ge 1) {
        return ('{0} min {1:00} s' -f [int][Math]::Floor($Duracion.TotalMinutes), $Duracion.Seconds)
    }
    return ('{0:N1} s' -f $Duracion.TotalSeconds)
}

function Format-Porcentaje {
    param([double]$Cubierto, [double]$Total)
    if ($Total -le 0) { return 'n/d' }
    return ('{0:N1} %' -f (100.0 * $Cubierto / $Total))
}

function Read-XmlSeguro {
    param([string]$Ruta)
    # Los XML de JaCoCo declaran un DTD externo: se ignora para no intentar descargarlo
    $settings = New-Object System.Xml.XmlReaderSettings
    $settings.DtdProcessing = [System.Xml.DtdProcessing]::Ignore
    $settings.XmlResolver = $null
    $reader = [System.Xml.XmlReader]::Create($Ruta, $settings)
    try {
        $doc = New-Object System.Xml.XmlDocument
        $doc.Load($reader)
        return $doc
    } finally {
        $reader.Close()
    }
}

function Invoke-Comando {
    # Ejecuta un programa externo y devuelve su salida (stdout + stderr) sin que
    # PowerShell 5.1 convierta stderr en errores.
    param([string]$Programa, [string]$Argumentos)
    $salida = & cmd.exe /c "`"$Programa`" $Argumentos 2>&1"
    return @($salida | ForEach-Object { "$_" })
}

# ---------------------------------------------------------------- 1. requisitos

function Get-VersionJava {
    param([string]$JavaExe)
    $lineas = Invoke-Comando $JavaExe '-version'
    foreach ($linea in $lineas) {
        if ($linea -match 'version "(\d+)(\.(\d+))?') {
            $mayor = [int]$Matches[1]
            # Java 8 y anteriores informan "1.8.0_x"
            if ($mayor -eq 1 -and $Matches[3]) { $mayor = [int]$Matches[3] }
            return $mayor
        }
    }
    return $null
}

function Test-Requisitos {
    param([string]$Raiz)
    Write-Titulo 'Requisitos'

    $faltan = @('build.gradle', 'settings.gradle', 'gradlew.bat') |
        Where-Object { -not (Test-Path (Join-Path $Raiz $_)) }
    if ($faltan) {
        Write-Fallo "No estas en la raiz del repositorio (faltan: $($faltan -join ', '))."
        return $false
    }
    Write-Ok "Raiz del repositorio: $Raiz"

    $javaExe = $null
    $origen = ''
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        $javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
        $origen = "JAVA_HOME ($env:JAVA_HOME)"
    } else {
        $cmd = Get-Command java.exe -ErrorAction SilentlyContinue
        if ($cmd) {
            $javaExe = $cmd.Source
            $origen = "PATH ($javaExe)"
        }
    }
    if (-not $javaExe) {
        Write-Fallo 'No se encontro Java. Instala un JDK 17 y define JAVA_HOME.'
        return $false
    }

    $version = Get-VersionJava $javaExe
    if ($null -eq $version) {
        Write-Aviso "No se pudo leer la version de Java en $origen."
    } elseif ($version -lt $JavaMinimo) {
        Write-Fallo "Java $version en ${origen}: se necesita JDK $JavaMinimo o superior."
        return $false
    } elseif ($version -gt $JavaMaximoGradle) {
        Write-Aviso ("Java $version en $origen no esta soportado por Gradle 8.12 (17-$JavaMaximoGradle). " +
            'El daemon usara el JDK 17 de gradle-daemon-jvm.properties si esta instalado; ' +
            'si Gradle falla, apunta JAVA_HOME a un JDK 17 o 21.')
    } else {
        Write-Ok "Java $version ($origen)"
    }

    if (Get-Command git.exe -ErrorAction SilentlyContinue) {
        $rama = (Invoke-Comando 'git' 'rev-parse --abbrev-ref HEAD') | Select-Object -First 1
        $commit = (Invoke-Comando 'git' 'log -1 --format="%h %s (%cr)"') | Select-Object -First 1
        Write-Ok "Rama: $rama"
        Write-Ok "Ultimo commit: $commit"
        $cambios = @(Invoke-Comando 'git' 'status --porcelain' | Where-Object { $_ })
        if ($cambios.Count -gt 0) {
            Write-Aviso "Hay $($cambios.Count) archivo(s) con cambios sin commitear (se verifica igual)."
        }
    } else {
        Write-Aviso 'git no esta disponible: no se muestra rama ni ultimo commit.'
    }
    return $true
}

# ---------------------------------------------------------------- 2. etapas de Gradle

function Invoke-Etapa {
    param([string]$Titulo, [string]$Raiz, [string[]]$Tareas)
    Write-Titulo "Etapa: $Titulo"
    Write-Linea ("  gradlew.bat " + ($Tareas -join ' ')) 'DarkGray'
    $reloj = [System.Diagnostics.Stopwatch]::StartNew()
    & (Join-Path $Raiz 'gradlew.bat') @Tareas '--console=plain'
    $codigo = $LASTEXITCODE
    $reloj.Stop()
    $duracion = Format-Duracion $reloj.Elapsed
    $paso = ($codigo -eq 0)
    $script:Etapas.Add([pscustomobject]@{ Titulo = $Titulo; Paso = $paso; Duracion = $duracion })
    if ($paso) {
        Write-Ok "$Titulo ($duracion)"
    } else {
        Write-Fallo "$Titulo termino con codigo $codigo ($duracion)"
    }
}

# ---------------------------------------------------------------- 3. resumen

function Show-PruebasUnitarias {
    param([string]$Raiz)
    Write-Linea ''
    Write-Linea 'Pruebas unitarias e integracion' 'White'
    $archivos = @(Get-ChildItem (Join-Path $Raiz 'build\test-results\test') -Filter '*.xml' -ErrorAction SilentlyContinue)
    if ($archivos.Count -eq 0) {
        Write-Fallo 'No hay resultados en build\test-results\test.'
        return
    }
    $total = 0; $fallidas = 0; $omitidas = 0
    foreach ($archivo in $archivos) {
        $suite = (Read-XmlSeguro $archivo.FullName).DocumentElement
        $total += [int]$suite.tests
        $fallidas += [int]$suite.failures + [int]$suite.errors
        $omitidas += [int]$suite.skipped
    }
    $pasadas = $total - $fallidas - $omitidas
    Write-Linea ("  Clases: {0}   Total: {1}   Pasadas: {2}   Fallidas: {3}   Omitidas: {4}" -f
        $archivos.Count, $total, $pasadas, $fallidas, $omitidas)
    if ($fallidas -gt 0) { Write-Fallo "$fallidas prueba(s) unitaria(s)/integracion fallaron." }
}

function Show-Cobertura {
    param([string]$Raiz)
    Write-Linea ''
    Write-Linea 'Cobertura JaCoCo' 'White'
    $ruta = Join-Path $Raiz 'build\reports\jacoco\test\jacocoTestReport.xml'
    if (-not (Test-Path $ruta)) {
        Write-Fallo 'No existe el reporte de cobertura (jacocoTestReport.xml); Gradle no lo genera si fallan pruebas.'
        return
    }
    $reporte = Read-XmlSeguro $ruta
    $contadores = @{}
    foreach ($c in $reporte.DocumentElement.SelectNodes('counter')) {
        $contadores[$c.type] = @([double]$c.missed, [double]$c.covered)
    }
    foreach ($par in @(@('INSTRUCTION', 'Instrucciones'), @('BRANCH', 'Ramas'), @('LINE', 'Lineas'), @('METHOD', 'Metodos'))) {
        $valor = 'n/d'
        if ($contadores.ContainsKey($par[0])) {
            $m = $contadores[$par[0]]
            $valor = Format-Porcentaje $m[1] ($m[0] + $m[1])
        }
        Write-Linea ("  {0,-14} {1}" -f $par[1], $valor)
    }

    Write-Linea ''
    Write-Linea "Top 5 de complejidad ciclomatica por metodo (umbral $UmbralComplejidad)" 'White'
    $metodos = foreach ($clase in $reporte.SelectNodes('//class')) {
        $nombreClase = ($clase.name -split '/')[-1]
        foreach ($metodo in $clase.SelectNodes('method')) {
            if ($metodo.name -like 'lambda$*') { continue }
            $cxty = $metodo.SelectSingleNode("counter[@type='COMPLEXITY']")
            if ($cxty) {
                [pscustomobject]@{
                    Nombre = "$nombreClase.$($metodo.name)"
                    Valor = [int]$cxty.missed + [int]$cxty.covered
                }
            }
        }
    }
    $top = @($metodos | Sort-Object Valor -Descending | Select-Object -First 5)
    foreach ($m in $top) {
        $color = 'Gray'
        if ($m.Valor -gt $UmbralComplejidad) { $color = 'Red' }
        Write-Linea ("  {0,3}  {1}" -f $m.Valor, $m.Nombre) $color
    }
    $altos = @($metodos | Where-Object { $_.Valor -gt $UmbralComplejidad })
    if ($altos.Count -gt 0) {
        Write-Fallo "$($altos.Count) metodo(s) superan complejidad ciclomatica $UmbralComplejidad."
    } else {
        Write-Ok "Ningun metodo supera complejidad ciclomatica $UmbralComplejidad."
    }
}

function Show-Karate {
    param([string]$Raiz)
    Write-Linea ''
    Write-Linea 'Pruebas de aceptacion (Karate)' 'White'
    $ruta = Join-Path $Raiz 'build\karate-reports\karate-summary-json.txt'
    if (-not (Test-Path $ruta)) {
        Write-Fallo 'No existe el resumen de Karate (build\karate-reports\karate-summary-json.txt).'
        return
    }
    $k = Get-Content $ruta -Raw -Encoding UTF8 | ConvertFrom-Json
    $features = [int]$k.featuresPassed + [int]$k.featuresFailed
    Write-Linea ("  Features: {0}   Escenarios pasados: {1}   Fallidos: {2}   Tiempo: {3:N1} s" -f
        $features, $k.scenariosPassed, $k.scenariosfailed, ([double]$k.elapsedTime / 1000))
    if ([int]$k.scenariosfailed -gt 0) { Write-Fallo "$($k.scenariosfailed) escenario(s) de Karate fallaron." }
}

function Show-SonarCloud {
    Write-Linea ''
    Write-Linea "SonarCloud (rama main, API publica, sin token)" 'White'
    try {
        # Windows PowerShell 5.1 no negocia TLS 1.2 por defecto
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $gate = Invoke-RestMethod -Uri "$SonarApi/qualitygates/project_status?projectKey=$SonarProjectKey" -TimeoutSec 20
        $metricas = 'coverage,violations,bugs,vulnerabilities,code_smells,complexity,cognitive_complexity'
        $medidas = Invoke-RestMethod -Uri "$SonarApi/measures/component?component=$SonarProjectKey&metricKeys=$metricas" -TimeoutSec 20
    } catch {
        Write-Aviso "No se pudo consultar SonarCloud (sin internet o API no disponible): $($_.Exception.Message)"
        return
    }
    $valores = @{}
    foreach ($m in $medidas.component.measures) { $valores[$m.metric] = $m.value }
    $estado = $gate.projectStatus.status
    $color = 'Green'
    if ($estado -ne 'OK') { $color = 'Red' }
    Write-Linea "  Quality Gate:            $estado" $color
    $etiquetas = [ordered]@{
        coverage = 'Cobertura (%)'; violations = 'Issues abiertos'; bugs = 'Bugs'
        vulnerabilities = 'Vulnerabilidades'; code_smells = 'Code smells'
        complexity = 'Complejidad ciclomatica'; cognitive_complexity = 'Complejidad cognitiva'
    }
    foreach ($clave in $etiquetas.Keys) {
        $valor = 'n/d'
        if ($valores.ContainsKey($clave)) { $valor = $valores[$clave] }
        Write-Linea ("  {0,-24} {1}" -f ($etiquetas[$clave] + ':'), $valor)
    }
    if ($estado -ne 'OK') { Write-Fallo "El Quality Gate de main en SonarCloud esta en $estado." }
}

# ---------------------------------------------------------------- 4. navegador y 5. archivo

function Open-Reportes {
    param([string]$Raiz, [bool]$UnitariasEjecutadas, [bool]$KarateEjecutado)
    $rutas = @()
    if ($UnitariasEjecutadas) {
        $rutas += 'build\reports\tests\test\index.html'
        $rutas += 'build\reports\jacoco\test\html\index.html'
    }
    if ($KarateEjecutado) {
        $rutas += 'build\karate-reports\karate-summary.html'
        $rutas += 'build\reports\tests\acceptanceTest\index.html'
    }
    Write-Titulo 'Reportes'
    foreach ($relativa in $rutas) {
        $completa = Join-Path $Raiz $relativa
        if (Test-Path $completa) {
            Start-Process $completa
            Write-Linea "  Abierto: $relativa"
        }
    }
    Start-Process $SonarDashboard
    Write-Linea "  Abierto: $SonarDashboard"
}

function Save-Resumen {
    param([string]$Raiz)
    $carpeta = Join-Path $Raiz 'build\verificacion'
    New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
    $archivo = Join-Path $carpeta 'resumen-verificacion.txt'
    $cabecera = @("Verificacion del proyecto - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')", '')
    $contenido = $cabecera + $script:Resumen
    [System.IO.File]::WriteAllLines($archivo, [string[]]$contenido, (New-Object System.Text.UTF8Encoding $true))
    Write-Host ''
    Write-Host "Resumen guardado en: $archivo" -ForegroundColor Cyan
}

# ---------------------------------------------------------------- programa principal

if ($Rapido -and $SoloKarate) {
    Write-Host 'Las opciones -Rapido y -SoloKarate no se pueden usar juntas.' -ForegroundColor Red
    exit 2
}

$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz
$inicio = Get-Date
$modo = 'completo'
if ($Rapido) { $modo = 'rapido (sin Karate)' }
if ($SoloKarate) { $modo = 'solo Karate' }

Write-Titulo "Verificacion de Task Manager API - modo $modo"
Write-Linea "  Fecha: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

if (-not (Test-Requisitos $raiz)) {
    Write-Linea ''
    Write-Linea 'No se cumplen los requisitos minimos; se detiene la verificacion.' 'Red'
    Save-Resumen $raiz
    exit 1
}

$ejecutarUnitarias = -not $SoloKarate
$ejecutarKarate = -not $Rapido

if ($ejecutarUnitarias) {
    Invoke-Etapa 'Pruebas unitarias e integracion + cobertura' $raiz `
        @('clean', 'test', 'jacocoTestReport', 'jacocoTestCoverageVerification', '--continue')
}
if ($ejecutarKarate) {
    # cleanAcceptanceTest obliga a ejecutar Karate aunque Gradle lo considere al dia
    Invoke-Etapa 'Pruebas de aceptacion (Karate)' $raiz @('cleanAcceptanceTest', 'acceptanceTest')
}

Write-Titulo 'RESUMEN'
Write-Linea 'Etapas' 'White'
foreach ($etapa in $script:Etapas) {
    $estado = 'FALLO'
    $color = 'Red'
    if ($etapa.Paso) { $estado = 'OK'; $color = 'Green' }
    Write-Linea ("  {0,-6} {1,-48} {2}" -f $estado, $etapa.Titulo, $etapa.Duracion) $color
}

if ($ejecutarUnitarias) {
    Show-PruebasUnitarias $raiz
    Show-Cobertura $raiz
} else {
    Write-Linea ''
    Write-Linea 'Pruebas unitarias y cobertura: no ejecutadas en modo -SoloKarate.' 'DarkGray'
}
if ($ejecutarKarate) {
    Show-Karate $raiz
} else {
    Write-Linea ''
    Write-Linea 'Karate: no ejecutado en modo -Rapido.' 'DarkGray'
}
Show-SonarCloud

Write-Titulo 'VEREDICTO'
if ($script:Fallos.Count -eq 0) {
    Write-Linea '  TODO OK' 'Green'
} else {
    Write-Linea "  Fallaron $($script:Fallos.Count) punto(s):" 'Red'
    foreach ($fallo in $script:Fallos) { Write-Linea "   - $fallo" 'Red' }
}
if ($script:Avisos.Count -gt 0) {
    Write-Linea "  Avisos ($($script:Avisos.Count)):" 'Yellow'
    foreach ($aviso in $script:Avisos) { Write-Linea "   - $aviso" 'Yellow' }
}
Write-Linea ("  Tiempo total: {0}" -f (Format-Duracion ((Get-Date) - $inicio)))

Save-Resumen $raiz
if (-not $SinNavegador) {
    Open-Reportes $raiz $ejecutarUnitarias $ejecutarKarate
}

if ($script:Fallos.Count -gt 0) { exit 1 }
exit 0
