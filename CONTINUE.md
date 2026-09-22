# Where we are

**All 16 phases are built.** 147 unit tests pass, lint is clean, and the
database, the three server functions and the security rules are live on your
real Supabase project.

What has **not** happened: the app has never been run on your phone. Every
screen is verified by tests, by curl against the live server, and by the
build — not by a person tapping it. That is the next thing, and
`docs/qa-checklist.md` is the list to walk.

---

## What works right now, with no further setup

- Sign in and register, with the role coming from the server.
- The whole student side: menu, item options, cart, checkout, orders.
- The whole staff side: queue, stock, sales with a chart and CSV, top-ups.
- Profile, settings, theme, fingerprint unlock, the POPIA page.
- Offline: the menu opens, the cart survives, orders queue and send later.
- English, Afrikaans and Sesotho.
- Weather-aware menu ordering.

## What is built but switched off until you do something

| Feature | What it needs |
|---|---|
| Push notifications | A Firebase project and `google-services.json` in `app/` |
| Google sign-in | A Google Cloud Web client id in `local.properties` |
| Automatic builds | The GitHub repo, and the repository secrets |
| Signed release | Your upload keystore |

None of these break the app while they are missing. The Google button hides
itself, push does nothing, and the release build is simply unsigned.

---

## Your list, in the order I would do it

### 1. Run it on your phone (30 minutes)

Press Run in Android Studio, sign in as **user123@gmail.com / FrogybyD1**,
and walk `docs/qa-checklist.md`. Ideally have a second phone signed in as
**admin@gmail.com / Admin1234!** so you can watch an order land on the staff
queue while you place it.

Tell me anything that looks wrong and I will fix it.

### 2. Rotate the secret key

It was pasted into a chat. Supabase → Project Settings → API keys →
regenerate the **secret** key. The app does not use it, so nothing breaks.

### 3. The GitHub repo

Never created. Once it exists:

```
git remote add origin https://github.com/Dylan-Gorrah/ri-magwinya.git
git push -u origin main
```

Then add the repository secrets `SUPABASE_URL` and `SUPABASE_ANON_KEY`, and
the Actions tab should go green.

### 4. Launcher icon

Still the Android Studio default. *File → New → Image Asset* with your PNG.

### 5. Firebase, for push (Phase 11)

1. Firebase project → add an Android app with package `com.rimagwinya.app`.
2. Download `google-services.json` into `app/` (already gitignored).
3. *Project settings → Service accounts → Generate new private key.*
4. In Supabase, set Edge Function secrets `FCM_SERVICE_ACCOUNT` (the whole
   JSON) and `FCM_PROJECT_ID`.

### 6. Google sign-in (Phase 12)

1. Google Cloud Console: OAuth consent screen, a **Web** client id, and an
   **Android** client id using the SHA-1 from `./gradlew signingReport`.
2. Supabase → Authentication → Providers → Google: enable, paste the Web
   client id and secret.
3. `GOOGLE_WEB_CLIENT_ID=…` in `local.properties`.

### 7. The translations (Phase 13)

Afrikaans and Sesotho are machine-assisted and need a first-language speaker
to read them. `app/src/main/res/values-af/strings.xml` and `values-st/`.
The Sesotho needs it more than the Afrikaans.

### 8. Release (Phase 16)

*Build → Generate Signed App Bundle* to make the keystore, then add the four
`RELEASE_*` lines to `local.properties`. Publish `docs/privacy-policy.md`
somewhere public for the store listing.

---

## Things worth knowing

- **Test data is in the database.** user123 has R33 and a couple of test
  orders; vetkoek and chips stock is slightly down. Say the word and I will
  reset it.
- **Order numbers skip** (#1, then #5). Postgres uses up identity values on
  orders that were refused and rolled back. Normal.
- **Two decisions I made** because you did not pick: collection breaks that
  have ended are hidden, and a break can be ordered for until 5 minutes
  before it ends (`AppConfig.SLOT_CUTOFF_MINUTES`).
- **Where I disagreed with the brief**, and why, is written up in
  `docs/PROGRESS.md`: the stock stepper sends a change rather than a total,
  and pay-at-counter requires a prior top-up.

## Quick facts

| | |
|---|---|
| Supabase project | `jykswltsegssjmvnqqbi` |
| Package | `com.rimagwinya.app` |
| Unit tests | 147, all passing |
| Build | `.\gradlew.bat assembleDebug` |
| Everything in detail | `docs/PROGRESS.md` |
| The phone walkthrough | `docs/qa-checklist.md` |
