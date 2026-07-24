[CmdletBinding()]
param([switch]$Force)

& (Join-Path $PSScriptRoot '..\deploy.ps1') -Version v2 -Force:$Force @args
