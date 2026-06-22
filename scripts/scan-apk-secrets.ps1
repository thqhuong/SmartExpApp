param(
    [string]$ApkPath = "app/build/outputs/apk/debug/app-debug.apk"
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$resolvedApk = if ([System.IO.Path]::IsPathRooted($ApkPath)) {
    $ApkPath
} else {
    Join-Path $repoRoot $ApkPath
}

if (-not (Test-Path -LiteralPath $resolvedApk)) {
    throw "APK not found: $resolvedApk"
}

$patterns = @(
    "GEMINI_API_KEY",
    "CLOUDFLARE_API_TOKEN",
    "AIza[0-9A-Za-z_-]{20,}",
    "sk-[A-Za-z0-9]{20,}",
    "cloudflare[_-]?api[_-]?token",
    "Gemini API",
    "Gemini Live",
    "GEMINI_",
    "bg_gemini",
    "ic_gemini",
    "gemini_smart_tip",
    "start_gemini"
)

$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("smartexp-apk-scan-" + [guid]::NewGuid())
$zipPath = Join-Path $tempDir "app.zip"
$extractDir = Join-Path $tempDir "unzipped"
$matches = New-Object System.Collections.Generic.List[string]
$latin1 = [System.Text.Encoding]::GetEncoding("ISO-8859-1")

New-Item -ItemType Directory -Path $tempDir | Out-Null

try {
    Copy-Item -LiteralPath $resolvedApk -Destination $zipPath
    Expand-Archive -Path $zipPath -DestinationPath $extractDir -Force

    foreach ($file in Get-ChildItem -Path $extractDir -Recurse -File) {
        $text = $latin1.GetString([System.IO.File]::ReadAllBytes($file.FullName))
        foreach ($pattern in $patterns) {
            if ([regex]::IsMatch($text, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
                $relative = $file.FullName.Substring($extractDir.Length).TrimStart([char[]]@("\", "/"))
                $matches.Add("$relative matched /$pattern/")
            }
        }
    }

    if ($matches.Count -gt 0) {
        $matches | Sort-Object -Unique | ForEach-Object { Write-Error $_ }
        throw "Potential secret or stale AI provider string found in APK."
    }

    Write-Host "APK secret scan passed: no stale Gemini app strings, Cloudflare token, Firebase key, or provider-key patterns found."
} finally {
    if (Test-Path -LiteralPath $tempDir) {
        Remove-Item -LiteralPath $tempDir -Recurse -Force
    }
}
