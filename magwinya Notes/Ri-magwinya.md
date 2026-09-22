# Ri-magwinya

The tuckshop app for Rosebank College, Bloemfontein.

Students order their food before break, pay from a campus wallet, and walk up
to the counter with a 4-digit code instead of standing in a queue. Staff work
the orders off a live screen, keep stock straight, and top up student wallets.

This vault is the plain-English version of the whole thing. Start here, click
into whatever you want to know more about. Links that look faded are notes I
haven't written yet.

---

## Start with the big picture

| Note | Why it's here |
|---|---|
| [[What the app is]] | The one-paragraph version, plus the problem it actually solves. |
| [[The two roles]] | Student or staff. One app, two completely different insides. |
| [[A student's day]] | Walk through it from opening the app to eating the vetkoek. |
| [[A staff member's day]] | Same walk-through, from behind the counter. |
| [[The rules that matter]] | The handful of rules that decide how everything behaves. |

---

## What students can do

| Feature | Why it's there | More |
|---|---|---|
| Browse the menu | Live prices and live stock, so nobody orders something that ran out ten minutes ago. | [[The menu]] |
| Build an order | A vetkoek isn't just a vetkoek. You pick fillings, sizes, tea strength. | [[Building an order]] |
| Cart and checkout | Check it, pick a break time, pay. | [[The cart and checkout]] |
| Pick a collection slot | Spreads the rush across three breaks so the counter doesn't drown. | [[Collection slots]] |
| Pay from a wallet | No cash at the counter, no card machine queue at break. | [[The campus wallet]] |
| Pay at the counter | A fallback for when the wallet is empty. It has a catch. | [[Pay at the counter]] |
| Get a collection code | Four digits. Proves the order is yours without an ID check. | [[The collection code]] |
| Track the order | Placed, preparing, ready. Updates live, no refreshing. | [[Order statuses]] |
| Collect loyalty stamps | Ten orders, a reward. Gives people a reason to keep using it. | [[Loyalty stamps]] |
| Get notified | A push when the food is ready, so you leave class at the right moment. | [[Notifications]] |
| See the weather | Cold day, hot drinks move to the top of the menu. | [[Weather]] |
| Use it offline | Campus wifi is campus wifi. The app keeps working. | [[Offline mode]] |
| Change the language | English, Afrikaans, Sesotho. | [[Languages]] |

---

## What staff can do

| Feature | Why it's there | More |
|---|---|---|
| Work the order queue | Every live order on one screen, with one button to move it forward. | [[The staff queue]] |
| Manage stock | Counts go up and down as things sell, and staff can correct them. | [[Stock]] |
| See the sales | Money in, busiest hour, what sells. | [[Sales]] |
| Top up wallets | Student taps the speed point, staff load the amount into the app. | [[Wallet top-up]] |
| Edit the menu | Add an item, change a price, pull something that's finished. | [[Stock]] |
| Mark a no-show | Someone ordered and never came. That gets recorded. | [[Order statuses]] |

---

## Getting in

| Feature | Why it's there | More |
|---|---|---|
| Sign up and sign in | Any email, plus a student number. Open sign-up, so the student number is what ties an account to a person. | [[Signing in]] |
| Google sign-in | One tap instead of another password to forget. | [[Google sign-in]] |
| Biometric unlock | Fingerprint on open, because the wallet has money in it. | [[Profile and settings]] |
| Profile and settings | Name, language, theme, notifications, privacy. | [[Profile and settings]] |

---

## Under the hood

| Note | Why it's here |
|---|---|
| [[The tech stack]] | What we're building it with, and why each piece. |
| [[How the app is put together]] | The folder layout and why the code is split that way. |
| [[Supabase in plain English]] | Our whole backend, explained without the jargon. |
| [[The database tables]] | Every table, what it holds, in normal words. |
| [[The server does the maths]] | Why the phone never gets to decide what something costs. |
| [[Security rules]] | How we stop a student reading another student's orders. |
| [[The design system]] | Colours, type, spacing, and the rules that keep it looking calm. |
| [[Testing and CI]] | What gets tested automatically and why. |

---

## The build

| Note | Why it's here |
|---|---|
| [[The build plan]] | All 17 phases, in order, one line each. |
| [[Open decisions]] | Things I still need Dylan to confirm. |
| [[What Dylan has to do himself]] | Every checkpoint that needs a human, in one list. |
