# Progress

Living record of where the build is. Read this and `CLAUDE.md` at the start
of every session.

The plain-English documentation lives in the `magwinya Notes` Obsidian vault.
Start at `Ri-magwinya.md`.

---

## Where we are

**Phase 0 — Project setup · DONE**
**Phase 1 — Foundation · DONE**
**Next: Phase 2 — Backend** (not started, waiting for go)

Supabase MCP connector is configured in `.mcp.json` for project
`jykswltsegssjmvnqqbi`. It needs a Claude Code restart and an OAuth sign-in
before it can be used.

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

## Phase 1 — Foundation · DONE

### Toolchain moved

Kotlin 2.2.10 -> **2.3.20**, to match KSP. KSP moved to standalone versioning
at 2.3.0 and the current 2.3.12 is built against Kotlin 2.3.20; pairing it
with 2.2.10 would have pulled a mismatched stdlib.

### Version catalog

Every library in `CLAUDE.md` section 4 is declared in `libs.versions.toml`,
with versions checked against Google Maven and Maven Central on 2026-09-22
rather than recalled. Key ones: Hilt 2.60.1, Room 2.8.5, Retrofit 3.0.0,
OkHttp 5.5.0, supabase-kt 3.8.0, Ktor 3.6.0, Coil 3.6.3, Vico 3.3.1,
Navigation Compose 2.10.1, desugar_jdk_libs 2.1.5.

**Only what Phase 1 uses is wired into `dependencies`** — Compose,
navigation, Hilt, splashscreen, AppCompat, desugaring and the test stack.
Room, Retrofit, Supabase and the rest are declared and ready, and get their
`implementation` line in the phase that first needs them. Declaring a
dependency costs nothing; including an unused one costs build time.

Retrofit 3.0.0 has an official kotlinx.serialization converter
(`com.squareup.retrofit2:converter-kotlinx-serialization`), so the
JakeWharton one the brief implies is no longer needed.

### Build configuration

- Core library desugaring on, for `java.time` on API 24 and 25
- `buildConfig = true`; `SUPABASE_URL` and `SUPABASE_ANON_KEY` read from
  `local.properties` and default to `""` so the project builds without them
- Java 17 source and target
- Room schema export configured at `app/schemas` ahead of Phase 10

### Design system

`core/designsystem/theme` — `Color.kt` (both palettes as an `RmColors`
CompositionLocal), `Type.kt`, `Shape.kt`, `Spacing.kt`, `Motion.kt`,
`Theme.kt`. No Material You dynamic colour: the palette is the brand, and
letting the wallpaper repaint it would undo the design system.

**Inter is bundled.** Fetched as the variable font from google/fonts, then
instanced to three static weights (400/500/600) with fonttools and subset to
the Latin ranges English, Afrikaans and Sesotho need. 1 MB became 195 KB, and
`tnum` is kept for the tabular price figures. Static rather than variable
because variable weight axes need API 26 and minSdk is 24 — the font would
otherwise render every weight as Regular on Android 7.

### Components

12 previews across 9 files in `core/designsystem/component`:

`RmButton` (primary, secondary, quiet, danger, small) · `RmTextField` ·
`StockPill` · `StatusPill` · `GroupedList` + `ListRow` + `ListDivider` ·
`SkeletonRow` · `Stepper` · `ChipRow` + `Chip` · `ToggleRow` + `RmToggle` ·
`Banner` · `Hud` · `EmptyState` · `RmBottomSheet` · `ProgressRail` ·
`CodeTiles` · `RoleTabBar` · `BrandMark` · `FoodIcon`

### Icons

All 41 `<symbol>` definitions in the prototype were converted to vector
drawables by a script, not by hand: 1 brand mark, 30 UI icons, 10 food icons.
The converter turns `<circle>`, `<ellipse>` and `<rect>` into path data,
inherits symbol-level stroke and fill attributes, and maps `currentColor` to
black so Compose can tint it. Script kept in the scratchpad; the output is
committed and marked "do not hand-edit".

### Navigation

Type-safe routes as `@Serializable` objects in `navigation/Routes.kt`. Auth
graph, student graph, staff graph, every destination reachable with a titled
placeholder. `Profile` is declared once at the root because both roles share
it and two graphs cannot own the same route.

The welcome screen's two role cards currently sign straight in so both graphs
can be walked. **That shortcut is removed in Phase 3** — in the real app the
cards only pick which sign-in copy you see, and the role comes from the
server.

### Other

- `MainActivity` extends `AppCompatActivity` with an AppCompat DayNight
  theme, needed for per-app language in Phase 13
- Splash screen via `core-splashscreen`, brand mark on the app background
- `RimagwinyaApp` with `@HiltAndroidApp`
- All user-facing text in `strings.xml` from the start
- `.gitignore` covers `google-services.json`, `*.jks`, `*.keystore` and
  service-account JSON

### Verified

- `./gradlew assembleDebug` — **BUILD SUCCESSFUL**, no warnings in app code
- `./gradlew testDebugUnitTest` — **8 tests, 0 failures** (`MoneyTest`)
- Debug APK 14.4 MB
- Not yet run on the phone.

### Still owed from Phase 1

- **Launcher icon.** *File -> New -> Image Asset* with Dylan's PNG. The
  template icons are still in place.

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
- **Now:** launcher icon via *File → New → Image Asset*.
- **Now:** restart Claude Code and sign in to the Supabase MCP connector.
- **Phase 2:** Supabase project URL + anon key into `local.properties`, and
  turn off Confirm email under *Authentication → Sign In / Providers*.
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
