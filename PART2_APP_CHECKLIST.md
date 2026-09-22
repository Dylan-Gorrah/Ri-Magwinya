# Ri-magwinya — Part 2 App Checklist (for Claude Code)

> **Dylan:** fill in your 3 features in section 5 before you start. Then open Claude Code in the project folder and say:
> *"Read PART2_APP_CHECKLIST.md and do the audit first. Don't change any code until I've seen the report."*

---

## Context

- **Ri-magwinya** is a campus tuckshop pre-ordering app for Rosebank College. Students order ahead and collect at break. Staff see incoming orders and manage stock.
- **Stack (from our Part 1 design):** Kotlin, Jetpack Compose, MVVM, Supabase (Auth + Postgres + REST API). Check `build.gradle.kts` for what's actually used.
- **The MVP already works.** Your job is to make sure every requirement below is in there. Add what's missing and don't rewrite what works.
- This is **OPSC6312 Part 2**, marked out of 100, due **Tue 22 Sep 2026, 23:59**. Time is tight, so stay focused.

## How to work

1. **Audit first.** Go through every item and mark it ✅ done, ⚠️ partial or ❌ missing, with the file path as proof. Show me the report before changing anything.
2. Do the **must** items first, because marks depend on them. Every item is a must unless it's tagged *(nice)*. Only do *(nice)* items if there's time.
3. One fix at a time, using the smallest change that works. Ask before any big refactor.
4. After each fix, `./gradlew assembleDebug testDebugUnitTest` must still pass.
5. Commit after each working fix with a clear message (e.g. `feat(auth): add register validation`). We need lots of real commits.
6. Don't build anything in **Out of scope** at the bottom.

---

## 1. Builds and runs on a real phone — 5 marks

- [ ] `./gradlew assembleDebug` builds with zero errors
- [ ] A fresh install opens without crashing (no saved session, no cached data)
- [ ] Nothing emulator-only: no `10.0.2.2` or `localhost` URLs. Everything points at the hosted Supabase project
- [ ] No crash traps: no `TODO()`, no `!!` on network or user data, no uncaught exceptions in coroutines
- [ ] *(nice)* Rotating the phone, or leaving the app and coming back, doesn't crash it or wipe what's on screen

## 2. Register + login — 10 marks

- [ ] Register screen: email, password, confirm password (+ name if the profile needs it)
- [ ] Login screen: email + password
- [ ] Auth goes through **Supabase Auth**, so passwords are hashed (bcrypt) on the server. Make sure a password is **never** stored in plain text anywhere. That means not in our own tables, not in DataStore/SharedPreferences, and not in any `Log` call
- [ ] If any custom table stores passwords, remove that and rely on Supabase Auth
- [ ] Clear error messages for: empty fields, bad email format, password too short, passwords don't match, wrong email/password, email already registered
- [ ] Handle Supabase's **email confirmation**. If it's on, show "check your email to confirm" after sign-up instead of failing silently. Tell me if it should be switched off in the dashboard for the demo
- [ ] The button shows loading and is disabled while the request runs (no double taps)
- [ ] The user stays logged in after closing and reopening the app
- [ ] Logout returns to login, and the back button can't get back into the app
- [ ] If the app has both roles, student and staff come from the database and each lands on its own home screen

## 3. Settings — 10 marks

Settings have to make sense for a tuckshop app.

- [ ] The settings screen is reachable from the main navigation
- [ ] It has real settings that fit the app, for example:
  - edit profile (name, phone)
  - dark mode (the Part 1 prototype had this)
  - usual collection slot (students)
  - *(nice)* change password
- [ ] Every setting actually saves and survives an app restart. Use DataStore for device settings and the Supabase `profiles` row for account details
- [ ] Changes apply straight away (the theme switches without restarting)
- [ ] Editable fields are validated (no empty name, valid phone number)
- [ ] Logout button
- [ ] No fake settings, meaning nothing that toggles but does nothing. No language picker or push-notification switches, because those are PoE

## 4. REST API + database — 20 marks (10 build, 10 integration)

- [ ] All real data comes from the hosted Supabase project through its REST API. No hardcoded fake lists left on real screens
- [ ] Tables fit the app, e.g. `profiles`, `menu_items`, `orders`, `order_items`, `collection_slots` (use whatever we really have)
- [ ] **Row Level Security is on for every table**, with sensible policies: students only see their own orders, logged-in users can read the menu, and only staff can change stock and order status
- [ ] Our Part 1 design specified PostgREST endpoints plus two custom functions (place order, change order status). Check what exists. If they're missing, a Postgres function called through `/rpc` for placing an order is the quickest way to show we *built* part of the API. It should check stock, take the stock off and create the order, all in one transaction. *(nice — ask me first)*
- [ ] Network code sits in a repository/data layer, not inside composables
- [ ] Every call is wrapped (try/catch or `Result`). Failures show a friendly message with a retry, never a crash
- [ ] No internet shows a clear message, not an endless spinner or a crash
- [ ] Every screen that fetches data has a loading state and an empty state ("No orders yet")
- [ ] The Supabase URL and anon key come from `BuildConfig` (via `local.properties`), not hardcoded in Kotlin files
- [ ] The **service_role key is nowhere** in the app or the repo. Use the anon key only

