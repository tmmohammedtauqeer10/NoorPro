# Al Noor Audio catalog (drop-in pack)

## What ships today

`catalog.json` is a **demo / placeholder** catalog (ExoPlayer test media URLs).
It is for UI + MediaSession wiring only. Do **not** treat it as production
nasheed/naat content.

## License-audited production pack (swap path)

1. Audit every track (PUBLIC_DOMAIN / CC0 / CC_BY / CC_BY_SA / PERMISSION_LETTER).
2. Host audio on your CDN (HTTPS only).
3. Create `catalog.production.json` beside this README with the **same schema**
   as `catalog.json` (`artists`, `tracks`, `playlists`, `shelves`, optional
   `demoNotice`).
4. Place the file at:

   ```text
   app/src/main/assets/al_noor_audio/catalog.production.json
   ```

5. Rebuild. `BundledAlNoorAudioRepository` prefers `catalog.production.json`
   when the asset exists; otherwise it loads `catalog.json`.

Optional: keep `demoNotice` empty / omit it for production so the home banner
hides. Attribution text on each track is still required and shown in Now Playing.

## Schema checklist (per track)

| Field | Required |
|-------|----------|
| `id`, `title`, `artistId`, `audioUrl` | yes |
| `license` enum | yes |
| `attributionText`, `sourceUrl` | yes |
| `licenseUrl`, `tags`, `language`, `coverUrl`, `durationMs` | recommended |

See `docs/AL_NOOR_AUDIO.md` for policy. No commercial / scraped / all-rights-reserved
tracks without a written permission letter on file.
