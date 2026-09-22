# Progress

Living record of where the build is. Read this and `CLAUDE.md` at the start
of every session.

The plain-English documentation lives in the `magwinya Notes` Obsidian vault.
Start at `Ri-magwinya.md`.

---

## Where we are

**Phase 0 — Project setup · DONE**
**Phase 1 — Foundation · DONE**
**Phase 2 — Backend · DONE** (all 12 migrations applied to the real project
on 2026-09-22; waiting on one run on the phone)
**Phase 3 — Authentication · READY TO TRY** (needs Dylan to register an
account and make a staff user)
**Phase 4 — Student menu · BUILT** (live stock updates in; waiting on a phone check)
**Phase 5 — Edge Functions · DONE** (deployed and tested against the live project)

The Supabase MCP connector (`supabase-rm` in `.mcp.json`, project
`jykswltsegssjmvnqqbi`) is signed in and working. The publishable key is in
`local.properties`.

---

## Phase 0 — Project setup · DONE

Dylan created the project from **Empty Activity** (Compose, not Views) and
Claude brought it in line with the brief.

### The generated project

| | |
|---|---|
| AGP | 9.2.1 |
| Gradle | 9.4.1 |
| Kotlin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| Java | 11 |
| compileSdk | **37** |
| targetSdk | 36 |
| minSdk | **24** |
| Package | **com.rimagwinya.app** |

### What was changed after generation

1. **Package renamed** `com.snokonoko.ri_magwinya` → `com.rimagwinya.app`.
   Source folders, package declarations, imports, `namespace`,
   `applicationId`, and the packageName assertion in
   `ExampleInstrumentedTest`. Dylan confirmed. Done now because Firebase
   (Phase 11) and the Google OAuth Android client (Phase 12) both get tied
   to it later.
2. **minSdk 33 → 24**, per the brief. Dylan confirmed.
3. **compileSdk 36.1 → 37.** Not a preference — the generated project
   didn't build. `androidx.core:core-ktx:1.19.0` and
   `androidx.lifecycle:lifecycle-runtime-compose:2.11.0` both require
   compiling against API 37. AGP downloaded the platform itself.
4. **`mipmap-anydpi` → `mipmap-anydpi-v26`.** `<adaptive-icon>` needs API
   26+, so at minSdk 24 resource linking failed. The density `.webp` files
   cover API 24–25. Image Asset Studio will regenerate this correctly in
   Phase 1 anyway.
5. **`docs/` created** and `rimagwinya-prototype.html` moved into it, which
   is where `CLAUDE.md` says it lives.

### Verified

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL**
- `./gradlew testDebugUnitTest` — **BUILD SUCCESSFUL** (1 template test)
- Not yet run on the phone. Dylan should press Run once to confirm.

---

## Phase 1 — Foundation · DONE

### Toolchain moved

Kotlin 2.2.10 -> **2.3.20**, to match KSP. KSP moved to standalone versioning
at 2.3.0 and the current 2.3.12 is built against Kotlin 2.3.20; pairing it
with 2.2.10 would have pulled a mismatched stdlib.

### Version catalog

Every library in `CLAUDE.md` section 4 is declared in `libs.versions.toml`,
with versions checked against Google Maven and Maven Central on 2026-09-22
rather than recalled. Key ones: Hilt 2.60.1, Room 2.8.5, Retrofit 3.0.0,
OkHttp 5.5.0, supabase-kt 3.8.0, Ktor 3.6.0, Coil 3.6.3, Vico 3.3.1,
Navigation Compose 2.10.1, desugar_jdk_libs 2.1.5.

**Only what Phase 1 uses is wired into `dependencies`** — Compose,
navigation, Hilt, splashscreen, AppCompat, desugaring and the test stack.
Room, Retrofit, Supabase and the rest are declared and ready, and get their
`implementation` line in the phase that first needs them. Declaring a
dependency costs nothing; including an unused one costs build time.

Retrofit 3.0.0 has an official kotlinx.serialization converter
(`com.squareup.retrofit2:converter-kotlinx-serialization`), so the
JakeWharton one the brief implies is no longer needed.

### Build configuration

