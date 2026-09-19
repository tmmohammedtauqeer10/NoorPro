# Combined 1.0.29 (1.0.28 qibla + Al Noor polish)

## Included
- Local 1.0.28 qibla accuracy base (Codex deen)
- Al Noor Audio + Pixabay demo tracks (6) + Media3 session shade controls
- namespace aligned to `com.noorpro.app` (Kotlin packages remain `com.example.*` for this merge)
- assetlinks.json package set to `com.noorpro.app` (SHA-256 placeholder — replace with Play App Signing cert)

## Still needed before production launch
- Paste Play Console **App signing** SHA-256 into `hosting/.well-known/assetlinks.json` and host it
- Real AdMob / `google-services.json` for `com.noorpro.app` if not already correct
- Rotate Play upload keystore passwords (were briefly exposed on GitHub; tip+history scrubbed)
- Device smoke: qibla, Al Noor play + shade controls, prayer alarms

## Version
- versionName 1.0.29 / versionCode 30

