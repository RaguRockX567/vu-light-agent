# Run as Administrator
Param(
  [string]$JarPath = "$PSScriptRoot\..\target\vu-light-agent-0.0.1-SNAPSHOT.jar",
  [string]$ServiceName = "VuNetLightAgent",
  [string]$JavaExe = "C:\Program Files\Eclipse Adoptium\jdk-17.0.5.8-hotspot\bin\java.exe",
  [int]$Port = 9001,
  [string]$BackendUrl = "http://localhost:8080/api/metrics",
  [string]$NssmExe = "C:\tools\nssm\nssm.exe"
)

if (-not (Test-Path $NssmExe)) { throw "nssm.exe not found at $NssmExe" }
if (-not (Test-Path $JarPath)) { throw "Jar not found at $JarPath. Build first." }

& $NssmExe install $ServiceName $JavaExe "-Dserver.port=$Port -jar `"$JarPath`""
& $NssmExe set $ServiceName AppDirectory (Split-Path -Parent $JarPath)
& $NssmExe set $ServiceName DisplayName "VuNet Light Agent"
& $NssmExe set $ServiceName Start SERVICE_AUTO_START
& $NssmExe set $ServiceName Environment "AGENT_PORT=$Port;BACKEND_URL=$BackendUrl"

Write-Host "Service installed. Starting..."
& $NssmExe start $ServiceName

Write-Host "Done. Use 'sc query $ServiceName' to check status."


