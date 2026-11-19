$ErrorActionPreference = 'Stop'
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$javafx = Join-Path $PSScriptRoot 'javafx'
$desktop = Join-Path $PSScriptRoot 'ams-desktop-1.0.0.jar'
$libraries = Join-Path $PSScriptRoot 'lib/*'
if (-not (Test-Path -LiteralPath $desktop)) { throw "Desktop JAR is missing: $desktop" }
& $java --module-path $javafx --add-modules javafx.controls -cp "$desktop;$libraries" com.thapar.ams.desktop.DesktopLauncher
exit $LASTEXITCODE
