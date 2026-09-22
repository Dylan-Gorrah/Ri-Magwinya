# Ri-magwinya — Demo Video Checklist

Read this top to bottom while you record. Every part tells you what to **show** and gives you something to **say**. Don't read the lines like a robot. Just hit the same points in your own words.

**What the brief wants:** the app running on a **real phone**, a **voice-over the whole way**, **every feature** shown, and the data sitting in **Supabase** (auth, API and database). Do all that and keep it tidy, and you get the full 5.

There's no length rule. Everything here fits in roughly 8–12 minutes if you don't rush.

> Swap anything in [brackets] for what your app actually has.

---

## Before you hit record

- [ ] Phone charged and on **Do Not Disturb**, so no WhatsApp pops up mid-video
- [ ] Mirror the phone to your laptop with **scrcpy** (free, needs USB debugging on) and record the laptop screen with **OBS**. That way the phone and Supabase are in the same recording, in one take
- [ ] Log out of the app so you start on the login/register screen
- [ ] A **new email** ready to register with
- [ ] **Email confirmation** sorted. If Supabase makes new users confirm their email, switch it off for the demo or keep that inbox open so you can click the link on camera
- [ ] A **staff account** already set up
- [ ] Menu items with stock loaded, so no screen is empty
- [ ] Supabase open in browser tabs: **Authentication → Users**, **Table Editor**, **SQL Editor**, **Logs**
- [ ] Nothing secret on screen. Close any tab showing your **service_role key**
- [ ] A quiet room. An earphone mic is fine
- [ ] One practice run before the real one

---

## 1. Intro — about 30 seconds

**Show:** the app open on the phone.

**Say:**
> "Hi, I'm Dylan Gorrah, ST10398445. This is Ri-magwinya, our campus tuckshop pre-ordering app for OPSC6312 Part 2.
>
> Here's the problem. Students queue the whole break, the popular stuff sells out before they get to the counter, and staff can't see what's coming. With Ri-magwinya, students order ahead and just collect, and staff see every order as it comes in.
>
> It's built in Kotlin with Jetpack Compose, and Supabase handles login, the REST API and the database. It's running on my actual phone right now."

*(If it's a group video, name everyone at the start.)*

---

## 2. Register — sign-in feature (10 marks)

**Show:**
- [ ] Hit register with **empty fields** → error
- [ ] A **bad email** → error
- [ ] A **short password** → error
- [ ] Passwords that **don't match** → error
- [ ] Now register properly with the new email

**Say:**
> "First, bad input. Empty fields, a wrong email, a short password and passwords that don't match each get a clear message, and nothing crashes. Now I'll register properly."

---

## 3. Show the password is encrypted

**Show (laptop):**
- [ ] **Authentication → Users**, where the new account is showing
- [ ] **SQL Editor**. Run this and point at the hash:

```sql
select email, encrypted_password from auth.users;
```

**Say:**
> "Here's the account I just made, in Supabase Auth. And this is how the password is stored: a bcrypt hash, not the real password. The plain password is never saved anywhere, not in our tables and not on the phone."

---

## 4. Login

**Show:**
- [ ] Log in with the **wrong password** → error
- [ ] Log in properly → home screen
- [ ] Close the app completely, open it again → still logged in

**Say:**
> "The wrong password gives an error, not a crash. The right one takes me in. And if I close the app and come back, I'm still logged in, because the session is saved."

---

## 5. Settings (10 marks)

**Show:**
- [ ] Open settings
- [ ] Change something you can see, like [dark mode], and something on your account, like [your name]
- [ ] Try saving an **empty name** → error
- [ ] Close and reopen the app → the settings stuck
- [ ] On the laptop, the updated row in the **profiles** table

**Say:**
> "In settings I can [switch to dark mode, change my name and pick my usual collection slot]. Empty values get blocked. The theme is saved on the phone, and my account details are saved in Supabase. Here's the updated row. When I reopen the app, everything's how I left it."

---

## 6. REST API + database (20 marks)

**Show:**
- [ ] The menu loading on the phone
- [ ] On the laptop, the **[menu_items]** table with the same items and the same stock
- [ ] Place an order on the phone → refresh **[orders]** → the new row is there, and the stock went down
- [ ] **Logs**, showing the API requests that just came in
- [ ] The **Policies** page with your RLS rules
- [ ] If you made your own database function or Edge Function (e.g. for placing orders), show it. That's the "we built this" part of the API mark
- [ ] Turn on **airplane mode** → try to load → friendly error → turn it off → **retry** works

**Say:**
> "Everything in the app comes from our Supabase REST API, which is hosted online. Here's the menu table, with the same items and stock as on the phone. When I place an order... here's the new row, and the stock has dropped. These are the API requests coming in live.
>
> Row Level Security means students only see their own orders, and only staff can change stock. And if the internet drops, the app tells you and lets you retry instead of crashing."

---

## 7. Our three features (30 marks — the big one, take your time)

Do all three the same way:
1. **Say** what it is and why students or staff need it
2. **Show** it working from start to finish
3. **Show** the change in Supabase
4. **Show** one thing going wrong and the app handling it

### Feature 1: ______________
- [ ] What it is + why it matters
- [ ] Working start to finish
- [ ] The change in Supabase
- [ ] One error handled

### Feature 2: ______________
- [ ] What it is + why it matters
- [ ] Working start to finish
- [ ] The change in Supabase
- [ ] One error handled

### Feature 3: ______________
- [ ] What it is + why it matters
- [ ] Working start to finish
- [ ] The change in Supabase
- [ ] One error handled

**Lines you can use, depending on which three you picked:**

- **Live stock menu:** "Students see what's actually left before they order. Sold-out items can't be added. When I order this, the stock drops. There it is in the table."
- **Collection slot + code:** "I pick a break slot, and full slots can't be picked. After ordering I get a four-digit code to show at the counter."
- **Staff order queue:** *(log in as staff on the same phone, or use a second phone to show it live)* "Staff see new orders come in and move them from new, to preparing, to ready, to collected. The student sees the status change."
- **Campus wallet:** "The price comes off my balance when I order, and if I don't have enough, it won't let me."
- **Stock management:** "Staff can add a new item or change stock, and students see it straight away."
- **Sales analytics:** "Staff can see today's totals and what's selling best."

---

## 8. Wrap-up — about 20 seconds

**Say:**
> "So that's Ri-magwinya: register and login with hashed passwords, settings that save, our Supabase REST API and database, and our three features, [feature 1], [feature 2] and [feature 3]. All on a real phone. Thanks for watching."

**Optional extras if you've got time** (not required):
- Logcat showing your log messages while you log in, which shows you understand the flow
- The green GitHub Actions run

---

## While you're recording

- Talk a bit slower than feels normal
- Say what you're about to tap before you tap it
- If you mess up, pause, breathe and redo just that bit. You can cut it out later
- Don't rush section 7. It's 30 of the 100 marks

---

## After recording

- [ ] Watch it back once. Can you hear yourself? Is anything private on screen?
- [ ] Cut the mistakes and dead air (Clipchamp on Windows or CapCut, both free)
- [ ] Upload to YouTube as **Unlisted**. If it's Private, your lecturer can't watch it
- [ ] Open the link in an **incognito window** to check it plays
- [ ] Paste the link into the README
