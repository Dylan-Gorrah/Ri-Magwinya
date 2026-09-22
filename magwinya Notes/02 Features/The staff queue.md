# The staff queue

Back to [[Ri-magwinya]]

The staff home screen and the busiest thing in the app. Everything else on the
staff side is occasional; this is what's open all day.

## The layout

**Counters at the top.** How many new, how many preparing, how many ready.
Glanceable — the question "how far behind are we" answered without reading
anything.

**Filter chips** under them. All, New, Preparing, Ready.

**The cards**, newest first.

## A card

```
  #1047                              4729
  Thabo Mokoena              Second break
  ─────────────────────────────────────────
  Vetkoek
  2 vetkoeks, 2 Polony, Cheese slice

  Tea
  Five Roses, With milk, 2 spoons
  ─────────────────────────────────────────
  Wallet · paid                      R27.00

  [        Start preparing         ]
```

Everything needed to make and hand over the order, with nothing to tap through
to.

- **Order number and collection code.** The code is large — it's what staff
  match against a student saying four digits. See [[The collection code]]
- **The student's name**, for the second check at handover
- **Every line with its full option label**, not "Vetkoek ×1". Staff need to
  know it's two vetkoeks with two polony and a cheese. That label is a frozen
  snapshot from when the order was placed, so it reads correctly even if the
  menu changed since. See [[Building an order]]
- **The slot**, so orders can be worked in collection order
- **The payment method.** `Wallet · paid` or `Counter · take payment`. Staff
  must know before handing anything over. See [[Pay at the counter]]

## One button, always the next step

No dropdowns, no status picker. The button says the thing to do:

| Status | Button |
|---|---|
| `placed` | **Start preparing** |
| `preparing` | **Mark ready** |
| `ready` | **Collected** |

Because it's a one-handed job done while holding food. A status menu would be
a wrong tap waiting to happen, and the moves are strictly ordered anyway. See
[[Order statuses]].

**Mark ready** also pushes a notification to the student. The most important
tap in the app. See [[Notifications]].

## Orders arrive by themselves

Realtime on `orders`. A student places an order and the card appears — no
refresh, no pull-to-refresh, no polling.

Updates land the same way, which matters with two staff on two phones: one
taps **Start preparing** and it moves on the other's screen too, so nobody
makes the same order twice.

## No-show

On `ready` cards only, and only **once the slot's window has ended**. Hidden
before that, so nobody is marked down for being three minutes late.

Tapping it sets `no_show`, adds 1 to that student's `no_show_count`, and at
two they lose counter payment. No refund, no stock restored — the food was
made. See [[Order statuses]].

## Top up

A **Top up** button in the top bar, because staff are on this screen when a
student walks over wanting to load money, and making them navigate elsewhere
for the second most common counter interaction would be silly. See
[[Wallet top-up]].

## Where the data comes from

```http
GET /rest/v1/orders?select=*,order_items(*)&order=placed_at.desc
```

No role filter in the URL. The security rules do it: students see their own
orders, staff see all of them, from the same query. See [[Security rules]].

## The states

- **Loading** — skeleton cards
- **Empty** — *"No orders yet."* Normal before first break, not an error
- **Error** — banner with retry, cached orders underneath. See
  [[Offline mode]]
