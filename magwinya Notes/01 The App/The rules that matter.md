# The rules that matter

Back to [[Ri-magwinya]]

Most of the app is screens. These are the handful of rules underneath that
decide how it actually behaves. If you only read one note past the overview,
read this one.

## 1. The server decides the price

The phone works out totals so it has something to show you, but that number
is decoration. When you place an order, the server recalculates every line
from its own copy of the menu and charges you that.

Both sides use the same formula and we test it hard, so they should always
agree. If they ever don't, the server wins. Otherwise anyone with a bit of
patience could talk the app into selling them a R40 order for R4. See
[[The server does the maths]].

## 2. The role lives on the account

Nothing the phone says about who you are is trusted. Student or staff comes
from your row in the database, checked on every request. See [[The two roles]].

## 3. Placing an order is one transaction

Stock check, slot capacity check, wallet check, code generation, writing the
order, deducting stock, taking the money — all of it happens in a single
database transaction that either completes or doesn't.

No half-orders. No money taken for food that was out of stock. No two people
getting the last vetkoek because they tapped at the same second.

## 4. Only staff can close an order

A student can cancel their own order while it's still just *placed*. That's
it. Preparing, ready, collected and no-show are all staff-only.

The prototype had an "I have collected it" button on the student's screen.
It's gone.

## 5. Fillings-only vetkoek takes no vetkoek stock

You can order a snoek filling with zero vetkoeks. It costs R10 and it takes
nothing off the vetkoek count. The prototype forced a minimum of one; the
real app doesn't.

This matters because "units consumed" and "quantity" are different numbers
for these items, and the stock system tracks the first one. See
[[Building an order]].

## 6. Status is never colour alone

Every pill has a word on it. **In stock**, **4 left**, **Sold out**, **Ready**.
Colour is a second signal, never the only one — for colourblind users, and for
anyone glancing at a phone in the sun. See [[The design system]].

## 7. Money is counted in cents

In the code, R12.50 is the whole number 1250. Never 12.5 as a decimal.
Decimals drift, and drifting money in a wallet app is how you end up with a
balance of R49.999999.

## 8. Every visible word lives in strings.xml

From the very first screen, no text is typed directly into the code. That
turns the Afrikaans and Sesotho translations from a rewrite into a
copy-and-translate job. See [[Languages]].

## 9. The weather can never hold up the menu

Weather is a nice extra. If the weather call is slow or fails, the menu loads
anyway in its normal order and the weather strip just doesn't appear. It's
also cached for an hour so it isn't fetched constantly. See [[Weather]].

## 10. Nothing secret goes in the repo

Supabase keys, the Google services file, signing keystores, service accounts —
all of them stay out of git. The app only ever carries the public anon key,
which on its own can't do anything the security rules don't allow. See
[[Security rules]].
