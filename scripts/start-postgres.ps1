param([switch]$StartBackend, [int]$ServerPort = 8081)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$settings = Join-Path $projectRoot '.env.postgres'
if (-not (Test-Path -LiteralPath $settings)) {
    $adminSecret = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
    $appSecret = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
    @("POSTGRES_ADMIN_PASSWORD=$adminSecret", "DB_PASSWORD=$appSecret", 'POSTGRES_PORT=55432') | Set-Content -LiteralPath $settings
}
foreach ($line in Get-Content -LiteralPath $settings) {
    if ($line -match '^(POSTGRES_ADMIN_PASSWORD|DB_PASSWORD|POSTGRES_PORT)=(.+)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
& docker compose --project-directory $projectRoot --env-file $settings up -d --wait postgres
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL startup failed. Check Docker Desktop and the database logs.' }
$testDbExists = (& docker compose --project-directory $projectRoot --env-file $settings exec -T postgres psql -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname = 'thapar_ams_test'") -join ''
if ($testDbExists.Trim() -ne '1') {
    & docker compose --project-directory $projectRoot --env-file $settings exec -T postgres psql -U postgres -c "CREATE DATABASE thapar_ams_test OWNER ams;" | Out-Null
}
Write-Host "PostgreSQL is ready on 127.0.0.1:$($env:POSTGRES_PORT), database thapar_ams (and thapar_ams_test), user ams, schema academic."
if ($StartBackend) {
    $env:SPRING_PROFILES_ACTIVE = 'local'
    $env:DB_URL = "jdbc:postgresql://127.0.0.1:$($env:POSTGRES_PORT)/thapar_ams?currentSchema=academic"
    $env:DB_USER = 'ams'
    $env:SERVER_PORT = "$ServerPort"
    & "$projectRoot/backend/mvnw.ps1" spring-boot:run
    exit $LASTEXITCODE
}
