# Launch next steps (NoorPro 1.0.29)

Internal testing AAB is already live. Before production:

## You do (Play / Firebase Console)
1. **Rotate upload keystore passwords** (they were briefly on GitHub; history scrubbed, still rotate).
2. Play Console → Setup → App integrity → copy **App signing key certificate SHA-256**.
3. Firebase → Project settings → download `google-services.json` for Android app `com.noorpro.app` (if package mismatch).
4. AdMob → create/confirm **production** app + banner units (not sample `ca-app-pub-394025609…`).
5. Complete Play **App content**: Data safety, Ads declaration, Content rating, Target audience, News app (No).

## I do after you paste values
- Put SHA-256 into `hosting/.well-known/assetlinks.json` and deploy to `noor-pro-d87e3.web.app`
- Wire real AdMob unit IDs into release BuildConfig
- Confirm `google-services.json` package = `com.noorpro.app`
- Re-check App Links with `adb` when device available
