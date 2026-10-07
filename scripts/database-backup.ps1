#Requires -Version 7.4
param(
 [ValidateSet('Backup','RestoreCheck')][string]$Mode = 'Backup',
 [string]$Directory = (Join-Path $PSScriptRoot '../.local/backups'),
 [string]$BackupPath,
 [ValidateRange(1,90)][int]$RetentionDays = 14
)
$ErrorActionPreference = 'Stop'
foreach ($variable in @('PGHOST','PGDATABASE','PGUSER','PGPASSWORD','TEAMFORGE_BACKUP_PASSPHRASE')) {
 if (-not [Environment]::GetEnvironmentVariable($variable)) { throw "Set $variable privately before running this command." }
}
if ($env:PGSSLMODE -ne 'require' -and $env:PGSSLMODE -ne 'verify-full') { throw 'PGSSLMODE must require encrypted transport.' }
if ($env:TEAMFORGE_BACKUP_PASSPHRASE.Length -lt 20) { throw 'Backup passphrase must contain at least 20 characters.' }
if ($Mode -eq 'Backup') { $dump = Get-Command pg_dump -ErrorAction Stop }
if ($Mode -eq 'RestoreCheck') {
 if ($env:PGDATABASE -notmatch '^teamforge_restore_[a-zA-Z0-9_]+$') { throw 'Restore only into a separate empty database named teamforge_restore_<suffix>.' }
 if (-not $BackupPath) { throw 'Provide the encrypted BackupPath.' }
 $restore = Get-Command pg_restore -ErrorAction Stop
 $psql = Get-Command psql -ErrorAction Stop
 $existing = & $psql.Source -X -At -v ON_ERROR_STOP=1 -c "SELECT count(*) FROM information_schema.tables WHERE table_schema='public'" 2>$null
 if ($LASTEXITCODE -ne 0 -or "$existing".Trim() -ne '0') { throw 'Restore target must be reachable and empty; no existing data will be overwritten.' }
}
$temporary = [IO.Path]::GetTempFileName()
try {
 if ($Mode -eq 'Backup') {
  & $dump.Source -Fc --no-owner --no-acl --file=$temporary 2>$null
  if ($LASTEXITCODE -ne 0) { throw 'Database backup failed. Check private connection settings and client version.' }
  $destination = [IO.Path]::GetFullPath($Directory)
  [IO.Directory]::CreateDirectory($destination) | Out-Null
  $name = 'teamforge-' + [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N') + '.tfaes'
  $encrypted = Join-Path $destination $name
  & (Join-Path $PSScriptRoot 'protect-backup.ps1') -Mode Encrypt -InputPath $temporary -OutputPath $encrypted
  # Remove only this tool's expired encrypted files, keeping the new backup.
  Get-ChildItem -LiteralPath $destination -File | Where-Object { $_.Name -match '^teamforge-\d{8}-\d{6}-[a-f0-9]{32}\.tfaes$' -and $_.LastWriteTimeUtc -lt [DateTime]::UtcNow.AddDays(-$RetentionDays) } | ForEach-Object { Remove-Item -LiteralPath $_.FullName }
  Write-Output "Encrypted backup saved: $encrypted"
 } else {
  Remove-Item -LiteralPath $temporary
  & (Join-Path $PSScriptRoot 'protect-backup.ps1') -Mode Decrypt -InputPath $BackupPath -OutputPath $temporary
  & $restore.Source --exit-on-error --no-owner --no-acl --dbname=$env:PGDATABASE $temporary 2>$null
  if ($LASTEXITCODE -ne 0) { throw 'Restore failed in the isolated target. Production was not changed.' }
  $verified = & $psql.Source -X -At -v ON_ERROR_STOP=1 -c "SELECT count(*) FROM flyway_schema_history WHERE success; SELECT count(*) FROM accounts; SELECT count(*) FROM project_tasks; SELECT count(*) FROM collaboration_messages" 2>$null
  if ($LASTEXITCODE -ne 0 -or [int]$verified[0] -lt 1) { throw 'Restore verification failed.' }
  Write-Output 'Isolated restore verified: migrations, accounts, tasks, and messages are readable.'
 }
} finally { if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary } }
