<#
.SYNOPSIS
    Upload every TurboBoost jar in ./dist to an existing CurseForge project.

.DESCRIPTION
    CurseForge projects must be created (and approved) on the website first — see
    CURSEFORGE.md. This script only uploads files. It looks up CurseForge's numeric
    game-version IDs for each Minecraft version + the Fabric loader, then uploads
    each jar in ./dist as its own file.

    Token:      -Token or $env:CURSEFORGE_TOKEN
                Make one at https://legacy.curseforge.com/account/api-tokens
    ProjectId:  the numeric id shown on your project page / in its URL.

.EXAMPLE
    $env:CURSEFORGE_TOKEN = "xxxxxxxx"
    ./build-all.ps1
    ./publish-to-curseforge.ps1 -ProjectId 123456
#>
param(
    [Parameter(Mandatory)] [int] $ProjectId,
    [string] $DistDir = (Join-Path $PSScriptRoot 'dist'),
    [ValidateSet('release', 'beta', 'alpha')] [string] $Channel = 'release',
    [string] $Token = $env:CURSEFORGE_TOKEN,
    [switch] $Yes
)

$ErrorActionPreference = 'Stop'
$base = 'https://minecraft.curseforge.com'

if (-not $Token) { throw "No token. Set `$env:CURSEFORGE_TOKEN or pass -Token. Make one at https://legacy.curseforge.com/account/api-tokens" }

$jars = Get-ChildItem $DistDir -Filter 'turboboost-*+mc*.jar' -ErrorAction SilentlyContinue
if (-not $jars) { throw "No jars in $DistDir. Run ./build-all.ps1 first." }

$headers = @{ 'X-Api-Token' = $Token }

# 1) Look up CurseForge's numeric game-version ids (MC versions + Fabric loader).
Write-Host "Fetching CurseForge game versions..." -ForegroundColor Cyan
$versions = Invoke-RestMethod -Uri "$base/api/game/versions" -Headers $headers
$fabric = $versions | Where-Object { $_.name -eq 'Fabric' } | Select-Object -First 1
if (-not $fabric) { throw "Couldn't find the 'Fabric' loader id on CurseForge (game/versions)." }

# 2) Build the upload plan from the jar names.
$plan = foreach ($j in $jars) {
    if ($j.Name -match '^turboboost-(.+)\+mc(.+)\.jar$') {
        $mc = $Matches[2]
        $mcEntry = $versions | Where-Object { $_.name -eq $mc } | Select-Object -First 1
        if (-not $mcEntry) { throw "CurseForge has no game version named '$mc'. Check game/versions." }
        [pscustomobject]@{
            Jar            = $j
            ModVer         = $Matches[1]
            Mc             = $mc
            GameVersionIds = @([int]$mcEntry.id, [int]$fabric.id)
        }
    }
}

Write-Host "`nAbout to upload $($plan.Count) file(s) to CurseForge project ${ProjectId}:" -ForegroundColor Yellow
$plan | ForEach-Object { Write-Host ("  {0,-8}  game-version ids: {1}" -f $_.Mc, ($_.GameVersionIds -join ', ')) }
if (-not $Yes) {
    if ((Read-Host "Type 'yes' to upload all") -ne 'yes') { Write-Host 'Aborted.' -ForegroundColor Red; return }
}

# 3) Upload each jar (curl.exe keeps the multipart 'metadata' part first + JSON type).
foreach ($p in $plan) {
    $meta = [ordered]@{
        changelog     = "TurboBoost $($p.ModVer) for Minecraft $($p.Mc)."
        changelogType = 'markdown'
        displayName   = "TurboBoost $($p.ModVer) (MC $($p.Mc))"
        gameVersions  = $p.GameVersionIds
        releaseType   = $Channel
    }
    $json = $meta | ConvertTo-Json -Depth 6 -Compress
    $tmp = New-TemporaryFile
    Set-Content -LiteralPath $tmp.FullName -Value $json -Encoding utf8NoBOM -NoNewline

    Write-Host "Uploading MC $($p.Mc) ..." -ForegroundColor Cyan
    $curlArgs = @(
        '-s', '-X', 'POST', "$base/api/projects/$ProjectId/upload-file",
        '-H', "X-Api-Token: $Token",
        '-F', "metadata=@$($tmp.FullName);type=application/json",
        '-F', "file=@$($p.Jar.FullName);type=application/java-archive"
    )
    $out = & curl.exe @curlArgs
    Remove-Item -LiteralPath $tmp.FullName -Force

    $res = $null
    try { $res = $out | ConvertFrom-Json } catch { throw "CurseForge returned non-JSON: $out" }
    if ($res.errorCode -or $res.errorMessage) { throw "CurseForge error $($res.errorCode): $($res.errorMessage)" }
    Write-Host "  ok -> file id $($res.id)" -ForegroundColor Green
}

Write-Host "`nDone. Check the Files tab of your CurseForge project." -ForegroundColor Green
