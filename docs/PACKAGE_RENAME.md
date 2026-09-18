# Package rename progress (`com.example` → `com.noorpro.app`)

**Keep:** `applicationId = com.noorpro.app`  
**Keep for now:** Gradle `namespace = com.example` (generated `R` / hand-written
`BuildConfig` under `com.example`)

## Done

| Step | Status |
|------|--------|
| Application class → `com.noorpro.app.NoorProApplication` | ✅ |
| Manifest `android:name` updated | ✅ |
| Al Noor / prayer packages already under `com.noorpro.app.*` | ✅ |

## Remaining (later passes)

1. Move `com.example.MainActivity` (+ receivers) → `com.noorpro.app`.
2. Move `com.example.data`, `ui`, `ads`, `utils`, `receiver` in batches;
   bulk-replace `package` / `import`.
3. Move debug/release `BuildConfig` + `NoorAppCheckProviderFactory` and tests.
4. Set `namespace = "com.noorpro.app"` and fix `R` / `BuildConfig` imports.
5. Full `assembleDebug` smoke on a machine with SDK (not required on offline box).

Do **not** commit `google-services.json` or secrets.
