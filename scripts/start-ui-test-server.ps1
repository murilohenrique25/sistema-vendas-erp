$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    & mvn -B -f backend/pom.xml test-compile dependency:build-classpath '-Dmdep.includeScope=test' '-Dmdep.outputFile=target/ui-test-classpath.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível preparar o servidor de testes.' }
    $dependencies=Get-Content -LiteralPath backend/target/ui-test-classpath.txt -Raw
    $testClasses=Join-Path $projectRoot 'backend/target/test-classes'
    $mainClasses=Join-Path $projectRoot 'backend/target/classes'
    & java -cp "$testClasses;$mainClasses;$($dependencies.Trim())" br.com.erp.UiTestServer
} finally { Pop-Location }
