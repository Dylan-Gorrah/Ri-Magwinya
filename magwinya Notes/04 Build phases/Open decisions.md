# Open decisions

Back to [[Ri-magwinya]]

What's settled and what still isn't. Each open one changes what gets built, so
it wants answering before the phase that depends on it.

---

## Settled

### Package name — `com.rimagwinya.app`

The app's permanent identity on the Play Store. Painful to change later,
because the Firebase project and the Google sign-in client both get tied to
it. Assumed and confirmed by everything in the brief already using it.

### Sign-in provider — Google

College email runs on Google, so students already have the account sitting on
their phone. Phase 12 uses Credential Manager to get a Google ID token and
hands that to Supabase.

What this means in practice, in Phase 12:

- Google Cloud Console needs an OAuth consent screen, a **Web** client ID and
  an **Android** client ID. The Android one needs the SHA-1 fingerprint from
  `./gradlew signingReport`.
- Supabase → Authentication → Providers → Google gets the Web client ID and
  secret pasted in.
- Both of those are dashboard jobs for Dylan. See
  [[What Dylan has to do himself]].

The brief's Azure/Microsoft alternative is dead. Ignore it.

### Loyalty — progress only, no redemption

Stamps are collected — one per collected order, written by the server — and
the profile shows how many you have out of ten. That's the entire feature.

**No redeeming.** No free-item flow, no "which items qualify" rule, nothing
touching the price calculation at checkout.

Why: redemption reaches into pricing, and pricing is the one part of this app
where a bug costs someone real money. The `loyalty_stamps` table already has
a `redeemed` boolean sitting unused, so if redemption is ever wanted later
it's an addition, not an unpick. See [[Loyalty stamps]].

### Note style — full detail

These notes carry the real thing: table columns, request bodies, error codes,
the actual formulas. Plain English around them, but not plain English
*instead* of them. The vault should be enough to rebuild from.

---

### Email domain — no restriction

**Students use ordinary Gmail accounts.** There is no college domain to check
against, so the domain rule is gone entirely:

- `AppConfig.ALLOWED_EMAIL_DOMAIN` is not needed. Drop it.
- The prototype's validation regex goes:
  ```js
  var okM = /^[^@\s]+@rcconnect\.edu\.za$/.test(v);   // delete this
  ```
  Replaced with an ordinary "is this a valid email address" check.
- `handle_new_user()` no longer rejects anything by domain. It still creates
  the profile row and still enforces that `student_number` is unique.
- Google sign-in accepts **any** Google account. After a first-time Google
  sign-in with no profile yet, the app asks for a student number on one extra
  screen before letting them through.

So the **student number is the only thing tying an account to a real person.**
Everything below follows from that.

---

## The thing to think about

### Sign-up is now open to anyone

This isn't a decision I need an answer on today, but it's a real consequence
and it shouldn't go unsaid.

With any Gmail accepted, anyone can make an account. Nothing checks that a
typed student number belongs to the person typing it, and `student_number` is
`unique` — so **whoever claims a number first owns it**, and the real student
is then locked out of ever using their own.

Most of the app survives this fine, because the wallet is the gate:

- A fake account has **R0.00**. It can browse, and it can't pay for anything.
- Topping up means standing in front of a staff member at the speed point with
  real money. That's a face-to-face check that no sign-up form can fake.

The hole is **pay at the counter**, which lets an account place an order
without paying first. A throwaway account could book stock and fill a
collection slot with no intention of collecting. `no_show_count` caps it at
two per account — but accounts are free and unlimited.

**What I'd suggest:** pay-at-counter is only offered to accounts that have had
at least one successful wallet top-up. In other words, staff have physically
seen this person once. It's a single extra condition inside `place_order`, no
new screens, and it makes the abuse cost a trip to the counter.

Wallet payment stays open to everyone, because wallet money was already
checked by a human going in.

I'll write it up that way unless you say otherwise — it's one line to remove
if you'd rather leave counter payment open. See [[Pay at the counter]].

### Claiming a student number

Worth knowing about even if we do nothing: if someone types a student number
that's already taken, they get "that student number is already registered".
The genuine student then can't register at all, and the fix is manual — Dylan
edits the row in Supabase.

At the scale of one campus tuckshop that's probably fine. Flagging it so
it's a known limitation rather than a surprise.

---

## Not a decision, just a flag

### Vetkoek at R3

Prices are rows in the database, editable from the staff stock screen in ten
seconds. Noted here only so nobody assumes it's baked into the code.

### The wallet holds real money

Students hand over real money at the speed point and the app records it as a
balance. If the app loses a top-up, someone is out of pocket.

So the wallet is a ledger: every change writes a `wallet_transactions` row,
and the balance only ever moves inside a server function — never by the app
writing to the column. See [[The campus wallet]] and [[Security rules]].
