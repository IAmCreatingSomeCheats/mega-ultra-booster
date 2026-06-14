<#
.SYNOPSIS
    Build TurboBoost for every supported Minecraft version and collect the jars.

.DESCRIPTION
    Loops the MC_MATRIX in build.gradle, building each version into its own jar,
    then copies the loadable jars (not the -sources jars) into ./dist.
    Run publish-to-modrinth.ps1 afterwards to upload them.
#>
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$versions = '1.21.1', '1.21.4', '1.21.8', '1.21.11'
$dist = Join-Path $root 'dist'

Remove-Item $dist -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $dist | Out-Null

# Clear previous jars (file-level delete avoids the Windows "can't delete build/"
# lock that `gradle clean` can hit). Best-effort.
Get-ChildItem (Join-Path $root 'build\libs') -Filter '*.jar' -ErrorAction SilentlyContinue |
    Remove-Item -Force -ErrorAction SilentlyContinue

foreach ($v in $versions) {
    Write-Host "`n==> Building TurboBoost for Minecraft $v" -ForegroundColor Cyan
    & "$root\gradlew.bat" -p $root build "-Pmcver=$v" --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Build failed for Minecraft $v" }
    Copy-Item (Join-Path $root "build\libs\turboboost-*+mc$v.jar") $dist -Force
}

Write-Host "`nAll versions built ->" -ForegroundColor Green
Get-ChildItem $dist | Select-Object Name, @{N = 'KB'; E = { [math]::Round($_.Length / 1KB) } } | Format-Table -AutoSize
