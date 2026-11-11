import assert from "node:assert/strict";
const { subtle } = globalThis.crypto;
const kp = await subtle.generateKey({ name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1,0,1]), hash: "SHA-256" }, true, ["sign","verify"]);
const pub = await subtle.exportKey("jwk", kp.publicKey);
const b64u = (b) => Buffer.from(b).toString("base64url");
async function sign(payload, kid = "k1") {
  const h = b64u(JSON.stringify({ alg: "RS256", kid, typ: "JWT" })), p = b64u(JSON.stringify(payload));
  const sig = await subtle.sign("RSASSA-PKCS1-v1_5", kp.privateKey, new TextEncoder().encode(`${h}.${p}`));
  return `${h}.${p}.${b64u(sig)}`;
}
let groqCalls = 0, groqMode = "ok", gemCalls = [], gemMode = "ok", glmCalls = [], glmMode = "ok";
globalThis.fetch = async (url, init) => {
  const u = String(url);
  if (u.includes("securetoken@system") || u.includes("firebaseappcheck")) return new Response(JSON.stringify({ keys: [{ ...pub, kid: "k1", alg: "RS256" }] }), { headers: { "cache-control": "max-age=600" } });
  if (u.includes("api.groq.com")) { groqCalls++; if (groqMode === "fail") return new Response("x", { status: 429 });
    return new Response(JSON.stringify({ choices: [{ message: { content: "Groq answer about focus.\nMETA: {\"fiqh\": false, \"followups\": [\"a\",\"b\"]}" } }] })); }
  if (u.includes("generativelanguage.googleapis.com")) { const m = /models\/([^:]+):/.exec(u)[1]; gemCalls.push({ m, body: JSON.parse(init.body), key: init.headers["x-goog-api-key"] });
    if (gemMode === "slow") await new Promise((r) => setTimeout(r, 6000));
    if (gemMode === "fail" || (gemMode === "fail-best" && m === "gemini-3.8-flash")) return new Response("{}", { status: 429 });
    const txt = gemMode === "mixed" ? "حجِ تمattuٰ اور حجِ قیran" : "Gemini answer.\nMETA: {\"fiqh\": false, \"followups\": [\"g\"]}";
    return new Response(JSON.stringify({ candidates: [{ content: { parts: [{ text: "thinking...", thought: true }, { text: txt }] } }] })); }
  if (u.includes("api.z.ai")) { const b = JSON.parse(init.body); glmCalls.push(b.model); if (glmMode === "fail") return new Response("{}", { status: 429 });
    return new Response(JSON.stringify({ choices: [{ message: { content: "GLM answer for focus.\nMETA: {\"fiqh\": false, \"followups\": []}" } }] })); }
  throw new Error("unexpected fetch " + u);
};
const { default: worker, parseMeta, handleChat } = await import("./src/worker.js");
const { detectLanguage, resolveLanguage } = await import("./src/lang.js");
const { verifyFirebaseIdToken, verifyAppCheckToken, identify } = await import("./src/auth.js");
const { chatSystemPrompt } = await import("./src/prompts.js");
const { localCount } = await import("./src/quota.js");
const { buildOrder, _resetCooldowns, ZHIPU_FREE_MODELS } = await import("./src/llm.js");

let aiCalls = [];
const mkAI = (reply) => ({ run: async (model, input) => { aiCalls.push({ model, input }); return typeof reply === "function" ? reply(model, input) : reply; } });
const env = (over = {}) => ({ AI: mkAI({ response: "Here are 5 ways to focus in salah.\nMETA: {\"fiqh\": false, \"followups\": [\"How to gain khushu?\"]}" }), ...over });
const post = (path, body, headers = {}) => new Request("https://w" + path, { method: "POST", headers: { "content-type": "application/json", ...headers }, body: JSON.stringify(body) });
let n = 0; const t = async (name, fn) => { await fn(); n++; console.log("ok -", name); };

