const admin = require("firebase-admin");
const {onRequest, onCall, HttpsError} = require("firebase-functions/v2/https");
const {onDocumentCreated, onDocumentDeleted, onDocumentWritten, onDocumentUpdated} = require("firebase-functions/v2/firestore");
const halal = require("./halal");
const {onSchedule} = require("firebase-functions/v2/scheduler");
const {defineSecret} = require("firebase-functions/params");
const {S3Client, PutObjectCommand} = require("@aws-sdk/client-s3");
const {getSignedUrl} = require("@aws-sdk/s3-request-presigner");
const {
  MediaConvertClient,
  DescribeEndpointsCommand,
  CreateJobCommand
} = require("@aws-sdk/client-mediaconvert");

admin.initializeApp();

// NVIDIA API key is stored in Google Secret Manager, never in source or the app.
// Set it once with:  firebase functions:secrets:set NVIDIA_API_KEY
const NVIDIA_API_KEY = defineSecret("NVIDIA_API_KEY");
const GROQ_API_KEY = defineSecret("GROQ_API_KEY");
const GROQ_BASE_URL = "https://api.groq.com/openai/v1/chat/completions";
const GROQ_MODELS = (process.env.NOOR_AI_GROQ_MODELS || "openai/gpt-oss-120b,qwen/qwen3.8-27b,openai/gpt-oss-20b")
  .split(",").map((m) => m.trim()).filter(Boolean);
// Resend API key is stored in Google Secret Manager, never in source or the app.
// Set it once with:  firebase functions:secrets:set RESEND_API_KEY
const RESEND_API_KEY = defineSecret("RESEND_API_KEY");
// 1.0.77: Quran.Foundation Content API (client-credentials). Values live in Secret Manager only.
const QF_CLIENT_ID = defineSecret("QF_CLIENT_ID");
const QF_CLIENT_SECRET = defineSecret("QF_CLIENT_SECRET");
// AWS credentials for presigned S3 reel uploads (served via CloudFront). Stored in
// Google Secret Manager, never in source or the app. Set once with:
//   firebase functions:secrets:set AWS_ACCESS_KEY_ID
//   firebase functions:secrets:set AWS_SECRET_ACCESS_KEY
const AWS_ACCESS_KEY_ID = defineSecret("AWS_ACCESS_KEY_ID");
const AWS_SECRET_ACCESS_KEY = defineSecret("AWS_SECRET_ACCESS_KEY");

// Non-secret AWS config. Set via functions/.env (or environment) after you create the
// bucket + CloudFront distribution:
//   NOOR_S3_BUCKET   = your S3 bucket name        (e.g. noor-pro-reels)
//   NOOR_S3_REGION   = the bucket's region        (e.g. ap-south-1)
//   NOOR_CDN_DOMAIN  = your CloudFront domain      (e.g. d123abc.cloudfront.net)
const S3_BUCKET = process.env.NOOR_S3_BUCKET || "";
const S3_REGION = process.env.NOOR_S3_REGION || "ap-south-1";
const CDN_DOMAIN = (process.env.NOOR_CDN_DOMAIN || "").replace(/^https?:\/\//, "").replace(/\/$/, "");
// IAM role ARN that MediaConvert assumes to read/write the S3 bucket. Set after you create the
// role:  NOOR_MEDIACONVERT_ROLE = arn:aws:iam::<acct>:role/NoorMediaConvertRole
const MEDIACONVERT_ROLE = process.env.NOOR_MEDIACONVERT_ROLE || "";
// Cached MediaConvert account-specific endpoint (resolved once per cold start).
let mediaConvertEndpoint = "";

const REGION = process.env.NOOR_AI_REGION || "us-central1";
const RESET_FROM_EMAIL = process.env.RESET_FROM_EMAIL || "Noor Pro <onboarding@resend.dev>";
const RESET_CONTINUE_URL = process.env.RESET_CONTINUE_URL || "https://noor-pro-d87e3.web.app";
// NVIDIA NIM (OpenAI-compatible) endpoint + models. NVIDIA retires hosted models without notice
// (meta/llama-3.3-70b-instruct now answers HTTP 410 Gone), so we keep an ordered fallback list and
// fall through on 404/410/429/5xx/empty answers. Override the whole list with NOOR_AI_MODELS
// (comma separated) or the first entry with NOOR_AI_MODEL.
const NVIDIA_BASE_URL =
  process.env.NOOR_AI_BASE_URL || "https://integrate.api.nvidia.com/v1/chat/completions";
const DEFAULT_MODELS = [
  "mistralai/mistral-large-2-instruct",
  "nvidia/llama-3.1-nemotron-70b-instruct",
  "nvidia/nemotron-3-super-120b-a12b",
  "google/gemma-4-31b-it",
  "mistralai/mistral-large",
  "nvidia/nemotron-4-340b-instruct"
];
const MODELS = (process.env.NOOR_AI_MODELS ?
  process.env.NOOR_AI_MODELS.split(",") :
  [process.env.NOOR_AI_MODEL, ...DEFAULT_MODELS])
  .map((m) => (m || "").trim()).filter(Boolean)
  .filter((m, i, all) => all.indexOf(m) === i);
const MAX_PROMPT_CHARS = 4000;
const MAX_HISTORY_TURNS = 10;
const MODEL_TIMEOUT_MS = 40000;

const modeInstructions = {
  AskNoor: "Answer general Islamic questions with mainstream Sunni-safe guidance. Be humble and avoid issuing binding fatwa.",
  DuaGenerator: "Create respectful duas. Present every dua in three labelled parts: Arabic (Arabic script), Transliteration (Latin), and Meaning (English). Then a short personal note. Do not invent Quran/Hadith quotes.",
  QuranHelper: "Explain Quran reflections simply. When you quote an ayah, show it as Arabic, Transliteration, and Meaning (with surah:ayah), then the reflection. Tell users to check reliable tafsir and avoid unsupported claims.",
  HadithExplainer: "Explain hadith meaning safely. Mention checking source/authenticity and give daily action points.",
  HajjUmrahGuide: "Give step-by-step Hajj or Umrah guidance and advise asking an official guide/scholar when uncertain.",
  AppGuide: [
    "You are the in-app assistant for the Noor Pro app itself. Help users find and use features. Key map:",
    "- Bottom navigation: Home, Quran, Reels, Ummah, More(Profile).",
    "- Quran: read surahs/juz, continue reading card, audio playback, bookmarks, translations.",
    "- Prayer: Home shows next prayer + today's schedule; full schedule and Qaza tracker under More.",
    "- Reels: swipe vertically; double-tap right/left = ±10s; hold = 2x speed; pull down to refresh; three-dot menu has quality, speed, captions, interested/not interested, report.",
    "- Ummah: community posts; pull down to refresh; create posts/reels with the + button; follow creators.",
    "- Messages: Profile → Messages. Tabs: Chats, People (everyone you follow/followers), Groups, Constitution. Group admins add members via Group info → Add members. QR icon shares your profile.",
    "- Profile: edit name/username/photo via the pencil; followers/following lists; saved posts; Creator Studio.",
    "- Also inside: Qibla compass, Duas, Tasbih, Hijri calendar, Zakat calculator, 99 Names, Islamic library, quizzes, Noor AI.",
    "Answer briefly with tap-by-tap steps. If a feature doesn't exist, say so honestly and suggest the closest alternative."
  ].join("\n")
};

function cors(res) {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Firebase-AppCheck");
}

/**
 * Gate for the AI endpoint: the caller must present a valid Firebase App Check token (proves the
 * request comes from the genuine app) OR a valid Firebase Auth ID token (signed-in user). A token
 * that is present but invalid is always rejected. Set NOOR_REQUIRE_APPCHECK=false only for local
 * emulators / temporary diagnostics: it lets requests with NO token through.
 */
async function requireAppCheck(req) {
  const appCheckToken = req.get("X-Firebase-AppCheck") || "";
  const authHeader = req.get("Authorization") || "";
  const idToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7).trim() : "";
  if (!appCheckToken && !idToken) {
    if ((process.env.NOOR_REQUIRE_APPCHECK || "true").toLowerCase() === "false") return;
    const err = new Error("Missing App Check or ID token");
    err.code = 401;
    throw err;
  }
  if (appCheckToken) {
    try {
      await admin.appCheck().verifyToken(appCheckToken);
      return;
    } catch (error) {
      if (!idToken) throw error;
    }
  }
  await admin.auth().verifyIdToken(idToken);
}

/** Optional Auth — returns uid when present, null otherwise. */
async function optionalAuthUid(req) {
  const authHeader = req.get("Authorization") || "";
  const idToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7).trim() : "";
  if (!idToken) return null;
  try {
    const decoded = await admin.auth().verifyIdToken(idToken);
    return decoded.uid || null;
  } catch (_) {
    return null;
  }
}

function cleanString(value, fallback = "") {
  if (typeof value !== "string") return fallback;
  return value.trim().slice(0, MAX_PROMPT_CHARS);
}

const LANGUAGE_NAMES = {en: "English", ur: "Urdu", hi: "Hindi", bn: "Bengali", ar: "Arabic"};

/** Removes the trailing FOLLOWUPS line and returns it as a list (max 3). */
function extractFollowups(answer) {
  const m = String(answer || "").match(/\n?\s*FOLLOWUPS:\s*(.+)\s*$/i);
  if (!m) return {text: answer, followups: []};
  const followups = m[1].split("|").map((q) => q.replace(/\*\*/g, "").trim()).filter(Boolean).slice(0, 3).map((q) => q.slice(0, 140));
  return {text: answer.slice(0, m.index).trim(), followups};
}

function buildSystemPrompt(mode, instructions, language) {
  const modeText = modeInstructions[mode] || modeInstructions.AskNoor;
  const lines = [
    "You are Noor AI, the assistant inside the Noor Pro Islamic app. Answer the user's ACTUAL question directly and specifically; never reply with generic boilerplate that ignores what was asked. Use the earlier conversation turns for context and follow-up questions.",
    modeText,
    "Creed and fiqh: follow the mainstream Sunni position (Ahl as-Sunnah wal-Jama'ah). For fiqh questions, mention the four Sunni madhhabs (Hanafi, Maliki, Shafi'i, Hanbali) where they differ and say which is which. Never promote Shia, Ahmadi, Quranist or other deviant views as correct, and do not take sectarian polemic positions; stay respectful.",
    "Sources: ground answers in the Quran and authentic Sunnah (Sahih al-Bukhari, Sahih Muslim, and the other books of the Sunan). Cite only what you are confident of: Quran as (Surah Name 2:255), hadith as collection plus number, e.g. (Sahih al-Bukhari 1). Never invent a verse, hadith, number or scholar. If you are unsure of a reference, say so and tell the user to verify.",
    "Depth: give a thorough, well-organised answer like a top AI assistant (roughly 250-600 words for real questions; shorter for greetings or simple app questions). Structure it with these '### ' sections when they fit: '### Summary' (2-3 sentence direct answer), '### Explanation' (the reasoning, context, and any madhhab differences), '### Practical steps' (numbered, actionable), '### Evidence' (each item: a '> ' quote or paraphrase of the Quran/hadith followed by its reference in parentheses). Skip sections that do not apply.",
    "Format: clear, warm language. Use clean Markdown: '### ' headings, '- ' bullets, numbered lists, **bold** for key terms, '> ' for quoted Quran/hadith, and a Markdown table (| a | b |) only when comparing several items (e.g. madhhab positions or a habit plan). No HTML, no links/URLs. Present Arabic in three lines: 'Arabic: ...', 'Transliteration: ...', 'Meaning: ...'; if unsure of the exact Arabic wording, give the meaning only.",
    "Tasawwuf / spirituality: you may draw on classical Sunni works and name them by book and author only (e.g. Ihya Ulum al-Din by al-Ghazali, Al-Risala al-Qushayriyya, Kashf al-Mahjub by Ali Hujwiri, Futuh al-Ghayb by Abd al-Qadir al-Jilani, Al-Hikam by Ibn Ata'illah); never give links, and never invent quotations from them.",
    "Never claim to be a scholar and do not issue a binding fatwa. For divorce, inheritance, medical, legal, self-harm, abuse, or complex rulings, advise consulting a qualified scholar or professional. Decline politely to answer requests that are unrelated to Islam or the app, or that are harmful.",
    "Always end with: Allah knows best."
  ];
  if (instructions) lines.push(`Style note from the app: ${instructions}`);
  if (language && LANGUAGE_NAMES[language]) {
    lines.push(`Write the whole answer in ${LANGUAGE_NAMES[language]} (keep Quran Arabic in Arabic script, and keep references like (Sahih al-Bukhari 1) in English).`);
  }
  lines.push("After 'Allah knows best.', add one final line exactly in the form 'FOLLOWUPS: question 1 | question 2 | question 3' with three short, natural follow-up questions the user might ask next (in the answer language).");
  return lines.join("\n");
}

