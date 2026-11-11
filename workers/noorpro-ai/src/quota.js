// Daily quota counter (SQLite-backed Durable Object, free plan) + per-isolate fallback.
export const DAILY_LIMITS = { user: 150, app: 80, anon: 30 };
export const COST = { chat: 1, creator: 1, transcribe: 3 };

export class Quota {
  constructor(ctx) { this.ctx = ctx; }
  async fetch(request) {
    const { limit, cost } = await request.json();
    const used = (await this.ctx.storage.get("n")) || 0;
    if (used + cost > limit) return Response.json({ ok: false, used, limit });
    await this.ctx.storage.put("n", used + cost);
    if (!used) await this.ctx.storage.setAlarm(Date.now() + 26 * 3600_000);
    return Response.json({ ok: true, used: used + cost, limit });
  }
  async alarm() { await this.ctx.storage.deleteAll(); }
}

const local = new Map();
export function localCount(key, cost, limit, day) {
  const k = `${day}|${key}`;
  const n = (local.get(k) || 0) + cost;
  if (local.size > 5000) local.clear();
  if (n > limit) return false;
  local.set(k, n);
  return true;
}

export async function takeQuota(env, ident, kind, now = Date.now()) {
  const limit = DAILY_LIMITS[ident.tier] ?? 30;
  const cost = COST[kind] ?? 1;
  const day = new Date(now).toISOString().slice(0, 10);
  if (!env.QUOTA) return localCount(ident.key, cost, limit, day);
  try {
    const stub = env.QUOTA.get(env.QUOTA.idFromName(`${day}|${ident.key}`));
    const r = await stub.fetch("https://quota/take", { method: "POST", body: JSON.stringify({ limit, cost }) });
    const j = await r.json();
    return !!j.ok;
  } catch { return localCount(ident.key, cost, limit, day); }
}

export async function burstOk(env, ident) {
  const rl = ident.tier === "anon" ? env.RL_ANON : env.RL_USER;
  if (!rl) return true;
  try { const { success } = await rl.limit({ key: ident.key }); return success; } catch { return true; }
}
