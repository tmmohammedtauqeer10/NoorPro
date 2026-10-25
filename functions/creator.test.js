const assert = require("assert");
const {dayId, aggregateCreatorTotals, dailyStatsEnabled} = require("./creator");

// dayId: IST rolls over at 18:30 UTC
assert.strictEqual(dayId(new Date("2026-10-24T18:29:00Z")), "20261024");
assert.strictEqual(dayId(new Date("2026-10-24T18:31:00Z")), "20261025");
assert.strictEqual(dayId(new Date("2026-01-01T00:00:00Z"), 0), "20260101");

const totals = aggregateCreatorTotals([
  {creatorUid: "a", type: "image", viewCount: 10, likeCount: 2, commentCount: 1, shareCount: 1, saveCount: 0, status: "approved", publicationStatus: "published"},
  {creatorUid: "a", type: "reel", viewCount: 90, likeCount: 8, saveCount: 3},
  {creatorUid: "a", type: "image", viewCount: 999, publicationStatus: "pending"},   // not published
  {creatorUid: "b", type: "image", viewCount: -5, likeCount: "7"},                  // junk counters → 0
  {creatorUid: "", viewCount: 50}, null,
]);
assert.deepStrictEqual(totals.get("a"), {views: 100, likes: 10, comments: 1, shares: 1, saves: 3, posts: 1, reels: 1});
assert.deepStrictEqual(totals.get("b"), {views: 0, likes: 0, comments: 0, shares: 0, saves: 0, posts: 1, reels: 0});
assert.strictEqual(totals.size, 2);

assert.strictEqual(dailyStatsEnabled({}), false);
assert.strictEqual(dailyStatsEnabled({CREATOR_DAILY_STATS: "TRUE"}), true);
assert.strictEqual(dailyStatsEnabled({CREATOR_DAILY_STATS: "1"}), false);

console.log("creator.test.js: all passed");
