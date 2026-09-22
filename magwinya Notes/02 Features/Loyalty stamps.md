# Loyalty stamps

Back to [[Ri-magwinya]]

Collect a stamp per order. Ten is the target. That's the whole feature.

## What it does

One stamp per **collected** order. Not per order placed — per order actually
picked up. Cancelled, no-show and in-progress orders earn nothing.

The profile shows how many you have:

```
  LOYALTY
  ●●●●●●●○○○      7 of 10
  Three more to go.
```

## What it doesn't do

**There's no redeeming.** Ten stamps don't buy anything. The counter shows
progress and stops there.

This was a decision, not an oversight. See [[Open decisions]].

Redemption would reach into pricing — which items qualify, how a free line is
represented on the order, how the server marks stamps used without a student
being able to spend the same ten twice. Pricing is the one part of this app
where a bug costs someone real money, and [[Building an order]] is already the
most complicated thing in it. Not worth the risk for a nice-to-have.

The table keeps a `redeemed` column anyway:

```sql
loyalty_stamps
  id         uuid
  user_id    uuid
  order_id   uuid  unique
  redeemed   boolean  default false
  created_at timestamptz
```

Unused for now. It means adding redemption later is a feature, not a
migration.

## `order_id unique` is the whole safety mechanism

One order can only ever produce one stamp. The database enforces it, so a
retried status change, a double tap, or a bug in `set_order_status` still
can't mint a second stamp for the same order.

That's it. A `unique` constraint instead of careful code.

## Where it's written

Inside `set_order_status`, on the `ready → collected` move, in the same
transaction as `completed_at`. See [[Order statuses]].

The count is just:

```sql
select count(*) from loyalty_stamps
where user_id = auth.uid() and not redeemed;
```

## Why it's gold

Loyalty is one of only three things allowed to use the gold colour — the
others being the staff badge and the top-seller marker. Gold means "this is a
small reward", and if it were used anywhere else it would stop meaning
anything. See [[The design system]].
