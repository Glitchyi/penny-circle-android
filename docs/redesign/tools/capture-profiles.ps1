param([string]$Round = "d1")
# Simulates different phones on emulator-5554 with `wm size` / `wm density` / font_scale, captures Today at top and scrolled, then resets.
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$out = "C:\Users\User\AppData\Local\Temp\claude\ec29f026d20d029c\d5c06396-29cf-4cbf-8d2d-60f24af756c5\scratchpad\emu\$Round"
New-Item -ItemType Directory -Force $out | Out-Null
$profiles = @(
  @{ n = 'xiaomi15';   w = 1200; h = 2670; d = 520; fs = '1.0' },
  @{ n = 'nord_ce';    w = 1080; h = 2400; d = 420; fs = '1.0' },
  @{ n = 'small_720';  w = 720;  h = 1600; d = 320; fs = '1.0' },
  @{ n = 'nord_font15'; w = 1080; h = 2400; d = 420; fs = '1.5' }
)
foreach ($p in $profiles) {
  & $adb -s emulator-5554 shell wm size "$($p.w)x$($p.h)" | Out-Null
  & $adb -s emulator-5554 shell wm density $p.d | Out-Null
  & $adb -s emulator-5554 shell settings put system font_scale $p.fs | Out-Null
  & $adb -s emulator-5554 shell am force-stop com.spendingbuddy.app
  & $adb -s emulator-5554 shell am start -n com.spendingbuddy.app/.MainActivity | Out-Null
  Start-Sleep -Seconds 13
  & $adb -s emulator-5554 shell screencap -p /sdcard/cap.png; & $adb -s emulator-5554 pull /sdcard/cap.png "$out\$($p.n)_top.png" | Out-Null
  $x = [int]($p.w / 2); $y1 = [int]($p.h * 0.78); $y2 = [int]($p.h * 0.30)
  & $adb -s emulator-5554 shell input swipe $x $y1 $x $y2 500; Start-Sleep -Seconds 1
  & $adb -s emulator-5554 shell screencap -p /sdcard/cap.png; & $adb -s emulator-5554 pull /sdcard/cap.png "$out\$($p.n)_scrolled.png" | Out-Null
}
& $adb -s emulator-5554 shell wm size reset | Out-Null; & $adb -s emulator-5554 shell wm density reset | Out-Null; & $adb -s emulator-5554 shell settings put system font_scale 1.0 | Out-Null
Get-ChildItem $out | Select-Object -ExpandProperty Name

