// 1.1.5 Halal Guard: pure moderation logic (no Firebase imports) so it can be unit-tested with
// mocked Vision / Video Intelligence clients. index.js wires it into the publisher and triggers,
// and every server-side check is behind an env flag that defaults OFF (see guardFlags()).
//
// The word list lives in halal-words.json and is shared byte-for-byte with the app
// (app/src/main/assets/halal/words.json). The Kotlin port (HalalTextFilter.kt) must stay in step
// with this file; both run the same vectors in halal-vectors.json.
"use strict";
const WORDS = require("./halal-words.json");

// ---------------------------------------------------------------- text filter
const ZERO_WIDTH = /[\u200B-\u200D\u2060\uFEFF\u00AD]/g;
const TOKEN = /[\p{L}\p{M}\p{N}@$!|+*\u200B-\u200D\u2060\uFEFF\u00AD]+/gu;
const LEET = {"0": "o", "1": "i", "3": "e", "4": "a", "5": "s", "7": "t", "8": "b", "9": "g",
  "@": "a", "$": "s", "!": "i", "|": "i", "+": "t"};

function normToken(raw) {
  let x = String(raw || "").replace(ZERO_WIDTH, "").toLowerCase().normalize("NFD")
    .replace(/\p{Mn}/gu, "").replace(/\u0640/g, "")
    .replace(/[\u0622\u0623\u0625\u0671]/g, "\u0627").replace(/\u0629/g, "\u0647")
    .replace(/[\u0649\u06CC]/g, "\u064A").replace(/\u06A9/g, "\u0643")
    .replace(/[\u06C1\u06BE]/g, "\u0647");
  x = x.replace(/^@+/, "").replace(/^[!|+*]+/, "").replace(/[!|+*]+$/, "");
  if (/[a-z]/.test(x)) x = x.replace(/[0134578@$!|+9]/g, (c) => LEET[c] || c);
  return x;
}
const collapse = (s) => s.replace(/(.)\1+/gu, "$1");

function compile(words) {
  const terms = (words.terms || []).map((t) => {
    const n = normToken(t.t);
    return {...t, n, c: collapse(n)};
  }).filter((t) => t.n);
  const patterns = (words.patterns || []).map((p) => ({...p, rx: new RegExp(p.re, "u")}));
  return {terms, patterns};
}
const COMPILED = compile(WORDS);

function wildEq(a, b) {
  if (a.length !== b.length) return false;
  for (let i = 0; i < a.length; i++) if (a[i] !== "*" && a[i] !== b[i]) return false;
  return true;
}

function termHit(n, t) {
  if (n.includes("*")) {
    const stars = (n.match(/\*/g) || []).length;
    if (n.length < 3 || n.length - stars < 2) return false;
    if (t.mode === "word") return wildEq(n, t.n);
    if (t.mode === "stem") return n.length >= t.n.length && wildEq(n.slice(0, t.n.length), t.n);
    return false;
  }
  const nc = collapse(n);
  const useC = t.c.length >= 3;
  if (t.mode === "word") return n === t.n || (useC && nc === t.c);
  if (t.mode === "stem") return n.startsWith(t.n) || (useC && nc.startsWith(t.c));
  return n.includes(t.n) || (useC && nc.includes(t.c));
}

function tokenize(text) {
  const out = [];
  const s = String(text || "");
  for (const m of s.matchAll(TOKEN)) {
    const n = normToken(m[0]);
    if (n) out.push({raw: m[0], start: m.index, end: m.index + m[0].length, n});
  }
  return out;
}

// Spaced-out obfuscation: "f u c k", "f.u.c.k", "s-h-i-t" -> one candidate token.
function joinedRuns(tokens, text) {
  const runs = [];
  let cur = [];
  const flush = () => {
    if (cur.length >= 3) runs.push({start: cur[0].start, end: cur[cur.length - 1].end, n: cur.map((t) => t.n).join("")});
    cur = [];
  };
  for (const t of tokens) {
    const single = [...t.n].length === 1;
    const gapOk = cur.length === 0 || (t.start - cur[cur.length - 1].end) <= 3;
    if (single && gapOk) cur.push(t);
    else {
      flush();
      if (single) cur.push(t);
    }
  }
  flush();
  return runs;
}

const SEV_RANK = {none: 0, mild: 1, severe: 2};

