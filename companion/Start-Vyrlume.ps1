[CmdletBinding()]
param(
    [string]$Workspace,
    [ValidateRange(1, 65535)]
    [int]$Port = 8080,
    [string]$Serial,
    [string]$Package,
    [string]$ApkPath,
    [switch]$NoLaunch
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

$RepoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($Workspace)) {
    $Workspace = $RepoRoot
}
$ToolchainRoot = Join-Path $RepoRoot '.toolchain'
$RuntimeRoot = Join-Path $ToolchainRoot 'code-server-runtime'
$LogRoot = Join-Path $RepoRoot 'evidence\launcher'
$LauncherLog = Join-Path $LogRoot 'launcher.log'
$CodeServerVersion = '4.143.1'
$CodeServerDirectoryName = "code-server-$CodeServerVersion-windows-amd64"
$CodeServerArchiveName = "$CodeServerDirectoryName.tar.gz"
$CodeServerDownloadUrl = "https://github.com/coder/code-server/releases/download/v$CodeServerVersion/$CodeServerArchiveName"
$CodeServerSha256 = 'a480ccbc368336370baaa16336143e1d8bb9868e66edb2ea3f076ebf0527080a'

New-Item -ItemType Directory -Path $LogRoot -Force | Out-Null

function Write-LauncherLog {
    param([string]$Message)

    $line = '[{0}] {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Message
    Write-Host $line
    Add-Content -LiteralPath $LauncherLog -Value $line -Encoding UTF8
}

function Invoke-NativeCommand {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    $output = & $FilePath @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    $text = ($output | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine
    if ($exitCode -ne 0) {
        throw "Command failed with exit code ${exitCode}: $FilePath $($Arguments -join ' ')`n$text"
    }
    return $text
}

function Find-Adb {
    $candidates = @(
        (Join-Path $ToolchainRoot 'android-sdk\platform-tools\adb.exe')
    )

    foreach ($sdkVariable in @('ANDROID_HOME', 'ANDROID_SDK_ROOT')) {
        $sdkRoot = [Environment]::GetEnvironmentVariable($sdkVariable)
        if (-not [string]::IsNullOrWhiteSpace($sdkRoot)) {
            $candidates += Join-Path $sdkRoot 'platform-tools\adb.exe'
        }
    }

    $localAppData = [Environment]::GetEnvironmentVariable('LOCALAPPDATA')
    if (-not [string]::IsNullOrWhiteSpace($localAppData)) {
        $candidates += Join-Path $localAppData 'Android\Sdk\platform-tools\adb.exe'
    }

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    throw 'adb.exe was not found. Checked the repo Android SDK, ANDROID_HOME, ANDROID_SDK_ROOT, and the LOCALAPPDATA Android SDK.'
}

function Select-AndroidDevice {
    param([Parameter(Mandatory = $true)][string]$AdbPath)

    $deviceText = Invoke-NativeCommand -FilePath $AdbPath -Arguments @('devices')
    $records = @()
    foreach ($line in ($deviceText -split '\r?\n')) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or
            $trimmed.StartsWith('List of devices attached') -or
            $trimmed.StartsWith('*')) {
            continue
        }

        $parts = $trimmed -split '\s+'
        if ($parts.Count -ge 2) {
            $records += [PSCustomObject]@{ Serial = $parts[0]; State = $parts[1] }
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($Serial)) {
        $selected = @($records | Where-Object { $_.Serial -eq $Serial })
        if ($selected.Count -eq 0) {
            throw "Device '$Serial' was not found by adb devices. Reconnect it and check the serial, or omit -Serial."
        }
        if ($selected[0].State -ne 'device') {
            throw "Device '$Serial' is $($selected[0].State), not online. Reconnect it, authorize USB debugging, and check the phone screen."
        }
        return $selected[0].Serial
    }

    $online = @($records | Where-Object { $_.State -eq 'device' })
    if ($online.Count -gt 1) {
        $serials = ($online | ForEach-Object { $_.Serial }) -join ', '
        throw "More than one online Android device was found ($serials). Re-run with -Serial <serial>."
    }
    if ($online.Count -eq 1) {
        return $online[0].Serial
    }

    if ($records.Count -gt 0) {
        $states = ($records | ForEach-Object { "$($_.Serial) ($($_.State))" }) -join ', '
        throw "No online Android device is available. adb reports: $states. Reconnect the device, authorize USB debugging, and check for offline/unauthorized state."
    }

    throw 'No Android device is connected. Connect exactly one device with USB debugging enabled, or select one with -Serial.'
}

function Test-CodeServerHealth {
    param([Parameter(Mandatory = $true)][int]$HealthPort)

    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:$HealthPort/healthz" `
            -UseBasicParsing -TimeoutSec 2 -MaximumRedirection 2 -ErrorAction Stop
        return ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400)
    }
    catch {
        return $false
    }
}

