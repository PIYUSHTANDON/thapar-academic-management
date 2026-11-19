param([string]$FacultyId = 'FACULTY001', [string]$DisplayName = 'Demo Faculty', [int]$ServerPort = 8081)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
& "$PSScriptRoot/start-postgres.ps1"
foreach ($line in Get-Content -LiteralPath (Join-Path $projectRoot '.env.postgres')) {
    if ($line -match '^(DB_PASSWORD|POSTGRES_PORT)=(.+)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
if (-not $env:SEED_FACULTY_PASSWORD) {
    $demoSecret = Read-Host 'Local Faculty demo password (12+ characters)' -AsSecureString
    $env:SEED_FACULTY_PASSWORD = [System.Net.NetworkCredential]::new('', $demoSecret).Password
}
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:DB_URL = "jdbc:postgresql://127.0.0.1:$($env:POSTGRES_PORT)/thapar_ams?currentSchema=academic"
$env:DB_USER = 'ams'
$env:SEED_ENABLED = 'true'
$env:SEED_MODE = 'faculty-demo'
$env:SEED_FACULTY_ID = $FacultyId
$env:SEED_FACULTY_NAME = $DisplayName
$env:DEBUG = 'false'
$env:SERVER_PORT = "$ServerPort"
$javaExecutable = if ($env:JAVA_HOME) { "$($env:JAVA_HOME)/bin/java.exe" } else { 'java' }
if (-not (Test-Path -LiteralPath "$projectRoot/backend/target/ams-1.0.0.jar")) { throw 'Build the backend using backend/mvnw.ps1 package first.' }
try {
    & $javaExecutable -jar "$projectRoot/backend/target/ams-1.0.0.jar"
    if ($LASTEXITCODE -ne 0) { throw "Local demo startup failed. Check for an existing server on port $ServerPort or a seed-account conflict." }
} finally {
    Remove-Item Env:SEED_FACULTY_PASSWORD -ErrorAction SilentlyContinue
}
