# NoorPro 1.2.0 (117)

Built on 1.2.0 (116) (decb288). The owner asked for: profile details that save, faster Noor AI with the fiqh picker
off the chat screen, Al-Noor Audio work (Barakah Melodies), copy/paste on share cards, and no phone number on other
people's profiles.

## Profile
- **Bug:** the sign-up answers (name, username, gender, profile type, birth date, city, language) were saved to
  `users/{uid}/private/profile`, but Edit profile only showed the name, username and bio. Edits didn't update that
  doc or reserve the new username, so on the next launch the onboarding check put the old name and username back.
- **Fix:** Edit profile loads the sign-up answers and adds a private **About you** section for gender, profile type,
  birth date, city/state/country, show-city and language. Save does three things: it reserves a changed username
  (in a transaction), saves the private doc and the public card through `OnboardingRepository.save`, and updates
  this phone. Your own profile shows your city plus your profile type and language. The onboarding check now also
  syncs the name.
- No rules change: it uses the same writes as onboarding (owner-only private doc, usernames, ummah_profiles).

## Phone number
- The phone row moved off the profile/More page into **Settings › Account**, marked "only you can see this".
- The number lives only in Firebase Auth. It is not stored in `ummah_profiles` or in any Firestore doc, and other
  people's profiles never show it. A test checks that the public card holds only name, handle, profileType, city
  and country.

## Noor AI speed
- **Fast** is now the default answer mode. Balanced and Research are still in the picker.
- Answers **stream** token by token (`Accept: text/event-stream` on `/v1/chat`):
  - The Worker races two providers and keeps the first one that sends text:
    - English / Roman Urdu: Gemini Flash-Lite and Groq qwen.
    - Urdu / Arabic / Hindi: Gemma 4 and Gemini 3.8 Flash. Flash-Lite and Groq are not used here because they
      made Urdu fiqh errors.
  - The trailing META line is held back while streaming.
  - If a stream fails, the Worker falls back to the normal provider chain.
- The app shows text as it arrives and skips the second word-by-word reveal.
- If the Worker is the old version, the app falls back to the plain JSON reply.
- Fast trims context: 6 history turns of up to 1,200 characters each, 3 phone passages and 2 knowledge-base entries.
- The **madhhab picker** is off the chat screen. Hanafi stays the default. It now lives in the chat ⋮ menu
  ("Fiqh madhhab") and in **Settings › Noor AI**.

## Al-Noor Audio: Barakah Melodies
- Licensed by Safar Media (full catalogue, closed testing plus 6 months after launch).
- Adds a shelf on Home/Nasheeds, a Browse tile and a page with:
  - The credit ("Barakah Melodies · licensed by Safar Media") and terms.
  - A "Follow Barakah Melodies on Spotify" link (open.spotify.com/artist/048kFRuX8hS3DJSbayEKWt).
- The tracks list is **empty until Safar Media delivers the files**. Sources are restricted to our own https
  hosting, never Spotify or YouTube.
- Rules are in code:
  - Every Barakah track carries the credit.
  - `reelClipLimitMs` = 30 s for Reels.
  - `canShareOutside` = false.
- Labbayk is not added (not licensed).

### Licence agreement terms built in (Safar Media)
- **No ads** during or over a Barakah track: `LicensedAudioGate.barakahPlaying` hides every banner slot
  (InlineAdCard, SponsoredAdMedia) while one plays. NoorPro has no interstitial or audio ads.
- **No AI**: Barakah tracks are tagged `no-ai`; `BarakahMelodies.aiMayProcess` is false. They are not in the
  reel sound library (so they never reach reel AI subtitles), and no code path sends player audio to the Worker.
- **Streaming only**: no download button, no offline file, and the Al-Noor player has no disk cache.
  `OFFLINE_ALLOWED = false` until an encrypted, in-app-only store is built.
- **Reels**: 30 s limit, credit line `♪ Barakah Melodies · <title> (licensed by Safar Media)` and
  `canShareOutside(soundId) = false` are ready. Barakah sounds stay out of the reel library until the files
  arrive and the in-app-only reel path is switched on.
- **Credit**: "Barakah Melodies · <title>" on every row and in the player (attribution includes the licensor).
  Spotify artist link in the section.
- **Remote track list / removal within 7 days**: tracks come from Remote Config `barakah_catalog_json`
  (`{"version":1,"tracks":[{"key","title","url","durationMs","cover","lang"}],"removed":["key"]}`), cached on
  the device. Remote Config refreshes at least every 12 h. A catalogue not refreshed for 7 days is hidden, and a
  removed track never plays again, even from a saved queue. Deleting the file from storage removes it at once.
- **Quarterly report**: each new play counts once. `BarakahStats` writes
  `licensed_audio_stats/barakah-melodies/months/{yyyy-MM}` (`plays.<track>`, `total`) and one
  `listeners/{uid}` doc per listener per month. `BarakahStats.quarterCsv(year, q)` gives
  `month,listeners,track,plays` for the email to Safar Media.

#### Firestore rule needed for the report (NOT deployed — needs approval)
Additive. Until it is deployed the counter writes fail silently and nothing else changes.
```
match /licensed_audio_stats/{catalog}/months/{month} {
  allow read: if isAdmin();
  allow create, update: if request.auth != null
    && request.resource.data.keys().hasOnly(['plays', 'total'])
    && request.resource.data.total is int;
  match /listeners/{uid} {
    allow read: if isAdmin();
    allow create, update: if request.auth != null && request.auth.uid == uid
      && request.resource.data.keys().hasOnly(['at']);
  }
}
```

## Copy / paste
- Share cards: the card's words appear as selectable text with a **Copy** button. The caption sent with the card
  can be edited, and a **Paste** button fills it from the clipboard. Feed, story and other apps all use that caption.
- Adhkar/dua cards (Azkar, Ruhani, Darood) and the hadith reader: the text is selectable, with Copy and Share.
- Noor AI answers were already selectable, with Copy and Share.

## Entry points
- Settings › Account: **Edit profile** and Phone number.
- Settings › Noor AI: Fiqh madhhab and Answer speed.
- Chat ⋮ menu: Fiqh madhhab.
- Al-Noor Audio: Home banner, More, Explore and Settings, and inside it the Barakah shelf and Browse tile.
