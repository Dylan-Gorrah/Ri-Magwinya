# Ri-magwinya — Privacy notice

*Last updated: 22 September 2026*

This is the same notice the app shows under **Profile → Privacy and POPIA**.
Play requires a publicly reachable URL for it: publish this page (GitHub
Pages is enough) and paste the link into the store listing.

Ri-magwinya is a tuckshop pre-ordering app for Rosebank College,
Bloemfontein. This page explains what it keeps about you and why, as the
Protection of Personal Information Act (POPIA) asks us to.

## What we keep

- Your **name, email address and student number**, so staff can find your
  order at the counter.
- Your **orders** — what was in them, and when you collected them.
- Your **campus wallet balance** and the record of top-ups and payments.
- If you turn notifications on, a **device token**, so we can tell you when
  your order is ready.

## Why

To take your order, to charge the right amount, to hand the right food to
the right person, and to let the tuckshop plan its stock. Nothing else.

## Card payments

Top-ups are paid on the tuckshop's speed point, which belongs to the bank
and is a separate system from this app. **No card number ever reaches this
app**, and we never store card or bank details.

## Who sees it

You see your own account. Tuckshop staff see orders, names and student
numbers so they can serve you, and the wallet top-ups they record. Nobody
else. Nothing is sold, and nothing is shared with anyone outside the
tuckshop.

## How long we keep it

Your account and its order history stay while you use the tuckshop. Ask
staff to remove your account and we delete it, along with your orders.

## Your rights

You may ask what we hold about you, ask us to correct it, or ask us to
delete it. Speak to the tuckshop, or email the address on the college notice
board.

## Security

Your password is hashed by our sign-in provider (Supabase Auth, using
bcrypt) and is never stored by the app. All traffic is encrypted in transit.
The database enforces row-level security, so one student's information
cannot be read by another.

## Where the data is

The app's data is held by Supabase, in their EU (Ireland) region.
Notifications are delivered through Google Firebase Cloud Messaging. Weather
is fetched from Open-Meteo using the tuckshop's fixed coordinates — nothing
about you is sent with it.

## Contact

Rosebank College Bloemfontein tuckshop, or the email address on the college
notice board.
