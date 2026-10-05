param(
    [string]$AppUrl = 'http://127.0.0.1:5173',
    [string]$ApiUrl = 'http://127.0.0.1:8080',
    [string]$RecommendationUrl = 'http://127.0.0.1:8001',
    [switch]$Release
)
$ErrorActionPreference = 'Stop'
function Read-Content($response) {
    if ($response.Content -is [byte[]]) { return [Text.Encoding]::UTF8.GetString($response.Content) }
    return [string]$response.Content
}
function Check-Response($url, $expected) {
    try { $response = Invoke-WebRequest -Uri $url -TimeoutSec 15 -UseBasicParsing }
    catch {
        if ($null -eq $_.Exception.Response) { throw "Cannot reach $url. Run scripts/start-local.ps1 for local development." }
        $response = $_.Exception.Response
    }
    if ([int]$response.StatusCode -ne $expected) { throw "Unexpected HTTP status at $url; expected $expected, received $([int]$response.StatusCode)." }
    return $response
}
$app = Check-Response "$($AppUrl.TrimEnd('/'))/" 200
if ((Read-Content $app) -notmatch '<div id="root"') { throw 'The app URL did not return the TeamForge frontend.' }
$health = Read-Content (Check-Response "$($ApiUrl.TrimEnd('/'))/actuator/health" 200) | ConvertFrom-Json
if ($health.status -ne 'UP') { throw 'The API is not healthy.' }
$recommendations = Read-Content (Check-Response "$($RecommendationUrl.TrimEnd('/'))/health" 200) | ConvertFrom-Json
if ($recommendations.status -ne 'ok') { throw 'The recommendation service is not healthy.' }
foreach ($path in @('/api/account/export', '/api/dashboard', '/api/projects', '/application.properties', '/.local/teamforge.mv.db')) {
    Check-Response "$($ApiUrl.TrimEnd('/'))$path" 401 | Out-Null
}
if ($Release) {
    $assets = [regex]::Matches((Read-Content $app), '(?:src|href)="(/assets/[^" ]+)"')
    if ($assets.Count -lt 2) { throw 'Release HTML is missing bundled JavaScript/CSS.' }
    foreach ($asset in $assets) { Check-Response "$($AppUrl.TrimEnd('/'))$($asset.Groups[1].Value)" 200 | Out-Null }
}
Write-Output 'PASS: frontend, API, recommendation service, and unauthenticated route protections.'
if ($Release) { Write-Output 'PASS: bundled release assets.' }
Write-Output 'This read-only smoke check does not verify authenticated workflows, PostgreSQL, or HTTPS.'
