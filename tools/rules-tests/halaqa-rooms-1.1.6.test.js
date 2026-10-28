// 1.1.6 Halaqa rooms — additive rules tests (Firestore emulator). Merges firestore-halaqa-rooms.rules into firestore.rules.
// Run: RULES=../../firestore.rules EXTRA=../../firestore-halaqa-rooms.rules npx firebase emulators:exec --only firestore "node halaqa-rooms-1.1.6.test.js"
const {initializeTestEnvironment, assertSucceeds, assertFails} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const {doc, setDoc, getDoc, getDocs, addDoc, collection, serverTimestamp, updateDoc, deleteDoc, Timestamp} = require("firebase/firestore");

function merged() {
  const base = fs.readFileSync(process.env.RULES || path.join(__dirname, "../../firestore.rules"), "utf8");
  const extra = fs.readFileSync(process.env.EXTRA || path.join(__dirname, "../../firestore-halaqa-rooms.rules"), "utf8");
  const i = base.lastIndexOf("}"); const j = base.lastIndexOf("}", i - 1);
  return base.slice(0, j) + extra + "\n  " + base.slice(j);
}

(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
  const env = await initializeTestEnvironment({projectId: "demo-np", firestore: {rules: merged(), host, port: Number(port)}});
  const soon = Timestamp.fromMillis(Date.now() + 3600e3);
  const room = (extra = {}) => ({title: "Tafseer of Surah Al-Kahf", topic: "DARS", description: "", hostUid: "hana", hostName: "Ustadha Hana", status: "live", scheduledAt: soon, createdAt: new Date(), ...extra});
  const mem = (role, extra = {}) => ({name: role, role, hand: false, muted: true, joinedAt: new Date(), ...extra});
  await env.withSecurityRulesDisabled(async (c) => {
    const f = c.firestore();
    await setDoc(doc(f, "admins/mod"), {role: "moderator"});
    await setDoc(doc(f, "halaqa_rooms/r1"), room());
    await setDoc(doc(f, "halaqa_rooms/old"), room({status: "ended"}));
    await setDoc(doc(f, "halaqa_rooms/r1/members/hana"), mem("host", {muted: false}));
    await setDoc(doc(f, "halaqa_rooms/r1/members/ali"), mem("speaker"));
    await setDoc(doc(f, "halaqa_rooms/r1/members/bea"), mem("listener", {hand: true}));
    await setDoc(doc(f, "halaqa_rooms/r1/links/ali_hana"), {from: "ali", to: "hana", offer: {type: "offer", sdp: "v=0"}, at: new Date()});
  });
  const db = (u) => env.authenticatedContext(u).firestore();
  const [hana, ali, bea, cy, mod] = ["hana", "ali", "bea", "cy", "mod"].map(db);
  const anon = env.unauthenticatedContext().firestore();
  const R = []; let failed = 0;
  async function t(name, p, ok) {
    try { await (ok ? assertSucceeds(p) : assertFails(p)); R.push("PASS " + name); } catch (e) { R.push("FAIL " + name + " " + e.message); failed++; }
  }
  const Rm = (d, id = "r1") => doc(d, `halaqa_rooms/${id}`);
  const M = (d, uid, id = "r1") => doc(d, `halaqa_rooms/${id}/members/${uid}`);
  const L = (d, id) => doc(d, `halaqa_rooms/r1/links/${id}`);
  const newRoom = (uid, extra = {}) => ({...room({hostUid: uid}), createdAt: serverTimestamp(), ...extra});

  // Rooms
  await t("anyone sees rooms", getDoc(Rm(anon)), 1);
  await t("schedule a room", setDoc(Rm(cy, "n1"), newRoom("cy", {status: "scheduled"})), 1);
  await t("host as someone else denied", setDoc(Rm(cy, "n2"), newRoom("hana")), 0);
  await t("bad topic denied", setDoc(Rm(cy, "n3"), newRoom("cy", {topic: "MUSIC"})), 0);
  await t("schedule a year ahead denied", setDoc(Rm(cy, "n4"), newRoom("cy", {scheduledAt: Timestamp.fromMillis(Date.now() + 400 * 86400e3)})), 0);
  await t("signed-out create denied", setDoc(Rm(anon, "n5"), newRoom("")), 0);
  await t("host goes live", updateDoc(Rm(cy, "n1"), {status: "live"}), 1);
  await t("stranger ends room denied", updateDoc(Rm(bea), {status: "ended"}), 0);
  await t("host can't hand room over", updateDoc(Rm(hana), {hostUid: "bea"}), 0);

  // Members
  await t("join as listener", setDoc(M(cy, "cy"), {...mem("listener"), joinedAt: serverTimestamp()}), 1);
  await t("join as speaker denied", setDoc(M(bea, "bea2"), {...mem("speaker"), joinedAt: serverTimestamp()}), 0);
  await t("join as host of others' room denied", setDoc(M(cy, "cy", "r1"), {...mem("host"), joinedAt: serverTimestamp()}), 0);
  await t("join unmuted denied", setDoc(M(cy, "cy", "n1"), {...mem("listener", {muted: false}), joinedAt: serverTimestamp()}), 0);
  await t("join an ended room denied", setDoc(M(cy, "cy", "old"), {...mem("listener"), joinedAt: serverTimestamp()}), 0);
  await t("raise hand", updateDoc(M(cy, "cy"), {hand: true}), 1);
  await t("promote yourself denied", updateDoc(M(bea, "bea"), {role: "speaker"}), 0);
  await t("listener unmutes denied", updateDoc(M(bea, "bea"), {muted: false}), 0);
  await t("host invites to speak", updateDoc(M(hana, "bea"), {role: "speaker", hand: false, muted: true}), 1);
  await t("speaker unmutes", updateDoc(M(bea, "bea"), {muted: false}), 1);
  await t("host mutes a speaker", updateDoc(M(hana, "bea"), {muted: true}), 1);
  await t("host unmutes others denied", updateDoc(M(hana, "bea"), {muted: false}), 0);
  await t("host makes someone host denied", updateDoc(M(hana, "ali"), {role: "host", muted: true}), 0);
  await t("speaker steps down", updateDoc(M(ali, "ali"), {role: "listener", muted: true}), 1);
  await t("non-host moves others denied", updateDoc(M(ali, "bea"), {role: "listener", muted: true}), 0);
  await t("rename yourself denied", updateDoc(M(cy, "cy"), {name: "Imam"}), 0);
  await t("host removes member", deleteDoc(M(hana, "cy")), 1);
  await t("leave", deleteDoc(M(bea, "bea")), 1);

  // Signalling links
  await t("parties read their link", getDoc(L(hana, "ali_hana")), 1);
  await t("outsider reads link denied", getDoc(L(cy, "ali_hana")), 0);
  await t("answerer adds answer", updateDoc(L(hana, "ali_hana"), {answer: {type: "answer", sdp: "v=0"}}), 1);
  await t("answerer rewrites offer denied", updateDoc(L(hana, "ali_hana"), {offer: {type: "offer", sdp: "x"}}), 0);
  await t("link to a non-member denied", setDoc(L(ali, "ali_cy2"), {from: "ali", to: "cy2", offer: {type: "offer", sdp: "v=0"}, at: serverTimestamp()}), 0);
  const dan = db("dan");
  await t("new listener joins", setDoc(M(dan, "dan"), {...mem("listener"), joinedAt: serverTimestamp()}), 1);
  await t("offerer creates link", setDoc(L(dan, "dan_hana"), {from: "dan", to: "hana", offer: {type: "offer", sdp: "v=0"}, at: serverTimestamp()}), 1);
  await t("non-party creates link denied", setDoc(L(ali, "dan_hana2"), {from: "dan", to: "hana2", offer: {}, at: serverTimestamp()}), 0);
  await t("wrong order link id denied", setDoc(L(hana, "hana_ali"), {from: "hana", to: "ali", offer: {}, at: serverTimestamp()}), 0);
  await t("offerer re-offers", setDoc(L(ali, "ali_hana"), {from: "ali", to: "hana", offer: {type: "offer", sdp: "v=1"}, at: serverTimestamp()}), 1);
  await t("offerer candidates", addDoc(collection(ali, "halaqa_rooms/r1/links/ali_hana/fromCandidates"), {uid: "ali", sdpMid: "0", sdpMLineIndex: 0, candidate: "c"}), 1);
  await t("answerer candidates", addDoc(collection(hana, "halaqa_rooms/r1/links/ali_hana/toCandidates"), {uid: "hana", sdpMid: "0", sdpMLineIndex: 0, candidate: "c"}), 1);
  await t("wrong side candidates denied", addDoc(collection(hana, "halaqa_rooms/r1/links/ali_hana/fromCandidates"), {uid: "hana", sdpMid: "0", sdpMLineIndex: 0, candidate: "c"}), 0);
  await t("outsider reads candidates denied", getDocs(collection(cy, "halaqa_rooms/r1/links/ali_hana/fromCandidates")), 0);

  // End
  await t("host ends room", updateDoc(Rm(hana), {status: "ended"}), 1);
  await t("reopen ended room denied", updateDoc(Rm(hana), {status: "live"}), 0);
  await t("moderator deletes room", deleteDoc(Rm(mod, "n1")), 1);

  console.log(R.join("\n"));
  console.log(`${R.length - failed}/${R.length} passed`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})();
