$ErrorActionPreference = 'Stop'
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$server = Join-Path $PSScriptRoot 'server/ams-1.0.0.jar'
if (-not (Test-Path -LiteralPath $server)) { throw "Server JAR is missing: $server" }
if (-not $env:DB_PASSWORD) {
    foreach ($candidate in @(
        (Join-Path $PSScriptRoot '.env.postgres'),
        (Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) '.env.postgres')
    )) {
        if (Test-Path -LiteralPath $candidate) {
            foreach ($line in Get-Content -LiteralPath $candidate) {
                if ($line -match '^(DB_PASSWORD|POSTGRES_PORT)=(.+)$') {
                    [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
                }
            }
            break
        }
    }
}
if (-not $env:POSTGRES_PORT) { $env:POSTGRES_PORT = '55432' }
if (-not $env:DB_URL) { $env:DB_URL = "jdbc:postgresql://127.0.0.1:$($env:POSTGRES_PORT)/thapar_ams?currentSchema=academic" }
if (-not $env:DB_USER) { $env:DB_USER = 'ams' }
if (-not $env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE = 'local' }
Set-Location -LiteralPath $PSScriptRoot
& $java -jar $server
exit $LASTEXITCODE
