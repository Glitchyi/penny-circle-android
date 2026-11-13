$sp = 'C:\Users\User\AppData\Local\Temp\claude\ec29f026d20d029c\d5c06396-29cf-4cbf-8d2d-60f24af756c5\scratchpad'
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$out = "$sp\emu\rocket"; New-Item -ItemType Directory -Force $out | Out-Null
function Dump { & $adb -s emulator-5554 shell uiautomator dump /sdcard/ui.xml | Out-Null; return ((& $adb -s emulator-5554 exec-out cat /sdcard/ui.xml) -join '') }
function FindCenter($pattern) {
    $x = Dump
    foreach ($n in [regex]::Matches($x, '<node [^>]*>')) {
        $s = $n.Value
        $txt = [regex]::Match($s, ' text="([^"]*)"').Groups[1].Value
        $desc = [regex]::Match($s, 'content-desc="([^"]*)"').Groups[1].Value
        $b = [regex]::Match($s, 'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
        if (($txt -match $pattern -or $desc -match $pattern) -and $b.Success -and ($txt -ne '' -or $desc -ne '')) {
            return @([int](([int]$b.Groups[1].Value + [int]$b.Groups[3].Value) / 2), [int](([int]$b.Groups[2].Value + [int]$b.Groups[4].Value) / 2)) } }
    return $null
}
# 1. fresh open day: onboarding (Signal Blue) + one purchase
& $adb -s emulator-5554 shell input keyevent 4; Start-Sleep -Seconds 2
& "$sp\seed-purchases.ps1" | Out-Null
Start-Sleep -Seconds 2
# 2. press FINISH TODAY, then confirm in the sheet
$c = FindCenter '^FINISH TODAY$'; if (-not $c) { throw 'finish button not found' }
& $adb -s emulator-5554 shell input tap $c[0] $c[1]; Start-Sleep -Seconds 2
$c = FindCenter "^Yes, that.s everything$"; if (-not $c) { throw 'confirm button not found' }
# 3. tap confirm and capture 10 rapid frames while the rocket flies
& $adb -s emulator-5554 shell ('input tap {0} {1}; for i in 1 2 3 4 5 6 7 8 9 10; do screencap -p /sdcard/r$i.png; done' -f $c[0], $c[1])
1..10 | ForEach-Object { & $adb -s emulator-5554 pull "/sdcard/r$_.png" "$out\frame_$_.png" | Out-Null }
Start-Sleep -Seconds 3
& $adb -s emulator-5554 shell dumpsys vibrator_manager > "$out\vibrator.txt"
Get-ChildItem $out | ForEach-Object { "{0}: {1}" -f $_.Name, $_.Length }