/** Strip hidden chain-of-thought some reasoning models put in the answer. */
function stripThinking(text) {
  return String(text || "")
    .replace(/<think>[\s\S]*?<\/think>/gi, "")
    .replace(/^[\s\S]*?<\/think>/i, "")
    .trim();
}

/** Pull "Sources:" bullets out of the answer so the client can render citations separately. */
function extractCitations(answer) {
  const idx = answer.search(/\n\s*\**Sources?:?\**\s*\n/i);
  if (idx < 0) return {body: answer, citations: []};
  const tail = answer.slice(idx);
  const citations = tail.split("\n")
    .map((l) => l.replace(/^\s*[-*•\d.)]+\s*/, "").trim())
    .filter((l) => l && !/^\**sources?:?\**$/i.test(l) && !/^allah knows best/i.test(l))
    .slice(0, 8)
    .map((ref) => ({ref: ref.slice(0, 200)}));
  return {body: answer, citations};
}

async function callNvidia(apiKey, model, messages, temperature, baseUrl = NVIDIA_BASE_URL) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), MODEL_TIMEOUT_MS);
  try {
    const response = await fetch(baseUrl, {
      method: "POST",
      signal: controller.signal,
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json",
        "Accept": "application/json"
      },
      body: JSON.stringify({
        model,
        messages,
        temperature,
        top_p: 0.9,
        max_tokens: 3000,
        stream: false
      })
    });
    if (!response.ok) {
      const detail = await response.text().catch(() => "");
      return {ok: false, status: response.status, detail: detail.slice(0, 300)};
    }
    const data = await response.json();
    const message = data && Array.isArray(data.choices) && data.choices[0] && data.choices[0].message;
    const text = stripThinking(message && typeof message.content === "string" ? message.content : "");
    return text ? {ok: true, text} : {ok: false, status: 200, detail: "empty answer"};
  } catch (error) {
    return {ok: false, status: 0, detail: error && error.message ? error.message : "request failed"};
  } finally {
    clearTimeout(timer);
  }
}

// Very small per-instance throttle so a leaked endpoint cannot burn the NVIDIA quota.
const recentCalls = new Map();
function throttled(key) {
  const now = Date.now();
  const list = (recentCalls.get(key) || []).filter((t) => now - t < 60000);
  list.push(now);
  recentCalls.set(key, list);
  if (recentCalls.size > 5000) recentCalls.clear();
  return list.length > 20;
}

exports.askNoorAi = onRequest(
  {
    region: REGION,
    cors: false,
    timeoutSeconds: 120,
    memory: "512MiB",
    maxInstances: 10,
    secrets: [NVIDIA_API_KEY, GROQ_API_KEY]
  },
  async (req, res) => {
    cors(res);
    if (req.method === "OPTIONS") {
      res.status(204).send("");
      return;
    }
    if (req.method !== "POST") {
      res.status(405).json({error: "Use POST."});
      return;
    }

    try {
      await requireAppCheck(req);
    } catch (error) {
      res.status(401).json({error: "App Check required."});
      return;
    }

    try {
      const prompt = cleanString(req.body && req.body.prompt);
      const mode = cleanString(req.body && req.body.mode, "AskNoor").slice(0, 40);
      const instructions = cleanString(req.body && req.body.instructions).slice(0, 400);
      const language = cleanString(req.body && req.body.language, "en").slice(0, 5);
      if (!prompt) {
        res.status(400).json({error: "Prompt is required."});
        return;
      }

      const uid = await optionalAuthUid(req);
      if (throttled(uid || req.ip || "anon")) {
        res.status(429).json({error: "Too many questions. Please wait a minute and try again."});
        return;
      }

      // Conversation history so Noor AI remembers the chat. Drop a trailing copy of the current
      // question if the client included it, and make sure roles alternate starting with a user turn.
      const rawHistory = Array.isArray(req.body && req.body.history) ? req.body.history : [];
      let history = rawHistory
        .filter((m) => m && (m.role === "user" || m.role === "assistant") &&
          typeof m.content === "string" && m.content.trim())
        .slice(-MAX_HISTORY_TURNS)
        .map((m) => ({role: m.role, content: m.content.trim().slice(0, MAX_PROMPT_CHARS)}));
      if (history.length && history[history.length - 1].role === "user" &&
          history[history.length - 1].content === prompt) {
        history.pop();
      }
      while (history.length && history[0].role !== "user") history.shift();
      const alternating = [];
      for (const m of history) {
        const last = alternating[alternating.length - 1];
        if (last && last.role === m.role) last.content += `\n${m.content}`;
        else alternating.push({...m});
      }
      history = alternating;
      if (history.length && history[history.length - 1].role === "user") history.pop();

      const clean = (v) => String(v || "").trim().replace(/^["']|["']$/g, "").replace(/^Bearer\s+/i, "");
      const groqKey = clean(GROQ_API_KEY.value());
      const rawKey = NVIDIA_API_KEY.value() || "";
      // A secret saved with a trailing newline/space or wrapping quotes is rejected by NVIDIA with 403.
      const apiKey = rawKey.trim().replace(/^["']|["']$/g, "").replace(/^Bearer\s+/i, "");
      if (apiKey !== rawKey) console.warn("askNoorAi: NVIDIA_API_KEY had stray whitespace/quotes; sanitized");
      if (!apiKey && !groqKey) {
        res.status(500).json({error: "Noor AI key is not configured."});
        return;
      }

      const messages = [
        {role: "system", content: buildSystemPrompt(mode, instructions, language)},
        ...history,
        {role: "user", content: prompt}
      ];
      const temperature = mode === "DuaGenerator" ? 0.65 : 0.3;

      let last = {status: 0, detail: "no models configured"};
      const tried = [];
      // Groq first (fast), then NVIDIA models as backup.
      for (const model of groqKey ? GROQ_MODELS : []) {
        const result = await callNvidia(groqKey, model, messages, temperature, GROQ_BASE_URL);
        if (result.ok) {
          const {citations} = extractCitations(result.text);
          console.log("askNoorAi served by groq", model);
          const fu = extractFollowups(result.text);
          res.json({answer: fu.text, followups: fu.followups, citations, provider: "groq", model});
          return;
        }
        console.error("askNoorAi groq model failed", model, result.status, result.detail);
        last = result;
        tried.push(`groq/${model}:${result.status}`);
      }
      for (const model of apiKey ? MODELS : []) {
        const result = await callNvidia(apiKey, model, messages, temperature);
        if (result.ok) {
          const {citations} = extractCitations(result.text);
          console.log("askNoorAi served by", model);
          const fu = extractFollowups(result.text);
          res.json({answer: fu.text, followups: fu.followups, citations, provider: "nvidia", model});
          return;
        }
        console.error("askNoorAi model failed", model, result.status, result.detail);
        last = result;
        tried.push(`${model}:${result.status}`);
      }
      // Shape-only hint (never the key): NVIDIA personal keys start with "nvapi-".
      const keyLooksValid = /^nvapi-/.test(apiKey);
      if (last.status === 401 || last.status === 403) {
        console.error(`askNoorAi: NVIDIA rejected the API key (HTTP ${last.status}); key length ${apiKey.length}, nvapi- prefix: ${keyLooksValid}. Create a new key at build.nvidia.com and run: firebase functions:secrets:set NVIDIA_API_KEY`);
      }
      res.status(502).json({
        error: "Noor AI is temporarily unavailable.",
        detail: `Upstream status ${last.status}`,
        tried
      });
    } catch (error) {
      console.error("askNoorAi failed", error);
      res.status(500).json({
        error: "Noor AI is temporarily unavailable.",
        detail: error && error.message ? error.message : "Unknown error"
      });
    }
  }
);

exports.sendPasswordResetEmail = onRequest(
  {
    region: REGION,
    cors: false,
    timeoutSeconds: 30,
    memory: "256MiB",
    maxInstances: 10,
    secrets: [RESEND_API_KEY]
  },
  async (req, res) => {
    cors(res);
    if (req.method === "OPTIONS") {
      res.status(204).send("");
      return;
    }
    if (req.method !== "POST") {
      res.status(405).json({error: "Use POST."});
      return;
    }

    try {
      await requireAppCheck(req);
    } catch (error) {
      res.status(401).json({error: "App Check required."});
      return;
    }

    try {
      const email = cleanString(req.body && req.body.email, "").toLowerCase();
      if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        res.status(400).json({error: "A valid email is required."});
        return;
      }

      const resendKey = RESEND_API_KEY.value();
      if (!resendKey) {
        res.status(500).json({error: "Password reset email service is not configured."});
        return;
      }

      let resetLink = "";
      try {
        resetLink = await admin.auth().generatePasswordResetLink(email, {
          url: RESET_CONTINUE_URL,
          handleCodeInApp: false
        });
      } catch (error) {
        // Avoid account enumeration. If the email is not registered, respond
        // as if a message was sent.
        if (error && error.code === "auth/user-not-found") {
          res.json({ok: true});
          return;
        }
        throw error;
      }

      const resendResponse = await fetch("https://api.resend.com/emails", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${resendKey}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          from: RESET_FROM_EMAIL,
          to: email,
          subject: "Reset your Noor Pro password",
          html: [
            "<div style=\"font-family:Arial,sans-serif;line-height:1.6;color:#111827\">",
            "<h2 style=\"color:#111827\">Reset your Noor Pro password</h2>",
            "<p>Assalamu alaikum,</p>",
            "<p>We received a request to reset the password for your Noor Pro account.</p>",
            `<p><a href=\"${resetLink}\" style=\"display:inline-block;background:#D4AF37;color:#07101F;padding:12px 18px;border-radius:10px;text-decoration:none;font-weight:bold\">Reset password</a></p>`,
            "<p>If you did not request this, you can safely ignore this email.</p>",
            "<p style=\"font-size:12px;color:#6B7280\">This link is generated securely by Firebase Authentication.</p>",
            "</div>"
          ].join(""),
          text: [
            "Reset your Noor Pro password",
            "",
            "Assalamu alaikum,",
            "We received a request to reset the password for your Noor Pro account.",
            "",
            resetLink,
            "",
            "If you did not request this, you can safely ignore this email."
          ].join("\n")
        })
      });

      if (!resendResponse.ok) {
        const detail = await resendResponse.text().catch(() => "");
        console.error("Resend API error", resendResponse.status, detail.slice(0, 500));
        res.status(502).json({error: "Unable to send password reset email."});
        return;
      }

      res.json({ok: true});
    } catch (error) {
      console.error("sendPasswordResetEmail failed", error);
      res.status(500).json({
        error: "Unable to send password reset email.",
        detail: error && error.message ? error.message : "Unknown error"
      });
    }
  }
);

