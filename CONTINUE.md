# Where we are

**Phases 0–4 are written. Phases 2–4 have never touched a real database.**

Everything compiles, 58 unit tests pass, and the SQL is tested against a
throwaway local Postgres. But no migration has been run on your actual
Supabase project and the app has never made a real request.

That's four phases of work resting on two small things only you can do.
Worth clearing before I build anything else.

---

## Done

- [x] **Phase 0** — project set up, renamed to `com.rimagwinya.app`, minSdk 24
- [x] **Phase 1** — design system, Inter font, 41 icons, all components, navigation
- [x] **Phase 2** — 11 migrations, RLS, networking layer *(written, not applied)*
- [x] **Phase 3** — welcome, login, register, role from the server *(built, not tried)*
- [x] **Phase 4** — pricing engine, cart, menu screen, item sheet *(built, not tried)*

Detail on any of it is in `docs/PROGRESS.md`.

---

## Not done

- [ ] Realtime stock updates (last bit of Phase 4 — needs a live database)
- [ ] Phase 5 onwards — Edge Functions, cart/checkout, orders, staff, and the rest

---

## What I need you to do

Three things, in this order.

### 1. Restart Claude Code

This is the big one. The Supabase connector is registered but **this session
started before it existed**, and Claude Code only loads connectors at startup.
So nothing I do mid-session can fix it.

```
exit
claude --continue
```

`--continue` picks this conversation back up instead of starting cold.

Then check it worked:

```
/mcp
```

You should see **supabase-rm**. First use will ask you to sign in to Supabase.

*If it's still not there:* tell me and I'll dig further — don't keep
re-running `claude mcp add`, the config is already correct.

### 2. Get me the anon key

Supabase dashboard → **Project Settings → API keys** → the **anon / public**
one. Not the service role key — that never goes near the app.

Paste both lines into `local.properties` (already gitignored):

```properties
SUPABASE_URL=https://jykswltsegssjmvnqqbi.supabase.co
SUPABASE_ANON_KEY=<paste it here>
```

### 3. Turn off email confirmation

Supabase → **Authentication → Sign In / Providers → Email** → switch off
**Confirm email**.

Otherwise every test account needs an inbox visit before it can sign in.

---

## Then say this

> Connector's live and the key is in. Apply the migrations and let's test it.

I'll then:

1. Run all 11 migrations against your project
2. Check the menu loads on your phone
3. Walk you through registering an account and making a staff user
4. Finish Phase 4's realtime bit
5. Report, and wait for your go on Phase 5

---

## If you want something else instead

- **"carry on building"** — I'll keep going on Phase 5 (Edge Functions) with
  everything still unverified. Possible, but the risk keeps stacking.
- **"just do the migrations by hand"** — I'll hand you the 11 SQL files in
  order to paste into the SQL Editor, no connector needed. Slower but it
  unblocks everything.
- **"what's in X?"** — ask about any phase, file or decision.

---

## Quick facts

| | |
|---|---|
| Supabase project | `jykswltsegssjmvnqqbi` |
| Package | `com.rimagwinya.app` |
| minSdk / compileSdk | 24 / 37 |
| Unit tests | 58 passing |
| Database tests | 35 assertions, `bash supabase/tests/run.sh` |
| Build | `.\gradlew.bat assembleDebug` |
| Last commit | `phase-4: cart, menu screen and item sheet` |

**Still outstanding from earlier:** the GitHub repo and remote were never
created, and the launcher icon is still the Android Studio default
(*File → New → Image Asset*). Neither blocks anything.

---

## Settled decisions, so nobody re-opens them

- Google sign-in, any Google account — no college domain
- Students use ordinary Gmail; the **student number** identifies them
- Loyalty is progress only, no redeeming
- Pay-at-counter needs one prior wallet top-up, and is withdrawn after two no-shows
- Money is integer cents everywhere; the **server** decides every price
