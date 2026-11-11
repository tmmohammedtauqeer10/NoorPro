// 1.2.0 (115) onboarding usernames + scholar applications — rules tests (Firestore + Storage emulators).
// The rules are already merged into firestore.rules / storage.rules; EXTRA (optional) appends another file.
// Run: npx firebase emulators:exec --only firestore,storage "node onboarding-scholars-1.2.0.test.js"
const {initializeTestEnvironment, assertSucceeds, assertFails} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const {doc, setDoc, getDoc, getDocs, query, where, collection, serverTimestamp, updateDoc, deleteDoc, writeBatch, runTransaction} = require("firebase/firestore");
const {ref, uploadBytes, getBytes, deleteObject} = require("firebase/storage");

function merged() {
  const base = fs.readFileSync(process.env.RULES || path.join(__dirname, "../../firestore.rules"), "utf8");
  if (!process.env.EXTRA) return base;
  const extra = fs.readFileSync(process.env.EXTRA, "utf8");
  const i = base.lastIndexOf("}"); const j = base.lastIndexOf("}", i - 1);
  return base.slice(0, j) + extra + "\n  " + base.slice(j);
}

(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
  const sHost = process.env.FIREBASE_STORAGE_EMULATOR_HOST;
  const cfg = {projectId: "demo-np", firestore: {rules: merged(), host, port: Number(port)}};
  if (sHost) { const [h, p] = sHost.split(":"); cfg.storage = {rules: fs.readFileSync(path.join(__dirname, "../../storage.rules"), "utf8"), host: h, port: Number(p)}; }
  const env = await initializeTestEnvironment(cfg);
  const BIO = "Studied the Dars-e-Nizami for eight years and teach fiqh and hadith at our madrasa.";
  const app = (uid, extra = {}) => ({uid, name: "Aisha Siddiqa", title: "Alimah", institution: "Jamia Darul Uloom", maslak: "hanafi", silsila: "",
    years: 6, refName: "Mufti Yusuf", refPhone: "+91 98765 43210", bio: BIO, certPath: `scholar_certificates/${uid}/ijazah_1.jpg`, status: "pending",
    submittedAt: serverTimestamp(), ...extra});
  await env.withSecurityRulesDisabled(async (c) => {
    const f = c.firestore();
    await setDoc(doc(f, "admins/mod"), {role: "moderator"});
    await setDoc(doc(f, "usernames/taken.name"), {uid: "bob", at: new Date()});
    await setDoc(doc(f, "ummah_profiles/alice"), {name: "Alice", handle: "alice"});
    await setDoc(doc(f, "ummah_profiles/vip"), {name: "Vip", handle: "vip", verified: true});
    await setDoc(doc(f, "users/alice/private/profile"), {fullName: "Alice", dob: "2000-01-01"});
    await setDoc(doc(f, "scholar_applications/rej"), {...app("rej"), status: "rejected", reason: "Blurry photo", submittedAt: new Date()});
    await setDoc(doc(f, "scholar_applications/pend"), {...app("pend"), submittedAt: new Date()});
  });
  const db = (u, claims) => env.authenticatedContext(u, claims).firestore();
  const alice = db("alice"), bob = db("bob"), mod = db("mod"), admin = db("boss", {admin: true}), pend = db("pend"), rej = db("rej"), vip = db("vip");
  const anon = env.unauthenticatedContext().firestore();
  const R = []; let failed = 0;
  async function t(name, p, ok) {
    try { await (ok ? assertSucceeds(p) : assertFails(p)); R.push("PASS " + name); } catch (e) { R.push("FAIL " + name + " " + e.message); failed++; }
  }
  const U = (d, n) => doc(d, `usernames/${n}`);

  // Usernames
  await t("anyone can check a username", getDoc(U(anon, "taken.name")), 1);
  await t("reserve a free username", setDoc(U(alice, "alice.k"), {uid: "alice", at: serverTimestamp()}), 1);
  await t("can't take someone else's username", setDoc(U(alice, "taken.name"), {uid: "alice", at: serverTimestamp()}), 0);
  await t("can't reserve for another uid", setDoc(U(alice, "free1"), {uid: "bob", at: serverTimestamp()}), 0);
  await t("uppercase username denied", setDoc(U(alice, "Alice"), {uid: "alice", at: serverTimestamp()}), 0);
  await t("too short denied", setDoc(U(alice, "ab"), {uid: "alice", at: serverTimestamp()}), 0);
  await t("too long denied", setDoc(U(alice, "a".repeat(21)), {uid: "alice", at: serverTimestamp()}), 0);
  await t("leading dot denied", setDoc(U(alice, ".alice"), {uid: "alice", at: serverTimestamp()}), 0);
  await t("double dot denied", setDoc(U(alice, "al..ice"), {uid: "alice", at: serverTimestamp()}), 0);
  await t("all digits denied", setDoc(U(alice, "12345"), {uid: "alice", at: serverTimestamp()}), 0);
  await t("extra keys denied", setDoc(U(alice, "alice2"), {uid: "alice", at: serverTimestamp(), x: 1}), 0);
  await t("signed-out reserve denied", setDoc(U(anon, "ghost"), {uid: "", at: serverTimestamp()}), 0);
  await t("can't overwrite a reservation", updateDoc(U(alice, "alice.k"), {at: serverTimestamp()}), 0);
  await t("transaction reserve + release old", runTransaction(alice, async (tx) => {
    const s = await tx.get(U(alice, "alice.new")); if (s.exists()) throw new Error("taken");
    await tx.get(U(alice, "alice.k"));
    tx.set(U(alice, "alice.new"), {uid: "alice", at: serverTimestamp()}); tx.delete(U(alice, "alice.k"));
  }), 1);
  await t("can't release someone else's username", deleteDoc(U(alice, "taken.name")), 0);
  await t("admin can release a username", deleteDoc(U(mod, "taken.name")), 1);

  // Private profile
  await t("owner reads private profile", getDoc(doc(alice, "users/alice/private/profile")), 1);
  await t("owner writes private profile", setDoc(doc(alice, "users/alice/private/profile"), {fullName: "Alice K", street: "12 Park Rd"}, {merge: true}), 1);
  await t("others can't read private profile", getDoc(doc(bob, "users/alice/private/profile")), 0);
  await t("others can't write private profile", setDoc(doc(bob, "users/alice/private/profile"), {fullName: "x"}), 0);
  await t("admin can read private profile", getDoc(doc(mod, "users/alice/private/profile")), 1);
  await t("admin can't write private profile", setDoc(doc(mod, "users/alice/private/profile"), {fullName: "x"}), 0);

  // Public profile fields
  const P = (d, u) => doc(d, `ummah_profiles/${u}`);
  await t("owner sets public fields", setDoc(P(alice, "alice"), {name: "Alice K", handle: "alice.new", profileType: "student", city: "Hyderabad", country: "India"}, {merge: true}), 1);
  await t("owner can't claim scholar type", setDoc(P(alice, "alice"), {profileType: "scholar"}, {merge: true}), 0);
  await t("owner can't give themselves a scholar badge", setDoc(P(alice, "alice"), {scholarBadge: true}, {merge: true}), 0);
  await t("new profile can't start with a badge", setDoc(P(bob, "bob"), {name: "Bob", scholarBadge: true}), 0);
  await t("owner can't self-verify", setDoc(P(alice, "alice"), {verified: true}, {merge: true}), 0);
  await t("verified owner keeps editing name", setDoc(P(vip, "vip"), {name: "Vip 2"}, {merge: true}), 1);
  await t("verified owner can't drop verification field", setDoc(P(vip, "vip"), {name: "V", handle: "vip"}), 0);
  await t("admin sets scholar badge", updateDoc(P(mod, "alice"), {scholarBadge: true}), 1);
  await t("owner can't remove badge", setDoc(P(alice, "alice"), {scholarBadge: false}, {merge: true}), 0);
  await t("owner still edits name with badge", setDoc(P(alice, "alice"), {name: "Alice"}, {merge: true}), 1);
  await t("admin can't edit name", updateDoc(P(mod, "alice"), {name: "Hacked"}), 0);
  await t("city too long denied", setDoc(P(alice, "alice"), {city: "x".repeat(61)}, {merge: true}), 0);

  // Applications
  const A = (d, u) => doc(d, `scholar_applications/${u}`);
  await t("apply as a scholar", setDoc(A(bob, "bob"), app("bob")), 1);
  await t("applicant reads own application", getDoc(A(bob, "bob")), 1);
  await t("others can't read an application", getDoc(A(alice, "bob")), 0);
  await t("admin reads an application", getDoc(A(mod, "bob")), 1);
  await t("claim-admin reads an application", getDoc(A(admin, "bob")), 1);
  await t("admin lists pending", getDocs(query(collection(mod, "scholar_applications"), where("status", "==", "pending"))), 1);
  await t("users can't list applications", getDocs(query(collection(alice, "scholar_applications"), where("status", "==", "pending"))), 0);
  await t("apply for someone else denied", setDoc(A(alice, "carol"), app("carol")), 0);
  await t("self-approved application denied", setDoc(A(alice, "alice"), app("alice", {status: "approved"})), 0);
  await t("bad maslak denied", setDoc(A(alice, "alice"), app("alice", {maslak: "other"})), 0);
  await t("short bio denied", setDoc(A(alice, "alice"), app("alice", {bio: "Short"})), 0);
  await t("bad phone denied", setDoc(A(alice, "alice"), app("alice", {refPhone: "call me"})), 0);
  await t("years out of range denied", setDoc(A(alice, "alice"), app("alice", {years: 99})), 0);
  await t("cert in someone else's folder denied", setDoc(A(alice, "alice"), app("alice", {certPath: "scholar_certificates/bob/ijazah_1.jpg"})), 0);
  await t("extra field denied", setDoc(A(alice, "alice"), app("alice", {verified: true})), 0);
  await t("client timestamp denied", setDoc(A(alice, "alice"), app("alice", {submittedAt: new Date(0)})), 0);
  await t("reapply after rejection", setDoc(A(rej, "rej"), app("rej")), 1);
  await t("applicant can't approve self", updateDoc(A(pend, "pend"), {status: "approved"}), 0);

  // Admin decisions
  const approve = (d, u, by) => { const b = writeBatch(d);
    b.set(doc(d, `scholars/${u}`), {name: "Aisha Siddiqa", title: "Alimah", verified: true, approvedBy: by, approvedAt: serverTimestamp()});
    b.update(A(d, u), {status: "approved", reason: "", decidedBy: by, decidedAt: serverTimestamp()}); return b.commit(); };
  await t("user can't approve", approve(alice, "pend", "alice"), 0);
  await t("admin (admins doc) approves", approve(mod, "pend", "mod"), 1);
  await t("scholars directory shows verified", getDoc(doc(anon, "scholars/pend")), 1);
  await t("approvedBy must be the admin", setDoc(doc(mod, "scholars/x"), {name: "X", title: "Y", verified: true, approvedBy: "other", approvedAt: serverTimestamp()}), 0);
  await t("admin can't change application content", updateDoc(A(mod, "bob"), {bio: BIO + " edited"}), 0);
  await t("reject with reason", updateDoc(A(admin, "bob"), {status: "rejected", reason: "Please upload a clearer ijazah.", decidedBy: "boss", decidedAt: serverTimestamp()}), 1);
  await t("reject needs own decidedBy", updateDoc(A(mod, "bob"), {status: "rejected", reason: "x", decidedBy: "boss", decidedAt: serverTimestamp()}), 0);
  await t("revoke", (() => { const b = writeBatch(mod);
    b.set(doc(mod, "scholars/pend"), {name: "Aisha Siddiqa", title: "Alimah", verified: false, approvedBy: "mod", approvedAt: serverTimestamp(), revokedReason: "Reference could not confirm."});
    b.update(A(mod, "pend"), {status: "revoked", reason: "Reference could not confirm.", decidedBy: "mod", decidedAt: serverTimestamp()}); return b.commit(); })(), 1);
  await t("withdraw a rejected application", deleteDoc(A(bob, "bob")), 1);
  await t("can't delete a revoked application", deleteDoc(A(pend, "pend")), 0);

  // Storage: certificates
  if (sHost) {
    const st = (u, c) => (u ? env.authenticatedContext(u, c) : env.unauthenticatedContext()).storage();
    const img = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 1, 2, 3]);
    const S = (s, p) => ref(s, p);
    await t("upload own certificate", uploadBytes(S(st("bob"), "scholar_certificates/bob/ijazah_1.jpg"), img, {contentType: "image/jpeg"}), 1);
    await t("upload PDF certificate", uploadBytes(S(st("bob"), "scholar_certificates/bob/ijazah_2.pdf"), img, {contentType: "application/pdf"}), 1);
    await t("upload into someone else's folder denied", uploadBytes(S(st("alice"), "scholar_certificates/bob/x.jpg"), img, {contentType: "image/jpeg"}), 0);
    await t("non-image upload denied", uploadBytes(S(st("bob"), "scholar_certificates/bob/x.txt"), img, {contentType: "text/plain"}), 0);
    await t("over 10 MB denied", uploadBytes(S(st("bob"), "scholar_certificates/bob/big.jpg"), new Uint8Array(10 * 1024 * 1024 + 1), {contentType: "image/jpeg"}), 0);
    await t("owner reads certificate", getBytes(S(st("bob"), "scholar_certificates/bob/ijazah_1.jpg")), 1);
    await t("others can't read certificate", getBytes(S(st("alice"), "scholar_certificates/bob/ijazah_1.jpg")), 0);
    await t("signed-out can't read certificate", getBytes(S(st(null), "scholar_certificates/bob/ijazah_1.jpg")), 0);
    await t("claim-admin reads certificate", getBytes(S(st("boss", {admin: true}), "scholar_certificates/bob/ijazah_1.jpg")), 1);
    await t("admins-doc admin reads certificate", getBytes(S(st("mod"), "scholar_certificates/bob/ijazah_1.jpg")), 1);
    await t("overwrite denied", uploadBytes(S(st("bob"), "scholar_certificates/bob/ijazah_1.jpg"), img, {contentType: "image/jpeg"}), 0);
    await t("owner deletes certificate", deleteObject(S(st("bob"), "scholar_certificates/bob/ijazah_2.pdf")), 1);
  } else R.push("SKIP storage (no FIREBASE_STORAGE_EMULATOR_HOST)");

  console.log(R.join("\n"));
  console.log(`\n${R.filter((r) => r.startsWith("PASS")).length} passed, ${failed} failed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})().catch((e) => { console.error(e); process.exit(1); });
