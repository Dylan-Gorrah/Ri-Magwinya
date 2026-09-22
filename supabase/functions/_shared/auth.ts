// Who is calling.
//
// This project uses the new publishable/secret keys and asymmetric JWT
// signing. The platform's built-in verify_jwt only understands the legacy
// keys, so the functions are deployed with verify_jwt = false and the user's
// token is verified here instead, against the project's published keys.

import { createClient, SupabaseClient } from "jsr:@supabase/supabase-js@2";

const URL = Deno.env.get("SUPABASE_URL")!;

function namedKey(envName: string, legacyName: string): string {
  const raw = Deno.env.get(envName);
  if (raw) {
    try {
      const key = JSON.parse(raw)["default"];
      if (key) return key;
    } catch { /* fall through to the legacy key */ }
  }
  return Deno.env.get(legacyName)!;
}

const PUBLISHABLE = namedKey("SUPABASE_PUBLISHABLE_KEYS", "SUPABASE_ANON_KEY");

export type Caller = {
  userId: string;
  /** Acts as the caller: RLS applies and auth.uid() is them. */
  db: SupabaseClient;
};

/**
 * The verified caller, or null if the request has no valid user token.
 *
 * Every database call goes through `db`, which carries the user's own
 * token. The SQL functions then see the real auth.uid() and enforce roles
 * themselves — the Edge Function never decides who is staff.
 */
export async function caller(req: Request): Promise<Caller | null> {
  const header = req.headers.get("Authorization") ?? "";
  const token = header.startsWith("Bearer ") ? header.slice(7).trim() : "";
  if (!token || token.startsWith("sb_")) return null;

  const db = createClient(URL, PUBLISHABLE, {
    global: { headers: { Authorization: `Bearer ${token}` } },
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data, error } = await db.auth.getClaims(token);
  const sub = data?.claims?.sub;
  if (error || !sub || data?.claims?.role !== "authenticated") return null;

  return { userId: sub, db };
}

let adminClient: SupabaseClient | null = null;

/**
 * Bypasses RLS. Used only to read a student's FCM token for a push —
 * never for anything a request asked for.
 */
export function admin(): SupabaseClient {
  adminClient ??= createClient(URL, namedKey("SUPABASE_SECRET_KEYS", "SUPABASE_SERVICE_ROLE_KEY"), {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  return adminClient;
}
