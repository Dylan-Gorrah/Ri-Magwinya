# Ri-magwinya — App Overview



## What the app is

Ri-magwinya is an Android app for pre-ordering food from the campus tuckshop at Rosebank College, Bloemfontein. Its tagline is "Order ahead. Skip the queue." Students order and pay before break, then collect with a 4-digit code when the food is ready. Tuckshop staff use the same app to run the order queue, manage stock, top up wallets and see sales.

Built for OPSC6312 Part 2. There is also a browser version at [dylan-gorrah.github.io/Ri-Magwinya](https://dylan-gorrah.github.io/Ri-Magwinya/).

## Who it is for and the problem it solves

At break, the queue eats the whole break. By the time a student reaches the counter, the vetkoek is often finished, and staff have no idea how many people are still coming.

Two kinds of people share one app:

- **Students** see what is really in stock, order the way they eat it, pay from a wallet, and skip the queue.
- **Tuckshop staff** see each order the moment it is placed and move it along with one tap. They also manage stock, top up wallets and see what is selling.

The account decides which half of the app you get. The role is stored on the server, not chosen by a button on the welcome screen.

## What it does

### For students

- **Live menu.** Stock updates on screen in real time. Sold-out items cannot be ordered.
- **Build your order.** Choose how many vetkoeks and which fillings, pick a chips size, set tea brand, milk and sugar, or mix your own sweets.
- **Collection slots.** Pick a break (First break, Second break or Afternoon) and see how full it is, e.g. "12 of 40 taken".
- **Pay** from the campus wallet, or pay at the counter if you qualify.
- **Track the order** live: placed, preparing, ready, collected. Cancel for free until the kitchen starts.
- **4-digit code** to show at the counter.
- **Order history** and a loyalty card ("7 of 10"), display only.

### For staff

- **Order queue** with counters, filters, the code, the student's name and the full order. One button moves each order to its next step.
- **Stock** screen with low-stock warnings, plus add, edit or remove an item.
- **Sales** screen: revenue, order count, average order, orders by hour as a bar chart, top sellers and CSV export.
- **Wallet top-up:** find a student by number or name, enter the amount, confirm.

### For both

- Sign in with email and password, or with Google.
- Profile and settings: theme (system, light, dark), language, notifications, fingerprint unlock, change password, sign out.
- Three languages: English, Afrikaans and Sesotho.
- Push notifications when an order is ready.
- Works offline: the menu still opens with no signal, and an order placed offline is sent when the phone reconnects.
- Weather-aware menu: hot food first on a cold or rainy day.

## How an order works

1. The student builds the order, picks a collection slot and chooses wallet or counter.
2. The app sends it to the `place-order` Edge Function. The server checks prices, stock, slot space and balance, then takes the money and stock in one go.
3. The student gets an order number and a 4-digit code.
4. Staff see it appear in the queue live and move it along. The student's screen updates live too.
5. At the counter, staff check the code and mark it collected.

Staff move an order forward. A student can only cancel, and only at the start:

```
Placed ──► Preparing ──► Ready ──► Collected   (+1 loyalty stamp)
  │                        │
  ▼ student,               ▼ staff, after
    before preparing         the slot ends
Cancelled                No-show
(refund + stock back)    (+1 no-show on the account)
```

When staff mark it ready, the student gets a push notification. Staff check the code before marking it collected.

## Tech used

The app is native Android, written in Kotlin. The backend is one hosted Supabase project.

| Part | What I used | Why, in one line |
| --- | --- | --- |
| Language | Kotlin | Google's recommended language for Android |
| Screens | Jetpack Compose, Material 3 | Modern Android UI in code, no XML layouts |
| Structure | MVVM + repositories | Screens only show state; logic lives in ViewModels and repositories |
| Dependency injection | Hilt (via KSP) | Wires classes together and makes them easy to test |
| Async | Coroutines, Flow, StateFlow | Network and database work without freezing the screen |
| Navigation | Navigation Compose, type-safe routes | Separate student, staff and sign-in sections |
| API calls | Retrofit + OkHttp + kotlinx.serialization | Every endpoint is a clear, testable Kotlin interface |
| Backend | Supabase: PostgreSQL, Auth, PostgREST, Edge Functions | Real database, sign-in and server code without running my own server |
| Sign-in and live updates | supabase-kt (Auth and Realtime) | Keeps the session and pushes order and stock changes live |
| Server code | Edge Functions (TypeScript) + PL/pgSQL functions | Ordering, status changes and top-ups run on the server |
| Offline | Room (cache + queue), DataStore (settings), WorkManager (sync) | The app works with no signal and catches up later |
| Push | Firebase Cloud Messaging | "Your order is ready" notifications |
| Google sign-in | Credential Manager + Google ID | One-tap sign-in |
| Fingerprint | androidx.biometric | Optional lock when the app opens |
| Charts | Vico | Orders-by-hour chart on the sales screen |
| Weather | Open-Meteo API | Free and needs no key |
| Tests | JUnit, MockK, Turbine, MockWebServer | 172 unit tests, no emulator needed |
| CI | GitHub Actions | Runs the tests and builds the APK on every push |

The app runs on Android 7 (API 24) and up.

Rubric checks: Retrofit is the external library and Supabase is the SDK.

## Security and privacy

The main idea: the phone is never trusted. The server checks everything that involves money, stock or roles.

- **Row Level Security on every table.** A student can only see their own orders and profile. Staff can see everything. The database enforces this, not the app.
- **No direct writes to orders, stock or money.** Orders, status changes and top-ups only go through server functions.
- **Prices are worked out on the server.** The total shown in the app is for display. The server recalculates it from the database, so a hacked app cannot pay R1 for a R30 meal.
- **Roles come from the server.** A student cannot make themselves staff. Column permissions block editing your own role or wallet balance.
- **No double charges.** Every order carries a unique `client_ref`. Sending the same order twice returns the first order.
- **Keys.** The app only has the public (publishable) key. The secret key lives only in Edge Function secrets.
- **Passwords** are hashed by Supabase Auth with bcrypt. The app never stores them.
- **No card data.** Top-ups happen on the tuckshop's card machine. Staff type the amount into the app. The two systems are separate.
- **POPIA.** A privacy page in the app explains what data is collected and why. Only the minimum is stored: name, email, student number, orders.

## Key business rules

**Pricing.** Built items are priced by what goes in them. Money is stored as whole cents, never decimals, so there are no rounding errors. The same formula runs in the app (`PriceCalculator`) and on the server (`place_order`).

| Example | Price |
| --- | --- |
| 2 vetkoeks + 2 polony + 1 cheese | R17.00 |
| 0 vetkoeks + 1 snoek (fillings only) | R10.00 |
| 3 Chappies + 1 mix packet | R13.00 |
| Large chips | R40.00 |
| Tea, any brand, milk or sugar | R10.00 |

**Stock.** Each vetkoek uses one from stock. An order of fillings only uses no vetkoek stock.

**Collection slots.** There are three slots a day: First break 09:40 (40 orders), Second break 11:00 (40) and Afternoon 14:30 (25). A full slot cannot be booked.

**Wallet.** Money comes off when the order is placed. Staff can top up from R10 to R1000, with quick buttons for R20, R50, R100 and R200.

**Pay at the counter.** This is a backup option. It needs at least one past wallet top-up, so staff have seen you in person. It is blocked after 2 no-shows.

**Order steps.** Placed, then preparing, then ready, then collected. Only staff move an order forward. A student can only cancel while it is still "placed". Cancelling refunds the wallet and puts the stock back.

**Weather.** Below 15°C, or with a 50% or higher chance of rain, hot items sort first. At 27°C or higher, cold drinks sort first. If the weather call fails, the menu still loads.

## Questions a lecturer may ask

**Why MVVM?** It keeps screens simple. The screen only shows a `UiState` from the ViewModel. The ViewModel holds the logic, and repositories fetch the data. Each part can be tested on its own.

**Where is the data stored?** In a hosted PostgreSQL database on Supabase. The phone also keeps a Room cache, so the app works offline.

**What is the external library and the SDK?** Retrofit is the main external library, used for every REST call. Supabase is the SDK.

**How do you stop someone cheating on price?** The server recalculates every price from the database inside one transaction. The app's total is display only.

**How do you stop a student becoming staff?** The role is a database column the student cannot edit, protected by Row Level Security and column permissions. The app reads the role from the server after sign-in.

**What happens if two people order the last item at the same time?** `place_order` locks the item row (`FOR UPDATE`). The first order gets it and the second gets an `OUT_OF_STOCK` error with a clear message.

**What if the network drops while placing an order?** The order is saved in a queue on the phone. WorkManager sends it when the signal comes back. The `client_ref` makes sure it is never charged twice.

**How do live updates work?** Supabase Realtime. The app subscribes to the orders and stock tables, and changes appear without a refresh.

**How is money handled?** As whole cents (integers), never decimals, so there are no rounding errors.

**What are your three own features?** (1) The live stock menu with server-priced ordering. (2) The campus wallet. (3) The staff order queue.

**How did you test it?** 172 unit tests with JUnit, MockK and Turbine, covering pricing, validation, checkout, cart, weather, offline sync and screen state. GitHub Actions runs them on every push.

**How does sign-in work?** Supabase Auth with email and password, or Google through Credential Manager. Supabase hashes passwords with bcrypt. The session is saved, so you stay signed in.

**What is POPIA and how does the app follow it?** POPIA is South Africa's data-protection law. The app collects only what it needs, explains it on a privacy page, and never handles card details.

## Known limits and what I would do next

Being upfront about these usually scores better than hiding them.

- **Student numbers are not verified.** Anyone can type any number, first come first served. A fix would need a link to the college's student system.
- **Pay at the counter is a risk** because accounts are free. It is limited by the top-up rule and the 2 no-show cap.
- **Loyalty is display only.** Stamps are counted, but there is no free item yet. The database already has a `redeemed` column ready for it.
- **The free Supabase plan pauses** after about a week of no use. It needs a paid plan for real use.
- **Opening the app with no signal at all** sends you to sign-in, because the profile is not saved on the phone yet. Once you are signed in, offline mode works.
- **Afrikaans and Sesotho** were machine translated, so they need a first-language speaker to check them.
