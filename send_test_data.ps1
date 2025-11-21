# Send test metrics to the application
$metrics = @(
    @{
        name = "system.cpu.load"
        value = [math]::Round((Get-Random -Minimum 10 -Maximum 90) / 100, 2)
        timestamp = [int][double]::Parse((Get-Date -UFormat %s)) * 1000
    },
    @{
        name = "system.memory.used"
        value = [math]::Round((Get-Random -Minimum 1 -Maximum 6) + (Get-Random -Minimum 0.0 -Maximum 1.0), 2)
        timestamp = [int][double]::Parse((Get-Date -UFormat %s)) * 1000
    },
    @{
        name = "system.memory.total"
        value = 8.0
        timestamp = [int][double]::Parse((Get-Date -UFormat %s)) * 1000
    }
) | ConvertTo-Json

$headers = @{
    "Content-Type" = "application/json"
}

$response = Invoke-RestMethod -Uri "http://localhost:9016/metrics" -Method Post -Body $metrics -Headers $headers
Write-Host "✅ Sent test metrics. Response: $($response | ConvertTo-Json)"
