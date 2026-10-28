// 1.1.6 Masjid pages — additive rules tests (Firestore emulator). Merges firestore-masjid-pages.rules into firestore.rules.
// Run: RULES=../../firestore.rules EXTRA=../../firestore-masjid-pages.rules npx firebase emulators:exec --only firestore "node masjid-pages-1.1.6.test.js"
const {initializeTestEnvironment, assertSucceeds, assertFails} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const {doc, setDoc, getDoc, getDocs, addDoc, collection, query, where, serverTimestamp, updateDoc, deleteDoc, arrayUnion, writeBatch} = require("firebase/firestore");

function merged() {
  const base = fs.readFileSync(process.env.RULES || path.join(__dirname, "../../firestore.rules"), "utf8");
  const extra = fs.readFileSync(process.env.EXTRA || path.join(__dirname, "../../firestore-masjid-pages.rules"), "utf8");
  const i = base.lastIndexOf("}"); const j = base.lastIndexOf("}", i - 1);
  return base.slice(0, j) + extra + "\n  " + base.slice(j);
}

(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
  const env = await initializeTestEnvironment({projectId: "demo-np", firestore: {rules: merged(), host, port: Number(port)}});
  const listing = (extra = {}) => ({name: "Masjid-e-Noor", address: "12 Cubbon Road", city: "Bengaluru", lat: 12.97, lng: 77.6,
    jamaat: {Fajr: "05:15", Dhuhr: "13:30", Asr: "16:45", Maghrib: "18:22", Isha: "20:00"}, jummahTime: "13:15", jummahBayan: "", adminUids: ["imam"], ...extra});
  await env.withSecurityRulesDisabled(async (c) => {
    const f = c.firestore();
    await setDoc(doc(f, "admins/mod"), {role: "moderator"});
    await setDoc(doc(f, "masjids/noor"), listing());
    await setDoc(doc(f, "masjids/rahma"), listing({name: "Masjid Ar-Rahma", adminUids: []}));
    await setDoc(doc(f, "masjids/noor/announcements/a1"), {text: "Tafseer Thursday", by: "imam", at: new Date()});
    await setDoc(doc(f, "masjid_claims/rahma_bob"), {masjidId: "rahma", masjidName: "Masjid Ar-Rahma", uid: "bob", name: "Bob", role: "Imam", contact: "+91 98450", note: "", status: "pending", at: new Date()});
    await setDoc(doc(f, "masjid_claims/noor_dan"), {masjidId: "noor", masjidName: "Masjid-e-Noor", uid: "dan", name: "Dan", role: "Imam", contact: "+91 98450", note: "", status: "rejected", at: new Date()});
  });
  const mod = env.authenticatedContext("mod").firestore();
  const imam = env.authenticatedContext("imam").firestore();
  const bob = env.authenticatedContext("bob").firestore();
  const carol = env.authenticatedContext("carol").firestore();
  const dan = env.authenticatedContext("dan").firestore();
  const anon = env.unauthenticatedContext().firestore();
  const R = []; let failed = 0;
  async function t(name, p, ok) {
    try { await (ok ? assertSucceeds(p) : assertFails(p)); R.push("PASS " + name); } catch (e) { R.push("FAIL " + name + " " + e.message); failed++; }
  }
  const M = (db, id = "noor") => doc(db, `masjids/${id}`);
  const claim = (uid, masjid = "rahma", extra = {}) => ({masjidId: masjid, masjidName: "Masjid", uid, name: "Carol Ahmed", role: "Committee member", contact: "carol@example.org", note: "", status: "pending", at: serverTimestamp(), ...extra});

  // Public reads
  await t("anon reads masjid", getDoc(M(anon)), 1);
  await t("anon lists masjids", getDocs(collection(anon, "masjids")), 1);
  await t("anon reads announcements", getDocs(collection(anon, "masjids/noor/announcements")), 1);
  // Listings
  await t("user creates masjid denied", setDoc(M(carol, "new1"), listing({adminUids: []})), 0);
  await t("moderator creates masjid", setDoc(M(mod, "new2"), listing({adminUids: []})), 1);
  await t("moderator bad time denied", setDoc(M(mod, "new3"), listing({jamaat: {Fajr: "5:15"}})), 0);
  await t("moderator unknown prayer key denied", setDoc(M(mod, "new4"), listing({jamaat: {Tahajjud: "03:00"}})), 0);
  await t("moderator extra field denied", setDoc(M(mod, "new5"), listing({verified: true})), 0);
  await t("moderator deletes", deleteDoc(M(mod, "new2")), 1);
  // Manager updates
  await t("imam updates times", updateDoc(M(imam), {jamaat: {Fajr: "05:20", Isha: "20:15"}, jummahTime: "13:30", jummahBayan: "Patience", updatedAt: serverTimestamp()}), 1);
  await t("imam client time denied", updateDoc(M(imam), {jummahBayan: "x", updatedAt: new Date(0)}), 0);
  await t("imam bad time denied", updateDoc(M(imam), {jummahTime: "25:00", updatedAt: serverTimestamp()}), 0);
  await t("imam renames denied", updateDoc(M(imam), {name: "Mine", updatedAt: serverTimestamp()}), 0);
  await t("imam adds admin denied", updateDoc(M(imam), {adminUids: ["imam", "carol"], updatedAt: serverTimestamp()}), 0);
  await t("imam long bayan denied", updateDoc(M(imam), {jummahBayan: "x".repeat(141), updatedAt: serverTimestamp()}), 0);
  await t("non-manager updates denied", updateDoc(M(carol), {jummahBayan: "x", updatedAt: serverTimestamp()}), 0);
  await t("imam of other masjid denied", updateDoc(M(imam, "rahma"), {jummahBayan: "x", updatedAt: serverTimestamp()}), 0);
  await t("imam deletes masjid denied", deleteDoc(M(imam)), 0);
  // Announcements
  const A = (db, id = "noor") => collection(db, `masjids/${id}/announcements`);
  await t("imam posts", addDoc(A(imam), {text: "Eid salah 7:30 AM", by: "imam", at: serverTimestamp()}), 1);
  await t("imam posts as other denied", addDoc(A(imam), {text: "x", by: "carol", at: serverTimestamp()}), 0);
  await t("imam empty post denied", addDoc(A(imam), {text: "", by: "imam", at: serverTimestamp()}), 0);
  await t("user posts denied", addDoc(A(carol), {text: "spam", by: "carol", at: serverTimestamp()}), 0);
  await t("imam posts elsewhere denied", addDoc(A(imam, "rahma"), {text: "x", by: "imam", at: serverTimestamp()}), 0);
  await t("moderator posts", addDoc(A(mod, "rahma"), {text: "Listing verified", by: "mod", at: serverTimestamp()}), 1);
  await t("edit announcement denied", updateDoc(doc(imam, "masjids/noor/announcements/a1"), {text: "edited"}), 0);
  await t("user deletes announcement denied", deleteDoc(doc(carol, "masjids/noor/announcements/a1")), 0);
  await t("imam deletes announcement", deleteDoc(doc(imam, "masjids/noor/announcements/a1")), 1);
  // Claims
  const C = (db, id) => doc(db, `masjid_claims/${id}`);
  await t("claim valid", setDoc(C(carol, "rahma_carol"), claim("carol")), 1);
  await t("claim anon denied", setDoc(C(anon, "rahma_x"), claim("x")), 0);
  await t("claim for someone else denied", setDoc(C(carol, "rahma_eve"), claim("eve")), 0);
  await t("claim wrong id denied", setDoc(C(carol, "other_carol"), claim("carol")), 0);
  await t("claim missing masjid denied", setDoc(C(carol, "ghost_carol"), claim("carol", "ghost")), 0);
  await t("claim pre-approved denied", setDoc(C(carol, "noor_carol"), claim("carol", "noor", {status: "approved"})), 0);
  await t("claim bad role denied", setDoc(C(carol, "noor_carol"), claim("carol", "noor", {role: "Owner"})), 0);
  await t("claim short contact denied", setDoc(C(carol, "noor_carol"), claim("carol", "noor", {contact: "1"})), 0);
  await t("claim client time denied", setDoc(C(carol, "noor_carol"), claim("carol", "noor", {at: new Date(0)})), 0);
  await t("re-claim while pending denied", setDoc(C(carol, "rahma_carol"), claim("carol")), 0);
  await t("re-apply after decline", setDoc(C(dan, "noor_dan"), claim("dan", "noor")), 1);
  await t("read own claim", getDoc(C(carol, "rahma_carol")), 1);
  await t("list own claims", getDocs(query(collection(carol, "masjid_claims"), where("uid", "==", "carol"))), 1);
  await t("read others' claim denied", getDoc(C(carol, "rahma_bob")), 0);
  await t("list all claims denied", getDocs(collection(carol, "masjid_claims")), 0);
  await t("moderator lists pending", getDocs(query(collection(mod, "masjid_claims"), where("status", "==", "pending"))), 1);
  await t("self-approve denied", updateDoc(C(bob, "rahma_bob"), {status: "approved", decidedBy: "bob", decidedAt: serverTimestamp()}), 0);
  await t("moderator edits claim content denied", updateDoc(C(mod, "rahma_bob"), {status: "approved", name: "x", decidedBy: "mod", decidedAt: serverTimestamp()}), 0);
  const b = writeBatch(mod);
  b.update(C(mod, "rahma_bob"), {status: "approved", decidedBy: "mod", decidedAt: serverTimestamp()});
  b.update(M(mod, "rahma"), {adminUids: arrayUnion("bob")});
  await t("moderator approves (batch)", b.commit(), 1);
  await t("approved imam now updates", updateDoc(M(bob, "rahma"), {jummahBayan: "Gratitude", updatedAt: serverTimestamp()}), 1);
  await t("withdraw own pending", deleteDoc(C(carol, "rahma_carol")), 1);
  await t("delete decided claim denied", deleteDoc(C(bob, "rahma_bob")), 0);
  // Follows (covered by the base users/{uid}/** owner rule)
  const F = (db, uid, id = "noor") => doc(db, `users/${uid}/followed_masjids/${id}`);
  await t("follow", setDoc(F(carol, "carol"), {name: "Masjid-e-Noor", at: serverTimestamp()}), 1);
  await t("follow for other denied", setDoc(F(carol, "bob"), {name: "x", at: serverTimestamp()}), 0);
  await t("read own follows", getDocs(collection(carol, "users/carol/followed_masjids")), 1);
  await t("read others' follows denied", getDocs(collection(bob, "users/carol/followed_masjids")), 0);
  await t("unfollow", deleteDoc(F(carol, "carol")), 1);

  console.log(R.join("\n"));
  console.log(`${R.length - failed}/${R.length} passed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})();
