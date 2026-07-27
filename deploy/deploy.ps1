[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('v1', 'v2')]
    [string]$Version,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$rootDir = Split-Path -Parent $PSScriptRoot
$deployDir = Join-Path $rootDir "deploy/$Version"
$configFile = Join-Path $deployDir 'config.env'
$configTemplate = Join-Path $deployDir 'config.env.template'
$composeFile = Join-Path $deployDir 'docker-compose.yml'
$composeTemplate = Join-Path $deployDir 'docker-compose.template.yml'

function Invoke-Git {
    param([string[]]$Arguments)
    & git @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "git $($Arguments -join ' ') failed."
    }
}

function Ensure-LocalDeploymentFiles {
    if (-not (Test-Path -LiteralPath $configFile)) {
        Copy-Item -LiteralPath $configTemplate -Destination $configFile
        Write-Host "Created $configFile. Review its passwords and public URLs before exposing the service."
    }
    if (-not (Test-Path -LiteralPath $composeFile)) {
        Copy-Item -LiteralPath $composeTemplate -Destination $composeFile
        Write-Host "Created $composeFile from its committed template."
    }
    elseif ((Get-FileHash -LiteralPath $composeTemplate).Hash -ne (Get-FileHash -LiteralPath $composeFile).Hash) {
        Copy-Item -LiteralPath $composeTemplate -Destination $composeFile -Force
        Write-Host "Refreshed $composeFile from its committed template."
    }
}

function Invoke-Deployment {
    Ensure-LocalDeploymentFiles
    & docker compose --env-file $configFile -f $composeFile up --build --detach --remove-orphans
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker deployment failed.'
    }
    Write-Host "Deployment complete. Open the URL configured by APP_PORT in $configFile."
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker Compose v2 is required.'
}
& docker compose version | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Compose v2 is required.'
}

Push-Location $rootDir
try {
    $changes = (git status --porcelain --untracked-files=no) -join "`n"
    if (-not [string]::IsNullOrWhiteSpace($changes)) {
        throw 'Tracked local changes are present; commit or stash them before running deployment.'
    }

    $before = (git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Unable to determine the current Git revision.' }
    Write-Host 'Updating source code...'
    Invoke-Git -Arguments @('pull', '--ff-only')
    $after = (git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Unable to determine the updated Git revision.' }

    if ($Force) {
        Invoke-Deployment
    }
    elseif ($before -ne $after) {
        $count = (git rev-list --count "$before..$after").Trim()
        if ($LASTEXITCODE -ne 0) { throw 'Unable to count the updated commits.' }
        $reply = Read-Host "Source code was updated ($count commit(s)). Redeploy now? [Y/n]"
        if ($reply -notmatch '^[Nn]$') {
            Invoke-Deployment
        }
        else {
            Write-Host "Redeployment skipped. Run .\\deploy\\$Version\\deploy.ps1 -Force when ready."
        }
    }
    else {
        Write-Host 'Already up to date; no redeployment is needed. Use -Force to rebuild anyway.'
    }
}
finally {
    Pop-Location
}
