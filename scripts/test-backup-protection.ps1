#Requires -Version 7.4
$ErrorActionPreference = 'Stop'
$folder = Join-Path ([IO.Path]::GetTempPath()) ('teamforge-backup-test-' + [Guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($folder) | Out-Null
$priorPassphrase = $env:TEAMFORGE_BACKUP_PASSPHRASE
try {
 $env:TEAMFORGE_BACKUP_PASSPHRASE = 'disposable-fixture-passphrase-only'
 $plain = Join-Path $folder 'fixture.dump'; $encrypted = Join-Path $folder 'fixture.tfaes'; $restored = Join-Path $folder 'restored.dump'
 [IO.File]::WriteAllBytes($plain, [Security.Cryptography.RandomNumberGenerator]::GetBytes(8192))
 & (Join-Path $PSScriptRoot 'protect-backup.ps1') Encrypt $plain $encrypted
 & (Join-Path $PSScriptRoot 'protect-backup.ps1') Decrypt $encrypted $restored
 if ((Get-FileHash $plain).Hash -ne (Get-FileHash $restored).Hash) { throw 'Round trip failed.' }
 $env:TEAMFORGE_BACKUP_PASSPHRASE = 'incorrect-disposable-fixture-passphrase'
 $rejected = $false
 try { & (Join-Path $PSScriptRoot 'protect-backup.ps1') Decrypt $encrypted (Join-Path $folder 'wrong.dump') } catch { $rejected = $true }
 if (-not $rejected -or (Test-Path (Join-Path $folder 'wrong.dump'))) { throw 'Incorrect passphrase was not safely rejected.' }
 $env:TEAMFORGE_BACKUP_PASSPHRASE = 'disposable-fixture-passphrase-only'
 $damaged = [IO.File]::ReadAllBytes($encrypted); $damaged[$damaged.Length-1] = $damaged[$damaged.Length-1] -bxor 1
 [IO.File]::WriteAllBytes($encrypted, $damaged)
 $rejected = $false
 try { & (Join-Path $PSScriptRoot 'protect-backup.ps1') Decrypt $encrypted (Join-Path $folder 'tampered.dump') } catch { $rejected = $true }
 if (-not $rejected -or (Test-Path (Join-Path $folder 'tampered.dump'))) { throw 'Tampered backup was not safely rejected.' }
 Write-Output 'Backup encryption: round trip, wrong-passphrase rejection, and tamper rejection passed.'
} finally {
 $env:TEAMFORGE_BACKUP_PASSPHRASE = $priorPassphrase
 # Delete only the explicitly created fixture directory inside the system temp folder.
 $resolved = [IO.Path]::GetFullPath($folder)
 $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
 if ($resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -and [IO.Path]::GetFileName($resolved) -match '^teamforge-backup-test-[a-f0-9]{32}$') { Remove-Item -LiteralPath $resolved -Recurse }
}
