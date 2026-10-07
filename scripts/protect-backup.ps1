#Requires -Version 7.4
param(
 [Parameter(Mandatory)][ValidateSet('Encrypt','Decrypt')][string]$Mode,
 [Parameter(Mandatory)][string]$InputPath,
 [Parameter(Mandatory)][string]$OutputPath
)
$ErrorActionPreference = 'Stop'
if ($env:TEAMFORGE_BACKUP_PASSPHRASE.Length -lt 20) { throw 'Set a private TEAMFORGE_BACKUP_PASSPHRASE of at least 20 characters. Store it separately from backups.' }
if (Test-Path -LiteralPath $OutputPath) { throw 'Output already exists; choose a new path.' }
$inputBytes = [IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $InputPath).Path)
$magic = [Text.Encoding]::ASCII.GetBytes('TFAES001')
if ($Mode -eq 'Encrypt') {
 $salt = [Security.Cryptography.RandomNumberGenerator]::GetBytes(16)
 $nonce = [Security.Cryptography.RandomNumberGenerator]::GetBytes(12)
 $payload = $inputBytes
 $tag = [byte[]]::new(16)
 $ciphertext = [byte[]]::new($payload.Length)
} else {
 if ($inputBytes.Length -lt 52 -or [Convert]::ToHexString($inputBytes[0..7]) -ne [Convert]::ToHexString($magic)) { throw 'Unsupported backup format.' }
 $salt = [byte[]]$inputBytes[8..23]; $nonce = [byte[]]$inputBytes[24..35]; $tag = [byte[]]$inputBytes[36..51]
 $ciphertext = [byte[]]::new($inputBytes.Length - 52)
 [Array]::Copy($inputBytes, 52, $ciphertext, 0, $ciphertext.Length)
 $payload = [byte[]]::new($ciphertext.Length)
}
$key = [Security.Cryptography.Rfc2898DeriveBytes]::Pbkdf2($env:TEAMFORGE_BACKUP_PASSPHRASE, $salt, 600000, [Security.Cryptography.HashAlgorithmName]::SHA256, 32)
$aes = [Security.Cryptography.AesGcm]::new($key, 16)
try {
 if ($Mode -eq 'Encrypt') {
  $aes.Encrypt($nonce, $payload, $ciphertext, $tag, $magic)
  $outputBytes = [byte[]]($magic + $salt + $nonce + $tag + $ciphertext)
 } else {
  try { $aes.Decrypt($nonce, $ciphertext, $tag, $payload, $magic) } catch { throw 'Backup authentication failed. Check the passphrase and file integrity.' }
  $outputBytes = $payload
 }
 $outputFile = [IO.File]::Open([IO.Path]::GetFullPath($OutputPath), [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
 try { $outputFile.Write($outputBytes) } finally { $outputFile.Dispose() }
} finally {
 $aes.Dispose()
 [Security.Cryptography.CryptographicOperations]::ZeroMemory($key)
 [Security.Cryptography.CryptographicOperations]::ZeroMemory($payload)
}
