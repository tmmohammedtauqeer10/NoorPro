// 1.1.3 editor pro: interactive story stickers (poll / quiz / question) — additive rules tests (Firestore emulator).
// Run: RULES=../../firestore.rules npx firebase emulators:exec --only firestore "node stories-interactive-1.1.3.test.js"
// Needs @firebase/rules-unit-testing + firebase; FIRESTORE_EMULATOR_HOST defaults to 127.0.0.1:8085.
const {initializeTestEnvironment, assertSucceeds, assertFails} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const {doc, setDoc, getDoc, getDocs, collection, serverTimestamp, Timestamp, updateDoc, deleteDoc} = require("firebase/firestore");

(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
  const env = await initializeTestEnvironment({
    projectId: "demo-np",
    firestore: {rules: fs.readFileSync(process.env.RULES || path.join(__dirname, "../../firestore.rules"), "utf8"), host, port: Number(port)}
  });
  const hours = (h) => Timestamp.fromMillis(Date.now() + h * 3600e3);
  await env.withSecurityRulesDisabled(async (c) => {
    const f = c.firestore();
    await setDoc(doc(f, "ummah_stories/poll"), {authorUid: "bob", type: "image", expiresAt: hours(20), interactive: {kind: "poll", prompt: "Fajr?", options: ["Yes", "Not yet"]}});
    await setDoc(doc(f, "ummah_stories/quiz"), {authorUid: "bob", type: "image", expiresAt: hours(20), interactive: {kind: "quiz", prompt: "Surahs?", options: ["114", "100", "120"], answer: 0}});
    await setDoc(doc(f, "ummah_stories/ask"), {authorUid: "bob", type: "image", expiresAt: hours(20), interactive: {kind: "question", prompt: "Favourite dua?"}});
    await setDoc(doc(f, "ummah_stories/plain"), {authorUid: "bob", type: "image", expiresAt: hours(20)});
    await setDoc(doc(f, "ummah_stories/old"), {authorUid: "bob", type: "image", expiresAt: hours(-1), interactive: {kind: "poll", prompt: "x", options: ["a", "b"]}});
  });
  const alice = env.authenticatedContext("alice").firestore();
  const carol = env.authenticatedContext("carol").firestore();
  const bob = env.authenticatedContext("bob").firestore();
  const anon = env.unauthenticatedContext().firestore();
  const R = []; let failed = 0;
  async function t(name, p, ok) {
    try { await (ok ? assertSucceeds(p) : assertFails(p)); R.push("PASS " + name); } catch (e) { R.push("FAIL " + name + " " + e.message); failed++; }
  }
  let n = 0;
  const story = (o = {}) => ({
    authorUid: "alice", authorName: "Alice", authorHandle: "alice", avatarUrl: "", mediaUrl: "https://x/s.jpg",
    mediaPath: "ummah_stories/alice/s.jpg", type: "image", caption: "", createdAt: serverTimestamp(), expiresAt: hours(23), ...o
  });
  const post = (o) => setDoc(doc(alice, `ummah_stories/a${n++}`), story(o));

  // Existing shapes unchanged.
  await t("plain image story still ok", post({}), 1);
  await t("video story still ok", post({type: "video", durationMs: 5000, thumbUrl: "", thumbPath: "", sharedPostId: "", mediaPath: "ummah_stories/alice/v.mp4"}), 1);
  await t("unknown key still denied", post({votes: 3}), 0);

  // Interactive create.
  await t("poll story ok", post({interactive: {kind: "poll", prompt: "Fajr on time?", options: ["Yes", "No"]}}), 1);
  await t("poll 4 options ok", post({interactive: {kind: "poll", prompt: "p", options: ["a", "b", "c", "d"]}}), 1);
  await t("poll 1 option denied", post({interactive: {kind: "poll", prompt: "p", options: ["a"]}}), 0);
  await t("poll 5 options denied", post({interactive: {kind: "poll", prompt: "p", options: ["a", "b", "c", "d", "e"]}}), 0);
  await t("poll with answer denied", post({interactive: {kind: "poll", prompt: "p", options: ["a", "b"], answer: 0}}), 0);
  await t("quiz ok", post({interactive: {kind: "quiz", prompt: "q", options: ["a", "b", "c"], answer: 2}}), 1);
  await t("quiz answer out of range denied", post({interactive: {kind: "quiz", prompt: "q", options: ["a", "b"], answer: 2}}), 0);
  await t("quiz no answer denied", post({interactive: {kind: "quiz", prompt: "q", options: ["a", "b"]}}), 0);
  await t("question ok", post({interactive: {kind: "question", prompt: "Ask me"}}), 1);
  await t("question with options denied", post({interactive: {kind: "question", prompt: "Ask", options: ["a", "b"]}}), 0);
  await t("unknown kind denied", post({interactive: {kind: "slider", prompt: "x"}}), 0);
  await t("extra interactive key denied", post({interactive: {kind: "question", prompt: "x", votes: 1}}), 0);
  await t("long prompt denied", post({interactive: {kind: "question", prompt: "x".repeat(121)}}), 0);
  await t("interactive on video denied", post({type: "video", durationMs: 5000, thumbUrl: "", thumbPath: "", sharedPostId: "", mediaPath: "ummah_stories/alice/v.mp4", interactive: {kind: "question", prompt: "x"}}), 0);
  await t("interactive for someone else denied", post({authorUid: "carol", interactive: {kind: "question", prompt: "x"}}), 0);
  await t("story update still denied", updateDoc(doc(bob, "ummah_stories/poll"), {interactive: {kind: "question", prompt: "x"}}), 0);

  // Responses.
  const ans = (db, s, uid, o) => setDoc(doc(db, `ummah_stories/${s}/responses/${uid}`), {createdAt: serverTimestamp(), ...o});
  await t("poll vote ok", ans(alice, "poll", "alice", {choice: 1}), 1);
  await t("poll vote again denied", ans(alice, "poll", "alice", {choice: 0}), 0);
  await t("poll vote out of range denied", ans(carol, "poll", "carol", {choice: 2}), 0);
  await t("poll vote negative denied", ans(carol, "poll", "carol", {choice: -1}), 0);
  await t("poll vote with text denied", ans(carol, "poll", "carol", {choice: 0, text: "hi"}), 0);
  await t("vote as someone else denied", ans(alice, "poll", "carol", {choice: 0}), 0);
  await t("author can't answer own", ans(bob, "poll", "bob", {choice: 0}), 0);
  await t("quiz answer ok", ans(alice, "quiz", "alice", {choice: 2}), 1);
  await t("question text ok", ans(alice, "ask", "alice", {text: "Rabbana atina"}), 1);
  await t("question empty denied", ans(carol, "ask", "carol", {text: ""}), 0);
  await t("question too long denied", ans(carol, "ask", "carol", {text: "x".repeat(201)}), 0);
  await t("question choice denied", ans(carol, "ask", "carol", {choice: 0}), 0);
  await t("plain story response denied", ans(alice, "plain", "alice", {choice: 0}), 0);
  await t("expired story response denied", ans(alice, "old", "alice", {choice: 0}), 0);
  await t("anon response denied", setDoc(doc(anon, "ummah_stories/poll/responses/x"), {choice: 0, createdAt: serverTimestamp()}), 0);
  await t("viewer reads own response", getDoc(doc(alice, "ummah_stories/poll/responses/alice")), 1);
  await t("other viewer can't read", getDoc(doc(carol, "ummah_stories/poll/responses/alice")), 0);
  await t("author lists responses", getDocs(collection(bob, "ummah_stories/poll/responses")), 1);
  await t("non-author list denied", getDocs(collection(carol, "ummah_stories/poll/responses")), 0);
  await t("response delete denied", deleteDoc(doc(alice, "ummah_stories/poll/responses/alice")), 0);

  // Viewers markers unchanged.
  await t("viewer marker still ok", setDoc(doc(alice, "ummah_stories/plain/viewers/alice"), {viewedAt: serverTimestamp()}), 1);

  console.log(R.join("\n"));
  console.log(`${R.length - failed}/${R.length} passed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})();
