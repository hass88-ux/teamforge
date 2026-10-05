$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
Push-Location $taskRoot
try {
    & npm --prefix frontend ci
    if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
    & npm --prefix frontend run lint
    if ($LASTEXITCODE -ne 0) { throw 'Frontend lint failed.' }
    & npm --prefix frontend test
    if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed.' }
    & npm --prefix frontend run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
    & mvn -B -f backend/pom.xml -Prelease clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Backend release verification failed.' }
    Write-Output 'Release JAR: backend/target/teamforge-api-0.1.0-SNAPSHOT.jar'
    Write-Output 'The JAR includes the frontend. Run the recommendation service separately.'
} finally { Pop-Location }
