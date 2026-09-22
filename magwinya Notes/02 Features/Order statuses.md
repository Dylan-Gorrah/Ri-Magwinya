# Order statuses

Back to [[Ri-magwinya]]

An order moves through a fixed set of states. Six of them, and the rules about
who can make which move are enforced on the server, not in the app.

## The states

| Status | Means |
|---|---|
| `placed` | Paid for (or approved for counter payment), waiting to be started |
| `preparing` | Staff have started making it |
| `ready` | Made, sitting on the counter, waiting to be collected |
| `collected` | Handed over. Done |
| `cancelled` | Called off before preparation started |
| `no_show` | Was ready, nobody came |

## The moves

| From | To | Who | What else happens |
|---|---|---|---|
| `placed` | `preparing` | staff | `prepared_at` set |
| `preparing` | `ready` | staff | `ready_at` set, push sent to the student |
| `ready` | `collected` | staff | `completed_at` set, +1 loyalty stamp |
| `ready` | `no_show` | staff | `no_show_count` on the profile goes up by 1 |
| `placed` | `cancelled` | the student, own order only | wallet refunded if they paid by wallet, stock restored, slot freed |

**Anything not in that table is refused** with `409 INVALID_TRANSITION`. You
can't jump `placed` straight to `ready`, can't un-collect an order, can't
cancel something that's already being made.

## The two rules that matter

### Students can't close their own orders

Only staff set `collected`. The student's screen has no "I've collected it"
button — the prototype had one and it's deliberately gone.

If students could mark their own orders collected, the record of what actually
went over the counter would be a record of what students *said* went over the
counter. Staff tap it while handing over the bag, which is the only moment
anyone actually knows.

### Cancelling stops when cooking starts

A student can cancel while the order is `placed`. The moment staff tap **Start
preparing**, the cancel button disappears.

The reason is ingredients. Once it's being made, cancelling means the tuckshop
eats the cost. Before that, nothing has happened and a refund costs nobody
anything.

The button doesn't grey out — it's removed, and the order screen says
preparation has started.

## What cancelling actually undoes

All inside one transaction in `set_order_status`:

- **Money back**, if it was a wallet order. A positive `wallet_transactions`
  row with `type = 'refund'`, and the balance goes up. Counter orders have
  nothing to refund.
- **Stock back.** Every line's `units_consumed` returns to `menu_items`, and
  each one writes a `stock_movements` row with `reason = 'cancel_restore'`, so
  the stock history explains itself.
- **Slot freed.** `orders_taken` drops by one and someone else can have the
  spot.

## What a no-show doesn't undo

Nothing. The food was made. The money stays taken, the stock stays consumed,
the slot stays used. See [[Collection slots]].

What it does do is add 1 to `profiles.no_show_count`, and at two the student
loses pay-at-counter. See [[Pay at the counter]].

Staff can only mark a no-show once the slot's window has ended — the action
doesn't appear on the card before then, so nobody gets marked down for being
three minutes late.

## Changing a status

One endpoint:

```http
PATCH /functions/v1/order-status
{ "order_id": "…", "status": "ready" }
```

| Response | Why |
|---|---|
| `403` | A student trying a staff-only move, or touching someone else's order |
| `409 INVALID_TRANSITION` | The move isn't in the table above |
| `200` | Done, updated order returned |

The function checks the role from the JWT and hands off to the
`set_order_status` SQL function, which does the timestamps, refunds, stock
restoration and loyalty stamp as one unit.

## How the student finds out

Two ways, both live:

- **Realtime.** The order screen subscribes to that row. A staff tap changes
  what's on the student's screen within about a second, no refreshing.
- **Push.** `preparing → ready` sends a notification, because that's the one
  the student needs when the app isn't open. See [[Notifications]].

## On screen

The student's order screen draws it as a progress rail with real timestamps:

```
  ●  Placed        09:12
  ●  Preparing     09:24
  ○  Ready
     Collected
```

Filled for done, hollow for current, grey for not yet. Cancelled and no-show
replace the rail with a single explanatory line rather than a broken-looking
half-filled one.
