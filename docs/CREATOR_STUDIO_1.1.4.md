# NoorPro 1.1.4: Creator Studio

The entry point is Profile → **Creator Studio** (it opens the existing `DeenScreen.CREATOR_STUDIO` route). In the composer, the toolbar has a new **Noor AI** button that opens the AI helper.

## Tabs
| Tab | What it shows | Data |
|---|---|---|
| Insights | Views (with a ▲/▼ change against the previous window), likes / comments / shares / saves, reach (unique reel viewers), engagement %, followers and follower growth, views per period, top 3 posts and top 3 reels, best time to post (by prayer time), watch time (**coming soon**) | Lifetime counters on your posts published in the 7, 28 or 90 day window, `followers/*.createdAt`, and `viewModel.prayers` |
| Content | Posts, Reels, Stories and Drafts with search and sort (Newest, Most viewed, Most engaged). You can edit the caption, pin to profile, archive or unarchive, and delete (with a confirmation). Drafts open in the composer. | `fetchMyPosts`, `StoryRepository.observeActive`, `ComposerDraftStore` |
| Comments | One inbox for comments on your 60 latest posts: All, Needs reply, Filtered (keyword filter or restricted accounts) and Hidden. You can reply inline, hide, delete, pin, restrict or block (block uses the existing `setBlocked`). | `fetchCommentsForPosts` and `CreatorModerationStore` (prefs) |
| AI helper | Caption ideas, hashtags, a reference check, and translation into AR, UR or EN. Results can be inserted into the composer. | `askNoorAi` in AskNoor mode, with the prompt framed on the client. Reference checks run on the offline Qur'an and hadith data. |

**Backend:** no new Noor AI mode is needed. `askNoorAi` already accepts `history`, and the client sends the last 10 turns. For translation, the client sets `answerLanguage` to the target language for that call only.

## Flags (Remote Config, default **false**)
- `creator_studio_edit_enabled`:
  - Turns on caption edit, pin to profile, archive, comment pin, server-side comment hide (otherwise hide is local only) and deleting other people's comments on your posts.
  - It needs the additive rules below.
- `creator_daily_stats_enabled`:
  - Turns on the per-day views chart.
  - It needs the `creatorDailySnapshot` function with `CREATOR_DAILY_STATS=true` in `functions/.env`.

## Additive rules (in `firestore.rules`, **not deployed**)
- `ummah_posts` (`ownerStudioUpdate`): the owner may change only `caption` (≤2200), `captionEditedAt` (== request.time), `pinnedToProfile`, `archived` and `pinnedCommentId`. The engagement-counter rule is unchanged.
- `ummah_comments`: the post owner may update `status` to `hidden` or `published` only, and may delete any comment on their post.
- `ummah_creator_stats/{uid}/days/{day}`: only the owner can read it; client writes are denied.
- Tests are in `tools/rules-tests/creator-studio-1.1.4.test.js`: 35/35 pass, and the 1.1.1 and 1.1.3 suites still pass.

## Function (not deployed)
- `creatorDailySnapshot` runs every day at 23:55 Asia/Kolkata.
- It sums per-creator totals and writes them to `ummah_creator_stats/{uid}/days/{yyyyMMdd}`.
- It does nothing unless `CREATOR_DAILY_STATS=true`.
- Cost: one read per post per day, plus one write per creator.
- The logic is pure and lives in `functions/creator.js`, tested by `creator.test.js` (`npm test`).

## Known limits
- Comments are public-read, so a "hidden" comment is filtered by the client (`observeComments`), not by the rules. That matches other apps, but it is not secret.
- Archived posts are still readable through the API; the feed and profile skip them on the client.
- Caption edits skip the publisher's moderation. Halal Guard (batch 3) adds a text filter on caption edits.
- Watch time and retention need a server summary of the private `views/{uid}.watchedMs` receipts, so that card shows "coming soon".
- The reference check can match plain times such as "5:30" as an ayah reference. It is advisory only.

## Deploy (when ready)
1. `firebase deploy --only firestore:rules`
2. `cd functions && npm test && firebase deploy --only functions:creatorDailySnapshot` (set `CREATOR_DAILY_STATS=true` first)
3. Remote Config: `creator_studio_edit_enabled=true`, then `creator_daily_stats_enabled=true` after the first snapshot has run.