- Core library desugaring on, for `java.time` on API 24 and 25
- `buildConfig = true`; `SUPABASE_URL` and `SUPABASE_ANON_KEY` read from
  `local.properties` and default to `""` so the project builds without them
- Java 17 source and target
- Room schema export configured at `app/schemas` ahead of Phase 10

### Design system

`core/designsystem/theme` — `Color.kt` (both palettes as an `RmColors`
CompositionLocal), `Type.kt`, `Shape.kt`, `Spacing.kt`, `Motion.kt`,
`Theme.kt`. No Material You dynamic colour: the palette is the brand, and
letting the wallpaper repaint it would undo the design system.

**Inter is bundled.** Fetched as the variable font from google/fonts, then
instanced to three static weights (400/500/600) with fonttools and subset to
the Latin ranges English, Afrikaans and Sesotho need. 1 MB became 195 KB, and
`tnum` is kept for the tabular price figures. Static rather than variable
because variable weight axes need API 26 and minSdk is 24 — the font would
otherwise render every weight as Regular on Android 7.

### Components

12 previews across 9 files in `core/designsystem/component`:

`RmButton` (primary, secondary, quiet, danger, small) · `RmTextField` ·
`StockPill` · `StatusPill` · `GroupedList` + `ListRow` + `ListDivider` ·
`SkeletonRow` · `Stepper` · `ChipRow` + `Chip` · `ToggleRow` + `RmToggle` ·
`Banner` · `Hud` · `EmptyState` · `RmBottomSheet` · `ProgressRail` ·
`CodeTiles` · `RoleTabBar` · `BrandMark` · `FoodIcon`

### Icons

All 41 `<symbol>` definitions in the prototype were converted to vector
drawables by a script, not by hand: 1 brand mark, 30 UI icons, 10 food icons.
The converter turns `<circle>`, `<ellipse>` and `<rect>` into path data,
inherits symbol-level stroke and fill attributes, and maps `currentColor` to
black so Compose can tint it. Script kept in the scratchpad; the output is
committed and marked "do not hand-edit".

### Navigation

Type-safe routes as `@Serializable` objects in `navigation/Routes.kt`. Auth
graph, student graph, staff graph, every destination reachable with a titled
placeholder. `Profile` is declared once at the root because both roles share
it and two graphs cannot own the same route.

The welcome screen's two role cards currently sign straight in so both graphs
can be walked. **That shortcut is removed in Phase 3** — in the real app the
cards only pick which sign-in copy you see, and the role comes from the
server.

### Other

- `MainActivity` extends `AppCompatActivity` with an AppCompat DayNight
  theme, needed for per-app language in Phase 13
- Splash screen via `core-splashscreen`, brand mark on the app background
- `RimagwinyaApp` with `@HiltAndroidApp`
- All user-facing text in `strings.xml` from the start
- `.gitignore` covers `google-services.json`, `*.jks`, `*.keystore` and
  service-account JSON

### Verified

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL**, no warnings in app code
- `./gradlew testDebugUnitTest` — **8 tests, 0 failures** (`MoneyTest`)
- Debug APK 14.4 MB
- Not yet run on the phone.

### Still owed from Phase 1

- **Launcher icon.** *File -> New -> Image Asset* with Dylan's PNG. The
  template icons are still in place.

---

## Phase 2 — Backend · DONE

### Applied to the real project — 2026-09-22

All migrations applied through the MCP connector, in order. Checked live:

- 17 items, 8 option groups, 30 options, today's 3 slots
- Signed out: the menu reads (200); `orders` returns nothing; `profiles`
  is refused; every `/rpc/*` function is refused
- Sign-up trigger creates the profile with role `student` and the student
  number normalised (tested inside a rolled-back transaction)
- Security advisor: the only warnings left are the seven functions signed-in
  users are meant to call. Each checks the caller itself

### 0012_hardening — found on the real project

1. **Function privileges.** Postgres and Supabase grant EXECUTE on new
   functions to everyone, including signed-out callers. Nothing leaked —
   each function refused without a user — but they are now unreachable
   signed out, and trigger/internal functions are unreachable entirely.
2. **Time zone.** The database is UTC, the tuckshop is UTC+2. "Today" was
   wrong between midnight and 02:00, and the sales chart would have shifted
   every hour by two. `place_order`, `ensure_slots` and `sales_summary` are
   pinned to `Africa/Johannesburg`.
