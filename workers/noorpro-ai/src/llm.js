// Model routing: Groq free tier first (only if GROQ_API_KEY secret is set), then Workers AI models.
// Groq free tier (live tests 2026-11-11): qwen3.8-27b gave fast (~1.5 s), mostly accurate Urdu and English;
// gpt-oss-120b was fine in English but invented Hajj facts in Urdu, so it is used for English/Latin script only.
// Groq free limits: 8k tokens/min and 1k requests/day per model, and output is capped near 1k tokens/min, so
// max_tokens is clamped and any 429 falls straight through to Workers AI.
export const GROQ_MODELS = ["qwen/qwen3.8-27b", "openai/gpt-oss-120b"];
export const GROQ_ENGLISH_ONLY = new Set(["openai/gpt-oss-120b", "openai/gpt-oss-20b"]);
export const GROQ_MAX_TOKENS = 950;
const LATIN_LANGS = new Set(["en", "ur-Latn", "hi-Latn"]);
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

/** Tries each provider/model in order; returns {text, provider, model} or null. */
export async function complete(env, messages, opts = {}) {
  const o = { maxTokens: 1200, temperature: 0.3, timeoutMs: 25_000, ...opts };
  const order = [];
  if (env.GROQ_API_KEY) for (const m of GROQ_MODELS) if (!GROQ_ENGLISH_ONLY.has(m) || LATIN_LANGS.has(o.lang || "en")) order.push(["groq", m]);
  const wa = (env.WORKERS_AI_MODELS ? String(env.WORKERS_AI_MODELS).split(",").map((s) => s.trim()).filter(Boolean) : (o.fast ? WORKERS_AI_FAST : WORKERS_AI_MODELS));
  for (const m of wa) order.push(["workers-ai", m]);
  // Optional: env NON_LATIN_PREFER="workers-ai" puts Workers AI (Gemma 4) ahead of Groq for Urdu/Arabic/Hindi etc.
  if (env.NON_LATIN_PREFER === "workers-ai" && !LATIN_LANGS.has(o.lang || "en")) order.sort((a, b) => (a[0] === "groq") - (b[0] === "groq"));
  const deadline = Date.now() + (o.totalMs || 45_000);
  for (const [provider, model] of order) {
    const left = deadline - Date.now();
    if (left < 3000) break;
    const fn = provider === "groq" ? callGroq : callWorkersAi;
    const text = await fn(env, model, messages, { ...o, timeoutMs: Math.min(o.timeoutMs, left) });
    if (text && text.length >= (o.minLength || 2)) {
      if (provider === "groq" && hasMixedScriptWords(text, o.lang) && order.some(([p]) => p === "workers-ai")) continue;
      return { text, provider, model };
    }
  }
  return null;
}
