# Ri-magwinya — Android build brief

> **Dylan:** save this file as `CLAUDE.md` in the root of the Android Studio project. Claude Code loads it automatically at the start of every session, so you never have to paste it again. Check section 12 before you start Phase 0.

---

## 1. How we work

You are the lead developer. Dylan is the human in the loop. He creates the empty project, you build the app phase by phase, and he handles anything that needs an account, a secret, a dashboard click, or a physical device.

**Rules**

1. **Session start.** Read this file and `docs/PROGRESS.md`. Tell Dylan which phase we are on and what comes next in three lines or fewer. Then wait for "go".
2. **One phase at a time.** Finish it, report, stop. Never start the next phase without Dylan saying go.
3. **Short reports.** Dylan wants plain English and short updates. Use the template in section 11. No essays.
4. **Human checkpoints.** When a step needs Dylan, stop and say exactly what to do, where to click, and what to paste back. Marked in this file as **HUMAN CHECKPOINT**.
5. **Never invent secrets.** No made-up URLs, keys, project IDs or client IDs. Ask.
6. **Never commit secrets.** `local.properties`, `google-services.json`, keystores and service account files stay out of git.
7. **The build stays green.** Every phase ends with a project that compiles, runs on a device, and passes its tests. If you break it, fix it before reporting.
8. **Commit per phase.** `git commit -m "phase-N: <summary>"` at the end of each phase.
9. **Keep `docs/PROGRESS.md` current.** After every phase: phase status, decisions made, anything Dylan still owes, what's next. This is how a fresh session picks up where the last one stopped.
10. **Disagree out loud.** If something in this brief is wrong, outdated or impossible, say so and propose an alternative. Do not quietly deviate.
11. **No UI strings in code.** Every user-facing string goes in `strings.xml` from Phase 1. This makes Phase 13 a translation job, not a refactor.
12. **Latest stable versions.** Use a Gradle version catalog (`libs.versions.toml`). Check each dependency's current stable version before adding it rather than trusting memory.

---

## 2. The app

Ri-magwinya is a campus tuckshop pre-ordering app for Rosebank College, Bloemfontein. Students browse a live menu, build their order, pick a collection slot, pay from a campus wallet, and collect with a 4-digit code when notified. Tuckshop staff work a live order queue, manage stock, top up wallets and view sales.

Two roles share one app. The role lives on the account in the database and decides which interface loads after sign-in.

---

## 3. Where the truth lives

| Source | Use it for |
|---|---|
| `docs/rimagwinya-prototype.html` | **Visual and behavioural source of truth.** Every screen, layout, copy line, state and interaction. Open it and study it before Phase 1. Translate it to idiomatic Compose and Material 3; do not port HTML literally. |
| This file | Stack, architecture, backend, business rules, phases. Where this file and the prototype disagree, **this file wins** (see 3.1). |

### 3.1 Where this brief overrides the prototype

The prototype is a front-end demo with fake data. The real app differs in these ways:

- **Role comes from the server**, never from which card was tapped on the welcome screen.
- **Prices are computed on the server.** Client-side totals are display only.
- **Fillings-only vetkoek orders** consume zero vetkoek stock (the prototype used a minimum of 1).
- **"Collected" is set by staff only**, after checking the code. Remove the student's "I have collected it" button.
- **Pay at the counter** is a second payment option, with a no-show rule (section 9).
- **Staff can top up wallets** — new screen, not in the prototype.
- **Weather strip** on the menu and forecast card on sales — new, not in the prototype.
- **No demo controls.** No "Switch role", no "Reset", no pre-filled credentials.

---

## 4. Tech stack (locked)

| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3. No XML layouts |
| Architecture | MVVM + repository layer, unidirectional data flow |
| DI | Hilt, via **KSP** (not kapt) |
| Async | Coroutines, `Flow`, `StateFlow` in ViewModels |
| Navigation | Navigation Compose with type-safe routes |
| REST client | Retrofit + OkHttp + kotlinx.serialization — used for **all** PostgREST, Edge Function and weather calls, so every endpoint is an explicit, testable interface |
| Auth + realtime | `supabase-kt` Auth and Realtime modules only |
| Database | Supabase (PostgreSQL) with Row Level Security |
| Server logic | Supabase Edge Functions (TypeScript) calling PL/pgSQL functions |
| Local storage | Room (cache + offline queue), DataStore (preferences) |
| Background sync | WorkManager |
| Push | Firebase Cloud Messaging |
| SSO | Credential Manager (`androidx.credentials` + Google ID) → Supabase ID-token sign-in |
| Biometrics | `androidx.biometric` |
| Images | Coil |
| Charts | Vico |
| Weather | Open-Meteo Forecast API (free, no key) |
| Tests | JUnit, MockK, Turbine, kotlinx-coroutines-test |
| CI | GitHub Actions |
| Min SDK | 24 |

---

## 5. Design system

