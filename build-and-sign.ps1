<#
.SYNOPSIS
Validates, builds, aligns, signs and verifies the production arm64 APK on Windows.
.EXAMPLE
.\build-and-sign.ps1 -PublicSmsUciPath C:\private-data\SMSSpamCollection
.NOTES
Requires the existing private key and one-line password file outside this repository.
Never generates a key or falls back to debug signing. Android SDK build-tools 36.0.0
and JDK 21 are required. Tests and lint always run before release assembly.
#>
[CmdletBinding()]
param(
    [string]$KeyStore = (Join-Path $env:USERPROFILE 'private-signing\penny-circle-release.p12'),
    [string]$PasswordFile = (Join-Path $env:USERPROFILE 'private-signing\password.txt'),
    [string]$KeyAlias = 'pennycircle',
    [string]$AndroidSdk,
    [string]$JavaHome,
    [string]$BuildToolsVersion = '36.0.0',
    [string]$PublicSmsUciPath
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repo = [IO.Path]::GetFullPath($PSScriptRoot)
function Invoke-Checked {
    param([string]$Program, [string[]]$Arguments)
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Command failed ($LASTEXITCODE): $Program" }
}
function Resolve-PrivateFile([string]$Path) {
    $resolved = (Resolve-Path -LiteralPath $Path -ErrorAction Stop).ProviderPath
    if ($resolved.StartsWith($repo + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Signing files must be outside the repository.'
    }
    if (-not (Test-Path -LiteralPath $resolved -PathType Leaf)) { throw 'Signing file is not a regular file.' }
    return $resolved
}
function Test-NativePageAlignment([string]$ApkPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($ApkPath)
    $libraryCount = 0
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName -notmatch '^lib/arm64-v8a/[^/]+\.so$') { continue }
            $stream = $entry.Open()
            $memory = [IO.MemoryStream]::new()
            try { $stream.CopyTo($memory); $bytes = $memory.ToArray() }
            finally { $stream.Dispose(); $memory.Dispose() }
            if ($bytes.Length -lt 64 -or $bytes[0] -ne 127 -or $bytes[1] -ne 69 -or $bytes[2] -ne 76 -or $bytes[3] -ne 70 -or $bytes[4] -ne 2 -or $bytes[5] -ne 1) {
                throw "Expected a little-endian 64-bit ELF library: $($entry.FullName)"
            }
            $headersOffset = [BitConverter]::ToUInt64($bytes, 32)
            $headerSize = [BitConverter]::ToUInt16($bytes, 54)
            $headerCount = [BitConverter]::ToUInt16($bytes, 56)
            if ($headerSize -lt 56 -or $headersOffset + [uint64]$headerSize * $headerCount -gt $bytes.LongLength) {
                throw "Invalid ELF program headers: $($entry.FullName)"
            }
            $loadCount = 0
            for ($headerIndex = 0; $headerIndex -lt $headerCount; $headerIndex++) {
                $offset = [int]($headersOffset + [uint64]$headerIndex * $headerSize)
                if ([BitConverter]::ToUInt32($bytes, $offset) -ne 1) { continue }
                $loadCount++
                $alignment = [BitConverter]::ToUInt64($bytes, $offset + 48)
                if ($alignment -lt 16384) { throw "Native library needs 16 KB ELF load alignment: $($entry.FullName) ($alignment bytes)" }
            }
            if ($loadCount -eq 0) { throw "No ELF load segments: $($entry.FullName)" }
            $libraryCount++
        }
        if ($libraryCount -eq 0) { throw 'Release has no ARM64 native libraries to verify.' }
        Write-Output "Native ELF alignment verified: $libraryCount ARM64 libraries, every load segment >=16 KB."
    }
    finally { $archive.Dispose() }
}
$KeyStore = Resolve-PrivateFile $KeyStore
$PasswordFile = Resolve-PrivateFile $PasswordFile
if (-not $AndroidSdk) { $AndroidSdk = $env:ANDROID_HOME }
if (-not $AndroidSdk) { $AndroidSdk = $env:ANDROID_SDK_ROOT }
if (-not $AndroidSdk) {
    $localProperties = Join-Path $repo 'local.properties'
    if (Test-Path -LiteralPath $localProperties) {
        $sdkLine = Get-Content -LiteralPath $localProperties | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($sdkLine) { $AndroidSdk = $sdkLine.Substring(8).Replace('\:', ':').Replace('\\', '\') }
    }
}
if (-not $AndroidSdk) { throw 'Supply -AndroidSdk or configure ANDROID_HOME/local.properties.' }
$AndroidSdk = (Resolve-Path -LiteralPath $AndroidSdk).ProviderPath
if (-not $JavaHome) { $JavaHome = $env:JAVA_HOME }
if (-not $JavaHome) {
    $studioJdk = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'
    if (Test-Path -LiteralPath (Join-Path $studioJdk 'bin\java.exe')) { $JavaHome = $studioJdk }
}
if (-not $JavaHome) { throw 'Supply -JavaHome or configure JAVA_HOME for JDK 21.' }
$JavaHome = (Resolve-Path -LiteralPath $JavaHome).ProviderPath
$toolsDir = Join-Path $AndroidSdk "build-tools\$BuildToolsVersion"
$zipalign = Join-Path $toolsDir 'zipalign.exe'
$apksigner = Join-Path $toolsDir 'apksigner.bat'
$aapt = Join-Path $toolsDir 'aapt2.exe'
foreach ($tool in @($zipalign, $apksigner, $aapt, (Join-Path $JavaHome 'bin\java.exe'))) {
    if (-not (Test-Path -LiteralPath $tool -PathType Leaf)) { throw "Required tool missing: $tool" }
}
$previousJava = $env:JAVA_HOME
$previousSdk = $env:ANDROID_HOME
$tempFiles = @()
Push-Location -LiteralPath $repo
try {
    $env:JAVA_HOME = $JavaHome
    $env:ANDROID_HOME = $AndroidSdk
    Invoke-Checked (Join-Path $JavaHome 'bin\java.exe') @('-version')
    $buildLimits = @('-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8', '--max-workers=1')
    $gradleArgs = @(':core:test', ':app:testDebugUnitTest', ':app:lintRelease', '--console=plain') + $buildLimits
    if ($PublicSmsUciPath) {
        $corpus = (Resolve-Path -LiteralPath $PublicSmsUciPath).ProviderPath
        $gradleArgs += "-PpublicSmsUciPath=$corpus"
    }
    Invoke-Checked (Join-Path $repo 'gradlew.bat') $gradleArgs
    Invoke-Checked (Join-Path $repo 'gradlew.bat') (@(':app:assembleRelease', '--console=plain') + $buildLimits)
    $unsigned = Join-Path $repo 'app\build\outputs\apk\release\app-release-unsigned.apk'
    if (-not (Test-Path -LiteralPath $unsigned -PathType Leaf)) { throw 'Unsigned release APK missing; no signing fallback is permitted.' }
    $badging = & $aapt dump badging $unsigned
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect release APK manifest.' }
    $badgingText = $badging -join "`n"
    if ($badgingText -match 'application-debuggable') { throw 'Refusing to distribute a debuggable APK.' }
    if ($badgingText -notmatch "native-code: 'arm64-v8a'\s*(?:`r?`n|$)") { throw 'Release must contain only the arm64-v8a native ABI.' }
    Test-NativePageAlignment $unsigned
    if ($badgingText -notmatch "package: name='com.spendingbuddy.app' versionCode='(\d+)' versionName='([^']+)'") { throw 'Unexpected release package metadata.' }
    $versionCode = $Matches[1]
    $version = $Matches[2]
    if ($version -notmatch '^[0-9A-Za-z._-]+$') { throw 'Unsafe version name for artifact filename.' }
    $outputDir = Join-Path $repo 'release'
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    $apk = Join-Path $outputDir "Penny-Circle-$version-arm64.apk"
    $aligned = Join-Path $outputDir (([Guid]::NewGuid().ToString('N')) + '-aligned.apk')
    $staged = Join-Path $outputDir (([Guid]::NewGuid().ToString('N')) + '-signed.apk')
    $tempFiles = @($aligned, $staged)
    Invoke-Checked $zipalign @('-P', '16', '-f', '4', $unsigned, $aligned)
    Invoke-Checked $zipalign @('-c', '-P', '16', '4', $aligned)
    # This PKCS12 uses the same store/key password. apksigner tries the store password for the key.
    # Supplying the same one-line file twice would consume a nonexistent second line.
    # The password is read directly by apksigner, never into PowerShell or command arguments.
    Invoke-Checked $apksigner @('sign', '--ks', $KeyStore, '--ks-key-alias', $KeyAlias, '--ks-pass', "file:$PasswordFile", '--v4-signing-enabled', 'false', '--out', $staged, $aligned)
    $verification = & $apksigner verify --verbose --print-certs $staged
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    $verification | Write-Output
    $certLine = $verification | Where-Object { $_ -match '^Signer #1 certificate SHA-256 digest:' } | Select-Object -First 1
    if (-not $certLine) { throw 'Verified public certificate fingerprint missing.' }
    Invoke-Checked $zipalign @('-c', '-P', '16', '4', $staged)
    Move-Item -LiteralPath $staged -Destination $apk -Force
    $hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText("$apk.sha256", "$hash  $([IO.Path]::GetFileName($apk))`n")
    $metadata = @(
        'Penny Circle production distribution',
        "Package: com.spendingbuddy.app", "Version: $version (code $versionCode)",
        'ABI: arm64-v8a', 'Debuggable: false', 'APK ZIP and ELF load-segment alignment: 16 KB verified',
        "Built UTC: $([DateTime]::UtcNow.ToString('o'))", "APK: $([IO.Path]::GetFileName($apk))",
        "SHA-256: $hash", $certLine,
        'Validation: core tests, app debug unit tests, release lint, release assembly, signature verification.',
        'Real-device behavior requires separate verification.'
    ) -join "`n"
    [IO.File]::WriteAllText("$apk.metadata.txt", $metadata + "`n")
    Write-Output "Signed APK: $apk"
}
finally {
    foreach ($tempFile in $tempFiles) {
        # Delete only the exact uniquely named temporary files created above.
        if (Test-Path -LiteralPath $tempFile -PathType Leaf) { Remove-Item -LiteralPath $tempFile -Force }
    }
    $env:JAVA_HOME = $previousJava
    $env:ANDROID_HOME = $previousSdk
    Pop-Location
}
