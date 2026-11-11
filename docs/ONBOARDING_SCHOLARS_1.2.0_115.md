# 1.2.0 (115) — Post-login onboarding + scholar applications

## What ships
- **Onboarding** (`ui/onboarding/OnboardingUi.kt`, `OnboardingFlow.kt`): shown full-screen right after sign-in and to signed-in
  users whose profile is incomplete. Steps: name (prefilled from Google) → unique username → gender (Male / Female / Prefer not to say)
  → profile type → date of birth → address (country + city required; state optional; street + postal code optional & private)
  → primary language (sets app UI language where available + Noor AI answer language). Existing users start at their first missing step.
- Remote Config kill switches (default ON): `onboarding_enabled`, `scholar_applications_enabled`.
- **Private** data: `users/{uid}/private/profile` (owner; admins may read). DOB never leaves it (only `ageBand`/`isChild` there too).
- **Public** (`ummah_profiles/{uid}`): name, handle, profileType (scholar shows as regular until verified), city/country only if
  the user allows and is 13+. `scholarBadge` / `verified` can only be set by admins.
- **Under 13**: Kids mode is switched on for the account (`KidsStore.setAccountChild`) even if `family_kids_enabled` is off;
  exiting still needs the parent PIN / parental gate. The profile is made private and location hidden.
- **Usernames**: `usernames/{name}` = {uid, at}, created in a transaction (old one released).
- **Scholar application** (`DeenScreen.SCHOLAR_APPLY`): writes `scholar_applications/{uid}` (status pending); certificate in
  Storage `scholar_certificates/{uid}/…` (owner + admins only, image/PDF < 10 MB). Applicant sees status; can re-apply after rejection.
- **Admin review** (`DeenScreen.SCHOLAR_REVIEW`, entry: shield icon in Ask a Scholar header for admins): pending list, certificate,
  Approve (writes `scholars/{uid}` {name,title,verified:true,approvedBy,approvedAt} + application approved + profile `scholarBadge`),
  Reject (reason required), Revoke for verified scholars (verified:false, status revoked, badge off).

## Who is an admin
`admin` custom claim **or** a doc at `admins/{uid}` (same as the Halal Guard moderator panel). Clients can't write `admins/`.
To make an account admin: Firebase Console → Firestore → `admins` → document id = the account's Auth UID → any field (e.g. `role: "admin"`).

## Rules (NOT deployed)
`firestore.rules` (block "BEGIN 1.2.0 (115)", also standalone `firestore-onboarding-scholars.rules`) and `storage.rules`
(`scholar_certificates`). Tests: `tools/rules-tests/onboarding-scholars-1.2.0.test.js` (Firestore + Storage emulators).
Until rules are deployed the app degrades: username reservation is skipped, answers are kept on the device and onboarding asks
again after 3 days; scholar applications show "not open on the server yet".

Deploy (only with the owner's explicit OK): `firebase deploy --only firestore:rules,storage`

Note: this commit also repairs `firestore.rules`, where the 1.2.0 feature merge block had been inserted inside the
`match /databases/{database}/documents` line (the file did not compile).

## Also in 115
- **Noor AI copy / paste**: answers (chat, Research mode) and your own messages are selectable by long-press;
  labelled **Copy** (plain text with references, "Copied" confirmation) and **Share** under every answer; the input
  supports long-press paste plus a one-tap **Paste** chip when the clipboard has text; voice-mode transcripts are
  selectable and the spoken answer has **Copy answer**. (`NoorAiCopy`, `NoorAiChatUi`, `NoorVoiceAssistant`).
- **Community refresh** (Halaqa rooms, Ask a Scholar, Dua requests): compact intro card with one primary action,
  count headers, clear empty states, calendar tiles for scheduled halaqas (reminders as before), gold "Verified scholar"
  badges on Halaqa hosts / members and on Ask a Scholar answers (from the public `scholars` collection), Ameen counts
  more prominent; report / moderation flows unchanged. No data-model or rules changes for this part.
- **Hadith easy to find + fixed**: "Hadith" tile in Home › Daily essentials, a Hadith / Daily quiz shortcut row on Home and
  in the Library, "Hadith" in More › Learn, Explore entry reworded. The book list now shows only real collections
  (Bukhari, Muslim, Abu Dawud, Tirmidhi, Nasa'i, Ibn Majah, Muwatta Malik, Nawawi 40, 40 Hadith Qudsi, Dehlawi 40); the
  removed six (Riyad as-Salihin, Adab al-Mufrad, Bulugh, Darimi, Ahmad, Hisn) had no source edition and opened the
  previous book's chapters or a blank page. Malik / Nawawi / Qudsi / Dehlawi are now bundled (Arabic + English, Urdu for
  Malik) so all ten work offline. Chapters are cleared between books, the translation falls back to English when an Urdu
  edition is missing or offline, hadith pair by number, and both screens have an empty state with "Try again".

## Islamic Quiz (115)
- Replaces the old 4-question mock. Route: `DeenScreen.QUIZ_DASHBOARD` / `ACTIVE_QUIZ` → `ui/quiz/QuizScreen`.
- **308 curated questions bundled offline** (`assets/quiz/bank_v1.json`, source in the build tools): Quran 34, Seerah 35,
  Hadith 33, Fiqh 36, Aqeedah 35 (Ash'ari/Maturidi), Islamic history 33, Awliya & Tasawwuf 34, Duas 33, Kids 35; every
  question has a one-line explanation and a source (Quran ayah, hadith collection + number, or classical book).
- **Fiqh follows the madhhab**: 19 questions carry Hanafi / Shafi'i / Maliki / Hanbali answers and explanations; default
  is the Noor AI madhhab (Hanafi if unset), switchable on the quiz home.
- Daily quiz (same 10 for everyone each day, one per section), section quizzes, difficulty (mixed/easy/medium/hard),
  optional 30 s timer, instant explanation + source, review-mistakes loop, day streak, XP (adds to the app's existing
  Deen points via `addQuizPoints`), best score per section, Kids mode shows only the Kids section.
- Languages: UI in English / Urdu / Hindi / Arabic (RTL for Urdu/Arabic). Questions are bundled in English and translated
  on demand through the Worker's `translate` task, cached per language; offline shows English with a note.
- "More questions with Noor AI": Worker `/v1/chat` with the madhhab param; JSON is validated (4 distinct options, answer
  in range, explanation and source present, Sunni/tasawwuf-safe filter), then each question is re-answered independently
  and kept only if both answers agree. Stored locally, marked "Noor AI".
