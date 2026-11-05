# Part 2: Build the app

Back to [[Ri-magwinya]] · [[The brief in plain English]]

**Submitted 22 Sep 2026 on Turnitin. Mark: not on file yet.**

## What was asked, and the marks

| Rubric row | Marks | What it wanted |
|---|---|---|
| Runs on a real phone | 5 | Builds and opens with no crash. Emulator only caps you at 3. |
| Register and login | 10 | Works, with passwords encrypted |
| Settings | 10 | Settings that fit the app and really save |
| 3 of our own features | 30 | Picked from the Part 1 design, working end to end |
| User interface | 10 | Same look on every screen, in the Part 1 palette |
| REST API and database | 20 | 10 for building it, 10 for using it. Hosted online, not localhost |
| GitHub, tests and README | 10 | Pushed with git (no zip), lots of commits, unit tests, Actions passing |
| Demo video | 5 | On a real phone, voice-over all the way, unlisted on YouTube |

Also checked, with no marks of their own:

- Comments, IIE Harvard references in the code, and logging (`Log.d` and `Log.e`).
- One external library and one SDK named in the README.
- An AI write-up, 500 words max.
- The PoE features (SSO, offline, push, languages) were worth 0 here.

## What we handed in

| Row | What we did | Where to look |
|---|---|---|
| Runs on a phone | A signed release APK built by Actions and put on Releases (v1.0.1) | GitHub Releases |
| Register and login | Supabase Auth (bcrypt), confirm password, email confirmation handled, role comes from the server | `feature/auth/` |
| Settings | Theme, notifications, fingerprint, language, change password, edit name and phone, privacy page. Saved in DataStore and `profiles` | `feature/profile/` |
| 3 features | 1. Live stock menu with server-priced ordering. 2. Campus wallet. 3. Staff order queue | README table |
| UI | One theme, navy, sky and gold, shared components | `core/designsystem/` |
| API and database | 11 tables, RLS on all of them, 15 migrations, our own functions (`place_order`, `set_order_status`, `load_wallet`, `adjust_stock`, `sales_summary`), 3 Edge Functions | `supabase/` |
| GitHub and tests | 38 commits, 169 unit tests plus 35 SQL checks, `build.yml` runs the tests and builds the APK | `.github/workflows/build.yml` |
| Video | https://youtu.be/Z6qdyHUlznk | README |
| Extra | A browser version on GitHub Pages | https://dylan-gorrah.github.io/Ri-Magwinya/ |

**Library and SDK:** Retrofit is the external library. Supabase is the SDK.

## What was left switched off

All built in Part 2, but off until they're set up. These are the Part 3 ones. See [[Part 3 - Final PoE]].

- **Push:** needs a Firebase project.
- **Google sign-in:** needs a Google Web client ID.
- **Languages:** working, but machine translated.

## Risks to carry into Part 3

- All 38 commits are Dylan's. See [[Status right now]].
- The test count doesn't match across the README, GitHub and the 13 Oct overview.
- `docs/PROGRESS.md`, `qa-checklist.md`, `store-listing.md` and `privacy-policy.md` were taken out of git on 23 Sep. They should still be on your PC. Part 3 will probably want the privacy policy and the store listing back.
