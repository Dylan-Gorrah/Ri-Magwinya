# Claude Code Rules

Back to [[Ri-magwinya]]

How Claude Code works with Dylan on Part 3. The `CLAUDE.md` in the project root points here.

1. **Read first.** Start at [[Ri-magwinya]], then [[Status right now]], [[Part 3 - Final PoE]] and [[Part 3 phases]], then the code. Say in three lines what you understood and what's next. Then wait for "go".
2. **Check the copy before changing anything.** Run `git status` and `git log origin/main..HEAD`, build, and run the tests. Say if the local copy and GitHub don't match.
3. **The app is already built.** Part 3 is mostly switching things on, proving them and writing them up. Don't rewrite what works.
4. **One phase at a time.** Finish it, report, stop. The phases are in [[Part 3 phases]].
   - **Update the notes after every phase.** Tick the Status column in [[Part 3 phases]], update [[Status right now]], and update the feature note for anything you changed. Same plain style as the rest of the vault.
5. **NEED YOU:** when a step needs Dylan (a password, a key, a dashboard, the phone), stop and write `NEED YOU:` with exactly where to click and what to paste back. The list is in [[What Dylan has to do himself]].
6. **Never invent or commit secrets.** `local.properties`, `google-services.json`, keystores and service account JSON stay out of git.
7. **The build stays green.** `./gradlew assembleDebug testDebugUnitTest` passes after every change.
8. **Small commits with clear messages**, like `feat(push): send order_ready to the student`.
9. **Plain, short English.** Use the report below. No essays.
10. **The deadline wins.** If something will take too long, say so and suggest what to cut.

## Report after each phase

```
## Phase N — <name> · DONE | BLOCKED

Built
- 3 to 6 plain bullets

Try it
- What to tap on the phone and what you should see

Tests
- X passing

NEED YOU
- What Dylan has to do, or "nothing"

Commit: ...
Next: ...
```
