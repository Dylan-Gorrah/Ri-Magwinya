# The campus wallet

Back to [[Ri-magwinya]]

A balance on the student's account that they spend on food. It is the main way
of paying, and it's the part of the app holding real money, so it gets treated
carefully.

## How money gets in

Only one way: **a student stands at the tuckshop and pays.**

They tap their bank card on the tuckshop's speed point. That machine belongs
to the bank and has nothing to do with this app. Once it approves, staff open
the top-up screen and load that amount onto the student's account.

The app **never sees a card number**, never talks to a bank, never processes a
payment. It records that money was received. See [[Wallet top-up]].

This is worth being blunt about in the UI copy too, because "add money" in
most apps means a card form, and here it doesn't.

## How money goes out

Placing a wallet order takes the total out at the moment the order is placed —
not on collection. The order exists because it's been paid for.

The only way money comes back is cancelling a `placed` order, which refunds in
full. See [[Order statuses]].

## Where it lives

Two things, which must always agree:

**The balance**, on the profile:

```sql
profiles
  wallet_balance  numeric(10,2)  not null  default 0  check (>= 0)
```

**The ledger**, one row per movement:

```sql
wallet_transactions
  id        uuid
  user_id   uuid
  amount    numeric(10,2)   -- positive in, negative out
  type      text            -- 'topup' | 'payment' | 'refund'
  order_id  uuid            -- for payments and refunds
  staff_id  uuid            -- who loaded it, for top-ups
  created_at timestamptz
```

The balance is a running total for speed. The ledger is the truth. Add up
every `amount` for a user and you should get their balance exactly — and if
you ever don't, the ledger is right and the balance is the bug.

That's why `check (>= 0)` is on the column. A negative balance isn't a state
the app should ever reach, so the database refuses to store one at all.

## Nothing writes the balance directly

Not the app, not a PATCH, not staff. The column is closed off by the security
rules — students can update `full_name`, `language` and `fcm_token` on their
own profile and nothing else. See [[Security rules]].

Every change to the balance happens inside a server function
(`place_order`, `set_order_status`, `load_wallet`), which writes the ledger
row and moves the balance in the same transaction. There is no path where one
happens without the other.

If the app could set its own balance, the app could give itself money.

## Paying for an order

Inside `place_order`, after the prices are recalculated:

1. Lock the profile row
2. Compare `wallet_balance` against the server's total
3. Short → `409 INSUFFICIENT_FUNDS`, whole transaction rolls back
4. Enough → subtract it, write a `payment` row for the negative amount

Same transaction as the stock and the slot, so there is no moment where money
has left and no order exists.

## When there isn't enough

The checkout screen says the exact shortfall — *"You're R12.00 short. Top up
at the tuckshop, or pay at the counter."* — rather than just disabling the
button.

A student who can't afford the order should be told how much they need and
where to get it, not left guessing. The alternative is [[Pay at the counter]].

## Where it's shown

- **The menu hero**, next to the greeting, so the balance is visible the whole
  time they're choosing.
- **The checkout screen**, with the balance before and after.
- **The profile**, with the full transaction history.

Always formatted through the shared `Money` helper — `R12.50`, tabular
figures so columns of prices line up. And in the code it's the integer `1250`,
never the decimal `12.5`. See [[The rules that matter]].
