# Part 3: Final PoE

Back to [[Ri-magwinya]] · [[The brief in plain English]] · [[Part 3 - Brief and rubric]]

**Due Thu 5 Nov 2026.** Time still to confirm. **100 marks.**

## What it wants, in plain English

The app has to be finished: no errors, runs on a real phone, and updated with the lecturer's feedback. Then we prove it with a video, a proper README and Play Store material.

**The video has to show:**

- Register and log in, with the password encrypted
- Google sign-in
- Changing settings
- The REST API and database, plus the actual data sitting in Supabase (Auth users and the tables)
- Offline mode, then the sync when the phone is back online
- A push notification arriving
- Switching between English, Afrikaans and Sesotho
- Our own features from Part 1
- The app icon and the final images
- A voice-over the whole way, and it must look professional

**On GitHub:**

- The README is the full report: what the app is for, the design decisions, how we used GitHub and GitHub Actions, images, and the video link
- Release notes: everything that changed since the prototype, and which new features this version adds
- Regular commits, with the last one tagged as the final PoE
- GitHub Actions runs the tests and builds the app

**In the code:**

- Comments with references
- Logging (`Log.d` and `Log.e`) to show we understand the code

**Also handed in:**

- Play Store evidence: the signed APK, screenshots, and a Play Console screenshot if we have an account
- The AI write-up: 500 words max, how AI was used and how it's cited

## Decisions (5 Nov, evening)

- **Lecturer feedback:** the doc Dylan has only holds the Part 2 links and cover page, with no feedback in it. Unless more turns up, the "updated from feedback" changes are Dylan's own list below.
- **Staff can't create accounts.** The staff screen is email and password only. Staff accounts are made by the manager in Supabase. The server already makes every sign-up a student.
- **The login flow gets redone.** It's glitchy. See Phase 1 in [[Part 3 phases]].
- **Navigation gets cleaned up.** There were errors moving around. See Phase 2.
- **Social logins:** Google first, because it's the one that earns the SSO marks. Facebook goes last as an extra, with a 45-minute limit.

## The rubric against where we are

| Rubric row | Marks | Where we are | What's left |
|---|---|---|---|
| Runs on a phone | 5 | v1.0.2 APK runs | Re-test on the phone after tonight's changes |
| SSO login | 10 | Built, switched off | Dylan sets up Google Cloud and Supabase, then we test. See [[Google sign-in]] |
| Offline mode | 10 | Built, on | Prove it in airplane mode. See [[Offline mode]] |
| Real-time notifications | 10 | Built, switched off | Dylan sets up Firebase, then we test. See [[Notifications]] |
| Multi-language | 10 | Built, on | Quick check of the strings, show it switching. See [[Languages]] |
| Our feature 4 | 10 | Built | Collection slots and the 4-digit code |
| Our feature 5 | 10 | Built | Staff sales dashboard |
| Play Store ready | 5 | Signed APK, store listing draft, privacy policy | Store screenshots, a signed AAB, feature graphic. Play Console screenshot only if there's an account |
| AI write-up | 5 | Unclear if Part 2 had one | Write or update it, 500 words max, with citations |
| User interface | 10 | Done | Nothing |
| GitHub, README and tests | 10 | 172 tests, Actions builds and tests | Commit the 13 Oct work, rewrite the README, add release notes, tag `final-poe`, keep CI green once Firebase is on |
| Demo video | 5 | Done 5 Nov: https://youtu.be/WvmOFFofuYo | Nothing |

Register, login, settings and the REST API have no marks of their own this time, but they still have to be in the video.

## Our features 4 and 5

Part 2 already used the live stock menu, the campus wallet and the staff queue. Part 3 needs two more from the Part 1 design. Both are already built:

- **Feature 4: Collection slots and the 4-digit code.** See [[Collection slots]] and [[The collection code]].
- **Feature 5: Staff sales dashboard.** Revenue, orders by hour, top sellers and the CSV export. See [[Sales]].

The weather-aware menu ([[Weather]]) gets shown as an extra. It only visibly re-sorts the menu on cold, rainy or very hot days, so it's riskier as a marked feature.

## In the brief but not marked

Blob storage and a NoSQL database are listed as learning outcomes, but no rubric row marks them, and the app uses neither. Skip them tonight.

## Gaps found in the code (5 Nov)

- **No logging.** There's no `Log.d` or `Log.e` in any of the 108 Kotlin files. Only OkHttp's network log. The brief asks for logging.
- The 13 Oct work is still uncommitted.
- The README still says the PoE features are "not part of Part 2" and switched off.
- No `final-poe` tag yet. The only tags are `v1.0.0` and `v1.0.1`. Git tags can't have spaces, so it's `final-poe`.
- Push and Google sign-in are off until they're set up.
- Every commit is Dylan's. The rubric only asks for "multiple commits", so that's fine, but it's safer if Refiloe and Achuma each push something.

## Order to do it in tonight

1. **Dylan:** start the Firebase and Google Cloud setup now. It's the slowest part. See [[What Dylan has to do himself]].
2. **Claude Code, at the same time:** commit the 13 Oct work, build, run the tests, add the logging.
3. **Claude Code:** plug in Firebase and Google, add the GitHub secret so CI still builds, then test both on the phone.
4. **Claude Code:** rewrite the README with release notes, the AI write-up, and a Play Store folder (screenshots and AAB).
5. **Dylan:** take the store screenshots, record the video, upload it unlisted.
6. Tag `final-poe`, push, check CI is green, open every link in incognito, submit.

## Questions for Dylan

1. What time is it due?
2. Did Part 2 come back with a mark and feedback? The brief says the app must be updated with it.
3. Do you have a Google Play developer account? If not, we skip the Play Console screenshot. The brief says "if applicable".
