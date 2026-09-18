# Digital Asset Links

`assetlinks.json` — `package_name` is `com.noorpro.app`.

Replace `REPLACE_WITH_PLAY_APP_SIGNING_SHA256` with the Play Console App Signing
certificate SHA-256 (colon-separated) before relying on verified App Links.

App Link path filters `/reel`, `/u`, and `/g` live in `AndroidManifest.xml`
(not in this JSON). Deploy with `firebase deploy --only hosting`.
