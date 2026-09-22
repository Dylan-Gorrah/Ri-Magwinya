// Push notifications through FCM HTTP v1.
//
// Does nothing until Phase 11 sets two secrets:
//   FCM_SERVICE_ACCOUNT  the whole service-account JSON from Firebase
//   FCM_PROJECT_ID       the Firebase project id
//
// A push is a courtesy on top of the in-app notification row the SQL has
// already written. So nothing here ever throws, and a request never fails or
// waits because a push could not be sent.

import { admin } from "./auth.ts";

type ServiceAccount = { client_email: string; private_key: string };

let cachedToken: { value: string; expires: number } | null = null;

function config(): { account: ServiceAccount; projectId: string } | null {
  const raw = Deno.env.get("FCM_SERVICE_ACCOUNT");
  const projectId = Deno.env.get("FCM_PROJECT_ID");
  if (!raw || !projectId) return null;
  try {
    return { account: JSON.parse(raw), projectId };
  } catch {
    console.error("FCM_SERVICE_ACCOUNT is not valid JSON");
    return null;
  }
}

function b64url(data: Uint8Array | string): string {
  const bytes = typeof data === "string" ? new TextEncoder().encode(data) : data;
  let s = "";
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/** An OAuth access token for FCM, from a JWT signed with the service account. */
async function accessToken(account: ServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cachedToken && cachedToken.expires > now + 60) return cachedToken.value;

  const header = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = b64url(JSON.stringify({
    iss: account.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));

  const pem = account.private_key.replace(/-----[^-]+-----/g, "").replace(/\s/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    "pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"],
  );
  const signature = new Uint8Array(
    await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${claims}`)),
  );

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${header}.${claims}.${b64url(signature)}`,
    }),
  });
  if (!res.ok) throw new Error(`token exchange ${res.status}`);
  const body = await res.json();
  cachedToken = { value: body.access_token, expires: now + (body.expires_in ?? 3600) };
  return cachedToken.value;
}

async function send(userId: string, title: string, body: string, data: Record<string, string>) {
  const cfg = config();
  if (!cfg) return; // Phase 11 not set up yet.

  try {
    const { data: profile } = await admin()
      .from("profiles").select("fcm_token").eq("id", userId).maybeSingle();
    const token = profile?.fcm_token;
    if (!token) return;

    const res = await fetch(
      `https://fcm.googleapis.com/v1/projects/${cfg.projectId}/messages:send`,
      {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${await accessToken(cfg.account)}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token,
            notification: { title, body },
            data,
            android: { priority: "high" },
          },
        }),
      },
    );
    if (!res.ok) console.error("fcm send failed", res.status);
  } catch (e) {
    console.error("fcm error", e instanceof Error ? e.message : e);
  }
}

/**
 * Sends a push in the background. The response goes back to the phone
 * straight away; the push finishes after it.
 */
export function push(userId: string, title: string, body: string, data: Record<string, string> = {}) {
  const task = send(userId, title, body, data);
  // deno-lint-ignore no-explicit-any
  const runtime = (globalThis as any).EdgeRuntime;
  if (runtime?.waitUntil) runtime.waitUntil(task);
}
