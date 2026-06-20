param(
    [string]$GradleTask = ":app:assembleDebug"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$appDir = Join-Path $repoRoot "app"
$googleServicesPath = Join-Path $appDir "google-services.json"
$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("smartexp-firebase-" + [guid]::NewGuid())
$backupPath = Join-Path $tempDir "google-services.json"
$gradle = if ($env:OS -eq "Windows_NT") {
    Join-Path $repoRoot "gradlew.bat"
} else {
    Join-Path $repoRoot "gradlew"
}
$hadLocalConfig = Test-Path -LiteralPath $googleServicesPath

New-Item -ItemType Directory -Path $tempDir | Out-Null

try {
    if ($hadLocalConfig) {
        Move-Item -LiteralPath $googleServicesPath -Destination $backupPath
        Write-Host "Temporarily moved local app/google-services.json out of the build path."
    }

    & $gradle ":app:clean" $GradleTask
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE."
    }
} finally {
    if (Test-Path -LiteralPath $googleServicesPath) {
        Remove-Item -LiteralPath $googleServicesPath -Force
    }
    if ($hadLocalConfig -and (Test-Path -LiteralPath $backupPath)) {
        Move-Item -LiteralPath $backupPath -Destination $googleServicesPath
        Write-Host "Restored local app/google-services.json."
    }
    if (Test-Path -LiteralPath $tempDir) {
        Remove-Item -LiteralPath $tempDir -Recurse -Force
    }
}
