#Requires -Version 7.2
param(
    [ValidatePattern('^[a-zA-Z0-9_-]+\.erpbak$')][string]$Name=('erp-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N')+'.erpbak'),
    [ValidatePattern('^[a-zA-Z0-9][a-zA-Z0-9_.-]*$')][string]$SourceContainer
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'backup-common.ps1')
$backupKey=Get-ErpBackupKey
$directory=Get-ErpBackupDirectory
$destination=Join-Path $directory $Name
if (Test-Path -LiteralPath $destination) { throw 'O arquivo de destino já existe; backups não são sobrescritos.' }
$operation=[guid]::NewGuid().ToString('N')
$containerDump='/tmp/erp-backup-'+$operation+'.dump'
$rawFile=Join-Path $directory ('temporary-'+$operation+'.dump')
$cipher=$null
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    if (-not $SourceContainer) {
        $SourceContainer=(Invoke-ErpDocker -DockerArguments @('compose','ps','-q','db') | Out-String).Trim()
        if (-not $SourceContainer) { throw 'O banco do Compose não está ativo.' }
    }
    Invoke-ErpDocker -DockerArguments @('exec',$SourceContainer,'pg_dump','-U','erp','-d','erp','-Fc','-f',$containerDump) | Out-Null
    Invoke-ErpDocker -DockerArguments @('cp',($SourceContainer+':'+$containerDump),$rawFile) | Out-Null
    # This local tool intentionally bounds memory; large production backups need streaming tooling.
    if ((Get-Item -LiteralPath $rawFile).Length -gt 256MB) { throw 'Backup excede o limite local de 256 MB. Use ferramenta de streaming para esse volume.' }
    $plain=[IO.File]::ReadAllBytes($rawFile)
    $nonce=[Security.Cryptography.RandomNumberGenerator]::GetBytes(12)
    $tag=[byte[]]::new(16);$encrypted=[byte[]]::new($plain.Length)
    $created=[DateTimeOffset]::UtcNow.ToString('O')
    $header='ERP-BACKUP-V1|'+$created
    $cipher=[Security.Cryptography.AesGcm]::new($backupKey)
    $cipher.Encrypt($nonce,$plain,$encrypted,$tag,[Text.Encoding]::UTF8.GetBytes($header))
    $envelope=@{format='ERP-BACKUP-V1';createdAt=$created;nonce=[Convert]::ToBase64String($nonce);tag=[Convert]::ToBase64String($tag);ciphertext=[Convert]::ToBase64String($encrypted)} | ConvertTo-Json -Compress
    $stream=[IO.File]::Open($destination,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
    try { $bytes=[Text.Encoding]::UTF8.GetBytes($envelope);$stream.Write($bytes,0,$bytes.Length) } finally { $stream.Dispose() }
    [pscustomobject]@{Path=$destination;SHA256=(Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash;CreatedAt=$created}
} finally {
    if ($cipher) { $cipher.Dispose() }
    [Array]::Clear($backupKey,0,$backupKey.Length)
    if ($plain) { [Array]::Clear($plain,0,$plain.Length) }
    if (Test-Path -LiteralPath $rawFile) { Remove-Item -LiteralPath $rawFile -Force }
    if ($SourceContainer) { & docker exec $SourceContainer rm -f $containerDump | Out-Null }
    Pop-Location
}
