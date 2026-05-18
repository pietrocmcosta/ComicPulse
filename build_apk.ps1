# ComicPulse Automated Build & Deploy Script
# Compiles the debug APK and copies it to C:\temp\comic_pulse_<versionName>.apk

Write-Host "--------------------------------------------------" -ForegroundColor Cyan
Write-Host "Starting ComicPulse APK Build Process..." -ForegroundColor Cyan
Write-Host "--------------------------------------------------" -ForegroundColor Cyan

# 1. Dynamically read versionName from app/build.gradle.kts
$gradleFile = "app/build.gradle.kts"
$versionName = "1.0"
if (Test-Path $gradleFile) {
    $content = Get-Content $gradleFile -Raw
    if ($content -match 'versionName\s*=\s*"([^"]+)"') {
        $versionName = $Matches[1]
    }
}
Write-Host "Resolved App Version: $versionName" -ForegroundColor Yellow

# 1.5. Copy Generated Icon to Android Mipmap Folders
$iconSource = "C:\Users\pietr\.gemini\antigravity\brain\abc7e71d-9cec-40b7-8acf-a934c179af86\comic_pulse_logo_1779049799860.png"
if (Test-Path $iconSource) {
    Write-Host "Copying Launcher Icon..." -ForegroundColor Yellow
    $resDir = "app\src\main\res"
    $mipmaps = @("mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi")
    foreach ($mipmap in $mipmaps) {
        $targetDir = "$resDir\$mipmap"
        if (!(Test-Path $targetDir)) {
            New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
        }
        Copy-Item -Path $iconSource -Destination "$targetDir\ic_launcher.png" -Force
        Copy-Item -Path $iconSource -Destination "$targetDir\ic_launcher_round.png" -Force
    }
    Write-Host "Icon copied successfully!" -ForegroundColor Green
}

# 2. Run Gradle assembleDebug
& "C:\Gradle\gradle-8.7\bin\gradle.bat" assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build Successful!" -ForegroundColor Green
    
    # 3. Ensure C:\temp exists
    if (!(Test-Path "C:\temp")) {
        New-Item -ItemType Directory -Path "C:\temp" | Out-Null
        Write-Host "Created directory C:\temp" -ForegroundColor Yellow
    }
    
    # 4. Copy the APK with dynamic version name
    $srcApk = "app/build/outputs/apk/debug/app-debug.apk"
    $destApk = "C:\temp\comic_pulse_$versionName.apk"
    
    if (Test-Path $srcApk) {
        Copy-Item -Path $srcApk -Destination $destApk -Force
        Write-Host "Success! APK copied to: $destApk" -ForegroundColor Green
    } else {
        Write-Host "Error: Compiled APK not found at: $srcApk" -ForegroundColor Red
    }
} else {
    Write-Host "Error: Gradle build failed with exit code $LASTEXITCODE" -ForegroundColor Red
}
