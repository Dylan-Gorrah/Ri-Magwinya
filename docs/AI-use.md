# How AI was used

> **Draft for Dylan to edit before hand-in.** It describes what happened in
> this project; check every line against your own memory of the work.

## Tools

- **Claude** (Anthropic, 2026a), a large language model, used in chat.
- **Claude Code** (Anthropic, 2026b), the same model working in the
  terminal, with access to the project folder. It could read and edit files,
  run Gradle builds and tests, and run git, but only with my approval.

## What it was used for

**Planning.** I wrote the design in Part 1 myself: the screens, the two roles,
the menu and the business rules. I turned that into a build brief
(`docs/BUILD_BRIEF.md`) and a phase plan, and used Claude to check it for
gaps and order the work into phases.

**Writing code.** Claude Code wrote most of the Kotlin, the SQL migrations
and the Edge Functions, one phase at a time, from my brief. After each phase
I ran the app on my phone, checked it against the prototype and the brief,
and asked for changes. Decisions that needed a person stayed with me:
the stack, the Supabase project, the payment rules, and setting up Firebase
and Google Cloud.

**Debugging.** In Part 3 I reported login glitches: staff sometimes landed in
the student app. Claude Code traced it to a slow profile request finishing
after the next sign-in, explained the cause, fixed it, and added tests that
fail without the fix. It also found a crash risk with fingerprint unlock on,
and Back buttons that popped two screens on a double tap.

**Tests.** It wrote most of the 182 unit tests and the SQL checks. I read
them to make sure they test the real rules from the brief (for example the
pricing check values), not just whatever the code happened to do.

**Documentation.** It drafted the README, the release notes, the store
listing, the privacy notice and this write-up. I edited them.

## What I checked myself

- I tested the app on my phone after the changes, not only the unit tests.
- No secret is in the code or in git. Keys, the signing key and
  `google-services.json` stay on my PC (`local.properties` is gitignored),
  and CI gets them from GitHub secrets.
- References in the code were checked by opening each page. Where code
  follows a guide, the guide is cited next to it and in the README.
- I can explain each feature, and the logging (`Rm` tags in Logcat) shows
  the flow I describe in the video.

## Limits I noticed

The AI was sometimes confidently wrong, for example about which Java version
a build was using, and needed its claims checked by running the build. It
can't test on a phone, so every "done" still needed me on the device.
Machine-written Afrikaans and Sesotho strings need a first-language speaker.

## How it is cited

In IIE Harvard style. AI help is acknowledged here and in the README's
References, rather than on every line of code, because it was used across
the whole project.

## References

Anthropic. 2026a. *Claude* [Large language model]. Available at:
<https://claude.ai> [Accessed 5 November 2026].

Anthropic. 2026b. *Claude Code.* [Online]. Available at:
<https://www.anthropic.com/claude-code> [Accessed 5 November 2026].
