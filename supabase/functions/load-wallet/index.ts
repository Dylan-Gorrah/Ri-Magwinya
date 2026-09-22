// POST /functions/v1/load-wallet
//
// Body: { student_number, amount }
//
// Staff record a top-up the student has just paid for on the tuckshop's
// speed point. The speed point is the bank's machine and a separate system;
// no card data ever comes near this app. load_wallet() in SQL checks the
// caller is staff and writes the ledger and the balance together.
//
// 403: not staff
// 404: STUDENT_NOT_FOUND
// 400: AMOUNT_OUT_OF_RANGE

import { caller } from "../_shared/auth.ts";
import { push } from "../_shared/fcm.ts";
import { BadRequest, fail, fromDatabase, handle, json } from "../_shared/http.ts";

const MIN = 10;
const MAX = 1000;

Deno.serve(handle("POST", async (body, req) => {
  const who = await caller(req);
  if (!who) return fail(401, "NOT_AUTHENTICATED");

  const number = typeof body.student_number === "string" ? body.student_number.trim() : "";
  if (number.length === 0 || number.length > 20) throw new BadRequest("student_number is required");

  const amount = body.amount;
  if (typeof amount !== "number" || !Number.isFinite(amount)) throw new BadRequest("amount must be a number");
  // Whole cents only: R12.345 is not an amount anyone paid.
  // (Compared with a tolerance: 12.3 * 100 is 1230.0000000000002 in floating point.)
  if (Math.abs(Math.round(amount * 100) - amount * 100) > 1e-6) throw new BadRequest("amount has more than two decimals");
  if (amount < MIN || amount > MAX) return fail(400, "AMOUNT_OUT_OF_RANGE", `R${MIN} to R${MAX}`);

  const { data: profile, error } = await who.db.rpc("load_wallet", {
    p_student_number: number,
    p_amount: amount,
  });
  if (error) return fromDatabase(error);

  push(
    profile.id,
    "Wallet topped up",
    `R${amount.toFixed(2)} added. Your balance is R${Number(profile.wallet_balance).toFixed(2)}.`,
    { type: "wallet_topup" },
  );

  // Only what the top-up screen shows. Not the email, not the FCM token.
  return json(200, {
    id: profile.id,
    full_name: profile.full_name,
    student_number: profile.student_number,
    wallet_balance: profile.wallet_balance,
  });
}));
