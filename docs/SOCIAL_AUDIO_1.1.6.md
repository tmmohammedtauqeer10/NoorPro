# NoorPro 1.1.6: Islamic social and audio features (local build, not released)

Six features behind **ten** Firebase Remote Config flags. **All of them default to `false`.** The flags are registered in
`IslamicFlags` and loaded through the existing `Reels2Flags` Remote Config map. When a flag is off, its More entry
is hidden and any stale deep link quietly steps back (`IslamicFeatureOff`). The version is unchanged (1.1.5 / 105).
Nothing has been deployed: no rules, indexes, functions, hosting, or store upload.

| # | Feature | Flag(s) | Commit | Works offline? | Needs a backend deploy? |
|---|---------|---------|--------|----------------|-------------------------|
| 1 | Dua requests | `dua_requests_enabled` | 9fe2360 | Composer drafts only | **Yes**: `firestore-dua-requests.rules` |
| 2 | Halaqa rooms (audio) | `halaqa_rooms_enabled` | 51e5670 | No | **Yes**: `firestore-halaqa-rooms.rules` (a TURN relay is recommended) |
| 3 | Ask a Scholar | `ask_scholar_enabled` | 8ad66c8 | Saved answers are cached | **Yes**: `firestore-ask-scholar.rules` + `firestore-social-1.1.6.indexes.json` |
| 4 | Good-deed challenges | `challenges_enabled` | f96627f | Solo progress is local | **Yes** for group challenges: `firestore-challenges.rules` |
| 4 | Ayah/Hadith share cards | `share_cards_enabled` | f96627f | Yes, fully local | No |
| 5 | Offline Quran manager | `quran_offline_enabled` | 0273f48 | Yes, once surahs are saved | No |
| 5 | Audio routines | `audio_routines_enabled` | 0273f48 | Yes, with saved surahs | No |
| 5 | Adhan sound picker | `adhan_voices_enabled` | 0273f48 | Yes | No |
| 6 | Car mode + Android Auto | `car_mode_enabled` | 88a7fe5 | Quran: yes, from saved surahs | No |
| 6 | Bayan podcasts | `bayan_podcasts_enabled` | 88a7fe5 | Resume points and cached feeds | No (uses publisher RSS) |

The test fix that keeps v110 green is commit 0836d05.

## Features
1. **Dua requests**
   - Users post a dua request to the Ummah, optionally anonymously, and others reply "I made dua".
   - Anonymous requests store no uid. Ownership is kept in a separate `dua_owners/{id}` document.
   - The text passes through the existing Halal Guard filter.
   - The "someone made dua for you" alert is a local WorkManager notification. There is no Cloud Function.
2. **Halaqa rooms**
   - Scheduled or live audio circles that reuse the existing `CallSession`/`CallConfig` WebRTC stack in a mesh.
   - There is a host, speakers, and listeners with raise-hand.
   - A local reminder fires 10 minutes before the start.
   - Signalling goes through `halaqa_rooms/{id}/links/...`.
3. **Ask a Scholar**
   - Questions are private to their asker. Ownership is kept in `scholar_question_owners`.
   - Admins verify scholars (`scholars/{uid}`) by hand in the console.
   - The answered library is public, with search and save.
4. **Challenges and share cards**
   - Challenges: solo or group good-deed challenges with day-by-day progress. Joining uses a code (`challenges/{code}/progress/{member}`).
   - Share cards: 5 original card designs in square and story formats for an Ayah or Hadith, each with its source line.
5. **Offline Quran, audio routines, and adhan sound**
   - Offline Quran: save or remove surahs per reciter, with storage usage shown (`QuranAudioCache`, `OfflineQuran`).
   - Audio routines: morning, evening, and sleep, with a sleep timer (`AudioRoutines`).
   - Adhan sound picker (`AdhanVoices`): choose Alarm tone, Notification tone, or Vibrate only, with a 4s preview.
     Each option maps to an existing prayer notification channel. No adhan recording is bundled.
