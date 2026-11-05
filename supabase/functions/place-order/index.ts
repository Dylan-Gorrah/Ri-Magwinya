// POST /functions/v1/place-order
//
// Body:
//   { slot_id, payment_method: "wallet" | "counter", client_ref,
//     lines: [{ item_id, base_qty?, quantity?, options: [{ option_id, count? }] }] }
//
// A thin wrapper. The price, the stock check, the slot check, the wallet
// and the counter rules all live in place_order() in SQL, in one
// transaction. This checks the caller and the shape of the body, and turns
// the SQL's exceptions into status codes.
//
// 409: OUT_OF_STOCK, SLOT_FULL, INSUFFICIENT_FUNDS, COUNTER_BLOCKED,
//      EMPTY_SELECTION, SLOT_NOT_TODAY, STUDENT_NUMBER_REQUIRED

import { admin, caller } from "../_shared/auth.ts";
import { push } from "../_shared/fcm.ts";
import { BadRequest, fail, fromDatabase, handle, int, json, Json, oneOf, optionalInt, uuid } from "../_shared/http.ts";

const MAX_LINES = 30;
const MAX_COUNT = 50;

function lines(value: unknown) {
  if (!Array.isArray(value) || value.length === 0) throw new BadRequest("lines must be a non-empty array");
  if (value.length > MAX_LINES) throw new BadRequest(`at most ${MAX_LINES} lines`);

  return value.map((raw, i) => {
    if (typeof raw !== "object" || raw === null) throw new BadRequest(`lines[${i}] must be an object`);
    const line = raw as Json;
    const options = line.options ?? [];
    if (!Array.isArray(options)) throw new BadRequest(`lines[${i}].options must be an array`);

    return {
      item_id: uuid(line.item_id, `lines[${i}].item_id`),
      base_qty: optionalInt(line.base_qty, `lines[${i}].base_qty`, 0, MAX_COUNT),
      quantity: optionalInt(line.quantity, `lines[${i}].quantity`, 1, MAX_COUNT),
      options: options.map((o, j) => {
        if (typeof o !== "object" || o === null) throw new BadRequest(`lines[${i}].options[${j}] must be an object`);
        const opt = o as Json;
        return {
          option_id: uuid(opt.option_id, `lines[${i}].options[${j}].option_id`),
          count: opt.count === undefined ? 1 : int(opt.count, `lines[${i}].options[${j}].count`, 0, MAX_COUNT),
        };
      }),
    };
  });
}

Deno.serve(handle("POST", async (body, req) => {
  const who = await caller(req);
  if (!who) return fail(401, "NOT_AUTHENTICATED");

  const params = {
    p_slot_id: uuid(body.slot_id, "slot_id"),
    p_payment_method: oneOf(body.payment_method, ["wallet", "counter"] as const, "payment_method"),
    p_client_ref: uuid(body.client_ref, "client_ref"),
    p_lines: lines(body.lines),
  };

  const { data: order, error } = await who.db.rpc("place_order", params);
  if (error) return fromDatabase(error);

  const { data: items } = await who.db
    .from("order_items")
    .select("id, item_id, item_name, options_label, quantity, unit_price, units_consumed, subtotal")
    .eq("order_id", order.id);

  await warnLowStock(items ?? []);

  // 200 either way: a replayed client_ref returns the order it already made.
  return json(200, { ...order, order_items: items ?? [] });
}));

type Line = { item_id: string | null; units_consumed: number };

/**
 * Tells every staff member when this order took an item down to its reorder
 * level. Only on the order that crosses the line, so staff get one alert per
 * item rather than one per sale after it. Never fails the order: the order
 * is already placed.
 */
async function warnLowStock(lines: Line[]) {
  try {
    const used = new Map<string, number>();
    for (const l of lines) {
      if (l.item_id && l.units_consumed > 0) used.set(l.item_id, (used.get(l.item_id) ?? 0) + l.units_consumed);
    }
    if (used.size === 0) return;

    const db = admin();
    const { data: menu } = await db
      .from("menu_items")
      .select("id, name, stock_quantity, reorder_level")
      .in("id", [...used.keys()]);
    const crossed = (menu ?? []).filter((m) =>
      m.stock_quantity <= m.reorder_level &&
      m.stock_quantity + (used.get(m.id) ?? 0) > m.reorder_level
    );
    if (crossed.length === 0) return;

    const { data: staff } = await db.from("profiles").select("id").eq("role", "staff");
    for (const item of crossed) {
      const title = item.stock_quantity === 0 ? `${item.name} is sold out` : `${item.name} is running low`;
      const message = `${item.stock_quantity} left. Restock from the Stock tab.`;
      for (const s of staff ?? []) {
        // The in-app notifications sheet reads this row; the push is extra.
        await db.from("notifications").insert({ user_id: s.id, title, message, type: "low_stock" });
        push(s.id, title, message, { type: "low_stock" });
      }
    }
  } catch (e) {
    console.error("low stock warning failed", e instanceof Error ? e.message : e);
  }
}