3. **Student numbers** are upper-cased and trimmed everywhere, not only in
   `claim_student_number`, so "ab123" and "AB123" cannot both register and
   staff typing lowercase on the top-up screen still find the student.
4. `touch_updated_at` got a fixed `search_path`.

### New-style API key

Supabase now issues `sb_publishable_…` keys instead of the JWT anon key.
It goes in `SUPABASE_ANON_KEY`. It is not a JWT, so the interceptor no longer
sends it as `Authorization: Bearer` when signed out — only `apikey`. The
`sb_secret_…` key is **not** stored anywhere in the project; it belongs in
Edge Function secrets only.

### Migrations — `supabase/migrations/`

Twelve numbered files, run in order. `supabase/README.md` has the full guide,
the verification queries and the curl checks.

0001 enums · 0002 tables · 0003 indexes · 0004 helpers · 0005 place_order ·
0006 set_order_status · 0007 wallet_and_sales · 0008 rls · 0009 realtime ·
0010 seed_menu

Worth knowing about the schema:

- **`student_number` is nullable.** Google sign-in creates a profile from a
  token with no student number in it, so there is a window where the account
  exists and the number does not. `place_order` raises
  `STUDENT_NUMBER_REQUIRED` without one, so such a profile can browse and
  nothing else. This was not in the brief — it is a consequence of the open
  sign-up decision and it would have surfaced as a Phase 12 crash.
- **No insert or update policies** on orders, order_items or
  wallet_transactions. The security definer functions are the only way in.
- **Column grants**, not just row policies, are what stop a student PATCHing
  their own `wallet_balance` or `role`. They may update `full_name`,
  `language` and `fcm_token` and nothing else.
- **Slot capacity is a check constraint**, so even a bug in `place_order`
  cannot oversell a break.
- **`place_order` is idempotent** on `(student_id, client_ref)`.
- **Pay-at-counter requires a prior top-up**, per section 12.1.

### Networking — `core/network/`, `data/`

- Retrofit + OkHttp + kotlinx.serialization, base URL from `BuildConfig`
- Auth interceptor sending `apikey` and `Authorization`. A `TokenProvider`
  interface returns null for now and the interceptor falls back to the anon
  key; Phase 3 rebinds it to the live session without touching this layer
- `ErrorMappingInterceptor` turns HTTP failures into a typed `ApiError`
  before they reach a repository, so screens match on a case rather than
  parsing a status code. `ConflictCode` holds the 409 reason codes
- Body logging in debug only, with `Authorization` and `apikey` redacted
- `SupabaseApi` — menu with nested options in one round trip, slots,
  `ensure_slots`
- DTOs, and `MenuMapper` as the single place `Double` becomes `Money`
- `MenuRepository`, `MenuViewModel` with one `UiState`, and
  `MenuSmokeScreen` wired onto the Menu route

`MenuSmokeScreen` is deliberately plain — no hero, no search, no categories.
Phase 4 replaces the file. It shows a clear "Backend not configured" banner
until the keys are in `local.properties`.

### Verified against a real Postgres

The migrations were applied to a **throwaway local Postgres 18** (the machine
already had one) with a small shim standing in for the Supabase-specific
pieces — `auth.users`, `auth.uid()`, the roles and the realtime publication.
See `supabase/tests/`.

- All ten migrations apply clean, in order
- Seed lands 17 items, 8 option groups, 30 options, 3 slots
- **`supabase/tests/run.sh` — 35 assertions, 0 failures**
  - every check value from section 9.1, placed as a real order
  - `units_consumed` is 0 for a fillings-only vetkoek, which is the rule most
    likely to be got wrong
  - option labels come out exactly as the prototype formats them
    (`2 vetkoeks, 2 Polony, Cheese slice`, `No vetkoek, Snoek`)
  - counter payment blocked with no top-up, allowed after one, blocked again
    after two no-shows
  - same `client_ref` twice gives one order and one charge
  - out of stock, empty selection, full slot
  - the whole transition table, including every move that must fail
  - cancel restores stock, refunds the wallet, frees the slot
  - the Google window: a profile with no student number can exist but cannot
    order; a duplicate number is refused

