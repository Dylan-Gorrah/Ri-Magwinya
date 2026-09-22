# Progress

Living record of where the build is. Read this and `CLAUDE.md` at the start
of every session.

The plain-English documentation lives in the `magwinya Notes` Obsidian vault.
Start at `Ri-magwinya.md`.

---

## Where we are

**Phase 0 — Project setup · DONE**
**Next: Phase 1 — Foundation** (not started, waiting for go)

---

## Phase 0 — Project setup · DONE

Dylan created the project from **Empty Activity** (Compose, not Views) and
Claude brought it in line with the brief.

### The generated project

| | |
|---|---|
| AGP | 9.2.1 |
| Gradle | 9.4.1 |
| Kotlin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| Java | 11 |
| compileSdk | **37** |
| targetSdk | 36 |
| minSdk | **24** |
| Package | **com.rimagwinya.app** |

### What was changed after generation

1. **Package renamed** `com.snokonoko.ri_magwinya` → `com.rimagwinya.app`.
   Source folders, package declarations, imports, `namespace`,
   `applicationId`, and the packageName assertion in
   `ExampleInstrumentedTest`. Dylan confirmed. Done now because Firebase
   (Phase 11) and the Google OAuth Android client (Phase 12) both get tied
   to it later.
2. **minSdk 33 → 24**, per the brief. Dylan confirmed.
3. **compileSdk 36.1 → 37.** Not a preference — the generated project
   didn't build. `androidx.core:core-ktx:1.19.0` and
   `androidx.lifecycle:lifecycle-runtime-compose:2.11.0` both require
   compiling against API 37. AGP downloaded the platform itself.
4. **`mipmap-anydpi` → `mipmap-anydpi-v26`.** `<adaptive-icon>` needs API
   26+, so at minSdk 24 resource linking failed. The density `.webp` files
   cover API 24–25. Image Asset Studio will regenerate this correctly in
   Phase 1 anyway.
5. **`docs/` created** and `rimagwinya-prototype.html` moved into it, which
   is where `CLAUDE.md` says it lives.

### Verified

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL**
- `./gradlew testDebugUnitTest` — **BUILD SUCCESSFUL** (1 template test)
- Not yet run on the phone. Dylan should press Run once to confirm.

---

## Decisions settled

See `magwinya Notes/04 Build phases/Open decisions.md` for the reasoning.
`CLAUDE.md` section 12 has been rewritten to match.

| | |
|---|---|
| Package | `com.rimagwinya.app` |
| min SDK | 24 |
| SSO | **Google.** Microsoft/Azure dropped |
| Email domain | **None.** Students use ordinary Gmail accounts |
| Identity | The **student number**, required and `unique` |
| Loyalty | **Progress only.** No redemption |
| Pay at counter | Requires **one prior wallet top-up**, plus the existing two-no-show block |

### Why the counter rule exists

Sign-up is open — any email, and nothing verifies a typed student number. A
throwaway account could book stock and slot capacity having paid nothing.
Requiring one prior top-up means staff have physically seen that person at
the speed point. `CLAUDE.md` section 12.1.

---

## Dylan still owes

- **Now:** press Run once, confirm the app opens on the phone.
- **Now:** create the GitHub repo `ri-magwinya` and add the remote. The local
  repo is committed and ready.
- **Phase 1:** launcher icon via *File → New → Image Asset*.
- **Phase 1:** Inter font files into `res/font`, if Claude can't fetch them.
- **Phase 2:** Supabase project URL + anon key, and whether the Supabase MCP
  connector is available.
- Later phases: Firebase (11), Google Cloud OAuth (12), a first-language
  review of the Afrikaans and Sesotho (13), GitHub secrets (15), the upload
  keystore (16).

Full list: `magwinya Notes/04 Build phases/What Dylan has to do himself.md`
(not written yet).

---

## Documentation status

The Obsidian vault has 28 notes, ~17,800 words.

| Batch | What | Status |
|---|---|---|
| 1 | Hub, the app, the two roles, both day walk-throughs, the rules, the build plan, open decisions | **Done** |
| 2 | All 20 feature notes | **Done** |
| 3 | Under the hood — stack, architecture, Supabase, tables, pricing, security, design system, testing | Not started |
| 4 | One note per build phase (0–16) | Not started |

---

## Notes for a fresh session

- The build is green. Keep it that way — every phase ends compiling and
  passing tests.
- No user-facing strings in code. `strings.xml` from Phase 1, or Phase 13
  becomes a refactor.
- Money is integer cents in the domain layer. Never `Double`.
- `local.properties` holds the Supabase keys from Phase 2 and is gitignored.
