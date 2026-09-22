# The cart and checkout

Back to [[Ri-magwinya]]

Two screens: check what you picked, then commit to it.

## The cart

Every line shows the item name, its full option label underneath, a stepper,
and the line total.

```
Vetkoek                                R17.00
2 vetkoeks, 2 Polony, Cheese slice
                                    [ − 1 + ]

Tea                                    R10.00
Five Roses, With milk, 2 spoons
                                    [ − 1 + ]
```

Stepping a line down to zero removes it. Empty cart gets a proper empty state
— an icon, "Nothing in your cart yet", and a button back to the menu — not a
blank screen.

### Where the cart lives

In memory from Phase 4, backed by Room from Phase 10 so it survives the app
being killed. Either way it's a `CartRepository` behind a `Flow`, so the
screens never know the difference.

### Lines don't merge unless they're identical

A line's identity is the item **plus** the exact selection. Add the same
vetkoek build twice and the quantity goes to 2; add a different build and you
get a second line. See [[Building an order]].

## Checkout

### The summary

The same lines again, read-only this time, with the total at the bottom. The
total is the phone's arithmetic and it's **display only** — the real number
comes back from the server. In practice they always match, because both sides
run the same formula. See [[The server does the maths]].

### The collection card

Pick one of the day's three slots. Each chip shows how full it is:

```
First break     09:40 – 10:00    12 of 40 taken
Second break    11:00 – 11:20    38 of 40 taken
Afternoon       14:30 – 14:50     3 of 25 taken
```

Full slots are disabled. So are slots whose window has already passed. See
[[Collection slots]].

### Payment

Two options.

**Campus wallet**, the default. Shows the current balance and what it'll be
afterwards. If the balance is short, the option is disabled with the exact
shortfall — "R12.00 short. Top up at the tuckshop." — not a vague error. See
[[The campus wallet]].

**Pay at the counter.** Only offered to students who have topped up at least
once, and blocked entirely at two no-shows, each with its own explanation. See
[[Pay at the counter]].

### The cancellation notice

Plain text above the button: you can cancel while the order is still *placed*,
and once the kitchen starts preparing it you can't. Better said here than
discovered later.

## Placing the order

The button calls one endpoint:

```http
POST /functions/v1/place-order
```

```json
{
  "slot_id": "…",
  "payment_method": "wallet",
  "client_ref": "a3f1…",
  "lines": [
    { "item_id": "…", "base_qty": 2,
      "options": [ { "option_id": "…", "count": 2 } ] },
    { "item_id": "…", "quantity": 1,
      "options": [ { "option_id": "…" } ] }
  ]
}
```

### What client_ref is for

A UUID the **phone** generates, once, before the request goes out. It's saved
with the cart, so a retry sends the same one.

`orders` has `unique(student_id, client_ref)`. If the same ref arrives twice —
the network dropped the response, the phone retried, the sync worker replayed
a queued order — the server returns the order it already made instead of
making a second one.

This is the whole reason you can safely retry a payment. Without it, a flaky
connection could charge someone twice for the same vetkoek. It matters most in
[[Offline mode]], where replays are the normal path rather than the unusual
one.

### What comes back

The created order, with its number, its 4-digit code, and its status. The app
goes straight to [[Order statuses]] with it.

### What can go wrong

All 409s, each with a reason code the app turns into a sentence:

| Code | What the student sees |
|---|---|
| `OUT_OF_STOCK` | "Vetkoek just sold out. Take it out of your cart to carry on." |
| `SLOT_FULL` | "Second break filled up. Pick another time." |
| `INSUFFICIENT_FUNDS` | "You're R12.00 short. Top up at the tuckshop, or pay at the counter." |
| `COUNTER_BLOCKED` | "Pay at the counter isn't available on your account." |

Nothing is charged and nothing is created when any of these come back — the
whole thing is one transaction that either completes or doesn't. See
[[The rules that matter]].

The failure that matters most is `OUT_OF_STOCK`, because it's the one that
happens for real: two people in the cart at once, one last vetkoek. One of
them gets it, the other gets a clear message and an untouched wallet.
