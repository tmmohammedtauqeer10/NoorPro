# Al Noor Audio — Islamic Spotify (nasheed / naat)

Product and engineering spec for the **Al Noor Audio** feature inside Noor Pro.

## Positioning

- **What:** Free streaming catalog of **copyright-free** Islamic nasheed and naat.
- **What it is not:** Not Quran recitation (existing Stitch/NowPlaying flows),
  not YouTube remixes, not commercial music, not user-upload DJ sets.
- **Promise:** Only tracks with clear free/public-domain / CC licenses that
  allow redistribution; every track shows attribution.

## License & attribution policy

Allowed license tags (examples):

| Tag | Meaning |
|-----|---------|
| `PUBLIC_DOMAIN` | No known copyright |
| `CC0` | Public domain dedication |
| `CC_BY` | Attribution required |
| `CC_BY_SA` | Attribution + share-alike |
| `PERMISSION_LETTER` | Explicit written permission on file |

**Disallowed:** All rights reserved, “personal use only”, unknown provenance,
scraped commercial platforms, tracks with instrumental music if product policy
excludes them (product decision — default catalog is vocal nasheed/naat only).

Each `Track` must store:

- `license` enum/tag
- `attributionText` (artist / project / URL)
- `sourceUrl` (canonical download/page)
- `licenseUrl` (optional deed link)

UI: Now Playing and track detail always show attribution; playlist export
includes license lines.

## User-facing features

1. **Browse** — curated shelves (New, Spiritual, Kids-safe, Language).
2. **Search** — title, artist, tags; offline recent queries cached.
3. **Playlists** — system curated + user playlists (local Room first; cloud later).
4. **Now Playing** — full-screen player (art, seek, shuffle/repeat, lyrics if any).
5. **Mini-player** — persistent bar above bottom nav when queue non-empty.
6. **Queue** — reorder, remove, play-next, clear.

## Data model (stubbed in `com.noorpro.app.audio.models`)

```text
Artist     id, name, bio?, imageUrl?
Track      id, title, artistId, audioUrl, durationMs, coverUrl?,
           license, attributionText, sourceUrl, tags[], language?
Playlist   id, title, description?, coverUrl?, trackIds[], isSystem
QueueState tracks[], currentIndex, shuffle, repeatMode
PlaybackState isPlaying, positionMs, bufferedMs
```

Repository returns `Flow` / suspend APIs; initial implementation may ship a
bundled JSON catalog under `assets/al_noor_audio/` (to be added later) before
remote CDN.

## Screens (Compose targets under `com.noorpro.app.audio.ui`)

| Screen | Role |
|--------|------|
| `AlNoorAudioHomeScreen` | Browse shelves + search entry |
| `AlNoorSearchScreen` | Search results |
| `AlNoorPlaylistScreen` | Playlist track list |
| `AlNoorNowPlayingScreen` | Full player + attribution |
| `AlNoorMiniPlayer` | Composable overlay / scaffold slot |
| `AlNoorQueueSheet` | Queue bottom sheet |

Wire into existing navigation in `MainActivity` / `DeenScreen` when product
ready; stubs compile independently after package rename or with dual packages.

## Player architecture

Package: `com.noorpro.app.audio.player`

- `AlNoorPlayer` — Media3 `ExoPlayer` wrapper (preferred over Quran’s
  `MediaPlayer`).
- `AlNoorMediaSessionController` + `AlNoorMediaSessionService` — Media3
  `media3-session` declared (1.2.0); channel `al_noor_playback` ensured at app
  start. **Do not** share channels with adhan.
- Audio focus: pause Al Noor when adhan alarm fires; resume optional.

## Separation from existing Quran audio

| | Quran (today) | Al Noor Audio |
|--|---------------|---------------|
| Package | `com.noorpro.app.ui.*` / `DeenViewModel` | `com.noorpro.app.audio` |
| Content | Surah / ayah streams | Nasheed / naat catalog |
| Queue type | `AudioQueueItem(surahId, ayah, …)` | `Track` + `QueueState` |
| Player | `android.media.MediaPlayer` | Media3 ExoPlayer |

Do not overload `AudioQueueItem` for nasheed; keep models separate.

## Implementation order

1. Models + in-memory / JSON repository stubs ✅
2. Player wrapper + unit-testable queue logic ✅
3. Compose screens + mini-player slot ✅
4. MediaSessionService + notification channel `al_noor_playback` ✅
   (`DefaultMediaNotificationProvider`, play/pause/next, metadata)
5. Curated asset pack + license audit checklist — demo catalog ✅;
   production drop-in via `catalog.production.json` (see `docs/AL_NOOR_CATALOG_SWAP.md`)
6. Optional Firestore/CDN catalog sync

## Compliance checklist before shipping tracks

- [ ] License verified and stored on each track
- [ ] Attribution visible in UI
- [ ] No copyrighted commercial nasheed without written permission
- [ ] Offline cache respects license (no redistribution of disallowed works)

## 1.0.68 — Free Dua / Tarjuma / Tafsir audio (streaming only)

Catalog: `audio/catalog/FreeAudioCatalog.kt` + `assets/al_noor_free_audio.txt`. All URLs were checked from the build box (HTTP 200/206, MP3). No offline download is offered.

| Section | Source | License / terms |
|---|---|---|
| Duas (Hisn al-Muslim, 66 tracks) | archive.org/details/mp3-smart-muzakir — Hamad Al-Duraihim | Public Domain Mark 1.0 |
| Duas (Quranic, Arabic + Urdu, 15) | archive.org/details/quranic-duas — Hajveri Apps | Public Domain Mark 1.0 |
| Tarjuma (Urdu, English; 6 editions × 114 surahs) | cdn.islamic.network | `SOURCE_TERMS` — https://alquran.cloud/terms-and-conditions (free streaming; copyright stays with reciters; never paywalled) |
| Tafsir Urdu (165 parts) | Dr. Ubaid ur Rahman, ISLAMFORT/MIRC uploads on archive.org | CC BY 4.0 |
| Tafsir English (121 parts, Al-Baqarah) | archive.org/details/Eng_Commentary_AlBaqarah1-286 — Wahiduddin Khan | Public Domain Mark 1.0 |

Rejected: everyayah.com (no clear license), mp3quran (terms unclear), NC-licensed archive items, YouTube rips, items with contradictory rights notes. Hindi/Bengali tarjuma: no clearly licensed source found.

## 1.0.69 — Podcasts tab (public RSS)

`audio/podcast/PodcastRepository.kt` lists curated mainstream Sunni podcasts' public RSS feeds (the same feeds podcast directories use). Episodes (title, artwork, date, duration; newest 60) are parsed live at runtime, cached in `cacheDir/podcast_feeds` (6h TTL, used as offline fallback), and streamed from the publisher's enclosure URL via the Al-Noor player. No downloading or rehosting. Each podcast shows its name, publisher, "via RSS" and the publisher site as text. License type: `PUBLISHER_RSS`.
