#Requires -Version 7.2
$ErrorActionPreference='Stop'
$previousKey=$env:ERP_BACKUP_KEY
$env:ERP_BACKUP_KEY=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$backup=$null;$tamperedPath=$null
function Test-CryptoRejection([string]$Path) {
    try { & (Join-Path $PSScriptRoot 'verify-backup.ps1') -Path $Path | Out-Null;return $false }
    catch {
        $exception=$_.Exception
        while($exception) {
            if($exception -is [Security.Cryptography.CryptographicException]) { return $true }
            $exception=$exception.InnerException
        }
        throw
    }
}
try {
    $backup=& (Join-Path $PSScriptRoot 'backup-database.ps1') -Name ('crypto-test-'+[guid]::NewGuid().ToString('N')+'.erpbak')
    $restored=& (Join-Path $PSScriptRoot 'verify-backup.ps1') -Path $backup.Path
    $document=[Text.Json.JsonDocument]::Parse([IO.File]::ReadAllText($backup.Path));$envelope=@{}
    try { foreach($field in @('format','createdAt','nonce','tag','ciphertext')) { $envelope[$field]=$document.RootElement.GetProperty($field).GetString() } } finally { $document.Dispose() }
    $encrypted=[Convert]::FromBase64String($envelope.ciphertext);$encrypted[0]=$encrypted[0] -bxor 1
    $envelope.ciphertext=[Convert]::ToBase64String($encrypted)
    $tamperedPath=Join-Path (Split-Path $backup.Path) ('tampered-'+[guid]::NewGuid().ToString('N')+'.erpbak')
    [IO.File]::WriteAllText($tamperedPath,($envelope | ConvertTo-Json -Compress))
    $tamperRejected=Test-CryptoRejection $tamperedPath
    $env:ERP_BACKUP_KEY=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
    $wrongKeyRejected=Test-CryptoRejection $backup.Path
    if(-not $tamperRejected -or -not $wrongKeyRejected -or -not $restored.Restored) { throw 'Ensaio de backup reprovado.' }
    [pscustomobject]@{Restored=$restored.Restored;Migration=$restored.Migration;Counts=$restored.TenantUserProductMovementCounts;CiphertextTamperRejected=$tamperRejected;WrongKeyRejected=$wrongKeyRejected;Timestamp=[DateTimeOffset]::UtcNow.ToString('O')}
} finally {
    $env:ERP_BACKUP_KEY=$previousKey
    $directory=[IO.Path]::GetFullPath((Join-Path (Split-Path -Parent $PSScriptRoot) 'backups'))+[IO.Path]::DirectorySeparatorChar
    foreach($file in @($backup.Path,$tamperedPath)) {
        if($file -and [IO.Path]::GetFullPath($file).StartsWith($directory,[StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $file)) { Remove-Item -LiteralPath $file -Force }
    }
}
