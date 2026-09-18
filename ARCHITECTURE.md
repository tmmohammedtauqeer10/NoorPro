# Noor Pro — Architecture

Structural foundation for modularizing the Android app while the source tree
is still under `com.example` (applicationId already `com.noorpro.app`).

## Current layout (as extracted)

| Area | Location today |
|------|----------------|
| Application / Activity | `app/src/main/java/com/example/` |
| Ads | `com.example.ads` |
| Data / Room / APIs | `com.example.data` |
| Prayer alarms | `com.example.receiver` + `PrayerSettingsController` |
| UI (Compose) | `com.example.ui.screens` / `viewmodel` / `components` / `theme` |
| Utils | `com.example.utils` |
| **Al Noor Audio (new stubs)** | `com.noorpro.app.audio.*` |

Gradle: `namespace = "com.example"`, `applicationId = "com.noorpro.app"`.

Existing audio UX is Quran-oriented (`StitchAudio*`, `NowPlayingScreen`,
`AudioQueueItem` in `DeenViewModel` via `MediaPlayer`). Reels use Media3
ExoPlayer in Ummah screens. Al Noor Audio is a **separate** catalog for
copyright-free nasheed/naat only — see `docs/AL_NOOR_AUDIO.md`.

## Target modules (logical)

These are **logical modules** first (packages + docs). Physical Gradle
modules can split later without changing public APIs much.

### 1. `core`
Shared app shell: `NoorProApplication`, navigation, theme, crash reporting,
preferences façade, location, permissions helpers.

### 2. `prayer`
Prayer times (Adhan lib + `OfflinePrayerCalculator`), logging/qaza Room DB,
alarm scheduling, notification channels, home-screen widgets.
→ `docs/PRAYER_NOTIFICATIONS_WIDGETS.md`

### 3. `audio` (Al Noor Audio)
Islamic Spotify-style browse/search/playlists/queue/now-playing for
**free, copyright-free nasheed & naat only**. License/attribution required.
→ package stubs under `com.noorpro.app.audio`
→ `docs/AL_NOOR_AUDIO.md`

### 4. `social` / reels
Ummah feed, reel upload/CDN, ExoPlayer playback, deep links.

### 5. `ads`
`AdManager`, AdMob unit wiring (debug test units vs release from env).

### 6. `data`
Room databases, Firestore/Auth repositories, Quran/Hadith/Dua models,
Drive PDF library, backup, secure account store.

## Package rename plan (`com.example` → `com.noorpro.app`)

**Do not rename in this foundation commit.** Plan when ready to ship:

1. Move `app/src/main/java/com/example/**` → `com/noorpro/app/**`
   (merge with existing `com.noorpro.app.audio` stubs).
2. Set `namespace = "com.noorpro.app"` in `app/build.gradle.kts`.
3. Update `AndroidManifest.xml` `android:name` for Application, Activity,
   receivers.
4. Bulk-replace `package com.example` / `import com.example` (keep
   `applicationId` as `com.noorpro.app`).
5. Fix `R` / `BuildConfig` imports after namespace change.
6. Update androidTest / test / debug source sets the same way.
7. Verify Firebase / App Links / FileProvider authority
   (`${applicationId}.fileprovider` already OK).
8. Full assembleDebug + instrumented smoke (on a machine with SDK; not on
   this box offline session).

## Dependency notes

- Prayer: `com.batoulapps.adhan:adhan`
- Reels: `androidx.media3:media3-exoplayer` (+ ui/hls)
- Quran audio today: `android.media.MediaPlayer` in `DeenViewModel`
- Al Noor Audio target player: Media3 + optional MediaSessionService
  (foreground), separate from Quran `MediaPlayer` until unified.

## Non-goals for this foundation

- No network Gradle builds on the box
- No secrets committed
- No full package rename yet
