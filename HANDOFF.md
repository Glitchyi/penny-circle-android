# Handoff: build and test Penny Circle from a Windows laptop

This is for a Claude Code session on a Windows laptop that starts with only Claude Code and `adb` installed, with an Android phone attached over USB. Read `AGENTS.md` first for product rules, then `TODO.md` and `BUILD-EVIDENCE.md` for status. Those three files are the source of truth; this file only covers getting from a bare Windows machine to recorded test results.

## Where the project stands (10 November 2026)

- Penny Circle (formerly Spending Buddy) is a native Android app in Kotlin and Jetpack Compose. Package and applicationId are `com.spendingbuddy.app`, version 0.2.0 (versionCode 2), minSdk 33, targetSdk 36.
- Friends form a circle of up to 12. The admin approves each join request made with a `pc1.` code or its QR. Phones sync directly over iroh QUIC, one connection per member. There is no backend and none may be added.
- `master` at commit `639e19f` builds. On macOS with JDK 21: `:core:test` 75 passed (1 skipped, the opt-in online iroh test), `:app:testDebugUnitTest` 30 passed, `:app:lintRelease` 0 errors and 34 warnings.
- **Nothing has ever been installed or run on a phone.** No circle has been formed between real devices. Every device result you produce is new information.
- The release keystore is not in the repo and will not be on this laptop. You can only produce a debug-signed APK.

## Rules that are easy to break

- Never uninstall the app, clear its data, or run `adb install` with a downgrade or reinstall flag that wipes data unless the user says yes for that phone. Uninstalling destroys local spending data that has not synced.
- A debug-signed APK cannot install over a release-signed one (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). If you hit that, stop and ask; do not uninstall on your own.
- Do not turn off Play Protect, antivirus or any OEM security control to make an install succeed. Report the block instead.
- Screenshots of Google Pay and the app's spending data are personal. Do not pull them off the phone, commit them, or paste them into tickets. If you capture a screen for evidence, use the test data you created.
- Accepting licences (Android SDK, Xiaomi prompts) is the user's decision. Ask them to do it.
- Commits carry no AI attribution of any kind. Ask the user which git name and email to use before the first commit; do not guess.
- State facts in the docs. Write "not tested" where something was not tested.

## 1. Set up the toolchain

Use PowerShell. Check what exists before installing anything:

```powershell
git --version; java -version; adb version; winget --version
```

Install what is missing. Ask the user before installing, since these are several GB.

```powershell
winget install --id Git.Git -e
winget install --id EclipseAdoptium.Temurin.21.JDK -e
```

Open a new PowerShell window afterwards so `PATH` updates. The build needs JDK 21 exactly as the compile target; `java -version` must report 21.

Android SDK without Android Studio:

1. Download the "Command line tools only" zip for Windows from <https://developer.android.com/studio#command-line-tools-only>.
2. Unzip so that this file exists: `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat` (the zip's inner `cmdline-tools` folder must be renamed to `latest`).
3. Ask the user to accept the licences themselves:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
```

4. Then install the packages:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" "platforms;android-36" "build-tools;36.0.0" "platform-tools"
```

## 2. Get the source and build

```powershell
git clone https://github.com/Glitchyi/spending-buddy-android.git
cd spending-buddy-android
```

If the clone asks for credentials, the repo is private: ask the user to sign in with `gh auth login` or supply access. Do not ask them to paste a token into chat.

Create `local.properties` in the repo root (it is gitignored). Use forward slashes:

```
sdk.dir=C:/Users/<name>/AppData/Local/Android/Sdk
```

Build and test:

```powershell
.\gradlew.bat :core:test :app:testDebugUnitTest :app:lintRelease :app:assembleDebug
```

Expected: the counts listed above, and `app\build\outputs\apk\debug\app-debug.apk` of roughly 120 MB. If the Gradle wrapper download times out, download `gradle-8.13-bin.zip` manually, verify its SHA-256 against the `.sha256` file published beside it on services.gradle.org, and place it in the wrapper's `dists` folder. If counts differ from the macOS run, record the difference; do not edit tests to make them pass.

## 3. Connect the phone and install

```powershell
adb devices -l
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getconf PAGE_SIZE
adb shell pm list packages com.spendingbuddy.app
```

Record the model, Android version and page size. If the last command prints the package, an older build is installed: stop and ask the user before doing anything, because of the signature and data rules above.

On Xiaomi phones, USB installs also need "Install via USB" and "USB debugging (Security settings)" switched on in Developer options, which may require a Mi account and a SIM. The user has to do that on the phone.

```powershell
adb install app\build\outputs\apk\debug\app-debug.apk
adb shell am start -W -n com.spendingbuddy.app/.MainActivity
```

## 4. Functional test pass

Work through these on the phone and note pass, fail or not tested for each. Use made-up purchases, not the user's real ones.

**One phone is enough for:**

1. Onboarding: name, limits, reminder permission prompt.
2. Add a purchase with the keypad (default) and with the rotary dial. Check decimal, undo, clear, and that input survives rotating the phone and pressing back.
3. Category and reason are required. Impulse shows in red and as the word "Impulse".
4. Save shows an Undo snackbar. Tap a purchase, delete it, undo it, and confirm the day's check-in comes back.
5. Finish the day and record a no-spend day. The stamp animates once; with animations disabled in Developer options it appears without animation.
6. Presets: create, edit, set default, delete with Undo, and "Save as preset" from the purchase sheet.
7. Activity: swipe between weeks, tap a chart bar, check totals against what you entered.
8. Screenshot import: share one image and several images from the gallery to Penny Circle (`ACTION_SEND` and `ACTION_SEND_MULTIPLE`), review the candidates, save. Nothing may be saved without review.
9. Settings: export a backup, restore it, change limits.
10. Create a circle. Check the join code, its QR, share, copy and "New code".
11. Accessibility: TalkBack labels on each screen, 48dp touch targets, font size at the largest setting, and the light theme's contrast outdoors if possible.

**Needs two or more phones (or one phone plus an emulator, see below):**

12. Join by pasting the code and by scanning the QR. The joiner sees "waiting"; nothing syncs before approval.
13. Admin approves; the joiner then sees the circle. Decline works. A request expires after 24 hours (note as not tested if you cannot wait).
14. Admin offline: close the app on the admin phone and confirm the request waits rather than failing.
15. Visibility: "Totals & status" shows no purchase details to others; "Full details" does.
16. Nudge: once per member per day; it disappears after the target checks in.
17. Suggest new limits: only the target can accept.
18. Leave the circle, including an admin leaving and the role passing on.

A second peer can be an Android emulator on the laptop if no second phone exists. That needs `sdkmanager "emulator" "system-images;android-36;google_apis;x86_64"` and hardware virtualisation enabled. Whether the iroh native library runs on that image has not been tried, so record the outcome either way.

## 5. Performance and battery measurements

Run these on whatever phone is attached, and again on the Xiaomi 15 when it is available. Record every number with the phone model next to it.

**Cold start.** Repeat five times and record the median `TotalTime`:

```powershell
adb shell am force-stop com.spendingbuddy.app
adb shell am start -W -n com.spendingbuddy.app/.MainActivity
```

**Frame timing.** Reset, use the app for a minute (scroll Activity, swipe weeks, spin the dial, open sheets), then dump:

```powershell
adb shell dumpsys gfxinfo com.spendingbuddy.app reset
adb shell dumpsys gfxinfo com.spendingbuddy.app
```

Record total frames, janky frames percentage, and the 50th, 90th and 99th percentile frame times. On a 120 Hz panel the frame budget is about 8.3 ms.

**Memory.** After the same minute of use:

```powershell
adb shell dumpsys meminfo com.spendingbuddy.app
```

Record TOTAL PSS, and again after an OCR import of several images, which is the heaviest path.

**Battery and background work.** Unplug is not possible over USB, so reset the counters, use wireless adb or leave the phone idle on USB, and compare:

```powershell
adb shell dumpsys batterystats --reset
adb shell dumpsys batterystats com.spendingbuddy.app
adb shell am get-standby-bucket com.spendingbuddy.app
adb shell dumpsys jobscheduler | findstr spendingbuddy
```

**Doze.** Check that sync and reminders recover, and that nothing crashes:

```powershell
adb shell dumpsys deviceidle force-idle
adb shell dumpsys deviceidle unforce
```

**Stability.** Watch for crashes during all of the above:

```powershell
adb logcat -c
adb logcat *:E | findstr /i "spendingbuddy AndroidRuntime"
```

**Size.** Record the APK size and the installed size from `adb shell dumpsys package com.spendingbuddy.app | findstr /i "codePath versionName"` and Settings > Apps. R8 shrinking is off in this build, which is why the APK is large; note the size but do not enable R8 without checking that iroh's JNI and kotlinx.serialization still work.

## 6. Xiaomi 15 instructions (for whoever has the phone)

Nobody who wrote this document has had a Xiaomi 15, so treat every setting name below as a starting point and write down what the phone actually shows. HyperOS menu names change between versions.

Xiaomi's battery management is the main risk for this app, because circle sync, join requests and reminders all depend on background work. Test in two configurations and record both.

**Configuration A, phone defaults.** Install, set nothing, and run sections 4 and 5. This shows what a normal user gets.

**Configuration B, restrictions relaxed.** Change these, then run the background parts again:

- Settings > Apps > Manage apps > Penny Circle > Autostart: on.
- Same screen > Battery saver: "No restrictions".
- Notifications: allowed, including lock screen.
- Recents: long-press the app card and lock it.
- In the app: Settings > Stay reachable on, and open "Battery settings" from there.

**What to measure on the Xiaomi 15, beyond section 5:**

1. Reminder timing. Note when the 10 PM check-in reminder and the 10 AM grace reminder actually arrive on two consecutive days, in each configuration.
2. Background sync. With the app swiped away for an hour, have another member add a purchase. Record how long it takes to appear after the Xiaomi is next unlocked, and whether it appears without opening the app.
3. Join requests as admin. With the app closed, send a join request from another phone. Record whether a notification arrives and how late.
4. Reboot. Restart the phone, do not open the app, and check whether reminders and sync resume.
5. Foreground service. With "Stay reachable" on, check after several hours whether the notification is still there or the system removed it.
6. Screen and input. Check edge-to-edge layout around the camera cut-out and gesture bar, the 120 Hz frame numbers from section 5, and whether the rotary dial and keypad haptics feel right.
7. Share sheet. Share one and several Google Pay screenshots into the app and check that OCR finds the amounts. Do not send the screenshots anywhere.
8. Heat and drain. After 15 minutes of active use and one multi-image import, note battery percentage used and whether the phone gets warm.

If someone can attach the Xiaomi 15 to a computer with adb, run the section 5 commands as written. If not, the manual observations above are still worth recording.

## 7. Record the results

- Add a dated section to `BUILD-EVIDENCE.md` with: laptop OS, JDK and Gradle versions, test and lint counts, APK size and SHA-256 (`Get-FileHash app\build\outputs\apk\debug\app-debug.apk`), then one table per phone with model, Android version and each measurement. Mark anything skipped as "not tested".
- Tick or amend the matching items in `TODO.md`. Add new items for every failure, with the steps that reproduce it.
- Keep fixes small and rebuild with the Gradle command in section 2 before recording a result. Device behaviour that depends on Xiaomi settings belongs in the docs as a limit, not as a code workaround, unless the user asks for one.
- Commit only when the user asks.

## Known gaps you may run into

- A saved purchase can be deleted but not edited.
- Discarding an import draft has no Undo.
- Whether a day completed under grace counts toward the streak is undecided; today it does not.
- iroh-gossip is not available in the Kotlin bindings (`computer.iroh` 1.2.0), so sync is per-member connections. A phone is reachable only while the app is open, "Stay reachable" is on, or for 15 minutes after a join code is shown.
- OCR uses bundled ML Kit Latin text recognition, on the device. A larger on-device model was considered and rejected; see the OCR decision in `TODO.md`.
- `AGENTS.md` names the users' phones as a Xiaomi 17 and a OnePlus Nord 6e. If the Xiaomi 15 is now one of the real phones, ask the user and update that line.
