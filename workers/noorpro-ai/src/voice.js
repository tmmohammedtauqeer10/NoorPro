// Voice: natural text-to-speech (Gemini TTS free tier, then Workers AI MeloTTS for supported languages) and
// speech-to-text with language detection (Groq Whisper large-v3, then Workers AI Whisper).
import { detectLanguage } from "./lang.js";

const json = (status, obj) => new Response(JSON.stringify(obj), { status, headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" } });
export const TTS_MODELS = ["gemini-3.8-flash-tts", "gemini-3.8-flash-lite-tts", "gemini-3.1-flash-tts-preview"];
const VOICES = { female: "Kore", male: "Charon", calm: "Sulafat" };
const MELO_LANGS = new Set(["en", "fr", "es", "zh", "ja", "ko"]);
const ttsCooldown = new Map();

export function pcmToWav(pcm, rate = 24000, channels = 1, bits = 16) {
  const h = new ArrayBuffer(44); const d = new DataView(h);
  const w = (o, s) => { for (let i = 0; i < s.length; i++) d.setUint8(o + i, s.charCodeAt(i)); };
  w(0, "RIFF"); d.setUint32(4, 36 + pcm.length, true); w(8, "WAVE"); w(12, "fmt "); d.setUint32(16, 16, true);
  d.setUint16(20, 1, true); d.setUint16(22, channels, true); d.setUint32(24, rate, true); d.setUint32(28, rate * channels * bits / 8, true);
  d.setUint16(32, channels * bits / 8, true); d.setUint16(34, bits, true); w(36, "data"); d.setUint32(40, pcm.length, true);
  const out = new Uint8Array(44 + pcm.length); out.set(new Uint8Array(h), 0); out.set(pcm, 44); return out;
}
const b64ToBytes = (b64) => { const s = atob(b64); const u = new Uint8Array(s.length); for (let i = 0; i < s.length; i++) u[i] = s.charCodeAt(i); return u; };
function bytesToB64(u) { let s = ""; const C = 0x8000; for (let i = 0; i < u.length; i += C) s += String.fromCharCode.apply(null, u.subarray(i, i + C)); return btoa(s); }

/** Cleans markdown/citation markers so they are not read aloud. */
export function speakable(text) {
  return String(text || "").replace(/\[K\d+\]/g, "").replace(/[*_#>`|]/g, "").replace(/\[(.*?)\]\((.*?)\)/g, "$1").replace(/\n{2,}/g, "\n").trim();
}

async function geminiTts(env, model, text, voice) {
  const r = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`, {
    method: "POST", headers: { "content-type": "application/json", "x-goog-api-key": env.GEMINI_API_KEY },
    body: JSON.stringify({ contents: [{ parts: [{ text }] }], generationConfig: { responseModalities: ["AUDIO"], speechConfig: { voiceConfig: { prebuiltVoiceConfig: { voiceName: voice } } } } }),
  });
  if (r.status === 429) { ttsCooldown.set(model, Date.now() + 60_000); return null; }
  if (!r.ok) return null;
  const p = (await r.json()).candidates?.[0]?.content?.parts?.find((x) => x.inlineData)?.inlineData;
  if (!p?.data) return null;
  if (/wav/i.test(p.mimeType || "")) return p.data;
  const rate = Number((/rate=(\d+)/.exec(p.mimeType || "") || [])[1]) || 24000;
  return bytesToB64(pcmToWav(b64ToBytes(p.data), rate));
}

export async function handleTts(body, env) {
  const text = speakable(body.text).slice(0, 700);
  if (!text) return json(400, { error: "empty" });
  const lang = (body.lang && body.lang !== "auto" ? String(body.lang) : detectLanguage(text)).split("-")[0];
  const voice = VOICES[body.voice] || VOICES.female;
  if (env.GEMINI_API_KEY) {
    for (const model of TTS_MODELS) {
      if ((ttsCooldown.get(model) || 0) > Date.now()) continue;
      try { const audio = await geminiTts(env, model, text, voice); if (audio) return json(200, { audio, mime: "audio/wav", provider: "gemini", model, lang }); } catch {}
    }
  }
  if (env.AI && MELO_LANGS.has(lang)) {
    try {
      const r = await env.AI.run("@cf/myshell-ai/melotts", { prompt: text, lang });
      if (typeof r?.audio === "string") return json(200, { audio: r.audio, mime: "audio/mpeg", provider: "workers-ai", model: "melotts", lang });
    } catch {}
  }
  return json(503, { error: "unavailable", fallback: "device" });
}

const WHISPER_LANG = { english: "en", urdu: "ur", hindi: "hi", arabic: "ar", bengali: "bn", persian: "fa", turkish: "tr", indonesian: "id", malay: "ms", french: "fr", tamil: "ta", malayalam: "ml", telugu: "te" };
export function voiceLanguage(text, whisperLang) {
  const byScript = detectLanguage(text);
  if (byScript !== "en") return byScript;               // script is decisive for ur/ar/hi/bn/...; Roman Urdu too
  const w = WHISPER_LANG[String(whisperLang || "").toLowerCase()] || String(whisperLang || "").slice(0, 2).toLowerCase();
  return /^[a-z]{2}$/.test(w) ? w : "en";
}

async function groqWhisper(env, b64, lang, format = "m4a") {
  const fd = new FormData();
  const fmt = { wav: ["audio/wav", "audio.wav"], ogg: ["audio/ogg", "audio.ogg"], webm: ["audio/webm", "audio.webm"], mp3: ["audio/mpeg", "audio.mp3"] }[format] || ["audio/mp4", "audio.m4a"];
  fd.append("file", new Blob([b64ToBytes(b64)], { type: fmt[0] }), fmt[1]);
  fd.append("model", "whisper-large-v3");
  fd.append("response_format", "verbose_json");
  if (lang) fd.append("language", lang);
  const ctrl = new AbortController(); const t = setTimeout(() => ctrl.abort(), 25_000);
  try {
    const r = await fetch("https://api.groq.com/openai/v1/audio/transcriptions", { method: "POST", signal: ctrl.signal, headers: { authorization: `Bearer ${env.GROQ_API_KEY}` }, body: fd });
    if (!r.ok) return null;
    return await r.json();
  } catch { return null; } finally { clearTimeout(t); }
}

/** Groq Whisper large-v3 first (best language ID), then the Workers AI Whisper handler. */
export async function handleTranscribeV2(body, env, fallback) {
  const audio = typeof body.audio === "string" ? body.audio : "";
  const lang = typeof body.lang === "string" && /^[a-z]{2}$/.test(body.lang) ? body.lang : "";
  if (audio && env.GROQ_API_KEY && audio.length <= 6 * 1024 * 1024) {
    const g = await groqWhisper(env, audio, lang, String(body.format || "m4a"));
    if (g && typeof g.text === "string" && g.text.trim()) {
      const text = g.text.trim();
      const segments = (g.segments || []).map((s) => ({ start: Number(s.start) || 0, end: Number(s.end) || 0, text: String(s.text || "").trim() })).filter((s) => s.text);
      const vtt = "WEBVTT\n\n" + segments.map((s, i) => `${i + 1}\n${vt(s.start)} --> ${vt(s.end)}\n${s.text}\n`).join("\n");
      return json(200, { text, language: hindustani(voiceLanguage(text, g.language), body.hint), whisperLanguage: g.language || "", segments, vtt, provider: "groq" });
    }
  }
  const res = await fallback(body, env);
  try {
    const j = await res.clone().json();
    if (res.status === 200) return json(200, { ...j, whisperLanguage: j.language, language: hindustani(voiceLanguage(j.text, j.language), body.hint), provider: "workers-ai" });
  } catch {}
  return res;
}
/** Spoken Hindi and Urdu are one language to Whisper; the user's app language (hint) decides the reply script. */
export function hindustani(detected, hint) {
  const h = typeof hint === "string" ? hint.toLowerCase().slice(0, 2) : "";
  if ((detected === "ur" || detected === "hi") && (h === "ur" || h === "hi")) return h;
  return detected;
}
function vt(s) {
  const ms = Math.max(0, Math.round(Number(s || 0) * 1000));
  const h = Math.floor(ms / 3600000), m = Math.floor((ms % 3600000) / 60000), sec = Math.floor((ms % 60000) / 1000), r = ms % 1000;
  return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:${String(sec).padStart(2, "0")}.${String(r).padStart(3, "0")}`;
}
