param([string]$OutputDirectory)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $projectRoot 'dist/java-desktop' }
$projectRoot = [System.IO.Path]::GetFullPath($projectRoot)
$OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
$workspaceRoot = [System.IO.Path]::GetFullPath((Split-Path $projectRoot -Parent))
if (-not $OutputDirectory.StartsWith($projectRoot + [System.IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw "OutputDirectory must be inside $projectRoot"
}
if (-not $projectRoot.StartsWith($workspaceRoot + [System.IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Project root is outside the workspace.'
}

& "$projectRoot/backend/mvnw.ps1" package
if ($LASTEXITCODE -ne 0) { throw 'Backend Maven package failed.' }
$dependencies = Join-Path $projectRoot 'desktop/target/runtime-dependencies'
& "$projectRoot/desktop/mvnw.ps1" package dependency:copy-dependencies '-DincludeScope=runtime' "-DoutputDirectory=$dependencies"
if ($LASTEXITCODE -ne 0) { throw 'Desktop Maven package or dependency copy failed.' }

$backendJar = Join-Path $projectRoot 'backend/target/ams-1.0.0.jar'
$desktopJar = Join-Path $projectRoot 'desktop/target/ams-desktop-1.0.0.jar'
foreach ($required in @($backendJar, $desktopJar, $dependencies)) {
    if (-not (Test-Path -LiteralPath $required)) { throw "Missing build output: $required" }
}
$javafxJars = @(Get-ChildItem -LiteralPath $dependencies -Filter 'javafx-*-win.jar' -File)
if ($javafxJars.Count -ne 3) { throw 'Expected the three Windows JavaFX runtime JARs (base, graphics, controls).' }

if (Test-Path -LiteralPath $OutputDirectory) {
    Remove-Item -LiteralPath $OutputDirectory -Recurse -Force
}
New-Item -ItemType Directory -Path $OutputDirectory, "$OutputDirectory/server", "$OutputDirectory/javafx", "$OutputDirectory/lib" | Out-Null
Copy-Item -LiteralPath $backendJar -Destination "$OutputDirectory/server/ams-1.0.0.jar"
Copy-Item -LiteralPath $desktopJar -Destination "$OutputDirectory/ams-desktop-1.0.0.jar"
foreach ($jar in Get-ChildItem -LiteralPath $dependencies -Filter '*.jar' -File) {
    if ($jar.Name -match '^javafx-.*-win\.jar$') {
        Copy-Item -LiteralPath $jar.FullName -Destination "$OutputDirectory/javafx/$($jar.Name)"
    } elseif ($jar.Name -notmatch '^javafx-') {
        Copy-Item -LiteralPath $jar.FullName -Destination "$OutputDirectory/lib/$($jar.Name)"
    }
}
Copy-Item -LiteralPath "$PSScriptRoot/run-packaged-desktop.ps1" -Destination "$OutputDirectory/run-desktop.ps1"
Copy-Item -LiteralPath "$PSScriptRoot/run-packaged-server.ps1" -Destination "$OutputDirectory/run-server.ps1"
Write-Host "Java desktop distribution: $OutputDirectory"