One latent bug found and fixed while testing: `replaces_price` subtracted the
plain item price, which would be wrong on an item that also had a base step.
No seeded item has both, but staff could create one from the stock screen.

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL**
- `./gradlew testDebugUnitTest` — **15 tests, 0 failures**

Still **not** run against the real Supabase project, and no request has been
made from the phone. That is the blocked part.

---

## Phase 3 — Authentication · READY TO TRY

Written and compiling. It cannot be tried until there is a database to
register against.

### Screens

- **Welcome** — brand mark, tagline, two role cards. The cards choose which
  sign-in copy you see **and nothing else**. The Phase 1 version signed you
  straight in so the graphs could be walked; that shortcut is gone, along
  with `WelcomePlaceholder.kt`.
- **Login** — email, password, one deliberately vague error.
- **Register** — name, email, student number, password, POPIA notice above
  the button.

Validation runs as you type but errors only appear once a field has been
left, so nothing nags mid-typing. The submit button stays disabled until the
form could actually succeed.

### Auth

- supabase-kt `Auth` for sign-up, sign-in, sign-out, session storage and
  background refresh. Passwords are bcrypt-hashed by Supabase; the app never
  hashes or stores one.
- `TokenProvider` rebound from the Phase 2 stub to the live session.
  **Nothing in `core/network` changed** — that was the point of the seam.
- `SessionViewModel` is the single place that decides which half of the app
  loads, and it decides from `profiles.role` on the server.
- Signing in or out swaps the whole navigation graph underneath whatever is
  on screen, so no screen has to know where to go afterwards.

### Two things the brief did not cover

**`claim_student_number` (migration 0011).** Google sign-in creates a profile
with no student number, so the app has to be able to set one. A column grant
would have let anyone rewrite theirs at any time and take a number belonging
to someone else. It is a `security definer` function that sets the value only
when it is still null.

**Error mapping.** Supabase returns 400 for a wrong password, which is not
what 400 usually means. `AuthRepository` translates it, and every failure
becomes a string resource rather than a sentence, so Phase 13 stays a
translation job.

A wrong password and an unknown email give the **same** message on purpose —
"no account with that email" tells anyone who asks which addresses have
accounts here.

### Verified

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL**
- `./gradlew testDebugUnitTest` — **27 tests, 0 failures**
  (`MoneyTest` 8, `MenuMapperTest` 7, `ValidationTest` 12)

Not yet exercised against a server: no account has been registered and no
sign-in has happened.

### Still owed

- **Checkpoint:** register a student in the app, create a staff user in
  *Authentication → Users*, and promote it. SQL is in `supabase/README.md`.

---

## Phase 4 — Student menu · IN PROGRESS

### Done: the pricing engine

`domain/pricing/PriceCalculator.kt` and `Selection.kt`. This is the hardest
logic in the app, so it was built and tested first, before anything depends
on it.

`Selection` holds **choices, not prices** — there is no money in it to
tamper with. The price is derived from it here, and derived again from the
same choices by `place_order` when the order is placed.

**`PriceCalculatorTest` — 21 tests**, covering every check value from section
9.1, plus: build items ignoring the outer stepper, an untouched sweets sheet
being unaddable, how each sheet opens, the "from R…" floors, and the option
labels.

The labels matter more than they look. They are frozen onto the order and are
what staff read at the counter, and the client and the server generate them
independently. Both produce:

```
2 vetkoeks, 2 Polony, Cheese slice
No vetkoek, Snoek
3 Chappies, Assorted mix packet
```

`Selection` is a data class, so cart lines can key on it directly and
"Vetkoek · 2 Polony" stays a separate line from "Vetkoek · Snoek". Setting a
count to zero removes the entry rather than storing a zero, so two identical
builds compare equal.

### Client and server agree

The same six orders are asserted twice, independently:

| | Client | Server |
|---|---|---|
| | `PriceCalculatorTest` | `supabase/tests/pricing_test.sql` |
| 2 vetkoeks + 2 polony + 1 cheese | R17.00 | R17.00 |
| 0 vetkoeks + 1 snoek | R10.00, 0 units | R10.00, 0 units |
| 3 Chappies + 1 mix | R13.00 | R13.00 |
| Large chips | R40.00 | R40.00 |
| Tea, any options | R10.00 | R10.00 |
| 4 Cokes | R64.00 | R64.00 |

