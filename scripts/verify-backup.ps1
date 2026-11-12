#Requires -Version 7.2
param([Parameter(Mandatory)][string]$Path)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'backup-common.ps1')
$backupKey=Get-ErpBackupKey
$directory=Get-ErpBackupDirectory
$operation=[guid]::NewGuid().ToString('N')
$rawFile=Join-Path $directory ('restore-'+$operation+'.dump')
$container='erp-restore-test-'+$operation
$createdContainer=$false;$cipher=$null
try {
    if ((Get-Item -LiteralPath $Path).Length -gt 350MB) { throw 'Arquivo excede o limite local de restauração.' }
    # Read strings verbatim: PowerShell 7.5 ConvertFrom-Json may coerce ISO timestamps.
    $document=[Text.Json.JsonDocument]::Parse([IO.File]::ReadAllText((Resolve-Path -LiteralPath $Path).Path))
    try {
        $envelope=@{}
        foreach ($field in @('format','createdAt','nonce','tag','ciphertext')) { $envelope[$field]=$document.RootElement.GetProperty($field).GetString() }
    } finally { $document.Dispose() }
    if ($envelope.format -ne 'ERP-BACKUP-V1') { throw 'Formato de backup não reconhecido.' }
    $nonce=[Convert]::FromBase64String($envelope.nonce);$tag=[Convert]::FromBase64String($envelope.tag)
    $encrypted=[Convert]::FromBase64String($envelope.ciphertext);$plain=[byte[]]::new($encrypted.Length)
    $cipher=[Security.Cryptography.AesGcm]::new($backupKey)
    # Authentication must succeed before creating a database or writing decrypted bytes.
    $cipher.Decrypt($nonce,$encrypted,$tag,$plain,[Text.Encoding]::UTF8.GetBytes(('ERP-BACKUP-V1|'+$envelope.createdAt)))
    [IO.File]::WriteAllBytes($rawFile,$plain)
    Invoke-ErpDocker -DockerArguments @('run','-d','--name',$container,'--network','none','-e','POSTGRES_HOST_AUTH_METHOD=trust','-e','POSTGRES_DB=erp','-e','POSTGRES_USER=erp','postgres:17.6-alpine') | Out-Null
    $createdContainer=$true;$ready=$false
    for ($attempt=0;$attempt -lt 30;$attempt++) {
        & docker exec $container pg_isready -h 127.0.0.1 -U erp -d erp *> $null
        if ($LASTEXITCODE -eq 0) { $ready=$true;break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'O PostgreSQL descartável não ficou pronto.' }
    Invoke-ErpDocker -DockerArguments @('cp',$rawFile,($container+':/tmp/restore.dump')) | Out-Null
    Invoke-ErpDocker -DockerArguments @('exec',$container,'pg_restore','-U','erp','-d','erp','--exit-on-error','/tmp/restore.dump') | Out-Null
    $migration=(Invoke-ErpDocker -DockerArguments @('exec',$container,'psql','-U','erp','-d','erp','-At','-v','ON_ERROR_STOP=1','-c','SELECT version FROM flyway_schema_history WHERE success=TRUE ORDER BY installed_rank DESC LIMIT 1;') | Out-String).Trim()
    if (-not $migration) { throw 'Backup não contém histórico de migrations válido.' }
    $failed=(Invoke-ErpDocker -DockerArguments @('exec',$container,'psql','-U','erp','-d','erp','-At','-v','ON_ERROR_STOP=1','-c','SELECT count(*) FROM flyway_schema_history WHERE success=FALSE;') | Out-String).Trim()
    if ($failed -ne '0') { throw 'Backup contém migration reprovada.' }
    $counts=(Invoke-ErpDocker -DockerArguments @('exec',$container,'psql','-U','erp','-d','erp','-At','-v','ON_ERROR_STOP=1','-c','SELECT (SELECT count(*) FROM tenant),(SELECT count(*) FROM app_user),(SELECT count(*) FROM product),(SELECT count(*) FROM stock_movement);') | Out-String).Trim()
    [pscustomobject]@{Restored=$true;Migration=$migration;TenantUserProductMovementCounts=$counts;SHA256=(Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash}
} finally {
    if ($cipher) { $cipher.Dispose() }
    [Array]::Clear($backupKey,0,$backupKey.Length)
    if ($plain) { [Array]::Clear($plain,0,$plain.Length) }
    if (Test-Path -LiteralPath $rawFile) { Remove-Item -LiteralPath $rawFile -Force }
    # Only the randomly named container created by this invocation is removed, with its temporary volume.
    if ($createdContainer -and $container -match '^erp-restore-test-[a-f0-9]{32}$') { & docker rm -f -v $container | Out-Null }
}
