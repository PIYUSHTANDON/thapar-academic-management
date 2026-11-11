param([string]$StudentId = 'STUDENT001', [string]$DisplayName = 'Demo Student', [string]$FacultyId = 'FACULTY001', [switch]$Restart)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
if (-not $env:SEED_STUDENT_PASSWORD) {
    $demoSecret = Read-Host 'Local Student demo password (12+ characters)' -AsSecureString
    $env:SEED_STUDENT_PASSWORD = [System.Net.NetworkCredential]::new('', $demoSecret).Password
}
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:SPRING_DATASOURCE_URL = 'jdbc:h2:file:./data/ams;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0'
$env:SEED_ENABLED = 'true'
$env:SEED_MODE = 'student-demo'
$env:SEED_FACULTY_ID = $FacultyId
$env:SEED_STUDENT_NAME = $DisplayName
$env:SEED_STUDENT_ID = $StudentId
$env:DEBUG = 'false'
$env:SERVER_PORT = '8080'
$portableJdk = Get-ChildItem -LiteralPath "$projectRoot/.tools/jdk21" -Directory -ErrorAction SilentlyContinue | Where-Object { Test-Path -LiteralPath "$($_.FullName)/bin/java.exe" } | Select-Object -First 1
if ($portableJdk) { $javaExecutable = "$($portableJdk.FullName)/bin/java.exe" } elseif ($env:JAVA_HOME) { $javaExecutable = "$env:JAVA_HOME/bin/java.exe" } else { $javaExecutable = 'java' }
if (-not (Test-Path -LiteralPath "$projectRoot/backend/target/ams-1.0.0.jar")) { throw 'Build the backend using backend/mvnw.ps1 package first.' }

if ($Restart) {
    $listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        $serverProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)"
        if ($serverProcess.Name -ne 'java.exe' -or $serverProcess.CommandLine -notlike "*$projectRoot*" -or $serverProcess.CommandLine -notlike '*ams-1.0.0.jar*') { throw 'Port 8080 is used by an unrecognized process; stop the project backend manually before proceeding.' }
        Stop-Process -Id $serverProcess.ProcessId
        Wait-Process -Id $serverProcess.ProcessId -Timeout 10 -ErrorAction SilentlyContinue
    }
}

try {
    & $javaExecutable -jar "$projectRoot/backend/target/ams-1.0.0.jar"
    if ($LASTEXITCODE -ne 0) { throw 'Local demo startup failed. Check for an existing server on port 8080 or a seed-account conflict.' }
} finally {
    Remove-Item Env:SEED_STUDENT_PASSWORD -ErrorAction SilentlyContinue
}
