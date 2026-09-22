# The menu

Back to [[Ri-magwinya]]

The first thing a student sees after signing in, and the screen they'll spend
most of their time on.

## What's on the screen, top to bottom

**The navy hero.** A deep navy block (`navyDeep`) with the greeting, the
student's name, their wallet balance, and a bell for notifications. The status
bar goes light over it — this is the only screen where that happens.

**The weather strip.** A thin line under the greeting: *14°C · Light rain ·
Hot drinks first today*. If the weather call failed or hasn't come back, the
strip just isn't there and nothing else changes. See [[Weather]].

**The active order card.** If the student has an order in progress, it sits
here with its status and code, tappable straight through to [[Order statuses]].
No order, no card.

**Search.** Filters by name as you type.

**Category chips.** All, Meals, Snacks, Drinks. Single-select, the active one
filled navy.

**"Today's menu"** with a live pill next to it, then the items themselves.

## The item rows

Everything lives in one grouped container with hairline dividers between rows,
not a stack of individual cards. One border, not seventeen. See
[[The design system]].

Each row carries:

- The food icon in a `skyWash` thumbnail
- Name, and the description in secondary text
- A stock pill
- The price on the right

## Stock pills

Three states, driven by `stock_quantity` and `reorder_level`:

| Condition | Pill | Colour |
|---|---|---|
| `stock_quantity > reorder_level` | **In stock** | success |
| `0 < stock_quantity <= reorder_level` | **4 left** | warning |
| `stock_quantity = 0` or `is_available = false` | **Sold out** | error |

Sold-out rows are dimmed and don't respond to taps — the sheet won't open at
all. Every pill has a word on it, never colour alone.

## How prices are shown

Most items show one price: `R16.00`.

Items where the price depends on what you pick show **from R28.00** instead,
using the cheapest thing you could possibly build. From the prototype:

```js
function minPrice(m){
  var r = repGroup(m);                       // a group that replaces the price
  if (r) return Math.min(...r.choices.map(c => c.price));
  if (m.baseStep) return m.price;            // vetkoek: one vetkoek
  if (m.price === 0) {                       // sweets: cheapest single sweet
    var q = grp(m).filter(g => g.type === "qty")[0];
    if (q) return Math.min(...q.choices.map(c => c.price));
  }
  return m.price;
}
```

So chips shows **from R28.00** (small), sweets shows **from R1.00**
(a Chappies), vetkoek shows **from R3.00**.

## Where the data comes from

One request, with the options nested inside it:

```http
GET /rest/v1/menu_items?select=*,option_groups(*,options(*))&order=sort_order
```

PostgREST builds that nesting from the foreign keys, so the whole menu
including every option group and every option arrives in a single round trip.
No N+1.

## Live stock

The menu subscribes to Realtime changes on `menu_items`. When staff sell the
last Score energy off the counter and tap the stepper down, every student
phone with the menu open moves that row to **Sold out** without anyone
refreshing.

Same channel keeps prices current if staff edit one mid-day.

## The order of the list

Default is the `sort_order` column, set by staff on the stock screen.

The weather can override it: cold day and `hot` items float up, hot day and
`cold` items float up. It's a re-sort of the same list, not a filter —
nothing disappears. See [[Weather]].

## The four states

Like every screen in the app:

- **Loading** — skeleton rows, not a spinner. The layout doesn't jump when
  the real rows arrive.
- **Empty** — only really possible if staff have hidden everything. "No items
  on the menu right now."
- **Error** — a banner with a retry, and the cached menu underneath it if
  there is one. See [[Offline mode]].
- **Success** — the list.
