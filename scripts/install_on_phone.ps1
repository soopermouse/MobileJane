$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$apk = Join-Path $root "Jane-Mobile-v1-debug.apk"
if (!(Test-Path $apk)) {
    & (Join-Path $PSScriptRoot "build_debug_apk.ps1")
}
if (!(Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "adb was not found. Install Android SDK Platform Tools or run from Android Studio's terminal."
}
adb devices
adb install -r $apk
Write-Host "Jane Mobile installed. Open Jane on the phone." -ForegroundColor Green
