param([string]$Round = "p1")
# Walks onboarding on emulator-5554 and captures screens in each theme. Uses uiautomator to find buttons by text or content description.
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$pkg = 'com.spendingbuddy.app'
$out = "C:\Users\User\AppData\Local\Temp\claude\ec29f026d20d029c\d5c06396-29cf-4cbf-8d2d-60f24af756c5\scratchpad\emu\$Round"
New-Item -ItemType Directory -Force $out | Out-Null
function Shot($name) { Start-Sleep -Milliseconds 1300; & $adb -s emulator-5554 shell screencap -p /sdcard/cap.png; & $adb -s emulator-5554 pull /sdcard/cap.png "$out\$name.png" | Out-Null }
function Dump { & $adb -s emulator-5554 shell uiautomator dump /sdcard/ui.xml | Out-Null; return ((& $adb -s emulator-5554 exec-out cat /sdcard/ui.xml) -join '') }
function TapMatch($pattern) {
    $x = Dump
    $m = [regex]::Matches($x, '<node[^>]*?(?:text|content-desc)="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
    foreach ($n in $m) { if ($n.Groups[1].Value -match $pattern) {
        $cx = [int](([int]$n.Groups[2].Value + [int]$n.Groups[4].Value) / 2); $cy = [int](([int]$n.Groups[3].Value + [int]$n.Groups[5].Value) / 2)
        & $adb -s emulator-5554 shell input tap $cx $cy; return $true } }
    Write-Host "NOT FOUND: $pattern"; return $false
}
& $adb -s emulator-5554 shell pm clear $pkg | Out-Null
& $adb -s emulator-5554 shell am start -n "$pkg/.MainActivity" | Out-Null
Start-Sleep -Seconds 4
Shot "01_name"
TapMatch '^Continue$' | Out-Null; Shot "02_theme_light"
foreach ($t in @(@(780,888,'03_theme_violet'), @(294,1392,'04_theme_mint'), @(780,1392,'05_theme_blue'))) { & $adb -s emulator-5554 shell input tap $t[0] $t[1]; Shot $t[2] }
TapMatch '^Continue$' | Out-Null; Shot "06_limits_blue"
TapMatch '^Continue$' | Out-Null; Shot "07_reminders_blue"
TapMatch '^Not now$' | Out-Null; Shot "08_today_blue"
& $adb -s emulator-5554 shell input tap 996 250; Shot "09_settings_blue"
& $adb -s emulator-5554 shell input swipe 540 1700 540 900 400; Shot "10_settings_scrolled"
"captured to $out"; Get-ChildItem $out | Select-Object -ExpandProperty Name
