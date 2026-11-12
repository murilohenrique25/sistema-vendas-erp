param(
    [Parameter(Mandatory)] [string] $CompanyName,
    [Parameter(Mandatory)] [string] $Slug,
    [Parameter(Mandatory)] [string] $AdminName,
    [Parameter(Mandatory)] [string] $Email,
    [string] $BaseUrl = 'http://127.0.0.1:18080'
)
$ErrorActionPreference = 'Stop'
if (-not $env:PROVISIONING_KEY -or $env:PROVISIONING_KEY.Length -lt 32) { throw 'Defina PROVISIONING_KEY com pelo menos 32 caracteres.' }
$securePassword = Read-Host 'Senha inicial (12 a 72 caracteres ASCII)' -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $payload = @{ companyName=$CompanyName; slug=$Slug; adminName=$AdminName; email=$Email; password=$plainPassword } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/provisioning" -Headers @{'X-Provisioning-Key'=$env:PROVISIONING_KEY} -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($payload))
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    $plainPassword = $null
    $payload = $null
}
