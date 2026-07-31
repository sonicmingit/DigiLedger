<#!
.SYNOPSIS
Builds a versioned DigiLedger Android debug APK.

.EXAMPLE
.\scripts\build-apk.ps1 -Version 1.2.3
#>
[CmdletBinding()]
param(
  [Parameter(Mandatory = $true, Position = 0)]
  [ValidatePattern('^\d+\.\d+\.\d+$')]
  [string]$Version,

  [Parameter(Mandatory = $true, Position = 1)]
  [ValidateNotNullOrEmpty()]
  [string]$Notes
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$packageJson = Join-Path $projectRoot 'package.json'
$manifestJson = Join-Path $projectRoot 'src\manifest.json'
$gradleFile = Join-Path $projectRoot 'android\app\build.gradle'
$changelogFile = Join-Path $projectRoot 'CHANGELOG.md'
$apkSource = Join-Path $projectRoot 'android\app\build\outputs\apk\debug\app-debug.apk'
$releaseDirectory = Join-Path $projectRoot 'dist\apk'

$parts = $Version.Split('.') | ForEach-Object { [int]$_ }
if ($parts[1] -gt 999 -or $parts[2] -gt 999) {
  throw 'The minor and patch versions must each be below 1000.'
}

# SemVer maps to a monotonically increasing Android versionCode: Mmmppp.
$versionCode = ($parts[0] * 1000000) + ($parts[1] * 1000) + $parts[2]
if ($versionCode -gt [int]::MaxValue) {
  throw 'The version is too large for an Android versionCode.'
}

function Set-Utf8File([string]$Path, [string]$Content) {
  $encoding = [System.Text.UTF8Encoding]::new($false)
  [System.IO.File]::WriteAllText($Path, $Content, $encoding)
}

function Replace-Required([string]$Path, [string]$Pattern, [string]$Replacement) {
  $content = Get-Content -Raw -Encoding UTF8 -LiteralPath $Path
  $updated = [regex]::Replace($content, $Pattern, $Replacement, 1)
  if ($updated -eq $content) {
    throw "Could not update the version setting in: $Path"
  }
  Set-Utf8File -Path $Path -Content $updated
}

function Add-ReleaseNotes([string]$ReleaseVersion, [string]$ReleaseNotes) {
  $heading = "## v$ReleaseVersion"
  $existing = if (Test-Path -LiteralPath $changelogFile) {
    Get-Content -Raw -Encoding UTF8 -LiteralPath $changelogFile
  } else {
    "# DigiLedger Changelog`n"
  }
  if ($existing -match "(?m)^$([regex]::Escape($heading))$") {
    throw "A changelog entry already exists for v$ReleaseVersion."
  }

  $items = $ReleaseNotes -split "`r?`n" |
    Where-Object { $_.Trim() } |
    ForEach-Object {
      $line = $_.Trim()
      if ($line.StartsWith('- ')) { $line } else { "- $line" }
    }
  $entry = "$heading - $(Get-Date -Format 'yyyy-MM-dd')`n`n$($items -join "`n")`n`n"
  Set-Utf8File -Path $changelogFile -Content ($existing.TrimEnd() + "`n`n" + $entry)
}

Replace-Required -Path $packageJson -Pattern '(?m)^  "version": "[^"]+",' -Replacement "  `"version`": `"$Version`","
Replace-Required -Path $manifestJson -Pattern '(?m)^  "versionName": "[^"]+",' -Replacement "  `"versionName`": `"$Version`","
Replace-Required -Path $gradleFile -Pattern '(?m)^\s*versionCode\s+\d+' -Replacement "        versionCode $versionCode"
Replace-Required -Path $gradleFile -Pattern '(?m)^\s*versionName\s+"[^"]+"' -Replacement "        versionName `"$Version`""

Push-Location $projectRoot
try {
  Write-Host "Building DigiLedger v$Version (Android versionCode $versionCode)..."
  & npm run sync:android
  if ($LASTEXITCODE -ne 0) { throw 'H5 build or Android sync failed.' }

  Push-Location (Join-Path $projectRoot 'android')
  try {
    & .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) { throw 'Android APK build failed.' }
  }
  finally {
    Pop-Location
  }
}
finally {
  Pop-Location
}

if (-not (Test-Path -LiteralPath $apkSource)) {
  throw "Build output was not found: $apkSource"
}

New-Item -ItemType Directory -Force -Path $releaseDirectory | Out-Null
$outputApk = Join-Path $releaseDirectory "digiLedger.v$Version.apk"
Copy-Item -LiteralPath $apkSource -Destination $outputApk -Force
Add-ReleaseNotes -ReleaseVersion $Version -ReleaseNotes $Notes
Write-Host "APK ready: $outputApk" -ForegroundColor Green
