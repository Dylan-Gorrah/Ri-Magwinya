# The two roles

Back to [[Ri-magwinya]]

One app, downloaded from one place. What you see after signing in depends on
what kind of account you have.

## Student

The default. Anyone who registers becomes a student.

Four tabs at the bottom:

- **Menu** — browse, search, build an order
- **Cart** — what you've picked, and checkout
- **Orders** — the one you're waiting for, and everything you've had before
- **Profile** — wallet, loyalty, settings

## Staff

Made by hand, not by registering. Dylan creates the account in Supabase and
then flips its role to `staff` with a bit of SQL.

Four tabs, completely different:

- **Queue** — the live order list, the main screen of their day
- **Stock** — counts, prices, adding and removing items
- **Sales** — money in, busy hours, top sellers
- **Profile** — same idea, minus the wallet

Staff also get a **Top up** button in the queue's top bar, because that's the
thing they'll reach for most often outside of the queue itself. See
[[Wallet top-up]].

## The important bit

**The role lives on the account in the database, not on the phone.**

The welcome screen has two cards on it, "I'm a student" and "I'm staff", and
that's purely decoration — it just pre-fills which sign-in form you see. If a
student taps the staff card, signs in, and their account says `student`, they
land on the student menu. Nothing else happens.

The prototype does it the other way round: tapping the card sets the role.
That's a demo shortcut and we're not copying it. If the phone could pick its
own role, anyone could give themselves the stock screen.

The same idea runs through the whole app. The database checks who you are on
every single request, and the rules live on the server where nobody can edit
them. See [[Security rules]].
