# Noor Pro — Architecture

Structural foundation for modularizing the Android app. Most sources are under
`com.noorpro.app` (applicationId already `com.noorpro.app`); Gradle `namespace`
and a few entry files remain `com.example` until the final rename pass.

## Current layout (as extracted)

| Area | Location today |
|------|----------------|
| Application | `com.noorpro.app.NoorProApplication` |
| Activity | `com.example.MainActivity` (still) |
| Ads | `com.noorpro.app.ads` |
| Data / Room / APIs | `com.noorpro.app.data` |
| Prayer alarms | `com.noorpro.app.receiver` + `PrayerSettingsController` |
| UI (Compose) | `com.noorpro.app.ui.screens` / `viewmodel` / `components` / `theme` |
| Utils | `com.noorpro.app.utils` |
| **Al Noor Audio** | `com.noorpro.app.audio.*` (+ MediaSessionService skeleton) |

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

**Incremental — in progress.** See `docs/PACKAGE_RENAME.md`.

Done: Application, receivers, `ads` / `data` / `ui` / `utils`, Al Noor + prayer.
`applicationId` stays `com.noorpro.app`. Gradle `namespace` still `com.example`.

Remaining:

1. Move `MainActivity` → `com.noorpro.app` (Manifest + PendingIntents).
2. Move debug/release `BuildConfig` / `NoorAppCheckProviderFactory` + leftover tests.
3. Set `namespace = "com.noorpro.app"` in `app/build.gradle.kts`; fix `R` / `BuildConfig`.
4. Verify Firebase / App Links / FileProvider
   (`${applicationId}.fileprovider` already OK).
5. Full assembleDebug + instrumented smoke (SDK machine; not offline box).

## Dependency notes

- Prayer: `com.batoulapps.adhan:adhan`
- Reels: `androidx.media3:media3-exoplayer` (+ ui/hls)
- Quran audio today: `android.media.MediaPlayer` in `DeenViewModel`
- Al Noor Audio player: Media3 ExoPlayer + `media3-session` +
  `AlNoorMediaSessionService` skeleton (`com.noorpro.app.audio.session`,
  channel `al_noor_playback`).
- Calm Compose motion: prayer countdown crossfade, timeline highlight
  slide; Al Noor mini-player show/hide, now-playing fade, press scale.

## Non-goals (ongoing)

- No network Gradle builds on the box when avoidable
- No secrets / `google-services.json` committed
- Full package rename nearly done; MainActivity + namespace still deferred
