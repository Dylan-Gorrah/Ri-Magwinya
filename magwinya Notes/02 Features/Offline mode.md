# Offline mode

Back to [[Ri-magwinya]]

Campus wifi is campus wifi. The app keeps working without it. Phase 10, and
one of the four the PoE marks separately.

## What works with no signal

- The menu, with options, prices and last-known stock
- The student's orders and their statuses
- The cart
- The weather strip, from cache
- Placing an order — it queues and sends itself later
- Staff stock adjustments — same

What doesn't: anything genuinely live. Realtime updates stop, and stock counts
are as fresh as the last time there was signal.

## Network-first, cache-fallback

Every repository does the same thing:

1. Return what's in Room immediately, so the screen has content now
2. Fire the network request
3. Success → write to Room, which pushes the update through the `Flow` and the
   screen refreshes itself
4. Failure → keep showing the cache, show the offline banner

Screens observe Room, not the network. They never know which branch happened,
which is why there's no "loading from cache" state anywhere — there's just
content, sometimes slightly stale.

What's cached: menu items with their option groups and options, the day's
slots, the student's orders, the cart, and the weather.

## The banner

A slim strip under the top bar:

> You're offline. Showing what we last saved.

Driven by a connectivity observer on `ConnectivityManager`. Appears and
disappears on its own. Deliberately small — the app still works, so it's
information, not an error.

## The queue

Two things can be done offline that change data:

- **Placing an order**
- **Staff adjusting stock**

They go into a pending-actions table in Room and a `SyncWorker` is scheduled.

### Placing an order offline

The order shows up in the orders list straight away, marked:

> Waiting for signal. We'll send it as soon as you're back online.

No code, no progress rail — it isn't a real order yet, and pretending
otherwise would be a lie the student acts on. Staff can't see it either,
because it hasn't been sent.

### SyncWorker

WorkManager, with a network constraint and exponential backoff. Fires when
connectivity comes back, replays the queue in order, and survives the app
being killed or the phone restarting.

## client_ref is what makes replay safe

This is the important bit.

Every order carries a `client_ref` UUID generated on the phone, and `orders`
has `unique(student_id, client_ref)`.

So if the worker sends an order, the response gets lost, and it retries — the
server sees a `client_ref` it already has and **returns the existing order**
instead of creating a second one.

Without that, a flaky connection during sync would be the difference between
one vetkoek and two, and between R17 and R34. It's the single mechanism
holding the whole offline story up. See [[The cart and checkout]].

## When a queued order fails

This is the case that needs real handling, because the world moved on while
the phone was offline. Any of the [[Order statuses]] failures can come back
hours later:

| What happened | What the student gets |
|---|---|
| `OUT_OF_STOCK` | "We couldn't place your order — vetkoek sold out while you were offline. Nothing was charged." |
| `SLOT_FULL` | "First break filled up before your order got through. Nothing was charged." |
| `INSUFFICIENT_FUNDS` | "Your order couldn't go through — not enough in your wallet." |

A notification, not a silent failure, because the student is probably not
looking at the app when the phone reconnects. They think they have food
coming. Finding out at 09:40 that they don't is the worst version of this
feature.

The queued order is removed and the cart is put back, so they can try again
without rebuilding it.

## Staff stock offline

The stepper moves immediately and the change queues.

Riskier than it sounds — two staff adjusting the same item offline will both
apply their change on reconnect. But stock movements are **deltas**, not
absolutes: `change_amount: -3` rather than `stock_quantity = 19`. Two people
selling three each lands at minus six, which is right.

Every one writes a `stock_movements` row, so the history explains any number
that looks wrong. See [[Stock]].

## Why it's built after everything else

Phase 10, not Phase 2. Caching something that doesn't work properly yet just
hides the bug behind a second layer, and then you're debugging two systems.

Online first, working, tested. Then cache it.
