<#
.SYNOPSIS
    Upload every TurboBoost jar in ./dist to an existing Modrinth project.

.DESCRIPTION
    Run build-all.ps1 first to produce ./dist/turboboost-<ver>+mc<mc>.jar for each
    supported Minecraft version. This script uploads each one as its own Modrinth
    version (version number "<ver>+mc<mc>", game version <mc>), attaching the
    Fabric API (required) + Sodium/Lithium (optional) dependencies.

    Your token is read from -Token or the MODRINTH_TOKEN environment variable and is
    never printed. Create one at https://modrinth.com/settings/pats
    (scope: "Create versions").

.EXAMPLE
    $env:MODRINTH_TOKEN = "mrp_xxx"
    ./build-all.ps1
    ./publish-to-modrinth.ps1
#>
param(
    [string] $Slug    = "turboboost",
    [string] $DistDir = (Join-Path $PSScriptRoot 'dist'),
    [ValidateSet("release", "beta", "alpha")]
    [string] $Channel = "release",
    [string] $Token   = $env:MODRINTH_TOKEN,
    [switch] $Yes     # skip the confirmation prompt
)

$ErrorActionPreference = "Stop"
$api = "https://api.modrinth.com/v2"

# Well-known Modrinth project ids for dependencies
$FABRIC_API = "P7dR8mSH"
$SODIUM     = "AANobbMI"
$LITHIUM    = "gvQqBUqZ"

if (-not $Token) { throw "No token. Set `$env:MODRINTH_TOKEN or pass -Token. Make one at https://modrinth.com/settings/pats" }

$jars = Get-ChildItem $DistDir -Filter 'turboboost-*+mc*.jar' -ErrorAction SilentlyContinue
if (-not $jars) { throw "No jars in $DistDir. Run ./build-all.ps1 first." }

$ua = "megaultra/turboboost (Modrinth publish script)"
$headers = @{ Authorization = $Token; "User-Agent" = $ua }

# Resolve slug -> project id
Write-Host "Resolving project '$Slug'..." -ForegroundColor Cyan
$project = Invoke-RestMethod -Uri "$api/project/$Slug" -Headers $headers
Write-Host "  -> $($project.title)  (id $($project.id))" -ForegroundColor Green

# Plan: parse mc version + mod version out of each jar name
$plan = foreach ($j in $jars) {
    if ($j.Name -match '^turboboost-(.+)\+mc(.+)\.jar$') {
        [pscustomobject]@{ Jar = $j; ModVer = $Matches[1]; Mc = $Matches[2] }
    }
}

Write-Host "`nAbout to publish $($plan.Count) version(s) to '$($project.title)':" -ForegroundColor Yellow
$plan | ForEach-Object { Write-Host ("  {0,-8}  ->  {1}+mc{0}" -f $_.Mc, $_.ModVer) }
if (-not $Yes) {
    if ((Read-Host "Type 'yes' to upload all") -ne "yes") { Write-Host "Aborted." -ForegroundColor Red; return }
}

foreach ($p in $plan) {
    $versionNumber = "$($p.ModVer)+mc$($p.Mc)"
    $data = [ordered]@{
        name           = "TurboBoost $($p.ModVer) (MC $($p.Mc))"
        version_number = $versionNumber
        changelog      = "TurboBoost $($p.ModVer) for Minecraft $($p.Mc)."
        game_versions  = @($p.Mc)
        version_type   = $Channel
        loaders        = @("fabric")
        featured       = ($p.Mc -eq "1.21.11")   # feature the newest
        project_id     = $project.id
        file_parts     = @("file")
        primary_file   = "file"
        dependencies   = @(
            @{ project_id = $FABRIC_API; dependency_type = "required" },
            @{ project_id = $SODIUM;     dependency_type = "optional" },
            @{ project_id = $LITHIUM;    dependency_type = "optional" }
        )
    }
    $json = $data | ConvertTo-Json -Depth 6 -Compress

    Write-Host "Uploading $versionNumber ..." -ForegroundColor Cyan

    # Upload via curl.exe: Modrinth requires the `data` (JSON) part FIRST and as
    # application/json. PowerShell's -Form can't guarantee field order, so curl is
    # the reliable path. JSON goes through a temp file to avoid shell quoting issues.
    $tmp = New-TemporaryFile
    Set-Content -LiteralPath $tmp.FullName -Value $json -Encoding utf8NoBOM -NoNewline
    $curlArgs = @(
        '-s', '-X', 'POST', "$api/version",
        '-H', "Authorization: $Token",
        '-H', "User-Agent: $ua",
        '-F', "data=@$($tmp.FullName);type=application/json",
        '-F', "file=@$($p.Jar.FullName);type=application/java-archive"
    )
    $out = & curl.exe @curlArgs
    Remove-Item -LiteralPath $tmp.FullName -Force

    $result = $null
    try { $result = $out | ConvertFrom-Json } catch { throw "Modrinth returned non-JSON: $out" }
    if ($result.error) { throw "Modrinth error: $($result.error) — $($result.description)" }
    Write-Host "  ok -> https://modrinth.com/mod/$Slug/version/$($result.version_number)" -ForegroundColor Green
}

Write-Host "`nDone. All versions uploaded." -ForegroundColor Green
