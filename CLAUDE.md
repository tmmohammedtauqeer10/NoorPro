# Noor Pro / Deen Android App

This is an Android Kotlin/Jetpack Compose project for an Islamic all-in-one app.

## Project Commands

Build debug APK:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
& 'C:\Users\tmmoh\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat' assembleDebug --console=plain --no-daemon
```

Install on the connected physical phone:

```powershell
..\android-sdk\platform-tools\adb.exe -s ZA222M6WZ7 install -r "app\build\outputs\apk\debug\app-debug.apk"
```

Launch and scan for startup crashes:

```powershell
..\android-sdk\platform-tools\adb.exe -s ZA222M6WZ7 logcat -c
..\android-sdk\platform-tools\adb.exe -s ZA222M6WZ7 shell monkey -p com.aistudio.deenislam.bvmzxl -c android.intent.category.LAUNCHER 1
Start-Sleep -Seconds 8
..\android-sdk\platform-tools\adb.exe -s ZA222M6WZ7 logcat -d -v brief | Select-String -Pattern 'FATAL EXCEPTION|Process: com.aistudio.deenislam.bvmzxl|AndroidRuntime' -CaseSensitive:$false | Select-Object -Last 80
```

## Important App Areas

- `app/src/main/java/com/example/ui/screens/HomeScreen.kt` - dashboard and quick actions.
- `app/src/main/java/com/example/ui/screens/NowPlayingScreen.kt` - Audio player section.
- `app/src/main/java/com/example/ui/viewmodel/DeenViewModel.kt` - navigation, app state, audio playback, data loading.
- `app/src/main/java/com/example/data/GithubApiService.kt` - GitHub JSON APIs for library, quiz, and audio catalog.
- `app/src/main/java/com/example/ui/theme/Color.kt` and `Theme.kt` - dark/light theme colors.

## Product Direction

The app should feel premium, fast, clean, and Islamic:

- Dark navy/gold premium style.
- Light theme should be white with soft pink shading.
- Keep UI simple and not crowded.
- Ummah section is planned as Islamic social media later.
- Audio section should feel like a Spotify-style Islamic audio hub.
- Library books come from the GitHub/Drive catalog and should open directly without showing source links.

## Current Package

Android package:

```text
com.aistudio.deenislam.bvmzxl
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```
