# Adds example purchases on emulator-5554 through the real UI (keypad). Test device only.
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
function Dump { & $adb -s emulator-5554 shell uiautomator dump /sdcard/ui.xml | Out-Null; return ((& $adb -s emulator-5554 exec-out cat /sdcard/ui.xml) -join '') }
function TapMatch($pattern) {
    $x = Dump
    foreach ($n in [regex]::Matches($x, '<node [^>]*>')) {
        $s = $n.Value
        $txt = [regex]::Match($s, ' text="([^"]*)"').Groups[1].Value
        $desc = [regex]::Match($s, 'content-desc="([^"]*)"').Groups[1].Value
        $b = [regex]::Match($s, 'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
        if (($txt -match $pattern -or $desc -match $pattern) -and $b.Success -and ($txt -ne '' -or $desc -ne '')) {
            $cx = [int](([int]$b.Groups[1].Value + [int]$b.Groups[3].Value) / 2); $cy = [int](([int]$b.Groups[2].Value + [int]$b.Groups[4].Value) / 2)
            & $adb -s emulator-5554 shell input tap $cx $cy; Start-Sleep -Milliseconds 500; return $true } }
    Write-Host "NOT FOUND: $pattern"; return $false
}
function AddPurchase($digits, $reason) {
    TapMatch '^Add purchase$' | Out-Null; Start-Sleep -Seconds 1
    foreach ($d in $digits.ToCharArray()) { if ($d -eq '.') { TapMatch '^Decimal point$' | Out-Null } else { TapMatch ("^(Digit )?$d$") | Out-Null } }
    TapMatch '^Next$' | Out-Null; Start-Sleep -Milliseconds 800
    TapMatch ('^' + $reason + '$') | Out-Null
    TapMatch '^Save purchase$' | Out-Null; Start-Sleep -Seconds 2
}
AddPurchase '12.5' 'Social'
"seeded"

