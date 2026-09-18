const admin = require("firebase-admin");
const {onRequest} = require("firebase-functions/v2/https");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
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
// Resend API key is stored in Google Secret Manager, never in source or the app.
// Set it once with:  firebase functions:secrets:set RESEND_API_KEY
const RESEND_API_KEY = defineSecret("RESEND_API_KEY");
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
// NVIDIA NIM (OpenAI-compatible) endpoint + model. Override via env if needed.
const NVIDIA_BASE_URL =
  process.env.NOOR_AI_BASE_URL || "https://integrate.api.nvidia.com/v1/chat/completions";
const MODEL = process.env.NOOR_AI_MODEL || "meta/llama-3.3-70b-instruct";
const MAX_PROMPT_CHARS = 2500;

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
 * Require a valid Firebase App Check token. In production this blocks scrapers/scripts.
 * Set NOOR_REQUIRE_APPCHECK=false only for local function emulators without App Check.
 */
async function requireAppCheck(req) {
  if ((process.env.NOOR_REQUIRE_APPCHECK || "true").toLowerCase() === "false") {
    return;
  }
  const appCheckToken = req.get("X-Firebase-AppCheck") || "";
  if (!appCheckToken) {
    const err = new Error("Missing App Check token");
    err.code = 401;
    throw err;
  }
  await admin.appCheck().verifyToken(appCheckToken);
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

function buildSystemPrompt(mode) {
  const modeText = modeInstructions[mode] || modeInstructions.AskNoor;
  return [
    "You are Noor AI inside an Islamic mobile app.",
    modeText,
    "Use clear, gentle English. If useful, include short Urdu-friendly wording, but do not overdo it.",
    "Whenever you include a dua, a Quran ayah, or any Arabic phrase, present it in three lines: 'Arabic: <arabic script>', 'Transliteration: <latin>', 'Meaning: <english>'. Keep the Arabic script accurate; if unsure of exact wording, give the meaning only and say to verify.",
    "Never claim to be a scholar. Do not give final fatwa.",
    "For divorce, inheritance, medical, legal, self-harm, abuse, or complex rulings, tell the user to consult a qualified scholar or relevant professional.",
    "If citing Quran or Hadith, only cite when you are confident. Otherwise say to verify with reliable sources.",
    "Always end with: Allah knows best."
  ].join("\n");
}

exports.askNoorAi = onRequest(
  {
    region: REGION,
    cors: false,
    timeoutSeconds: 60,
    memory: "512MiB",
    maxInstances: 10,
    secrets: [NVIDIA_API_KEY]
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
      const mode = cleanString(req.body && req.body.mode, "AskNoor");
      if (!prompt) {
        res.status(400).json({error: "Prompt is required."});
        return;
      }

      // Optional Auth uid is available for future per-user rate limits / logging.
      await optionalAuthUid(req);

      // Optional conversation history so Noor AI remembers the chat (last 8 turns max).
      const rawHistory = Array.isArray(req.body && req.body.history) ? req.body.history : [];
      const history = rawHistory
        .filter((m) => m && (m.role === "user" || m.role === "assistant") && typeof m.content === "string")
        .slice(-8)
        .map((m) => ({role: m.role, content: m.content.slice(0, MAX_PROMPT_CHARS)}));

      const apiKey = NVIDIA_API_KEY.value();
      if (!apiKey) {
        res.status(500).json({error: "Noor AI key is not configured."});
        return;
      }

      const temperature = mode === "DuaGenerator" ? 0.65 : 0.35;
      const nvResponse = await fetch(NVIDIA_BASE_URL, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${apiKey}`,
          "Content-Type": "application/json",
          "Accept": "application/json"
        },
        body: JSON.stringify({
          model: MODEL,
          messages: [
            {role: "system", content: buildSystemPrompt(mode)},
            ...history,
            {role: "user", content: prompt}
          ],
          temperature,
          top_p: 0.9,
          max_tokens: 1024,
          stream: false
        })
      });

      if (!nvResponse.ok) {
        const detail = await nvResponse.text().catch(() => "");
        console.error("NVIDIA API error", nvResponse.status, detail.slice(0, 500));
        res.status(502).json({
          error: "Noor AI is temporarily unavailable.",
          detail: `Upstream status ${nvResponse.status}`
        });
        return;
      }

      const data = await nvResponse.json();
      const text =
        data &&
        Array.isArray(data.choices) &&
        data.choices[0] &&
        data.choices[0].message &&
        typeof data.choices[0].message.content === "string"
          ? data.choices[0].message.content.trim()
          : "";

      res.json({
        answer: text || "Noor AI could not generate an answer right now. Please try again.",
        provider: "nvidia",
        model: MODEL
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
  "video/x-matroska": "mkv"
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

// Validate and promote a submission into the public feed. Both the create trigger and the
// authenticated recovery endpoint use this function, so tester uploads created before the trigger
// was deployed can still be promoted. The operation is idempotent because the public post keeps the
// submission ID.
async function promoteUmmahSubmission(submission, submissionId) {
  if (!submission || !submission.exists) return {published: false, status: "missing"};

  const data = submission.data() || {};
  if (["rejected", "removed"].includes(data.status)) {
    return {published: false, status: data.status};
  }

  const type = cleanString(data.type, "text").toLowerCase();
  const allowedTypes = new Set(["text", "image", "reel", "video"]);
  const creatorUid = cleanString(data.creatorUid, "").slice(0, 128);
  const caption = cleanString(data.caption, "").slice(0, 2000);
  const arabicText = cleanString(data.arabicText, "").slice(0, 2000);
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
    publishedAt: admin.firestore.FieldValue.serverTimestamp()
  };

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

// New uploads promote automatically.
exports.publishUmmahSubmission = onDocumentCreated(
  {
    document: "ummah_submissions/{submissionId}",
    region: REGION,
    timeoutSeconds: 60,
    memory: "256MiB"
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
