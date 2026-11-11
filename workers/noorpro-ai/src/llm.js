// Model routing (see buildOrder): Gemini (free AI Studio tier) first, then Groq / Workers AI, then Z.ai GLM (free
// Flash models only). Each provider is used only if its secret / binding exists.
// Live evaluation 2026-11-11 (Urdu Hajj, Arabic istikhara, Hindi Ramadan, English khushu):
//  - gemini-3.8-flash: best overall, Hanafi-correct Urdu (3 fara'id; sa'i, Muzdalifa, rami, qurbani, halq, tawaf
//    al-wada as wajibat; Arafat 9 Dhul Hijjah), excellent Arabic/Hindi, but slow (7-50 s) -> first for non-Latin.
//  - gemini-3.5-flash-lite: 2-6 s, accurate English/Arabic/Hindi; Urdu put sa'i under arkan -> first for English,
//    third for non-Latin (after Gemma 4).
//  - Z.ai glm-4.7-flash invented Hajj "in Ramadan" in Urdu; glm-4.5-flash Urdu omitted Arafat -> English only, last.
export const GEMINI_MODELS = { best: "gemini-3.8-flash", fast: "gemini-3.5-flash-lite" };
const GEMINI_THINKING = { "gemini-3.8-flash": "low", "gemini-3.5-flash-lite": "minimal" };
// Only the models Z.ai lists as free; anything else would be paid usage.
export const ZHIPU_FREE_MODELS = ["glm-4.7-flash", "glm-4.5-flash"];
// Groq free tier (live tests 2026-11-11): qwen3.8-27b gave fast (~1.5 s), mostly accurate Urdu and English;
// gpt-oss-120b was fine in English but invented Hajj facts in Urdu, so it is used for English/Latin script only.
// Groq free limits: 8k tokens/min and 1k requests/day per model, and output is capped near 1k tokens/min, so
// max_tokens is clamped and any 429 falls straight through to Workers AI.
export const GROQ_MODELS = ["qwen/qwen3.8-27b", "openai/gpt-oss-120b"];
export const GROQ_ENGLISH_ONLY = new Set(["openai/gpt-oss-120b", "openai/gpt-oss-20b"]);
export const GROQ_MAX_TOKENS = 950;
export const LATIN_LANGS = new Set(["en", "ur-Latn", "hi-Latn"]);
// Chosen after live Urdu/English/Roman-Urdu tests on 2026-11-11: Gemma 4 (thinking off) gave fluent, accurate
// Urdu at ~22 neurons/answer; gpt-oss-120b is the backup. llama-4-scout, mistral-small and gpt-oss-20b made
// serious factual errors about Hajj in Urdu, so they are not used for religious answers.
export const WORKERS_AI_MODELS = [
  "@cf/google/gemma-4-26b-a4b-it",
  "@cf/openai/gpt-oss-120b",
];
/** Order for short creator tasks (captions, hashtags, translation, moderation). */
export const WORKERS_AI_FAST = [
  "@cf/google/gemma-4-26b-a4b-it",
  "@cf/openai/gpt-oss-20b",
  "@cf/openai/gpt-oss-120b",
];

/** True when a non-Latin answer has Latin letters glued inside native words (e.g. "تمattuٰ"), a sign of a weak answer. */
export function hasMixedScriptWords(text, lang) {
  if (LATIN_LANGS.has(lang || "en")) return false;
  const hits = String(text).match(/[\u0600-\u06FF\u0900-\u0DFF][A-Za-z]{2,}|[A-Za-z]{2,}[\u0600-\u06FF\u0900-\u0DFF]/g);
  return (hits?.length || 0) >= 2;
}

export function cleanModelText(s) {
  return String(s || "").replace(/<think>[\s\S]*?<\/think>/gi, "").replace(/^\s*<\|[^>]*\|>\s*/g, "").trim();
}

function extractText(res) {
  if (res == null) return "";
  if (typeof res === "string") return res;
  if (typeof res.response === "string") return res.response;
  if (res.response && typeof res.response === "object") return JSON.stringify(res.response);
  const c = res.choices?.[0]?.message?.content;
  if (typeof c === "string") return c;
  if (Array.isArray(res.output)) {
    const parts = [];
    for (const o of res.output) for (const k of o.content || []) if (typeof k.text === "string" && (k.type === "output_text" || !k.type)) parts.push(k.text);
    if (parts.length) return parts.join("");
  }
  if (typeof res.result?.response === "string") return res.result.response;
  return "";
}

