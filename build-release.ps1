$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

if (-not (Test-Path ".\keystore.properties")) {
    Write-Output "Create keystore.properties from keystore.properties.example first."
    Write-Output "Then create the matching .jks key with Android Studio or keytool."
    exit 1
}

$gradlePath = $null
if (Test-Path "C:\tmp\gradle-8.10.2\bin\gradle.bat") {
    $gradlePath = "C:\tmp\gradle-8.10.2\bin\gradle.bat"
} elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
    $gradlePath = "gradle"
} else {
    throw "Gradle was not found."
}

Write-Output "Building Phone APK..."
& $gradlePath assemblePhoneRelease
if ($LASTEXITCODE -ne 0) { throw "Phone build failed." }

Write-Output ""
Write-Output "Building TV APK..."
& $gradlePath assembleTvRelease
if ($LASTEXITCODE -ne 0) { throw "TV build failed." }

Write-Output ""
Write-Output "=== Build Complete ==="
Write-Output ""

$phoneApk = Get-ChildItem "app\build\outputs\apk\phone\release\*.apk" | Select-Object -First 1
$tvApk = Get-ChildItem "app\build\outputs\apk\tv\release\*.apk" | Select-Object -First 1

if ($phoneApk) {
    Write-Output "Phone APK: $($phoneApk.FullName)"
    Write-Output "  Size: $([math]::Round($phoneApk.Length / 1MB, 2)) MB"
}

if ($tvApk) {
    Write-Output "TV APK:    $($tvApk.FullName)"
    Write-Output "  Size: $([math]::Round($tvApk.Length / 1MB, 2)) MB"
}

Write-Output ""
Write-Output "Rename APKs for GitHub Release:"
Write-Output "  Phone: SafePulse-phone-v1.5.0.apk"
Write-Output "  TV:    SafePulse-tv-v1.5.0.apk"
