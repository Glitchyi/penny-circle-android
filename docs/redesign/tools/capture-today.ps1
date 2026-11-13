param([string]$Round = "q1")
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$out = "C:\Users\User\AppData\Local\Temp\claude\ec29f026d20d029c\d5c06396-29cf-4cbf-8d2d-60f24af756c5\scratchpad\emu\$Round"
New-Item -ItemType Directory -Force $out | Out-Null
function Shot($name) { Start-Sleep -Milliseconds 1500; & $adb -s emulator-5554 shell screencap -p /sdcard/cap.png; & $adb -s emulator-5554 pull /sdcard/cap.png "$out\$name.png" | Out-Null }
function Tap($x, $y) { & $adb -s emulator-5554 shell input tap $x $y }
& $adb -s emulator-5554 shell am force-stop com.spendingbuddy.app; & $adb -s emulator-5554 shell am start -n com.spendingbuddy.app/.MainActivity | Out-Null; Start-Sleep -Seconds 7
# theme cards in Settings (scrolled to top): light, violet, mint, blue
$themes = @(@('01_today_light', 284, 1272), @('02_today_violet', 780, 1272), @('03_today_mint', 284, 1776), @('04_today_blue', 780, 1776))
foreach ($t in $themes) {
    Tap 996 250; Start-Sleep -Seconds 2
    Tap $t[1] $t[2]; Start-Sleep -Milliseconds 900
    & $adb -s emulator-5554 shell input keyevent 4; Start-Sleep -Seconds 1
    Shot $t[0]
}
# tab bar: Activity and Circle in the current (Signal Blue) theme
Tap 540 2280; Shot "05_activity_blue"
Tap 900 2280; Shot "06_circle_blue"
Tap 150 2280; Start-Sleep -Seconds 1
Get-ChildItem $out | Select-Object -ExpandProperty Name
