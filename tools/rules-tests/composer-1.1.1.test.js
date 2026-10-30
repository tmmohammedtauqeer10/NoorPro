// 1.1.1 composer rules tests (Firestore emulator).
// Run: RULES=../../firestore.rules npx firebase emulators:exec --only firestore "node composer-1.1.1.test.js"
// Needs @firebase/rules-unit-testing + firebase; FIRESTORE_EMULATOR_HOST defaults to 127.0.0.1:8085.
const {initializeTestEnvironment, assertSucceeds, assertFails} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const {doc, setDoc, getDoc, getDocs, addDoc, collection, query, where, serverTimestamp, Timestamp, updateDoc, deleteDoc} = require("firebase/firestore");

(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
  const env = await initializeTestEnvironment({
    projectId: "demo-np",
    firestore: {rules: fs.readFileSync(process.env.RULES || path.join(__dirname, "../../firestore.rules"), "utf8"), host, port: Number(port)}
  });
  const hours = (h) => Timestamp.fromMillis(Date.now() + h * 3600e3);
  await env.withSecurityRulesDisabled(async (c) => {
    const f = c.firestore();
    await setDoc(doc(f, "ummah_posts/poll1"), {approved: true, status: "published", creatorUid: "bob", poll: {options: ["A", "B", "C"], endsAt: hours(24)}});
    await setDoc(doc(f, "ummah_posts/closed"), {approved: true, status: "published", creatorUid: "bob", poll: {options: ["A", "B"], endsAt: hours(-1)}});
    await setDoc(doc(f, "ummah_posts/plain"), {approved: true, status: "published", creatorUid: "bob"});
    await setDoc(doc(f, "ummah_follower_posts/fp1"), {creatorUid: "bob", caption: "followers only", approved: true, status: "published"});
    await setDoc(doc(f, "ummah_users/alice/following/bob"), {uid: "bob"});
  });
  const alice = env.authenticatedContext("alice").firestore();
  const carol = env.authenticatedContext("carol").firestore();
  const bob = env.authenticatedContext("bob").firestore();
  const anon = env.unauthenticatedContext().firestore();

  const sub = (o = {}) => ({creatorUid: "alice", status: "pending", approved: false, type: "text", caption: "Salam", arabicText: "", ...o});
  const S = collection(alice, "ummah_submissions");
  const R = []; let failed = 0;
  async function t(name, p, ok) {
    try { await (ok ? assertSucceeds(p) : assertFails(p)); R.push("PASS " + name); } catch (e) { R.push("FAIL " + name + " " + e.message); failed++; }
  }

  // Submissions: old shape unchanged, new fields optional + validated.
  await t("legacy submission still ok", addDoc(S, sub()), 1);
  await t("aiLabel bool ok", addDoc(S, sub({aiLabel: true})), 1);
  await t("aiLabel string denied", addDoc(S, sub({aiLabel: "yes"})), 0);
  await t("location ok", addDoc(S, sub({locationName: "Hyderabad"})), 1);
  await t("location too long denied", addDoc(S, sub({locationName: "x".repeat(81)})), 0);
  await t("visibility followers ok", addDoc(S, sub({visibility: "followers"})), 1);
  await t("visibility other denied", addDoc(S, sub({visibility: "private"})), 0);
  await t("poll ok", addDoc(S, sub({poll: {options: ["Fajr", "Isha"], endsAt: hours(24)}})), 1);
  await t("poll 1 option denied", addDoc(S, sub({poll: {options: ["Fajr"], endsAt: hours(24)}})), 0);
  await t("poll 5 options denied", addDoc(S, sub({poll: {options: ["a", "b", "c", "d", "e"], endsAt: hours(24)}})), 0);
  await t("poll past end denied", addDoc(S, sub({poll: {options: ["a", "b"], endsAt: hours(-1)}})), 0);
  await t("poll >8 days denied", addDoc(S, sub({poll: {options: ["a", "b"], endsAt: hours(24 * 9)}})), 0);
  await t("poll extra key denied", addDoc(S, sub({poll: {options: ["a", "b"], endsAt: hours(2), votes: 9}})), 0);
  await t("poll on reel denied", addDoc(S, sub({type: "reel", mediaUrl: "https://x/v.mp4", poll: {options: ["a", "b"], endsAt: hours(2)}})), 0);
  await t("reel cover thumbnail ok", addDoc(S, sub({type: "reel", mediaUrl: "https://x/v.mp4", thumbnailUrl: "https://x/c.jpg"})), 1);

  // Poll votes.
  const vote = (db, post, uid, option) => setDoc(doc(db, `ummah_posts/${post}/poll_votes/${uid}`), {uid, option, createdAt: serverTimestamp()});
  await t("vote ok", vote(alice, "poll1", "alice", 2), 1);
  await t("vote twice (update) denied", vote(alice, "poll1", "alice", 1), 0);
  await t("vote delete denied", deleteDoc(doc(alice, "ummah_posts/poll1/poll_votes/alice")), 0);
  await t("vote for someone else denied", vote(alice, "poll1", "carol", 0), 0);
  await t("vote option out of range denied", vote(carol, "poll1", "carol", 3), 0);
  await t("vote negative denied", vote(carol, "poll1", "carol", -1), 0);
  await t("vote closed poll denied", vote(carol, "closed", "carol", 0), 0);
  await t("vote on post without poll denied", vote(carol, "plain", "carol", 0), 0);
  await t("vote extra key denied", setDoc(doc(carol, "ummah_posts/poll1/poll_votes/carol"), {uid: "carol", option: 0, createdAt: serverTimestamp(), weight: 5}), 0);
  await t("vote anon denied", vote(anon, "poll1", "x", 0), 0);
  await t("tally read signed-in ok", getDocs(collection(carol, "ummah_posts/poll1/poll_votes")), 1);
  await t("tally read anon denied", getDocs(collection(anon, "ummah_posts/poll1/poll_votes")), 0);

  // Followers-only posts.
  await t("follower reads followers post", getDoc(doc(alice, "ummah_follower_posts/fp1")), 1);
  await t("follower query by creator ok", getDocs(query(collection(alice, "ummah_follower_posts"), where("creatorUid", "==", "bob"))), 1);
  await t("creator reads own followers post", getDoc(doc(bob, "ummah_follower_posts/fp1")), 1);
  await t("non-follower denied", getDoc(doc(carol, "ummah_follower_posts/fp1")), 0);
  await t("non-follower query denied", getDocs(query(collection(carol, "ummah_follower_posts"), where("creatorUid", "==", "bob"))), 0);
  await t("anon denied", getDoc(doc(anon, "ummah_follower_posts/fp1")), 0);
  await t("client write denied", setDoc(doc(bob, "ummah_follower_posts/x"), {creatorUid: "bob"}), 0);

  // Existing behaviour spot-checks.
  await t("published post still public", getDoc(doc(anon, "ummah_posts/plain")), 1);

  console.log(R.join("\n"));
  console.log(`${R.length - failed}/${R.length} passed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})();
