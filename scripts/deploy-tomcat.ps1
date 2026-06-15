param(
    [string]$TomcatHome,
    [string]$WarPath = (Join-Path $PSScriptRoot "..\target\sync-ad-maximo-0.1.0-SNAPSHOT.war"),
    [switch]$BuildFirst = $true,
    [switch]$RestartTomcat = $true,
    [switch]$OpenBrowser = $true,
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($TomcatHome)) {
    $TomcatHome = $env:TOMCAT9_HOME
}
if ([string]::IsNullOrWhiteSpace($TomcatHome)) {
    $TomcatHome = $env:TOMCAT_HOME
}
if ([string]::IsNullOrWhiteSpace($TomcatHome)) {
    $TomcatHome = $env:CATALINA_HOME
}

function Resolve-AbsolutePath {
    param([string]$Path)
    return [System.IO.Path]::GetFullPath((Resolve-Path -LiteralPath $Path).Path)
}

function Wait-ForFile {
    param(
        [string]$Path,
        [int]$TimeoutSeconds = 60
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Path -LiteralPath $Path) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Wait-ForHttp {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 120
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                return $true
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

if ($BuildFirst) {
    & (Join-Path $PSScriptRoot "mvn-jdk.ps1") package
    if ($LASTEXITCODE -ne 0) {
        throw "Falló la compilación del WAR."
    }
}

if (-not (Test-Path -LiteralPath $WarPath)) {
    throw "No existe el WAR: $WarPath"
}

$warFullPath = Resolve-Path -LiteralPath $WarPath
$warFileName = Split-Path -Leaf $warFullPath
$contextName = [System.IO.Path]::GetFileNameWithoutExtension($warFileName)

if ([string]::IsNullOrWhiteSpace($TomcatHome)) {
    throw "Define -TomcatHome o la variable TOMCAT9_HOME."
}

if (-not (Test-Path -LiteralPath $TomcatHome)) {
    throw "No existe TomcatHome: $TomcatHome"
}

$binDir = Join-Path $TomcatHome "bin"
$startupBat = Join-Path $binDir "startup.bat"
$shutdownBat = Join-Path $binDir "shutdown.bat"
$webappsDir = Join-Path $TomcatHome "webapps"
$deployWar = Join-Path $webappsDir $warFileName
$explodedDir = Join-Path $webappsDir $contextName

if (-not (Test-Path -LiteralPath $startupBat) -or -not (Test-Path -LiteralPath $shutdownBat)) {
    throw "TomcatHome no parece válido: faltan bin\\startup.bat o bin\\shutdown.bat"
}

if (-not (Test-Path -LiteralPath $webappsDir)) {
    throw "No existe el directorio webapps en $TomcatHome"
}

if ($RestartTomcat) {
    Start-Process -FilePath $shutdownBat -WindowStyle Hidden | Out-Null
    Start-Sleep -Seconds 3
}

if (Test-Path -LiteralPath $deployWar) {
    Remove-Item -LiteralPath $deployWar -Force
}

if (Test-Path -LiteralPath $explodedDir) {
    Remove-Item -LiteralPath $explodedDir -Recurse -Force
}

Copy-Item -LiteralPath $warFullPath -Destination $deployWar -Force

if ($RestartTomcat) {
    Start-Process -FilePath $startupBat -WindowStyle Hidden | Out-Null
}

$baseUrl = "http://localhost:$Port/$contextName"
$loginUrl = "$baseUrl/login"
if ($RestartTomcat) {
    if (-not (Wait-ForHttp -Url $loginUrl -TimeoutSeconds 120)) {
        throw "Tomcat no respondió en $loginUrl dentro del tiempo esperado. Revisa logs en $TomcatHome\logs\catalina*.log"
    }
}

Write-Host "Desplegado: $deployWar"
Write-Host "URL: $loginUrl"

if ($OpenBrowser) {
    Start-Process $loginUrl | Out-Null
}