Premium, calm, minimal, iOS-inspired. Typography and spacing do the work. Neutral surfaces, one action colour, colour only where it means something. No emojis anywhere in the UI. No gradients.

### 5.1 Colour

| Token | Light | Dark | Use |
|---|---|---|---|
| background | `#F2F5F9` | `#0B1220` | Silver white app background |
| surface | `#FFFFFF` | `#141C2B` | Cards, lists, sheets |
| surfaceVariant | `#E9EEF5` | `#1E2739` | Pressed rows, disabled fills |
| text | `#0F1B2E` | `#EEF2F7` | Primary text |
| text2 | `#5A6779` | `#95A2B4` | Secondary text |
| text3 | `#93A0B0` | `#68758A` | Captions, placeholders |
| border | `#0F1B2E` @ 8% | `#FFFFFF` @ 9% | Hairlines |
| borderStrong | `#0F1B2E` @ 16% | `#FFFFFF` @ 18% | Secondary button outline |
| **navy** (primary) | `#16305B` | `#2F5C9E` | **The only action colour.** Primary buttons, active tab, selected chip |
| navyDeep | `#0D1F3D` | `#1B3A6B` | Menu hero header |
| sky | `#3E7FBF` | `#7FB3E0` | Information: status pills, links, focus rings |
| skyWash | `#E8F1FA` | sky @ 13% | Pill and thumbnail backgrounds |
| gold | `#B8892C` | `#D9AC55` | **Sparingly:** staff badge, loyalty, top seller. Nothing else |
| goldWash | `#FAF2E3` | gold @ 13% | |
| dough | `#E9A84F` | same | Brand mark fill only |
| success | `#2A7050` | `#5FBD8C` | In stock, ready, collected |
| warning | `#9A6B12` | `#D9AC55` | Low stock, new order |
| error | `#A33A2E` | `#E08476` | Sold out, errors, destructive |

Status is **never** shown by colour alone. Every pill carries a text label.

### 5.2 Type

Inter, bundled in `res/font` (Regular 400, Medium 500, SemiBold 600). If you cannot download the font files, **HUMAN CHECKPOINT:** ask Dylan to drop them in.

| Role | Size | Weight | Notes |
|---|---|---|---|
| Large title | 27sp | 600 | letter spacing −0.03em |
| Screen title | 20sp | 600 | −0.025em |
| Body strong | 15sp | 600 | Row titles |
| Body | 14sp | 400 | |
| Secondary | 13.5sp | 400 | text2 |
| Caption | 11.5sp | 400 | text3 |
| Label | 10.5sp | 600 | Uppercase, +0.12em, text3 |

Prices use tabular figures.

### 5.3 Space, shape, motion

- **Spacing:** 4, 8, 12, 16, 20, 24, 32, 40, 48, 64 dp only. Screen padding 20dp.
- **Radius:** 8, 12, 16, 20dp. Buttons 12. Sheets 26 on the top corners. Chips 10.
- **Touch targets:** 48dp minimum.
- **Depth:** hairline borders first, shadows rarely and only soft. If everything is elevated, nothing is.
- **Motion:** fast 180ms, normal 300ms, slow 460ms. Smooth easing `CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)`. Spring for sheets, toggles, button press and selection indicators. Press scale 0.96–0.975.
- **Reduced motion:** respect the system animator scale.

### 5.4 Components to build once and reuse

`RmButton` (primary, secondary, quiet, danger, small) · `RmTextField` (with error state) · `StockPill` · `StatusPill` · `GroupedList` + `ListRow` (one container, hairline dividers, no per-row shadow) · `Stepper` · `ChipRow` · `ToggleRow` · `Banner` (info, warning, error, success) · `RmBottomSheet` (grab handle, scrim) · `Hud` (styled snackbar, one message at a time) · `ProgressRail` · `CodeTiles` · `RoleTabBar` · `SkeletonRow` · `EmptyState` · `BrandMark` · `FoodIcon`

### 5.5 Brand assets

- **Brand mark:** convert the `<symbol id="brand">` SVG in the prototype into a vector drawable `ic_brand_mark.xml` — vetkoek face, navy ring, split sky and gold outer arc.
- **Food icons:** convert each `f-*` symbol into `ic_food_<name>.xml`.
- **Launcher icon:** **HUMAN CHECKPOINT** — Dylan runs *File → New → Image Asset* with his icon PNG to generate the adaptive launcher icons.

---

## 6. Architecture

