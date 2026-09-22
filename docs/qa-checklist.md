# Final QA on the phone

Nothing in this project has been run on a real phone yet — everything is
verified by unit tests, by curl against the live Supabase project, and by
the build. This is the list to walk before calling it done.

Two accounts exist already:

| | Email | Role |
|---|---|---|
| Student | user123@gmail.com | student, TEST001 |
| Staff | admin@gmail.com | staff, STAFF001 |

Ideally do the staff half on a second phone, so you can watch an order
appear on one while placing it on the other.

## Before you start

- [ ] `local.properties` has `SUPABASE_URL` and `SUPABASE_ANON_KEY`.
- [ ] Supabase → Authentication → Email: **Confirm email is off**.
- [ ] The secret key was rotated after it was pasted into a chat.

## Sign in

- [ ] Welcome screen: both role cards lead to sign-in; neither card changes
      what you get after signing in.
- [ ] Register with a new email and a fresh student number → lands on the
      student menu.
- [ ] Registering again with the **same student number** → "That student
      number is already registered."
- [ ] Wrong password → "Email or password is incorrect."
- [ ] Kill the app and reopen → still signed in.
- [ ] Sign in as admin@gmail.com → lands on the staff queue, not the menu.

## Student: menu and ordering

- [ ] The real menu loads: 17 items, prices, stock pills.
- [ ] Weather strip in the hero, and the menu order changes with it.
- [ ] Search and the category chips filter the list.
- [ ] A sold-out row is dimmed and will not open.
- [ ] Vetkoek: 2 vetkoeks + 2 polony + 1 cheese → button reads **R17.00**.
- [ ] 0 vetkoeks + snoek → **R10.00**, and the sheet allows it.
- [ ] Chips → Large → **R40.00**.
- [ ] Tea with any brand, milk and sugar → **R10.00**.
- [ ] Sweets: untouched sheet cannot be added ("Choose something first").
- [ ] Cart: steppers change plain lines; a build shows Remove instead.
- [ ] Collection chips show "x of 40 taken"; a break that has ended is gone.
- [ ] Checkout: wallet balance, "R… left after this order", cancellation
      notice.
- [ ] Place the order → the code screen opens, the wallet drops.
- [ ] Order status: code tiles, the rail with real times, the right total.

## Staff, at the same time

- [ ] The new order appears in the queue **without refreshing**.
- [ ] The card shows the full option label and the collection code.
- [ ] Start preparing → the student's screen moves, live.
- [ ] Mark ready → the student's screen moves; with Firebase set up, a push
      arrives within about five seconds and opens the order when tapped.
- [ ] Mark collected → it leaves the queue; the student's loyalty count goes
      up by one.
- [ ] Stock: a stepper changes the number, and the student's menu follows.
- [ ] Take an item to 0 → it shows Sold out on the student's menu.
- [ ] Sales: revenue and the chart include the collected order; CSV export
      opens the share sheet and the file opens in a spreadsheet.
- [ ] Top up: find TEST001, add R50, confirm → the student's balance rises.

## The awkward cases

- [ ] Order more than the wallet holds → wallet is blocked with the exact
      shortfall.
- [ ] Pay at counter on an account with no top-up → explained, not just
      disabled.
- [ ] Cancel an order while it is still "placed" → money back, stock back.
- [ ] Try to cancel one that is "preparing" → refused with a clear line.
- [ ] Two phones ordering the last item: one wins, the other gets "just sold
      out".

## Offline

- [ ] Airplane mode: the menu still opens (from the cache), with the offline
      banner.
- [ ] Place an order offline → "Waiting for signal", and it appears on the
      Orders screen.
- [ ] Turn the connection back on → it sends itself, and the order appears
      properly.
- [ ] Offline stock change by staff → sent when the signal returns.
- [ ] Kill the app with items in the cart → they are still there on reopen.

## Settings and the rest

- [ ] Theme: System / Light / Dark each apply immediately and survive a
      restart.
- [ ] Language: Afrikaans and Sesotho change the whole app at once and
      survive a restart.
- [ ] Notifications toggle off → no push arrives.
- [ ] Fingerprint unlock on → closing and reopening asks for the fingerprint;
      "Sign in with my password instead" works.
- [ ] Change password → sign out and back in with the new one.
- [ ] Privacy and POPIA page reads correctly in all three languages.

## Accessibility and look

- [ ] Every status is readable as words, not only as a colour.
- [ ] Nothing tappable is smaller than a fingertip.
- [ ] Largest font size in Android settings: no clipped or overlapping text.
- [ ] Dark mode on every screen.
- [ ] Back from every screen goes somewhere sensible.

## Release build

- [ ] `./gradlew assembleRelease` installs and runs (R8 on — this is where
      a missing keep rule shows up, usually as a crash on the menu).
- [ ] Sign in, place an order and open the staff queue on the release build.
- [ ] `./gradlew bundleRelease` produces the AAB to upload.
