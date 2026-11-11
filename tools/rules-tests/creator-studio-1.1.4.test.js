// 1.1.4 Creator Studio — additive rules tests (Firestore emulator).
// Run: RULES=../../firestore.rules npx firebase emulators:exec --only firestore "node creator-studio-1.1.4.test.js"
const {initializeTestEnvironment, assertSucceeds, assertFails} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const {doc, setDoc, getDoc, getDocs, collection, serverTimestamp, updateDoc, deleteDoc, increment} = require("firebase/firestore");

(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
  const env = await initializeTestEnvironment({
    projectId: "demo-np",
    firestore: {rules: fs.readFileSync(process.env.RULES || path.join(__dirname, "../../firestore.rules"), "utf8"), host, port: Number(port)}
  });
  const base = {approved: true, status: "published", creatorUid: "alice", type: "image", caption: "Sabr", likeCount: 1, viewCount: 4, commentCount: 2};
  await env.withSecurityRulesDisabled(async (c) => {
    const f = c.firestore();
    for (const id of ["p1", "p2", "p3"]) await setDoc(doc(f, `ummah_posts/${id}`), base);
    await setDoc(doc(f, "ummah_posts/bobs"), {...base, creatorUid: "bob"});
    for (const [id, post, by] of [["c1", "p1", "bob"], ["c2", "p1", "carol"], ["c3", "bobs", "alice"], ["c4", "p1", "bob"], ["c5", "bobs", "carol"]])
      await setDoc(doc(f, `ummah_comments/${id}`), {postId: post, creatorUid: by, text: "hi", status: "published", likeCount: 0});
    await setDoc(doc(f, "ummah_comments/orphan"), {postId: "gone", creatorUid: "bob", text: "x", status: "published"});
    await setDoc(doc(f, "ummah_creator_stats/alice/days/20261025"), {views: 10});
  });
  const alice = env.authenticatedContext("alice").firestore();
  const bob = env.authenticatedContext("bob").firestore();
  const carol = env.authenticatedContext("carol").firestore();
  const anon = env.unauthenticatedContext().firestore();
  const R = []; let failed = 0;
  async function t(name, p, ok) {
    try { await (ok ? assertSucceeds(p) : assertFails(p)); R.push("PASS " + name); } catch (e) { R.push("FAIL " + name + " " + e.message); failed++; }
  }
  const P = (db, id) => doc(db, `ummah_posts/${id}`);
  const C = (db, id) => doc(db, `ummah_comments/${id}`);

  // Posts: owner studio fields.
  await t("owner edits caption + server time", updateDoc(P(alice, "p1"), {caption: "Sabr is beautiful", captionEditedAt: serverTimestamp()}), 1);
  await t("owner pins to profile", updateDoc(P(alice, "p1"), {pinnedToProfile: true}), 1);
  await t("owner archives", updateDoc(P(alice, "p2"), {archived: true}), 1);
  await t("owner pins a comment", updateDoc(P(alice, "p1"), {pinnedCommentId: "c1"}), 1);
  await t("caption > 2200 denied", updateDoc(P(alice, "p1"), {caption: "x".repeat(2201)}), 0);
  await t("captionEditedAt client time denied", updateDoc(P(alice, "p1"), {captionEditedAt: new Date(0)}), 0);
  await t("archived non-bool denied", updateDoc(P(alice, "p1"), {archived: "yes"}), 0);
  await t("owner + media change denied", updateDoc(P(alice, "p1"), {caption: "x", mediaUrl: "https://evil"}), 0);
  await t("owner approval change denied", updateDoc(P(alice, "p3"), {approved: false}), 0);
  await t("owner counter jump denied", updateDoc(P(alice, "p3"), {caption: "y", likeCount: 999}), 0);
  await t("stranger caption edit denied", updateDoc(P(bob, "p1"), {caption: "hacked"}), 0);
  await t("stranger archive denied", updateDoc(P(bob, "p1"), {archived: true}), 0);
  await t("anon pin denied", updateDoc(P(anon, "p1"), {pinnedToProfile: true}), 0);
  // Existing behaviour unchanged.
  await t("like +1 still ok", updateDoc(P(bob, "p3"), {likeCount: 2}), 1);
  await t("like +5 still denied", updateDoc(P(bob, "p3"), {likeCount: 7}), 0);
  await t("owner delete still ok", deleteDoc(P(alice, "p3")), 1);
  await t("stranger delete still denied", deleteDoc(P(bob, "p2")), 0);

  // Comments: post owner may hide/unhide and delete.
  await t("post owner hides comment", updateDoc(C(alice, "c1"), {status: "hidden"}), 1);
  await t("post owner unhides", updateDoc(C(alice, "c1"), {status: "published"}), 1);
  await t("post owner bad status denied", updateDoc(C(alice, "c1"), {status: "removed"}), 0);
  await t("post owner edits text denied", updateDoc(C(alice, "c1"), {text: "edited"}), 0);
  await t("post owner status + text denied", updateDoc(C(alice, "c1"), {status: "hidden", text: "x"}), 0);
  await t("commenter self-hide denied", updateDoc(C(bob, "c4"), {status: "hidden"}), 0);
  await t("stranger hide denied", updateDoc(C(carol, "c1"), {status: "hidden"}), 0);
  await t("hide on someone else's post denied", updateDoc(C(alice, "c5"), {status: "hidden"}), 0);
  await t("orphan comment hide denied", updateDoc(C(alice, "orphan"), {status: "hidden"}), 0);
  await t("post owner deletes comment", deleteDoc(C(alice, "c2")), 1);
  await t("stranger deletes comment denied", deleteDoc(C(carol, "c4")), 0);
  await t("own comment delete still ok", deleteDoc(C(bob, "c4")), 1);
  await t("comment like still ok", updateDoc(C(carol, "c1"), {likeCount: 1}), 1);
  await t("comment read still public", getDoc(C(anon, "c1")), 1);

  // Creator stats.
  await t("owner reads own stats", getDocs(collection(alice, "ummah_creator_stats/alice/days")), 1);
  await t("others' stats denied", getDocs(collection(bob, "ummah_creator_stats/alice/days")), 0);
  await t("anon stats denied", getDoc(doc(anon, "ummah_creator_stats/alice/days/20261025")), 0);
  await t("client stats write denied", setDoc(doc(alice, "ummah_creator_stats/alice/days/20261026"), {views: 1e9}), 0);

  console.log(R.join("\n"));
  console.log(`${R.length - failed}/${R.length} passed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})();