function scanText(text, compiled = COMPILED) {
  const tokens = tokenize(text);
  const matches = [];
  const candidates = [...tokens, ...joinedRuns(tokens, text)];
  for (const tok of candidates) {
    for (const t of compiled.terms) {
      if (termHit(tok.n, t)) {
        matches.push({kind: "term", term: t.t, cat: t.cat, sev: t.sev, start: tok.start, end: tok.end});
        break;
      }
    }
  }
  const joined = tokens.map((t) => t.n).join(" ");
  for (const p of compiled.patterns) {
    if (p.rx.test(joined)) matches.push({kind: "pattern", term: p.id, cat: p.cat, sev: p.sev, start: -1, end: -1});
  }
  let severity = "none";
  for (const m of matches) if (SEV_RANK[m.sev] > SEV_RANK[severity]) severity = m.sev;
  const categories = [...new Set(matches.map((m) => m.cat))];
  let action = "allow";
  if (severity === "severe") action = "block";
  else if (matches.some((m) => m.sev === "mild" && (m.cat === "hate" || m.cat === "scam"))) action = "queue";
  else if (severity === "mild") action = "mask";
  return {action, severity, categories, matches};
}

function maskText(text, result) {
  const s = String(text || "");
  const spans = (result || scanText(s)).matches.filter((m) => m.kind === "term" && m.start >= 0)
    .sort((a, b) => a.start - b.start);
  let out = "";
  let pos = 0;
  for (const m of spans) {
    if (m.start < pos) continue;
    const word = s.slice(m.start, m.end);
    const chars = [...word];
    out += s.slice(pos, m.start) + chars[0] + "\u2022".repeat(Math.max(2, chars.length - 1));
    pos = m.end;
  }
  return out + s.slice(pos);
}

const CAT_LABEL = {profanity: "bad language", sexual: "sexual content", hate: "hate or sectarian insults", scam: "a possible scam"};
function textUserReason(result) {
  const what = result.categories.map((c) => CAT_LABEL[c] || c).join(", ");
  if (result.action === "block") return `This contains ${what}, which breaks NoorPro's Halal community guidelines. Please edit it and try again.`;
  if (result.action === "queue") return `This may contain ${what}. A moderator will review it before it goes public.`;
  if (result.action === "mask") return `Some words were hidden because they break the Halal community guidelines (${what}).`;
  return "";
}

// ---------------------------------------------------------------- media (SafeSearch)
const LIKELIHOOD = {UNKNOWN: 0, VERY_UNLIKELY: 1, UNLIKELY: 2, POSSIBLE: 3, LIKELY: 4, VERY_LIKELY: 5};
const LIKELIHOOD_NAME = ["UNKNOWN", "VERY_UNLIKELY", "UNLIKELY", "POSSIBLE", "LIKELY", "VERY_LIKELY"];
function lk(v) {
  if (typeof v === "number") return Math.max(0, Math.min(5, v | 0));
  const k = String(v || "UNKNOWN").toUpperCase();
  return Object.prototype.hasOwnProperty.call(LIKELIHOOD, k) ? LIKELIHOOD[k] : 0;
}

const USER_REASON = {
  adult: "This media looks like it contains nudity or sexual content, which isn't allowed on NoorPro.",
  racy: "This media looks immodest (revealing clothing or suggestive poses). Please share modest content.",
  violence: "This media looks graphically violent or gory, which isn't allowed on NoorPro.",
  review: "Your post is being checked by a moderator and will appear once approved, in shaa Allah.",
  restricted: "Your account can't post right now because of a moderation action."
};

// Thresholds (brief): adult or racy >= LIKELY, or violence >= VERY_LIKELY -> reject.
// Any POSSIBLE (or violence LIKELY) -> moderation_queue. Everything else -> approve.
function decideSafeSearch(annotation) {
  const a = annotation || {};
  const scores = {adult: lk(a.adult), racy: lk(a.racy), violence: lk(a.violence), medical: lk(a.medical), spoof: lk(a.spoof)};
  const named = Object.fromEntries(Object.entries(scores).map(([k, v]) => [k, LIKELIHOOD_NAME[v]]));
  if (scores.adult >= 4) return {decision: "reject", reason: "adult", userReason: USER_REASON.adult, scores: named};
  if (scores.racy >= 4) return {decision: "reject", reason: "racy", userReason: USER_REASON.racy, scores: named};
  if (scores.violence >= 5) return {decision: "reject", reason: "violence", userReason: USER_REASON.violence, scores: named};
  if (scores.adult === 3) return {decision: "queue", reason: "possible_adult", userReason: USER_REASON.review, scores: named};
  if (scores.racy === 3) return {decision: "queue", reason: "possible_racy", userReason: USER_REASON.review, scores: named};
  if (scores.violence >= 3) return {decision: "queue", reason: "possible_violence", userReason: USER_REASON.review, scores: named};
  return {decision: "approve", reason: "", userReason: "", scores: named};
}