```
com.rimagwinya.app
├── RimagwinyaApp.kt            @HiltAndroidApp
├── MainActivity.kt             AppCompatActivity (needed for per-app language, see 10.13)
├── core/
│   ├── config/                 AppConfig: coordinates, top-up limits, slot capacity
│   ├── designsystem/           theme, tokens, components
│   ├── money/                  Money (integer cents) + formatter "R12.50"
│   ├── network/                Retrofit, OkHttp, auth interceptor, error mapping
│   ├── database/               Room database, DAOs, entities
│   ├── datastore/              preferences
│   ├── connectivity/           network observer
│   └── util/
├── data/
│   ├── remote/                 Retrofit services + DTOs
│   ├── repository/             Auth, Menu, Cart, Order, Staff, Wallet, Weather, Settings
│   └── sync/                   SyncWorker, pending action queue
├── domain/
│   ├── model/                  MenuItem, OptionGroup, CartLine, Order, Slot, Weather…
│   └── pricing/                PriceCalculator — pure Kotlin, heavily unit tested
├── feature/
│   ├── auth/                   welcome, login, register
│   ├── menu/                   menu screen, item sheet
│   ├── cart/                   cart, checkout
│   ├── orders/                 order status, history
│   ├── staff/                  queue, stock, sales, wallet top-up, item editor
│   └── profile/                profile, settings, notifications sheet
├── navigation/                 root host, auth graph, student graph, staff graph
└── notifications/              FCM service, channels, deep links
```

- ViewModels expose one `UiState` per screen via `StateFlow`; one-off events through a `Channel`.
- Every screen handles **loading, empty, error and success.**
- No business logic in composables.
- **Money is integer cents** in the domain layer. Never `Double`.
- Use `java.time` with core library desugaring enabled (min SDK 24).

---

## 7. Backend (Supabase)

All SQL lives in numbered migration files under `supabase/migrations/`. All Edge Functions live under `supabase/functions/`.

### 7.1 Tables

**profiles** — one row per auth user
`id uuid PK → auth.users on delete cascade` · `full_name text not null` · `email text unique not null` · `student_number text unique not null` · `role user_role not null default 'student'` (student, staff) · `wallet_balance numeric(10,2) not null default 0 check ≥ 0` · `no_show_count int not null default 0` · `language text not null default 'en' check in (en, af, st)` · `fcm_token text` · `created_at timestamptz default now()` · `last_login timestamptz`

**menu_items**
`id uuid PK` · `slug text unique not null` · `name text not null` · `description text` · `category menu_category not null` (Meals, Snacks, Drinks) · `price numeric(10,2) not null check ≥ 0` · `image_url text` · `icon_key text not null` · `stock_quantity int not null default 0 check ≥ 0` · `reorder_level int not null default 5` · `is_available boolean not null default true` · `temperature_tag text check in (hot, cold)` · `base_step_label text` · `base_step_singular text` · `base_step_min int` · `sort_order int default 0` · `updated_at timestamptz`

**option_groups**
`id uuid PK` · `item_id uuid → menu_items on delete cascade` · `key text` · `label text` · `type text check in (single, qty)` · `replaces_price boolean default false` · `sort_order int`

**options**
`id uuid PK` · `group_id uuid → option_groups on delete cascade` · `key text` · `name text` · `price numeric(10,2) default 0 check ≥ 0` · `is_default boolean default false` · `sort_order int`

**collection_slots**
`id uuid PK` · `name text` · `starts_at time` · `ends_at time check > starts_at` · `capacity int default 40` · `orders_taken int default 0 check ≤ capacity` · `service_date date` · `unique(name, service_date)`

