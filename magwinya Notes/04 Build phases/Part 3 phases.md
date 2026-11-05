# Part 3 phases

Back to [[Ri-magwinya]] · [[Part 3 - Final PoE]] · [[Claude Code Rules]]

Claude Code works through these in order: one phase, report, stop, wait for "go". After each phase it updates the Status column here and [[Status right now]].

**The deadline is tonight.** "Marked" means the phase earns rubric marks. "Extra" only happens if there's time left.

| # | Phase | Marked or extra | Needs Dylan | Status |
|---|---|---|---|---|
| 0 | Save the 13 Oct work | Marked (GitHub) | No | Done 5 Nov. Committed. **Push waits on Dylan** (GitHub account) |
| 1 | Login flow revamp | Marked (SSO, UI, runs on phone) | Tell it the glitches you've seen | Done 5 Nov. 182 tests, 36 SQL checks. Needs a phone test |
| 2 | Navigation clean-up | Marked (runs on phone, UI) | No | Done 5 Nov. Needs a phone test |
| 3 | Logging and code references | Required by the brief | No | Done 5 Nov. 76 log lines, 9 references. See [[Logging]] |
| 4 | Switch on Google sign-in | Marked (SSO, 10) | Yes: Google Cloud and Supabase | Done 5 Nov. Needs the GitHub secret and a phone test |
| 5 | Switch on push notifications | Marked (push, 10) | Yes: Firebase and Supabase secrets | Done 5 Nov. Needs place-order deployed, the GitHub secret and a phone test |
| 6 | Facebook login | Extra (no marks) | Yes: Meta developer app | Skipped (no marks, no time) |
| 7 | README, release notes, AI write-up, Play Store folder | Marked (GitHub 10, Play Store 5, AI 5) | Screenshots, read the AI write-up | Done 5 Nov. v1.1.0, 5 screenshots in the README |
| 8 | Final checks and the `final-poe` tag | Marked | Phone test, video link | In progress: video in, tagging |

---

## Phase 0: Save the 13 Oct work

The sign-in and session rework from 13 Oct is sitting uncommitted. Save it before touching anything.

- Run `git status` and `git log origin/main..HEAD`. Build with `assembleDebug` and run `testDebugUnitTest`. Report the test count.
- If it's green, commit the 13 Oct code, the new `SessionViewModelTest.kt` and the 1.0.2 version bump together. Commit the vault notes separately. Push.
- Don't commit `.idea/` noise or `.claude/`. Check `.gitignore` covers them.
- If the build or tests fail, stop and report. Don't fix anything yet.

**Done when:** the work is on GitHub and Actions is green.

## Phase 1: Login flow revamp

Dylan says the login flow is glitchy and wants it redone properly.

**How it should work:**

- **Welcome:** two cards, Student and Staff.
- **Student:** log in, create account, and "Continue with Google". Facebook gets added in Phase 6.
- **Staff:** email and password only. No "Create account" link and no Google button. One line under the form: "Staff accounts are set up by the tuckshop manager."
- **The role still comes from the server.** Staff who sign in on the student screen land in the staff app. If a student signs in on the staff screen, sign them out and show "This account isn't a staff account." Say in the report if you think another way is better.
- **The server already blocks staff sign-up.** `handle_new_user()` always makes a student. Check that's still true, and add a SQL test in `supabase/tests/` if there isn't one.

**Find the glitches before fixing them.** Read the auth code and `navigation/RootNavHost.kt`, then list what's wrong. Things to look for:

- The welcome or login screen flashing before the app opens for someone already signed in
- A double tap opening two screens or sending two requests
- Back from the app returning to login, or back from login leaving the app
- A spinner stuck after an error, or old errors not clearing
- The keyboard covering fields, or the keyboard's Next and Done keys doing nothing
- The 13 Oct problem: a slow profile answer putting staff into the student app

Report the list first, then fix it.

**It must never crash on:** a wrong password, no signal, empty fields, a duplicate student number, or a cancelled Google picker.

Add unit tests for the new rules (staff screen has no sign-up, role routing, a student on the staff screen).

**Done when:** on the phone, every path works first time with no flash and no double screens. Closing and reopening the app keeps you signed in. Sign out goes back to welcome, and back doesn't go into the app again.

## Phase 2: Navigation clean-up

Dylan hit errors moving around the app.

- Go through every route in `navigation/` and every `navigate(` call.
- The usual fixes:
  - Bottom tabs with `launchSingleTop` and `restoreState`.
  - Ignore a tap if the screen isn't `RESUMED`, so a double tap can't navigate twice.
  - `popUpTo` on sign-in and sign-out, so back can't return to login or leave you on a dead screen.
  - Back from checkout after placing an order.
  - Opening an order from a notification, with a sensible back stack.
