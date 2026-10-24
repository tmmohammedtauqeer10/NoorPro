# NoorPro 1.1.3 — Ummah Messaging sample (design review)

Status: **sample UI only**. These screens are real Compose code that runs on sample data
(`MessagingSamples`). They are not wired into navigation, so the current Messages screen
(`StitchMessagesScreen`) has not changed. Nothing has been deployed.

## Screens (`ui/screens/NoorMessaging*.kt`)
| Screen | Composable | Notes |
|---|---|---|
| Messages home | `NoorMessagesHome` | N mark + serif "Messages", salam greeting, unified search entry, Jumu'ah/Ramadan banner with a salah-time hint, mint pill tabs Chats / Groups / Discover with unread badges, pinned chats, online dots, gold read ticks, "New" FAB menu (New chat / New circle / Join with code) |
| 1:1 chat | `NoorDirectChat` | Eight-point-star cream wallpaper, emerald (mine) and white gold-hairline (theirs) bubbles, reply quotes, reactions, voice notes, dua card, salah pause hint, salam quick replies, composer with ayah/dua share |
| Group chat | `NoorGroupChat` | Pinned note, sender names with Admin/Owner chips, system messages, image and voice messages, group quick replies |
| Circle info | `NoorGroupInfo` | Hero with topic and visibility, Mute/Invite/Search/Media, invite link and code, admin tools (join requests, edit, who can message, moderation), members with roles, report/leave |
| Create circle | `NoorCreateGroup` | Photo, name, about, Islamic topic tiles, three privacy levels, member picker |
| Discover | `NoorDiscoverGroups` | Topic chips, featured circle, public cards with Join/Request/Open; private circles never listed |
| Find | `NoorFindPeople` | One search across people, circles and chats. Exact name/@username matches come first, then prefix, contains and fuzzy matches ranked by relationship. Filters, suggestions, and a QR / @username card |

Logic lives in `data/messaging/Messaging.kt`: `UnifiedSearch`, `GroupDiscovery`, `GroupPermissions`,
`IslamicTouches`, and `MessagingFlags`. It is covered by `MessagingLogicTest`, and `V105MessagingTest`
produces the renders.

## Works with today's backend (no deploy)
- Creating a group with extra fields (`photoUrl`, `topicId`, `tags`, `visibility`, `adminUids`), because the current create rule doesn't restrict keys.
- Group and 1:1 text, voice, and media using the existing `ummah_groups/*/messages` and `ummah_chats` paths and Storage.
- Reports, blocks, local mute/pin, salam quick replies, and Jumu'ah/Ramadan banners.
- Find/unified search and Discover filtering on the client, over groups that can already be read.

## Needs a rules/functions deploy (flag `messaging_groups_v2_enabled` / `messaging_discover_enabled`, default off)
1. Enforcing admin roles (`adminUids`): only the owner or admins can edit info or remove members, and only the owner can promote.
2. Member metadata updates limited to `lastMessage`, `lastSenderUid`, `updatedAt`.
3. Leaving a group (self-removal from `memberUids`), with the owner blocked from leaving.
4. Self-join only when `visibility == public_open`, plus `ummah_groups/{gid}/join_requests/{uid}` that admins approve.
5. Invite codes: `ummah_group_invites/{code}` (get-only, no list) and `join_tickets/{uid}` checked with `existsAfter`.
6. Admins deleting any message, and server-side mute via `mutedUids` ("admins only" announcements).
7. Optional: a Cloud Function for group-message FCM, plus a client handler for `kind=group`.

All rules changes have to be **additive**, with emulator tests in `tools/rules-tests/`, and deployed only after review.
