# Building an order

Back to [[Ri-magwinya]]

The hardest thing in the app. Everything else is screens; this is the bit with
actual logic in it, and it's the reason Phase 4 exists on its own.

## The problem

"Vetkoek, R3" isn't an order. A real order is *two vetkoeks with two polony
and a cheese slice*, and that's R17.00. Meanwhile tea has three questions
attached to it and none of them change the price, and chips has one question
where the answer **is** the price.

Rather than writing a special case per item, there's one small engine that
handles all of it, and each item's behaviour is just data in the database.

## The three kinds of option group

Every group is a row in `option_groups` with a `type` and a `replaces_price`
flag.

### Single select (`type = 'single'`)

Radio buttons. Pick exactly one. Used for tea brand, milk, sugar.

Adds the chosen option's `price` to the total. For tea every option is R0.00,
which is why tea is R10.00 no matter what you do to it.

### Single select that replaces the price (`replaces_price = true`)

Same radio buttons, but the chosen option's price **becomes** the base price
instead of adding to it. Only chips uses this:

- Small — R28.00 (default)
- Medium — R35.00
- Large — R40.00

Large chips is R40.00, not R28 + R40.

### Quantity (`type = 'qty'`)

A stepper next to every choice, each one counted independently. Used for
vetkoek fillings and for sweets.

Each choice adds `option.price × count`.

## The base step

Separate from option groups. Four columns on `menu_items`:

| Column | Vetkoek's value | What it does |
|---|---|---|
| `base_step_label` | `Vetkoeks` | The plural, shown as the group heading |
| `base_step_singular` | `vetkoek` | For "1 vetkoek" and "No vetkoek" |
| `base_step_min` | `0` | How low the stepper goes |
| — | starts at 1 | The sensible default |

It's a stepper on the item itself, at `item.price` each. Two vetkoeks is R6.00
of vetkoek before any filling is added.

**The minimum is zero.** Set it to zero and you're buying fillings on their
own — a snoek for R10.00, no vetkoek. The sheet says *Fillings only, no
vetkoek.* underneath when you do.

The prototype forced a minimum of one. The real app doesn't, and that
difference is the whole reason `units_consumed` exists further down.

## The formula

One calculation, written once in `domain/pricing/PriceCalculator.kt` and
mirrored exactly inside the `place_order` SQL function:

```
unit =
    if the item has a base step:          item.price × base_qty
    else if a group replaces the price:   the selected option's price
    else:                                 item.price

  + for every qty group:                  Σ (option.price × count)
  + for every non-replacing single group: the selected option's price
```

Then:

```
build item     = has a base step OR has any qty group
quantity       = 1 for build items; otherwise the outer stepper
line total     = unit × quantity
units consumed = base_qty for base-step items (can be 0)
                 otherwise quantity
add to cart    = only allowed when unit > 0
```

### Why build items are always quantity 1

On a vetkoek the steppers **are** the quantity. If there were also an outer
"how many" stepper you'd have two different ways to say the same thing and a
guaranteed argument about which one won. So build items hide the outer
stepper entirely.

A Coke has no options at all, so it gets the outer stepper and quantity does
what you'd expect.

### Why units consumed is a separate number

Stock counts vetkoeks, not orders. Three cases:

- Two vetkoeks with fillings → **2** off the vetkoek stock
- Zero vetkoeks, one snoek → **0** off the vetkoek stock
- Four Cokes → **4** off the Coke stock

For anything without a base step, units consumed and quantity are the same
number. For vetkoek they come apart, and the stock system uses the first one.

### Why the total must be above zero

Sweets are priced at R0.00 with everything in a qty group, so an untouched
sweets sheet costs nothing. The button stays disabled and says
**Choose something first** until `unit > 0`.

## The check values

These are in `CLAUDE.md` and they're the unit tests. If any of these drift,
the engine is broken.

| What you build | Should cost |
|---|---|
| 2 vetkoeks + 2 polony + 1 cheese slice | **R17.00** |
| 0 vetkoeks + 1 snoek | **R10.00** |
| 3 Chappies + 1 assorted mix packet | **R13.00** |
| Large chips | **R40.00** |
| Tea, any combination of brand, milk and sugar | **R10.00** |

Working the first one through:

```
base:    R3.00 × 2   = R6.00
polony:  R3.00 × 2   = R6.00
cheese:  R5.00 × 1   = R5.00
                       ------
unit                 = R17.00
quantity             = 1        (build item)
line total           = R17.00
units consumed       = 2        (the base_qty)
```

## The option label

Every line carries a human-readable description of what it is, built as the
student changes things and then **frozen onto the order** when it's placed:

```
2 vetkoeks, 2 Polony, Cheese slice
Five Roses, With milk, 2 spoons
No vetkoek, Snoek
```

The rules, from the prototype's `sheetLabel()`:

- Base step of 0 → `No vetkoek`, 1 → `1 vetkoek`, more → `3 vetkoeks`
- Qty choices with a count of 0 are left out entirely
- Count of 1 → just the name; more than 1 → `2 Polony`
- Single-select choices are named as-is
- Joined with `, `

This string is saved into `order_items.options_label` as a snapshot, so staff
read exactly what the student saw even if the menu changes afterwards. The
machine-readable version goes into `order_items.options` as JSON.

## Cart lines stay separate

A cart line is keyed by the item **plus the exact selection**. So:

- *Vetkoek · 2 Polony*
- *Vetkoek · Snoek*

are two separate lines that never merge, even though they're the same item.
Adding *Vetkoek · 2 Polony* a second time bumps the existing line instead of
creating a duplicate. See [[The cart and checkout]].

## What the phone sends

The client sends **selections, not prices**:

```json
{
  "item_id": "…",
  "base_qty": 2,
  "options": [
    { "option_id": "…polony…", "count": 2 },
    { "option_id": "…cheese…", "count": 1 }
  ]
}
```

No total. No unit price. The server looks up every one of those IDs in its own
tables, runs the same formula, and charges what it gets. The number the phone
showed was only ever for the student's benefit.

See [[The server does the maths]] for why that matters.