await t("detect Urdu", () => assert.equal(detectLanguage("مجھے حج کے بارے میں جاننا ہے"), "ur"));
await t("detect Arabic", () => assert.equal(detectLanguage("ما هي أركان الإسلام؟"), "ar"));
await t("detect Hindi", () => assert.equal(detectLanguage("नमाज़ में ध्यान कैसे लगाएं?"), "hi"));
await t("detect Bengali", () => assert.equal(detectLanguage("নামাজে মনোযোগ কিভাবে বাড়াব?"), "bn"));
await t("detect Roman Urdu", () => assert.equal(detectLanguage("namaz mein dhyan kaise lagaye mujhe batao"), "ur-Latn"));
await t("detect English", () => assert.equal(detectLanguage("How can I improve focus in salah?"), "en"));
await t("explicit language wins, auto detects", () => { assert.equal(resolveLanguage("hi", "hello"), "hi"); assert.equal(resolveLanguage("auto", "مجھے حج کے بارے میں جاننا ہے"), "ur"); });
await t("system prompt carries creed + language", () => { const s = chatSystemPrompt("AskNoor", "ur"); assert.match(s, /Ahl as-Sunnah/); assert.match(s, /Tasawwuf/); assert.match(s, /Mawlid/); assert.match(s, /Urdu/); assert.match(s, /META/); });
await t("parseMeta splits META json", () => { const r = parseMeta("Body text\nMETA: {\"fiqh\": true, \"followups\": [\"x\",\"y\"]}"); assert.equal(r.body, "Body text"); assert.equal(r.fiqh, true); assert.deepEqual(r.followups, ["x","y"]); });
await t("parseMeta legacy FOLLOWUPS", () => { const r = parseMeta("Body\nFOLLOWUPS: a | b"); assert.equal(r.body, "Body"); assert.deepEqual(r.followups, ["a","b"]); assert.equal(r.fiqh, null); });
await t("health GET", async () => { const r = await worker.fetch(new Request("https://w/health"), env()); assert.equal(r.status, 200); assert.equal((await r.json()).groq, false); });
await t("405 on PUT", async () => assert.equal((await worker.fetch(new Request("https://w/v1/chat", { method: "PUT" }), env())).status, 405));
await t("404 unknown route", async () => assert.equal((await worker.fetch(post("/v1/nope", {}), env())).status, 404));
await t("bad json 400", async () => assert.equal((await worker.fetch(new Request("https://w/v1/chat", { method: "POST", body: "{" }), env())).status, 400));
await t("chat answers via Workers AI with detected Urdu", async () => {
  aiCalls = [];
  const r = await worker.fetch(post("/v1/chat", { prompt: "مجھے حج کے بارے میں جاننا ہے", mode: "HajjUmrahGuide", language: "auto" }), env());
  const j = await r.json(); assert.equal(r.status, 200); assert.equal(j.language, "ur"); assert.equal(j.fiqh, false); assert.equal(j.provider, "workers-ai");
  assert.equal(aiCalls[0].model, "@cf/google/gemma-4-26b-a4b-it"); assert.match(aiCalls[0].input.messages[0].content, /Urdu/);
});
await t("chat passes passages + cites only matched refs", async () => {
  const e = env({ AI: mkAI({ response: "Be patient (Qur'an 2:153). Long enough answer here.\nMETA: {\"fiqh\": false, \"followups\": []}" }) });
  const r = await handleChat({ prompt: "patience", passages: [{ ref: "Qur'an 2:153", text: "O you who believe, seek help through patience" }, { ref: "Sahih al-Bukhari 1", text: "x" }] }, e);
  const j = await r.json(); assert.deepEqual(j.citations, ["Qur'an 2:153"]);
});
await t("Groq first when only Groq is set, falls back to Workers AI", async () => {
  _resetCooldowns(); groqCalls = 0; groqMode = "ok";
  let j = await (await handleChat({ prompt: "focus in salah" }, env({ GROQ_API_KEY: "k" }))).json();
  assert.equal(j.provider, "groq"); assert.equal(groqCalls, 1);
  groqMode = "fail"; groqCalls = 0;
  j = await (await handleChat({ prompt: "focus in salah" }, env({ GROQ_API_KEY: "k" }))).json();
  assert.equal(j.provider, "workers-ai"); assert.equal(groqCalls, 2);
  groqCalls = 0; // rate-limited Groq models are cooled down, not retried on the next request
  j = await (await handleChat({ prompt: "focus in salah" }, env({ GROQ_API_KEY: "k" }))).json();
  assert.equal(j.provider, "workers-ai"); assert.equal(groqCalls, 0);
  groqMode = "ok"; _resetCooldowns();
});
await t("routing: English Gemini Flash-Lite first; Urdu Gemini 3.8 Flash then Gemma; GLM only free + Latin", () => {
  const e = { GEMINI_API_KEY: "g", GROQ_API_KEY: "q", ZHIPU_API_KEY: "z", AI: {} };
  const en = buildOrder(e, { lang: "en" }).map((x) => x[1]);
  assert.equal(en[0], "gemini-3.5-flash-lite"); assert.equal(en[1], "qwen/qwen3.8-27b"); assert.deepEqual(en.slice(-2), ["glm-4.7-flash", "glm-4.5-flash"]);
  const ur = buildOrder(e, { lang: "ur" }).map((x) => x[1]);
  assert.deepEqual(ur.slice(0, 3), ["gemini-3.8-flash", "@cf/google/gemma-4-26b-a4b-it", "gemini-3.5-flash-lite"]);
  assert.ok(!ur.some((m) => m.includes("gpt-oss-20b") || m === "openai/gpt-oss-120b" || m.startsWith("glm")));
  assert.deepEqual(ZHIPU_FREE_MODELS, ["glm-4.7-flash", "glm-4.5-flash"]);
  assert.equal(buildOrder({ AI: {} }, { lang: "en" })[0][0], "workers-ai");
});
await t("Gemini answers first, strips thoughts, sends key only in header", async () => {
  _resetCooldowns(); gemCalls = []; gemMode = "ok";
  const j = await (await handleChat({ prompt: "مجھے حج کے بارے میں جاننا ہے" }, env({ GEMINI_API_KEY: "gk", GROQ_API_KEY: "k" }))).json();
  assert.equal(j.provider, "gemini"); assert.equal(j.model, "gemini-3.8-flash"); assert.equal(j.answer, "Gemini answer."); assert.equal(j.language, "ur");
  assert.equal(gemCalls[0].key, "gk"); assert.match(gemCalls[0].body.systemInstruction.parts[0].text, /Ahl as-Sunnah/);
});
await t("Gemini 429 falls through (Urdu -> Gemma) and is cooled down", async () => {
  _resetCooldowns(); gemCalls = []; gemMode = "fail-best"; aiCalls = [];
  let j = await (await handleChat({ prompt: "مجھے حج کے بارے میں جاننا ہے" }, env({ GEMINI_API_KEY: "gk" }))).json();
  assert.equal(j.provider, "workers-ai"); assert.equal(gemCalls.length, 1);
  gemCalls = [];
  j = await (await handleChat({ prompt: "مجھے حج کے بارے میں جاننا ہے" }, env({ GEMINI_API_KEY: "gk" }))).json();
  assert.equal(j.provider, "workers-ai"); assert.equal(gemCalls.length, 0);
  gemMode = "ok"; _resetCooldowns();
});
await t("mixed-script Gemini answer is rejected", async () => {
  _resetCooldowns(); gemMode = "mixed";
  const j = await (await handleChat({ prompt: "مجھے حج کے بارے میں جاننا ہے" }, env({ GEMINI_API_KEY: "gk" }))).json();
  assert.equal(j.provider, "workers-ai"); gemMode = "ok";
});
await t("Urdu: slow Gemini loses the race to Gemma after the grace period", async () => {
  _resetCooldowns(); gemMode = "slow"; const t0 = Date.now();
  const j = await (await handleChat({ prompt: "مجھے حج کے بارے میں جاننا ہے" }, env({ GEMINI_API_KEY: "gk" }))).json();
  assert.equal(j.provider, "workers-ai"); assert.ok(Date.now() - t0 < 5500, "should not wait for slow Gemini");
  gemMode = "ok";
});
await t("Z.ai GLM is the last English fallback", async () => {
  _resetCooldowns(); glmCalls = []; gemMode = "fail"; groqMode = "fail";
  const deadAI = { run: async () => { throw new Error("down"); } };
  const j = await (await handleChat({ prompt: "focus in salah" }, { AI: deadAI, GEMINI_API_KEY: "g", GROQ_API_KEY: "q", ZHIPU_API_KEY: "z" })).json();
  assert.equal(j.provider, "zhipu"); assert.deepEqual(glmCalls, ["glm-4.7-flash"]);
  glmCalls = [];
  const r = await handleChat({ prompt: "مجھے حج کے بارے میں جاننا ہے" }, { AI: deadAI, GEMINI_API_KEY: "g", ZHIPU_API_KEY: "z" });
  assert.equal(r.status, 503); assert.equal(glmCalls.length, 0);
  gemMode = "ok"; groqMode = "ok"; _resetCooldowns();
});
await t("mixed-script detector", async () => {
  const { hasMixedScriptWords } = await import("./src/llm.js");
  assert.equal(hasMixedScriptWords("حجِ تمattuٰ اور حجِ قیran", "ur"), true);
  assert.equal(hasMixedScriptWords("حج تمتع (Hajj al-Tamattu) اور قران", "ur"), false);
  assert.equal(hasMixedScriptWords("anything goes", "en"), false);
});
await t("503 when every model fails", async () => {
  const r = await handleChat({ prompt: "x?" }, { AI: { run: async () => { throw new Error("down"); } } });
  assert.equal(r.status, 503);
});
await t("creator hashtags", async () => {
  const r = await worker.fetch(post("/v1/creator", { task: "hashtags", text: "Sabr reminder", topic: "Reminder" }), env({ AI: mkAI({ response: "#Sabr #Patience #Quran #Sabr" }) }));
  assert.deepEqual((await r.json()).items, ["#Sabr", "#Patience", "#Quran"]);
});
await t("creator hooks list", async () => {
  const r = await worker.fetch(post("/v1/creator", { task: "hooks", topic: "Dua" }), env({ AI: mkAI({ response: "1. One\n2. Two\n3. Three" }) }));
  assert.deepEqual((await r.json()).items, ["One", "Two", "Three"]);
});
await t("creator subtitles keeps indexes", async () => {
  const r = await worker.fetch(post("/v1/creator", { task: "subtitles", text: "1|Hello\n2|World", lang: "ur" }), env({ AI: mkAI({ response: "1|ہیلو\n2|دنیا" }) }));
  const j = await r.json(); assert.deepEqual(j.lines.map((l) => l.index), [1, 2]);
});
await t("creator moderate: guard unsafe forces block", async () => {
  const ai = mkAI((model) => model.includes("llama-guard") ? { response: "unsafe\nS10" } : { response: '{"verdict":"ok","issues":[],"suggestion":""}' });
  const j = await (await worker.fetch(post("/v1/creator", { task: "moderate", text: "bad words" }), env({ AI: ai }))).json();
  assert.equal(j.verdict, "block");
});
await t("creator unknown task 400", async () => assert.equal((await worker.fetch(post("/v1/creator", { task: "x", text: "y" }), env())).status, 400));
await t("transcribe builds VTT", async () => {
  const ai = mkAI({ text: "Bismillah", segments: [{ start: 0, end: 1.5, text: "Bismillah" }], transcription_info: { language: "ar" } });
  const j = await (await worker.fetch(post("/v1/transcribe", { audio: "AAAA" }), env({ AI: ai }))).json();
  assert.match(j.vtt, /00:00:00.000 --> 00:00:01.500/); assert.equal(j.language, "ar");
});
await t("413 oversized body", async () => {
  const r = await worker.fetch(new Request("https://w/v1/chat", { method: "POST", headers: { "content-length": "999999" }, body: "{}" }), env());
  assert.equal(r.status, 413);
});
await t("burst limiter 429", async () => {
  const r = await worker.fetch(post("/v1/chat", { prompt: "hi" }), env({ RL_ANON: { limit: async () => ({ success: false }) } }));
  assert.equal(r.status, 429);
});
await t("daily quota via DO 429", async () => {
  const QUOTA = { idFromName: (n) => n, get: () => ({ fetch: async () => Response.json({ ok: false }) }) };
  const r = await worker.fetch(post("/v1/chat", { prompt: "hi" }), env({ QUOTA }));
  assert.equal((await r.json()).error, "daily_limit");
});
await t("local quota counter", () => { assert.ok(localCount("k", 1, 2, "d")); assert.ok(localCount("k", 1, 2, "d")); assert.ok(!localCount("k", 1, 2, "d")); });
const now = Math.floor(Date.now() / 1000);
await t("valid Firebase ID token -> user tier", async () => {
  const tok = await sign({ aud: "noor-pro-d87e3", iss: "https://securetoken.google.com/noor-pro-d87e3", sub: "uid1", iat: now, exp: now + 3600 });
  assert.equal((await verifyFirebaseIdToken(tok)).sub, "uid1");
  const id = await identify(new Request("https://w", { headers: { authorization: `Bearer ${tok}` } }));
  assert.deepEqual(id, { tier: "user", key: "u:uid1" });
});
await t("valid App Check token -> app tier", async () => {
  const tok = await sign({ aud: ["projects/684547719535", "projects/noor-pro-d87e3"], iss: "https://firebaseappcheck.googleapis.com/684547719535", sub: "1:684547719535:android:x", iat: now, exp: now + 3600 });
  assert.ok(await verifyAppCheckToken(tok));
  assert.equal((await identify(new Request("https://w", { headers: { "x-firebase-appcheck": tok, "cf-connecting-ip": "1.2.3.4" } }))).tier, "app");
});
await t("wrong-project tokens rejected -> anon", async () => {
  const tok = await sign({ aud: "other", iss: "https://securetoken.google.com/other", sub: "u", iat: now, exp: now + 3600 });
  assert.equal(await verifyFirebaseIdToken(tok), null);
  const ac = await sign({ aud: ["projects/1"], iss: "https://firebaseappcheck.googleapis.com/1", sub: "x", iat: now, exp: now + 3600 });
  assert.equal(await verifyAppCheckToken(ac), null);
  assert.equal((await identify(new Request("https://w", { headers: { authorization: `Bearer ${tok}` } }))).tier, "anon");
});
console.log(`\n${n} tests passed`);
const { isRulingQuestion } = await import("./src/worker.js");
await t("ruling detector: practical questions are not fiqh", () => {
  for (const q of ["How can I improve focus in salah?", "مجھے حج کے بارے میں جاننا ہے", "namaz mein dhyan kaise lagaye", "Tell me about Mawlid"]) assert.equal(isRulingQuestion(q), false, q);
  for (const q of ["Is it halal to work in a bank?", "Does vomiting break my fast?", "کیا سود حرام ہے؟", "kya mortgage jaiz hai", "zakat on gold"]) assert.equal(isRulingQuestion(q), true, q);
});
await t("chat fiqh flag needs both model and question", async () => {
  const e = env({ AI: mkAI({ response: "Answer text long enough.\nMETA: {\"fiqh\": true, \"followups\": []}" }) });
  assert.equal((await (await handleChat({ prompt: "How can I improve focus in salah?" }, e)).json()).fiqh, false);
  assert.equal((await (await handleChat({ prompt: "Is it halal to work in a bank?" }, e)).json()).fiqh, true);
});
console.log(`${n} tests passed (incl. fiqh)`);