**orders**
`id uuid PK` · `order_number bigint generated always as identity` (the #1047 shown to users) · `student_id uuid → profiles` · `collection_code char(4)` · `slot_id uuid → collection_slots` · `total_amount numeric(10,2) check > 0` · `status order_status default 'placed'` (placed, preparing, ready, collected, cancelled, no_show) · `payment_method payment_method` (wallet, counter) · `client_ref uuid` · `unique(student_id, client_ref)` · `placed_at` · `prepared_at` · `ready_at` · `completed_at` · `cancelled_at`

**order_items**
`id uuid PK` · `order_id uuid → orders on delete cascade` · `item_id uuid → menu_items on delete set null` · `item_name text not null` (snapshot) · `options_label text` (snapshot, e.g. "2 vetkoeks, 2 Polony") · `options jsonb` · `quantity int check > 0` · `unit_price numeric(10,2) not null` (snapshot, server-computed) · `units_consumed int not null` · `subtotal numeric(10,2) generated always as (quantity * unit_price) stored`

**stock_movements** — `id` · `item_id` · `change_amount int` · `reason` (sale, restock, waste, correction, cancel_restore) · `staff_id` · `order_id` · `created_at`

**wallet_transactions** — `id` · `user_id` · `amount numeric(10,2)` (positive top-up or refund, negative payment) · `type` (topup, payment, refund) · `order_id` · `staff_id` · `created_at`

**notifications** — `id` · `user_id` · `title` · `message` · `type` (order_placed, order_ready, low_stock, wallet_topup) · `is_read boolean default false` · `order_id` · `sent_at`

**loyalty_stamps** — `id` · `user_id` · `order_id unique` · `redeemed boolean default false` · `created_at`

### 7.2 Functions and triggers

| Name | Does |
|---|---|
| `is_staff()` | `security definer`, returns whether `auth.uid()` has role staff. Used by RLS |
| `handle_new_user()` | Trigger on `auth.users` insert. Creates the profile with role `student`. No domain check (see section 12); `student_number` uniqueness is enforced by the column constraint |
| `log_stock_change()` | Trigger on `menu_items` stock update. Writes a `stock_movements` row with `auth.uid()` |
| `ensure_slots(date)` | Creates the day's three slots if missing. The app calls it when loading slots |
| `place_order(slot, method, lines jsonb, client_ref)` | **One transaction:** locks item and slot rows `FOR UPDATE`, recomputes every price from the database, checks stock and slot capacity, checks wallet balance or counter eligibility, generates a unique code, writes order and lines, deducts stock and wallet, logs movements, increments the slot. Returns the existing order if `client_ref` was already used |
| `set_order_status(order_id, status)` | Enforces the transitions in section 9, sets timestamps, restores stock and refunds on cancel, adds a loyalty stamp on collected, increments `no_show_count` on no-show |
| `load_wallet(user_id, amount)` | Staff only. Writes the transaction and updates the balance atomically |
| `sales_summary(from, to)` | Staff only. Revenue, order count, average, orders by hour, top items |

### 7.3 Security (RLS on every table)

- **profiles:** users select their own row; staff select all. Users may only update `full_name`, `language`, `fcm_token` — use column privileges. Role, balance and counts change only through functions.
- **menu_items, option_groups, options:** select for `anon` and `authenticated`; insert, update and delete only when `is_staff()`.
- **collection_slots:** select for authenticated; writes only through functions.
- **orders, order_items:** students select their own; staff select all. **No direct insert or update** — functions only.
- **wallet_transactions:** users select their own; staff select all; writes through functions.
- **notifications:** users select their own and update `is_read` only.
- **stock_movements:** staff select.
- Enable Realtime on `orders`, `menu_items`, `collection_slots`.
- The **service role key never goes in the app.** Edge Functions only.

### 7.4 Endpoints

All under `https://<project>.supabase.co`. Every request sends `apikey: <anon key>` and `Authorization: Bearer <user access token>`.

**Generated by PostgREST**

| Method | Path | Notes |
|---|---|---|
| GET | `/rest/v1/menu_items?select=*,option_groups(*,options(*))&order=sort_order` | Menu with nested options |
| POST, PATCH, DELETE | `/rest/v1/menu_items` | Staff. PATCH with `Prefer: return=representation` |
| GET | `/rest/v1/orders?select=*,order_items(*)` | RLS scopes by role |
| GET | `/rest/v1/collection_slots?service_date=eq.<date>` | Call `ensure_slots` first |
| GET | `/rest/v1/notifications` | |
| PATCH | `/rest/v1/profiles?id=eq.<id>` | Name, language, FCM token |
| POST | `/rest/v1/rpc/ensure_slots` · `/rest/v1/rpc/sales_summary` | |

**Written by us as Edge Functions** — thin wrappers: verify the JWT, validate the body, call the SQL function, send FCM, map errors.

| Method | Path | Body | Errors |
|---|---|---|---|
| POST | `/functions/v1/place-order` | `slot_id, payment_method, client_ref, lines[{item_id, base_qty?, quantity?, options[{option_id, count?}]}]` | 409 `OUT_OF_STOCK`, `SLOT_FULL`, `INSUFFICIENT_FUNDS`, `COUNTER_BLOCKED` |
| PATCH | `/functions/v1/order-status` | `order_id, status` | 403 wrong role, 409 `INVALID_TRANSITION` |
| POST | `/functions/v1/load-wallet` | `student_number, amount` | 403, 404 `STUDENT_NOT_FOUND`, 400 `AMOUNT_OUT_OF_RANGE` |

**Third party:** Supabase Auth (`/auth/v1/*`), Google OAuth via Credential Manager, FCM HTTP v1 (from Edge Functions only), Open-Meteo `GET https://api.open-meteo.com/v1/forecast`.

---

## 8. Menu seed

Copy descriptions from `MENU_SEED` in the prototype. Prices in Rands.

| slug | Name | Cat | Price | Stock | Icon | Temp | Options |
|---|---|---|---|---|---|---|---|
| wors | Boerewors roll | Meals | 30 | 22 | wors | hot | — |
| vetkoek | Vetkoek | Meals | 3 | 40 | vetkoek | hot | **Base step** "Vetkoeks" / "vetkoek", min 0, start 1. **qty** "Fillings": Polony 3, Liver spread 4, Salami 4, Cheese slice 5, Achar 5, Snoek 10 |
| chips | Fried chips | Meals | 28 | 30 | chips | hot | **single, replaces price** "Choose a size": Small 28 (default), Medium 35, Large 40 |
| simba | Simba chips | Snacks | 12 | 36 | packet | — | — |
| doritos | Doritos | Snacks | 25 | 18 | triangle | — | — |
| sweets | Assorted sweets | Snacks | 0 | 80 | sweet | — | **qty** "Pick your sweets": Chappies 1, Fizzer 2, Sparkles 3, Lollipop 3, Assorted mix packet 10 |
| tea | Tea | Drinks | 10 | 50 | cup | hot | **single** "Which tea": Five Roses (default), Freshpak Rooibos, Joko, Glen · **single** "Milk": With milk (default), Black · **single** "Sugar": No sugar (default), 1 spoon, 2 spoons, 3 spoons |
| coffee | Coffee | Drinks | 10 | 50 | cup | hot | Milk and Sugar groups as for tea |
| coke | Coca-Cola 440ml | Drinks | 16 | 44 | can | cold | — |
| fanta | Fanta Orange 440ml | Drinks | 16 | 32 | can | cold | — |
| stoney | Stoney Ginger Beer 440ml | Drinks | 16 | 24 | can | cold | — |
| cremesoda | Sparletta Creme Soda 440ml | Drinks | 16 | 20 | can | cold | — |
| twizza | Twizza 500ml | Drinks | 12 | 28 | bottle | cold | — |
| energade | Energade 500ml | Drinks | 18 | 22 | bottle | cold | — |
| switch | Switch Energy 500ml | Drinks | 20 | 16 | energy | cold | — |
| mofaya | MoFaya 500ml | Drinks | 22 | 14 | energy | cold | — |
| score | Score Energy 500ml | Drinks | 18 | 4 | energy | cold | — |

**Slots per day:** First break 09:40–10:00 (capacity 40) · Second break 11:00–11:20 (40) · Afternoon 14:30–14:50 (25)

---

## 9. Business rules

### 9.1 Pricing — implement once in `PriceCalculator`, mirror exactly in `place_order`

```
unit =
    if item has base step:           item.price × base_qty
    else if a group replaces price:  selected option price
    else:                            item.price
  + Σ qty groups:                    option.price × count
  + Σ non-replacing single groups:   selected option price

build item      = has a base step OR any qty group
quantity        = 1 for build items (the steppers ARE the quantity); otherwise the outer stepper
line total      = unit × quantity
units consumed  = base_qty for base-step items (may be 0), otherwise quantity
add to cart     = only when unit > 0
```

Cart lines are keyed by item plus the exact selection, so "Vetkoek · 2 Polony" and "Vetkoek · Snoek" stay separate lines.

**Check values:** 2 vetkoeks + 2 polony + 1 cheese = **R17.00** · 0 vetkoeks + 1 snoek = **R10.00** · 3 Chappies + 1 mix packet = **R13.00** · Large chips = **R40.00** · Tea with any options = **R10.00**

### 9.2 Orders

| From | To | Who | Side effects |
|---|---|---|---|
| placed | preparing | staff | `prepared_at` |
| preparing | ready | staff | `ready_at`, push to student |
| ready | collected | staff | `completed_at`, +1 loyalty stamp |
| ready | no_show | staff | +1 `no_show_count` |
| placed | cancelled | student, own order | refund wallet if paid by wallet, restore stock, free the slot |

- Students cannot cancel once preparation starts.
- Collection code: 4 digits, unique among the day's orders that are not collected, cancelled or no-show.

### 9.3 Payment

- **Campus wallet (default).** Money leaves the wallet when the order is placed.
- **Top-ups happen at the tuckshop speed point.** The student taps their card, staff enter the amount in the app's top-up screen. The speed point and the app are separate systems — state this in the UI copy and the docs.
- Top-up range R10 to R1000. Presets R20, R50, R100, R200 plus custom.
- **Pay at the counter (fallback).** Student pays at the speed point on collection. Requires at least one prior wallet top-up (see 12.1) and is blocked once `no_show_count ≥ 2`; explain either reason on the checkout screen.
- No card data ever touches the app.

### 9.4 Weather

- Open-Meteo, Bloemfontein `latitude=-29.12&longitude=26.21`, `current=temperature_2m,weather_code,precipitation`, `daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max`, `timezone=Africa/Johannesburg`, `forecast_days=2`.
- Cache in Room for **60 minutes.** A failed weather call must never block or delay the menu.
- **Cold** — current temperature below 15°C, or today's rain probability 50% or higher: `hot` items sort first.
- **Hot** — current temperature 27°C or higher: `cold` items sort first.
- Otherwise: default `sort_order`.
- Menu hero shows a slim strip, e.g. "14°C · Light rain · Hot drinks first today".
- Rain 50% or higher: banner "Rain expected. Order ahead so you're not queueing outside."
- Staff sales screen: tomorrow's forecast card with a stock hint, e.g. "Cold tomorrow. Check tea and coffee stock."
- Map WMO weather codes to short labels: clear, partly cloudy, fog, drizzle, rain, showers, thunderstorm.

---

## 10. Phases

Phases 1–9 cover the core app. Phases 10–13 are the features the module marks as **PoE only**. Write unit tests for logic as you go; Phase 15 fills the gaps and adds CI.

### Phase 0 — Project setup · Dylan

1. Android Studio → **File → New → New Project**
2. **Phone and Tablet → Empty Activity** → Next
3. **Name:** `Ri-magwinya` · **Package name:** `com.rimagwinya.app` · **Minimum SDK:** API 24 · **Build configuration language:** Kotlin DSL
4. **Finish** and wait for Gradle sync to complete
5. Plug in your phone with USB debugging on, press **Run**, confirm "Hello Android!" appears. Use the phone, not the emulator — on 8 GB of RAM the emulator will eat your time.
6. In the project root create `docs/` and copy in `rimagwinya-prototype.html`
7. Save this file as `CLAUDE.md` in the project root
8. Create an empty GitHub repo `ri-magwinya`, then in the Android Studio terminal:
   ```
   git init
   git add .
   git commit -m "phase-0: empty project"
   git branch -M main
   git remote add origin https://github.com/Dylan-Gorrah/ri-magwinya.git
   git push -u origin main
   ```
9. Tell Claude: **"Phase 0 done. Start."**

### Phase 1 — Foundation

- Version catalog with the core dependencies from section 4. Hilt and Room through KSP. Core library desugaring on. `buildConfig = true`.
- Package structure from section 6. Create `docs/PROGRESS.md`.
- Theme: light and dark colour schemes, Inter typography, shapes, spacing and motion tokens.
- The reusable components from 5.4, each with a Compose preview.
- Brand mark and food icons converted to vector drawables.
- Navigation: auth graph, student graph (Menu, Cart, Orders, Profile) and staff graph (Queue, Stock, Sales, Profile) with titled placeholder screens.
- Splash screen using the brand mark.
- `.gitignore` covers `local.properties`, `google-services.json`, `*.jks`, `*.keystore`.
- **HUMAN CHECKPOINT:** launcher icon via Image Asset Studio.

**Done when:** the app runs on the phone, every placeholder screen is reachable, dark mode follows the system, component previews render.

### Phase 2 — Backend

- **HUMAN CHECKPOINT first.** Ask Dylan for:
  1. The Supabase **project URL** and **anon key** → you put them in `local.properties` and expose them through `BuildConfig`, defaulting to empty strings so the build never fails without them
  2. Whether the **Supabase MCP connector** is connected — if yes, apply migrations through it; if not, give him each SQL file in order and wait for him to run it in the SQL Editor
  3. To turn off **Confirm email** under *Authentication → Sign In / Providers → Email* for development
  4. If the free tier's two-project limit blocks a new project, he pauses or deletes one first
- Migrations: enums, tables, constraints, indexes, triggers, RLS policies, the functions in 7.2, Realtime publication.
- Seed the menu, options and slot definitions from section 8.
- Retrofit and OkHttp: base URL, `apikey` header, bearer interceptor (anon key until Phase 3), JSON config, typed error mapping, body logging in debug only.
- Smoke test: the menu placeholder lists item names fetched from Supabase.

**Done when:** menu names load from Supabase on the phone, and a request without a token cannot read orders.

### Phase 3 — Authentication

- Welcome (role cards), Login, Register — matching the prototype.
- Live validation: a well-formed email address (no domain restriction — see section 12), password 8+ characters, student number required, all fields required. The button stays disabled until the form is valid. POPIA banner above the button.
- `supabase-kt` Auth for sign-up, sign-in, sign-out, session persistence and refresh. The OkHttp interceptor sends the live access token.
- After sign-in, fetch the profile and route by **server role**.
- States: loading, "Email or password is incorrect", network failure with retry.
- Passwords are hashed by Supabase Auth with bcrypt. Never hash or store them client-side.
- **HUMAN CHECKPOINT:** Dylan registers a student account in the app, creates a staff user in *Authentication → Users*, then runs the SQL you give him to set that user's role to staff.

**Done when:** registering with any valid email lands on the student menu, a duplicate student number is refused with a clear message, staff login lands on the queue, a wrong password shows the error, and killing and reopening the app keeps you signed in.

### Phase 4 — Student menu

- Menu screen: navy hero with greeting, wallet balance and bell; search; category chips; "Today's menu" with a live pill; grouped list with stock pills; sold-out rows dimmed and disabled; "from R…" pricing for build and size items.
- Item sheet with the full option engine: base stepper, qty groups, single groups, outer quantity only for non-build items, live total on the button, disabled with "Choose something first" when the unit price is zero.
- `PriceCalculator` in `domain/pricing` with unit tests covering every item in section 8 and every check value in 9.1.
- `CartRepository` held in memory for now.
- Realtime: stock changes update the menu live.

**Done when:** you can build a vetkoek with fillings, pick a chips size, set tea brand, milk and sugar, and mix sweets — and every total matches 9.1.

### Phase 5 — Edge Functions

- `place-order`, `order-status`, `load-wallet` in TypeScript: verify the JWT, validate the body, call the SQL function, return JSON, map errors to the codes in 7.4.
- A shared FCM HTTP v1 sender. It **does nothing until Phase 11** sets the secret — don't let a missing secret fail a request.
- Retrofit interface for the three functions with typed error parsing.
- **HUMAN CHECKPOINT:** deploy through the MCP connector, or give Dylan the exact `supabase functions deploy` commands.

**Done when:** each function works when called from the app, and each 409 case returns its reason code. Include one curl example per function in the report.

### Phase 6 — Cart and checkout

- Cart: lines with option labels, steppers, empty state, slot chips showing "12 of 40 taken", totals.
- Checkout: summary, collection card, payment choice (wallet or pay at counter), insufficient balance blocks wallet with a clear message, counter blocked at two no-shows with an explanation, cancellation notice.
- Place order generates a `client_ref`, calls `place-order`, then navigates to the order status screen.
- Friendly messages for each 409 reason.

**Done when:** a full student order works end to end — wallet drops, stock drops, slot count rises — and each failure case shows the right message.

### Phase 7 — Student orders

- Order status: animated code tiles, timestamped progress rail, items, total, cancel only while placed.
- Realtime subscription on the student's own order.
- Order history split into in progress and past, with skeleton loading and an empty state.
- Active order card at the top of the menu.

**Done when:** a status change made in the database appears on the student's screen without refreshing.

### Phase 8 — Staff

- **Queue:** counters, filter chips, one next-step button per card, code plus student name plus the full option label, realtime inserts and updates, a no-show action on ready orders once their slot has ended, a **Top up** button in the top bar.
- **Stock:** steppers that PATCH stock, low-stock banner, sold-out state, add, edit and remove item sheet.
- **Sales:** revenue, order count, average order, orders by hour as a Vico bar chart with the peak marked in text as well as colour, top sellers, CSV export through the share sheet.
- **Wallet top-up:** find a student by student number, presets plus custom amount, confirm, call `load-wallet`, show the new balance.

**Done when:** a student orders on one phone and staff see it instantly on another, advance it, and the student sees every change live.

### Phase 9 — Profile and settings · *required feature*

- Profile: identity card with student number, role pill, wallet and loyalty progress ("7 of 10", display only — no redemption) for students.
- Settings: language, theme (System, Light, Dark), order notifications, biometric unlock, change password, privacy and POPIA page, sign out. Stored in DataStore.
- Biometric: when enabled and a session exists, `BiometricPrompt` gates the app on open, with password sign-in as the fallback.

**Done when:** settings survive a restart, the theme switches instantly, and the biometric gate works on the phone.

### Phase 10 — Offline mode with sync · *PoE*

- Room caches the menu with options, slots, the student's orders, the cart and weather.
- Repositories are network-first with cache fallback, exposing `Flow` from Room.
- Connectivity observer shows a slim "You're offline" banner.
- Offline writes are queued: **placing an order** (using `client_ref` so replays are safe) and **staff stock adjustments**.
- `SyncWorker` on a network constraint with exponential backoff replays the queue. A 409 on replay notifies the user with the reason.
- Queued orders show "Waiting for signal. We'll send it as soon as you're back online."

**Done when:** in airplane mode the menu and orders still open, an order placed offline shows as waiting, and reconnecting sends it — or explains clearly why it couldn't go through.

### Phase 11 — Push notifications · *PoE*

- **HUMAN CHECKPOINT:**
  1. Create a Firebase project, add an Android app with package `com.rimagwinya.app`, download `google-services.json` into `app/`
  2. *Project settings → Service accounts → Generate new private key*, then set Supabase Edge Function secrets `FCM_SERVICE_ACCOUNT` (the full JSON) and `FCM_PROJECT_ID`
- Google Services plugin and Firebase Messaging.
- `FirebaseMessagingService`: save refreshed tokens to `profiles.fcm_token`, notification channels (orders, wallet, stock).
- `POST_NOTIFICATIONS` permission on Android 13+ with a short explanation first.
- Tapping a notification deep-links to that order.
- In-app notifications sheet from the `notifications` table. Honour the settings toggle.

**Done when:** staff mark an order ready and the student's phone gets a push within about five seconds, and tapping it opens the order.

### Phase 12 — Single sign-on · *PoE*

- **HUMAN CHECKPOINT:**
  1. Google Cloud Console: OAuth consent screen, a **Web** client ID and an **Android** client ID using the SHA-1 from `./gradlew signingReport`
  2. Supabase *Authentication → Providers → Google*: enable, paste the Web client ID and secret
- Credential Manager → Google ID token → Supabase ID-token sign-in.
- **Any** Google account is accepted (section 12). On a first Google sign-in with no profile row yet, show a one-field screen asking for the student number before routing into the app. Handle the duplicate-number case there.
- "Continue with Google" on login and register.

**Done when:** signing in with Google on the phone lands on the correct role's app.

### Phase 13 — Multi-language · *PoE*

- Add `values-af/strings.xml` and `values-st/strings.xml` alongside English.
- Per-app language with `AppCompatDelegate.setApplicationLocales`, a `locales_config.xml`, `android:localeConfig` in the manifest, and the `AppLocalesMetadataHolderService` with `autoStoreLocales` for devices below Android 13. `MainActivity` must extend `AppCompatActivity` with an AppCompat DayNight theme.
- Language picker in settings; also save the choice to `profiles.language`.
- Plurals and format strings for counts and prices.
- **HUMAN CHECKPOINT:** a first-language speaker reviews the Afrikaans and Sesotho strings. Machine translation will contain mistakes.

**Done when:** switching language updates the whole app immediately and survives a restart.

### Phase 14 — Weather

- Retrofit service and DTOs for Open-Meteo, WMO code mapping, Room cache with the 60-minute expiry.
- Menu weather strip, sorting rules and rain banner from 9.4.
- Staff forecast card on the sales screen.
- Unit tests for the thresholds, code mapping and cache expiry.

**Done when:** menu order changes with the weather (prove it with a fake repository in a test), and it still works offline from the cache.

### Phase 15 — Tests and CI

- Fill gaps. At minimum: `PriceCalculator`, validation (email format, password, student number), order status transitions, cart line merging, weather rules, `Money` formatting, DTO-to-domain mapping, ViewModel state changes with Turbine.
- `.github/workflows/android-ci.yml`: on push and pull request to `main`; JDK version required by the project's AGP; Gradle caching; write `google-services.json` from a secret; run `./gradlew testDebugUnitTest lintDebug`; upload test reports as an artifact.
- **HUMAN CHECKPOINT:** add GitHub secrets `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_SERVICES_JSON` (base64).

**Done when:** the GitHub Actions tab shows a green run on push. Report the test count.

### Phase 16 — Release preparation

- `versionCode` and `versionName`, R8 with keep rules for kotlinx.serialization, Retrofit and supabase-kt, resource shrinking.
- **HUMAN CHECKPOINT:** Dylan creates the upload keystore through *Build → Generate Signed App Bundle*, stores the passwords somewhere safe, never commits the file.
- Signed release AAB.
- Privacy policy page (POPIA) inside the app, plus the same text as markdown for the store listing.
- Store listing draft: short description, full description, screenshot list, feature graphic size.
- Final QA pass on the phone against every screen in the prototype.

**Done when:** a signed AAB builds and installs on the phone.

### 10.13 Gotchas to watch for

- `AppCompatActivity` crashes without an AppCompat theme.
- `java.time` on API 24 and 25 needs core library desugaring.
- `supabase-kt` needs a Ktor client engine — use the OkHttp engine.
- Use KSP for Hilt and Room. Enable Room schema export.
- The Google Services plugin fails the build without `google-services.json`. CI writes it from a secret.
- Keep R8 rules for serialization, or release builds crash when parsing JSON.
- Never log tokens or keys, even in debug.

---

## 11. Phase report template

```
## Phase N — <name> · DONE | BLOCKED

Built
- 3 to 6 plain-English bullets

Try it
- Exact steps on the phone and what you should see

Tests
- X passing (new: …)

Decisions I made
- Anything you might want to change, or "none"

Needs you
- Checkpoint actions, or "nothing"

Commit: phase-N: …
Next: Phase N+1 — one line
```

---

## 12. Decisions — settled

1. **Package name** — `com.rimagwinya.app`. Confirmed.
2. **SSO provider** — **Google.** Credential Manager → Google ID token → Supabase ID-token sign-in. The Microsoft/Azure alternative is dropped.
3. **Email domain** — **none.** Students use ordinary personal accounts (Gmail and the like). There is no college domain to check, so `ALLOWED_EMAIL_DOMAIN` does not exist. Registration validates that the address is a well-formed email and nothing more. `handle_new_user()` rejects nothing by domain.
4. **Identity** — the **student number** is the only thing tying an account to a real person. It is required at sign-up, required on first Google sign-in, and `unique` in the database.
5. **Loyalty** — **progress only.** One stamp per collected order, profile shows "7 of 10". No redemption flow, nothing touching `PriceCalculator` or checkout. `loyalty_stamps.redeemed` stays in the schema, unused, so redemption can be added later without a migration.
6. **Vetkoek base price R3** — prices are data, changeable from Supabase or the stock screen. Not a blocker.

### 12.1 What open sign-up means

Anyone can create an account and nothing verifies that a typed student number belongs to them. Two consequences to build around:

- **Pay-at-counter is the exposure.** It lets an account consume stock and slot capacity having paid nothing. `no_show_count` caps that at two per account, but accounts are free and unlimited. **Rule: `place_order` only accepts `payment_method = 'counter'` from a student who has had at least one successful wallet top-up** — that is, staff have physically seen them at the speed point. Wallet payment stays open to everyone, because the money was already checked by a human going in. Return `COUNTER_BLOCKED` with a reason the checkout screen can explain.
- **Student numbers are first-come.** A number typed by the wrong person locks the genuine student out, and the fix is Dylan editing the row in Supabase. Known limitation at this scale; do not build a verification flow for it.
