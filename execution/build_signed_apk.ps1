# Yaqeen Gulf — Deterministic APK Build & Sideload Script (Layer 3 Execution Tool)
param(
    [string]$BuildType = "Debug", # "Debug" or "Release"
    [string]$AdbPath = "",
    [switch]$Install,
    [switch]$Launch
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Building Yaqeen Gulf APK ($BuildType)" -ForegroundColor Cyan
Write-Host " Project root: $projectRoot" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Dynamically resolve JAVA_HOME if not already set
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $foundJava = $null
    # Check if java is already available in PATH
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $javaBin = Split-Path -Parent $javaCmd.Source
        $foundJava = Split-Path -Parent $javaBin
    }
    
    if (-not $foundJava) {
        # Check standard JDK directories
        $candidatePaths = @(
            "$env:ProgramFiles\Microsoft\jdk-17*",
            "$env:ProgramFiles\Java\jdk-17*",
            "$env:ProgramFiles\Eclipse Adoptium\jdk-17*",
            "$env:ProgramFiles\Android\Android Studio\jbr"
        )
        foreach ($cand in $candidatePaths) {
            $match = Get-Item $cand -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($match -and (Test-Path "$($match.FullName)\bin\java.exe")) {
                $foundJava = $match.FullName
                break
            }
        }
    }

    if ($foundJava) {
        $env:JAVA_HOME = $foundJava
        $env:Path = "$foundJava\bin;" + $env:Path
        Write-Host "Dynamically resolved JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
    } else {
        Write-Error "JAVA_HOME is not set and no suitable JDK 17 was found in standard system locations."
    }
}

# 2. Dynamically resolve ADB
function Resolve-Adb {
    param([string]$OverridePath, [string]$RootDir)
    if ($OverridePath -and (Test-Path $OverridePath)) {
        return (Resolve-Path $OverridePath).Path
    }
    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCmd) {
        return $adbCmd.Source
    }
    if ($env:ANDROID_HOME -and (Test-Path "$env:ANDROID_HOME\platform-tools\adb.exe")) {
        return "$env:ANDROID_HOME\platform-tools\adb.exe"
    }
    if ($env:ANDROID_SDK_ROOT -and (Test-Path "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe")) {
        return "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
    }
    # Parse local.properties if present
    $localProps = Join-Path $RootDir "local.properties"
    if (Test-Path $localProps) {
        $sdkLine = Get-Content $localProps | Where-Object { $_ -match "^sdk\.dir\s*=" } | Select-Object -First 1
        if ($sdkLine) {
            $rawDir = ($sdkLine -split "=", 2)[1].Trim()
            $cleanedDir = $rawDir -replace '\\:', ':' -replace '\\\\', '\'
            $candidateAdb = Join-Path $cleanedDir "platform-tools\adb.exe"
            if (Test-Path $candidateAdb) {
                return $candidateAdb
            }
        }
    }
    return $null
}

Set-Location $projectRoot

$gradlew = Join-Path $projectRoot "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    Write-Error "gradlew.bat not found at $gradlew"
}

$task = ":app:assemble$BuildType"
Write-Host "Executing: $gradlew $task" -ForegroundColor Yellow

& $gradlew $task

if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed with exit code $LASTEXITCODE"
}

$apkDir = Join-Path $projectRoot "app\build\outputs\apk\$($BuildType.ToLower())"
$apks = Get-ChildItem -Path $apkDir -Filter "*.apk" -ErrorAction SilentlyContinue

if ($apks) {
    Write-Host "`nBuild Succeeded! Generated APK(s):" -ForegroundColor Green
    foreach ($apk in $apks) {
        $sizeMB = [math]::Round($apk.Length / 1MB, 2)
        Write-Host " -> $($apk.FullName) ($sizeMB MB)" -ForegroundColor Green
    }

    if ($Install) {
        $resolvedAdb = Resolve-Adb -OverridePath $AdbPath -RootDir $projectRoot
        if (-not $resolvedAdb) {
            Write-Warning "ADB executable not found. Please specify -AdbPath or ensure Android SDK platform-tools is installed."
        } else {
            $targetApk = $apks[0].FullName
            Write-Host "`nInstalling APK via ADB: $resolvedAdb install -r $targetApk" -ForegroundColor Cyan
            & $resolvedAdb install -r $targetApk
            if ($Launch) {
                Write-Host "Launching Yaqeen Gulf on device..." -ForegroundColor Cyan
                & $resolvedAdb shell am start -n com.yaqeengulf.deepfake/.MainActivity
            }
        }
    }
} else {
    Write-Warning "Build finished successfully but no APK files were found in $apkDir"
}
