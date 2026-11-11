// 1.1.9 1:1 chat PERMISSION_DENIED fix — rules tests (Firestore emulator).
// Run against the old rules with OLD=1 (the "fix" cases are expected to FAIL there) and against the new rules without it.
// RULES=<file> [OLD=1] npx firebase emulators:exec --only firestore "node chats-1.1.9.test.js"
const {initializeTestEnvironment, assertSucceeds, assertFails} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const {doc, getDoc, getDocs, collection, query, orderBy, writeBatch, serverTimestamp, increment, updateDoc, setDoc} = require("firebase/firestore");

(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
  const rules = fs.readFileSync(process.env.RULES || path.join(__dirname, "../../firestore.rules"), "utf8");
  const OLD = process.env.OLD === "1";
  const env = await initializeTestEnvironment({projectId: "demo-np-chat", firestore: {rules, host, port: Number(port)}});
  await env.withSecurityRulesDisabled(async (c) => {
    const f = c.firestore();
    // Existing chat created by bob, so participants are stored as [bob, alice].
    await setDoc(doc(f, "ummah_chats/alice_bob"), {participantUids: ["bob", "alice"], participantNames: {bob: "Bob", alice: "Alice"}, lastMessage: "hi"});
    await setDoc(doc(f, "ummah_chats/alice_bob/messages/m1"), {senderUid: "bob", type: "text", text: "salam", mediaUrl: "", createdAt: new Date()});
  });
  const alice = env.authenticatedContext("alice").firestore();
  const bob = env.authenticatedContext("bob").firestore();
  const carol = env.authenticatedContext("carol").firestore();
  const anon = env.unauthenticatedContext().firestore();
  const R = []; let failed = 0;
  async function t(name, p, ok) {
    try { await (ok ? assertSucceeds(p) : assertFails(p)); R.push("PASS " + name); } catch (e) { R.push("FAIL " + name + " " + e.message); failed++; }
  }
  const fix = !OLD; // cases fixed by the 1.1.9 rules
  const msgs = (db, id) => query(collection(db, `ummah_chats/${id}/messages`), orderBy("createdAt"));
  function send(db, me, other, order) {
    const id = [me, other].sort().join("_");
    const b = writeBatch(db);
    b.set(doc(db, `ummah_chats/${id}`), {participantUids: order, participantNames: {[me]: me, [other]: other}, lastMessage: "x",
      lastMessageType: "text", lastSenderUid: me, updatedAt: serverTimestamp(), [`unread_${other}`]: increment(1)}, {merge: true});
    b.set(doc(collection(db, `ummah_chats/${id}/messages`)), {senderUid: me, senderName: me, text: "salam", mediaUrl: "", type: "text",
      replyToMessageId: "", replyToName: "", replyToText: "", replyToType: "", createdAt: serverTimestamp()});
    return b.commit();
  }

  // Opening a chat with a contact before the first message.
  await t("member reads not-yet-created chat doc", getDoc(doc(alice, "ummah_chats/alice_dave")), fix);
  await t("member listens to not-yet-created chat messages", getDocs(msgs(alice, "alice_dave")), fix);
  await t("non-member cannot read not-yet-created chat doc", getDoc(doc(carol, "ummah_chats/alice_dave")), false);
  await t("non-member cannot list not-yet-created chat messages", getDocs(msgs(carol, "alice_dave")), false);
  await t("signed-out cannot read not-yet-created chat", getDoc(doc(anon, "ummah_chats/alice_dave")), false);
  // First message creates the chat (unchanged behaviour).
  await t("member creates chat + first message", send(alice, "alice", "dave", ["alice", "dave"]), true);
  await t("after create: member reads chat", getDoc(doc(alice, "ummah_chats/alice_dave")), true);
  await t("after create: non-member still denied", getDoc(doc(carol, "ummah_chats/alice_dave")), false);
  await t("after create: non-member messages denied", getDocs(msgs(carol, "alice_dave")), false);
  // Reply into a chat the other member created (participants stored in the other order).
  await t("member replies with reversed participant order", send(alice, "alice", "bob", ["alice", "bob"]), fix);
  await t("member replies with stored order (unchanged)", send(alice, "alice", "bob", ["bob", "alice"]), true);
  await t("member cannot swap a participant", updateDoc(doc(alice, "ummah_chats/alice_bob"), {participantUids: ["alice", "carol"]}), false);
  await t("member cannot add a third participant", updateDoc(doc(alice, "ummah_chats/alice_bob"), {participantUids: ["alice", "bob", "carol"]}), false);
  await t("member cannot shrink participants", updateDoc(doc(alice, "ummah_chats/alice_bob"), {participantUids: ["alice", "alice"]}), false);
  await t("non-member cannot update chat", updateDoc(doc(carol, "ummah_chats/alice_bob"), {lastMessage: "x"}), false);
  await t("non-member cannot read existing chat", getDoc(doc(carol, "ummah_chats/alice_bob")), false);
  await t("non-member cannot read existing messages", getDocs(msgs(carol, "alice_bob")), false);
  await t("non-member cannot post into chat", setDoc(doc(collection(carol, "ummah_chats/alice_bob/messages")), {senderUid: "carol", type: "text", text: "x", mediaUrl: "", createdAt: serverTimestamp()}), false);
  await t("member reads existing messages", getDocs(msgs(bob, "alice_bob")), true);
  await t("typing update on existing chat", updateDoc(doc(alice, "ummah_chats/alice_bob"), {typing_alice: 0}), true);
  await t("chat delete still denied", (async () => { const {deleteDoc} = require("firebase/firestore"); return deleteDoc(doc(alice, "ummah_chats/alice_bob")); })(), false);

  console.log(R.join("\n"));
  console.log(`${R.length - failed}/${R.length} passed (${OLD ? "OLD rules" : "NEW rules"})`);
  await env.cleanup();
  process.exit(failed ? 1 : 0);
})();
