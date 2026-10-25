// 1.1.5 Halal Guard — additive rules tests (Firestore emulator).
// Run: RULES=$PWD/firestore.rules npx firebase emulators:exec --only firestore "node halal-guard-1.1.5.test.js"
const {initializeTestEnvironment, assertSucceeds, assertFails} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const {doc, setDoc, getDoc, getDocs, collection, addDoc, updateDoc, deleteDoc, serverTimestamp} = require("firebase/firestore");

(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
  const env = await initializeTestEnvironment({
    projectId: "demo-np",
    firestore: {rules: fs.readFileSync(process.env.RULES || path.join(__dirname, "../../firestore.rules"), "utf8"), host, port: Number(port)}
  });
  await env.withSecurityRulesDisabled(async (c) => {
    const f = c.firestore();
    await setDoc(doc(f, "admins/mod1"), {addedBy: "console"});
    await setDoc(doc(f, "moderation_queue/q1"), {kind: "post", targetId: "s1", creatorUid: "bob", status: "open"});
    await setDoc(doc(f, "moderation_audit/a1"), {actorUid: "mod1", action: "approve"});
    await setDoc(doc(f, "user_strikes/bob"), {count: 1});
    await setDoc(doc(f, "ummah_reports/r1"), {reporterUid: "carol", targetType: "post", targetId: "p1", reason: "Immodest / indecent", status: "open"});
    await setDoc(doc(f, "moderation_notices/bob/items/n1"), {kind: "post", reason: "x", seen: false});
    await setDoc(doc(f, "pilgrimageGuides/g1"), {published: true, title: "Umrah"});
    await setDoc(doc(f, "ummah_posts/p1"), {approved: true, status: "published", creatorUid: "alice", caption: "hi", likeCount: 0});
  });
  const mod = env.authenticatedContext("mod1").firestore();
  const claim = env.authenticatedContext("claimAdmin", {admin: true}).firestore();
  const bob = env.authenticatedContext("bob").firestore();
  const carol = env.authenticatedContext("carol").firestore();
  const anon = env.unauthenticatedContext().firestore();
  const R = []; let failed = 0;
  async function t(name, p, ok) {
    try { await (ok ? assertSucceeds(p) : assertFails(p)); R.push("PASS " + name); } catch (e) { R.push("FAIL " + name + " " + e.message); failed++; }
  }

  // admins/{uid}
  await t("moderator reads own admins doc", getDoc(doc(mod, "admins/mod1")), 1);
  await t("user reads own (missing) admins doc", getDoc(doc(bob, "admins/bob")), 1);
  await t("user cannot read someone else's admins doc", getDoc(doc(bob, "admins/mod1")), 0);
  await t("user cannot make self admin", setDoc(doc(bob, "admins/bob"), {x: 1}), 0);
  await t("moderator cannot add admins from the client", setDoc(doc(mod, "admins/carol"), {x: 1}), 0);
  await t("anon cannot list admins", getDocs(collection(anon, "admins")), 0);

  // queue
  await t("admins-doc moderator reads queue", getDocs(collection(mod, "moderation_queue")), 1);
  await t("claim admin reads queue", getDoc(doc(claim, "moderation_queue/q1")), 1);
  await t("user cannot read queue", getDocs(collection(bob, "moderation_queue")), 0);
  await t("anon cannot read queue", getDoc(doc(anon, "moderation_queue/q1")), 0);
  await t("moderator cannot write queue directly", updateDoc(doc(mod, "moderation_queue/q1"), {status: "resolved"}), 0);
  await t("user cannot create queue items", setDoc(doc(bob, "moderation_queue/q2"), {kind: "post"}), 0);

  // reports
  await t("moderator reads reports", getDocs(collection(mod, "ummah_reports")), 1);
  await t("claim admin reads a report", getDoc(doc(claim, "ummah_reports/r1")), 1);
  await t("reporter still cannot read reports", getDoc(doc(carol, "ummah_reports/r1")), 0);
  await t("user still files a report with new reason", addDoc(collection(bob, "ummah_reports"), {reporterUid: "bob", targetType: "post", targetId: "p1", reason: "Sectarian hate / takfir", status: "open"}), 1);
  await t("cannot file report as someone else", addDoc(collection(bob, "ummah_reports"), {reporterUid: "carol", reason: "Scam"}), 0);
  await t("moderator cannot edit report from client", updateDoc(doc(mod, "ummah_reports/r1"), {status: "resolved"}), 0);
  await t("moderator cannot delete report from client", deleteDoc(doc(mod, "ummah_reports/r1")), 0);

  // audit + strikes
  await t("moderator reads audit", getDocs(collection(mod, "moderation_audit")), 1);
  await t("user cannot read audit", getDocs(collection(bob, "moderation_audit")), 0);
  await t("nobody writes audit from client", addDoc(collection(mod, "moderation_audit"), {x: 1}), 0);
  await t("user reads own strikes", getDoc(doc(bob, "user_strikes/bob")), 1);
  await t("user cannot read others' strikes", getDoc(doc(carol, "user_strikes/bob")), 0);
  await t("moderator reads strikes", getDoc(doc(mod, "user_strikes/bob")), 1);
  await t("user cannot clear own strikes", setDoc(doc(bob, "user_strikes/bob"), {count: 0}), 0);

  // notices
  await t("user reads own notices", getDocs(collection(bob, "moderation_notices/bob/items")), 1);
  await t("others cannot read notices", getDocs(collection(carol, "moderation_notices/bob/items")), 0);
  await t("user marks notice seen", updateDoc(doc(bob, "moderation_notices/bob/items/n1"), {seen: true}), 1);
  await t("user cannot rewrite notice reason", updateDoc(doc(bob, "moderation_notices/bob/items/n1"), {reason: "nothing"}), 0);
  await t("user cannot create notices", addDoc(collection(bob, "moderation_notices/bob/items"), {seen: false}), 0);
  await t("user cannot delete notices", deleteDoc(doc(bob, "moderation_notices/bob/items/n1")), 0);

  // isAdmin() unchanged: an admins/{uid} doc does NOT grant pilgrimage publishing.
  await t("admins-doc moderator cannot publish pilgrimage guide", setDoc(doc(mod, "pilgrimageGuides/g2"), {published: true}), 0);
  await t("claim admin can still publish pilgrimage guide", setDoc(doc(claim, "pilgrimageGuides/g2"), {published: true}), 1);
  await t("published guide still public", getDoc(doc(anon, "pilgrimageGuides/g1")), 1);

  // Private safety settings + public comment preference
  await t("owner writes private safety settings", setDoc(doc(bob, "users/bob/settings/safety"), {gender: "female", messageRequests: "following", hideNonFollowerComments: true, blurFlagged: true}), 1);
  await t("others cannot read private gender", getDoc(doc(carol, "users/bob/settings/safety")), 0);
  await t("owner sets public commentsFrom", setDoc(doc(bob, "ummah_profiles/bob"), {commentsFrom: "followers"}, {merge: true}), 1);

  console.log(R.join("\n"));
  console.log(`halal-guard rules: ${R.length - failed}/${R.length} passed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})();
