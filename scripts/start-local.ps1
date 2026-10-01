$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskLogs = Join-Path $env:TEMP 'teamforge-local'
New-Item -ItemType Directory -Force -Path $taskLogs | Out-Null

function Start-TeamForgeService($name, $port, $executable, $arguments, $workingDirectory) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
        Write-Output "$name already has a listener on port $port; leaving it running."
        return
    }
    Start-Process -FilePath $executable -ArgumentList $arguments -WorkingDirectory $workingDirectory -WindowStyle Hidden -RedirectStandardOutput (Join-Path $taskLogs "$name.out.log") -RedirectStandardError (Join-Path $taskLogs "$name.err.log") | Out-Null
    Write-Output "Started $name on port $port."
}

$taskJar = Join-Path $taskRoot 'backend/target/teamforge-api-0.1.0-SNAPSHOT.jar'
$taskPython = Join-Path $taskRoot 'ai/.venv/Scripts/python.exe'
$taskVite = Join-Path $taskRoot 'frontend/node_modules/vite/bin/vite.js'
foreach ($taskRequired in @($taskJar, $taskPython, $taskVite)) {
    if (-not (Test-Path -LiteralPath $taskRequired)) { throw "Missing dependency: $taskRequired. Follow README local setup first." }
}
Start-TeamForgeService 'recommendations' 8001 $taskPython '-m uvicorn teamforge_ai.app:app --host 127.0.0.1 --port 8001' (Join-Path $taskRoot 'ai')
Start-TeamForgeService 'backend' 8080 (Get-Command java.exe).Source ('-jar "{0}"' -f $taskJar) $taskRoot
Start-TeamForgeService 'frontend' 5173 (Get-Command node.exe).Source ('"{0}" --host 127.0.0.1 --port 5173 --strictPort' -f $taskVite) (Join-Path $taskRoot 'frontend')
Write-Output 'Preview: http://127.0.0.1:5173/'
Write-Output "Logs: $taskLogs"
