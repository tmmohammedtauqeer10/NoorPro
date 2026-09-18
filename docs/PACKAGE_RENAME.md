# Package rename progress (`com.example` → `com.noorpro.app`)

**Keep:** `applicationId = com.noorpro.app`  
**Done:** Gradle `namespace = com.noorpro.app` (generated `R` / hand-written
`BuildConfig` under `com.noorpro.app`)

## Done

| Step | Status |
|------|--------|
| Application class → `com.noorpro.app.NoorProApplication` | ✅ |
| Manifest `android:name` updated | ✅ |
| Al Noor / prayer packages under `com.noorpro.app.*` | ✅ |
| `ads`, `data`, `receiver`, `utils`, `ui` → `com.noorpro.app.*` | ✅ |
| Manifest receivers updated (`PrayerAlarmReceiver`, `BootCompletedReceiver`) | ✅ |
| Matching unit tests for `data` / `ui` moved | ✅ |
| Proguard Firestore keeps for `com.noorpro.app.data.**` | ✅ |
| `media3-session` declared + `AlNoorMediaSessionService` skeleton | ✅ |
| `MainActivity` → `com.noorpro.app` (Manifest + PendingIntents) | ✅ |
| Debug/release `BuildConfig` + `NoorAppCheckProviderFactory` | ✅ |
| Leftover root unit / `androidTest` under `com.example` | ✅ |
| Gradle `namespace = "com.noorpro.app"`; `R` / `BuildConfig` imports | ✅ |

## Remaining

1. Full `assembleDebug` smoke on a machine with SDK (not required on offline box).
2. Any *generated* `com.example` under `app/build/` is stale output only — delete
   `app/build` / clean rebuild; not hand-written source.

**Progress (Kotlin files under `app/src`):** **100%** under `com.noorpro.app`
(hand-written sources; no `com.example` package path left under `app/src`).

Do **not** commit `google-services.json` or secrets.
