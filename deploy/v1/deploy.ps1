[CmdletBinding()]
param([switch]$Force)

& (Join-Path $PSScriptRoot '..\deploy.ps1') -Version v1 -Force:$Force @args
