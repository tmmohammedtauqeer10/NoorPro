// node halal.test.js  (Halal Guard pure logic; Vision / Video Intelligence clients are mocked)
"use strict";
const assert = require("assert");
const h = require("./halal");
const vectors = require("./halal-vectors.json");

let passed = 0;
const tests = [];
const test = (name, fn) => tests.push([name, fn]);

test("shared vectors", () => {
  for (const c of vectors.cases) {
    const r = h.scanText(c.text);
    assert.strictEqual(r.action, c.action, `${c.text} -> ${r.action} (${r.matches.map((m) => m.term)})`);
    if (c.masked) assert.strictEqual(h.maskText(c.text, r), c.masked);
  }
});
test("normalisation: leet, marks, zero-width, Arabic letters", () => {
  assert.strictEqual(h.normToken("Sh1T"), "shit");
  assert.strictEqual(h.normToken("f\u200Buck"), "fuck");
  assert.strictEqual(h.normToken("\u0634\u064E\u0631\u0652\u0645\u064F\u0648\u0637\u064E\u0629"), "\u0634\u0631\u0645\u0648\u0637\u0647");
  assert.strictEqual(h.normToken("2024"), "2024");
  assert.strictEqual(h.normToken("@user"), "user");
});
test("zero-width obfuscation is caught", () => assert.strictEqual(h.scanText("mad\u200Barchod").action, "block"));
test("masking keeps clean words", () => assert.strictEqual(h.maskText("ok shit ok"), "ok s\u2022\u2022\u2022 ok"));
test("user reasons", () => {
  assert.match(h.textUserReason(h.scanText("madarchod")), /bad language/);
  assert.strictEqual(h.textUserReason(h.scanText("salaam")), "");
});
test("word lists are identical in app assets", () => {
  const fs = require("fs");
  const path = require("path");
  const a = fs.readFileSync(path.join(__dirname, "halal-words.json"));
  const b = fs.readFileSync(path.join(__dirname, "..", "app", "src", "main", "assets", "halal", "words.json"));
  assert.ok(a.equals(b), "app/src/main/assets/halal/words.json must be a byte copy of functions/halal-words.json");
});

test("SafeSearch thresholds", () => {
  assert.strictEqual(h.decideSafeSearch({adult: "LIKELY"}).decision, "reject");
  assert.strictEqual(h.decideSafeSearch({adult: "VERY_LIKELY"}).reason, "adult");
  assert.strictEqual(h.decideSafeSearch({racy: "LIKELY"}).reason, "racy");
  assert.strictEqual(h.decideSafeSearch({violence: "LIKELY"}).decision, "queue");
  assert.strictEqual(h.decideSafeSearch({violence: "VERY_LIKELY"}).decision, "reject");
  assert.strictEqual(h.decideSafeSearch({adult: "POSSIBLE"}).decision, "queue");
  assert.strictEqual(h.decideSafeSearch({racy: "POSSIBLE"}).reason, "possible_racy");
  assert.strictEqual(h.decideSafeSearch({adult: "UNLIKELY", racy: "UNLIKELY", violence: "POSSIBLE"}).decision, "queue");
  assert.strictEqual(h.decideSafeSearch({adult: "VERY_UNLIKELY", racy: "UNLIKELY", medical: "VERY_LIKELY", spoof: "LIKELY"}).decision, "approve");
  assert.strictEqual(h.decideSafeSearch({adult: 4}).decision, "reject");
  assert.strictEqual(h.decideSafeSearch({}).decision, "approve");
  assert.match(h.decideSafeSearch({adult: "LIKELY"}).userReason, /nudity/);
});
test("video frame thresholds", () => {
  assert.strictEqual(h.decideVideoFrames([{pornographyLikelihood: "UNLIKELY"}, {pornographyLikelihood: "LIKELY", timeOffset: {seconds: 3}}]).decision, "reject");
  assert.strictEqual(h.decideVideoFrames([{pornographyLikelihood: "POSSIBLE"}]).decision, "queue");
  assert.strictEqual(h.decideVideoFrames([]).decision, "approve");
});

