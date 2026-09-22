// PATCH /functions/v1/order-status
//
// Body: { order_id, status }
//
// set_order_status() in SQL owns the transition table and who may make each
// move. This checks the caller and the body, and pushes to the student when
// their order is ready.
//
// 403: wrong role, or someone else's order
// 409: INVALID_TRANSITION

import { caller } from "../_shared/auth.ts";
import { push } from "../_shared/fcm.ts";
import { fail, fromDatabase, handle, json, oneOf, uuid } from "../_shared/http.ts";

const STATUSES = ["preparing", "ready", "collected", "cancelled", "no_show"] as const;

Deno.serve(handle("PATCH", async (body, req) => {
  const who = await caller(req);
  if (!who) return fail(401, "NOT_AUTHENTICATED");

  const { data: order, error } = await who.db.rpc("set_order_status", {
    p_order_id: uuid(body.order_id, "order_id"),
    p_status: oneOf(body.status, STATUSES, "status"),
  });
  if (error) return fromDatabase(error);

  if (order.status === "ready") {
    push(
      order.student_id,
      `Order #${order.order_number} is ready`,
      `Collect at the tuckshop. Your code is ${order.collection_code}.`,
      { type: "order_ready", order_id: order.id },
    );
  }

  return json(200, order);
}));
