// 1.1.6 Ask a Scholar — additive rules tests (Firestore emulator). Merges firestore-ask-scholar.rules into firestore.rules.
// Run: RULES=../../firestore.rules EXTRA=../../firestore-ask-scholar.rules npx firebase emulators:exec --only firestore "node ask-scholar-1.1.6.test.js"
const {initializeTestEnvironment, assertSucceeds, assertFails} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const {doc, setDoc, getDoc, getDocs, query, where, orderBy, collection, serverTimestamp, updateDoc, deleteDoc, writeBatch} = require("firebase/firestore");

function merged() {
  const base = fs.readFileSync(process.env.RULES || path.join(__dirname, "../../firestore.rules"), "utf8");
  const extra = fs.readFileSync(process.env.EXTRA || path.join(__dirname, "../../firestore-ask-scholar.rules"), "utf8");
  const i = base.lastIndexOf("}"); const j = base.lastIndexOf("}", i - 1);
  return base.slice(0, j) + extra + "\n  " + base.slice(j);
}

(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
  const env = await initializeTestEnvironment({projectId: "demo-np", firestore: {rules: merged(), host, port: Number(port)}});
  const Q = "Is it permissible to combine Dhuhr and Asr while travelling for work every week?";
  const A = "When a journey reaches the distance of travel, combining Dhuhr and Asr is a concession (rukhsah). Regular weekly trips still count as travel.";
  await env.withSecurityRulesDisabled(async (c) => {
    const f = c.firestore();
    await setDoc(doc(f, "admins/mod"), {role: "moderator"});
    await setDoc(doc(f, "scholars/shaykh"), {name: "Shaykh Abdullah", title: "Imam, Leicester", verified: true});
    await setDoc(doc(f, "scholars/pending"), {name: "Not yet", title: "", verified: false});
    await setDoc(doc(f, "scholar_questions/open1"), {text: Q, category: "SALAH", anonymous: true, name: "", status: "open", createdAt: new Date()});
    await setDoc(doc(f, "scholar_question_owners/open1"), {uid: "asker", at: new Date()});
    await setDoc(doc(f, "scholar_questions/done1"), {text: Q, category: "SALAH", anonymous: false, name: "Bilal", status: "answered", createdAt: new Date(),
      answer: A, scholarUid: "shaykh", scholarName: "Shaykh Abdullah", scholarTitle: "Imam", answeredAt: new Date()});
    await setDoc(doc(f, "scholar_question_owners/done1"), {uid: "bilal", at: new Date()});
  });
  const db = (u, claims) => env.authenticatedContext(u, claims).firestore();
  const asker = db("asker"), other = db("other"), shaykh = db("shaykh"), pending = db("pending"), mod = db("mod"), admin = db("boss", {admin: true});
  const anon = env.unauthenticatedContext().firestore();
  const R = []; let failed = 0;
  async function t(name, p, ok) {
    try { await (ok ? assertSucceeds(p) : assertFails(p)); R.push("PASS " + name); } catch (e) { R.push("FAIL " + name + " " + e.message); failed++; }
  }
  const D = (d, id) => doc(d, `scholar_questions/${id}`);
  const O = (d, id) => doc(d, `scholar_question_owners/${id}`);
  const ask = (d, uid, id, extra = {}) => { const b = writeBatch(d);
    b.set(D(d, id), {text: Q, category: "FAMILY", anonymous: true, name: "", status: "open", createdAt: serverTimestamp(), ...extra});
    b.set(O(d, id), {uid, at: serverTimestamp()}); return b.commit(); };
  const ans = (extra = {}) => ({status: "answered", answer: A, scholarUid: "shaykh", scholarName: "Shaykh Abdullah", scholarTitle: "Imam, Leicester", answeredAt: serverTimestamp(), ...extra});

  // Scholars directory
  await t("anyone sees scholars", getDoc(doc(anon, "scholars/shaykh")), 1);
  await t("users can't make themselves scholars", setDoc(doc(other, "scholars/other"), {name: "Me", title: "", verified: true}), 0);
  await t("scholar can't edit own verification", updateDoc(doc(pending, "scholars/pending"), {verified: true}), 0);
  await t("admin verifies a scholar", setDoc(doc(admin, "scholars/new"), {name: "Ustadha Maryam", title: "Alimah", verified: true}), 1);

  // Asking
  await t("ask anonymously", ask(asker, "asker", "q1"), 1);
  await t("ask with name", ask(asker, "asker", "q2", {anonymous: false, name: "Yusuf"}), 1);
  await t("anonymous with a name denied", ask(asker, "asker", "q3", {name: "Yusuf"}), 0);
  await t("too-short question denied", ask(asker, "asker", "q4", {text: "Is it ok?"}), 0);
  await t("pre-answered question denied", ask(asker, "asker", "q5", {status: "answered"}), 0);
  await t("question without owner doc denied", setDoc(D(asker, "q6"), {text: Q, category: "FAMILY", anonymous: true, name: "", status: "open", createdAt: serverTimestamp()}), 0);
  await t("owner doc for someone else denied", ask(asker, "other", "q7"), 0);
  await t("signed-out ask denied", ask(anon, "", "q8"), 0);

  // Privacy before answer
  await t("asker reads own open question", getDoc(D(asker, "open1")), 1);
  await t("others can't read open question", getDoc(D(other, "open1")), 0);
  await t("unverified scholar can't read open question", getDoc(D(pending, "open1")), 0);
  await t("verified scholar reads inbox", getDocs(query(collection(shaykh, "scholar_questions"), where("status", "==", "open"))), 1);
  await t("user can't list open questions", getDocs(query(collection(other, "scholar_questions"), where("status", "==", "open"))), 0);
  await t("anyone reads answered library", getDocs(query(collection(anon, "scholar_questions"), where("status", "==", "answered"))), 1);
  await t("anyone reads an answered Q&A", getDoc(D(anon, "done1")), 1);
  await t("asker lists own ownership", getDocs(query(collection(asker, "scholar_question_owners"), where("uid", "==", "asker"))), 1);
  await t("others can't read ownership", getDoc(O(other, "open1")), 0);

  // Answering
  await t("non-scholar answers denied", updateDoc(D(other, "open1"), ans({scholarUid: "other"})), 0);
  await t("unverified scholar answers denied", updateDoc(D(pending, "open1"), ans({scholarUid: "pending", scholarName: "Not yet"})), 0);
  await t("scholar can't impersonate name", updateDoc(D(shaykh, "open1"), ans({scholarName: "Mufti Big Name"})), 0);
  await t("scholar can't rewrite the question", updateDoc(D(shaykh, "open1"), ans({text: "changed question text that is long enough"})), 0);
  await t("one-line answer denied", updateDoc(D(shaykh, "open1"), ans({answer: "Yes."})), 0);
  await t("verified scholar answers", updateDoc(D(shaykh, "open1"), ans()), 1);
  await t("scholar refines own answer", updateDoc(D(shaykh, "open1"), ans({answer: A + " And Allah knows best."})), 1);
  await t("another scholar can't overwrite", updateDoc(D(db("new"), "open1"), ans({scholarUid: "new", scholarName: "Ustadha Maryam", scholarTitle: "Alimah"})), 0);
  await t("asker can't edit the answer", updateDoc(D(asker, "open1"), {answer: A + " (edited)"}), 0);

  // Withdraw & moderation
  await t("asker can't delete once answered", deleteDoc(D(asker, "open1")), 0);
  await t("asker withdraws open question", (() => { const b = writeBatch(asker); b.delete(D(asker, "q1")); b.delete(O(asker, "q1")); return b.commit(); })(), 1);
  await t("others can't delete", deleteDoc(D(other, "q2")), 0);
  await t("moderator removes answered Q&A", deleteDoc(D(mod, "done1")), 1);

  console.log(R.join("\n"));
  console.log(`${R.length - failed}/${R.length} passed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})();
