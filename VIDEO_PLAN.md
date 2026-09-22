# Demo video — what to cover

Short version of `VIDEO_CHECKLIST.md`, with our actual features filled in.
Tick as you record. Roughly 8–12 minutes. Talk slower than feels normal.

**Accounts:** student `user123@gmail.com` / `FrogybyD1` · staff
`admin@gmail.com` / `Admin1234!`

---

## Before you press record

- [ ] Phone charged, on Do Not Disturb, screen mirrored to the laptop
- [ ] Signed out of the app, sitting on the welcome screen
- [ ] A fresh email ready to register with
- [ ] "Confirm email" switched **off** in Supabase (else registering stalls)
- [ ] Supabase tabs open: Authentication → Users, Table Editor, SQL Editor, Logs, Policies
- [ ] Any tab showing the **secret key** closed
- [ ] One practice run

---

## 1. Intro — 30 seconds

- [ ] Say your name and student number, and that this is OPSC6312 Part 2
- [ ] The problem in one line: the queue eats the whole break and things sell out
- [ ] The fix in one line: order ahead, collect with a code, staff see it live
- [ ] Say the stack: Kotlin, Jetpack Compose, Supabase for login, API and database
- [ ] Say it is running on a real phone

## 2. Register — 10 marks

- [ ] Empty fields → error
- [ ] Bad email → error
- [ ] Short password → error
- [ ] **Passwords that don't match → error** (we added the confirm field)
- [ ] Register properly with the new email

## 3. Prove the password is hashed

- [ ] Laptop: Authentication → Users, showing the new account
- [ ] SQL Editor: `select email, encrypted_password from auth.users;`
- [ ] Point at the bcrypt hash and say the real password is stored nowhere

## 4. Login

- [ ] Wrong password → clear error, no crash
- [ ] Right password → straight to the menu
- [ ] Close the app fully, reopen → still signed in

## 5. Settings — 10 marks

- [ ] Open Profile
- [ ] Switch **dark mode** → it changes instantly
- [ ] **Edit profile** → change your name and add a phone number → save
- [ ] Try saving an **empty name** → blocked
- [ ] Laptop: the `profiles` row now shows the new name and phone
- [ ] Close and reopen the app → dark mode and the name stuck

## 6. REST API and database — 20 marks

- [ ] Menu loading on the phone
- [ ] Laptop: `menu_items` table, same items, same stock numbers
- [ ] Place an order on the phone
- [ ] Laptop: new row in `orders`, and the stock has dropped
- [ ] Logs: the API requests arriving live
- [ ] Policies page: Row Level Security on every table
- [ ] Show `place_order` in the SQL Editor and say it prices the order itself — the app never sends a price
- [ ] **Airplane mode** → try to load → friendly message → off again → it recovers

## 7. Our three features — 30 marks, take your time

For each: say what it is and why, show it working, show the database change, show one error handled.

### Feature 1 — Live stock menu with server-priced ordering
- [ ] Say: students see what is actually left, and the server decides the price
- [ ] Build a **vetkoek**: 2 vetkoeks, 2 polony, 1 cheese → button shows **R17.00**
- [ ] Add **large chips** → R40.00 (the size replaces the price, not adds to it)
- [ ] Place the order
- [ ] Laptop: `menu_items` stock dropped, `order_items` shows the exact options
- [ ] **Error:** staff set an item to 0 → back on the student menu it says Sold out and won't open

### Feature 2 — Campus wallet
- [ ] Say: money comes off the wallet, and you cannot order what you cannot pay for
- [ ] Show the balance on the menu hero
- [ ] **Error first:** order more than the balance → "you are short R…" and the button is blocked
- [ ] Staff → Top up → student number → R50 → confirm
- [ ] Laptop: `wallet_transactions` has the top-up, `profiles.wallet_balance` went up
- [ ] Back as the student: place the order → balance drops

### Feature 3 — Staff order queue
- [ ] Say: staff see orders arrive and move them along; students watch it change
- [ ] Sign in as staff (second phone, or the browser version side by side)
- [ ] The order that was just placed is already in the queue
- [ ] Start preparing → Mark ready → the student's screen updates without touching it
- [ ] Mark collected → it leaves the queue
- [ ] Laptop: `orders.status` and the timestamps changing
- [ ] **Error:** try to cancel as the student once it is preparing → refused with a clear message

## 8. Optional extras if there is time

- [ ] Logcat while signing in and ordering, showing the log messages
- [ ] The green GitHub Actions run, and the APK it built
- [ ] The collection code screen and the four-digit code at the counter
- [ ] Sales screen: revenue, chart, top sellers, CSV export

## 9. Wrap-up — 20 seconds

- [ ] Recap: register and login with hashed passwords, settings that save, our Supabase API and database, and the three features
- [ ] Say it all ran on a real phone
- [ ] Thanks for watching

---

## After recording

- [ ] Watch it back — audible? nothing secret on screen?
- [ ] Cut the mistakes and dead air
- [ ] Upload to YouTube as **Unlisted** (not Private)
- [ ] Check the link in an incognito window
- [ ] Paste the link into `README.md` at the top
