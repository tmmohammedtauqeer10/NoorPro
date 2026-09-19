# Play Console production checklist (Noor Pro / `com.noorpro.app`)

Use this before promoting a release to production. Do **not** commit secrets
(`google-services.json`, `.env`, keystores, OAuth client secrets).

## 1. App Links — Digital Asset Links (SHA-256)

- Host: `https://noor-pro-d87e3.web.app/.well-known/assetlinks.json`
- Source template in repo: `hosting/.well-known/assetlinks.json`
- Package: **`com.noorpro.app`** (not legacy AI Studio ids)
- Fingerprint: **Play App Signing** certificate SHA-256 (Play Console →
  Setup → App integrity → App signing key certificate), not only the upload key
- Paths verified in Manifest (`autoVerify`): `/reel`, `/u`, `/g`
- After deploy: `adb shell pm get-app-links com.noorpro.app` should show
  `verified` for the host

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.noorpro.app",
      "sha256_cert_fingerprints": ["PASTE_PLAY_APP_SIGNING_SHA256"]
    }
  }
]
```

## 2. AdMob

- Manifest `APPLICATION_ID` is the production AdMob app id
- Release banner / interstitial unit ids come from BuildConfig / `.env`
  (`ADMOB_FEED_BANNER`, `ADMOB_BOTTOM_BANNER`) — never ship Google sample
  `ca-app-pub-394025609…` units in release
- UMP / consent form tested in EEA
- AdMob console: block sensitive categories as needed (app requests max rating G)
- Play Console: Advertising ID declaration matches AdMob usage

## 3. `google-services.json`

- Place under `app/google-services.json` locally / CI secret store
- **Gitignored** — never commit
- Package name inside must be `com.noorpro.app`
- Confirm Firebase project `noor-pro-d87e3` (or current production project)
- After package rename, re-download from Firebase console if needed

## 4. Firebase App Check

- Release: Play Integrity provider (`NoorAppCheckProviderFactory` release source set)
- Debug: debug provider only in debug builds
- Enforce App Check on Firestore / Storage / Callables in Firebase console
  when ready (start in monitor mode)
- Confirm R8 does **not** strip `NoorAppCheckProviderFactory` /
  `CrashReporter` (keep rules in `proguard-rules.pro`)

## 5. Data safety form

Declare accurately for:

| Data / permission | Why |
|-------------------|-----|
| Location (fine/coarse) | Prayer times, Qibla |
| Camera / photos | Reels / profile (if used) |
| Microphone | Reels / voice (if used) |
| App activity / crash logs | Crashlytics |
| Device ids / Advertising ID | AdMob |
| Account info | Firebase Auth |

- Link privacy policy URL (Hosting / Play listing)
- Exact alarms + full-screen intent: justify prayer adhan reminders
- No background location permission (keep it that way unless product requires it)

## 6. Maps / OAuth

### Maps

- `MAPS_API_KEY` in `local.properties` (Secrets Gradle plugin → Manifest placeholder)
- Restrict key in Google Cloud Console to Android apps:
  package `com.noorpro.app` + **release** SHA-1 (and debug SHA-1 for local)
- Do not commit the raw key

### OAuth / Google Sign-In

- Web + Android OAuth clients in Google Cloud / Firebase
- Android client: package `com.noorpro.app` + SHA-1 of signing certs used
- Store client ids via Firebase / BuildConfig — not hard-coded secrets in git
- Verify Credentials Manager / Google ID token audience matches

## 7. Signing & release hygiene

- Play App Signing enabled
- Upload keystore only in CI secrets / local secure store (gitignored `*.jks`)
- `isMinifyEnabled` / `isShrinkResources` on for release
- Smoke: cold start, prayer alarm after reboot, Al Noor playback notification
  (`al_noor_playback`), reel open via App Link, ads after consent

## 8. Notifications channels (sanity)

| Channel | Purpose |
|---------|---------|
| `prayer_adhan_channel_v2` | High — adhan |
| `prayer_notification_channel_v2` | Soft reminders |
| `prayer_ongoing_channel_v1` | Low — next prayer shade |
| `al_noor_playback` | Al Noor Media3 session (not prayer) |

Quran audio stays on the existing MediaPlayer path — do not merge with Al Noor.
