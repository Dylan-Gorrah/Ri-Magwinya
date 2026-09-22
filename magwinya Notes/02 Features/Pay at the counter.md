# Pay at the counter

Back to [[Ri-magwinya]]

The fallback. Order now, pay when you collect. It exists so an empty wallet
doesn't mean no lunch — but it's the one place the app extends credit, so it
has conditions on it.

## How it works for the student

Pick **Pay at the counter** at checkout instead of the wallet. The order goes
through with `payment_method = 'counter'`, no money moves, and the student
pays at the speed point when they collect.

Stock still drops. The slot is still booked. The only difference is when the
money arrives.

## Why it's restricted

An order placed this way costs the tuckshop something before anyone has paid
anything. Ingredients get used, a collection spot is taken, and the food gets
made. If the student doesn't come, that's a real loss.

Two conditions guard it.

### 1. You must have topped up at least once

Pay-at-counter is only offered to accounts with at least one successful
top-up in `wallet_transactions`.

This is the more important of the two, and it's here because
**sign-up is open** — any email works, and nothing verifies a typed student
number. Without this rule, anyone could make a throwaway account and book
stock they never intend to collect, and `no_show_count` would only stop them
after two goes, on an account that costs nothing to replace.

A top-up means staff have physically seen this person at the counter with real
money. That's not a check any sign-up form can do, and it's already part of
using the app normally.

Wallet payment stays open to everyone, because wallet money has already been
handed over.

See section 12.1 of `CLAUDE.md` and [[Open decisions]].

### 2. Two no-shows and it's gone

`profiles.no_show_count` goes up each time staff mark a ready order as a
no-show. At **2**, counter payment is refused.

It isn't a ban. The student can still order as much as they like — they just
have to have money in the wallet first. The tuckshop stops making food on
credit for someone who has twice not come for it.

## The check

Inside `place_order`, before anything is written:

```
if payment_method = 'counter':
    if no prior topup in wallet_transactions  → COUNTER_BLOCKED
    if no_show_count >= 2                     → COUNTER_BLOCKED
```

A `409` either way. The app already knows which case it is — it has the
profile — so it shows the right sentence without a second round trip:

| Case | What the student sees |
|---|---|
| Never topped up | "Pay at the counter is available once you've topped up at the tuckshop for the first time." |
| Two no-shows | "Pay at the counter isn't available because two orders weren't collected. You can still order using your wallet." |

Both are explanations, not refusals. A blocked option with no reason next to
it is the thing students would complain about.

## Collecting a counter order

Staff see the payment method on the queue card, so they know to take money
before handing over. Then **Collected** as normal.

If they don't come, the slot ends, the no-show action appears, and the count
goes up. See [[Order statuses]].

## What it isn't

Not an account, not a tab, not a debt. Nothing is owed after a no-show — the
tuckshop absorbs the food and the student loses the option. There's no
chasing anyone for R17.
