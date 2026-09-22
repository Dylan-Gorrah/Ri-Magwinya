# The build plan

Back to [[Ri-magwinya]]

Seventeen phases. Each one ends with an app that compiles, runs on the phone
and passes its tests — no phase leaves the build broken for the next one to
inherit.

One phase at a time. Claude finishes it, reports, stops, and waits for Dylan
to say go.

## The setup

| # | Phase | In one line |
|---|---|---|
| 0 | [[Phase 0 — Project setup]] | Dylan makes the empty Android project and gets it running on his phone. |
| 1 | [[Phase 1 — Foundation]] | Dependencies, folders, theme, the reusable components, empty screens you can navigate between. |
| 2 | [[Phase 2 — Backend]] | The Supabase database, the tables, the rules, the menu data. |
| 3 | [[Phase 3 — Authentication]] | Sign up, sign in, stay signed in, land on the right side of the app. |

## The core app

| # | Phase | In one line |
|---|---|---|
| 4 | [[Phase 4 — Student menu]] | The menu and the item sheet, with the full option engine and its price maths. |
| 5 | [[Phase 5 — Edge Functions]] | The three bits of server code: place an order, change its status, load a wallet. |
| 6 | [[Phase 6 — Cart and checkout]] | Cart, slot picking, payment choice, and actually placing the order. |
| 7 | [[Phase 7 — Student orders]] | The order screen with the code and the progress rail, updating live. |
| 8 | [[Phase 8 — Staff]] | Queue, stock, sales, wallet top-up. The entire staff side. |
| 9 | [[Phase 9 — Profile and settings]] | Profile, theme, notifications toggle, biometrics, privacy. |

## The extras the module marks separately

These four are the ones the PoE specifically asks for.

| # | Phase | In one line |
|---|---|---|
| 10 | [[Phase 10 — Offline mode]] | Caching, and a queue that replays orders once signal comes back. |
| 11 | [[Phase 11 — Push notifications]] | Firebase, so "your order is ready" reaches a locked phone. |
| 12 | [[Phase 12 — Single sign-on]] | One-tap sign-in with an existing account. |
| 13 | [[Phase 13 — Multi-language]] | Afrikaans and Sesotho, switchable inside the app. |

## Finishing

| # | Phase | In one line |
|---|---|---|
| 14 | [[Phase 14 — Weather]] | The weather strip, the menu re-sorting, the staff forecast card. |
| 15 | [[Phase 15 — Tests and CI]] | Fill the test gaps, and GitHub runs them on every push. |
| 16 | [[Phase 16 — Release]] | Signing, shrinking, privacy policy, store listing, a real installable build. |

## Why this order

- **Nothing visual before the design system.** Phase 1 builds the buttons and
  lists once, so every later screen is assembly rather than invention.
- **Backend before auth, auth before anything personal.** You can't route
  someone by role until roles exist.
- **The price maths before the cart.** Phase 4 gets the hardest logic in the
  app out of the way, with tests, before anything depends on it.
- **The server functions before checkout.** Phase 6 has something real to call.
- **Offline after everything online works.** Caching something that doesn't
  work yet just hides the bug.
- **Push before SSO before languages.** Each needs an outside account set up,
  so they're grouped where Dylan can do the dashboard clicks in a batch.

See [[What Dylan has to do himself]] for every point where the build stops and
waits for a human.
