// Retrieval over the bundled Sunni knowledge base (src/kb_data.js): Gemini embeddings (multilingual) with a
// keyword fallback, so Urdu/Arabic/Hindi questions find the English summaries too.
import { KB, KB_DIM } from "./kb_data.js";

export const MADHHABS = ["hanafi", "shafii", "maliki", "hanbali"];
export const MADHHAB_NAMES = { hanafi: "Hanafi", shafii: "Shafi'i", maliki: "Maliki", hanbali: "Hanbali", compare: "all four madhhabs" };
export function normMadhhab(m) {
  const s = String(m || "").toLowerCase().replace(/[^a-z]/g, "");
  if (s === "shafi" || s === "shafii") return "shafii";
  if (s === "compare" || s === "all" || s === "allfour") return "compare";
  return MADHHABS.includes(s) ? s : "hanafi";
}

let vectors = null;
function decode() {
  if (vectors) return vectors;
  vectors = KB.map((e) => {
    const bin = atob(e.v); const bytes = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    return new Float32Array(bytes.buffer);
  });
  return vectors;
}

export async function embedQuery(env, text, timeoutMs = 4000) {
  if (!env.GEMINI_API_KEY) return null;
  const ctrl = new AbortController(); const t = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const r = await fetch("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent", {
      method: "POST", signal: ctrl.signal, headers: { "content-type": "application/json", "x-goog-api-key": env.GEMINI_API_KEY },
      body: JSON.stringify({ model: "models/gemini-embedding-001", content: { parts: [{ text: String(text).slice(0, 2000) }] }, taskType: "RETRIEVAL_QUERY", outputDimensionality: KB_DIM }),
    });
    if (!r.ok) return null;
    const v = (await r.json()).embedding?.values;
    if (!Array.isArray(v) || v.length !== KB_DIM) return null;
    const n = Math.hypot(...v) || 1;
    return Float32Array.from(v, (x) => x / n);
  } catch { return null; } finally { clearTimeout(t); }
}

export function keywordScore(e, q) {
  const s = String(q).toLowerCase();
  let hits = 0;
  for (const tag of e.tags || []) if (tag.length >= 3 && s.includes(tag.toLowerCase())) hits += tag.includes(" ") ? 2 : 1;
  return Math.min(hits, 4) / 4;
}

/**
 * Top-k KB entries: cosine similarity (if available) blended with tag hits. gemini-embedding-001 gives unrelated
 * entries ~0.64-0.74, so an entry is kept only if the best match is >= 0.75 and it is within `spread` of the best.
 */
export function rank(queryVec, q, k = 4, spread = 0.07) {
  const vecs = decode();
  const scored = KB.map((e, i) => {
    let cos = 0;
    if (queryVec) { const v = vecs[i]; for (let j = 0; j < v.length; j++) cos += v[j] * queryVec[j]; }
    const kw = keywordScore(e, q);
    const score = queryVec ? cos + 0.15 * kw : kw;
    return { e, score };
  }).sort((a, b) => b.score - a.score);
  const top = scored[0]?.score || 0;
  const keep = queryVec ? (top >= 0.75 ? scored.filter((x) => x.score >= Math.max(0.72, top - spread)) : [])
    : scored.filter((x) => x.score >= 0.25);
  return keep.slice(0, k).map((x) => ({ ...x.e, score: Math.round(x.score * 1000) / 1000 }));
}

export async function retrieve(env, query, k = 4, spread = 0.07) {
  const qv = await embedQuery(env, query);
  return rank(qv, query, k, spread);
}

/** Text block for the model: the chosen madhhab's position first, then the others when comparing. */
export function kbBlock(entries, madhhab = "hanafi") {
  if (!entries.length) return "";
  const lines = entries.map((e, i) => {
    let body = e.text || e.common || "";
    if (e.positions) {
      const order = madhhab === "compare" ? MADHHABS : [madhhab, ...MADHHABS.filter((m) => m !== madhhab)];
      const pos = order.map((m) => `${MADHHAB_NAMES[m]}${m === madhhab ? " (user's madhhab)" : ""}: ${e.positions[m]}`).join("\n  ");
      body = `${e.common}\n  ${pos}`;
    }
    return `[K${i + 1}] ${e.topic}\n  ${body}\n  Sources: ${(e.src || []).join("; ")}`;
  });
  return `NoorPro knowledge base (verified Sunni summaries; cite as [K1], [K2]... and name their classical sources):\n${lines.join("\n")}`;
}

/** "Sources" references for the app: the KB entries the answer cited, else (for fiqh) the top entry. */
export function kbCitations(answer, entries) {
  const used = entries.filter((_, i) => new RegExp(`\\[K${i + 1}\\]`).test(answer));
  return used.map((e) => `${e.topic}: ${(e.src || []).slice(0, 3).join("; ")}`);
}
export const KB_SIZE = KB.length;
