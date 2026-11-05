# Video script

Back to [[Ri-magwinya]] · [[Part 3 - Final PoE]] · [[Part 3 phases]]

The flow for the Part 3 demo video. Voice-over the whole way, short and calm.

## Before you record

- **Logins:**
  - Staff: `admin@gmail.com` / `Admin1234!`
  - Student: `user123@gmail.com` / `FrogybyD1`
- **Two devices.** Keep the student signed in on your phone. Do the staff part on a second phone or in the [browser version](https://dylan-gorrah.github.io/Ri-Magwinya/). Push goes to whichever phone is signed in, and signing out clears it, so a student who signs out first won't get the "order ready" push.
- **Deploy `place-order`** first, so the low-stock push works. See [[Notifications]].
- Install `Ri-magwinya-v1.1.0.apk` (the one with the new icon).

## 1. Intro (about 30 sec)

- "Hi, I'm Dylan Gorrah, ST10398445. This is Ri-magwinya, version 1.1.0."
- "It's a pre-order app for the Rosebank College tuckshop in Bloemfontein."
- "At break the queue takes the whole break, and the vetkoek runs out. With Ri-magwinya you order and pay before break, then collect with a 4-digit code."
- "One app, two sides: students order, and tuckshop staff run the orders."
- Show the **app icon** on the home screen, then open the app.

## 2. Part 3 checklist (about 30 sec, README on screen)

"Here's what Part 3 asks for, and where you'll see it in this video:"

- [ ] Runs on a real phone, no errors
- [ ] Register and login, with passwords encrypted
- [ ] Single sign-on with Google
- [ ] Settings
- [ ] REST API and a real database (Supabase)
- [ ] Offline mode with sync
- [ ] Real-time push notifications
- [ ] English, Afrikaans and Sesotho
- [ ] Our own features: **collection slots with the 4-digit code**, and the **staff sales dashboard**
- [ ] GitHub with commits, GitHub Actions, README and release notes
- [ ] Play Store ready: signed APK and AAB, icon and listing

## 3. Sign up with Google

- Tap the **Student** card, then **Continue with Google**, and pick an account.
- "It's a new account, so the app asks for my student number once." Type it in.
- "It lands on the menu. My role comes from the database, not from the card I tapped."

See [[Google sign-in]].

## 4. Register with email (quick)

- Sign out, tap **Create account**, and fill in the form.
- "The button stays off until every field is valid. The POPIA notice sits above it."
- "Passwords go to Supabase Auth and are hashed with bcrypt. The app never stores them."

See [[Signing in]].

## 5. Student uses the app

- **Menu:** "Live stock. Sold-out items can't be ordered. The weather strip sorts hot food first when it's cold."
- **Build an order:** 2 vetkoeks + polony + cheese. "The total updates live: R17."
- **Cart:** pick a break. "This is feature 4, collection slots. You can see how full each break is."
- **Pay from wallet**, then **Place order**.
- **Order screen:** "Here's my 4-digit code. The status updates live."
- **Offline:** turn on airplane mode and place an order. "It's waiting for signal." Turn airplane mode off. "It sends by itself." See [[Offline mode]].
- **Settings:** switch the language to Afrikaans, then Sesotho, then back to English. Toggle dark mode. See [[Languages]].

## 6. Show the database (about 45 sec, laptop)

- Supabase → **Authentication → Users**: "Here are the real accounts, including the Google one."
- **Table editor → orders**: "Here's the order I just placed, with the code."
- "The app talks to Supabase through a REST API, using Retrofit. Every table has Row Level Security, so a student can only see their own orders."

## 7. Staff side

- Sign in on the **Staff** card with `admin@gmail.com`.
- "Staff accounts are made by the manager in the database, not in the app. There's no staff sign-up. The staff screen is email and password only, so a student can't make themselves staff."
- Quick demo: sign in with the student account on the staff screen. "It's refused: this account isn't a staff account."
- **Queue:** "The student's order appeared live." Tap **Start preparing**, then **Mark ready**.
- **Student's phone:** "And there's the push notification." Tap it and it opens the order.
- Back on staff: check the code, then **Collected**.
- **Stock:** low-stock warning and steppers.
- **Sales:** "This is feature 5, the sales dashboard: revenue, orders by hour, top sellers, and a CSV export." See [[Sales]].
- **Top up:** find the student, add R50. See [[Wallet top-up]].

## 8. End (about 20 sec)

- Show GitHub: the green Actions run, the README with release notes, and the release with the APK.
- "That's Ri-magwinya: order ahead, skip the queue. Thanks for watching."

## After recording

**Done:** https://youtu.be/WvmOFFofuYo

Upload it unlisted and send the link. It goes in the README, then the `final-poe` tag goes on (Phase 8).