// Video Intelligence EXPLICIT_CONTENT_DETECTION returns per-frame pornographyLikelihood.
function decideVideoFrames(frames) {
  let max = 0;
  let at = null;
  for (const f of frames || []) {
    const v = lk(f && f.pornographyLikelihood);
    if (v > max) { max = v; at = f.timeOffset || null; }
  }
  const scores = {pornography: LIKELIHOOD_NAME[max], frames: (frames || []).length};
  if (max >= 4) return {decision: "reject", reason: "adult_video", userReason: USER_REASON.adult, scores, at};
  if (max === 3) return {decision: "queue", reason: "possible_adult_video", userReason: USER_REASON.review, scores, at};
  return {decision: "approve", reason: "", userReason: "", scores, at};
}

const DECISION_RANK = {approve: 0, queue: 1, reject: 2};
function combineDecisions(list) {
  let best = {decision: "approve", reason: "", userReason: ""};
  for (const d of list || []) if (DECISION_RANK[d.decision] > DECISION_RANK[best.decision]) best = d;
  return {decision: best.decision, reason: best.reason, userReason: best.userReason};
}

const MAX_IMAGE_BYTES = 15 * 1024 * 1024;
const MAX_INLINE_VIDEO_BYTES = 20 * 1024 * 1024;

// item: {images: [https urls], video: {gsUri?, url?} | null}
// deps: {vision: {safeSearchDetection}, video: {annotateVideo}, fetchBytes(url, max) -> Buffer|null, videoEnabled}
// Any client error -> queue (never auto-publish something we failed to check).
async function scanMedia(item, deps) {
  const parts = [];
  try {
    for (const url of (item.images || []).filter(Boolean).slice(0, 10)) {
      const content = await deps.fetchBytes(url, MAX_IMAGE_BYTES);
      if (!content) { parts.push({decision: "queue", reason: "image_unreadable", userReason: USER_REASON.review, url}); continue; }
      const [res] = await deps.vision.safeSearchDetection({image: {content}});
      if (res && res.error && res.error.message) throw new Error(res.error.message);
      parts.push({...decideSafeSearch((res && res.safeSearchAnnotation) || {}), source: "image", url});
    }
    const v = item.video;
    if (v && (v.gsUri || v.url) && !deps.videoEnabled && !(item.images || []).some(Boolean)) {
      // No poster frame and video scanning is off: a person has to look at it.
      parts.push({decision: "queue", reason: "video_unscanned", userReason: USER_REASON.review, source: "video"});
    }
    if (v && (v.gsUri || v.url) && deps.videoEnabled) {
      let request = null;
      if (v.gsUri) request = {inputUri: v.gsUri};
      else {
        const bytes = await deps.fetchBytes(v.url, MAX_INLINE_VIDEO_BYTES);
        if (bytes) request = {inputContent: bytes.toString("base64")};
        else parts.push({decision: "queue", reason: "video_too_large", userReason: USER_REASON.review, source: "video"});
      }
      if (request) {
        const [operation] = await deps.video.annotateVideo({...request, features: ["EXPLICIT_CONTENT_DETECTION"]});
        const [res] = await operation.promise();
        const ann = (res && res.annotationResults && res.annotationResults[0]) || {};
        if (ann.error && ann.error.message) throw new Error(ann.error.message);
        parts.push({...decideVideoFrames((ann.explicitAnnotation && ann.explicitAnnotation.frames) || []), source: "video"});
      }
    }
  } catch (error) {
    return {decision: "queue", reason: "scan_error", userReason: USER_REASON.review,
      error: String((error && error.message) || error).slice(0, 300), parts};
  }
  return {...combineDecisions(parts), parts};
}

// ---------------------------------------------------------------- flags, new accounts, strikes
function guardFlags(env = process.env) {
  const on = (k) => String(env[k] || "").toLowerCase() === "true";
  return {scan: on("HALAL_GUARD_SCAN"), video: on("HALAL_GUARD_VIDEO"), text: on("HALAL_TEXT_FILTER"),
    newAccount: on("HALAL_NEW_ACCOUNT_QUEUE")};
}
const DAY_MS = 86400000;
const NEW_ACCOUNT_DAYS = 7;
const NEW_ACCOUNT_POSTS = 3;
function newAccountGate({createdMs, nowMs, priorPublished}) {
  if (!createdMs || !Number.isFinite(createdMs)) return false;
  return (nowMs - createdMs) < NEW_ACCOUNT_DAYS * DAY_MS && (Number(priorPublished) || 0) < NEW_ACCOUNT_POSTS;
}

