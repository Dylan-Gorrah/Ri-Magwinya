# Stock

Back to [[Ri-magwinya]]

The staff screen for counts, prices and what's on the menu at all.

## The stock list

Every item with a stepper next to its count:

```
  Vetkoek                    In stock
  R3.00                      [ − 40 + ]

  Score Energy 500ml         4 left
  R18.00                     [ −  4 + ]

  Doritos                    Sold out
  R25.00                     [ −  0 + ]
```

Tapping a stepper PATCHes the item:

```http
PATCH /rest/v1/menu_items?id=eq.<id>
Prefer: return=representation
{ "stock_quantity": 39 }
```

`Prefer: return=representation` means the updated row comes back in the same
call, so the screen shows what the database actually holds rather than what
the app assumed.

Students see the change live over Realtime, within about a second. See
[[The menu]].

## Why staff adjust it by hand

The app already decrements stock when orders are placed. The steppers are for
everything that happens outside the app:

- A walk-up sale in cash
- A delivery arriving
- Something dropped, burnt, or gone off
- A miscount that needs correcting

A tuckshop isn't a closed system, and pretending otherwise means the counts
drift until nobody trusts them.

## The low-stock banner

Anything at or below its `reorder_level` (default 5) appears in a warning
banner at the top:

> **Running low:** Score Energy (4), MoFaya (3)

Crossing the threshold also fires a `low_stock` notification to staff on the
low-importance stock channel — useful, not urgent, no sound. See
[[Notifications]].

At zero, the item goes **Sold out**: dimmed on the student menu and impossible
to add to a cart.

## Every change is logged

A trigger on `menu_items` writes a row whenever stock moves:

```sql
stock_movements
  id            uuid
  item_id       uuid
  change_amount int      -- the delta, not the new total
  reason        text     -- sale | restock | waste | correction | cancel_restore
  staff_id      uuid     -- from auth.uid()
  order_id      uuid     -- when a sale caused it
  created_at    timestamptz
```

It records **deltas**, which does two jobs. It makes the history explain
itself — any count can be traced back through every change that produced it —
and it makes offline edits safe, because two staff each selling three lands at
minus six rather than one overwriting the other. See [[Offline mode]].

`staff_id` comes from `auth.uid()` inside the trigger, not from the app, so it
can't be spoofed or forgotten.

## Editing the menu

A sheet for adding, editing and removing items. Name, description, category,
price, icon, reorder level, `temperature_tag`, sort order, and the option
groups.

**Prices are data, not code.** Changing the price of a vetkoek is a
thirty-second job on this screen — no rebuild, no release, no waiting for
anyone. That's the reason the R3 vetkoek price was never worth arguing about
in [[Open decisions]].

Removing an item doesn't erase history. `order_items` holds `item_name`,
`options_label` and `unit_price` as **snapshots**, and the foreign key is
`on delete set null` — so old orders still read correctly for something that
hasn't been sold in months.

Options live in their own tables and are edited as part of the item. Adding a
new vetkoek filling is a row in `options`, and it appears on every student's
sheet with no app change. See [[Building an order]].

## Who's allowed

Reading the menu is open to everyone, including signed-out users. Writing to
`menu_items`, `option_groups` and `options` requires `is_staff()`.

So even if a student's app were modified to send a PATCH, the database refuses
it. The screen is staff-only because the *data* is staff-only, not because the
navigation hides it. See [[Security rules]].
