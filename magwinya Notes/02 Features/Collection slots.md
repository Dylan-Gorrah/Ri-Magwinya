# Collection slots

Back to [[Ri-magwinya]]

You don't collect whenever you like. You pick a break, and each break only
holds so many orders.

## The three slots

| Name | Window | Capacity |
|---|---|---|
| First break | 09:40 – 10:00 | 40 |
| Second break | 11:00 – 11:20 | 40 |
| Afternoon | 14:30 – 14:50 | 25 |

Twenty minutes each. The afternoon one is smaller because fewer people are
still on campus and there's less staff on.

## Why cap them at all

Forty orders in twenty minutes is thirty seconds an order, and that's just the
handing over. Without a cap the app would happily take 200 orders for first
break and then the queue it was meant to remove would reappear, except now
everyone's food is going cold in it.

The cap is the whole point of pre-ordering. It converts "how long is the
queue" into a number the student can see **before** they commit.

## The table

```sql
collection_slots
  id            uuid  primary key
  name          text                       -- 'First break'
  starts_at     time
  ends_at       time  check (> starts_at)
  capacity      int   default 40
  orders_taken  int   default 0  check (<= capacity)
  service_date  date
  unique (name, service_date)
```

Two things worth pointing at:

- **`service_date`.** Slots are per day, not templates. Today's first break is
  a different row from tomorrow's, so `orders_taken` resets naturally and the
  history stays intact.
- **`check (orders_taken <= capacity)`.** The cap is a database constraint,
  not an `if` statement in the app. Even a bug in the server code can't
  oversell a slot — the database refuses the write.

## How a day's slots appear

Nobody creates them by hand. The app calls this when it loads the checkout
screen:

```http
POST /rest/v1/rpc/ensure_slots
{ "service_date": "2026-09-22" }
```

`ensure_slots(date)` creates the day's three rows if they aren't there yet,
and does nothing if they are. Safe to call every time, from every phone.

Then the app reads them:

```http
GET /rest/v1/collection_slots?service_date=eq.2026-09-22
```

## Booking one

Not by the app. `place_order` does it inside the same transaction as
everything else:

1. `SELECT … FOR UPDATE` on the slot row, which locks it
2. Check `orders_taken < capacity`
3. If not → `SLOT_FULL`, whole transaction rolls back
4. If so → write the order, `orders_taken = orders_taken + 1`

The row lock is what makes this safe when two people tap **Place order** on
the last spot at the same moment. The second one waits for the first to
finish, then sees the real count, then fails properly.

## What the student sees

Chips on the checkout screen showing the live count:

```
First break     09:40 – 10:00    12 of 40 taken
Second break    11:00 – 11:20    38 of 40 taken     [nearly full]
Afternoon       14:30 – 14:50     3 of 25 taken
```

Disabled when full, and disabled when the window has already passed — no
booking first break at 11:30.

`collection_slots` is on Realtime, so a slot that fills while the student is
looking at it updates on the spot rather than failing at the last step.

## Freeing a spot

Cancelling an order drops `orders_taken` by one and the spot goes back on
sale. See [[Order statuses]].

A **no-show** does not. The slot was held, staff made the food, the student
didn't come. Giving the capacity back would be pretending the order never
happened, and it's exactly the behaviour being discouraged. See
[[Pay at the counter]].