// Returns a short-lived presigned S3 PUT URL so a signed-in user can upload a reel/video
// straight to S3, plus the final CloudFront playback URL the app stores. AWS credentials stay
// server-side; the client never sees them. If AWS isn't configured yet, returns 503 and the
// app falls back to Firebase Storage automatically.
const ALLOWED_UPLOAD_TYPES = {
  "video/mp4": "mp4",
  "video/quicktime": "mov",
  "video/webm": "webm",
  "video/3gpp": "3gp",
  "video/x-matroska": "mkv",
  "image/jpeg": "jpg",
  "image/png": "png",
  "image/webp": "webp"
};
const MAX_REEL_BYTES = 100 * 1024 * 1024;

exports.getReelUploadUrl = onRequest(
  {
    region: REGION,
    cors: false,
    timeoutSeconds: 30,
    memory: "256MiB",
    maxInstances: 10,
    secrets: [AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY]
  },
  async (req, res) => {
    cors(res);
    if (req.method === "OPTIONS") {
      res.status(204).send("");
      return;
    }
    if (req.method !== "POST") {
      res.status(405).json({error: "Use POST."});
      return;
    }

    // Only signed-in users may request an upload URL.
    const authHeader = req.get("Authorization") || "";
    const idToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7).trim() : "";
    let uid = "";
    try {
      if (!idToken) throw new Error("Missing token");
      const decoded = await admin.auth().verifyIdToken(idToken);
      uid = decoded.uid;
    } catch (error) {
      res.status(401).json({error: "Sign in required."});
      return;
    }

    const accessKey = AWS_ACCESS_KEY_ID.value();
    const secretKey = AWS_SECRET_ACCESS_KEY.value();
    if (!S3_BUCKET || !CDN_DOMAIN || !accessKey || !secretKey) {
      // Not configured yet — the app will fall back to Firebase Storage.
      res.status(503).json({error: "Reel CDN is not configured."});
      return;
    }

    const contentType = cleanString(req.body && req.body.contentType, "video/mp4");
    const ext = ALLOWED_UPLOAD_TYPES[contentType];
    if (!ext) {
      res.status(400).json({error: "Unsupported video type."});
      return;
    }
    const contentLength = Number(req.body && req.body.contentLength);
    if (!Number.isSafeInteger(contentLength) || contentLength <= 0 || contentLength >= MAX_REEL_BYTES) {
      res.status(400).json({error: "Video must be smaller than 100 MB."});
      return;
    }

    try {
      const key = `reels/${uid}/${Date.now()}.${ext}`;
      const s3 = new S3Client({
        region: S3_REGION,
        credentials: {accessKeyId: accessKey, secretAccessKey: secretKey}
      });
      const command = new PutObjectCommand({
        Bucket: S3_BUCKET,
        Key: key,
        ContentType: contentType,
        ContentLength: contentLength
      });
      const uploadUrl = await getSignedUrl(s3, command, {expiresIn: 300});
      const playbackUrl = `https://${CDN_DOMAIN}/${key}`;
      res.json({uploadUrl, playbackUrl, key});
    } catch (error) {
      console.error("getReelUploadUrl failed", error);
      res.status(500).json({
        error: "Unable to create upload URL.",
        detail: error && error.message ? error.message : "Unknown error"
      });
    }
  }
);

// Kicks off an AWS MediaConvert job that transcodes an uploaded reel into adaptive HLS
// (720p + 480p renditions) written to hls/<base>/index.m3u8. The app plays the HLS URL for smooth
// quality-adaptive streaming and falls back to the original MP4 while transcoding is in progress
// or if HLS isn't configured. AWS credentials stay server-side.
exports.startReelTranscode = onRequest(
  {
    region: REGION,
    cors: false,
    timeoutSeconds: 30,
    memory: "256MiB",
    maxInstances: 10,
    secrets: [AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY]
  },
  async (req, res) => {
    cors(res);
    if (req.method === "OPTIONS") {
      res.status(204).send("");
      return;
    }
    if (req.method !== "POST") {
      res.status(405).json({error: "Use POST."});
      return;
    }

    const authHeader = req.get("Authorization") || "";
    const idToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7).trim() : "";
    let uid = "";
    try {
      if (!idToken) throw new Error("Missing token");
      const decoded = await admin.auth().verifyIdToken(idToken);
      uid = decoded.uid;
    } catch (error) {
      res.status(401).json({error: "Sign in required."});
      return;
    }

    const accessKey = AWS_ACCESS_KEY_ID.value();
    const secretKey = AWS_SECRET_ACCESS_KEY.value();
    if (!S3_BUCKET || !CDN_DOMAIN || !MEDIACONVERT_ROLE || !accessKey || !secretKey) {
      // HLS not configured — app keeps using the MP4.
      res.status(503).json({error: "Transcoding is not configured."});
      return;
    }

    // The key must be a reel this user just uploaded.
    const key = cleanString(req.body && req.body.key, "");
    const extension = key.split(".").pop().toLowerCase();
    const allowedExtensions = new Set(Object.values(ALLOWED_UPLOAD_TYPES));
    if (!key.startsWith(`reels/${uid}/`) || !allowedExtensions.has(extension)) {
      res.status(400).json({error: "Invalid reel key."});
      return;
    }

    const base = key.slice(0, -(extension.length + 1)); // reels/<uid>/<ts>
    const credentials = {accessKeyId: accessKey, secretAccessKey: secretKey};

    // Android sends the original encoded dimensions plus the phone's rotation metadata. Build
    // output frames with the same displayed aspect ratio so portrait reels never get padded into
    // a forced 16:9 landscape canvas.
    const mediaWidth = Number(req.body && req.body.mediaWidth);
    const mediaHeight = Number(req.body && req.body.mediaHeight);
    const requestedRotation = Number(req.body && req.body.mediaRotationDegrees);
    const hasSourceGeometry = Number.isInteger(mediaWidth) && mediaWidth >= 16 && mediaWidth <= 8192 &&
      Number.isInteger(mediaHeight) && mediaHeight >= 16 && mediaHeight <= 8192;
    const mediaRotationDegrees = [0, 90, 180, 270].includes(requestedRotation) ? requestedRotation : 0;
    const quarterTurn = mediaRotationDegrees === 90 || mediaRotationDegrees === 270;
    const displayWidth = hasSourceGeometry ? (quarterTurn ? mediaHeight : mediaWidth) : 0;
    const displayHeight = hasSourceGeometry ? (quarterTurn ? mediaWidth : mediaHeight) : 0;

    function evenDimension(value) {
      const rounded = Math.max(16, Math.round(value));
      return rounded % 2 === 0 ? rounded : rounded - 1;
    }

    function rendition(maxLongEdge) {
      const scale = Math.min(1, maxLongEdge / Math.max(displayWidth, displayHeight));
      return {
        width: evenDimension(displayWidth * scale),
        height: evenDimension(displayHeight * scale)
      };
    }

    function hlsOutput(nameModifier, width, height, maxBitrate) {
      return {
        NameModifier: nameModifier,
        ContainerSettings: {Container: "M3U8", M3u8Settings: {}},
        VideoDescription: {
          ...(width && height ? {Width: width, Height: height, ScalingBehavior: "FIT"} : {}),
          CodecSettings: {
            Codec: "H_264",
            H264Settings: {
              RateControlMode: "QVBR",
              MaxBitrate: maxBitrate,
              QvbrSettings: {QvbrQualityLevel: 7},
              SceneChangeDetect: "TRANSITION_DETECTION"
            }
          }
        },
        AudioDescriptions: [{
          CodecSettings: {
            Codec: "AAC",
            AacSettings: {Bitrate: 96000, CodingMode: "CODING_MODE_2_0", SampleRate: 48000}
          }
        }]
      };
    }

    try {
      if (!mediaConvertEndpoint) {
        const probe = new MediaConvertClient({region: S3_REGION, credentials});
        const described = await probe.send(new DescribeEndpointsCommand({}));
        mediaConvertEndpoint = described.Endpoints[0].Url;
      }
      const mc = new MediaConvertClient({region: S3_REGION, endpoint: mediaConvertEndpoint, credentials});

      const outputs = hasSourceGeometry ? (() => {
        const high = rendition(1280);
        const low = rendition(854);
        const highOutput = hlsOutput("_720", high.width, high.height, 2500000);
        if (high.width === low.width && high.height === low.height) return [highOutput];
        return [highOutput, hlsOutput("_480", low.width, low.height, 1000000)];
      })() : [hlsOutput("_source", null, null, 2500000)];

      await mc.send(new CreateJobCommand({
        Role: MEDIACONVERT_ROLE,
        Settings: {
          Inputs: [{
            FileInput: `s3://${S3_BUCKET}/${key}`,
            TimecodeSource: "ZEROBASED",
            // AUTO applies MP4/MOV rotation metadata before scaling. The output itself is upright.
            VideoSelector: {Rotate: "AUTO"},
            AudioSelectors: {"Audio Selector 1": {DefaultSelection: "DEFAULT"}}
          }],
          OutputGroups: [{
            Name: "HLS",
            OutputGroupSettings: {
              Type: "HLS_GROUP_SETTINGS",
              HlsGroupSettings: {
                Destination: `s3://${S3_BUCKET}/hls/${base}/index`,
                SegmentLength: 6,
                MinSegmentLength: 0
              }
            },
            Outputs: outputs
          }]
        }
      }));

      const hlsUrl = `https://${CDN_DOMAIN}/hls/${base}/index.m3u8`;
      res.json({ok: true, hlsUrl});
    } catch (error) {
      console.error("startReelTranscode failed", error);
      res.status(500).json({
        error: "Unable to start transcoding.",
        detail: error && error.message ? error.message : "Unknown error"
      });
    }
  }
);

const {composerFields} = require("./composer");

