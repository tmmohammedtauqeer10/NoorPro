# Noor Pro Android App

Noor Pro is a Jetpack Compose Android application.

View the original app in AI Studio:
https://ai.studio/apps/e62d703a-cdb3-4550-b111-25c060f506e4

## Prerequisites

- Android Studio
- JDK 17
- Android SDK 36 and SDK Build Tools 36.0.0

Android Gradle Plugin 9.1.1 requires Gradle 9.3.1 and JDK 17. Use the
committed Gradle wrapper rather than a system Gradle installation.

## Setup

1. Open this project directory in Android Studio.
2. Allow Gradle sync to complete.
3. Create `.env` in the project root using `.env.example` as a template.
4. For authentication, add your Firebase Android app's `google-services.json`
   to `app/google-services.json` and enable Email/Password or Google providers
   in Firebase Authentication.
5. Configure `GOOGLE_WEB_CLIENT_ID` if testing Google sign-in.
6. Run the app on an emulator or physical device.

Without Firebase configuration, authenticated sign-in is disabled and guest
mode remains available.

## Ummah Feed

The Ummah feed reads approved content from the Firestore `ummah_posts`
collection. Create Firestore in the Firebase console and deploy
`firestore.rules` before publishing posts.

Each post document supports:

```text
approved: true
type: "image" | "video" | "reel"
mediaUrl: "https://..."
thumbnailUrl: "https://..." (optional)
creatorName: "Noor Pro"
creatorHandle: "@noorpro"
caption: "Post caption"
sourceReference: "Quran 2:286" (recommended)
publishedAt: Firestore timestamp
```

Only approved posts are readable in the app. User uploads and comments are
intentionally disabled until moderation tooling is available.

Debug builds use Android's standard generated debug keystore.

## Command Line

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

Release signing requires these environment variables:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`

## Public Website And OAuth Links

The Firebase Hosting site in `hosting/` provides the public pages required for
the Google OAuth consent screen:

- Home page: `https://noor-pro-d87e3.web.app`
- Privacy policy: `https://noor-pro-d87e3.web.app/privacy`
- Terms of service: `https://noor-pro-d87e3.web.app/terms`
- Support: `https://noor-pro-d87e3.web.app/support`
- Account deletion: `https://noor-pro-d87e3.web.app/delete-account`

Install and authenticate the Firebase CLI, then publish the site and Firestore
rules from the project root:

```powershell
npm install -g firebase-tools
firebase login
firebase deploy --only hosting,firestore:rules
```

Add `noor-pro-d87e3.web.app` as an authorized domain in the Google OAuth
consent screen configuration.

## Firebase App Check

Noor Pro uses Firebase App Check for protected Firebase requests:

- Debug APKs use the App Check debug provider.
- Release APKs use the Play Integrity provider.

When App Check enforcement is enabled, each development device must register
its debug token once:

1. Install and open the debug APK.
2. In Android Studio Logcat, search for `DebugAppCheckProvider`.
3. Copy the debug token shown in the log.
4. Open Firebase Console > App Check > Apps > Noor Pro > Manage debug tokens.
5. Add the copied token, then reopen the app and test sign-in.

Never distribute a release build that uses the debug provider.


## Al Noor Audio (Islamic Spotify)

Browse **copyright-free** nasheed & naat separately from Quran audio:

1. Open the app → **Explore** (or Profile → Explore All Tools).
2. Tap **Al Noor Audio**.
3. Browse shelves / playlists, search, and open **Now Playing**.
4. A mini-player appears above the bottom bar while a track is queued.

Catalog: `app/src/main/assets/al_noor_audio/catalog.json` (demo / public-domain placeholders only — replace with license-audited tracks before shipping). See `docs/AL_NOOR_AUDIO.md`.

Quran recitation remains under **Audio** / existing Now Playing flows and uses a separate `MediaPlayer`.

## Prayer ongoing notification & home widgets

- **Settings → Show next prayer in shade** enables a quiet ongoing notification
  (`prayer_ongoing_channel_v1`, LOW) such as `Next: Maghrib · 01:24`.
  This never uses the HIGH adhan channel.
- **Home-screen widgets** (long-press home → widgets → Noor Pro):
  - **Next prayer** (2×2) — name, time, countdown
  - **Today** (4×2) — Fajr–Isha row
- Widgets refresh on add, every ~30 minutes, after boot/timezone change, and with the ongoing worker. Spec: `docs/PRAYER_NOTIFICATIONS_WIDGETS.md`.

## Polish (Sep 2026)

- Calm in-app motion: prayer countdown crossfade + timeline highlight slide;
  Al Noor mini-player enter/exit, now-playing artwork/title fade, playlist
  press scale.
- Package rename step 1: `NoorProApplication` → `com.noorpro.app` (see
  `docs/PACKAGE_RENAME.md`). `applicationId` unchanged.
- Al Noor MediaSession / notification controls: **stub** + channel
  `al_noor_playback` (real Media3 session when `media3-session` is added).

