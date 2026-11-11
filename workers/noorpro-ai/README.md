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
Groq free tier first **only if** the secret `GROQ_API_KEY` is set (`npx wrangler secret put GROQ_API_KEY`, value via stdin),
then Workers AI: `@cf/google/gemma-4-26b-a4b-it` (thinking off, ~22 neurons/answer), `@cf/openai/gpt-oss-120b`.
Creator tasks may also use `gpt-oss-20b`. Subtitles use `whisper-large-v3-turbo`; safety check adds `llama-guard-3-8b`.
Free allowance: 10,000 neurons/day (UTC), roughly 400 chat answers/day.

## Abuse protection (free features only)
- Identity: Firebase ID token (`Authorization: Bearer`) or Firebase App Check token (`X-Firebase-AppCheck`); anything else is "anon" by IP.
- Burst: Workers Rate Limiting bindings, 12/min per user, 5/min per anonymous IP.
- Daily quota: SQLite Durable Object counter, 150/day per user, 80/day per App Check IP, 30/day per anonymous IP (transcribe costs 3).
- Size caps: 64 KB JSON (chat/creator), ~6 MB base64 audio. Tokens and prompts are never logged.

## Deploy / test
`npm i && node test.mjs && npx wrangler deploy` (needs `CLOUDFLARE_API_TOKEN` in the environment; never commit it).
