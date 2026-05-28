$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

if (-not (Test-Path ".\keystore.properties")) {
    Write-Output "Create keystore.properties from keystore.properties.example first."
    Write-Output "Then create the matching .jks key with Android Studio or keytool."
    exit 1
}

if (Test-Path "C:\tmp\gradle-8.10.2\bin\gradle.bat") {
    & "C:\tmp\gradle-8.10.2\bin\gradle.bat" assembleRelease
} elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
    gradle assembleRelease
} else {
    throw "Gradle was not found."
}
