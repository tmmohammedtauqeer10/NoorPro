// Verifies Firebase Auth ID tokens and Firebase App Check tokens (RS256, JWKS cached per isolate).
export const PROJECT_ID = "noor-pro-d87e3";
export const PROJECT_NUMBER = "684547719535";
const SKEW_S = 300;
const SOURCES = {
  id: { jwks: "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com" },
  appcheck: { jwks: "https://firebaseappcheck.googleapis.com/v1/jwks" },
};
const caches = { id: { keys: null, exp: 0 }, appcheck: { keys: null, exp: 0 } };

function b64urlToBytes(s) {
  s = s.replace(/-/g, "+").replace(/_/g, "/");
  while (s.length % 4) s += "=";
  const bin = atob(s);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}
const b64urlJson = (s) => JSON.parse(new TextDecoder().decode(b64urlToBytes(s)));

async function getJwks(kind, nowMs, force = false) {
  const c = caches[kind];
  if (!force && c.keys && nowMs < c.exp) return c.keys;
  const r = await fetch(SOURCES[kind].jwks);
  if (!r.ok) throw new Error("jwks");
  const body = await r.json();
  const m = /max-age=(\d+)/.exec(r.headers.get("cache-control") || "");
  caches[kind] = { keys: body.keys || [], exp: nowMs + (m ? Number(m[1]) : 3600) * 1000 };
  return caches[kind].keys;
}

async function verifyJwt(kind, token, nowMs) {
  const parts = String(token || "").split(".");
  if (parts.length !== 3) return null;
  let header, payload;
  try { header = b64urlJson(parts[0]); payload = b64urlJson(parts[1]); } catch { return null; }
  if (header.alg !== "RS256" || !header.kid) return null;
  let keys = await getJwks(kind, nowMs);
  let jwk = keys.find((k) => k.kid === header.kid);
  if (!jwk) { keys = await getJwks(kind, nowMs, true); jwk = keys.find((k) => k.kid === header.kid); }
  if (!jwk) return null;
  const key = await crypto.subtle.importKey("jwk", { kty: "RSA", n: jwk.n, e: jwk.e, alg: "RS256", ext: true },
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
  const ok = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", key, b64urlToBytes(parts[2]), new TextEncoder().encode(`${parts[0]}.${parts[1]}`));
  if (!ok) return null;
  const now = Math.floor(nowMs / 1000);
  if (typeof payload.exp !== "number" || payload.exp + SKEW_S < now) return null;
  if (typeof payload.iat === "number" && payload.iat - SKEW_S > now) return null;
  return payload;
}

export async function verifyFirebaseIdToken(token, nowMs = Date.now()) {
  const p = await verifyJwt("id", token, nowMs);
  if (!p) return null;
  if (p.aud !== PROJECT_ID || p.iss !== `https://securetoken.google.com/${PROJECT_ID}`) return null;
  if (typeof p.sub !== "string" || !p.sub) return null;
  return p;
}

export async function verifyAppCheckToken(token, nowMs = Date.now()) {
  const p = await verifyJwt("appcheck", token, nowMs);
  if (!p) return null;
  if (p.iss !== `https://firebaseappcheck.googleapis.com/${PROJECT_NUMBER}`) return null;
  const aud = Array.isArray(p.aud) ? p.aud : [p.aud];
  if (!aud.includes(`projects/${PROJECT_NUMBER}`) && !aud.includes(`projects/${PROJECT_ID}`)) return null;
  if (typeof p.sub !== "string" || !p.sub) return null; // sub = Firebase app id
  return p;
}

/** Returns {tier: "user"|"app"|"anon", key}. Never throws; tokens are never logged. */
export async function identify(request, nowMs = Date.now()) {
  const ip = request.headers.get("cf-connecting-ip") || "0.0.0.0";
  const m = /^Bearer\s+(.+)$/i.exec(request.headers.get("authorization") || "");
  if (m) {
    try { const c = await verifyFirebaseIdToken(m[1].trim(), nowMs); if (c) return { tier: "user", key: `u:${c.sub}` }; } catch {}
  }
  const ac = request.headers.get("x-firebase-appcheck");
  if (ac) {
    try { const c = await verifyAppCheckToken(ac.trim(), nowMs); if (c) return { tier: "app", key: `ip:${ip}` }; } catch {}
  }
  return { tier: "anon", key: `ip:${ip}` };
}
