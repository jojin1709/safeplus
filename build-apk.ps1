$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

$gradlePath = $null
if (Test-Path "C:\tmp\gradle-8.10.2\bin\gradle.bat") {
    $gradlePath = "C:\tmp\gradle-8.10.2\bin\gradle.bat"
} elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
    $gradlePath = "gradle"
} else {
    throw "Gradle was not found. Install Android Studio or Gradle, then run this script again."
}

Write-Output "Building Phone debug APK..."
& $gradlePath assemblePhoneDebug
if ($LASTEXITCODE -ne 0) { throw "Phone build failed." }

Write-Output ""
Write-Output "Building TV debug APK..."
& $gradlePath assembleTvDebug
if ($LASTEXITCODE -ne 0) { throw "TV build failed." }

Write-Output ""
Write-Output "=== Debug Build Complete ==="

$phoneApk = Get-ChildItem "app\build\outputs\apk\phone\debug\*.apk" | Select-Object -First 1
$tvApk = Get-ChildItem "app\build\outputs\apk\tv\debug\*.apk" | Select-Object -First 1

if ($phoneApk) {
    Write-Output "Phone APK: $($phoneApk.FullName)"
}
if ($tvApk) {
    Write-Output "TV APK:    $($tvApk.FullName)"
}
