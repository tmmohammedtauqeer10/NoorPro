// 1.1.6 Dua requests — additive rules tests (Firestore emulator). Merges firestore-dua-requests.rules into firestore.rules.
// Run: RULES=../../firestore.rules EXTRA=../../firestore-dua-requests.rules npx firebase emulators:exec --only firestore "node dua-requests-1.1.6.test.js"
const {initializeTestEnvironment, assertSucceeds, assertFails} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const {doc, setDoc, getDoc, getDocs, collection, query, where, serverTimestamp, updateDoc, deleteDoc, increment, writeBatch} = require("firebase/firestore");

function merged() {
  const base = fs.readFileSync(process.env.RULES || path.join(__dirname, "../../firestore.rules"), "utf8");
  const extra = fs.readFileSync(process.env.EXTRA || path.join(__dirname, "../../firestore-dua-requests.rules"), "utf8");
  const i = base.lastIndexOf("}"); const j = base.lastIndexOf("}", i - 1);
  return base.slice(0, j) + extra + "\n  " + base.slice(j);
}

(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
  const env = await initializeTestEnvironment({projectId: "demo-np", firestore: {rules: merged(), host, port: Number(port)}});
  const dua = (extra = {}) => ({text: "Please make dua for my mother's surgery tomorrow", category: "HEALTH", anonymous: false, name: "Aisha",
    authorUid: "aisha", ameenCount: 0, prayedCount: 0, answered: false, createdAt: new Date(), ...extra});
  await env.withSecurityRulesDisabled(async (c) => {
    const f = c.firestore();
    await setDoc(doc(f, "admins/mod"), {role: "moderator"});
    await setDoc(doc(f, "dua_requests/d1"), dua());
    await setDoc(doc(f, "dua_owners/d1"), {uid: "aisha", at: new Date()});
    await setDoc(doc(f, "dua_requests/d2"), dua({anonymous: true, name: "", authorUid: "", ameenCount: 1}));
    await setDoc(doc(f, "dua_owners/d2"), {uid: "omar", at: new Date()});
    await setDoc(doc(f, "dua_requests/d2/responses/bilal_ameen"), {uid: "bilal", kind: "ameen", at: new Date()});
  });
  const mod = env.authenticatedContext("mod").firestore();
  const aisha = env.authenticatedContext("aisha").firestore();
  const omar = env.authenticatedContext("omar").firestore();
  const bilal = env.authenticatedContext("bilal").firestore();
  const anon = env.unauthenticatedContext().firestore();
  const R = []; let failed = 0;
  async function t(name, p, ok) {
    try { await (ok ? assertSucceeds(p) : assertFails(p)); R.push("PASS " + name); } catch (e) { R.push("FAIL " + name + " " + e.message); failed++; }
  }
  const D = (db, id) => doc(db, `dua_requests/${id}`);
  const O = (db, id) => doc(db, `dua_owners/${id}`);
  const Rs = (db, id, rid) => doc(db, `dua_requests/${id}/responses/${rid}`);
  const fresh = (uid, extra = {}) => ({...dua({name: "Omar", authorUid: uid}), createdAt: serverTimestamp(), ...extra});
  function post(db, id, data, ownerUid) {
    const b = writeBatch(db); b.set(D(db, id), data); b.set(O(db, id), {uid: ownerUid, at: serverTimestamp()}); return b.commit();
  }

  // Reading
  await t("anyone reads requests", getDoc(D(anon, "d1")), 1);
  await t("anonymous request has no uid", getDoc(D(bilal, "d2")).then(s => { if (s.data().authorUid !== "") throw new Error("leak"); }), 1);
  await t("owner reads own ownership", getDoc(O(omar, "d2")), 1);
  await t("others can't read ownership", getDoc(O(bilal, "d2")), 0);
  await t("moderator reads ownership", getDoc(O(mod, "d2")), 1);
  await t("owner queries own ids", getDocs(query(collection(omar, "dua_owners"), where("uid", "==", "omar"))), 1);
  await t("query others' ids denied", getDocs(query(collection(bilal, "dua_owners"), where("uid", "==", "omar"))), 0);

  // Posting
  await t("post named request", post(omar, "n1", fresh("omar"), "omar"), 1);
  await t("post anonymous request", post(omar, "n2", fresh("omar", {anonymous: true, name: "", authorUid: ""}), "omar"), 1);
  await t("anonymous with uid denied", post(omar, "n3", fresh("omar", {anonymous: true, name: ""}), "omar"), 0);
  await t("post as someone else denied", post(omar, "n4", fresh("aisha"), "omar"), 0);
  await t("ownership for someone else denied", post(omar, "n5", fresh("omar"), "aisha"), 0);
  await t("request without ownership denied", setDoc(D(omar, "n6"), fresh("omar")), 0);
  await t("pre-counted request denied", post(omar, "n7", fresh("omar", {ameenCount: 50}), "omar"), 0);
  await t("too short denied", post(omar, "n8", fresh("omar", {text: "pray"}), "omar"), 0);
  await t("unknown category denied", post(omar, "n9", fresh("omar", {category: "MONEY"}), "omar"), 0);
  await t("signed-out post denied", post(anon, "n10", fresh(""), ""), 0);
  await t("claim existing request denied", setDoc(O(bilal, "d1"), {uid: "bilal", at: serverTimestamp()}), 0);

  // Ameen / prayed
  function respond(db, uid, id, kind, on) {
    const b = writeBatch(db); const rid = `${uid}_${kind}`;
    if (on) b.set(Rs(db, id, rid), {uid, kind, at: serverTimestamp()}); else b.delete(Rs(db, id, rid));
    b.update(D(db, id), {[kind === "ameen" ? "ameenCount" : "prayedCount"]: increment(on ? 1 : -1)}); return b.commit();
  }
  await t("say Ameen", respond(bilal, "bilal", "d1", "ameen", true), 1);
  await t("say Ameen twice denied", respond(bilal, "bilal", "d1", "ameen", true), 0);
  await t("I prayed for you", respond(bilal, "bilal", "d1", "prayed", true), 1);
  await t("undo Ameen", respond(bilal, "bilal", "d1", "ameen", false), 1);
  await t("bump count without response denied", updateDoc(D(bilal, "d1"), {ameenCount: increment(5)}), 0);
  await t("respond as someone else denied", setDoc(Rs(bilal, "d1", "omar_ameen"), {uid: "omar", kind: "ameen", at: serverTimestamp()}), 0);
  await t("edit text via counter denied", (() => { const b = writeBatch(omar); b.set(Rs(omar, "d1", "omar_ameen"), {uid: "omar", kind: "ameen", at: serverTimestamp()});
    b.update(D(omar, "d1"), {ameenCount: increment(1), text: "changed text here"}); return b.commit(); })(), 0);
  await t("read own response", getDoc(Rs(bilal, "d1", "bilal_prayed")), 1);
  await t("read others' response denied", getDoc(Rs(omar, "d1", "bilal_prayed")), 0);

  // Owner actions
  await t("owner marks answered", updateDoc(D(aisha, "d1"), {answered: true}), 1);
  await t("anonymous owner marks answered", updateDoc(D(omar, "d2"), {answered: true}), 1);
  await t("stranger marks answered denied", updateDoc(D(bilal, "d2"), {answered: true}), 0);
  await t("owner edits text denied", updateDoc(D(aisha, "d1"), {text: "Different words entirely"}), 0);
  await t("stranger deletes denied", deleteDoc(D(bilal, "d1")), 0);
  await t("moderator deletes", deleteDoc(D(mod, "n1")), 1);
  await t("owner deletes request + ownership", (() => { const b = writeBatch(omar); b.delete(D(omar, "d2")); b.delete(O(omar, "d2")); return b.commit(); })(), 1);

  // Reports use the existing pipeline
  await t("report a request", setDoc(doc(bilal, "ummah_reports/r1"), {postId: "d1", targetType: "dua_request", targetId: "d1", targetUid: "", reason: "Spam or scam", details: "", reporterUid: "bilal", status: "open", createdAt: serverTimestamp()}), 1);

  console.log(R.join("\n"));
  console.log(`${R.length - failed}/${R.length} passed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})();
