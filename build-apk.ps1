$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

if (-not (Get-Command gradle -ErrorAction SilentlyContinue)) {
    throw "Gradle was not found. Install Android Studio or Gradle, then run this script again."
}

gradle assembleDebug

$apk = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) {
    Write-Output "APK created: $apk"
}