// Validate and promote a submission into the public feed. Both the create trigger and the
// authenticated recovery endpoint use this function, so tester uploads created before the trigger
// was deployed can still be promoted. The operation is idempotent because the public post keeps the
// submission ID.
async function promoteUmmahSubmission(submission, submissionId, opts = {}) {
  if (!submission || !submission.exists) return {published: false, status: "missing"};

  const data = submission.data() || {};
  if (["rejected", "removed"].includes(data.status)) {
    return {published: false, status: data.status};
  }
  // 1.1.5 Halal Guard: queued items wait for a moderator; the recovery endpoint must not bypass it.
  const moderatorApproved = opts.moderatorApproved === true;
  if (data.status === "in_review" && !moderatorApproved) return {published: false, status: "in_review"};

  const type = cleanString(data.type, "text").toLowerCase();
  const allowedTypes = new Set(["text", "image", "reel", "video"]);
  const creatorUid = cleanString(data.creatorUid, "").slice(0, 128);
  let caption = cleanString(data.caption, "").slice(0, 2000);
  let arabicText = cleanString(data.arabicText, "").slice(0, 2000);
  const mediaUrl = cleanString(data.mediaUrl, "").slice(0, 2000);
  const hasValidMedia = type === "text" || /^https:\/\//i.test(mediaUrl);

  if (!allowedTypes.has(type) || !creatorUid || (!caption && !arabicText && !mediaUrl) || !hasValidMedia) {
    await submission.ref.update({
      status: "rejected",
      approved: false,
      moderationReason: "Invalid submission",
      reviewedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    return {published: false, status: "rejected"};
  }

  // 1.1.5 Halal Guard (flagged; with every flag off only an existing suspension/ban is enforced).
  let guard = null;
  if (!moderatorApproved) {
    guard = await guardSubmission(data, submissionId, {type, creatorUid, caption, arabicText, mediaUrl});
    if (guard.decision === "reject") {
      await submission.ref.update({
        status: "rejected",
        approved: false,
        moderationReason: guard.userReason,
        guardBlocked: true,
        moderation: guardSummary(guard),
        reviewedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      await addModerationNotice(creatorUid, {kind: "post", targetId: submissionId, reason: guard.userReason});
      return {published: false, status: "rejected", reason: guard.userReason};
    }
    if (guard.decision === "queue") {
      await admin.firestore().collection("moderation_queue").doc(submissionId).set({
        kind: "post", targetId: submissionId, creatorUid,
        creatorName: cleanString(data.creatorName, "Community member").slice(0, 60),
        type, caption: caption.slice(0, 600), arabicText: arabicText.slice(0, 300), mediaUrl,
        thumbnailUrl: cleanString(data.thumbnailUrl, "").slice(0, 2000),
        mediaUrls: Array.isArray(data.mediaUrls) ? data.mediaUrls.filter((u) => typeof u === "string").slice(0, 10) : [],
        reasons: guard.reasons, userReason: guard.userReason, scan: guard.scan || null,
        status: "open", createdAt: admin.firestore.FieldValue.serverTimestamp()
      }, {merge: true});
      await submission.ref.update({
        status: "in_review",
        moderationReason: guard.userReason,
        moderation: guardSummary(guard),
        reviewedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      return {published: false, status: "in_review"};
    }
    if (guard.masked.caption) caption = guard.masked.caption;
    if (guard.masked.arabicText) arabicText = guard.masked.arabicText;
  }

  // Likes and comments can be added while the uploader sees the pending reel. Carry those totals
  // into the public document so publishing never resets working engagement back to zero.
  let likeCount = Number(data.likeCount) || 0;
  let commentCount = Number(data.commentCount) || 0;
  let saveCount = Number(data.saveCount) || 0;
  try {
    const [likes, comments, saves] = await Promise.all([
      admin.firestore().collectionGroup("likes").where("postId", "==", submissionId).get(),
      admin.firestore().collection("ummah_comments").where("postId", "==", submissionId).get(),
      admin.firestore().collectionGroup("saved").where("postId", "==", submissionId).get()
    ]);
    likeCount = likes.size;
    commentCount = comments.docs.filter((doc) => doc.get("status") !== "removed").length;
    saveCount = saves.size;
  } catch (error) {
    console.warn("Unable to pre-count pending reel engagement", submissionId, error);
  }

  const postRef = admin.firestore().collection("ummah_posts").doc(submissionId);
  const publishedPost = {
    caption,
    arabicText,
    category: cleanString(data.category, "Reminder").slice(0, 60),
    mediaUrl,
    hlsUrl: cleanString(data.hlsUrl, "").slice(0, 2000),
    thumbnailUrl: cleanString(data.thumbnailUrl, "").slice(0, 2000),
    mediaPath: cleanString(data.mediaPath, "").slice(0, 1000),
    storageProvider: cleanString(data.storageProvider, "firebase").slice(0, 20),
    sourceReference: cleanString(data.sourceReference, "").slice(0, 500),
    type,
    creatorUid,
    creatorName: cleanString(data.creatorName, "Community member").slice(0, 60),
    creatorHandle: cleanString(data.creatorHandle, "@member").slice(0, 40),
    creatorPhotoUrl: cleanString(data.creatorPhotoUrl, "").slice(0, 2000),
    approved: true,
    status: "published",
    likeCount,
    commentCount,
    shareCount: Number(data.shareCount) || 0,
    viewCount: Number(data.viewCount) || 0,
    saveCount,
    mediaWidth: Math.max(0, Number(data.mediaWidth) || 0),
    mediaHeight: Math.max(0, Number(data.mediaHeight) || 0),
    mediaRotationDegrees: [0, 90, 180, 270].includes(Number(data.mediaRotationDegrees)) ?
      Number(data.mediaRotationDegrees) : 0,
    displayAspectRatio: Math.max(0, Number(data.displayAspectRatio) || 0),
    ...carouselField(type, mediaUrl, data.mediaUrls),
    publishedAt: admin.firestore.FieldValue.serverTimestamp()
  };
  // 1.1.5 decision fields (only when the guard actually ran or a moderator approved).
  if (moderatorApproved || (guard && guard.ran)) {
    publishedPost.moderation = {...guardSummary(guard || {decision: "approve", reasons: []}),
      status: moderatorApproved ? "approved_by_moderator" : "auto_approved"};
    publishedPost.sensitive = opts.sensitive === true;
  }
  Object.assign(publishedPost, reelSearchFields({...publishedPost, soundTitle: data.soundTitle || data.audioTitle || ""}));
  // 1.1.1 composer fields (optional, validated). Followers-only posts go to their own collection.
  const extra = composerFields(type, data);
  Object.assign(publishedPost, extra);
  const followersOnly = extra.visibility === "followers";
  if (followersOnly) {
    const followerRef = admin.firestore().collection("ummah_follower_posts").doc(submissionId);
    await admin.firestore().runTransaction(async (transaction) => {
      const existing = await transaction.get(followerRef);
      if (!existing.exists) transaction.set(followerRef, publishedPost);
      transaction.update(submission.ref, {
        approved: true,
        status: "published",
        postId: followerRef.id,
        reviewedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    });
    return {published: true, status: "published", postId: followerRef.id, visibility: "followers"};
  }

  await admin.firestore().runTransaction(async (transaction) => {
    const existing = await transaction.get(postRef);
    if (!existing.exists) transaction.set(postRef, publishedPost);
    transaction.update(submission.ref, {
      approved: true,
      status: "published",
      postId: postRef.id,
      reviewedAt: admin.firestore.FieldValue.serverTimestamp()
    });
  });
  return {published: true, status: "published", postId: postRef.id};
}

// 1.0.91 Reels search: Firestore has no full-text search, so every published post carries derived
// arrays for array-contains queries (client ranks the results). Keys must match the app's
// ReelSearchKeys.kt. Derived fields only; nothing user-controlled beyond the post's own text.
const TASHKEEL = /[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640]/g;
function reelSearchKey(token) {
  let x = String(token || "").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(TASHKEEL, "")
    .replace(/[\u0622\u0623\u0625\u0671]/g, "\u0627").replace(/\u0649/g, "\u064A").replace(/\u06CC/g, "\u064A")
    .replace(/\u0629/g, "\u0647").replace(/\u06C1/g, "\u0647").replace(/\u06BE/g, "\u0647");
  x = x.replace(/[^\p{L}\p{N}_]/gu, "");
  return x.replace(/(ee|ea|ie)/g, "i").replace(/(oo|ou)/g, "u").replace(/([a-z])\1+/g, "$1");
}
const reelSearchTokens = (t) => String(t || "").split(/[^\p{L}\p{N}_\u064B-\u065F\u0670]+/u).map(reelSearchKey).filter(Boolean);
const reelHashtags = (t) => [...new Set([...String(t || "").matchAll(/(?<![\p{L}\p{N}_])#([\p{L}\p{N}_]{1,40})/gu)].map((m) => m[1].toLowerCase()))].slice(0, 30);
const REEL_SEARCH_VERSION = 1;
function reelSearchFields(d) {
  const caption = `${d.caption || ""} ${d.arabicText || ""}`;
  const hashtags = reelHashtags(caption);
  const handle = String(d.creatorHandle || "").replace(/^@/, "");
  const prefixes = (tokens, set, cap) => {
    for (const t of tokens) for (let i = 2; i <= Math.min(t.length, 15); i++) { if (set.size >= cap) return; set.add(t.slice(0, i)); }
  };
  const cap = new Set(); prefixes(reelSearchTokens(caption).slice(0, 60), cap, 220);
  hashtags.forEach((h) => { const k = reelSearchKey(h); if (k.length >= 2) cap.add(k.slice(0, 15)); });
  const user = new Set(); prefixes([reelSearchKey(handle), ...reelSearchTokens(d.creatorName)], user, 60);
  const sound = new Set(); prefixes(reelSearchTokens(d.soundTitle || d.audioTitle || ""), sound, 40);
  const all = new Set([...cap, ...user, ...sound]);
  return {hashtags, captionKeywords: [...cap], usernameLower: [...user], searchKeywords: [...all].slice(0, 320),
    searchSrc: `${REEL_SEARCH_VERSION}|${caption}|${handle}|${d.creatorName || ""}|${d.soundTitle || d.audioTitle || ""}`.slice(0, 1500),
    searchVersion: REEL_SEARCH_VERSION};
}

async function indexReelDoc(ref, data) {
  const f = reelSearchFields(data || {});
  if ((data || {}).searchSrc === f.searchSrc && Array.isArray((data || {}).searchKeywords)) return false;
  await ref.update(f);
  return true;
}

// 1.0.75: optional multi-photo carousel. Only for image posts; 2..10 https strings and the first
// must equal mediaUrl (back-compat). Anything else is silently dropped (post stays single-photo).
function carouselField(type, mediaUrl, raw) {
  if (type !== "image" || !Array.isArray(raw)) return {};
  const urls = raw.filter((u) => typeof u === "string" && /^https:\/\//i.test(u) && u.length <= 2000).slice(0, 10);
  if (urls.length < 2 || urls[0] !== mediaUrl) return {};
  return {mediaUrls: urls};
}

// 1.0.75: hourly cleanup of expired stories (backs up the Firestore TTL policy, which can lag up
// to ~24h and never touches Storage). Deletes the doc, its viewers, and the story image.
exports.cleanupExpiredStories = onSchedule(
  {schedule: "every 60 minutes", region: REGION, timeoutSeconds: 300, memory: "256MiB"},
  async () => {
    const db = admin.firestore();
    const bucket = admin.storage().bucket();
    const snap = await db.collection("ummah_stories")
      .where("expiresAt", "<=", admin.firestore.Timestamp.now()).limit(400).get();
    for (const doc of snap.docs) {
      // 1.0.92: video stories also carry a poster frame at thumbPath; shared-reel stories have no file.
      for (const field of ["mediaPath", "thumbPath"]) {
        const path = cleanString(doc.get(field), "");
        if (path.startsWith("ummah_stories/")) await bucket.file(path).delete().catch(() => {});
      }
      await db.recursiveDelete(doc.ref).catch((e) => console.warn("story delete failed", doc.id, e));
    }
    console.log(`cleanupExpiredStories removed ${snap.size}`);
  }
);

// New uploads promote automatically.
exports.publishUmmahSubmission = onDocumentCreated(
  {
    document: "ummah_submissions/{submissionId}",
    region: REGION,
    // 1.1.5: room for the (flagged) Halal Guard media scan.
    timeoutSeconds: 300,
    memory: "512MiB"
  },
  async (event) => {
    if (!event.data) return;
    await promoteUmmahSubmission(event.data, event.params.submissionId);
  }
);

// Recovery endpoint for pending uploads. Only the submission owner can request promotion, and the
// same server-side validation above still runs before anything becomes public.
exports.publishUmmahSubmissionNow = onRequest(
  {
    region: REGION,
    cors: false,
    timeoutSeconds: 60,
    memory: "256MiB",
    maxInstances: 10
  },
  async (req, res) => {
    cors(res);
    if (req.method === "OPTIONS") return res.status(204).send("");
    if (req.method !== "POST") return res.status(405).json({error: "Use POST."});

    const authHeader = req.get("Authorization") || "";
    const idToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7).trim() : "";
    let uid = "";
    try {
      if (!idToken) throw new Error("Missing token");
      uid = (await admin.auth().verifyIdToken(idToken)).uid;
    } catch (error) {
      return res.status(401).json({error: "Sign in required."});
    }

    const submissionId = cleanString(req.body && req.body.submissionId, "");
    if (!/^[A-Za-z0-9_-]{10,160}$/.test(submissionId)) {
      return res.status(400).json({error: "Invalid submission."});
    }

    try {
      const ref = admin.firestore().collection("ummah_submissions").doc(submissionId);
      const submission = await ref.get();
      if (!submission.exists) return res.status(404).json({error: "Submission not found."});
      if (cleanString(submission.get("creatorUid"), "") !== uid) {
        return res.status(403).json({error: "This is not your submission."});
      }
      const result = await promoteUmmahSubmission(submission, submissionId);
      return res.json({ok: result.published, ...result});
    } catch (error) {
      console.error("publishUmmahSubmissionNow failed", error);
      return res.status(500).json({error: "Unable to publish this reel."});
    }
  }
);


// ---------------------------------------------------------------------------------------------
// 1.0.77 qfProxy: the app never sees the Quran.Foundation client secret. Whitelisted read-only
// content calls (word-by-word verses, chapter recitation timings, translations) are proxied with
// a cached client-credentials token, guarded by App Check or a Firebase ID token, and cached.
// Default is production (approved 2026-10-22); set QF_ENV=prelive to use prelive.
// ---------------------------------------------------------------------------------------------
const QF_HOSTS = {
  prelive: {auth: "https://prelive-oauth2.quran.foundation", api: "https://apis-prelive.quran.foundation"},
  production: {auth: "https://oauth2.quran.foundation", api: "https://apis.quran.foundation"}
};
let qfToken = {value: "", exp: 0, env: ""};
const qfCache = new Map(); // key -> {at, body}
const QF_CACHE_MS = 12 * 60 * 60 * 1000;

function qfEnv() {
  return (process.env.QF_ENV || "production").toLowerCase() === "prelive" ? "prelive" : "production";
}

async function qfAccessToken(force = false) {
  const env = qfEnv();
  if (!force && qfToken.value && qfToken.env === env && Date.now() < qfToken.exp - 60_000) return qfToken.value;
  const basic = Buffer.from(`${QF_CLIENT_ID.value().trim()}:${QF_CLIENT_SECRET.value().trim()}`).toString("base64");
  const r = await fetch(`${QF_HOSTS[env].auth}/oauth2/token`, {
    method: "POST",
    headers: {"Authorization": `Basic ${basic}`, "Content-Type": "application/x-www-form-urlencoded"},
    body: "grant_type=client_credentials&scope=content"
  });
  if (!r.ok) {
    const e = new Error(`QF token ${r.status}`);
    e.code = 502;
    throw e;
  }
  const j = await r.json();
  qfToken = {value: j.access_token, exp: Date.now() + (Number(j.expires_in) || 3600) * 1000, env};
  return qfToken.value;
}

async function qfGet(path) {
  const env = qfEnv();
  for (let attempt = 0; attempt < 2; attempt++) {
    const token = await qfAccessToken(attempt > 0);
    const r = await fetch(`${QF_HOSTS[env].api}/content/api/v4${path}`, {
      headers: {"x-auth-token": token, "x-client-id": QF_CLIENT_ID.value().trim(), "Accept": "application/json"}
    });
    if (r.status === 401 && attempt === 0) continue;
    if (!r.ok) {
      const e = new Error(`QF ${r.status}`);
      e.code = r.status === 404 ? 404 : 502;
      throw e;
    }
    return r.json();
  }
  throw Object.assign(new Error("QF auth failed"), {code: 502});
}

async function qfWords(chapter) {
  const verses = [];
  for (let page = 1; page <= 10; page++) {
    const j = await qfGet(`/verses/by_chapter/${chapter}?words=true&word_fields=text_uthmani&language=en&per_page=50&page=${page}`);
    verses.push(...(j.verses || []));
    const next = j.pagination && j.pagination.next_page;
    if (!next) break;
  }
  return {verses};
}

async function qfTimings(chapter, reciter) {
  const j = await qfGet(`/chapter_recitations/${reciter}/${chapter}?segments=true`);
  const a = j.audio_file || {};
  // Same shape the app already parses: audio_files[0].verse_timings[].segments.
  return {audio_files: [{audio_url: a.audio_url, verse_timings: a.timestamps || a.verse_timings || []}]};
}

exports.qfProxy = onRequest(
  {region: REGION, cors: false, timeoutSeconds: 60, memory: "256MiB", maxInstances: 10,
    secrets: [QF_CLIENT_ID, QF_CLIENT_SECRET]},
  async (req, res) => {
    cors(res);
    if (req.method === "OPTIONS") return res.status(204).send("");
    if (req.method !== "GET") return res.status(405).json({error: "Use GET."});
    try {
      await requireAppCheck(req);
    } catch (error) {
      return res.status(401).json({error: "Unauthorized."});
    }
    const kind = cleanString(req.query.kind, "");
    const chapter = Number(req.query.chapter);
    const reciter = Number(req.query.reciter || 7);
    const translation = Number(req.query.translation || 0);
    if (!Number.isInteger(chapter) || chapter < 1 || chapter > 114) return res.status(400).json({error: "Bad chapter."});
    if (!["words", "timings", "translation"].includes(kind)) return res.status(400).json({error: "Bad kind."});
    if (kind === "timings" && ![6, 7, 9, 12].includes(reciter)) return res.status(400).json({error: "Bad reciter."});
    if (kind === "translation" && !(Number.isInteger(translation) && translation > 0 && translation < 10000)) {
      return res.status(400).json({error: "Bad translation."});
    }
    const key = `${qfEnv()}|${kind}|${chapter}|${reciter}|${translation}`;
    const hit = qfCache.get(key);
    if (hit && Date.now() - hit.at < QF_CACHE_MS) {
      res.set("Cache-Control", "private, max-age=86400");
      return res.json(hit.body);
    }
    try {
      const body = kind === "words" ? await qfWords(chapter) :
        kind === "timings" ? await qfTimings(chapter, reciter) :
          await qfGet(`/quran/translations/${translation}?chapter_number=${chapter}`);
      if (qfCache.size > 400) qfCache.clear();
      qfCache.set(key, {at: Date.now(), body});
      res.set("Cache-Control", "private, max-age=86400");
      return res.json(body);
    } catch (error) {
      console.warn("qfProxy failed", kind, chapter, error.message);
      return res.status(error.code === 404 ? 404 : 502).json({error: "Quran data unavailable."});
    }
  }
);

// 1.0.78: push a high-priority data message to the callee when a 1:1 call is created, so the
// app can show a full-screen incoming-call UI. Firestore rules already restrict who may call.
exports.notifyIncomingCall = onDocumentCreated(
    {document: "ummah_calls/{callId}", region: REGION},
    async (event) => {
      const call = event.data && event.data.data();
      if (!call || call.status !== "ringing" || !call.calleeUid) return;
      const db = admin.firestore();
      const tokensSnap = await db.collection("ummah_device_tokens").doc(call.calleeUid).collection("tokens").get();
      const tokens = tokensSnap.docs.map((d) => d.id).filter(Boolean).slice(0, 20);
      if (!tokens.length) return;
      const resp = await admin.messaging().sendEachForMulticast({
        tokens,
        data: {
          kind: "call",
          callId: event.params.callId,
          callerUid: String(call.callerUid || ""),
          callerName: String(call.callerName || "Noor member").slice(0, 60),
          video: call.video ? "1" : "0",
        },
        android: {priority: "high", ttl: 45000},
      });
      // Remove tokens FCM reports as permanently invalid.
      await Promise.all(resp.responses.map((r, i) => {
        const code = r.error && r.error.code;
        if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
          return tokensSnap.docs[i].ref.delete().catch(() => null);
        }
        return null;
      }));
    });


// 1.0.84: keep commentCount accurate when a comment is deleted (author delete or account
// deletion). Clients may only ever +1 commentCount (rules), so the trusted -1 lives here.
// Transaction clamps at 0; falls back to a pending ummah_submissions doc like the client does.
exports.decrementCommentCountOnDelete = onDocumentDeleted(
    {document: "ummah_comments/{commentId}", region: REGION},
    async (event) => {
      const c = event.data && event.data.data();
      const postId = c && typeof c.postId === "string" ? c.postId.trim() : "";
      if (!postId || postId.includes("/")) return;
      const db = admin.firestore();
      for (const col of ["ummah_posts", "ummah_submissions"]) {
        const ref = db.collection(col).doc(postId);
        const done = await db.runTransaction(async (tx) => {
          const snap = await tx.get(ref);
          if (!snap.exists) return false;
          const cur = Number(snap.get("commentCount")) || 0;
          tx.update(ref, {commentCount: Math.max(0, cur - 1)});
          return true;
        });
        if (done) return;
      }
    },
);

// 1.0.86: featured YouTube channel shelf (MAF Nasheed). Returns public video metadata only
// (id, title, date, thumbnail) — playback stays in YouTube's official embedded player in-app.
// Allow-listed channel IDs so this cannot be used as an open proxy. Cached in memory for 3 h.
const YT_CHANNELS = {"UCDPznGiG1OrrNsRgmSCcAgg": "MAF Nasheed Channel"};
const ytCache = new Map();
const YT_CACHE_MS = 3 * 60 * 60 * 1000;
const xmlText = (s) => String(s || "").replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">")
  .replace(/&quot;/g, "\"").replace(/&#39;/g, "'");

async function ytFromRss(channelId) {
  const r = await fetch(`https://www.youtube.com/feeds/videos.xml?channel_id=${channelId}`);
  if (!r.ok) throw new Error(`rss ${r.status}`);
  const xml = await r.text();
  const videos = [...xml.matchAll(/<entry>([\s\S]*?)<\/entry>/g)].map((m) => {
    const e = m[1];
    const id = (e.match(/<yt:videoId>([\w-]{11})<\/yt:videoId>/) || [])[1];
    return id && {id, title: xmlText((e.match(/<title>([\s\S]*?)<\/title>/) || [])[1]),
      published: (e.match(/<published>([^<]+)<\/published>/) || [])[1] || "", ago: "",
      thumb: `https://i.ytimg.com/vi/${id}/hqdefault.jpg`};
  }).filter(Boolean);
  if (!videos.length) throw new Error("rss empty");
  return {videos, avatar: "", source: "rss"};
}

async function ytFromChannelPage(channelId) {
  const r = await fetch(`https://www.youtube.com/channel/${channelId}/videos`,
    {headers: {"User-Agent": "Mozilla/5.0", "Accept-Language": "en"}});
  if (!r.ok) throw new Error(`page ${r.status}`);
  const html = await r.text();
  const m = html.match(/var ytInitialData = (\{[\s\S]*?\});<\/script>/);
  if (!m) throw new Error("no data");
  const videos = [];
  const walk = (o) => {
    if (Array.isArray(o)) return o.forEach(walk);
    if (!o || typeof o !== "object") return;
    const l = o.lockupViewModel;
    if (l && /^[\w-]{11}$/.test(l.contentId || "")) {
      const md = (l.metadata || {}).lockupMetadataViewModel || {};
      const rows = (((md.metadata || {}).contentMetadataViewModel || {}).metadataRows || []);
      const parts = rows.flatMap((row) => (row.metadataParts || []).map((p) => ((p.text || {}).content || "")));
      let durationSec = 0;
      const findDur = (x) => {
        if (durationSec || !x || typeof x !== "object") return;
        for (const val of Object.values(x)) {
          if (typeof val === "string" && /^\d{1,2}(:\d{2}){1,2}$/.test(val)) {
            durationSec = val.split(":").reduce((a, n) => a * 60 + Number(n), 0); return;
          }
          findDur(val);
        }
      };
      findDur(l.contentImage);
      videos.push({id: l.contentId, title: ((md.title || {}).content || ""), published: "", durationSec,
        ago: parts.find((p) => / ago$/.test(p)) || "", thumb: `https://i.ytimg.com/vi/${l.contentId}/hqdefault.jpg`});
    }
    Object.values(o).forEach(walk);
  };
  walk(JSON.parse(m[1]));
  if (!videos.length) throw new Error("page empty");
  const avatar = (html.match(/<meta property="og:image" content="([^"]+)"/) || [])[1] || "";
  return {videos, avatar, source: "page"};
}

// Owner-uploaded audio for Audio mode: Firestore docs nasheed_audio/{videoId} {url: "https://..."}.
// Read with the Admin SDK (no client rules needed). Only https URLs are passed through. Never YouTube audio.
let ytAudio = {at: 0, map: {}};
async function ytAudioMap() {
  if (Date.now() - ytAudio.at < 10 * 60 * 1000) return ytAudio.map;
  try {
    const snap = await admin.firestore().collection("nasheed_audio").limit(500).get();
    const map = {};
    snap.forEach((d) => {
      const url = String((d.data() || {}).url || "").trim();
      if (/^[\w-]{11}$/.test(d.id) && /^https:\/\//.test(url) && !/youtube\.com|youtu\.be|googlevideo\.com/.test(url)) map[d.id] = url;
    });
    ytAudio = {at: Date.now(), map};
  } catch (error) {
    console.warn("ytAudioMap", error.message);
  }
  return ytAudio.map;
}
// 1.0.90: owner audio that is not (yet) on YouTube. Shown in the app as audio-only "New" tracks until a
// channel video appears whose title fuzzy-matches or whose duration is within 3 s; then it is linked
// automatically (nasheed_audio/{videoId}). Ambiguous matches are only logged / reported, never guessed.
// Extra entries can be added as Firestore docs nasheed_pending/{key} {title, titleEn, url, durationSec}.
const MAF_MEDIA = "https://noorpro-media.web.app/maf";
const NASHEED_PENDING = [
  {key: "abhi-umeed-baqi-hai", title: "ابھی اُمید باقی ہے", titleEn: "Abhi Umeed Baqi Hai",
    url: `${MAF_MEDIA}/abhi-umeed-baqi-hai.mp3`, durationSec: 367.6},
  {key: "safar-mein-tera-sahara", title: "سفر میں تیرا سہارا", titleEn: "Safar Mein Tera Sahara",
    url: `${MAF_MEDIA}/safar-mein-tera-sahara.mp3`, durationSec: 308.8},
];
const ytNormArabic = (s) => String(s || "").replace(/[\u064B-\u065F\u0670\u06EA-\u06ED\u200C\u200D]/g, "")
  .replace(/[^\u0600-\u06FF]/g, "");
const ytNormLatin = (s) => String(s || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase()
  .replace(/[^a-z]/g, "").replace(/(ee|ea|ie)/g, "i").replace(/(oo|ou)/g, "u").replace(/(.)\1+/g, "$1");
const ytTitleMatch = (video, p) => {
  const va = ytNormArabic(video.title); const pa = ytNormArabic(p.title);
  if (pa.length >= 6 && va.includes(pa)) return true;
  const vl = ytNormLatin(video.title); const pl = ytNormLatin(p.titleEn);
  return pl.length >= 8 && vl.includes(pl);
};
let ytPending = {at: 0, list: NASHEED_PENDING};
async function ytPendingList() {
  if (Date.now() - ytPending.at < 10 * 60 * 1000) return ytPending.list;
  const list = [...NASHEED_PENDING];
  try {
    const snap = await admin.firestore().collection("nasheed_pending").limit(100).get();
    snap.forEach((d) => {
      const x = d.data() || {};
      if (/^https:\/\//.test(String(x.url || "")) && !list.some((p) => p.key === d.id)) {
        list.push({key: d.id, title: String(x.title || ""), titleEn: String(x.titleEn || ""), url: String(x.url),
          durationSec: Number(x.durationSec) || 0});
      }
    });
  } catch (error) {
    console.warn("ytPendingList", error.message);
  }
  ytPending = {at: Date.now(), list};
  return list;
}
async function ytAutoLink(videos, map) {
  const pending = await ytPendingList();
  const linkedUrls = new Set(Object.values(map));
  const audioOnly = []; const ambiguous = [];
  for (const p of pending) {
    if (linkedUrls.has(p.url)) continue;
    const cands = videos.filter((v) => !map[v.id] && (ytTitleMatch(v, p) ||
      (p.durationSec > 0 && v.durationSec > 0 && Math.abs(v.durationSec - p.durationSec) <= 3)));
    const strong = cands.filter((v) => ytTitleMatch(v, p));
    const pick = strong.length === 1 ? strong[0] : (cands.length === 1 ? cands[0] : null);
    if (pick) {
      try {
        await admin.firestore().collection("nasheed_audio").doc(pick.id).set({url: p.url, title: p.title,
          pendingKey: p.key, linkedBy: "youtubeChannelFeed", linkedAt: new Date().toISOString()}, {merge: true});
        map[pick.id] = p.url; ytAudio.at = 0;
        console.log("ytAutoLink", p.key, "->", pick.id);
        continue;
      } catch (error) {
        console.warn("ytAutoLink write", error.message);
      }
    }
    if (cands.length > 1) ambiguous.push({key: p.key, candidates: cands.map((v) => ({id: v.id, title: v.title, durationSec: v.durationSec || 0}))});
    audioOnly.push({key: p.key, title: p.title, titleEn: p.titleEn, audioUrl: p.url, durationSec: p.durationSec, isNew: true});
  }
  if (ambiguous.length) console.warn("ytAutoLink ambiguous", JSON.stringify(ambiguous));
  return {audioOnly, ambiguous};
}
const withAudio = async (body) => {
  const map = {...await ytAudioMap()};
  const {audioOnly, ambiguous} = await ytAutoLink(body.videos, map);
  return {...body, audioOnly, audioAmbiguous: ambiguous,
    videos: body.videos.map((v) => ({...v, audioUrl: map[v.id] || ""}))};
};

exports.youtubeChannelFeed = onRequest(
  {region: REGION, cors: false, timeoutSeconds: 30, memory: "256MiB", maxInstances: 5},
  async (req, res) => {
    cors(res);
    if (req.method === "OPTIONS") return res.status(204).send("");
    if (req.method !== "GET") return res.status(405).json({error: "Use GET."});
    const channelId = cleanString(req.query.channel, "UCDPznGiG1OrrNsRgmSCcAgg");
    if (!YT_CHANNELS[channelId]) return res.status(400).json({error: "Unknown channel."});
    const hit = ytCache.get(channelId);
    if (hit && Date.now() - hit.at < YT_CACHE_MS) {
      res.set("Cache-Control", "public, max-age=600");
      return res.json(await withAudio(hit.body));
    }
    // RSS has exact dates (latest 15); the public channel page lists more. Merge both by video id.
    const [rss, page] = await Promise.allSettled([ytFromRss(channelId), ytFromChannelPage(channelId)]);
    if (rss.status === "rejected") console.warn("youtubeChannelFeed rss", rss.reason && rss.reason.message);
    if (page.status === "rejected") console.warn("youtubeChannelFeed page", page.reason && page.reason.message);
    const r1 = rss.status === "fulfilled" ? rss.value.videos : [];
    const p1 = page.status === "fulfilled" ? page.value : null;
    if (!r1.length && !p1) {
      if (hit) return res.json(await withAudio(hit.body));
      return res.status(502).json({error: "Channel feed unavailable."});
    }
    const byId = new Map(r1.map((v) => [v.id, v]));
    const merged = p1 ? p1.videos.map((v) => byId.has(v.id) ? {...byId.get(v.id), ago: v.ago} : v) : r1;
    r1.forEach((v) => {
      if (!merged.some((x) => x.id === v.id)) merged.push(v);
    });
    const data = {videos: merged, avatar: p1 ? p1.avatar : "", source: [r1.length && "rss", p1 && "page"].filter(Boolean).join("+")};
    const body = {channelId, title: YT_CHANNELS[channelId], url: `https://www.youtube.com/channel/${channelId}`,
      avatar: data.avatar, source: data.source, updatedAt: new Date().toISOString(), videos: data.videos.slice(0, 50)};
    ytCache.set(channelId, {at: Date.now(), body});
    res.set("Cache-Control", "public, max-age=600");
    return res.json(await withAudio(body));
  }
);

// 1.0.91: index any post written to ummah_posts by another path (admin tools, legacy code).
exports.indexReelSearchOnCreate = onDocumentCreated(
  {document: "ummah_posts/{postId}", region: REGION, timeoutSeconds: 30, memory: "256MiB"},
  async (event) => {
    if (!event.data) return;
    await indexReelDoc(event.data.ref, event.data.data()).catch((e) => console.warn("indexReelSearch", event.params.postId, e.message));
  }
);

// 1.0.91: one-off / repeatable backfill of the search arrays on existing published posts.
// Idempotent (skips docs already indexed with the same source text), bounded to 400 docs per call,
// writes only derived fields. Call repeatedly with ?after=<lastId> until done=true.
exports.backfillReelSearch = onRequest(
  {region: REGION, timeoutSeconds: 120, memory: "256MiB", maxInstances: 1},
  async (req, res) => {
    if (req.method !== "POST") return res.status(405).json({error: "Use POST."});
    const db = admin.firestore();
    let q = db.collection("ummah_posts").orderBy(admin.firestore.FieldPath.documentId()).limit(400);
    const after = cleanString(req.query.after, "");
    if (after) q = q.startAfter(after);
    const snap = await q.get();
    let updated = 0;
    for (const doc of snap.docs) {
      if (await indexReelDoc(doc.ref, doc.data()).catch(() => false)) updated++;
    }
    const last = snap.docs.length ? snap.docs[snap.docs.length - 1].id : "";
    return res.json({scanned: snap.size, updated, last, done: snap.size < 400});
  }
);

// 1.0.92: people search index on ummah_profiles — `searchKeys` = prefixes (2..15) of the normalized
// @username and each name word, plus `nameLower`. Derived fields only; skipped when unchanged so the
// write trigger never loops.
function peopleSearchFields(d) {
  const handle = reelSearchKey(String(d.handle || "").replace(/^@/, ""));
  const keys = new Set();
  for (const t of [handle, ...reelSearchTokens(d.name), reelSearchKey(d.name)]) {
    for (let i = 1; i <= Math.min(t.length, 15); i++) keys.add(t.slice(0, i));
  }
  return {searchKeys: [...keys].slice(0, 120), nameLower: String(d.name || "").toLowerCase().slice(0, 60),
    peopleSrc: `1|${d.name || ""}|${d.handle || ""}`.slice(0, 200)};
}
async function indexPersonDoc(ref, data) {
  const f = peopleSearchFields(data || {});
  if ((data || {}).peopleSrc === f.peopleSrc && Array.isArray((data || {}).searchKeys)) return false;
  await ref.set(f, {merge: true});
  return true;
}
exports.indexPeopleSearch = onDocumentWritten(
  {document: "ummah_profiles/{uid}", region: REGION, timeoutSeconds: 30, memory: "256MiB"},
  async (event) => {
    const after = event.data && event.data.after;
    if (!after || !after.exists) return;
    await indexPersonDoc(after.ref, after.data()).catch((e) => console.warn("indexPeopleSearch", event.params.uid, e.message));
  }
);
exports.backfillPeopleSearch = onRequest(
  {region: REGION, timeoutSeconds: 120, memory: "256MiB", maxInstances: 1},
  async (req, res) => {
    if (req.method !== "POST") return res.status(405).json({error: "Use POST."});
    let q = admin.firestore().collection("ummah_profiles").orderBy(admin.firestore.FieldPath.documentId()).limit(400);
    const after = cleanString(req.query.after, "");
    if (after) q = q.startAfter(after);
    const snap = await q.get();
    let updated = 0;
    for (const doc of snap.docs) if (await indexPersonDoc(doc.ref, doc.data()).catch(() => false)) updated++;
    const last = snap.docs.length ? snap.docs[snap.docs.length - 1].id : "";
    return res.json({scanned: snap.size, updated, last, done: snap.size < 400});
  }
);

// 1.1.4 Creator Studio: nightly per-creator totals so the app can chart per-day views/likes.
// OFF unless CREATOR_DAILY_STATS=true is set in functions/.env (and the app flag
// creator_daily_stats_enabled is on). Cost: one read per published post per day + one write per creator.
const creatorStats = require("./creator");
exports.creatorDailySnapshot = onSchedule(
  {schedule: "every day 23:55", timeZone: "Asia/Kolkata", region: REGION, timeoutSeconds: 540, memory: "512MiB"},
  async () => {
    if (!creatorStats.dailyStatsEnabled(process.env)) return console.log("creatorDailySnapshot disabled");
    const db = admin.firestore();
    const day = creatorStats.dayId(new Date());
    const all = [];
    let last = null;
    for (;;) {
      let q = db.collection("ummah_posts").orderBy(admin.firestore.FieldPath.documentId()).limit(1000);
      if (last) q = q.startAfter(last);
      const snap = await q.get();
      snap.docs.forEach((d) => all.push(d.data()));
      if (snap.size < 1000) break;
      last = snap.docs[snap.docs.length - 1];
    }
    const totals = creatorStats.aggregateCreatorTotals(all);
    let batch = db.batch(); let ops = 0;
    for (const [uid, t] of totals) {
      batch.set(db.doc(`ummah_creator_stats/${uid}/days/${day}`), {...t, day, at: admin.firestore.FieldValue.serverTimestamp()});
      if (++ops === 400) { await batch.commit(); batch = db.batch(); ops = 0; }
    }
    if (ops) await batch.commit();
    console.log(`creatorDailySnapshot ${day}: ${totals.size} creators from ${all.length} posts`);
  }
);

// =====================================================================================
// 1.1.5 Halal Guard. Pure logic lives in halal.js. Every check below is behind an env flag that
// defaults OFF (functions/.env): HALAL_GUARD_SCAN, HALAL_GUARD_VIDEO, HALAL_TEXT_FILTER,
// HALAL_NEW_ACCOUNT_QUEUE. Vision / Video Intelligence use the functions' default service account
// (no API key); the client libraries are required lazily so nothing loads while the flags are off.
// =====================================================================================
// Trigger functions are only exported when their flag is on at deploy time (.env is read during
// deploy), so with the defaults they don't exist and cost nothing. Flip a flag -> redeploy.
const HG = halal.guardFlags();
let visionSingleton = null;
let videoSingleton = null;
function guardDeps(flags) {
  return {
    flags,
    fetchBytes: guardFetchBytes,
    get vision() {
      if (!visionSingleton) {
        const {ImageAnnotatorClient} = require("@google-cloud/vision");
        visionSingleton = new ImageAnnotatorClient();
      }
      return visionSingleton;
    },
    get video() {
      if (!videoSingleton) {
        const {VideoIntelligenceServiceClient} = require("@google-cloud/video-intelligence");
        videoSingleton = new VideoIntelligenceServiceClient();
      }
      return videoSingleton;
    }
  };
}

// Only our own media hosts are fetched (no SSRF to arbitrary URLs); anything else is queued.
function guardHostAllowed(url) {
  let host = "";
  try { host = new URL(url).hostname.toLowerCase(); } catch (e) { return false; }
  const cdn = String(process.env.NOOR_CDN_DOMAIN || "").toLowerCase();
  return host === "firebasestorage.googleapis.com" || host === "storage.googleapis.com" ||
    host.endsWith(".firebasestorage.app") || host.endsWith(".cloudfront.net") || (cdn && host === cdn);
}
async function guardFetchBytes(url, max) {
  if (!/^https:\/\//i.test(String(url || "")) || !guardHostAllowed(url)) return null;
  const res = await fetch(url, {redirect: "follow"});
  if (!res.ok) throw new Error(`media fetch ${res.status}`);
  const len = Number(res.headers.get("content-length") || 0);
  if (len && len > max) return null;
  const buf = Buffer.from(await res.arrayBuffer());
  return buf.length > max ? null : buf;
}

function guardSummary(g) {
  return {
    decision: g.decision || "approve",
    reasons: Array.isArray(g.reasons) ? g.reasons.slice(0, 10) : [],
    scan: g.scan || null,
    text: g.text || {},
    checkedAt: admin.firestore.FieldValue.serverTimestamp()
  };
}

async function addModerationNotice(uid, {kind, targetId, reason}) {
  if (!uid) return;
  await admin.firestore().collection("moderation_notices").doc(uid).collection("items").add({
    kind: String(kind || "post"), targetId: String(targetId || "").slice(0, 200),
    reason: String(reason || "").slice(0, 500), seen: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  }).catch((e) => console.warn("notice failed", uid, e));
}

async function loadStrikes(uid) {
  if (!uid) return null;
  try {
    const snap = await admin.firestore().collection("user_strikes").doc(uid).get();
    return snap.exists ? snap.data() : null;
  } catch (e) {
    console.warn("strikes read failed", uid, e);
    return null;
  }
}

async function guardSubmission(data, submissionId, ctx) {
  const flags = halal.guardFlags();
  const anyFlag = flags.scan || flags.text || flags.newAccount;
  const strikes = await loadStrikes(ctx.creatorUid);
  if (!anyFlag) {
    if (halal.isRestricted(strikes, Date.now())) return {...(await halal.guardContent({strikes}, {flags: {}})), ran: true};
    return {decision: "approve", reasons: [], masked: {}, ran: false};
  }
  try {
    const images = [];
    const video = (ctx.type === "reel" || ctx.type === "video") ? {
      gsUri: cleanString(data.storageProvider, "firebase") === "firebase" && cleanString(data.mediaPath, "") ?
        `gs://${admin.storage().bucket().name}/${cleanString(data.mediaPath, "")}` : "",
      url: ctx.mediaUrl
    } : null;
    if (ctx.type === "image") {
      const urls = Array.isArray(data.mediaUrls) ? data.mediaUrls.filter((u) => typeof u === "string") : [];
      images.push(...(urls.length ? urls : [ctx.mediaUrl]));
    } else if (video) {
      const thumb = cleanString(data.thumbnailUrl, "");
      if (thumb) images.push(thumb);
    }
    let createdMs = 0;
    let priorPublished = 0;
    if (flags.newAccount) {
      const user = await admin.auth().getUser(ctx.creatorUid);
      createdMs = Date.parse(user.metadata.creationTime) || 0;
      priorPublished = (await admin.firestore().collection("ummah_posts")
        .where("creatorUid", "==", ctx.creatorUid).count().get()).data().count;
    }
    const result = await halal.guardContent({
      texts: {caption: ctx.caption, arabicText: ctx.arabicText}, images, video, createdMs, priorPublished, strikes
    }, guardDeps(flags));
    return {...result, ran: true};
  } catch (error) {
    console.error("Halal Guard failed; queueing", submissionId, error);
    return {decision: "queue", reasons: ["guard_error"], userReason: halal.USER_REASON.review, masked: {}, scan: null, ran: true};
  }
}

async function enqueue(id, item) {
  await admin.firestore().collection("moderation_queue").doc(id).set({
    ...item, status: "open", createdAt: admin.firestore.FieldValue.serverTimestamp()
  }, {merge: true});
}

// Stories are written by the client straight into ummah_stories, so they are checked after create.
if (HG.scan || HG.text) exports.halalGuardStory = onDocumentCreated(
  {document: "ummah_stories/{storyId}", region: REGION, timeoutSeconds: 300, memory: "512MiB"},
  async (event) => {
    const flags = halal.guardFlags();
    if (!event.data || !(flags.scan || flags.text)) return;
    const d = event.data.data() || {};
    if (cleanString(d.sharedPostId, "")) return; // re-share of an already-moderated post
    const uid = cleanString(d.authorUid, "");
    const type = cleanString(d.type, "image");
    const mediaUrl = cleanString(d.mediaUrl, "");
    const mediaPath = cleanString(d.mediaPath, "");
    const images = type === "video" ? [cleanString(d.thumbUrl, "")].filter(Boolean) : [mediaUrl].filter(Boolean);
    const video = type === "video" ? {gsUri: mediaPath ? `gs://${admin.storage().bucket().name}/${mediaPath}` : "", url: mediaUrl} : null;
    let g;
    try {
      g = await halal.guardContent({texts: {caption: cleanString(d.caption, "")}, images, video, strikes: await loadStrikes(uid)}, guardDeps(flags));
    } catch (e) {
      g = {decision: "queue", reasons: ["guard_error"], userReason: halal.USER_REASON.review, masked: {}};
    }
    if (g.decision === "reject") {
      const bucket = admin.storage().bucket();
      for (const field of ["mediaPath", "thumbPath"]) {
        const path = cleanString(d[field], "");
        if (path.startsWith("ummah_stories/")) await bucket.file(path).delete().catch(() => {});
      }
      await event.data.ref.delete();
      await addModerationNotice(uid, {kind: "story", targetId: event.params.storyId, reason: g.userReason});
      return;
    }
    const update = {moderation: guardSummary(g)};
    if (g.masked.caption) update.caption = g.masked.caption;
    if (g.decision === "queue") {
      update.inReview = true;
      await enqueue(`story_${event.params.storyId}`, {kind: "story", targetId: event.params.storyId, creatorUid: uid,
        creatorName: cleanString(d.authorName, ""), type, caption: cleanString(d.caption, "").slice(0, 300),
        mediaUrl, thumbnailUrl: cleanString(d.thumbUrl, ""), reasons: g.reasons, scan: g.scan || null});
    }
    await event.data.ref.update(update);
  }
);

async function textGuard(value) {
  const r = halal.scanText(value || "");
  return {r, masked: r.action === "mask" ? halal.maskText(value, r) : "", reason: halal.textUserReason(r)};
}

// Comments: severe -> removed (deleted, which also decrements the post's count), hate/scam ->
// hidden pending review, mild -> masked. Suspended/banned accounts' comments are removed.
if (HG.text || HG.scan || HG.newAccount) exports.halalGuardComment = onDocumentCreated(
  {document: "ummah_comments/{commentId}", region: REGION, timeoutSeconds: 30, memory: "256MiB"},
  async (event) => {
    const flags = halal.guardFlags();
    if (!event.data || !(flags.text || flags.scan || flags.newAccount)) return;
    const d = event.data.data() || {};
    const uid = cleanString(d.creatorUid, "");
    const text = cleanString(d.text, "");
    if (halal.isRestricted(await loadStrikes(uid), Date.now())) {
      await event.data.ref.delete();
      await addModerationNotice(uid, {kind: "comment", targetId: event.params.commentId, reason: halal.USER_REASON.restricted});
      return;
    }
    if (!flags.text) return;
    const {r, masked, reason} = await textGuard(text);
    if (r.action === "block") {
      await event.data.ref.delete();
      await addModerationNotice(uid, {kind: "comment", targetId: event.params.commentId, reason});
      await admin.firestore().collection("moderation_audit").add({actorUid: "system", action: "auto_remove", target: "comment",
        id: event.params.commentId, subjectUid: uid, reason: r.categories.join(","), at: admin.firestore.FieldValue.serverTimestamp()});
    } else if (r.action === "queue") {
      await event.data.ref.update({status: "in_review"});
      await enqueue(`comment_${event.params.commentId}`, {kind: "comment", targetId: event.params.commentId, creatorUid: uid,
        creatorName: cleanString(d.creatorName, ""), type: "comment", caption: text.slice(0, 600), postId: cleanString(d.postId, ""),
        reasons: r.categories.map((c) => `text_${c}`)});
    } else if (r.action === "mask") {
      await event.data.ref.update({text: masked});
    }
  }
);

// Caption edits (Creator Studio) skip the publisher, so re-check them here.
if (HG.text) exports.halalGuardCaptionEdit = onDocumentUpdated(
  {document: "ummah_posts/{postId}", region: REGION, timeoutSeconds: 30, memory: "256MiB"},
  async (event) => {
    if (!halal.guardFlags().text || !event.data) return;
    const before = event.data.before.data() || {};
    const after = event.data.after.data() || {};
    if ((before.caption || "") === (after.caption || "")) return;
    const {r, masked, reason} = await textGuard(after.caption || "");
    if (r.action === "block") {
      await event.data.after.ref.update({caption: before.caption || ""});
      await addModerationNotice(after.creatorUid, {kind: "caption", targetId: event.params.postId, reason});
    } else if (r.action === "mask") {
      await event.data.after.ref.update({caption: masked});
    } else if (r.action === "queue") {
      await enqueue(`edit_${event.params.postId}`, {kind: "post_edit", targetId: event.params.postId, creatorUid: after.creatorUid || "",
        creatorName: after.creatorName || "", type: after.type || "text", caption: String(after.caption || "").slice(0, 600),
        mediaUrl: after.mediaUrl || "", thumbnailUrl: after.thumbnailUrl || "", reasons: r.categories.map((c) => `text_${c}`)});
    }
  }
);

// Profiles: avatar SafeSearch + name/bio text filter.
if (HG.scan || HG.text) exports.halalGuardProfile = onDocumentWritten(
  {document: "ummah_profiles/{uid}", region: REGION, timeoutSeconds: 60, memory: "256MiB"},
  async (event) => {
    const flags = halal.guardFlags();
    if (!event.data || !event.data.after.exists || !(flags.scan || flags.text)) return;
    const before = event.data.before.exists ? event.data.before.data() : {};
    const after = event.data.after.data() || {};
    const uid = event.params.uid;
    const update = {};
    if (flags.text) {
      for (const field of ["name", "bio"]) {
        if ((before[field] || "") === (after[field] || "") || !after[field]) continue;
        const {r, masked, reason} = await textGuard(after[field]);
        if (r.action === "block") {
          update[field] = field === "name" ? (before.name || "Community member") : (before.bio || "");
          await addModerationNotice(uid, {kind: `profile_${field}`, targetId: uid, reason});
        } else if (r.action === "mask") update[field] = masked;
      }
    }
    const photo = cleanString(after.photoUrl, "");
    if (flags.scan && photo && photo !== cleanString(before.photoUrl, "")) {
      const s = await halal.scanMedia({images: [photo]}, guardDeps(flags));
      if (s.decision === "reject") {
        update.photoUrl = "";
        await addModerationNotice(uid, {kind: "avatar", targetId: uid, reason: s.userReason});
      } else if (s.decision === "queue") {
        await enqueue(`avatar_${uid}`, {kind: "avatar", targetId: uid, creatorUid: uid, creatorName: after.name || "",
          type: "image", caption: "", mediaUrl: photo, reasons: [s.reason]});
      }
    }
    if (Object.keys(update).length) await event.data.after.ref.update(update);
  }
);

// Group names / descriptions (server side; the app also warns while typing).
if (HG.text) exports.halalGuardGroup = onDocumentCreated(
  {document: "ummah_groups/{groupId}", region: REGION, timeoutSeconds: 30, memory: "256MiB"},
  async (event) => {
    if (!halal.guardFlags().text || !event.data) return;
    const d = event.data.data() || {};
    const update = {};
    for (const field of ["name", "description"]) {
      const {r, masked, reason} = await textGuard(d[field] || "");
      if (r.action === "block") {
        update[field] = field === "name" ? "Group" : "";
        await addModerationNotice(d.ownerUid, {kind: "group", targetId: event.params.groupId, reason});
      } else if (r.action === "mask") update[field] = masked;
    }
    if (Object.keys(update).length) await event.data.ref.update(update);
  }
);

async function isModeratorUid(auth) {
  if (!auth || !auth.uid) return false;
  if (auth.token && auth.token.admin === true) return true;
  return (await admin.firestore().collection("admins").doc(auth.uid).get()).exists;
}

async function removeContent(kind, targetId, subjectUid) {
  const db = admin.firestore();
  switch (kind) {
  case "post": case "post_edit": {
    await db.collection("ummah_submissions").doc(targetId).update({status: "removed", approved: false}).catch(() => {});
    await db.collection("ummah_posts").doc(targetId).delete().catch(() => {});
    await db.collection("ummah_follower_posts").doc(targetId).delete().catch(() => {});
    return;
  }
  case "comment": await db.collection("ummah_comments").doc(targetId).delete().catch(() => {}); return;
  case "story": await db.recursiveDelete(db.collection("ummah_stories").doc(targetId)).catch(() => {}); return;
  case "avatar": case "user": case "profile":
    await db.collection("ummah_profiles").doc(subjectUid || targetId).update({photoUrl: ""}).catch(() => {}); return;
  default: return; // messages etc.: reported content is kept; the account action is what applies
  }
}

async function approveContent(kind, targetId, sensitive) {
  const db = admin.firestore();
  if (kind === "post") {
    const ref = db.collection("ummah_submissions").doc(targetId);
    const snap = await ref.get();
    if (snap.exists) return promoteUmmahSubmission(snap, targetId, {moderatorApproved: true, sensitive});
  } else if (kind === "comment") {
    await db.collection("ummah_comments").doc(targetId).update({status: "published"}).catch(() => {});
  } else if (kind === "story") {
    await db.collection("ummah_stories").doc(targetId).update({inReview: false, sensitive}).catch(() => {});
  }
  return null;
}

// Admin-only moderation decisions (queue items, reports, or a user directly). Caller must have the
// `admin` custom claim or an admins/{uid} document. Every call is written to moderation_audit.
exports.setModerationDecision = onCall(
  {region: REGION, timeoutSeconds: 300, memory: "512MiB", maxInstances: 5},
  async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Sign in required.");
    if (!(await isModeratorUid(request.auth))) throw new HttpsError("permission-denied", "Moderators only.");
    const v = halal.validateDecision(request.data);
    if (!v.ok) throw new HttpsError("invalid-argument", v.error);
    const {action, target, id, reason, days, sensitive, strike} = v.value;
    const db = admin.firestore();
    let docRef = null;
    let item = {};
    if (target === "queue") docRef = db.collection("moderation_queue").doc(id);
    if (target === "report") docRef = db.collection("ummah_reports").doc(id);
    if (docRef) {
      const snap = await docRef.get();
      if (!snap.exists) throw new HttpsError("not-found", "Item not found.");
      item = snap.data() || {};
    }
    const kind = target === "user" ? "user" : (target === "report" ? String(item.targetType || "post") : String(item.kind || "post"));
    const targetId = target === "user" ? id : String(item.targetId || item.postId || "");
    const subjectUid = target === "user" ? id : String(item.creatorUid || item.targetUid || "");
    const now = Date.now();
    const effects = halal.decisionEffects(action, strike);
    let strikes = null;
    for (const effect of effects) {
      if (effect === "publish") await approveContent(kind, targetId, sensitive);
      if (effect === "remove_content" && targetId) await removeContent(kind, targetId, subjectUid);
      if (["strike", "suspend", "ban", "unban"].includes(effect) && subjectUid) {
        const ref = db.collection("user_strikes").doc(subjectUid);
        strikes = await db.runTransaction(async (tx) => {
          const cur = (await tx.get(ref)).data() || {};
          let next = {...cur};
          if (effect === "strike") next = {...cur, ...halal.applyStrike(cur, now, days)};
          if (effect === "suspend") next = {...cur, suspendedUntilMs: now + days * halal.DAY_MS};
          if (effect === "ban") next = {...cur, banned: true};
          if (effect === "unban") next = {...cur, banned: false, suspendedUntilMs: 0};
          next.updatedAt = admin.firestore.FieldValue.serverTimestamp();
          tx.set(ref, next, {merge: true});
          return next;
        });
        if (effect === "ban") {
          await admin.auth().updateUser(subjectUid, {disabled: true}).catch((e) => console.warn("disable failed", e));
          await admin.auth().revokeRefreshTokens(subjectUid).catch(() => {});
        }
        if (effect === "unban") await admin.auth().updateUser(subjectUid, {disabled: false}).catch(() => {});
      }
      if (effect === "notify" && subjectUid) {
        const until = strikes && strikes.suspendedUntilMs > now ? ` Your account is suspended until ${new Date(strikes.suspendedUntilMs).toISOString().slice(0, 10)}.` : "";
        await addModerationNotice(subjectUid, {kind: action, targetId,
          reason: `${reason || "A moderator reviewed your content under the Halal community guidelines."}${until}`});
      }
      if (effect === "close" && docRef) {
        await docRef.update({status: "resolved", resolution: action, resolvedBy: request.auth.uid,
          resolvedAt: admin.firestore.FieldValue.serverTimestamp()});
      }
    }
    await db.collection("moderation_audit").add({
      actorUid: request.auth.uid, action, target, id, kind, targetId, subjectUid, reason, days, sensitive,
      effects, strikeCount: strikes ? Number(strikes.count) || 0 : null, at: admin.firestore.FieldValue.serverTimestamp()
    });
    return {ok: true, effects, strikes: strikes ? {count: Number(strikes.count) || 0, suspendedUntilMs: Number(strikes.suspendedUntilMs) || 0, banned: strikes.banned === true} : null};
  }
);
