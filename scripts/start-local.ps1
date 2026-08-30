[CmdletBinding()]
param(
  [Parameter(Mandatory = $true)]
  [string]$DbPassword,
  [string]$DbUsername = 'root',
  [string]$DbUrl = 'jdbc:mysql://127.0.0.1:3306/washer?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false',
  [switch]$EnableMockLogin
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $projectRoot 'backend'
$adminDir = Join-Path $projectRoot 'admin-web'
$localDir = Join-Path $projectRoot '.local'
$logDir = Join-Path $localDir 'logs'

function Test-ListeningPort([int]$Port) {
  return [System.Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners().Port -contains $Port
}

if (Test-ListeningPort 18080) {
  throw 'Port 18080 is already in use. Stop the existing backend process before starting local development.'
}

if (Test-ListeningPort 18073) {
  throw 'Port 18073 is already in use. Stop the existing admin-web process before starting local development.'
}

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if (
  -not (Test-Path (Join-Path $adminDir 'node_modules/.bin/vite.cmd')) -or
  -not (Test-Path (Join-Path $adminDir 'node_modules/.bin/vue-tsc.cmd'))
) {
  Push-Location $adminDir
  try {
    npm.cmd ci
    if ($LASTEXITCODE -ne 0) {
      throw 'Failed to install admin-web dependencies.'
    }
  } finally {
    Pop-Location
  }
}

$env:WASHER_DB_URL = $DbUrl
$env:WASHER_DB_USERNAME = $DbUsername
$env:WASHER_DB_PASSWORD = $DbPassword
$env:WECHAT_MINIAPP_MOCK_LOGIN_ENABLED = if ($EnableMockLogin) { 'true' } else { 'false' }

$backendProcess = Start-Process -FilePath 'mvn.cmd' -ArgumentList 'spring-boot:run' -WorkingDirectory $backendDir -RedirectStandardOutput (Join-Path $logDir 'backend.out.log') -RedirectStandardError (Join-Path $logDir 'backend.err.log') -PassThru -WindowStyle Hidden
$adminProcess = Start-Process -FilePath 'npm.cmd' -ArgumentList 'run', 'dev:local' -WorkingDirectory $adminDir -RedirectStandardOutput (Join-Path $logDir 'admin-web.out.log') -RedirectStandardError (Join-Path $logDir 'admin-web.err.log') -PassThru -WindowStyle Hidden

Set-Content -Path (Join-Path $localDir 'backend.pid') -Value $backendProcess.Id
Set-Content -Path (Join-Path $localDir 'admin-web.pid') -Value $adminProcess.Id

Write-Host 'Backend:   http://127.0.0.1:18080/ping'
Write-Host 'Admin web: http://127.0.0.1:18073'
Write-Host "Logs:      $logDir"
