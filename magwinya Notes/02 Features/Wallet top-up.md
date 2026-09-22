# Wallet top-up

Back to [[Ri-magwinya]]

A staff-only screen for putting money on a student's account. New in the real
app — the prototype doesn't have it at all.

## The real-world flow

1. Student comes to the counter and says they want R50 on their account
2. They tap their card on the **speed point** — the bank's machine, separate
   from this app entirely
3. The machine approves
4. **Only then** staff open the top-up screen and load R50

Step 4 is bookkeeping. The money already moved in step 3.

This ordering matters. If staff load first and the card declines, the student
has credit they never paid for. The screen's copy says so plainly: *Take
payment on the speed point first, then load the amount here.*

## The screen

Reached from a **Top up** button in the queue's top bar, because that's where
staff already are.

**Find the student.** Type a student number, the app looks it up, and shows
the name and current balance so staff can confirm it's the right person before
any money goes on.

Not found → `404 STUDENT_NOT_FOUND` → *"No student with that number. Check
it and try again."*

**Pick an amount.** Presets R20, R50, R100, R200, plus a custom field.
Anything from **R10 to R1000**.

The floor stops fat-fingered R1 top-ups. The ceiling is a typo guard — R2000
instead of R200 is one slipped finger, and this is the only screen in the app
that creates money out of nothing.

**Confirm.** A summary — student, amount, balance before and after — then the
call. The new balance shows immediately.

## The call

```http
POST /functions/v1/load-wallet
{ "student_number": "ST2024001", "amount": 50.00 }
```

| Response | Why |
|---|---|
| `403` | Caller isn't staff |
| `404 STUDENT_NOT_FOUND` | No profile with that number |
| `400 AMOUNT_OUT_OF_RANGE` | Outside R10–R1000 |
| `200` | Done, new balance returned |

Behind it, `load_wallet(user_id, amount)` does two things in one transaction:
writes a `wallet_transactions` row with `type = 'topup'` and `staff_id` set to
whoever did it, and adds the amount to `wallet_balance`.

Both or neither. Never a balance change without a ledger row explaining it.
See [[The campus wallet]].

## Who did it

Every top-up records the staff member. It's the one operation in the app that
creates value, so it should never be anonymous — if a balance looks wrong,
the ledger says who put it there and when.

## What it also unlocks

A student's first top-up turns on [[Pay at the counter]] for them, because it
means a staff member has physically seen them. Not something staff need to
think about — it just happens.

## Range and staff-only, twice

Both rules are checked in the Edge Function *and* in the SQL. The function's
check gives a good error message; the SQL's check is what actually protects
the data if anything ever reaches the database another way.

The app's own validation is a third layer, and the least important one — it's
there to make the screen pleasant, not to make it safe. See [[Security rules]].