### Done: the cart

`CartRepository`, in memory behind a `StateFlow<Cart>`. Phase 10 backs it
with Room and the shape the screens see does not change.

A line is keyed by the item **and** the selection, so "Vetkoek · 2 Polony"
and "Vetkoek · Snoek" stay separate while the same build merges. Build items
have no outer quantity — the steppers are the count — so `setQuantity` on one
is ignored rather than creating a second way to say the same thing.

**`CartRepositoryTest` — 10 tests.**

### Done: the screens

- **`MenuScreen`** — navy hero with greeting, wallet balance and bell;
  search; category chips; "Today's menu" with a live pill; one grouped list
  with a stock pill per row; "from R…" on variable-priced items; sold-out
  rows dimmed and unopenable. Loading, empty, error and success all handled.
- **`ItemSheetContent`** — the option engine on screen. One sheet drives
  every item because the behaviour is data: base step, qty group,
  single-select, or single-select that replaces the price. Nothing is
  special-cased per item. The button carries the live total and reads
  "Choose something first" while the unit price is zero.
- **`MenuIcons`** maps `icon_key` to a drawable, so staff can add an item and
  pick its icon without an app release.
- `MenuSmokeScreen` from Phase 2 is deleted, replaced by the real screen.

### Done: live stock updates

`MenuRealtime` subscribes to `menu_items` over the supabase-kt socket while
the menu screen is alive. An update patches stock, availability and price on
the one item it names, in place, with no spinner. An insert or delete, and
every (re)connect, triggers a quiet full reload instead — the payload has no
option groups, and anything changed while the socket was down was never
sent. If the socket fails, the menu still works and it retries with backoff
up to 30 seconds.

Rows are read field by field (`toMenuUpdate`) rather than decoded whole, so
a missing column or a numeric sent as a string patches what it can.
**`MenuChangeTest` — 7 tests.**

### Fixed: strings in code

`MenuRepository.friendly()` returned English sentences and the menu screen
had the "not configured" text inline, against rule 11. Both are now string
resources; `ApiError.messageRes()` in `core/network` is the shared mapping.

### Verified

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL**
- `./gradlew testDebugUnitTest` — **65 tests, 0 failures**
- Not yet run on the phone against the live database.

---

## Phase 5 — Edge Functions · DONE

### Deployed — 2026-09-22

`place-order`, `order-status`, `load-wallet` in `supabase/functions/`, with
shared code in `_shared/`. Each verifies the caller, validates the body,
calls its SQL function **as the caller** (so the SQL's own role checks
decide, never the function), and maps the SQL's exceptions to
`{ code, detail }` with the right status.

**`verify_jwt = false`, deliberately.** The project uses the new
publishable/secret keys and asymmetric JWTs; the platform's built-in check
only understands the legacy keys. `_shared/auth.ts` verifies the token with
`getClaims` instead. Recorded in `supabase/config.toml`.

**FCM sender** (`_shared/fcm.ts`): full HTTP v1 flow, signed with the
service account via WebCrypto. Does nothing until the Phase 11 secrets
exist, and never fails or delays a request — pushes run after the response.

### Tested live, with real accounts

| Case | Result |
|---|---|
| Order: 2 vetkoeks + 2 polony + cheese, large chips, snoek-only vetkoek | 200, R67 = 17 + 40 + 10, snoek line consumes 0 vetkoeks |
| Same `client_ref` again | same order, charged once |
| R48 of Coke with R33 left | 409 `INSUFFICIENT_FUNDS`, detail `15.00` |
| 5 Score with 4 in stock | 409 `OUT_OF_STOCK` |
| Vetkoek with nothing on it | 409 `EMPTY_SELECTION` |
| Counter payment, never topped up | 409 `COUNTER_BLOCKED` / `NO_TOPUP_YET` |
| Student moves an order | 403 |
| placed → ready, preparing → cancelled, collected → ready | 409 `INVALID_TRANSITION` |
| placed → preparing → ready → collected | 200 each, timestamps set, one loyalty stamp |
| Student cancels while placed | wallet refunded, stock restored, slot freed |
| Top-up: no login / student / R5 / R12.345 / unknown number | 401 / 403 / 400 / 400 / 404 |
| Top-up R100 typing `test001` | 200, found as `TEST001` |