const STRIKE_LIMIT = 3;
function applyStrike(rec, nowMs, days = 7) {
  const r = rec || {};
  const count = (Number(r.count) || 0) + 1;
  const out = {count, lastStrikeMs: nowMs, banned: r.banned === true,
    suspendedUntilMs: Number(r.suspendedUntilMs) || 0};
  if (count % STRIKE_LIMIT === 0) out.suspendedUntilMs = Math.max(out.suspendedUntilMs, nowMs + clampDays(days) * DAY_MS);
  return out;
}
function clampDays(d) { const n = Math.round(Number(d) || 7); return Math.max(1, Math.min(365, n)); }
function isRestricted(rec, nowMs) {
  if (!rec) return false;
  return rec.banned === true || (Number(rec.suspendedUntilMs) || 0) > nowMs;
}

const ACTIONS = ["approve", "remove", "warn", "suspend", "ban", "unban", "dismiss"];
const TARGETS = ["queue", "report", "user"];
function validateDecision(data) {
  const d = data || {};
  const action = String(d.action || "");
  const target = String(d.target || "");
  const id = String(d.id || "").slice(0, 200);
  if (!ACTIONS.includes(action)) return {ok: false, error: "Unknown action"};
  if (!TARGETS.includes(target)) return {ok: false, error: "Unknown target"};
  if (!id || id.includes("/")) return {ok: false, error: "Missing id"};
  return {ok: true, value: {action, target, id, reason: String(d.reason || "").slice(0, 500),
    days: clampDays(d.days), sensitive: d.sensitive === true, strike: d.strike !== false}};
}
// What each action does (index.js executes it). Pure so it is testable.
function decisionEffects(action, strike = true) {
  switch (action) {
  case "approve": return ["publish", "close"];
  case "remove": return strike ? ["remove_content", "strike", "notify", "close"] : ["remove_content", "notify", "close"];
  case "warn": return ["strike", "notify", "close"];
  case "suspend": return ["suspend", "notify", "close"];
  case "ban": return ["remove_content", "ban", "close"];
  case "unban": return ["unban"];
  case "dismiss": return ["close"];
  default: return [];
  }
}

// ---------------------------------------------------------------- whole-submission guard
// input: {texts: {caption, arabicText, ...}, images: [...], video, createdMs, priorPublished, strikes}
async function guardContent(input, deps) {
  const flags = deps.flags || {};
  const nowMs = deps.nowMs || Date.now();
  const out = {decision: "approve", reasons: [], userReason: "", masked: {}, scan: null, text: {}};
  if (isRestricted(input.strikes, nowMs)) {
    const until = input.strikes.banned ? "" : ` until ${new Date(input.strikes.suspendedUntilMs).toISOString().slice(0, 10)}`;
    return {...out, decision: "reject", reasons: ["account_restricted"], userReason: `${USER_REASON.restricted}${until}`};
  }
  const queue = (reason, userReason) => {
    if (out.decision === "approve") { out.decision = "queue"; out.userReason = userReason || USER_REASON.review; }
    out.reasons.push(reason);
  };
  if (flags.text) {
    for (const [field, value] of Object.entries(input.texts || {})) {
      if (!value) continue;
      const r = scanText(value);
      if (r.action === "allow") continue;
      out.text[field] = {action: r.action, categories: r.categories, terms: r.matches.map((m) => m.term).slice(0, 10)};
      if (r.action === "block") return {...out, decision: "reject", reasons: [`text_${field}`], userReason: textUserReason(r)};
      if (r.action === "queue") queue(`text_${field}`, textUserReason(r));
      if (r.action === "mask") out.masked[field] = maskText(value, r);
    }
  }
  const hasMedia = (input.images || []).some(Boolean) || (input.video && (input.video.gsUri || input.video.url));
  if (flags.scan && hasMedia) {
    // Object.create keeps lazy client getters lazy (a spread would construct both clients).
    const scanDeps = Object.assign(Object.create(deps), {videoEnabled: flags.video === true});
    const s = await scanMedia({images: input.images || [], video: input.video || null}, scanDeps);
    out.scan = {decision: s.decision, reason: s.reason, error: s.error || "", parts: (s.parts || []).map((p) =>
      ({decision: p.decision, reason: p.reason || "", source: p.source || "", scores: p.scores || {}}))};
    if (s.decision === "reject") return {...out, decision: "reject", reasons: [s.reason], userReason: s.userReason};
    if (s.decision === "queue") queue(s.reason, s.userReason);
  }
  if (flags.newAccount && newAccountGate({createdMs: input.createdMs, nowMs, priorPublished: input.priorPublished})) {
    queue("new_account", "New accounts' first posts are checked by a moderator. Thanks for your patience!");
  }
  return out;
}

module.exports = {
  WORDS, normToken, collapse, tokenize, scanText, maskText, textUserReason,
  LIKELIHOOD, decideSafeSearch, decideVideoFrames, combineDecisions, scanMedia,
  guardFlags, newAccountGate, applyStrike, isRestricted, validateDecision, decisionEffects, guardContent,
  STRIKE_LIMIT, DAY_MS, USER_REASON
};
