# Sales

Back to [[Ri-magwinya]]

The staff reporting screen. Usually opened at the end of the day.

## What's on it

**The headline numbers**

- Revenue
- Order count
- Average order value

**Orders by hour**, a bar chart. Shows when the rush actually is, which is
usually not quite when people assume.

**Top sellers**, a ranked list. The number one gets the gold marker — one of
only three uses of gold in the whole app. See [[The design system]].

**Tomorrow's weather**, with a stock hint:

> Cold tomorrow. Check tea and coffee stock.

A nudge, not an instruction. See [[Weather]].

**CSV export**, through the Android share sheet.

## Where the numbers come from

One call:

```http
POST /rest/v1/rpc/sales_summary
{ "from": "2026-09-22", "to": "2026-09-22" }
```

The aggregation happens **in the database**, not on the phone. The phone gets
back a handful of totals rather than every order for the period.

The alternative — pull all the orders and add them up in Kotlin — means
downloading a year of order history to display three numbers, on campus wifi,
and getting slower every month. Postgres sums rows for a living.

`sales_summary` is `security definer` and checks `is_staff()` before it
returns anything. See [[Security rules]].

## The chart

Vico, which is the Compose-native charting library — so the chart takes the
app's own colours and typography rather than looking like a library dropped
into the middle of the screen.

**The peak hour is named in text as well as coloured:**

> Busiest: 11:00 – 12:00 · 34 orders

Because a chart where the tallest bar is the only signal is useless to someone
who can't distinguish the colours, and is hard work for everyone else. Same
rule as the status pills — never colour alone. See
[[The rules that matter]].

## CSV export

Not a file saved somewhere to be found later. It goes straight into the share
sheet, so it can go to WhatsApp, Gmail, Drive, or anywhere else on the phone.

One row per order: number, date, time, student, items, total, payment method,
status.

Which means the tuckshop's records can leave the app and go into a
spreadsheet. Useful on its own, and a hedge — an app that traps its own data
is one nobody wants to depend on.

## What isn't here

No profit, no margins, no cost of goods. The app knows what things sold for,
not what they cost to make. Showing a "profit" figure built on numbers the app
doesn't have would be worse than showing nothing.

No per-student analytics either. Staff can see orders; they don't get a
dashboard of who buys the most. It isn't needed to run a tuckshop, and it's
personal information under POPIA. See [[Profile and settings]].
