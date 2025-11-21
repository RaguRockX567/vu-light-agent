# run_with_mock.ps1
param()
$module = "C:\Users\Asus\Downloads\vu-light-agent\vu-light-agent"
$mock = Join-Path $module "mock_backend\server.py"
Push-Location $module

# build the agent
if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd -B clean package
} else {
  mvn -B clean package
}

# start mock backend (background)
Start-Job -ScriptBlock {
  py C:\Users\Asus\Downloads\vu-light-agent\vu-light-agent\mock_backend\server.py
} | Out-Null
Start-Sleep -Seconds 2

# env vars
$env:AGENT_PORT="9001"
$env:BACKEND_URL="http://localhost:8080/api/metrics"

# run agent in a new window
Start-Process powershell -ArgumentList "-NoExit","-Command","cd `"$module`"; java -jar target\vu-light-agent-0.0.1-SNAPSHOT.jar"

Pop-Location
Write-Host "✅ Agent and mock backend started. Try: curl http://localhost:9001/health"
