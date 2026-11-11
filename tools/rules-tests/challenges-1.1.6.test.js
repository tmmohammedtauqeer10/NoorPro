// 1.1.6 Good-deed challenges — additive rules tests (Firestore emulator). Merges firestore-challenges.rules into firestore.rules.
// Run: RULES=../../firestore.rules EXTRA=../../firestore-challenges.rules npx firebase emulators:exec --only firestore "node challenges-1.1.6.test.js"
const {initializeTestEnvironment, assertSucceeds, assertFails} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const {doc, setDoc, getDoc, getDocs, query, where, collection, serverTimestamp, updateDoc, deleteDoc, arrayUnion, arrayRemove} = require("firebase/firestore");

function merged() {
  const base = fs.readFileSync(process.env.RULES || path.join(__dirname, "../../firestore.rules"), "utf8");
  const extra = fs.readFileSync(process.env.EXTRA || path.join(__dirname, "../../firestore-challenges.rules"), "utf8");
  const i = base.lastIndexOf("}"); const j = base.lastIndexOf("}", i - 1);
  return base.slice(0, j) + extra + "\n  " + base.slice(j);
}

(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
  const env = await initializeTestEnvironment({projectId: "demo-np", firestore: {rules: merged(), host, port: Number(port)}});
  const base = {title: "A page a day", kind: "QURAN", goal: "Read at least one page", days: 21, startDay: "2026-10-28", creatorName: "Aisha"};
  await env.withSecurityRulesDisabled(async (c) => {
    const f = c.firestore();
    await setDoc(doc(f, "admins/mod"), {role: "moderator"});
    await setDoc(doc(f, "challenges/ABCD2345"), {...base, createdBy: "aisha", members: ["aisha", "bilal"], createdAt: new Date()});
    await setDoc(doc(f, "challenges/ABCD2345/progress/bilal"), {name: "Bilal", done: ["2026-10-28"], updatedAt: new Date()});
    await setDoc(doc(f, "challenges/FULLFULL"), {...base, createdBy: "aisha", members: Array.from({length: 30}, (_, i) => "m" + i), createdAt: new Date()});
  });
  const db = (u) => env.authenticatedContext(u).firestore();
  const aisha = db("aisha"), bilal = db("bilal"), zara = db("zara"), omar = db("omar"), mod = db("mod");
  const anon = env.unauthenticatedContext().firestore();
  const R = []; let failed = 0;
  async function t(name, p, ok) {
    try { await (ok ? assertSucceeds(p) : assertFails(p)); R.push("PASS " + name); } catch (e) { R.push("FAIL " + name + " " + e.message); failed++; }
  }
  const C = (d, id) => doc(d, `challenges/${id}`);
  const P = (d, id, u) => doc(d, `challenges/${id}/progress/${u}`);
  const mk = (d, uid, id, extra = {}) => setDoc(C(d, id), {...base, createdBy: uid, members: [uid], createdAt: serverTimestamp(), ...extra});

  // Create
  await t("create with invite code", mk(zara, "zara", "ZARA2345"), 1);
  await t("code with ambiguous letters denied", mk(zara, "zara", "ZARA0OI1"), 0);
  await t("short code denied", mk(zara, "zara", "ZAR"), 0);
  await t("create for someone else denied", mk(zara, "omar", "ZARA2346"), 0);
  await t("create with friends pre-added denied", mk(zara, "zara", "ZARA2347", {members: ["zara", "omar"]}), 0);
  await t("odd length denied", mk(zara, "zara", "ZARA2348", {days: 9}), 0);
  await t("unknown kind denied", mk(zara, "zara", "ZARA2349", {kind: "CASINO"}), 0);
  await t("long title denied", mk(zara, "zara", "ZARA2352", {title: "x".repeat(61)}), 0);
  await t("bad start day denied", mk(zara, "zara", "ZARA2353", {startDay: "tomorrow"}), 0);
  await t("extra field (score) denied", mk(zara, "zara", "ZARA2354", {score: 100}), 0);
  await t("signed-out create denied", setDoc(C(anon, "ANON2345"), {...base, createdBy: "", members: [""], createdAt: serverTimestamp()}), 0);

  // Read
  await t("anyone signed in can open by code", getDoc(C(omar, "ABCD2345")), 1);
  await t("signed-out can't open", getDoc(C(anon, "ABCD2345")), 0);
  await t("member lists own challenges", getDocs(query(collection(bilal, "challenges"), where("members", "array-contains", "bilal"))), 1);
  await t("can't list everyone's challenges", getDocs(collection(omar, "challenges")), 0);

  // Join / leave
  await t("join appends yourself", updateDoc(C(omar, "ABCD2345"), {members: arrayUnion("omar")}), 1);
  await t("can't add a friend for them", updateDoc(C(zara, "ABCD2345"), {members: arrayUnion("someone")}), 0);
  await t("can't join a full challenge", updateDoc(C(omar, "FULLFULL"), {members: arrayUnion("omar")}), 0);
  await t("joining can't change the title", updateDoc(C(zara, "ABCD2345"), {members: arrayUnion("zara"), title: "Mine now"}), 0);
  await t("member can't rename", updateDoc(C(bilal, "ABCD2345"), {title: "Renamed"}), 0);
  await t("member can't remove another member", updateDoc(C(bilal, "ABCD2345"), {members: arrayRemove("omar")}), 0);
  await t("member leaves", updateDoc(C(omar, "ABCD2345"), {members: arrayRemove("omar")}), 1);
  await t("creator removes a member", updateDoc(C(aisha, "ABCD2345"), {members: ["aisha"]}), 1);
  await t("creator can't remove themselves", updateDoc(C(aisha, "ZARA2345"), {members: []}), 0);
  await t("rejoin after removal", updateDoc(C(bilal, "ABCD2345"), {members: arrayUnion("bilal")}), 1);

  // Progress
  await t("member checks in", setDoc(P(bilal, "ABCD2345", "bilal"), {name: "Bilal", done: ["2026-10-28", "2026-10-29"], updatedAt: serverTimestamp()}), 1);
  await t("can't check in for someone else", setDoc(P(bilal, "ABCD2345", "aisha"), {name: "Aisha", done: ["2026-10-28"], updatedAt: serverTimestamp()}), 0);
  await t("non-member can't check in", setDoc(P(omar, "ABCD2345", "omar"), {name: "Omar", done: [], updatedAt: serverTimestamp()}), 0);
  await t("no points field", setDoc(P(bilal, "ABCD2345", "bilal"), {name: "Bilal", done: [], points: 9, updatedAt: serverTimestamp()}), 0);
  await t("done list capped", setDoc(P(bilal, "ABCD2345", "bilal"), {name: "Bilal", done: Array.from({length: 41}, (_, i) => "d" + i), updatedAt: serverTimestamp()}), 0);
  await t("members see the group", getDocs(collection(aisha, "challenges/ABCD2345/progress")), 1);
  await t("outsiders can't see progress", getDocs(collection(omar, "challenges/ABCD2345/progress")), 0);
  await t("member clears own progress", deleteDoc(P(bilal, "ABCD2345", "bilal")), 1);

  // Delete
  await t("member can't delete the challenge", deleteDoc(C(bilal, "ABCD2345")), 0);
  await t("creator deletes", deleteDoc(C(zara, "ZARA2345")), 1);
  await t("moderator removes", deleteDoc(C(mod, "FULLFULL")), 1);

  console.log(R.join("\n"));
  console.log(`${R.length - failed}/${R.length} passed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})();
