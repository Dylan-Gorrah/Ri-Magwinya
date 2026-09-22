# Ri-magwinya

**Order ahead. Skip the queue.**

Ri-magwinya is an Android app for the tuckshop at Rosebank College,
Bloemfontein. Students order from their phone between lectures, pay from a
campus wallet, and collect at break with a four-digit code. Staff work a live
queue instead of guessing what is coming.

Built for **OPSC6312 Part 2** by **Dylan Gorrah (ST10398445)**.

| | |
|---|---|
| **Try it in a browser** | https://dylan-gorrah.github.io/Ri-Magwinya/ |
| **Download the APK** | [Releases](https://github.com/Dylan-Gorrah/Ri-Magwinya/releases) → the `.apk` under Assets |
| **Demo video** | _(paste the unlisted YouTube link here)_ |
| **Test accounts** | Student `user123@gmail.com` / `FrogybyD1` · Staff `admin@gmail.com` / `Admin1234!` |

---

## The problem it solves

At break, the queue is the whole break. By the time you reach the counter the
vetkoek is finished, and the person behind the counter has no idea how many
people are still coming. So:

- **Students** see what is actually in stock, build their order the way they
  eat it, pay from a wallet, and collect with a code. No queue, no
  disappointment.
- **Staff** see each order the moment it is placed, with the full order
  written out, and move it along with one tap. They can also see what is
  selling and top up a student's wallet after a card payment.

## Who it is for

Two kinds of people share one app, and **the account decides which half you
get** — not a button on the welcome screen.

- **Students** — menu, cart, checkout, order tracking, profile.
- **Tuckshop staff** — order queue, stock, sales, wallet top-ups.

---

## What is in it

### For students
- **Live stock menu.** Sold-out items cannot be ordered, and stock updates on
  your screen while you are looking at it.
- **Build your order.** Choose how many vetkoeks, then the fillings; pick a
  chips size; choose tea brand, milk and sugar; mix your own sweets. Fillings
  on their own are allowed and use no vetkoek.
- **Collection times.** Pick a break, see how full it is, and get a four-digit
  code for the counter.
- **Campus wallet.** Money comes off when you order. Not enough, and it tells
  you exactly how short you are.
- **Track the order** from placed → preparing → ready → collected, live, and
  cancel free until the kitchen starts.
- **Works offline.** The menu still opens with no signal, and an order placed
  in a dead spot is sent the moment you are back.

### For staff
- **Order queue** with counters, filters, the full order as the student built
  it, the code and the student's name. One button per order.
- **Stock** with low-stock warnings, and add, edit or remove an item.
- **Sales** — revenue, orders by hour as a chart, top sellers, CSV export.
- **Wallet top-ups** after a student pays on the tuckshop card machine.

### Both
Profile and settings, dark mode, English/Afrikaans/Sesotho, a POPIA privacy
page, and a weather-aware menu that puts hot food first on a cold or rainy
day.

---

## The three features marked for Part 2

The rubric asks for three of our own features. These are the three:

| # | Feature | Where to look |
|---|---|---|
| 1 | **Live stock menu with server-priced ordering** | `feature/menu/`, `domain/pricing/PriceCalculator.kt`, `supabase/migrations/0005_place_order.sql` |
| 2 | **Campus wallet** | `feature/cart/CheckoutViewModel.kt`, `feature/staff/TopUpViewModel.kt`, `supabase/migrations/0007_wallet_and_sales.sql` |
| 3 | **Staff order queue** | `feature/staff/QueueViewModel.kt`, `supabase/migrations/0006_set_order_status.sql` |

Each one changes the database visibly, handles its own failures (sold out,
not enough money, an illegal status jump), and has unit tests.

---

## Tech stack

| Part | What we used |
|---|---|
| Language | **Kotlin** |
| UI | **Jetpack Compose** with Material 3, no XML layouts |
| Architecture | **MVVM** with a repository layer and one-way data flow |
| Dependency injection | **Hilt** (through KSP) |
| Async | **Coroutines**, `Flow`, `StateFlow` |
| Navigation | **Navigation Compose**, type-safe routes |
| REST client | **Retrofit** + **OkHttp** + **kotlinx.serialization** |
| Backend (SDK) | **Supabase** — Auth, PostgreSQL, PostgREST, Edge Functions |
| Auth and realtime | **supabase-kt** (Auth and Realtime modules) |
| Local storage | **Room** (cache and offline queue), **DataStore** (settings) |
| Background work | **WorkManager** |
| Charts | **Vico** |
| Push (PoE) | **Firebase Cloud Messaging** |
| Sign-in (PoE) | **Credential Manager** with Google ID |
| Biometrics | **androidx.biometric** |
| Weather | **Open-Meteo** (free, no key) |
| Tests | **JUnit**, **MockK**, **Turbine**, **MockWebServer**, coroutines-test |
| CI | **GitHub Actions** |
| Min / target SDK | 24 (Android 7) / 36 |

**At least one external library:** Retrofit (plus OkHttp, Vico, MockK and the
rest above). **At least one SDK:** Supabase.

---

## How to run it

### The quickest look
Open **https://dylan-gorrah.github.io/Ri-Magwinya/** on any phone, or install
the APK from [Releases](https://github.com/Dylan-Gorrah/Ri-Magwinya/releases).
Both talk to the same live database as the Android app.

### Build it yourself

You need **Android Studio** (Ladybug or newer) and **JDK 17**.

1. Clone it:
   ```
   git clone https://github.com/Dylan-Gorrah/Ri-Magwinya.git
   ```
2. Create `local.properties` in the project root (it is gitignored, so it is
   not in the repo) and put the Supabase details in it:
   ```properties
   sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
   SUPABASE_URL=https://jykswltsegssjmvnqqbi.supabase.co
   SUPABASE_ANON_KEY=<the publishable key from Supabase → Project Settings → API keys>
   ```
   **Without these the app still builds and runs** — it just shows "Backend
   not configured" instead of the menu. Nothing is hardcoded in Kotlin.
3. Plug in a phone with USB debugging on, and press **Run**.
   ```
   ./gradlew assembleDebug       # or build it from the command line
   ./gradlew testDebugUnitTest   # 163 unit tests
   ./gradlew lintDebug           # clean
   ```

Sign in with one of the test accounts above, or register your own — any email
works, there is no college domain to match.

---

## The backend

Everything lives in one hosted **Supabase** project. Nothing is faked and
there are no hardcoded lists on real screens.

### Tables
`profiles` · `menu_items` · `option_groups` · `options` · `collection_slots` ·
`orders` · `order_items` · `stock_movements` · `wallet_transactions` ·
`notifications` · `loyalty_stamps`

### Row Level Security is on for every table
- A student sees **their own** orders; staff see all of them.
- Anyone signed in can read the menu; only staff can change it.
- **Nothing writes orders, stock or balances directly.** There are no insert
  or update policies on `orders`, `order_items` or `wallet_transactions` at
  all — the only way in is through the database functions below.
- Column grants, not just row policies, are what stop a student editing their
  own `role` or `wallet_balance`.

### The API
Most reads are **PostgREST** endpoints (`/rest/v1/menu_items`, `/rest/v1/orders`
and so on). The parts that must not go wrong are ours:

| We wrote | What it does |
|---|---|
| `place_order()` | One transaction: locks the rows, **recomputes every price from the database**, checks stock, the slot and the wallet, makes a unique code, writes the order, deducts stock and money. Sending the same `client_ref` twice returns the same order rather than charging twice. |
| `set_order_status()` | Owns the transition table, so nobody can skip a step or un-collect an order. Restores stock and refunds on a cancel. |
| `load_wallet()` | Staff only. Writes the ledger and the balance together. |
| `adjust_stock()` | Stock as a **change**, never a total, so a sale made mid-tap cannot be wiped out. |
| `sales_summary()` | The whole sales screen, aggregated in SQL rather than by downloading a year of orders. |

Three **Edge Functions** (`place-order`, `order-status`, `load-wallet`) wrap
those: verify the caller, check the body, map failures to clear codes like
`OUT_OF_STOCK`, `SLOT_FULL`, `INSUFFICIENT_FUNDS`.

All the SQL is in `supabase/migrations/`, numbered and run in order.
`supabase/README.md` explains how to apply them.

### Keys
The app uses the **publishable** key only, read from `local.properties` through
`BuildConfig`. The **secret key is not in the app or the repo** — it belongs in
Edge Function secrets. Row Level Security is what protects the data, which is
why the publishable key is safe in a client.

---

## Tests

**163 unit tests**, plain JUnit in `app/src/test`, no emulator needed.

| What they cover | Roughly |
|---|---|
| Pricing — every check value in the brief | 21 |
| Checkout rules: slots, payment, refusals, what goes on the wire | 18 |
| Cart, over a fake database | 11 |
| Weather thresholds, codes, cache | 13 |
| Staff: queue steps, item editor, sales, CSV | 13 |
| Edge Function contract, with MockWebServer | 11 |
| Validation: email, password, student number, phone | 20 |
| Order progress and mapping | 8 |
| Money | 8 |
| Offline queue | 7 |
| Live stock updates | 7 |
| Menu screen state, with Turbine | 7 |
| Profile, settings, register form | 13 |

```
./gradlew testDebugUnitTest
```

The database has its own tests too: `supabase/tests/run.sh` applies every
migration to a throwaway Postgres and asserts 35 things about the rules.

---

## GitHub Actions

`.github/workflows/build.yml` runs on every push and pull request to `main`:

checkout → JDK 17 → `chmod +x gradlew` → **unit tests** → **`assembleDebug`** →
upload the APK as an artifact.

It does **not** need `local.properties`. The build reads the keys from that
file if it exists, then from environment variables, then falls back to empty —
so CI never fails just because the file is missing.

**Secrets to add** (repo → Settings → Secrets and variables → Actions), both
optional:

| Secret | Why |
|---|---|
| `SUPABASE_URL` | So CI builds with the same backend as the phone |
| `SUPABASE_ANON_KEY` | Same |

Pushing a tag like `v1.0.0` also publishes the APK to **Releases**.

---

## How the code is laid out

```
app/src/main/java/com/rimagwinya/app/
├── core/          theme and components, money, network, database, settings
├── data/          remote (Retrofit + DTOs), repository, sync
├── domain/        model, pricing
├── feature/       auth, menu, cart, orders, staff, profile, notifications
└── navigation/    the routes and both graphs
supabase/
├── migrations/    every table, policy and function, numbered
├── functions/     the three Edge Functions
└── tests/         SQL tests against a throwaway database
docs/              the web build, the prototype, progress notes, QA checklist
```

Rules the code sticks to: **no business logic in composables**, **money is
integer cents** (never a Double), and **every user-facing word is in
`strings.xml`** so the three languages are a translation job rather than a
rewrite.

---

## Not part of Part 2

These are built but are PoE features, worth nothing here, and they are switched
off unless configured: Google sign-in, push notifications, offline sync, and
the Afrikaans and Sesotho translations. None of them can break the app while
they are off.

---

## References

Written in IIE Harvard style, and repeated in the code next to whatever they
informed.

- Google. 2026. *Android developers guide.* [Online]. Available at:
  <https://developer.android.com/guide> [Accessed 22 September 2026].
- Google. 2026. *Jetpack Compose documentation.* [Online]. Available at:
  <https://developer.android.com/develop/ui/compose/documentation>
  [Accessed 22 September 2026].
- Supabase. 2026. *Supabase documentation.* [Online]. Available at:
  <https://supabase.com/docs> [Accessed 22 September 2026].
- Supabase. 2026. *Row Level Security.* [Online]. Available at:
  <https://supabase.com/docs/guides/database/postgres/row-level-security>
  [Accessed 22 September 2026].
- Square. 2026. *Retrofit.* [Online]. Available at:
  <https://square.github.io/retrofit/> [Accessed 22 September 2026].
- Patryk Goworowski. 2026. *Vico charts.* [Online]. Available at:
  <https://patrykandpatrick.com/vico/wiki/> [Accessed 22 September 2026].
- Open-Meteo. 2026. *Weather forecast API.* [Online]. Available at:
  <https://open-meteo.com/en/docs> [Accessed 22 September 2026].
- GitHub. 2026. *Building and testing Java with Gradle.* [Online]. Available
  at: <https://docs.github.com/en/actions> [Accessed 22 September 2026].
- AI assistance: Anthropic. 2026. *Claude* [Large language model]. Available
  at: <https://claude.ai> [Accessed 22 September 2026]. Used to: build the app
  from the Part 1 design, write the SQL migrations and Edge Functions, write
  the unit tests, and draft this documentation.

## Documentation in this repo

| File | What is in it |
|---|---|
| `docs/PROGRESS.md` | Every phase, what was built, and every decision with its reasoning |
| `docs/qa-checklist.md` | The walkthrough to do on a phone before submitting |
| `docs/privacy-policy.md` | The POPIA notice, also shown inside the app |
| `docs/store-listing.md` | Play Store listing draft |
| `VIDEO_PLAN.md` | What to cover in the demo video, in order |
| `supabase/README.md` | Applying the migrations, and curl examples for the API |
