#Requires -Version 7.2
function Get-ErpBackupKey {
    if (-not $env:ERP_BACKUP_KEY) { throw 'Defina ERP_BACKUP_KEY com uma chave Base64 de 32 bytes, guardada separadamente do backup.' }
    try { $backupKey=[Convert]::FromBase64String($env:ERP_BACKUP_KEY) } catch { throw 'ERP_BACKUP_KEY deve ser Base64 válido.' }
    if ($backupKey.Length -ne 32) { throw 'ERP_BACKUP_KEY deve conter exatamente 32 bytes.' }
    return ,$backupKey
}
function Invoke-ErpDocker {
    param([string[]]$DockerArguments)
    $output=& docker @DockerArguments
    if ($LASTEXITCODE -ne 0) { throw "Docker falhou na operação $($DockerArguments[0])." }
    return $output
}
function Get-ErpBackupDirectory {
    $workspace=[IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
    $directory=[IO.Path]::GetFullPath((Join-Path $workspace 'backups'))
    if (-not $directory.StartsWith($workspace+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'Diretório de backup fora do projeto.' }
    [IO.Directory]::CreateDirectory($directory) | Out-Null
    return $directory
}
