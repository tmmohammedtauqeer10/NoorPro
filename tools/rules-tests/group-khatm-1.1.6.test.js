// 1.1.6 Group Khatm — additive rules tests (Firestore emulator). Merges firestore-group-khatm.rules into firestore.rules.
// Run: RULES=../../firestore.rules EXTRA=../../firestore-group-khatm.rules npx firebase emulators:exec --only firestore "node group-khatm-1.1.6.test.js"
const {initializeTestEnvironment, assertSucceeds, assertFails} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const {doc, setDoc, getDoc, getDocs, collection, query, where, serverTimestamp, updateDoc, deleteDoc, arrayUnion, arrayRemove} = require("firebase/firestore");

function merged() {
  const base = fs.readFileSync(process.env.RULES || path.join(__dirname, "../../firestore.rules"), "utf8");
  const extra = fs.readFileSync(process.env.EXTRA || path.join(__dirname, "../../firestore-group-khatm.rules"), "utf8");
  const i = base.lastIndexOf("}"); const j = base.lastIndexOf("}", i - 1);  // before the documents block closes
  return base.slice(0, j) + extra + "\n  " + base.slice(j);
}

(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
  const env = await initializeTestEnvironment({projectId: "demo-np", firestore: {rules: merged(), host, port: Number(port)}});
  await env.withSecurityRulesDisabled(async (c) => {
    const f = c.firestore();
    await setDoc(doc(f, "group_khatms/K7M2QP"), {title: "Family", dedication: "", creatorUid: "alice", creatorName: "Alice", memberUids: ["alice", "bob"], createdAt: new Date()});
    await setDoc(doc(f, "group_khatms/K7M2QP/paras/1"), {uid: "bob", name: "Bob", status: "claimed", at: new Date()});
    await setDoc(doc(f, "group_khatms/FULL22"), {title: "Full", dedication: "", creatorUid: "zed", creatorName: "Z", memberUids: Array.from({length: 100}, (_, i) => "m" + i), createdAt: new Date()});
  });
  const alice = env.authenticatedContext("alice").firestore();
  const bob = env.authenticatedContext("bob").firestore();
  const carol = env.authenticatedContext("carol").firestore();
  const anon = env.unauthenticatedContext().firestore();
  const R = []; let failed = 0;
  async function t(name, p, ok) {
    try { await (ok ? assertSucceeds(p) : assertFails(p)); R.push("PASS " + name); } catch (e) { R.push("FAIL " + name + " " + e.message); failed++; }
  }
  const G = (db, c = "K7M2QP") => doc(db, `group_khatms/${c}`);
  const P = (db, n, c = "K7M2QP") => doc(db, `group_khatms/${c}/paras/${n}`);
  const good = (uid) => ({title: "Ramadan", dedication: "For Nani", creatorUid: uid, creatorName: "C", memberUids: [uid], createdAt: serverTimestamp()});
  const claim = (uid, status = "claimed") => ({uid, name: uid, status, at: serverTimestamp()});

  // Create
  await t("create valid", setDoc(G(carol, "AB3K9Z"), good("carol")), 1);
  await t("create anon denied", setDoc(G(anon, "AB3K9Y"), good("x")), 0);
  await t("create bad code denied", setDoc(G(carol, "AB0K9Z"), good("carol")), 0);
  await t("create lowercase code denied", setDoc(G(carol, "ab3k9z"), good("carol")), 0);
  await t("create for someone else denied", setDoc(G(carol, "AB3K9X"), good("dave")), 0);
  await t("create with extra members denied", setDoc(G(carol, "AB3K9W"), {...good("carol"), memberUids: ["carol", "dave"]}), 0);
  await t("create client time denied", setDoc(G(carol, "AB3K9V"), {...good("carol"), createdAt: new Date(0)}), 0);
  await t("create extra field denied", setDoc(G(carol, "AB3K9U"), {...good("carol"), admin: true}), 0);
  await t("create empty title denied", setDoc(G(carol, "AB3K9T"), {...good("carol"), title: ""}), 0);
  await t("overwrite existing denied", setDoc(G(carol, "K7M2QP"), good("carol")), 0);
  // Read
  await t("get by code signed in", getDoc(G(carol)), 1);
  await t("get anon denied", getDoc(G(anon)), 0);
  await t("list mine", getDocs(query(collection(bob, "group_khatms"), where("memberUids", "array-contains", "bob"))), 1);
  await t("list all denied", getDocs(collection(bob, "group_khatms")), 0);
  // Paras before joining
  await t("non-member reads paras denied", getDocs(collection(carol, "group_khatms/K7M2QP/paras")), 0);
  await t("non-member claim denied", setDoc(P(carol, 5), claim("carol")), 0);
  // Join
  await t("join adds self", updateDoc(G(carol), {memberUids: arrayUnion("carol")}), 1);
  await t("join adding others denied", updateDoc(G(env.authenticatedContext("dave").firestore()), {memberUids: ["alice", "bob", "carol", "dave", "eve"]}), 0);
  await t("join full khatm denied", updateDoc(G(carol, "FULL22"), {memberUids: arrayUnion("carol")}), 0);
  await t("join + title change denied", updateDoc(G(env.authenticatedContext("dave").firestore()), {memberUids: arrayUnion("dave"), title: "x"}), 0);
  await t("member removes another denied", updateDoc(G(carol), {memberUids: arrayRemove("bob")}), 0);
  // Claims
  await t("member reads paras", getDocs(collection(carol, "group_khatms/K7M2QP/paras")), 1);
  await t("claim open para", setDoc(P(carol, 5), claim("carol")), 1);
  await t("claim taken para denied", setDoc(P(carol, 1), claim("carol")), 0);
  await t("claim para 31 denied", setDoc(P(carol, 31), claim("carol")), 0);
  await t("claim para 0 denied", setDoc(P(carol, 0), claim("carol")), 0);
  await t("claim para 05 denied", setDoc(P(carol, "05"), claim("carol")), 0);
  await t("claim as someone else denied", setDoc(P(carol, 6), claim("bob")), 0);
  await t("claim bad status denied", setDoc(P(carol, 7), claim("carol", "skipped")), 0);
  await t("claim already done ok", setDoc(P(carol, 30), claim("carol", "done")), 1);
  await t("mark own done", updateDoc(P(carol, 5), {status: "done", at: serverTimestamp()}), 1);
  await t("mark other's done denied", updateDoc(P(carol, 1), {status: "done", at: serverTimestamp()}), 0);
  await t("steal para via update denied", updateDoc(P(carol, 1), {uid: "carol"}), 0);
  await t("release other's para denied", deleteDoc(P(carol, 1)), 0);
  await t("organiser frees abandoned para", deleteDoc(P(alice, 1)), 1);
  await t("release own para", deleteDoc(P(carol, 30)), 1);
  // Organiser + leave
  await t("organiser edits title", updateDoc(G(alice), {title: "Family Khatm", dedication: "For Nani"}), 1);
  await t("member edits title denied", updateDoc(G(bob), {title: "x"}), 0);
  await t("organiser edits members directly denied", updateDoc(G(alice), {title: "y", memberUids: ["alice"]}), 0);
  await t("member leaves", updateDoc(G(bob), {memberUids: arrayRemove("bob")}), 1);
  await t("organiser can't leave", updateDoc(G(alice), {memberUids: arrayRemove("alice")}), 0);
  await t("member delete khatm denied", deleteDoc(G(carol)), 0);
  await t("organiser deletes khatm", deleteDoc(G(alice)), 1);

  console.log(R.join("\n"));
  console.log(`\n${R.length - failed}/${R.length} passed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})();
