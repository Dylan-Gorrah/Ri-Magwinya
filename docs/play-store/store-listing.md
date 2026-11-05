# Play Store listing

Ready to paste into the Play Console. Images are next to this file:
[`icon-512.png`](icon-512.png) and
[`feature-graphic-1024x500.png`](feature-graphic-1024x500.png). Screenshots
are listed in [`screenshots.md`](screenshots.md).

## App name

**Ri-magwinya** (30 characters max; this is 11)

## Short description

*80 characters max.*

> Order from the Rosebank College tuckshop, skip the queue, collect with a code.

(78 characters.)

## Full description

*4000 characters max.*

> **Order ahead. Skip the queue.**
>
> Ri-magwinya is the pre-ordering app for the Rosebank College tuckshop in
> Bloemfontein. Browse today's menu between lectures, build your order the
> way you actually eat it, pick a break to collect in, and pay from your
> campus wallet. When your food is ready you get a notification and a
> four-digit code — show it at the counter and go.
>
> **For students**
>
> • Today's menu with live stock, so you never order something that has
>   already sold out.
> • Build a vetkoek the way you want it: choose how many, then add polony,
>   cheese, achar, snoek and the rest. Fillings on their own are fine too.
> • Pick your chips size, your tea brand, milk and sugar, or mix your own
>   sweets.
> • Choose a collection break: first break, second break or the afternoon.
>   You can see how full each one is before you choose.
> • Pay from your campus wallet, topped up at the tuckshop counter.
> • Sign in with email or with your Google account.
> • A notification the moment your order is ready, even with the app
>   closed.
> • Watch your order move from placed to preparing to ready, live.
> • Cancel free of charge until the kitchen starts on it.
> • Works offline: the menu still opens without signal, and an order placed
>   in a dead spot is sent the moment you are back.
> • English, Afrikaans and Sesotho.
>
> **For tuckshop staff**
>
> • A live order queue with the full order as the student built it, the
>   collection code and the student's name.
> • One button per order: start preparing, mark ready, mark collected.
> • Stock management with low-stock warnings, straight from the counter,
>   and a push alert when an item runs low.
> • Today's sales: revenue, orders by hour, top sellers, and a CSV export.
> • Record wallet top-ups after a student pays on the speed point.
>
> **About payments**
>
> Top-ups are paid on the tuckshop's card machine, which belongs to the bank
> and is a separate system. No card details ever go into this app.
>
> **Privacy**
>
> We keep your name, email, student number and order history so staff can
> find your order at the counter. Nothing is sold or shared. The full POPIA
> notice is in the app under Profile → Privacy and POPIA.

## Category and tags

- Category: **Food & Drink**
- Tags: food ordering, campus, tuckshop, pre-order
- Content rating questionnaire: no objectionable content; the app takes
  orders for food and holds a prepaid balance.

## Data safety form — the honest answers

| Question | Answer |
|---|---|
| Does the app collect personal data? | Yes |
| Name, email address | Collected, for account and order handover (from Google if you sign in with Google) |
| Student number | Collected, as the identifier staff use |
| Purchase history | Collected (orders and wallet transactions) |
| Device identifier | Collected only if notifications are on (FCM token) |
| Financial info | **No card or bank details.** A prepaid balance only |
| Is data shared with third parties? | No |
| Is data encrypted in transit? | Yes |
| Can users request deletion? | Yes, through tuckshop staff |

## Screenshots

See [`screenshots.md`](screenshots.md).

## Feature graphic

[`feature-graphic-1024x500.png`](feature-graphic-1024x500.png): 1024 × 500
px PNG, no transparency. The brand mark on the navy background (`#0D1F3D`)
with "Ri-magwinya" and the tagline "Order ahead. Skip the queue.", nothing
near the edges, because Play crops it on some layouts.

## App icon

[`icon-512.png`](icon-512.png): 512 × 512 px PNG, no transparency, no
rounded corners (Play rounds it). Same brand mark as the launcher icon.

Both are rendered by [`src/render.py`](src/render.py) from the prototype's
SVG, so they can be regenerated.

## Release notes for 1.1.0 (what's new, 500 characters max)

> Sign in with Google. Get a notification the moment your order is ready.
> A separate staff sign-in. Staff now get an alert when an item runs low.
> Smoother sign-in and navigation, and a new app icon.

## The signed bundle

`app-release.aab`, version 1.1.0 (versionCode 4), signed with the upload key.
Built with `./gradlew bundleRelease`. It is attached to the GitHub Release
rather than committed, like the APK.