6. **Car mode and bayan podcasts**
   - **Car mode**
     - A full-screen car UI with big targets (at least 76dp) that keeps the screen on.
     - It offers quick surahs, routines, and bayans.
   - **Android Auto**
     - `NoorCarLibraryService` is a Media3 `MediaLibraryService` with its own ExoPlayer.
     - It is declared `android:enabled="false"`. `CarComponents.sync` enables it only while `car_mode_enabled` is on.
   - **Bayan podcasts**
     - Shows come only from the publisher's own public RSS feed. Audio is streamed and never rehosted.
     - Playback resumes where you stopped, with a 5s rewind.
     - Chapters come from Podcasting 2.0 JSON, `psc:chapter`, or timestamps in the notes.
     - Episode notes are shown as plain text.

## Content and licensing
- **Islah-e-Muashra:** they have said audio bayans are not permissible, so they are **never listed**.
  - `BayanLogic.EXCLUDED_TERMS` filters them by id, name, publisher, feed, and website. A test covers this.
  - The UI notice says such organisations are not listed.
- **Bayan feeds:** only `PUBLISHER_RSS` sources are allowed. Confirm with each publisher before a show is enabled.
- **Quran audio:** comes from the existing EveryAyah sources. Their terms for offline saving still need confirming.
- **Card text:** only the app's existing licensed Quran/Hadith text is used, with attribution.
- **Paid APIs and secrets:** none were added. QF secrets and `qfProxy` are untouched.

## Turning a feature on
1. Deploy its rules first (below). Without the rules, the app shows its calm "not switched on yet" notice.
2. In the Firebase console, open Remote Config and add the boolean flag with default `false`.
3. Roll it out to a condition such as internal testers.

## Backend steps (NOT done in this build)
The rules files are *additive*. Paste their blocks inside `match /databases/{database}/documents { … }` of
`firestore.rules`. Merge `firestore-social-1.1.6.indexes.json` into `firestore.indexes.json`.

```bash
cd tools/rules-tests
RULES=../../firestore.rules EXTRA=../../firestore-dua-requests.rules npx firebase emulators:exec --only firestore "node dua-requests-1.1.6.test.js"   # 35/35
RULES=../../firestore.rules EXTRA=../../firestore-halaqa-rooms.rules npx firebase emulators:exec --only firestore "node halaqa-rooms-1.1.6.test.js"   # 44/44
RULES=../../firestore.rules EXTRA=../../firestore-ask-scholar.rules  npx firebase emulators:exec --only firestore "node ask-scholar-1.1.6.test.js"    # 34/34
RULES=../../firestore.rules EXTRA=../../firestore-challenges.rules   npx firebase emulators:exec --only firestore "node challenges-1.1.6.test.js"     # 36/36
# then: firebase deploy --only firestore:rules,firestore:indexes
```

## Tests
- **JVM (Robolectric):** 699 tests, 0 failures, 0 errors, 1 skipped (pre-existing).
  - The new `com.noorpro.app.v112` package has 122 tests: logic tests plus render tests for every screen, including dark mode.
- **Rules emulator:** 149/149 (35 + 44 + 34 + 36).
- **Sheets:** `/workspace/v112_{dua_requests,halaqa_rooms,ask_scholar,challenges,share_cards,offline_audio,car_bayan}_sheet.png` (box only, not in the repo).

## Known limits and blockers
- **Halaqa:** there is no TURN relay, so some carrier NATs will fail. The mesh suits small rooms.
- **Adhan:** no adhan recording is bundled because licensed audio is needed. Android freezes a channel's sound once the channel is created, so each licensed voice added later will need its own channel.
- **Alerts:** dua and halaqa alerts are local only. There is no FCM push and no Cloud Function.
- **Caps:** daily caps on requests and questions are enforced on the client. The rules only check shape and ownership.
- **Scholars:** an admin must add them in the console. There is no in-app application flow.
- **Android Auto:** not yet tested on the Desktop Head Unit or a real car.
- **Licences:** confirm EveryAyah offline terms, and get each podcast publisher's consent, before release.
