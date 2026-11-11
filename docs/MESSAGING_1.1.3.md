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

## Sharing reels and posts into Messages (sample)
- `NoorShareSheet` (`ui/screens/NoorMessagingShare.kt`) is what a reel's share button would open. It has:
  - search;
  - a recent & frequent row covering people and circles, with check marks for multi-select;
  - an optional note (500 characters max, matching the rule);
  - "Send to N";
  - Add to story / Copy link / Share out (Android share) / Save.
- `MsgSharedCard` is the reel/post card bubble for 1:1 and group chats: a cover with a play icon, the creator, the caption, and the sender's note below it. Tapping it calls `onOpenShared`. Replies quote the reel ("Reel · Creator"), and reactions work as usual.
  - `MsgShared.from(UmmahMessage)` maps the live 1.0.82 `post_share` fields: sharedPostId, Kind, Thumb, CreatorUid/Name/Photo and Caption.
- Ordering and send plan: `ShareTargets.rank` puts the people you share with most first, then the most recent, and an exact match beats a prefix match. `ShareTargets.plan` splits the selection into people and circles and caps it at 15 recipients.

### Deploy status
- **People (1:1): already live.** It uses the existing `UmmahRepository.sendPostShare` and the 1.0.82 `post_share` rule on `ummah_chats/*/messages`.
- **Circles (groups): need an additive rules deploy.** The `ummah_groups/{gid}/messages` create rule only allows
  `text, image, video, audio, location, file, pdf`, and it requires a non-empty `mediaUrl` for anything that isn't text. A `post_share` sent to a group is refused today.
  - Proposed additive rule: for the new type only, allow `post_share` in group messages when the sender is a member. Validate the same `shared*` fields as the 1:1 rule (post exists; kind is reel or post; size limits; text ≤ 500; mediaUrl ""), with no `sharedTo`.
  - Also needed: a client `sendPostShareToGroup` that updates the group's lastMessage, and emulator tests. Keep it gated by `messaging_group_share_enabled` (default off); circles are hidden from the sheet until it's deployed.