const okVision = (ann) => ({calls: 0, async safeSearchDetection(req) { this.calls++; assert.ok(req.image.content); return [{safeSearchAnnotation: ann}]; }});
const bytes = async () => Buffer.from("img");
test("scanMedia approves clean images", async () => {
  const vision = okVision({adult: "VERY_UNLIKELY", racy: "UNLIKELY"});
  const r = await h.scanMedia({images: ["https://a/1.jpg", "https://a/2.jpg"]}, {vision, fetchBytes: bytes});
  assert.strictEqual(r.decision, "approve");
  assert.strictEqual(vision.calls, 2);
});
test("scanMedia rejects when any carousel image is explicit", async () => {
  let i = 0;
  const vision = {async safeSearchDetection() { i++; return [{safeSearchAnnotation: i === 2 ? {adult: "VERY_LIKELY"} : {}}]; }};
  const r = await h.scanMedia({images: ["https://a/1", "https://a/2"]}, {vision, fetchBytes: bytes});
  assert.strictEqual(r.decision, "reject");
});
test("scanMedia queues on client error", async () => {
  const vision = {async safeSearchDetection() { throw new Error("PERMISSION_DENIED: Vision API disabled"); }};
  const r = await h.scanMedia({images: ["https://a/1"]}, {vision, fetchBytes: bytes});
  assert.strictEqual(r.decision, "queue");
  assert.strictEqual(r.reason, "scan_error");
  assert.match(r.error, /PERMISSION_DENIED/);
});
test("scanMedia queues on per-image API error field", async () => {
  const vision = {async safeSearchDetection() { return [{error: {message: "bad image"}}]; }};
  assert.strictEqual((await h.scanMedia({images: ["https://a/1"]}, {vision, fetchBytes: bytes})).decision, "queue");
});
test("scanMedia queues unreadable image", async () => {
  const r = await h.scanMedia({images: ["https://a/1"]}, {vision: okVision({}), fetchBytes: async () => null});
  assert.strictEqual(r.reason, "image_unreadable");
});
const videoClient = (frames, seen) => ({async annotateVideo(req) {
  seen.push(req);
  return [{promise: async () => [{annotationResults: [{explicitAnnotation: {frames}}]}]}];
}});
test("video scanning uses gs:// uri and only when enabled", async () => {
  const seen = [];
  const video = videoClient([{pornographyLikelihood: "VERY_LIKELY"}], seen);
  const off = await h.scanMedia({images: [], video: {gsUri: "gs://b/v.mp4"}}, {vision: okVision({}), video, fetchBytes: bytes, videoEnabled: false});
  assert.strictEqual(off.decision, "queue");
  assert.strictEqual(off.reason, "video_unscanned");
  assert.strictEqual(seen.length, 0);
  const withThumb = await h.scanMedia({images: ["https://a/t.jpg"], video: {gsUri: "gs://b/v.mp4"}}, {vision: okVision({}), video, fetchBytes: bytes, videoEnabled: false});
  assert.strictEqual(withThumb.decision, "approve");
  const on = await h.scanMedia({images: [], video: {gsUri: "gs://b/v.mp4"}}, {vision: okVision({}), video, fetchBytes: bytes, videoEnabled: true});
  assert.strictEqual(on.decision, "reject");
  assert.deepStrictEqual(seen[0], {inputUri: "gs://b/v.mp4", features: ["EXPLICIT_CONTENT_DETECTION"]});
});
test("CDN video is sent inline, too-large video is queued", async () => {
  const seen = [];
  const video = videoClient([{pornographyLikelihood: "UNLIKELY"}], seen);
  const r = await h.scanMedia({images: [], video: {url: "https://cdn/v.mp4"}}, {video, fetchBytes: bytes, videoEnabled: true});
  assert.strictEqual(r.decision, "approve");
  assert.ok(seen[0].inputContent);
  const big = await h.scanMedia({images: [], video: {url: "https://cdn/v.mp4"}}, {video, fetchBytes: async () => null, videoEnabled: true});
  assert.strictEqual(big.reason, "video_too_large");
});

