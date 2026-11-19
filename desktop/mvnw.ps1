param([Parameter(ValueFromRemainingArguments=$true)][string[]]$MavenArgs)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$versionOnly = ($MavenArgs.Count -eq 1 -and ($MavenArgs[0] -eq '-version' -or $MavenArgs[0] -eq '--version'))
if (-not $versionOnly) {
    & "$projectRoot/mvnw.ps1" -pl contracts -am install -q
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
$maven = Join-Path $projectRoot '.tools/apache-maven-3.9.9/bin/mvn.cmd'
if (!(Test-Path -LiteralPath $maven)) {
    & "$projectRoot/mvnw.ps1" -version | Out-Null
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
& $maven "-Dmaven.repo.local=$projectRoot/.tools/m2" '-f' "$PSScriptRoot/pom.xml" @MavenArgs
exit $LASTEXITCODE
