# Package rename progress (`com.example` → `com.noorpro.app`)

**Keep:** `applicationId = com.noorpro.app`  
**Keep for now:** Gradle `namespace = com.example` (generated `R` / hand-written
`BuildConfig` under `com.example`)

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

## Remaining (later passes)

1. Move `com.example.MainActivity` → `com.noorpro.app` (update Manifest + PendingIntents).
2. Move debug/release `BuildConfig` + `NoorAppCheckProviderFactory` and leftover root tests /
   `androidTest` under `com.example`.
3. Set `namespace = "com.noorpro.app"` and fix all `com.example.R` / `BuildConfig` imports.
4. Full `assembleDebug` smoke on a machine with SDK (not required on offline box).

**Progress (Kotlin files under `app/src`):** ~90% under `com.noorpro.app` (121 / 135);
~10% remaining (14 files still `com.example`, mostly tests + BuildConfig + MainActivity).

Do **not** commit `google-services.json` or secrets.
