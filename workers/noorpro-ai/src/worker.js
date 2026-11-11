// NoorPro AI Worker: Noor AI chat, creator tools, subtitles. No secrets in the app; keys live only as Worker secrets.
import { identify } from "./auth.js";
import { resolveLanguage, LANG_NAMES } from "./lang.js";
import { chatSystemPrompt, passagesBlock, contextBlock, creatorPrompt, CREATOR_SYSTEM } from "./prompts.js";
import { complete, GEMINI_MODELS } from "./llm.js";
import { retrieve, kbBlock, kbCitations, normMadhhab, MADHHAB_NAMES, KB_SIZE } from "./kb.js";
import { handleImage } from "./image.js";
import { handleTts, handleTranscribeV2 } from "./voice.js";
import { takeQuota, burstOk, takeImageQuota, takeTtsQuota } from "./quota.js";
export { Quota } from "./quota.js";

const MAX_BODY = 64 * 1024;
const MAX_AUDIO_B64 = 6 * 1024 * 1024;
const MODES = new Set(["AskNoor", "DuaGenerator", "QuranHelper", "HadithExplainer", "HajjUmrahGuide", "AppGuide", "HifzHelper"]);
const CREATOR_TASKS = new Set(["captions", "hooks", "hashtags", "translate", "subtitles", "moderate"]);

const json = (status, obj) => new Response(JSON.stringify(obj), {
  status, headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store", "x-content-type-options": "nosniff" },
});

/** Splits the trailing META JSON line (or legacy FOLLOWUPS line) off a model answer. */
export function parseMeta(text) {
  let body = String(text || "").trim();
  let fiqh = null, followups = [];
  const m = /\n?\s*\**META\**:?\s*(\{[\s\S]*\})\s*$/i.exec(body);
  if (m) {
    body = body.slice(0, m.index).trim();
    try {
      const j = JSON.parse(m[1]);
      if (typeof j.fiqh === "boolean") fiqh = j.fiqh;
      if (Array.isArray(j.followups)) followups = j.followups.map((s) => String(s).trim()).filter(Boolean).slice(0, 3);
    } catch {}
  }
  const f = /\n?\s*FOLLOWUPS:\s*(.+)\s*$/i.exec(body);
  if (f) { body = body.slice(0, f.index).trim(); if (!followups.length) followups = f[1].split("|").map((s) => s.trim()).filter(Boolean).slice(0, 3); }
  body = body.replace(/\n?\s*META:\s*$/i, "").trim();
  return { body, fiqh, followups: followups.map((s) => s.replace(/\*\*/g, "").slice(0, 140)) };
}

const RULING = [
  /\b(halal|haram|makruh|makrooh|mubah|fard|farz|wajib|sunnah or|permissible|permitted|allowed|forbidden|sinful|ruling|fatwa|fiqh|madhhab|valid|invalid|nullif|break(s)? (my |the )?(fast|wudu|wudhu|salah|namaz)|kaffarah|kaffara|fidya|zakat|zakah|nisab|inheritance|talaq|divorce|nikah|mahr|riba|interest|mortgage|insurance)\b/i,
  /\b(is it|is this|are we|am i)\s+(ok|okay|allowed|permissible|halal|haram|sin|a sin|wajib|fard|sunnah)\b/i,
  /\b(jaiz|najaiz|na jaiz|gunah hai|haram hai|halal hai|farz hai|wajib hai|sunnat hai|kya .{0,30}sakte)\b/i,
  /(حلال|حرام|جائز|ناجائز|مکروہ|فرض|واجب|فتو|طلاق|نکاح|وراثت|زکوٰۃ|زکاة|زکات|سود|ربا|کفارہ|کیا .{0,40}سکتے)/,
  /(हलाल|हराम|जायज़|जाइज़|नाजायज़|फ़र्ज़|वाजिब|फ़तवा|तलाक़|ज़कात|सूद)/,
  /((وضو|روزہ|روزے|نماز|غسل).{0,25}(ٹوٹ|باطل|فاسد)|(वुज़ू|रोज़ा|नमाज़).{0,25}(टूट)|(wudu|wuzu|roza|namaz).{0,20}(toot|tut|tootta|toot jata)|(الوضوء|الصوم|الصلاة).{0,25}(ينقض|يبطل|يفسد))/i,
];
export function isRulingQuestion(q) { return RULING.some((r) => r.test(String(q || ""))); }

function cleanHistory(history) {
  if (!Array.isArray(history)) return [];
  return history.slice(-10).map((h) => ({
    role: h && h.role === "assistant" ? "assistant" : "user",
    content: String((h && h.content) || "").slice(0, 2500),
  })).filter((h) => h.content.trim());
}

