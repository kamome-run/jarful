# Run in a normal Windows PowerShell session with network access:
# powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/Verify-Jdk25.ps1
# Downloads build dependencies, runs tests, and creates Windows/Android packages.
# It does not install Jarful, change saved app data, or publish anything.
param(
    [string] $JdkHome = '',
    [string] $AndroidSdk = '',
    [switch] $CheckOnly
)
$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$support = Join-Path $repo 'build/jdk25-support'
$log = Join-Path $support 'verification.log'

function Invoke-Checked([string] $program, [string[]] $arguments) {
    # Native tools can emit informational messages on stderr in PowerShell 5.1.
    $savedPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $program @arguments
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $savedPreference }
    if ($code -ne 0) { throw "$program exited with code $code. Log: $log" }
}

function Find-Jdk25 {
    $candidates = @()
    if ($JdkHome) { $candidates = @($JdkHome) }
    else {
        if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
        $adoptium = Join-Path $env:ProgramFiles 'Eclipse Adoptium'
        if (Test-Path -LiteralPath $adoptium) {
            $candidates += @(Get-ChildItem -LiteralPath $adoptium -Directory -Filter 'jdk-25*' | Sort-Object Name -Descending | ForEach-Object FullName)
        }
    }
    foreach ($candidate in $candidates) {
        $compiler = Join-Path $candidate 'bin/javac.exe'
        if (Test-Path -LiteralPath $compiler) {
            $version = & $compiler -version
            if ($LASTEXITCODE -eq 0 -and "$version" -match '^javac 25(?:\.|\s|$)') { return [IO.Path]::GetFullPath($candidate) }
        }
    }
    throw 'JDK 25 not found. Install Temurin JDK 25 or supply -JdkHome.'
}

function Install-AndroidTools([string] $sdk) {
    $manager = Join-Path $sdk 'cmdline-tools/latest/bin/sdkmanager.bat'
    if (Test-Path -LiteralPath $manager) { return $manager }
    $existing = @(Get-ChildItem -LiteralPath (Join-Path $sdk 'cmdline-tools') -Filter sdkmanager.bat -Recurse -ErrorAction SilentlyContinue | Sort-Object FullName -Descending | Select-Object -First 1)
    if ($existing.Count -gt 0) { return $existing[0].FullName }

    # Official Windows command-line tools and SHA-256 from developer.android.com/studio.
    $url = 'https://dl.google.com/android/repository/commandlinetools-win-15859902_latest.zip'
    $checksum = '90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a'
    $archive = Join-Path $support 'android-commandlinetools.zip'
    if (-not (Test-Path -LiteralPath $archive) -or (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ne $checksum) {
        $savedProgress = $ProgressPreference
        try {
            # PowerShell 5.1's per-block progress updates can greatly slow large downloads.
            $ProgressPreference = 'SilentlyContinue'
            Invoke-WebRequest -Uri $url -OutFile $archive -UseBasicParsing -TimeoutSec 600
        } finally { $ProgressPreference = $savedProgress }
    }
    if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ne $checksum) { throw 'Android command-line tools checksum mismatch' }
    $unpack = Join-Path $support ('unpack-' + [Guid]::NewGuid().ToString('N'))
    # The SDK contains paths exceeding PowerShell 5.1/.NET Framework MAX_PATH.
    # JDK 25's ZIP extractor handles these paths using the Windows long-path API.
    [IO.Directory]::CreateDirectory($unpack) | Out-Null
    Push-Location -LiteralPath $unpack
    try { Invoke-Checked (Join-Path $env:JAVA_HOME 'bin/jar.exe') @('--extract', '--file', $archive) }
    finally { Pop-Location }
    $from = (Resolve-Path -LiteralPath (Join-Path $unpack 'cmdline-tools')).Path
    $to = [IO.Path]::GetFullPath((Join-Path $sdk 'cmdline-tools/latest'))
    # Verify the exact recursive move endpoints before moving the extracted tools.
    if (-not $from.StartsWith($support + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unexpected extraction path' }
    if (-not $to.StartsWith([IO.Path]::GetFullPath($sdk).TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Unexpected SDK destination' }
    if (Test-Path -LiteralPath $to) { throw "Incomplete Android tools at $to; inspect that directory before retrying." }
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($to)) | Out-Null
    Move-Item -LiteralPath $from -Destination $to
    return $manager
}

$jdk = Find-Jdk25
if (-not $AndroidSdk) {
    if ($env:ANDROID_HOME) { $AndroidSdk = $env:ANDROID_HOME }
    elseif ($env:ANDROID_SDK_ROOT) { $AndroidSdk = $env:ANDROID_SDK_ROOT }
    elseif (Test-Path -LiteralPath (Join-Path $env:LOCALAPPDATA 'Android/Sdk')) { $AndroidSdk = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
    else { $AndroidSdk = Join-Path $support 'android-sdk' }
}
$AndroidSdk = [IO.Path]::GetFullPath($AndroidSdk)
Write-Output "JDK 25: $jdk"
Write-Output "Android SDK: $AndroidSdk"
Write-Output "Gradle cache: $(Join-Path $repo 'build/jdk25-gradle-cache')"
if ($CheckOnly) { return }

[IO.Directory]::CreateDirectory($support) | Out-Null
$savedEnvironment = @{}
foreach ($name in @('JAVA_HOME', 'ANDROID_HOME', 'ANDROID_SDK_ROOT', 'GRADLE_USER_HOME')) {
    $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$transcribing = $false
$savedSecurityProtocol = [Net.ServicePointManager]::SecurityProtocol
Push-Location -LiteralPath $repo
try {
    Start-Transcript -LiteralPath $log -Force | Out-Null
    $transcribing = $true
    $env:JAVA_HOME = $jdk
    $env:ANDROID_HOME = $AndroidSdk
    $env:ANDROID_SDK_ROOT = $AndroidSdk
    $env:GRADLE_USER_HOME = Join-Path $repo 'build/jdk25-gradle-cache'
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $manager = Install-AndroidTools $AndroidSdk
    # sdkmanager presents Google's license agreements for the user to review.
    Invoke-Checked $manager @("--sdk_root=$AndroidSdk", '--licenses')
    Invoke-Checked $manager @("--sdk_root=$AndroidSdk", 'platforms;android-36', 'build-tools;35.0.0', 'platform-tools')
    $wrapper = Join-Path $repo 'gradlew.bat'
    Invoke-Checked $wrapper @('--version')
    Invoke-Checked $wrapper @(':shared:desktopTest', ':desktopApp:test', ':androidApp:assembleDebug', ':androidApp:assembleRelease', ':desktopApp:createDistributable', ':desktopApp:packageMsi', ':desktopApp:packageExe', '--no-daemon', '--stacktrace')
    Write-Output 'PASS: tests and Windows/Android builds completed.'
    Write-Output "Windows app: $(Join-Path $repo 'desktopApp/build/compose/binaries/main/app/Jarful/Jarful.exe')"
} finally {
    if ($transcribing) { Stop-Transcript | Out-Null }
    Pop-Location
    foreach ($name in $savedEnvironment.Keys) { [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process') }
    [Net.ServicePointManager]::SecurityProtocol = $savedSecurityProtocol
}
