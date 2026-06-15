param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs = @("test")
)

$preferredJavaHomes = @(
    "C:\Program Files\Java\jdk-24",
    "C:\Program Files\Java\jdk-21",
    "C:\Program Files\Java\jdk-17",
    "C:\Program Files\Java\jdk-11",
    "C:\Program Files (x86)\Java\jdk-24",
    "C:\Program Files (x86)\Java\jdk-21",
    "C:\Program Files (x86)\Java\jdk-17",
    "C:\Program Files (x86)\Java\jdk-11"
)

if ($env:JAVA_HOME) {
    $preferredJavaHomes += $env:JAVA_HOME
}

$javaHome = $null
foreach ($candidate in $preferredJavaHomes) {
    if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path (Join-Path $candidate "bin\java.exe"))) {
        $javaHome = $candidate
        break
    }
}

if (-not $javaHome) {
    throw "No se encontró un JDK válido. Instala Java 11+ o define JAVA_HOME antes de ejecutar este script."
}

$mvnCmd = Get-Command mvn -ErrorAction Stop

$env:JAVA_HOME = $javaHome
$env:PATH = "$(Join-Path $javaHome 'bin');$($env:PATH)"

Write-Host "Usando JAVA_HOME=$javaHome"
Write-Host "Ejecutando: mvn $($MavenArgs -join ' ')"

& $mvnCmd.Source @MavenArgs
exit $LASTEXITCODE
