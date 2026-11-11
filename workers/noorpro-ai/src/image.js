// Islamic image generation (free Workers AI flux-1-schnell) with server-side guidelines:
// no depictions of prophets, Sahabah, Ahl al-Bayt, awliya or any living beings (people, faces, animals),
// nothing immodest and no non-Islamic religious imagery. Checked before (keywords + model) and after (vision check).
import { complete } from "./llm.js";
import { resolveLanguage } from "./lang.js";

export const IMAGE_MODEL = "@cf/black-forest-labs/flux-1-schnell";
const json = (status, obj) => new Response(JSON.stringify(obj), { status, headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" } });

// Hard blocks: words that always mean a living being, an immodest or a non-Islamic religious image.
const BLOCK = [
  /\b(person|people|persons|man|men|woman|women|girl|girls|boy|boys|child|children|kid|kids|baby|human|humans|face|faces|portrait|selfie|body|bodies|crowd|family|couple|bride|groom|hijabi|muslimah|sheikh|shaykh|imam praying|someone|figure|silhouette of a (man|woman|person))\b/i,
  /\b(prophet|prophets|nabi|rasul|messenger of allah|sahaba|sahabah|companions?|ahl al-bayt|ahlul bayt|wali|awliya|saint|angel|angels|jibril|gabriel)\b/i,
  /\b(animal|animals|cat|cats|dog|dogs|horse|horses|camel|camels|bird|birds|eagle|falcon|dove|doves|pigeon|lion|tiger|deer|sheep|goat|cow|fish|butterfly|butterflies|insect|bee|bees|spider|snake|elephant|monkey|pet)\b/i,
  /\b(nude|naked|sexy|bikini|lingerie|cleavage|seductive|kiss|kissing|dating)\b/i,
  /\b(cross|crucifix|jesus|christ|idol|idols|deity|deities|buddha|ganesh|ganesha|krishna|shiva|church|temple|christmas|santa|wine|beer|alcohol|casino|gambling|tarot|zodiac)\b/i,
  /(انسان|آدمی|مرد|عورت|خواتین|لوگ|لڑکی|لڑکا|بچہ|بچے|بچوں|چہرہ|چہرے|جانور|پرند|تتلی|مچھلی|شیر|بلی|کتا|گھوڑا|اونٹ|نبی|رسول|صحابہ|فرشتہ|رجل|امرأة|وجه|حيوان|طائر|قطة|كلب|حصان|جمل|صحابة|ملاك)/,
  /(आदमी|औरत|लड़की|लड़का|बच्चा|चेहरा|जानवर|पक्षी|बिल्ली|कुत्ता|घोड़ा|ऊंट|नबी|फ़रिश्ता|मूर्ति|भगवान)/,
];
/** Place names that mention a prophet but describe architecture only. */
const PLACE_OK = /(prophet'?s'? mosque|mosque of the prophet|masjid (an|al)[- ]?nabawi|al[- ]?masjid an[- ]?nabawi|nabawi mosque|مسجد نبوی|مسجدِ نبوی|المسجد النبوي|मस्जिद-?ए-? ?नबवी)/gi;
export function blockedByKeywords(prompt) { const p = String(prompt || "").replace(PLACE_OK, "masjid"); return BLOCK.some((r) => r.test(p)); }

export const STYLES = {
  calligraphy: "elegant Islamic calligraphy-inspired poster, flowing decorative abstract Arabic-style strokes without legible words, gold on deep green, ornamental border",
  mosque: "majestic mosque architecture with domes and minarets, completely empty deserted courtyard, clear sky without birds, architectural photography, soft light",
  nature: "serene natural landscape, mountains, sky, water and trees, no animals or people",
  geometric: "intricate Islamic geometric pattern (girih/arabesque), symmetrical tessellation",
  reel: "vertical-friendly soft gradient background with subtle Islamic geometric motifs and empty space in the centre for text",
  card: "minimal share-card background, soft colours, subtle arabesque corners, large empty centre for text",
};

const REFUSAL = {
  en: "Sorry, I can't create that. Noor AI only makes images without people, faces, animals or other living beings (and never depicts prophets, Sahabah, Ahl al-Bayt or awliya), and nothing immodest or non-Islamic. Try calligraphy art, a mosque, nature, a geometric pattern or a background.",
  ur: "معذرت، یہ تصویر نہیں بنائی جا سکتی۔ نور اے آئی صرف ایسی تصاویر بناتا ہے جن میں انسان، چہرے، جانور یا کوئی جاندار نہ ہو (انبیاء، صحابہ، اہلِ بیت اور اولیاء کی تصویر کبھی نہیں)، اور کوئی غیر اسلامی یا نامناسب چیز نہ ہو۔ خطاطی، مسجد، قدرتی منظر، اسلامی نقش یا بیک گراؤنڈ آزمائیں۔",
  hi: "माफ़ कीजिए, यह तस्वीर नहीं बन सकती। नूर एआई सिर्फ़ ऐसी तस्वीरें बनाता है जिनमें इंसान, चेहरे, जानवर या कोई जानदार न हो (अंबिया, सहाबा, अहले बैत और औलिया की तस्वीर कभी नहीं), और कुछ भी ग़ैर-इस्लामी या अनुचित न हो। ख़त्ताती, मस्जिद, क़ुदरती नज़ारा या इस्लामी पैटर्न आज़माएं।",
  ar: "عذرًا، لا يمكن إنشاء هذه الصورة. نور يصنع صورًا فقط بلا أشخاص أو وجوه أو حيوانات أو أي كائن حي (ولا يصوّر الأنبياء ولا الصحابة ولا آل البيت ولا الأولياء)، وبلا محتوى غير لائق أو غير إسلامي. جرّب الخط العربي أو مسجدًا أو منظرًا طبيعيًا أو زخرفة هندسية.",
};
export function refusalText(lang) { return REFUSAL[lang] || REFUSAL[String(lang).split("-")[0]] || REFUSAL.en; }

async function classify(env, prompt) {
  const out = await complete(env, [{ role: "user", content:
`You check image requests for an Islamic app. Rules: no depiction of any prophet, Sahabi, member of Ahl al-Bayt, wali or angel; no people, faces, hands, bodies or silhouettes of people; no animals, birds, insects or any living creature; nothing immodest; no non-Islamic religious symbols or idols, alcohol or gambling. Allowed: calligraphy-style art, mosques and Islamic architecture, Kaaba and Masjid an-Nabawi architecture, nature without animals, geometric patterns, lanterns, books, prayer mats, backgrounds.
Request: """${String(prompt).slice(0, 500)}"""
Write the safe prompt without naming any person (say "Masjid an-Nabawi" for the Prophet's Mosque).
Return ONLY JSON: {"allow": true|false, "prompt": "a safe, vivid English image prompt for the allowed request (empty if not allowed)"}` }],
    { maxTokens: 220, temperature: 0, fast: true, lang: "en", totalMs: 15_000 });
  const m = /\{[\s\S]*\}/.exec(out?.text || "");
  if (!m) return null;
  try { const j = JSON.parse(m[0]); return { allow: j.allow === true, prompt: String(j.prompt || "").slice(0, 600) }; } catch { return null; }
}

/** Vision check of the generated image. Returns true (clean), false (contains forbidden content) or null (unknown). */
export async function imageIsClean(env, b64) {
  if (env.GEMINI_API_KEY) {
    try {
      const r = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${"gemini-3.5-flash-lite"}:generateContent`, {
        method: "POST", headers: { "content-type": "application/json", "x-goog-api-key": env.GEMINI_API_KEY },
        body: JSON.stringify({ contents: [{ parts: [{ inlineData: { mimeType: "image/jpeg", data: b64 } },
          { text: 'Look carefully. Does this image contain any human or human-like figure, face, hand, body or silhouette, any animal, bird, insect or other living creature, any nudity, or any cross, idol or non-Islamic religious symbol? Return ONLY JSON {"forbidden": true|false, "what": "short"}' }] }],
          generationConfig: { maxOutputTokens: 80, temperature: 0, thinkingConfig: { thinkingLevel: "minimal" } } }),
      });
      if (r.ok) {
        const t = (await r.json()).candidates?.[0]?.content?.parts?.map((p) => p.text || "").join("") || "";
        const m = /\{[\s\S]*\}/.exec(t);
        if (m) { const j = JSON.parse(m[0]); if (typeof j.forbidden === "boolean") return !j.forbidden; }
      }
    } catch {}
  }
  return null;
}

async function generate(env, prompt) {
  const res = await env.AI.run(IMAGE_MODEL, { prompt, steps: 6 });
  return typeof res?.image === "string" ? res.image : null;
}

export async function handleImage(body, env) {
  const raw = String(body.prompt || "").trim().slice(0, 600);
  const style = STYLES[body.style] ? body.style : "calligraphy";
  const lang = resolveLanguage(body.language || "auto", raw || "x");
  if (!raw && !body.style) return json(400, { error: "empty" });
  if (blockedByKeywords(raw)) return json(200, { refused: true, reason: "guidelines", message: refusalText(lang) });
  if (!env.AI) return json(503, { error: "unavailable" });
  const verdict = raw ? await classify(env, raw) : { allow: true, prompt: "" };
  if (!verdict) return json(503, { error: "unavailable" });
  if (!verdict.allow || blockedByKeywords(verdict.prompt)) return json(200, { refused: true, reason: "guidelines", message: refusalText(lang) });
  const base = `${verdict.prompt || raw || ""}. Style: ${STYLES[style]}. Strictly no people, no human figures, no faces, no hands, no animals, no birds, no living creatures, no readable text, no watermark. High detail, beautiful lighting.`;
  for (let attempt = 0; attempt < 2; attempt++) {
    let img;
    try { img = await generate(env, attempt ? `${base} Absolutely empty of any figures or creatures; architecture, nature and ornament only.` : base); } catch { img = null; }
    if (!img) return json(503, { error: "unavailable" });
    const clean = await imageIsClean(env, img);
    if (clean === true) return json(200, { image: img, mime: "image/jpeg", width: 1024, height: 1024, style, model: IMAGE_MODEL, checked: true });
    if (clean === null) return json(503, { error: "check_unavailable", message: "The image safety check is not available right now. Please try again later." });
  }
  return json(200, { refused: true, reason: "check_failed", message: refusalText(lang) });
}
