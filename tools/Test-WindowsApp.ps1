# Starts the packaged app with isolated data, checks native startup, then closes it.
param([int] $StartupSeconds = 15, [switch] $CaptureExceptions)
$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$app = Join-Path $repo 'desktopApp/build/compose/binaries/main/app/Jarful/Jarful.exe'
$support = Join-Path $repo 'build/jdk25-support'
$run = Join-Path $support ('app-smoke-' + [Guid]::NewGuid().ToString('N'))
if (-not (Test-Path -LiteralPath $app -PathType Leaf)) { throw 'Build :desktopApp:createDistributable first.' }
if ($StartupSeconds -lt 5 -or $StartupSeconds -gt 60) { throw 'StartupSeconds must be between 5 and 60.' }
[IO.Directory]::CreateDirectory($run) | Out-Null
$stdout = Join-Path $run 'stdout.log'
$stderr = Join-Path $run 'stderr.log'
$savedAppData = $env:APPDATA
$savedJavaOptions = $env:JAVA_TOOL_OPTIONS
$player = $null
$application = $null
try {
    $env:APPDATA = $run
    if ($CaptureExceptions) {
        $env:JAVA_TOOL_OPTIONS = ($savedJavaOptions + ' -Xlog:exceptions=info:file=exceptions.log').Trim()
    }
    $player = Start-Process -FilePath $app -WorkingDirectory $run -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    if ($player.WaitForExit($StartupSeconds * 1000)) { throw "Packaged app exited early with code $($player.ExitCode). Logs: $run" }
    $player.Refresh()
    $children = @(Get-CimInstance Win32_Process -Filter "ParentProcessId = $($player.Id)")
    $children |
        Select-Object ProcessId,ParentProcessId,ExecutablePath,CommandLine |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $run 'child-processes.json') -Encoding UTF8
    # jpackage relaunches itself on Windows; the child loads the JVM while
    # the original launcher waits. Inspect only our exact executable's child.
    $jvmChild = $children | Where-Object { $_.ExecutablePath -eq $app } | Select-Object -First 1
    $application = if ($null -ne $jvmChild) { Get-Process -Id $jvmChild.ProcessId } else { $player }
    $output = (Get-Content -LiteralPath $stdout,$stderr -Raw -ErrorAction SilentlyContinue) -join "`n"
    if ($output -match 'Exception in thread|NoClassDefFoundError|UnsatisfiedLinkError|NoSuchMethodError|FATAL ERROR') {
        throw "Packaged app reported a startup failure. Logs: $run"
    }
    if ($application.HasExited -or -not $application.Responding) {
        throw "Packaged app stopped responding. Logs: $run"
    }
    $modules = @($application.Modules | ForEach-Object FileName)
    $modules | Set-Content -LiteralPath (Join-Path $run 'native-modules.log') -Encoding UTF8
    $runtime = [IO.Path]::GetFullPath((Join-Path (Split-Path $app) 'runtime')) + [IO.Path]::DirectorySeparatorChar
    if (-not ($modules | Where-Object { $_.StartsWith($runtime, [StringComparison]::OrdinalIgnoreCase) -and $_ -like '*\jvm.dll' })) {
        throw 'The app did not load its bundled JDK runtime.'
    }
    if (-not ($modules | Where-Object { $_ -match 'skiko.*\.dll$' })) { throw 'Skiko native rendering library was not loaded.' }
    Write-Output "PASS: packaged JDK 25 app stayed running and loaded bundled JVM/Skiko ($StartupSeconds seconds). Logs: $run"
} finally {
    $env:APPDATA = $savedAppData
    $env:JAVA_TOOL_OPTIONS = $savedJavaOptions
    if ($null -ne $application -and $application.Id -ne $player.Id) {
        if (-not $application.HasExited) {
            $null = $application.CloseMainWindow()
            if (-not $application.WaitForExit(5000)) { $application.Kill(); $application.WaitForExit() }
        }
        $application.Dispose()
    }
    if ($null -ne $player) {
        if (-not $player.HasExited) {
            $null = $player.CloseMainWindow()
            if (-not $player.WaitForExit(5000)) { $player.Kill(); $player.WaitForExit() }
        }
        $player.Dispose()
    }
}