Test data left in the database: order #1 (collected), order #2
(cancelled), user123 on R33, vetkoek stock 38, chips 29.

### App side

- `FunctionsApi` — the three functions as a Retrofit interface, with DTOs
- `ErrorMappingInterceptor` — a known reason code now wins over the HTTP
  status, so 404 `STUDENT_NOT_FOUND` and 400 `AMOUNT_OUT_OF_RANGE` reach the
  screen as codes rather than as a generic "not found"
- **Bug fixed from Phase 2:** `ApiError` extended `Exception`. OkHttp wraps a
  non-IOException thrown from an interceptor in "IOException: canceled", so
  every refusal would have reached the screen as **Offline** — "No
  connection" instead of "Sold out". It now extends `IOException`.
- **`FunctionsApiTest` — 11 tests** with MockWebServer, using the real
  response bodies. They are what caught the bug.

### Accounts

| | Email | Role | Student no. |
|---|---|---|---|
| Student | user123@gmail.com | student | TEST001 |
| Staff | admin@gmail.com | staff | STAFF001 |

Passwords are with Dylan. The staff password was changed from the one
requested because Supabase requires 6+ characters and the app's login 8+.
The staff account was created directly in the database because sign-up hit
Supabase's email rate limit — **Confirm email was still on** at the time.

### Verified

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL**
- `./gradlew testDebugUnitTest` — **76 tests, 0 failures**

---

## Decisions settled

See `magwinya Notes/04 Build phases/Open decisions.md` for the reasoning.
`CLAUDE.md` section 12 has been rewritten to match.

| | |
|---|---|
| Package | `com.rimagwinya.app` |
| min SDK | 24 |
| SSO | **Google.** Microsoft/Azure dropped |
| Email domain | **None.** Students use ordinary Gmail accounts |
| Identity | The **student number**, required and `unique` |
| Loyalty | **Progress only.** No redemption |
| Pay at counter | Requires **one prior wallet top-up**, plus the existing two-no-show block |

### Why the counter rule exists

Sign-up is open — any email, and nothing verifies a typed student number. A
throwaway account could book stock and slot capacity having paid nothing.
Requiring one prior top-up means staff have physically seen that person at
the speed point. `CLAUDE.md` section 12.1.

---

## Dylan still owes

- **Now:** press Run once, confirm the app opens on the phone.
- **Now:** create the GitHub repo `ri-magwinya` and add the remote. The local
  repo is committed and ready.
- **Now:** launcher icon via *File → New → Image Asset*.
- **Now:** rotate the `sb_secret_…` key (it was pasted into a chat).
- **Now:** open the app on the phone and confirm the real menu loads.
- **Now:** check *Confirm email* is really off — sign-ups on 2026-09-22 still
  tried to send confirmation emails.
- **Phase 2:** turn off Confirm email under *Authentication → Sign In /
  Providers → Email*.
- **Phase 3:** make a staff account and promote it (SQL in
  `supabase/README.md`).
- Later phases: Firebase (11), Google Cloud OAuth (12), a first-language
  review of the Afrikaans and Sesotho (13), GitHub secrets (15), the upload
  keystore (16).

Full list: `magwinya Notes/04 Build phases/What Dylan has to do himself.md`
(not written yet).

---

## Documentation status

The Obsidian vault has 28 notes, ~17,800 words.

| Batch | What | Status |
|---|---|---|
| 1 | Hub, the app, the two roles, both day walk-throughs, the rules, the build plan, open decisions | **Done** |
| 2 | All 20 feature notes | **Done** |
| 3 | Under the hood — stack, architecture, Supabase, tables, pricing, security, design system, testing | Not started |
| 4 | One note per build phase (0–16) | Not started |

---

## Notes for a fresh session

- The build is green. Keep it that way — every phase ends compiling and
  passing tests.
- No user-facing strings in code. `strings.xml` from Phase 1, or Phase 13
  becomes a refactor.
- Money is integer cents in the domain layer. Never `Double`.
- `local.properties` holds the Supabase keys from Phase 2 and is gitignored.
