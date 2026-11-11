# noorpro-ai (Cloudflare Worker)

Free backend for Noor AI chat, Hifz Coach AI and creator tools. Live at `https://noorpro-ai.noorpro.workers.dev`
(Cloudflare account Noorpro.official@gmail.com, Workers Free plan). The app reads Remote Config `noor_ai_url`
and falls back to that URL; the old `askNoorAi` Cloud Function stays as the second fallback.

## Endpoints (POST JSON)
| Path | Body | Returns |
|---|---|---|
| `/v1/chat` | `prompt, mode, language ("auto" or code), history[], passages[{ref,text}], context{surah,ayahRef,arabic,translation}` | `answer, language, fiqh, followups[], citations[], provider, model` |
| `/v1/creator` | `task (captions/hooks/hashtags/translate/subtitles/moderate), text, topic, lang, kind` | `items[]` / `text` / `lines[]` / `verdict, issues[], suggestion` |
| `/v1/transcribe` | `audio` (base64 AAC/M4A, <= ~4.4 MB), `lang?` | `text, language, segments[], vtt` |
| `GET /health` | | `{ok, groq, workersAi}` |

## Models
Each provider is used only if its secret/binding exists (`GEMINI_API_KEY`, `GROQ_API_KEY`, `ZHIPU_API_KEY` as Worker
secrets set via stdin; `AI` binding). Routing (`buildOrder` in `src/llm.js`, live-evaluated 2026-11-11):

| Request | Order |
|---|---|
| Chat, English / Roman Urdu / Hinglish | Gemini `gemini-3.5-flash-lite` → Groq `qwen/qwen3.8-27b` → Groq `openai/gpt-oss-120b` → Gemini `gemini-3.8-flash` → Workers AI Gemma 4 → Workers AI gpt-oss-120b → Z.ai `glm-4.7-flash` → `glm-4.5-flash` |
| Chat, Urdu / Arabic / Hindi / Bengali / … | **race** Gemini `gemini-3.8-flash` vs Workers AI Gemma 4 (Gemini preferred if it lands within 4 s of Gemma) → Gemini Flash-Lite → Groq qwen → Workers AI gpt-oss-120b |
| Creator (short tasks) | Gemini Flash-Lite first, then the chat chain for the language |

Why: gemini-3.8-flash gave the most complete Hanafi-correct Urdu Hajj answer (3 fara'id; sa'i, Muzdalifa, rami,
qurbani, halq, tawaf al-wada as wajibat; Arafat from 9 Dhul Hijjah) but its latency swings 7–50 s; Gemma 4 was also
correct (12–20 s). Flash-Lite is fast (2–6 s) and accurate in English/Arabic/Hindi but put sa'i under arkan in Urdu.
Groq qwen made Hajj errors in Urdu; gpt-oss-120b and GLM-4.7-Flash invented Hajj facts in Urdu, so they are Latin-only.
Only Z.ai's free Flash models are allowed (`ZHIPU_FREE_MODELS`); paid GLM models are never called.
Free-tier limits: any 429 is skipped and that model is cooled down (Gemini/Z.ai 60 s, Groq 30 s) per isolate.
Groq `max_tokens` is clamped to 950. A non-Latin answer with Latin letters glued into words is rejected and retried.
Subtitles use `whisper-large-v3-turbo`; the safety check adds `llama-guard-3-8b`.
Workers AI free allowance: 10,000 neurons/day (UTC), roughly 400 Gemma answers/day.

## 1.2.0 (114) additions
- **Madhhab**: `madhhab` = `hanafi` (default) | `shafii` | `maliki` | `hanbali` | `compare`. Fiqh follows the chosen
  school's relied-upon view, noting other schools when relevant; `compare` lists all four. Aqidah is always
  Ash'ari/Maturidi, Ahl as-Sunnah wal-Jama'ah with Tasawwuf.
- **Knowledge base** (`kb/*.json` -> `node kb/build.mjs` -> `src/kb_data.js`): 39 original summaries with classical
  citations (Tahawiyya, Fiqh al-Akbar, Nasafiyya, Bahar-e-Shariat / Fatawa Ridawiyya, Nur al-Idah, Umdat al-Salik,
  al-Risala, Umdat al-Fiqh, Ihya, Qushayriyya; Qur'an and hadith numbers). Retrieval: gemini-embedding-001 (256-d)
  cosine + keyword boost, kept only when the top score is >= 0.75 and within a small spread; keyword-only fallback.
- **Tiers**: `tier` = `fast` (Flash-Lite first, short) | `balanced` | `research`.
  `/v1/research` (or `/v1/chat` with `tier: research`) plans queries, searches the KB, then writes a cited answer with
  the strongest model; with `Accept: text/event-stream` it streams `data: {"type":"stage"|"answer"|"error",...}`.
  Research costs 4 quota units.
- **Voice**: `/v1/transcribe` = Groq whisper-large-v3 (language detection; `hint` resolves Hindi/Urdu) then Workers AI
  Whisper. `/v1/tts` = Gemini TTS (natural voices for Urdu, Hindi, Arabic, English, ...) then MeloTTS (en/fr/es/zh/ja/ko);
  otherwise `503 {fallback:"device"}` and the app uses Android TTS. Own daily counter (150/user).
- **Images**: `/v1/image` = flux-1-schnell. Keyword block (people, faces, prophets, Sahabah, Ahl al-Bayt, awliya, angels,
  animals, immodest or non-Islamic content; en/ur/hi/ar) -> LLM classifier -> generate -> Gemini vision check (fails
  closed). Localized refusals. Limits: 6/day per user, 3 App Check, 2 anon, 150/day global.

## Abuse protection (free features only)
- Identity: Firebase ID token (`Authorization: Bearer`) or Firebase App Check token (`X-Firebase-AppCheck`); anything else is "anon" by IP.
- Burst: Workers Rate Limiting bindings, 12/min per user, 5/min per anonymous IP.
- Daily quota: SQLite Durable Object counter, 150/day per user, 80/day per App Check IP, 30/day per anonymous IP (transcribe costs 3).
- Size caps: 64 KB JSON (chat/creator), ~6 MB base64 audio. Tokens and prompts are never logged.

## Deploy / test
`npm i && node test.mjs && npx wrangler deploy` (needs `CLOUDFLARE_API_TOKEN` in the environment; never commit it).
