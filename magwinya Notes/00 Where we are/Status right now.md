# Status right now

Back to [[Ri-magwinya]]

Updated Thu 5 Nov 2026, evening.

## The short version

- **The app is built.** All 16 build phases were done on 22 and 23 Sep for Part 2. That included the four PoE features (single sign-on, offline, push, languages). They were built early, but some are switched off until they get set up.
- **Part 3 is the final PoE, due today (5 Nov).** The exact time still needs confirming. The brief and rubric are in [[Part 3 - Final PoE]], with what's left for each rubric row.
- **The plan is in [[Part 3 phases]].** Claude Code works through it in order and ticks it off.
- **So most of Part 3 is switching things on, proving they work, and writing it up.** Very little is building from scratch.

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
- Next: Phase 2 (navigation), then Phase 3 (logging).

## What's not set up yet

Checked in `local.properties` and `app/`:

- **No `GOOGLE_WEB_CLIENT_ID`**, so Google sign-in is switched off.
- **No `google-services.json`**, so push notifications are switched off.
- The Supabase keys and the release signing keys are there.

## Things that don't line up

- **Commits.** Every commit is yours. Refiloe and Achuma have none. In APPR6312 Part 2 that cost marks ("unequal source-control participation"). If the PoE rubric checks this, they each need to push something real.

## Questions for Dylan

1. What time is it due today?
2. Did Part 2 come back with a mark and feedback? Anything the lecturer flagged goes first.
3. Firebase (push) and the Google Web client ID (sign-in) aren't set up. They're the slowest jobs, so start them first. See [[What Dylan has to do himself]].
