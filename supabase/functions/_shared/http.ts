// Shared request and response plumbing for every Edge Function.
//
// Every error leaves as { code, detail } — the shape the app's
// ErrorMappingInterceptor reads — so a screen matches on a code and never on
// an English sentence.

export type Json = Record<string, unknown>;

export function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

export function fail(status: number, code: string, detail?: string | null): Response {
  return json(status, { code, detail: detail ?? null });
}

/** Thrown by validators; turned into a 400 by `handle`. */
export class BadRequest extends Error {
  constructor(readonly detail: string) {
    super(detail);
  }
}

/**
 * The exceptions the SQL functions raise, and the HTTP status each one
 * becomes. The SQL raises the code as the message and any specifics
 * (which item, which slot, which reason) as the detail.
 */
const STATUS: Record<string, number> = {
  NOT_AUTHENTICATED: 401,
  FORBIDDEN: 403,

  STUDENT_NOT_FOUND: 404,
  ORDER_NOT_FOUND: 404,
  SLOT_NOT_FOUND: 404,
  ITEM_NOT_FOUND: 404,
  NO_PROFILE: 404,

  AMOUNT_OUT_OF_RANGE: 400,
  EMPTY_ORDER: 400,
  INVALID_BASE_QTY: 400,
  OPTION_REQUIRED: 400,

  OUT_OF_STOCK: 409,
  SLOT_FULL: 409,
  SLOT_NOT_TODAY: 409,
  INSUFFICIENT_FUNDS: 409,
  COUNTER_BLOCKED: 409,
  INVALID_TRANSITION: 409,
  EMPTY_SELECTION: 409,
  STUDENT_NUMBER_REQUIRED: 409,

  CODE_SPACE_EXHAUSTED: 503,
};

/** A Postgres error from supabase-js, as a response. */
export function fromDatabase(error: { message?: string; details?: string | null; code?: string }): Response {
  const code = (error.message ?? "").trim();
  const status = STATUS[code];
  if (status) return fail(status, code, error.details);

  // 22P02: a malformed uuid or enum value got past validation.
  if (error.code === "22P02") return fail(400, "BAD_REQUEST", "Malformed value");

  console.error("unmapped database error", error.code, code);
  return fail(500, "SERVER_ERROR");
}

/**
 * Wraps a handler: checks the method, parses the body, and turns anything
 * thrown into a clean response rather than a stack trace.
 */
export function handle(method: string, fn: (body: Json, req: Request) => Promise<Response>) {
  return async (req: Request): Promise<Response> => {
    if (req.method !== method) return fail(405, "METHOD_NOT_ALLOWED");

    let body: Json;
    try {
      const parsed = await req.json();
      if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
        return fail(400, "BAD_REQUEST", "Body must be a JSON object");
      }
      body = parsed as Json;
    } catch {
      return fail(400, "BAD_REQUEST", "Body is not valid JSON");
    }

    try {
      return await fn(body, req);
    } catch (e) {
      if (e instanceof BadRequest) return fail(400, "BAD_REQUEST", e.detail);
      console.error("unhandled", e instanceof Error ? e.message : e);
      return fail(500, "SERVER_ERROR");
    }
  };
}

// --- Validators. Each throws BadRequest naming the field. -----------------

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function uuid(value: unknown, field: string): string {
  if (typeof value !== "string" || !UUID.test(value)) throw new BadRequest(`${field} must be a uuid`);
  return value.toLowerCase();
}

export function oneOf<T extends string>(value: unknown, allowed: readonly T[], field: string): T {
  if (typeof value !== "string" || !allowed.includes(value as T)) {
    throw new BadRequest(`${field} must be one of ${allowed.join(", ")}`);
  }
  return value as T;
}

export function int(value: unknown, field: string, min: number, max: number): number {
  if (typeof value !== "number" || !Number.isInteger(value) || value < min || value > max) {
    throw new BadRequest(`${field} must be a whole number from ${min} to ${max}`);
  }
  return value;
}

export function optionalInt(value: unknown, field: string, min: number, max: number): number | undefined {
  return value === undefined || value === null ? undefined : int(value, field, min, max);
}
