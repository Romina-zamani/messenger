param(
    [string]$Password = 'changeit'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$certificateDirectory = Join-Path $projectRoot 'backend\src\main\resources\certs'
$keyStore = Join-Path $certificateDirectory 'localhost.p12'
$trustStore = Join-Path $certificateDirectory 'localhost-truststore.p12'
$publicCertificate = Join-Path $certificateDirectory 'localhost.cer'
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot' }
$keytool = Join-Path $javaHome 'bin\keytool.exe'

if (-not (Test-Path -LiteralPath $keytool)) {
    throw "keytool was not found at $keytool. Set JAVA_HOME to JDK 21."
}

New-Item -ItemType Directory -Force -Path $certificateDirectory | Out-Null
if (-not (Test-Path -LiteralPath $keyStore)) {
  & $keytool -genkeypair `
    -alias localhost `
    -keyalg RSA `
    -keysize 2048 `
    -validity 365 `
    -storetype PKCS12 `
    -keystore $keyStore `
    -storepass $Password `
    -keypass $Password `
    -dname 'CN=localhost, OU=Learning, O=Java Messenger, L=Tehran, C=IR' `
    -ext 'SAN=dns:localhost,ip:127.0.0.1' `
    -ext 'KU=digitalSignature,keyEncipherment' `
    -ext 'EKU=serverAuth'

  if ($LASTEXITCODE -ne 0) { throw "keytool generation failed with exit code $LASTEXITCODE" }
}

if (-not (Test-Path -LiteralPath $trustStore)) {
  & $keytool -exportcert -alias localhost -keystore $keyStore -storepass $Password -file $publicCertificate
  if ($LASTEXITCODE -ne 0) { throw "certificate export failed with exit code $LASTEXITCODE" }
  & $keytool -importcert -noprompt -alias localhost -file $publicCertificate -keystore $trustStore -storetype PKCS12 -storepass $Password
  if ($LASTEXITCODE -ne 0) { throw "trust-store import failed with exit code $LASTEXITCODE" }
  Remove-Item -LiteralPath $publicCertificate
}

Write-Host "Created $keyStore"
Write-Host "Created $trustStore"
Write-Host 'This file contains a private key. It is ignored by Git.'