## 5. Our 3 features — 30 marks (the biggest chunk)

The rubric marks three "user defined" features from our Part 1 design. These are the candidates from the design and prototype:

- **Live stock menu:** students see what's actually left, sold-out items can't be ordered, and stock drops when an order is placed
- **Collection slot + 4-digit code:** pick a break-time slot (slots have a limit) and get a code to show at the counter
- **Staff order queue:** staff see new orders come in live and move them from new → preparing → ready → collected
- **Campus wallet:** the balance is shown and taken off when ordering, and you can't order without enough
- **Stock management:** staff add/edit menu items and update stock
- **Sales analytics:** simple totals and best sellers for staff

> **Dylan — the 3 we're submitting:**
> 1. ____________________
> 2. ____________________
> 3. ____________________
>
> *(Claude Code: if this is still blank, tell me which candidates already work end to end and ask me to pick.)*

For each of the three:

- [ ] Works start to finish with real Supabase data
- [ ] The change shows up in the database (we'll show it in the video)
- [ ] Bad input and errors are handled, e.g. empty cart, quantity 0, item sells out mid-order, not enough balance, slot full
- [ ] Empty states handled
- [ ] At least one unit test on its logic
- [ ] Logging on the key steps

## 6. User interface — 10 marks

- [ ] One `MaterialTheme` used everywhere. Colours come from the theme, with no random `Color(0xFF...)` inside screens
- [ ] Palette from Part 1: navy `#0E2748`, sky blue `#4C8FCB`, silver-white, gold `#B8923F`
- [ ] Same fonts, button style, spacing and top bar on every screen
- [ ] Dark mode keeps text readable
- [ ] Back navigation works everywhere, with no dead-end screens
- [ ] Each field has the right keyboard (email, number, password with show/hide), and Next/Done moves through the form
- [ ] Nothing is cut off on a small phone, and long screens scroll
- [ ] Errors show on screen (under the field or in a snackbar), not just in Logcat
- [ ] *(nice)* Content descriptions on icons

## 7. Code quality — required for submission

- [ ] Comments on every class and key function, explaining what it does and why
- [ ] References in comments for anything borrowed (Android docs, Supabase docs, tutorials, AI help), in IIE Harvard style:
  ```kotlin
  // Reference: Author. Year. Title. [Online]. Available at: <link> [Accessed 21 September 2026].
  // AI assistance: Anthropic. 2026. Claude [Large language model]. Available at: https://claude.ai [Accessed 21 September 2026]. Used to: <what it helped with>.
  ```
- [ ] Logging with `android.util.Log` and a `TAG` per class. Use `Log.d` for normal flow (login started, menu loaded, order placed) and `Log.e` for errors, with the exception. **Never log passwords, tokens or keys**
- [ ] At least one external library (e.g. Retrofit/Ktor, Coil) and one SDK (Supabase) in use. List them for the README

## 8. Unit tests — part of the 10 GitHub marks

- [ ] Tests live in `app/src/test`. They're plain JVM JUnit with no emulator, so they run in GitHub Actions
- [ ] They cover the main logic: email/password validation, order totals, stock check (can't order more than what's left), wallet balance check, collection code format, order status changes
- [ ] Edge cases are included: empty cart, quantity 0, invalid email, exact balance
- [ ] If logic is stuck inside composables, pull it out into plain functions or classes so it can be tested
- [ ] *(nice)* ViewModel tests with a fake repository
- [ ] `./gradlew testDebugUnitTest` passes

## 9. GitHub Actions — part of the 10 GitHub marks

- [ ] `.github/workflows/build.yml` exists
- [ ] It runs on every push and pull request to `main`
- [ ] Steps: checkout → set up JDK 17 → make `gradlew` executable → run unit tests → `assembleDebug` → upload the APK as an artifact
- [ ] **CI must not need `local.properties`.** Gradle reads `SUPABASE_URL` / `SUPABASE_ANON_KEY` from `local.properties` if it exists, then from environment variables, then falls back to an empty string. That way the build never fails just because the file is missing
- [ ] If the build needs real values, tell me which GitHub Secrets to add (repo → Settings → Secrets and variables → Actions)
- [ ] The latest run is green

## 10. Repo hygiene

- [ ] `.gitignore` covers `local.properties`, `build/`, `.gradle/`, `*.jks`, `*.keystore`
- [ ] No secrets committed. Check the git history too, not just the current files
- [ ] No zip files or APKs committed
- [ ] The README is being done separately, so leave it unless I ask

---

## Out of scope — do NOT build (PoE only, 0 marks in Part 2)

- Single sign-on (Google sign-in)
- Offline mode + sync (Room)
- Push notifications (Firebase Cloud Messaging)
- Multi-language / South African languages

If any of these are already half-built, leave them alone. Just make sure they can't crash the app or show up as broken screens.

---

## When you're done, give me

1. The audit table again, updated (✅ / ⚠️ / ❌ + file paths)
2. The list of commits you made
3. Anything I have to do by hand (GitHub Secrets, Supabase dashboard settings like RLS or email confirmation)
4. A demo path for the video: which screens to tap through, in order, to show every section above
