// NoorPro AI Worker: Noor AI chat, creator tools, subtitles. No secrets in the app; keys live only as Worker secrets.
import { identify } from "./auth.js";
import { resolveLanguage, LANG_NAMES } from "./lang.js";
import { chatSystemPrompt, passagesBlock, contextBlock, creatorPrompt, CREATOR_SYSTEM } from "./prompts.js";
import { complete } from "./llm.js";
import { takeQuota, burstOk } from "./quota.js";
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

export async function handleChat(body, env) {
  const prompt = String(body.prompt || body.message || "").trim().slice(0, 4000);
  if (!prompt) return json(400, { error: "empty" });
  const mode = MODES.has(body.mode) ? body.mode : "AskNoor";
  const lang = resolveLanguage(body.language, prompt);
  const passages = Array.isArray(body.passages) ? body.passages.slice(0, 6) : [];
  const userParts = [contextBlock(body.context), passagesBlock(passages), `Question: ${prompt}`].filter(Boolean).join("\n\n");
  const messages = [
    { role: "system", content: chatSystemPrompt(mode, lang) },
    ...cleanHistory(body.history),
    { role: "user", content: userParts },
  ];
  const out = await complete(env, messages, { maxTokens: mode === "DuaGenerator" ? 700 : 1400, temperature: mode === "DuaGenerator" ? 0.6 : 0.3, minLength: 20 });
  if (!out) return json(503, { error: "unavailable", language: lang });
  const { body: answer, fiqh, followups } = parseMeta(out.text);
  if (!answer) return json(503, { error: "unavailable", language: lang });
  return json(200, {
    answer, language: lang, languageName: LANG_NAMES[lang] || (lang === "hi-Latn" ? "Hinglish" : "English"),
    fiqh: fiqh === true && isRulingQuestion(prompt), followups, citations: citedPassages(answer, passages), provider: out.provider, model: out.model,
  });
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
    { maxTokens: task === "subtitles" || task === "translate" ? 1500 : 600, temperature: task === "translate" || task === "subtitles" || task === "moderate" ? 0.1 : 0.7, totalMs: 40_000, fast: task !== "moderate" });
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
      return json(200, { ok: true, service: "noorpro-ai", groq: !!env.GROQ_API_KEY, workersAi: !!env.AI });
    if (request.method !== "POST") return new Response("Method Not Allowed", { status: 405, headers: { allow: "GET, POST" } });
    const route = { "/v1/chat": "chat", "/v1/creator": "creator", "/v1/transcribe": "transcribe" }[url.pathname];
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
    if (!(await takeQuota(env, ident, route))) return json(429, { error: "daily_limit", tier: ident.tier });
    try {
      if (route === "chat") return await handleChat(body, env);
      if (route === "creator") return await handleCreator(body, env);
      return await handleTranscribe(body, env);
    } catch { return json(500, { error: "internal" }); }
  },
};