function Resolve-CodeServer {
    $repoCommand = Join-Path $ToolchainRoot "$CodeServerDirectoryName\bin\code-server.cmd"
    if (Test-Path -LiteralPath $repoCommand -PathType Leaf) {
        return (Resolve-Path -LiteralPath $repoCommand).Path
    }

    foreach ($commandName in @('code-server.cmd', 'code-server')) {
        $command = Get-Command $commandName -CommandType Application -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($null -ne $command) {
            return $command.Source
        }
    }

    Write-LauncherLog "code-server was not found; downloading pinned official release $CodeServerVersion."
    $downloadRoot = Join-Path $ToolchainRoot 'downloads'
    New-Item -ItemType Directory -Path $downloadRoot -Force | Out-Null
    $archive = Join-Path $downloadRoot $CodeServerArchiveName

    if (Test-Path -LiteralPath $archive -PathType Leaf) {
        $existingHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($existingHash -ne $CodeServerSha256) {
            throw "Existing code-server archive has the wrong SHA256. Expected $CodeServerSha256 but got $existingHash. Remove '$archive' and retry."
        }
    }
    else {
        [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
        $temporaryArchive = "$archive.download"
        if (Test-Path -LiteralPath $temporaryArchive) {
            Remove-Item -LiteralPath $temporaryArchive -Force
        }

        Invoke-WebRequest -Uri $CodeServerDownloadUrl -OutFile $temporaryArchive -UseBasicParsing
        $downloadedHash = (Get-FileHash -LiteralPath $temporaryArchive -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($downloadedHash -ne $CodeServerSha256) {
            Remove-Item -LiteralPath $temporaryArchive -Force
            throw "Downloaded code-server archive failed SHA256 verification. Expected $CodeServerSha256 but got $downloadedHash."
        }
        Move-Item -LiteralPath $temporaryArchive -Destination $archive
    }

    $tar = Get-Command 'tar.exe' -CommandType Application -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($null -eq $tar) {
        throw 'tar.exe is required to extract the verified code-server archive but was not found.'
    }

    Write-LauncherLog 'SHA256 verified; extracting code-server into the repo toolchain.'
    [void](Invoke-NativeCommand -FilePath $tar.Source -Arguments @('-xzf', $archive, '-C', $ToolchainRoot))
    if (-not (Test-Path -LiteralPath $repoCommand -PathType Leaf)) {
        throw "Extraction completed but '$repoCommand' is missing."
    }
    return (Resolve-Path -LiteralPath $repoCommand).Path
}

function Quote-NativeArgument {
    param([AllowEmptyString()][string]$Value)

    if ($null -eq $Value -or $Value.Length -eq 0) {
        return '""'
    }
    if ($Value -notmatch '[\s"]') {
        return $Value
    }

    $escaped = [regex]::Replace($Value, '(\\*)"', '$1$1\"')
    $escaped = [regex]::Replace($escaped, '(\\+)$', '$1$1')
    return '"' + $escaped + '"'
}

function Start-CodeServer {
    param(
        [Parameter(Mandatory = $true)][string]$CodeServerPath,
        [Parameter(Mandatory = $true)][string]$WorkspacePath
    )

    if (Test-CodeServerHealth -HealthPort $Port) {
        Write-LauncherLog "Reusing healthy code-server at http://127.0.0.1:$Port/."
        return
    }

    $userData = Join-Path $RuntimeRoot 'user-data'
    $extensions = Join-Path $RuntimeRoot 'extensions'
    New-Item -ItemType Directory -Path $userData -Force | Out-Null
    New-Item -ItemType Directory -Path $extensions -Force | Out-Null

    $serverOut = Join-Path $LogRoot 'code-server.stdout.log'
    $serverErr = Join-Path $LogRoot 'code-server.stderr.log'
    $serverArguments = @(
        '--bind-addr', "127.0.0.1:$Port",
        '--auth', 'none',
        '--user-data-dir', $userData,
        '--extensions-dir', $extensions,
        $WorkspacePath
    )
    $argumentLine = ($serverArguments | ForEach-Object { Quote-NativeArgument $_ }) -join ' '

    Write-LauncherLog "Starting code-server bound only to 127.0.0.1:$Port."
    if ([IO.Path]::GetExtension($CodeServerPath) -ieq '.cmd') {
        $innerCommand = (Quote-NativeArgument $CodeServerPath) + ' ' + $argumentLine
        $commandLine = '"' + $innerCommand + '"'
        $commandProcessor = Join-Path ([Environment]::SystemDirectory) 'cmd.exe'
        $serverProcess = Start-Process -FilePath $commandProcessor `
            -ArgumentList @('/d', '/s', '/c', $commandLine) `
            -WindowStyle Hidden -RedirectStandardOutput $serverOut `
            -RedirectStandardError $serverErr -PassThru
    }
    else {
        $serverProcess = Start-Process -FilePath $CodeServerPath -ArgumentList $argumentLine `
            -WindowStyle Hidden -RedirectStandardOutput $serverOut `
            -RedirectStandardError $serverErr -PassThru
    }

    $deadline = [DateTime]::UtcNow.AddSeconds(45)
    while ([DateTime]::UtcNow -lt $deadline) {
        if (Test-CodeServerHealth -HealthPort $Port) {
            Write-LauncherLog 'code-server is healthy.'
            return
        }
        if ($serverProcess.HasExited) {
            throw "code-server exited with code $($serverProcess.ExitCode) before becoming healthy. See '$serverErr'."
        }
        Start-Sleep -Milliseconds 750
        $serverProcess.Refresh()
    }

    throw "code-server did not become healthy within 45 seconds. It has not been killed; inspect '$serverErr'."
}

function Test-InstalledPackage {
    param(
        [Parameter(Mandatory = $true)][string]$AdbPath,
        [Parameter(Mandatory = $true)][string[]]$AdbArguments,
        [Parameter(Mandatory = $true)][string]$PackageName
    )

    if ($PackageName -notmatch '^[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)+$') {
        throw "Invalid Android package name '$PackageName'."
    }
    $output = Invoke-NativeCommand -FilePath $AdbPath -Arguments ($AdbArguments + @('shell', 'pm', 'list', 'packages', $PackageName))
    $entries = @($output -split '\r?\n' | ForEach-Object { $_.Trim() })
    return ($entries -contains "package:$PackageName")
}

try {
    $resolvedWorkspace = (Resolve-Path -LiteralPath $Workspace -ErrorAction Stop).ProviderPath
    if (-not (Test-Path -LiteralPath $resolvedWorkspace -PathType Container)) {
        throw "Workspace is not a directory: '$resolvedWorkspace'."
    }

    $adb = Find-Adb
    $selectedSerial = Select-AndroidDevice -AdbPath $adb
    $adbArguments = @('-s', $selectedSerial)
    Write-LauncherLog "Using Android device $selectedSerial."

    $codeServer = Resolve-CodeServer
    Start-CodeServer -CodeServerPath $codeServer -WorkspacePath $resolvedWorkspace

    [void](Invoke-NativeCommand -FilePath $adb -Arguments ($adbArguments + @('reverse', "tcp:$Port", "tcp:$Port")))
    Write-LauncherLog "adb reverse is active for tcp:$Port on $selectedSerial."

    $folder = [Uri]::EscapeDataString($resolvedWorkspace)
    $editorUrl = "http://127.0.0.1:$Port/?folder=$folder"
    $deepLink = 'vyrlume://connect?url={0}&name={1}' -f
        [Uri]::EscapeDataString($editorUrl), [Uri]::EscapeDataString('USB VS Code')

    if ($NoLaunch) {
        Write-LauncherLog "NoLaunch was specified; skipping package detection, installation, and app launch."
        Write-LauncherLog "Editor URL: $editorUrl"
        Write-LauncherLog "Deep link: $deepLink"
        Write-LauncherLog 'Vyrlume USB companion is ready; code-server and adb reverse remain running.'
        exit 0
    }

    if ([string]::IsNullOrWhiteSpace($Package)) {
        $packageCandidates = @('app.vyrlume.mobile', 'app.vyrlume.mobile.debug')
    }
    else {
        $packageCandidates = @($Package)
    }

    $targetPackage = $null
    foreach ($candidate in $packageCandidates) {
        if (Test-InstalledPackage -AdbPath $adb -AdbArguments $adbArguments -PackageName $candidate) {
            $targetPackage = $candidate
            break
        }
    }

    if ($null -eq $targetPackage) {
        if ([string]::IsNullOrWhiteSpace($ApkPath)) {
            $installApk = Join-Path $RepoRoot 'dist\Vyrlume-0.1.0-debug.apk'
        }
        else {
            $installApk = $ApkPath
        }

        if (-not (Test-Path -LiteralPath $installApk -PathType Leaf)) {
            throw "No supported Vyrlume package is installed and no APK was found at '$installApk'. Build the app or pass -ApkPath <path>."
        }

        $installApk = (Resolve-Path -LiteralPath $installApk).Path
        Write-LauncherLog "Installing Vyrlume from '$installApk'."
        [void](Invoke-NativeCommand -FilePath $adb -Arguments ($adbArguments + @('install', '-r', $installApk)))

        foreach ($candidate in $packageCandidates) {
            if (Test-InstalledPackage -AdbPath $adb -AdbArguments $adbArguments -PackageName $candidate) {
                $targetPackage = $candidate
                break
            }
        }
        if ($null -eq $targetPackage) {
            throw "The APK install command finished, but none of the expected packages was found: $($packageCandidates -join ', ')."
        }
    }

    Write-LauncherLog "Launching $targetPackage with $deepLink"
    $launchCommand = "am start -W -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d '$deepLink' $targetPackage"
    $launchOutput = Invoke-NativeCommand -FilePath $adb -Arguments ($adbArguments + @('shell', $launchCommand))
    if ($launchOutput -match '(?im)^\s*(Error|Exception)\b' -or $launchOutput -match '(?i)ActivityNotFoundException') {
        throw "Android reported an app launch failure:`n$launchOutput"
    }

    Write-LauncherLog 'Vyrlume launched successfully; code-server and adb reverse remain running.'
    Write-LauncherLog "Editor URL: $editorUrl"
    exit 0
}
catch {
    $message = $_.Exception.Message
    Write-LauncherLog "ERROR: $message"
    [Console]::Error.WriteLine("Vyrlume USB launcher failed: $message")
    exit 1
}
