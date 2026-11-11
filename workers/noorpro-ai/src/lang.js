// Language/script detection for Noor AI (kept in sync with the app's NoorAiLanguage.kt).
const URDU_ONLY = /[\u0679\u0688\u0691\u06BA\u06BE\u06C1\u06C3\u06D2\u06D3]/; // ٹ ڈ ڑ ں ھ ہ ۃ ے ۓ
const URDU_LEAN = /[\u067E\u0686\u0698\u06AF\u06A9\u06CC]/;                 // پ چ ژ گ ک ی (also Persian)
const ROMAN_URDU = new Set(("kya kia hai hain hy he mujhe muje mujhko mera meri mere tum aap ap apko kaise kese kaisay kyun kyon kyu " +
  "nahi nahin nai nhi karna karo karein karen kar raha rahi hota hoti hogi hoga mein main me bhi aur ya ke ka ki ko se par pe " +
  "batao bataen bataiye bataye chahiye chahte sakta sakti sakte liye wala wali kab kahan kitna kitni jab tab agar lekin magar " +
  "namaz namaaz roza rozay wazu wuzu dua duaa gunah sawab jannat jahannam bare baare baray bara janna jaanna jaanana").split(" "));
const HINGLISH_HINTS = new Set("kripya bhagwan dhanyavad".split(" "));

export const LANG_NAMES = {
  en: "English", ur: "Urdu", "ur-Latn": "Roman Urdu", hi: "Hindi", ar: "Arabic", bn: "Bengali",
  fa: "Persian", id: "Indonesian", ms: "Malay", tr: "Turkish", fr: "French", ta: "Tamil", ml: "Malayalam", te: "Telugu",
};

/** Returns a language code for the question text: ur, ar, hi, bn, ta, ml, te, ur-Latn or en. */
export function detectLanguage(text) {
  const t = String(text || "");
  const count = (re) => (t.match(re) || []).length;
  const arabicScript = count(/[\u0600-\u06FF\u0750-\u077F\uFB50-\uFDFF\uFE70-\uFEFF]/g);
  const deva = count(/[\u0900-\u097F]/g);
  const beng = count(/[\u0980-\u09FF]/g);
  const tamil = count(/[\u0B80-\u0BFF]/g);
  const mal = count(/[\u0D00-\u0D7F]/g);
  const tel = count(/[\u0C00-\u0C7F]/g);
  const latin = count(/[A-Za-z]/g);
  const best = Math.max(arabicScript, deva, beng, tamil, mal, tel, latin);
  if (best === 0) return "en";
  if (best === arabicScript) {
    if (URDU_ONLY.test(t)) return "ur";
    // Persian-style letters without Urdu-only letters: Urdu is far more common in NoorPro's audience.
    if (URDU_LEAN.test(t) && /(ہے|میں|کے|کی|کا|کیا|نہیں|مجھے|کر)/.test(t)) return "ur";
    if (URDU_LEAN.test(t) && !/[ةىأإآ]/.test(t)) return "ur";
    return "ar";
  }
  if (best === deva) return "hi";
  if (best === beng) return "bn";
  if (best === tamil) return "ta";
  if (best === mal) return "ml";
  if (best === tel) return "te";
  const words = t.toLowerCase().split(/[^a-z']+/).filter(Boolean);
  const hits = words.filter((w) => ROMAN_URDU.has(w)).length;
  if (words.length && (hits >= 2 && hits / words.length >= 0.2)) return words.some((w) => HINGLISH_HINTS.has(w)) ? "hi-Latn" : "ur-Latn";
  return "en";
}

export function languageInstruction(code) {
  switch (code) {
    case "ur": return "Write the whole answer in Urdu, in Urdu (Nastaliq/Arabic) script. Keep Qur'an quotes in Arabic with an Urdu translation.";
    case "ur-Latn": return "The user wrote in Roman Urdu (Urdu in Latin letters). Write the whole answer in simple Roman Urdu (Latin letters), not in Urdu script and not in English.";
    case "hi-Latn": return "The user wrote in Hinglish (Hindi in Latin letters). Answer in simple Hinglish in Latin letters.";
    case "hi": return "Write the whole answer in Hindi (Devanagari script). Keep Qur'an quotes in Arabic with a Hindi translation.";
    case "ar": return "Write the whole answer in clear Modern Standard Arabic.";
    case "bn": return "Write the whole answer in Bengali (Bangla script).";
    case "ta": return "Write the whole answer in Tamil.";
    case "ml": return "Write the whole answer in Malayalam.";
    case "te": return "Write the whole answer in Telugu.";
    default: return LANG_NAMES[code] && code !== "en" ? `Write the whole answer in ${LANG_NAMES[code]}.` : "Write the answer in clear, simple English.";
  }
}

/** Resolve requested language: explicit code wins unless it is "auto"/empty; then detect from the prompt. */
export function resolveLanguage(requested, prompt) {
  const r = String(requested || "").trim();
  if (!r || r === "auto") return detectLanguage(prompt);
  return r in LANG_NAMES || r === "hi-Latn" ? r : detectLanguage(prompt);
}
