$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (Test-Path ".\gradlew.bat") {
    & .\gradlew.bat assembleDebug
} elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
    gradle assembleDebug
} else {
    Write-Host "No Gradle wrapper or Gradle installation was found." -ForegroundColor Red
    Write-Host "Open this project in Android Studio, let Gradle Sync finish, then choose Build > Build APK(s)."
    exit 1
}

$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
if (!(Test-Path $apk)) { throw "APK was not produced: $apk" }
Copy-Item $apk (Join-Path $root "Jane-Mobile-v1-debug.apk") -Force
Write-Host "Built: Jane-Mobile-v1-debug.apk" -ForegroundColor Green
