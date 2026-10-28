# NoorPro 1.1.6 — Islamic features (local build, not released)

Six features, each behind its own Firebase Remote Config flag (**all default `false`**). Flags are
registered in `IslamicFlags` and loaded through the existing `Reels2Flags` Remote Config map at startup.
With a flag off the More entry is hidden and any stale deep link quietly steps back (`IslamicFeatureOff`).
Version is unchanged (1.1.5 / 105).

| # | Feature | Flag | More entry | Commit | Works offline? | Needs a backend deploy? |
|---|---------|------|-----------|--------|----------------|-------------------------|
| 1 | Islamic events calendar | `islamic_calendar_enabled` | Islamic Calendar | fbaedb8 | Yes (Umm al-Qura + local WorkManager reminders) | No |
| 2 | Group Khatm | `group_khatm_enabled` | Group Khatm | d3653c5 | No – shows a calm notice until rules exist | **Yes**: `firestore-group-khatm.rules`, `hosting/k.html` |
| 3 | Hifz coach | `hifz_coach_enabled` | Hifz Coach | 6fda668 | Yes (on-device `SpeechRecognizer` ar-SA, offline preferred) | No (Noor AI tips optional) |
| 4 | Masjid pages | `masjid_pages_enabled` | Masjid Pages | de24e00 | Followed masjid cached on device | **Yes**: `firestore-masjid-pages.rules` |
| 5 | Family & Kids | `family_kids_enabled` | Family & Kids | e5be13e | Yes – fully local | No |
| 6 | Daily Deen goals | `daily_deen_goals_enabled` | Daily Deen Goals | 1a46b0f | Yes – fully local | No (Noor AI encouragement optional) |

## Turning a feature on
1. Firebase console → Remote Config → add the boolean parameter (name from the table), default `false`.
2. Publish a condition (e.g. internal testers / % rollout) with value `true`.
3. Features 2 and 4: deploy their rules **first** (below), otherwise the app shows its "not switched on yet" notice.

## Backend steps (NOT done in this build)
Both rule files are *additive*: paste the blocks inside `match /databases/{database}/documents { … }` of
`firestore.rules`, then run the emulator tests before `firebase deploy --only firestore:rules`.

```bash
cd tools/rules-tests   # or any folder with firebase-tools + @firebase/rules-unit-testing
RULES=../../firestore.rules EXTRA=../../firestore-group-khatm.rules npx firebase emulators:exec --only firestore "node group-khatm-1.1.6.test.js"
RULES=../../firestore.rules EXTRA=../../firestore-masjid-pages.rules npx firebase emulators:exec --only firestore "node masjid-pages-1.1.6.test.js"
```
Local results: group khatm 43/43, masjid pages 54/54.

* **Group Khatm** — `group_khatms/{CODE}` + `paras/{1..30}` (transactional claims). Invite links are
  `https://noor-pro-d87e3.web.app/k?c=CODE`; the app link is in the manifest, the web fallback is
  `hosting/k.html` (served as `/k` via `cleanUrls`) — needs `firebase deploy --only hosting`.
* **Masjid pages** — `masjids`, `masjids/{id}/announcements`, `masjid_claims`. Listings are created by
  moderators (`admin` claim or `admins/{uid}` doc); approved imams/committee may edit only jamaat, Jummah
  and bayan. Followed masjid lives under `users/{uid}/…` (base owner wildcard). There is no mosque
  finder in the app, so directions open the maps app via `PilgrimMapProvider`.

## Feature notes
* **Calendar** — Hijri month grid, key events with own neutral "recommended acts" text, local reminders.
* **Group Khatm** — create, share code/link, claim a para, mark done, live progress, completion du'as.
* **Hifz coach** — recite, word-level alignment, progressive hiding, SM-2 review plan from `HifzStore`.
  Recognition quality depends on the device's Arabic speech pack (no paid speech API).
* **Family & Kids** — Noor Kids: 10 short surahs (existing recitation audio + bundled Qur'an text),
  8 daily du'as, 5 prophet stories (own wording, Qur'an references, **no images of prophets or faces**),
  8 manners cards with hadith references, one star per activity per day. *Kids mode* hides Reels, Ummah,
  messaging, creator studio, AI hub and account switching (bottom bar, More, and a route redirect);
  leaving needs the parent PIN (4 digits, salted SHA-256, 5 tries → 30 s lock) or a maths gate if
  forgotten. Kids mode is only enforced while the flag is on, so turning the flag off never traps a
  device. Family prayer tracker: up to 10 members, kept 60 days, prefs `family_prayer_v1`, never uploaded.
* **Daily Deen goals** — 5 salah (read/written through the existing Room prayer tracker), Qur'an pages
  (target 1–30), morning/evening adhkar, sadaqah, du'a; goals can be switched off. Gentle streak: any deed
  counts, today never breaks it, one quiet day is forgiven; no shaming copy (unit-tested). Weekly summary
  with optional Noor AI encouragement (only weekly counts are sent; falls back to a calm notice).
  **Widget-ready**: every change writes a JSON snapshot to prefs `deen_goals_widget` / key `snapshot`
  (`day, done, total, streak, next, salah, pages, pagesTarget`); read with `DeenGoalsStore.widgetSnapshot()`.

## Tests & sheets
Unit + Robolectric light/dark render tests live in `app/src/test/java/com/noorpro/app/v111/`.
Review sheets: `/workspace/v111_{calendar,group_khatm,hifz,masjid,kids,deen_goals}_sheet.png`.

## Content & privacy
All Islamic text is original wording or the bundled Qur'an text; du'as/hadith carry their source.
No API keys or secrets in the app, no paid Google Cloud APIs.
