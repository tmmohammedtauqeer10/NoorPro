# Al Noor Audio — production catalog drop-in

Replace the demo pack without code changes:

1. License-audit every track (see `docs/AL_NOOR_AUDIO.md`).
2. Write `app/src/main/assets/al_noor_audio/catalog.production.json`
   (same JSON schema as `catalog.json`).
3. Rebuild the app. Runtime prefers `catalog.production.json` over
   `catalog.json` when present (`BundledAlNoorAudioRepository`).

Attribution UI: Now Playing always shows `LicenseAttribution` from each track.
Keep `attributionText` / `sourceUrl` / `license` populated on every row.

Do not commit secrets or private permission letters with PII; store letters
outside git if needed and reference them by id in your audit spreadsheet.
