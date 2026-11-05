# Status right now

Back to [[Ri-magwinya]]

Updated Thu 5 Nov 2026, evening.

## The short version

- **The app is built.** All 16 build phases were done on 22 and 23 Sep for Part 2. That included the four PoE features (single sign-on, offline, push, languages). They were built early, but some are switched off until they get set up.
- **Part 3 is the final PoE, due today (5 Nov).** The exact time still needs confirming. The brief and rubric are in [[Part 3 - Final PoE]], with what's left for each rubric row.
- **The plan is in [[Part 3 phases]].** Claude Code works through it in order and ticks it off.
- **So most of Part 3 is switching things on, proving they work, and writing it up.** Very little is building from scratch.

## Setup done on 5 Nov, evening

- **Firebase:** project `ri-magwinya` made, Android app `com.rimagwinya.app` registered, Cloud Messaging V1 is on. The service account key was generated.
- **Google sign-in:** switched on through Firebase Authentication, which made the Web and both Android OAuth clients. Debug and release SHA-1s added. The OAuth app is "In production", so any Google account works.
- **`local.properties`:** `GOOGLE_WEB_CLIENT_ID` added.
- **Supabase:** the Google provider is on, with the Web and Android client IDs. No client secret is needed for the in-app sign-in. All three Edge Functions are deployed.
- **Still Dylan's:** paste `FCM_SERVICE_ACCOUNT` into Supabase Secrets, and drag `google-services.json` into `app/`.

The IDs are in [[Links and facts]].

## Where the code is

| Copy | What's in it |
|---|---|
| **`C:\Users\Dylan\Desktop\RI-magwinya`** | **The real app. Work here.** Same 38 commits as GitHub, plus uncommitted work from 13 Oct (see below). Has `local.properties`, `CLAUDE.md`, `CONTINUE.md` and the v1.0.2 APK. |
| GitHub `Dylan-Gorrah/Ri-Magwinya` | The real app as of 23 Sep (v1.0.1). Public. |
| `C:\Users\Dylan\Desktop\Rimagwinya` (no hyphen) | An **empty** Android Studio project (package `com.snokonoko.ri_magwinya`). No app code, no git. Ignore it. |

**Claude Code runs in `Desktop\RI-magwinya`.**

## Tonight's progress (5 Nov)

- **Phase 0 done.** The 13 Oct work is committed (`a197ff7`), and so are the notes. **Not pushed yet:** this PC's active GitHub account is `RI-Dev-Bloom`, which can't push to the repo. Dylan pushes (see [[Part 3 phases]]).
- **Phase 1 done.** Staff screen is email and password only, a student on the staff screen is refused, and six login glitches are fixed. See [[Signing in]]. 182 unit tests and 36 SQL checks pass.
- **Phase 2 done.** Every link and Back button ignores a second fast tap (two taps on Back used to pop the menu too and leave a blank screen). Notification taps open the order for students only, with Back going to the menu. The unused placeholder screen is gone.
- **Phase 3 done.** `Log.d` and `Log.e` across sign-in, checkout, queue, top-up, sync and push, all under `Rm` tags. 9 IIE Harvard references in the code and the README. See [[Logging]].
- **Phase 4 done.** Google sign-in is on in the debug and release builds. CI reads `GOOGLE_WEB_CLIENT_ID` from a GitHub secret. See [[Google sign-in]].
- **Phase 5 done.** Push is on. CI writes `google-services.json` from a secret. New: low-stock alerts to staff from `place-order`. See [[Notifications]].
- **Dylan still has to:** push the commits, add 2 GitHub secrets, deploy `place-order`, and test on the phone.
- **Pushed 5 Nov, 21:43.** `gh` is switched to `Dylan-Gorrah` now.
- **Phase 7 done.** Version 1.1.0 (versionCode 4). README rewritten as the report, with release notes from the prototype to v1.1.0. `docs/AI-use.md` (478 words, **Dylan edits it**). `docs/play-store/` has the listing, privacy policy, screenshot list, 512 icon, feature graphic and the signed AAB (not committed). **The launcher icon was still Android's default**; it's now the brand mark.
- Phase 6 (Facebook) skipped.
- **Both CI runs green** (5 Nov, 21:55), with push on in the CI build.
- Video flow is in [[Video script]].
- **Create account bug fixed.** A taken student number showed "Something went wrong" instead of saying so. See [[Signing in]]. 183 tests.
- **Crash after sign-in fixed.** Push-token fetch crashed the app when Firebase couldn't issue a token. See [[Notifications]].
- **Phase 8 done.** Video: https://youtu.be/WvmOFFofuYo. README has the link and 5 screenshots. CI green. The last commit is tagged `final-poe`, and the GitHub Release "Final PoE (v1.1.0)" has the signed APK and AAB.
- **Left for Dylan:** submit, and open the repo, the release and the video in an incognito window first.

## What's not set up yet

Checked in `local.properties` and `app/`:

- ~~No `GOOGLE_WEB_CLIENT_ID`~~ Done 5 Nov.
- ~~No `google-services.json`~~ Done 5 Nov (it was in the project root; moved to `app/`).
- The Supabase keys and the release signing keys are there.

## Things that don't line up

- **Commits.** Every commit is yours. Refiloe and Achuma have none. In APPR6312 Part 2 that cost marks ("unequal source-control participation"). If the PoE rubric checks this, they each need to push something real.

## Questions for Dylan

1. What time is it due today?
2. Did Part 2 come back with a mark and feedback? Anything the lecturer flagged goes first.
3. Firebase (push) and the Google Web client ID (sign-in) aren't set up. They're the slowest jobs, so start them first. See [[What Dylan has to do himself]].
