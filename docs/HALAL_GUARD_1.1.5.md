# Halal Guard 1.1.5

Server side: `a42645f` (text filter, SafeSearch / Video Intelligence scan, moderation triggers,
`setModerationDecision`) and `f1a4ffa` (additive moderator rules + emulator tests).
Client side: `8ef3bf1` (logic), `958f489` (UI), `38acdf0` + render tests (tests). **Nothing is deployed.**

## Client (Android)
| Area | Where | Works with today's backend? |
|---|---|---|
| Text pre-check: friendly note under the composer, comment box (feed sheet + community), bio/name edit, group name/description. Severe words disable Send/Post/Save; mild words only warn (the server masks or queues them). | `data/halal/HalalTextFilter.kt`, `HalalTextWarning`, `halalBlocks()` | Yes (on-device; same `words.json` as `functions/halal-words.json`, byte-identical; same 38 vectors pass) |
| Islamic report reasons first: Immodest / indecent, Haram content, Wrong Quran / hadith, Sectarian hate / takfir, Scam / fake charity, then the existing reasons. | `UgcTerms.REPORT_REASONS`, `UgcReportReasonList` | Yes (`ummah_reports` create rule is unchanged) |
| Halal community guidelines screen (8 sections, with references), linked from the terms gate and Settings. | `HalalGuidelinesScreen/Dialog` | Yes |
| Moderator panel: queue with media preview, scan reasons/scores, strikes; Approve / Approve & blur / Remove / Warn; account sheet Suspend (days) / Ban / Lift; Reports tab; Audit log tab. Hidden unless moderator (admin claim **or** `admins/{uid}` doc, same as `isModerator()` in the rules) **and** flag on. | `ModeratorPanel`, `ModeratorRepository`, `ModeratorAccess` | Needs rules + `setModerationDecision` |
| Safety settings: message requests (Everyone / Only people I follow → Requests folder in Messages), hide comments from non-followers on my posts, blur flagged media, optional private Sister/Brother for circles. Saved to `users/{uid}/settings/safety` (owner-only, already deployed) and `ummah_profiles/{uid}.commentsFrom`. | `SafetySettingsScreen`, `SafetyStore`, `SafetyRules` | Yes |
| Sisters-only / Brothers-only circles: audience chips in Create group, join gate in Discover groups. | `CircleAudiencePicker`, `SafetyRules.canJoin` | Client-enforced only (flag off) |
| Blocked / in-review post card: shown once per post when the creator's own `ummah_submissions` doc becomes `rejected` + `guardBlocked` or `in_review`. | `HalalNoticeHost`, `HalalNotices` | Yes once the functions are deployed (reads the owner's own submission, already allowed) |
| Moderator notices (warn / remove / suspend) from `moderation_notices/{uid}/items`. | `ModerationNoticeStore` | Needs rules (flag off) |
| Sensitive media blur: posts a moderator approved with `sensitive: true` show a "Sensitive content – tap to view" cover in the feed. | `SensitiveMedia` in `UmmahPostMedia` | Harmless without the backend (nothing is marked yet) |

### Remote Config flags (client)
| Key | Default | Turn on when |
|---|---|---|
| `halal_text_warnings_enabled` | true | — (client only) |
| `halal_safety_settings_enabled` | true | — (owner-only doc already allowed) |
| `halal_blur_flagged_enabled` | true | — |
| `halal_moderator_panel_enabled` | **false** | rules + `setModerationDecision` deployed and first admin created |
| `halal_moderation_notices_enabled` | **false** | rules deployed |
| `halal_gender_circles_enabled` | **false** | you accept client-only enforcement (see limits) |

### Known limits
- "Only people I follow" moves chats into Requests on the device; it does not stop the message being delivered or the push notification. Server-side message requests need an additive `ummah_chats` rule + FCM function change.
- "Hide comments from non-followers" filters on the client (comments are public-read), like Creator Studio's hidden comments.
- Circles are enforced only in the app; the self-declared gender is private and unverified. A rule-enforced version would need the gender in a server-readable private doc and an `ummah_groups` join rule.
- Reels player (full-screen) and stories are not blurred yet; only feed media.

### Tests
- `halal/HalalTextFilterTest` – all shared vectors (`functions/halal-vectors.json`) incl. masked output, EN / Roman Urdu / Hindi / Arabic & Urdu script / obfuscation (leet, spacing, dots, zero-width, repeats), word-list parity with the server.
- `halal/HalalGuardLogicTest` – moderator visibility, flag defaults, report reasons, message requests, comment audience, circles, notices, strikes.
- `v108/V108HalalGuardRenderTest` – Robolectric renders (light + dark) of guidelines, report sheet + pre-check, moderator queue / reports / audit, safety settings, blocked + queued notice and blur cover; also a click test that Approve calls the decision callback.

## Server (from a42645f / f1a4ffa)
- Env flags in `functions/.env`, all default off; trigger functions are exported only when their flag is on at deploy time:
  `HALAL_TEXT_FILTER`, `HALAL_GUARD_SCAN` (Vision SafeSearch), `HALAL_GUARD_VIDEO` (Video Intelligence explicit content, inline ≤ 20 MB, larger videos are queued), `HALAL_NEW_ACCOUNT_QUEUE`.
- `setModerationDecision` (callable, always exported) – approve / remove / warn / suspend / ban / unban / dismiss; 3 strikes → suspension; every decision writes `moderation_audit`.
- Rules (additive): `isModerator()`, `admins/{uid}` self-read, `moderation_queue` + `moderation_audit` moderator read, `user_strikes` moderator/self read, `moderation_notices` owner read + `seen` update, `ummah_reports` moderator read. Emulator tests: `tools/rules-tests/halal-guard-1.1.5.test.js`.

## Make the first admin
Preferred (narrow): Firebase console → Authentication → copy your UID → Firestore → collection `admins` → document ID = that UID, field `addedAt` (timestamp), `name` "T.M Mohammed Tauqeer". The console writes with admin rights, so the `allow write: if false` rule doesn't block it.
Alternative: custom claim with the Admin SDK (`admin.auth().setCustomUserClaims(uid, {admin: true})`), then sign out and in. Note the `admin` claim also unlocks the older `isAdmin()` rules, so the doc is the smaller grant.
