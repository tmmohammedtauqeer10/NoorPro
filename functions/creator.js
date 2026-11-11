// 1.1.4 Creator Studio helpers (pure, unit-tested in creator.test.js).
"use strict";

const n = (v) => (typeof v === "number" && isFinite(v) && v > 0 ? Math.floor(v) : 0);

/** yyyyMMdd for a Date in a fixed UTC offset (minutes), default IST (+330) — the app's main audience. */
function dayId(date, offsetMinutes = 330) {
  const d = new Date(date.getTime() + offsetMinutes * 60000);
  const p = (x) => String(x).padStart(2, "0");
  return `${d.getUTCFullYear()}${p(d.getUTCMonth() + 1)}${p(d.getUTCDate())}`;
}

/**
 * Sums lifetime counters per creator from ummah_posts docs ({creatorUid, viewCount, likeCount, ...}).
 * Archived posts still count (their views happened); pending/rejected posts do not.
 */
function aggregateCreatorTotals(posts) {
  const out = new Map();
  for (const p of posts || []) {
    if (!p || typeof p.creatorUid !== "string" || !p.creatorUid) continue;
    if (p.publicationStatus && p.publicationStatus !== "published") continue;
    if (p.status && p.status !== "approved") continue;
    const t = out.get(p.creatorUid) || {views: 0, likes: 0, comments: 0, shares: 0, saves: 0, posts: 0, reels: 0};
    t.views += n(p.viewCount); t.likes += n(p.likeCount); t.comments += n(p.commentCount);
    t.shares += n(p.shareCount); t.saves += n(p.saveCount);
    if (p.type === "reel" || p.type === "video") t.reels += 1; else t.posts += 1;
    out.set(p.creatorUid, t);
  }
  return out;
}

/** The Remote-Config-style env switch; anything but "true" is off. */
function dailyStatsEnabled(env) {
  return String((env || {}).CREATOR_DAILY_STATS || "").toLowerCase() === "true";
}

/** Fields the post owner may change from Creator Studio (mirrors firestore.rules). */
const OWNER_EDITABLE = ["caption", "captionEditedAt", "pinnedToProfile", "archived", "pinnedCommentId"];

module.exports = {dayId, aggregateCreatorTotals, dailyStatsEnabled, OWNER_EDITABLE};