test("flags default off", () => {
  assert.deepStrictEqual(h.guardFlags({}), {scan: false, video: false, text: false, newAccount: false});
  assert.deepStrictEqual(h.guardFlags({HALAL_GUARD_SCAN: "true", HALAL_TEXT_FILTER: "TRUE"}), {scan: true, video: false, text: true, newAccount: false});
});
test("new-account gate", () => {
  const now = Date.UTC(2026, 9, 25);
  assert.strictEqual(h.newAccountGate({createdMs: now - 2 * h.DAY_MS, nowMs: now, priorPublished: 0}), true);
  assert.strictEqual(h.newAccountGate({createdMs: now - 2 * h.DAY_MS, nowMs: now, priorPublished: 3}), false);
  assert.strictEqual(h.newAccountGate({createdMs: now - 8 * h.DAY_MS, nowMs: now, priorPublished: 0}), false);
  assert.strictEqual(h.newAccountGate({createdMs: 0, nowMs: now, priorPublished: 0}), false);
});
test("strikes: 3 = suspension for the chosen days", () => {
  const now = 1000;
  let r = h.applyStrike(null, now, 7);
  assert.strictEqual(r.count, 1);
  assert.strictEqual(h.isRestricted(r, now), false);
  r = h.applyStrike(r, now, 7);
  r = h.applyStrike(r, now, 14);
  assert.strictEqual(r.count, 3);
  assert.strictEqual(r.suspendedUntilMs, now + 14 * h.DAY_MS);
  assert.strictEqual(h.isRestricted(r, now + 13 * h.DAY_MS), true);
  assert.strictEqual(h.isRestricted(r, now + 15 * h.DAY_MS), false);
  assert.strictEqual(h.isRestricted({banned: true}, now), true);
});
test("decision validation + effects", () => {
  assert.strictEqual(h.validateDecision({action: "nuke", target: "queue", id: "x"}).ok, false);
  assert.strictEqual(h.validateDecision({action: "approve", target: "queue", id: "a/b"}).ok, false);
  const v = h.validateDecision({action: "suspend", target: "report", id: "r1", days: 999});
  assert.strictEqual(v.ok, true);
  assert.strictEqual(v.value.days, 365);
  assert.deepStrictEqual(h.decisionEffects("approve"), ["publish", "close"]);
  assert.ok(h.decisionEffects("remove").includes("strike"));
  assert.ok(!h.decisionEffects("remove", false).includes("strike"));
  assert.ok(h.decisionEffects("ban").includes("ban"));
});

const flagsAll = {scan: true, video: false, text: true, newAccount: true};
test("guardContent keeps lazy clients lazy", async () => {
  let videoBuilt = 0;
  const deps = {flags: {scan: true, video: false}, fetchBytes: bytes, get vision() { return okVision({}); }, get video() { videoBuilt++; return {}; }};
  const r = await h.guardContent({texts: {}, images: ["https://a"]}, deps);
  assert.strictEqual(r.decision, "approve");
  assert.strictEqual(videoBuilt, 0);
});
test("guardContent: everything off -> approve, no client calls", async () => {
  const vision = {async safeSearchDetection() { throw new Error("must not be called"); }};
  const r = await h.guardContent({texts: {caption: "madarchod"}, images: ["https://a"]}, {flags: {}, vision, fetchBytes: bytes});
  assert.strictEqual(r.decision, "approve");
});
test("guardContent: severe text rejects before media scan", async () => {
  const vision = {async safeSearchDetection() { throw new Error("must not be called"); }};
  const r = await h.guardContent({texts: {caption: "watch p0rn"}, images: ["https://a"]}, {flags: flagsAll, vision, fetchBytes: bytes});
  assert.strictEqual(r.decision, "reject");
  assert.match(r.userReason, /sexual content/);
});
test("guardContent: mild text is masked and still approved", async () => {
  const r = await h.guardContent({texts: {caption: "oh shit"}, images: []}, {flags: flagsAll, fetchBytes: bytes});
  assert.strictEqual(r.decision, "approve");
  assert.strictEqual(r.masked.caption, "oh s\u2022\u2022\u2022");
});
test("guardContent: possible media -> queue; scan error -> queue", async () => {
  const r = await h.guardContent({texts: {}, images: ["https://a"]}, {flags: flagsAll, vision: okVision({racy: "POSSIBLE"}), fetchBytes: bytes});
  assert.strictEqual(r.decision, "queue");
  assert.deepStrictEqual(r.reasons, ["possible_racy"]);
  const e = await h.guardContent({texts: {}, images: ["https://a"]}, {flags: flagsAll, vision: {async safeSearchDetection() { throw new Error("x"); }}, fetchBytes: bytes});
  assert.strictEqual(e.decision, "queue");
});
test("guardContent: new account queues, restricted account rejects", async () => {
  const now = Date.UTC(2026, 9, 25);
  const r = await h.guardContent({texts: {caption: "salaam"}, createdMs: now - h.DAY_MS, priorPublished: 1}, {flags: flagsAll, nowMs: now});
  assert.strictEqual(r.decision, "queue");
  assert.deepStrictEqual(r.reasons, ["new_account"]);
  const s = await h.guardContent({texts: {}, strikes: {suspendedUntilMs: now + h.DAY_MS}}, {flags: {}, nowMs: now});
  assert.strictEqual(s.decision, "reject");
  assert.match(s.userReason, /until 2026-10-26/);
});

(async () => {
  for (const [name, fn] of tests) {
    try { await fn(); passed++; console.log("ok -", name); } catch (e) { console.error("FAIL -", name, "\n", e.message); process.exitCode = 1; }
  }
  console.log(`halal: ${passed}/${tests.length} passed`);
})();
