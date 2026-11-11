param([Parameter(ValueFromRemainingArguments=$true)][string[]]$MavenArgs)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$mavenHome = Join-Path $projectRoot '.tools/apache-maven-3.9.9'
if (!(Test-Path -LiteralPath "$mavenHome/bin/mvn.cmd")) {
    $toolsDir = Join-Path $projectRoot '.tools'
    New-Item -ItemType Directory -Force $toolsDir | Out-Null
    $archive = Join-Path $toolsDir 'maven.zip'
    Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile $archive
    Expand-Archive -LiteralPath $archive -DestinationPath $toolsDir -Force
}
& "$mavenHome/bin/mvn.cmd" "-Dmaven.repo.local=$projectRoot/.tools/m2" '-f' "$PSScriptRoot/pom.xml" @MavenArgs
exit $LASTEXITCODE