- Remove any placeholder routes still in use.
- Test both roles: tap fast through every tab, open things, go back, rotate the phone.

**Done when:** both roles can go anywhere and back with no crash and no wrong screen. List every error you found and how you fixed it.

## Phase 3: Logging and code references

The brief says: "Make use of logging to show your understanding of your code." There's none at the moment, only OkHttp's network log.

- Add `Log.d` for the normal flow and `Log.e` for failures, with a `TAG` per class starting with `Rm` (for example `RmAuth`, `RmCheckout`).
- Cover the repositories, the sign-in, checkout, queue and top-up ViewModels, `SyncWorker`, the Firebase messaging service and Google sign-in.
- **Never log** tokens, passwords, keys or full email addresses.
- Wherever code follows a guide (Android Developers, Supabase, Firebase), add a comment with an IIE Harvard reference. Make sure each one is also in the README References.

**Done when:** filtering Logcat by `Rm` tells a readable story of signing in and placing an order.

## Phase 4: Switch on Google sign-in

**NEED YOU first.** The steps are in [[What Dylan has to do himself]]. Give Dylan the debug and release SHA-1 from `./gradlew signingReport` before he makes the Android client ID.

- Put `GOOGLE_WEB_CLIENT_ID` in `local.properties`, and add it as a GitHub secret passed through `build.yml`, so the release APK shows the button too.
- Test on the phone:
  - A new Google account gets the student-number screen, then the student app.
  - An existing account goes straight in.
  - Cancelling the picker shows no error.

**Done when:** Google sign-in works on the release APK, not only from Android Studio.

## Phase 5: Switch on push notifications

**NEED YOU first:**

- `google-services.json` in `app/`
- Supabase secrets `FCM_SERVICE_ACCOUNT` and `FCM_PROJECT_ID`
- GitHub secret `GOOGLE_SERVICES_JSON` (base64)

Then:

- Make `build.yml` write `google-services.json` from the secret before it builds.
- Test on the phone:
  - Staff mark an order ready, and the student's phone gets a push within about 5 seconds, even with the app closed.
  - Tapping it opens that order.
  - Low stock alerts reach staff.

**Done when:** both pushes arrive on the phone and CI is still green.

## Phase 6: Facebook login (extra)

Only once Phases 0 to 5 are done. **It earns no extra marks.** SSO is 10 marks with one provider or two.

**NEED YOU first:** the Meta developer app and the Supabase Facebook provider. The steps are in [[What Dylan has to do himself]].

- Use Supabase OAuth with a browser redirect: `signInWith(Facebook)`, a deep link `rimagwinya://login-callback`, an intent filter, and `handleDeeplinks`.
- A first Facebook sign-in gets the student-number screen, the same as Google.
- The Meta app stays in development mode, so only Dylan's account and any added testers can sign in. That's fine for the video. Say so in the README.
- **If it isn't working within 45 minutes, hide the button and move on.**

## Phase 7: README, release notes, AI write-up and Play Store

- Version **1.1.0** (versionCode 4).
- **README rewrite.** It's the report, so it needs:
  - What the app is for and who it's for
  - Design decisions, with screenshots
  - How we used GitHub and GitHub Actions
  - Features 1 to 5, with where to look in the code: [[Part 3 - Final PoE]] has 4 and 5
  - The PoE features: SSO, offline, push, languages
  - How to run it, test accounts, and a note that staff accounts are made by the manager
  - A placeholder for the video link
  - References
- **Release notes**, in the README, from the prototype onwards:
  - Part 1 prototype
  - v1.0.x for Part 2
  - v1.1.0 for Part 3, naming clearly which new features this version adds
- **AI write-up**, 500 words max, in `docs/AI-use.md` and linked from the README. Say honestly how Claude and Claude Code were used (planning, code, debugging, docs) and how that's cited in IIE Harvard style. **Dylan reads and edits it before it's handed in.**
- **`docs/play-store/`:**
  - The store listing and the POPIA privacy policy, back in git
  - The list of phone screenshots needed
  - A 1024×500 feature graphic and the 512×512 icon
  - A signed AAB from `bundleRelease`

## Phase 8: Final checks and the tag

- All tests pass and CI is green.
- A GitHub Release called "Final PoE (v1.1.0)", with the signed APK and AAB attached.
- **Wait for Dylan's video link.** Put it in the README, commit, then tag that last commit `final-poe` and push the tag. Git tags can't have spaces.
- Open the repo, the release and the video in an incognito window to check they're all public.
