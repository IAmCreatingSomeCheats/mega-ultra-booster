<#
.SYNOPSIS
    Upload a new TurboBoost version to an existing Modrinth project via the API.

.DESCRIPTION
    Use this for version *updates* after you've created the project once on the
    website (see PUBLISHING.md). It resolves your project slug to its id, attaches
    the Fabric API (required) + Sodium/Lithium (optional) dependencies, and uploads
    the jar.

    Your token is read from -Token or the MODRINTH_TOKEN environment variable and is
    never printed. Create one at: https://modrinth.com/settings/pats
    (scope: "Create versions").

.EXAMPLE
    $env:MODRINTH_TOKEN = "mrp_xxx"
    ./publish-to-modrinth.ps1 -Version 1.0.1 -GameVersions 1.21.11 -Changelog "Fixed X"
#>
param(
    [string]   $Slug         = "turboboost",
    [string]   $Version      = "1.0.0",
    [string]   $Jar          = "build/libs/turboboost-1.0.0.jar",
    [string[]] $GameVersions = @("1.21.11"),
    [string[]] $Loaders      = @("fabric"),
    [ValidateSet("release","beta","alpha")]
    [string]   $Channel      = "release",
    [string]   $Changelog    = "New TurboBoost release.",
    [string]   $Token        = $env:MODRINTH_TOKEN,
    [switch]   $Yes          # skip the confirmation prompt
)

$ErrorActionPreference = "Stop"
$api = "https://api.modrinth.com/v2"
$ua  = "megaultra/turboboost/$Version (Modrinth publish script)"

# Well-known Modrinth project ids for dependencies
$FABRIC_API = "P7dR8mSH"
$SODIUM     = "AANobbMI"
$LITHIUM    = "gvQqBUqZ"

if (-not $Token)            { throw "No token. Set `$env:MODRINTH_TOKEN or pass -Token. Make one at https://modrinth.com/settings/pats" }
$jarPath = Resolve-Path -LiteralPath $Jar
$headers = @{ Authorization = $Token; "User-Agent" = $ua }

# 1) Resolve slug -> project id
Write-Host "Resolving project '$Slug'..." -ForegroundColor Cyan
$project = Invoke-RestMethod -Uri "$api/project/$Slug" -Headers $headers
$projectId = $project.id
Write-Host "  -> $($project.title)  (id $projectId)" -ForegroundColor Green

# 2) Build the version metadata
$data = [ordered]@{
    name           = "TurboBoost $Version"
    version_number = $Version
    changelog      = $Changelog
    game_versions  = $GameVersions
    version_type   = $Channel
    loaders        = $Loaders
    featured       = $true
    project_id     = $projectId
    file_parts     = @("file")
    primary_file   = "file"
    dependencies   = @(
        @{ project_id = $FABRIC_API; dependency_type = "required" },
        @{ project_id = $SODIUM;     dependency_type = "optional" },
        @{ project_id = $LITHIUM;    dependency_type = "optional" }
    )
}
$json = $data | ConvertTo-Json -Depth 6 -Compress

Write-Host ""
Write-Host "About to publish:" -ForegroundColor Yellow
Write-Host "  project : $($project.title) ($Slug)"
Write-Host "  version : $Version  [$Channel]"
Write-Host "  mc      : $($GameVersions -join ', ')   loaders: $($Loaders -join ', ')"
Write-Host "  file    : $jarPath"
if (-not $Yes) {
    $ok = Read-Host "Type 'yes' to upload"
    if ($ok -ne "yes") { Write-Host "Aborted." -ForegroundColor Red; return }
}

# 3) Multipart upload (PowerShell 7 -Form handles multipart/form-data)
$resp = Invoke-RestMethod -Uri "$api/version" -Method Post -Headers $headers -Form @{
    data = $json
    file = Get-Item -LiteralPath $jarPath
}

Write-Host ""
Write-Host "Published!  https://modrinth.com/mod/$Slug/version/$($resp.version_number)" -ForegroundColor Green
