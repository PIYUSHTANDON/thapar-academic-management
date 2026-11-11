param([string]$FacultyId = 'FACULTY001', [string]$DisplayName = 'Demo Faculty')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
if (-not $env:SEED_FACULTY_PASSWORD) {
    $demoSecret = Read-Host 'Local Faculty demo password (12+ characters)' -AsSecureString
    $env:SEED_FACULTY_PASSWORD = [System.Net.NetworkCredential]::new('', $demoSecret).Password
}
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:SPRING_DATASOURCE_URL = 'jdbc:h2:file:./data/ams;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0'
$env:SEED_ENABLED = 'true'
$env:SEED_MODE = 'faculty-demo'
$env:SEED_FACULTY_ID = $FacultyId
$env:SEED_FACULTY_NAME = $DisplayName
$env:DEBUG = 'false'
$env:SERVER_PORT = '8080'
$portableJdk = Get-ChildItem -LiteralPath "$projectRoot/.tools/jdk21" -Directory -ErrorAction SilentlyContinue | Where-Object { Test-Path -LiteralPath "$($_.FullName)/bin/java.exe" } | Select-Object -First 1
if ($portableJdk) { $javaExecutable = "$($portableJdk.FullName)/bin/java.exe" } elseif ($env:JAVA_HOME) { $javaExecutable = "$env:JAVA_HOME/bin/java.exe" } else { $javaExecutable = 'java' }
if (-not (Test-Path -LiteralPath "$projectRoot/backend/target/ams-1.0.0.jar")) { throw 'Build the backend using backend/mvnw.ps1 package first.' }
try {
    & $javaExecutable -jar "$projectRoot/backend/target/ams-1.0.0.jar"
    if ($LASTEXITCODE -ne 0) { throw 'Local demo startup failed. Check for an existing server on port 8080 or a seed-account conflict.' }
} finally {
    Remove-Item Env:SEED_FACULTY_PASSWORD -ErrorAction SilentlyContinue
}