async function callGroq(env, model, messages, { maxTokens, temperature, timeoutMs }) {
  const ctrl = new AbortController();
  const t = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const r = await fetch("https://api.groq.com/openai/v1/chat/completions", {
      method: "POST", signal: ctrl.signal,
      headers: { "content-type": "application/json", authorization: `Bearer ${env.GROQ_API_KEY}` },
      body: JSON.stringify({ model, messages, max_tokens: Math.min(maxTokens, GROQ_MAX_TOKENS), temperature,
        ...(model.includes("gpt-oss") ? { reasoning_effort: "low" } : model.includes("qwen3") ? { reasoning_effort: "none" } : {}) }),
    });
    if (r.status === 429) { cool(`groq:${model}`, 30_000); return null; }
    if (!r.ok) return null;
    const j = await r.json();
    return cleanModelText(j.choices?.[0]?.message?.content);
  } catch { return null; } finally { clearTimeout(t); }
}

// 429 / overload cool-down per isolate so a rate-limited model is skipped for a while instead of retried each time.
const cooldown = new Map();
export function coolingDown(key) { const t = cooldown.get(key); return !!t && t > Date.now(); }
export function _resetCooldowns() { cooldown.clear(); }
function cool(key, ms) { cooldown.set(key, Date.now() + ms); }