function citedPassages(answer, passages) {
  if (!Array.isArray(passages)) return [];
  const a = answer.toLowerCase();
  return passages.map((p) => String(p.ref || "").trim()).filter((r) => r && a.includes(r.toLowerCase().replace(/^qur'an\s+/, ""))).slice(0, 6);
}

const TIERS = new Set(["fast", "balanced", "research"]);

/** Builds the chat request (system prompt, KB grounding, history) shared by normal and research answers. */
async function prepareChat(body, env, kbEntries) {
  const prompt = String(body.prompt || body.message || "").trim().slice(0, 4000);
  const mode = MODES.has(body.mode) ? body.mode : "AskNoor";
  const lang = resolveLanguage(body.language, prompt);
  const madhhab = normMadhhab(body.madhhab);
  const tier = TIERS.has(body.tier) ? body.tier : "balanced";
  const passages = Array.isArray(body.passages) ? body.passages.slice(0, 6) : [];
  const kb = kbEntries ?? (mode === "AppGuide" || mode === "DuaGenerator" ? [] : await retrieve(env, prompt, tier === "fast" ? 2 : 4));
  const userParts = [contextBlock(body.context), kbBlock(kb, madhhab), passagesBlock(passages), `Question: ${prompt}`].filter(Boolean).join("\n\n");
  const messages = [
    { role: "system", content: chatSystemPrompt(mode, lang, { madhhab, tier, kb: kb.length > 0 }) },
    ...cleanHistory(body.history),
    { role: "user", content: userParts },
  ];
  return { prompt, mode, lang, madhhab, tier, passages, kb, messages };
}

function chatResult(c, out, extra = {}) {
  const { body: answer, fiqh, followups } = parseMeta(out.text);
  if (!answer) return null;
  const citations = [...kbCitations(answer, c.kb), ...citedPassages(answer, c.passages)].slice(0, 8);
  return {
    answer, language: c.lang, languageName: LANG_NAMES[c.lang] || (c.lang === "hi-Latn" ? "Hinglish" : "English"),
    fiqh: fiqh === true && isRulingQuestion(c.prompt), followups, citations, madhhab: c.madhhab, tier: c.tier,
    kb: c.kb.map((e) => e.id), provider: out.provider, model: out.model, ...extra,
  };
}

export async function handleChat(body, env) {
  const c = await prepareChat(body, env);
  if (!c.prompt) return json(400, { error: "empty" });
  const out = await complete(env, c.messages, {
    maxTokens: c.mode === "DuaGenerator" ? 700 : c.tier === "fast" ? 700 : 1400,
    temperature: c.mode === "DuaGenerator" ? 0.6 : 0.3, minLength: 20, lang: c.lang, tier: c.tier,
  });
  if (!out) return json(503, { error: "unavailable", language: c.lang });
  const r = chatResult(c, out);
  return r ? json(200, r) : json(503, { error: "unavailable", language: c.lang });
}

// ------------------------------------------------------------------ research mode
/** Multi-step research: plan sub-queries, search the KB for each, then write a cited answer with the best model. */
export async function runResearch(body, env, emit = () => {}) {
  const prompt = String(body.prompt || "").trim().slice(0, 4000);
  if (!prompt) return { status: 400, body: { error: "empty" } };
  emit({ type: "stage", stage: "planning", label: "Understanding your question" });
  const plan = await complete(env, [{ role: "user", content:
    `Break this Islamic question into 2-4 short English search queries for a knowledge base of Sunni aqidah, fiqh (four madhhabs) and tasawwuf summaries. Return ONLY JSON {"queries":["..."]}.\nQuestion: ${prompt}` }],
    { maxTokens: 200, temperature: 0.2, fast: true, lang: "en", totalMs: 12_000 });
  let queries = [];
  try { queries = (JSON.parse((/\{[\s\S]*\}/.exec(plan?.text || "") || ["{}"])[0]).queries || []).map(String).slice(0, 4); } catch {}
  emit({ type: "stage", stage: "searching", label: "Searching the knowledge base", queries });
  const lists = await Promise.all([prompt, ...queries].map((q) => retrieve(env, q, 4, 0.1)));
  const seen = new Set(); const kb = [];
  for (const l of lists) for (const e of l) if (!seen.has(e.id) && kb.length < 8) { seen.add(e.id); kb.push(e); }
  emit({ type: "stage", stage: "sources", label: `Found ${kb.length} sources`, sources: kb.map((e) => e.topic) });
  const c = await prepareChat({ ...body, tier: "research" }, env, kb);
  emit({ type: "stage", stage: "writing", label: "Writing a cited answer" });
  const out = await complete(env, c.messages, { maxTokens: 3000, temperature: 0.3, minLength: 40, lang: c.lang, tier: "research", totalMs: 100_000 });
  if (!out) return { status: 503, body: { error: "unavailable", language: c.lang } };
  const r = chatResult(c, out, { steps: { queries, sources: kb.map((e) => e.topic) } });
  return r ? { status: 200, body: r } : { status: 503, body: { error: "unavailable", language: c.lang } };
}

function researchStream(body, env) {
  const { readable, writable } = new TransformStream();
  const w = writable.getWriter(); const enc = new TextEncoder();
  const send = (obj) => w.write(enc.encode(`data: ${JSON.stringify(obj)}\n\n`)).catch(() => {});
  (async () => {
    try {
      const r = await runResearch(body, env, send);
      await send(r.status === 200 ? { type: "answer", ...r.body } : { type: "error", ...r.body });
    } catch { await send({ type: "error", error: "internal" }); }
    await w.close().catch(() => {});
  })();
  return new Response(readable, { headers: { "content-type": "text/event-stream; charset=utf-8", "cache-control": "no-store", "x-content-type-options": "nosniff" } });
}

function parseList(text, max) {
  return String(text).split("\n").map((l) => l.trim()).filter((l) => /^(\d+[.)]|[-•*])\s+/.test(l))
    .map((l) => l.replace(/^(\d+[.)]|[-•*])\s+/, "").replace(/\*\*/g, "").replace(/^["“]|["”]$/g, "").trim()).filter((l) => l.length >= 2).slice(0, max);
}
function parseJsonLoose(text) {
  const m = /\{[\s\S]*\}/.exec(String(text));
  if (!m) return null;
  try { return JSON.parse(m[0]); } catch { return null; }
}

async function guard(env, text) {
  if (!env.AI) return null;
  try {
    const r = await env.AI.run("@cf/meta/llama-guard-3-8b", { messages: [{ role: "user", content: String(text).slice(0, 3000) }] });
    const s = typeof r?.response === "string" ? r.response : JSON.stringify(r?.response ?? r);
    if (/unsafe/i.test(s)) return { safe: false, categories: (s.match(/S\d{1,2}/g) || []) };
    if (/safe/i.test(s)) return { safe: true, categories: [] };
  } catch {}
  return null;
}

export async function handleCreator(body, env) {
  const task = String(body.task || "");
  if (!CREATOR_TASKS.has(task)) return json(400, { error: "task" });
  const text = String(body.text || "").slice(0, 3000);
  if (task !== "captions" && task !== "hooks" && !text.trim()) return json(400, { error: "empty" });
  const lang = body.lang && body.lang !== "auto" ? resolveLanguage(body.lang, text) : resolveLanguage("auto", text || body.topic || "");
  const prompt = creatorPrompt(task, { text, topic: body.topic, lang, kind: body.kind === "post" ? "post" : "reel" });
  let g = null;
  if (task === "moderate") g = await guard(env, text);
  const out = await complete(env, [{ role: "system", content: CREATOR_SYSTEM }, { role: "user", content: prompt }],
    { maxTokens: task === "subtitles" || task === "translate" ? 1500 : 600, temperature: task === "translate" || task === "subtitles" || task === "moderate" ? 0.1 : 0.7, totalMs: 40_000, fast: task !== "moderate", lang });
  if (!out) {
    if (task === "moderate" && g) return json(200, { task, verdict: g.safe ? "ok" : "block", issues: g.safe ? [] : ["Flagged by the safety check"], suggestion: "", partial: true });
    return json(503, { error: "unavailable" });
  }
  const t = out.text;
  const base = { task, language: lang, provider: out.provider, model: out.model };
  switch (task) {
    case "captions": return json(200, { ...base, items: parseList(t, 3) });
    case "hooks": return json(200, { ...base, items: parseList(t, 5) });
    case "hashtags": {
      const tags = [...new Set((t.match(/#[\p{L}\p{N}_]{2,40}/gu) || []))].slice(0, 12);
      return json(200, { ...base, items: tags });
    }
    case "translate": return json(200, { ...base, text: t.replace(/^"""|"""$/g, "").trim() });
    case "subtitles": {
      const lines = t.split("\n").map((l) => /^\s*(\d+)\s*\|\s*(.*)$/.exec(l)).filter(Boolean).map((m) => ({ index: Number(m[1]), text: m[2].trim() }));
      return json(200, { ...base, lines });
    }
    case "moderate": {
      const j = parseJsonLoose(t) || {};
      let verdict = ["ok", "review", "block"].includes(j.verdict) ? j.verdict : "review";
      if (g && !g.safe) verdict = "block";
      const issues = Array.isArray(j.issues) ? j.issues.map(String).filter(Boolean).slice(0, 6) : [];
      return json(200, { ...base, verdict, issues, suggestion: String(j.suggestion || "").slice(0, 2000), guard: g ? (g.safe ? "safe" : "unsafe") : "n/a" });
    }
  }
  return json(400, { error: "task" });
}

function vttTime(s) {
  const ms = Math.max(0, Math.round(Number(s || 0) * 1000));
  const h = Math.floor(ms / 3600000), m = Math.floor((ms % 3600000) / 60000), sec = Math.floor((ms % 60000) / 1000), r = ms % 1000;
  return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:${String(sec).padStart(2, "0")}.${String(r).padStart(3, "0")}`;
}

export async function handleTranscribe(body, env) {
  const audio = typeof body.audio === "string" ? body.audio : "";
  if (!audio || audio.length > MAX_AUDIO_B64) return json(400, { error: "audio" });
  if (!env.AI) return json(503, { error: "unavailable" });
  const input = { audio, task: "transcribe", vad_filter: true };
  if (typeof body.lang === "string" && /^[a-z]{2}$/.test(body.lang)) input.language = body.lang;
  let res;
  try { res = await env.AI.run("@cf/openai/whisper-large-v3-turbo", input); } catch { return json(503, { error: "unavailable" }); }
  const segments = (res?.segments || []).map((s) => ({ start: Number(s.start) || 0, end: Number(s.end) || 0, text: String(s.text || "").trim() })).filter((s) => s.text);
  const vtt = "WEBVTT\n\n" + segments.map((s, i) => `${i + 1}\n${vttTime(s.start)} --> ${vttTime(s.end)}\n${s.text}\n`).join("\n");
  return json(200, { text: String(res?.text || "").trim(), language: res?.transcription_info?.language || "", segments, vtt });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "GET" && (url.pathname === "/" || url.pathname === "/health"))
      return json(200, { ok: true, service: "noorpro-ai", gemini: !!env.GEMINI_API_KEY, groq: !!env.GROQ_API_KEY, zhipu: !!env.ZHIPU_API_KEY, workersAi: !!env.AI, kb: KB_SIZE, features: ["chat", "research", "creator", "transcribe", "tts", "image"] });
    if (request.method !== "POST") return new Response("Method Not Allowed", { status: 405, headers: { allow: "GET, POST" } });
    const route = { "/v1/chat": "chat", "/v1/research": "research", "/v1/creator": "creator", "/v1/transcribe": "transcribe", "/v1/tts": "tts", "/v1/image": "image" }[url.pathname];
    if (!route) return json(404, { error: "not_found" });
    const len = Number(request.headers.get("content-length") || 0);
    const cap = route === "transcribe" ? MAX_AUDIO_B64 + 4096 : MAX_BODY;
    if (len > cap) return json(413, { error: "too_large" });
    const ident = await identify(request);
    if (!(await burstOk(env, ident))) return json(429, { error: "slow_down", retryAfter: 60 });
    let body;
    try {
      const raw = await request.text();
      if (raw.length > cap) return json(413, { error: "too_large" });
      body = JSON.parse(raw);
    } catch { return json(400, { error: "bad_json" }); }
    if (!body || typeof body !== "object") return json(400, { error: "bad_json" });
    if (route === "image") { if (!(await takeImageQuota(env, ident))) return json(429, { error: "daily_limit", kind: "image", tier: ident.tier }); }
    else if (route === "tts") { if (!(await takeTtsQuota(env, ident))) return json(429, { error: "daily_limit", kind: "tts", tier: ident.tier }); }
    else if (!(await takeQuota(env, ident, route === "chat" && body.tier === "research" ? "research" : route))) return json(429, { error: "daily_limit", tier: ident.tier });
    try {
      if (route === "chat" && body.tier === "research") {
        if ((request.headers.get("accept") || "").includes("text/event-stream")) return researchStream(body, env);
        const r = await runResearch(body, env); return json(r.status, r.body);
      }
      if (route === "research") {
        if ((request.headers.get("accept") || "").includes("text/event-stream")) return researchStream(body, env);
        const r = await runResearch(body, env); return json(r.status, r.body);
      }
      if (route === "chat") return await handleChat(body, env);
      if (route === "creator") return await handleCreator(body, env);
      if (route === "tts") return await handleTts(body, env);
      if (route === "image") return await handleImage(body, env);
      return await handleTranscribeV2(body, env, handleTranscribe);
    } catch { return json(500, { error: "internal" }); }
  },
};
