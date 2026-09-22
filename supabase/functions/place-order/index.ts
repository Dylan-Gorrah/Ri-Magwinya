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

import { caller } from "../_shared/auth.ts";
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

  // 200 either way: a replayed client_ref returns the order it already made.
  return json(200, { ...order, order_items: items ?? [] });
}));