async function callGemini(env, model, messages, { maxTokens, temperature, timeoutMs }) {
  const ctrl = new AbortController();
  const t = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const sys = messages.filter((m) => m.role === "system").map((m) => m.content).join("\n\n");
    const contents = messages.filter((m) => m.role !== "system").map((m) => ({ role: m.role === "assistant" ? "model" : "user", parts: [{ text: String(m.content) }] }));
    const r = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`, {
      method: "POST", signal: ctrl.signal,
      headers: { "content-type": "application/json", "x-goog-api-key": env.GEMINI_API_KEY },
      body: JSON.stringify({ ...(sys ? { systemInstruction: { parts: [{ text: sys }] } } : {}), contents,
        generationConfig: { maxOutputTokens: maxTokens, temperature, thinkingConfig: { thinkingLevel: GEMINI_THINKING[model] || "low" } } }),
    });
    if (r.status === 429 || r.status === 503) { cool(`gemini:${model}`, 60_000); return null; }
    if (!r.ok) return null;
    const j = await r.json();
    const parts = j.candidates?.[0]?.content?.parts || [];
    return cleanModelText(parts.filter((p) => !p.thought).map((p) => p.text || "").join(""));
  } catch { return null; } finally { clearTimeout(t); }
}

async function callZhipu(env, model, messages, { maxTokens, temperature, timeoutMs }) {
  if (!ZHIPU_FREE_MODELS.includes(model)) return null; // never call paid GLM models
  const ctrl = new AbortController();
  const t = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const r = await fetch("https://api.z.ai/api/paas/v4/chat/completions", {
      method: "POST", signal: ctrl.signal,
      headers: { "content-type": "application/json", authorization: `Bearer ${env.ZHIPU_API_KEY}` },
      body: JSON.stringify({ model, messages, max_tokens: maxTokens, temperature, thinking: { type: "disabled" } }),
    });
    if (r.status === 429) { cool(`zhipu:${model}`, 60_000); return null; }
    if (!r.ok) return null;
    const j = await r.json();
    return cleanModelText(j.choices?.[0]?.message?.content);
  } catch { return null; } finally { clearTimeout(t); }
}

async function callWorkersAi(env, model, messages, { maxTokens, temperature, timeoutMs }) {
  if (!env.AI) return null;
  const run = env.AI.run(model, model.includes("gpt-oss")
    ? { input: messages.filter((m) => m.role !== "system").map((m) => ({ role: m.role, content: m.content })),
        instructions: messages.filter((m) => m.role === "system").map((m) => m.content).join("\n\n"),
        reasoning: { effort: "low" }, max_output_tokens: maxTokens + 400 }
    : { messages, max_tokens: maxTokens, temperature, ...(model.includes("gemma-4") || model.includes("qwen3") ? { chat_template_kwargs: { enable_thinking: false } } : {}) });
  try {
    const res = await Promise.race([run, new Promise((_, rej) => setTimeout(() => rej(new Error("timeout")), timeoutMs))]);
    return cleanModelText(extractText(res));
  } catch { return null; }
}

/** Provider/model order for a request. Exported for tests. */
export function buildOrder(env, o = {}) {
  const latin = LATIN_LANGS.has(o.lang || "en");
  const gem = (m) => (env.GEMINI_API_KEY ? [["gemini", m]] : []);
  const groq = env.GROQ_API_KEY ? GROQ_MODELS.filter((m) => !GROQ_ENGLISH_ONLY.has(m) || latin).map((m) => ["groq", m]) : [];
  const waList = env.WORKERS_AI_MODELS ? String(env.WORKERS_AI_MODELS).split(",").map((s) => s.trim()).filter(Boolean) : (o.fast ? WORKERS_AI_FAST : WORKERS_AI_MODELS);
  const wa = env.AI ? waList.filter((m) => latin || !m.includes("gpt-oss-20b")).map((m) => ["workers-ai", m]) : [];
  const glm = env.ZHIPU_API_KEY && latin ? ZHIPU_FREE_MODELS.map((m) => ["zhipu", m]) : [];
  let order;
  if (o.tier === "research") {
    // Deepest answers: best Gemini first with a long timeout, then Gemma 4, then the rest.
    order = [...gem(GEMINI_MODELS.best), ...wa.slice(0, 1), ...gem(GEMINI_MODELS.fast), ...(latin ? groq : []), ...wa.slice(1), ...glm];
  } else if (o.tier === "fast" && !o.fast) {
    // Quickest accurate route: Flash-Lite (2-6 s), then Groq for Latin script, Gemma 4 for others.
    order = [...gem(GEMINI_MODELS.fast), ...(latin ? [...groq, ...wa] : [...wa.slice(0, 1), ...gem(GEMINI_MODELS.best), ...groq, ...wa.slice(1)]), ...glm];
  } else if (o.fast) order = [...gem(GEMINI_MODELS.fast), ...(latin ? [...groq, ...wa] : [...wa, ...groq]), ...gem(GEMINI_MODELS.best), ...glm];
  else if (latin) order = [...gem(GEMINI_MODELS.fast), ...groq, ...gem(GEMINI_MODELS.best), ...wa, ...glm];
  else if (env.NON_LATIN_PREFER === "groq") order = [...gem(GEMINI_MODELS.best), ...groq, ...wa.slice(0, 1), ...gem(GEMINI_MODELS.fast), ...wa.slice(1)];
  else order = [...gem(GEMINI_MODELS.best), ...wa.slice(0, 1), ...gem(GEMINI_MODELS.fast), ...groq, ...wa.slice(1)];
  return order;
}

const CALLERS = { gemini: callGemini, groq: callGroq, "workers-ai": callWorkersAi, zhipu: callZhipu };
// Per-call timeouts: slow-but-best Gemini gets a bounded slice so fallbacks still fit in the deadline.
const PROVIDER_TIMEOUT = { "gemini:gemini-3.8-flash": 25_000, "gemini:gemini-3.5-flash-lite": 10_000, "groq:qwen/qwen3.8-27b": 15_000, "groq:openai/gpt-oss-120b": 15_000 };
// gemini-3.8-flash is the most accurate for Urdu/Arabic/Hindi but its latency swings (7-50 s). For those requests it
// is raced against Gemma 4: Gemini's answer is preferred if it arrives within HEDGE_GRACE_MS after Gemma's.
export const HEDGE_GRACE_MS = 4_000;

function okText(text, o) { return !!text && text.length >= (o.minLength || 2) && !hasMixedScriptWords(text, o.lang); }

async function hedged(env, messages, o, first, second, left) {
  const call = ([p, m]) => CALLERS[p](env, m, messages, { ...o, timeoutMs: Math.min(PROVIDER_TIMEOUT[`${p}:${m}`] || o.timeoutMs, left) })
    .then((text) => (okText(text, o) ? { text, provider: p, model: m } : null), () => null);
  const a = call(first), b = call(second);
  const bRes = await Promise.race([b, a.then((r) => (r ? "A" : b))]);
  if (bRes === "A") return a; // preferred model finished first
  if (!bRes) return a;        // fallback failed: wait for the preferred one
  const aRes = await Promise.race([a, new Promise((r) => setTimeout(() => r(null), HEDGE_GRACE_MS))]);
  return aRes || bRes;
}

/** Tries each provider/model in order; returns {text, provider, model} or null. */
export async function complete(env, messages, opts = {}) {
  const o = { maxTokens: 1200, temperature: 0.3, timeoutMs: 25_000, ...opts };
  const order = buildOrder(env, o);
  const deadline = Date.now() + (o.totalMs || 50_000);
  let start = 0;
  if (!o.fast && o.tier !== "research" && o.tier !== "fast" && !LATIN_LANGS.has(o.lang || "en") && order[0]?.[1] === GEMINI_MODELS.best && order[1]?.[0] === "workers-ai"
      && !coolingDown(`gemini:${order[0][1]}`)) {
    const r = await hedged(env, messages, o, order[0], order[1], deadline - Date.now());
    if (r) return r;
    start = 2;
  }
  for (let i = start; i < order.length; i++) {
    const [provider, model] = order[i];
    if (coolingDown(`${provider}:${model}`)) continue;
    const left = deadline - Date.now();
    if (left < 3000) break;
    const pt = o.tier === "research" && provider === "gemini" && model === GEMINI_MODELS.best ? 60_000 : PROVIDER_TIMEOUT[`${provider}:${model}`];
    const text = await CALLERS[provider](env, model, messages, { ...o, timeoutMs: Math.min(pt || o.timeoutMs, left) });
    if (text && text.length >= (o.minLength || 2)) {
      // Mixed-script guard: a non-Latin answer with Latin letters glued into words is retried on the next model.
      if (hasMixedScriptWords(text, o.lang) && i < order.length - 1) continue;
      return { text, provider, model };
    }
  }
  return null;
}
