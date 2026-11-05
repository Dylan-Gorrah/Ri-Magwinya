# Links and facts

Back to [[Ri-magwinya]]

Everything you'll paste into the submission doc, the README or a Claude Code session.

## Links

| What | Where |
|---|---|
| GitHub repo | https://github.com/Dylan-Gorrah/Ri-Magwinya |
| APK downloads | https://github.com/Dylan-Gorrah/Ri-Magwinya/releases |
| Browser version | https://dylan-gorrah.github.io/Ri-Magwinya/ |
| Part 2 demo video | https://youtu.be/Z6qdyHUlznk |

## Test accounts

These are already public in the README.

| Role | Email | Password |
|---|---|---|
| Student | `user123@gmail.com` | `FrogybyD1` |
| Staff | `admin@gmail.com` | `Admin1234!` |

## The app

| | |
|---|---|
| Local project folder | `C:\Users\Dylan\Desktop\RI-magwinya` |
| Package | `com.rimagwinya.app` |
| Version | 1.0.2 locally (versionCode 3). GitHub is still on 1.0.1 |
| Min and target SDK | 24 (Android 7) and 36 |
| Supabase project | `jykswltsegssjmvnqqbi` |
| Supabase URL | `https://jykswltsegssjmvnqqbi.supabase.co` |
| Unit tests | 172 `@Test`s locally (169 on GitHub), plus 35 SQL checks in `supabase/tests/` |
| Build | `.\gradlew.bat assembleDebug` |
| Tests | `.\gradlew.bat testDebugUnitTest` |

## What goes in `local.properties`

It's gitignored, so it's never on GitHub.

```properties
sdk.dir=C\:\\Users\\Dylan\\AppData\\Local\\Android\\Sdk
SUPABASE_URL=https://jykswltsegssjmvnqqbi.supabase.co
SUPABASE_ANON_KEY=<publishable key: Supabase, Project Settings, API keys>
GOOGLE_WEB_CLIENT_ID=<for Google sign-in>
RELEASE_STORE_FILE=C:/path/to/rimagwinya.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=rimagwinya
RELEASE_KEY_PASSWORD=...
```

Only the **publishable** key ever goes in the app. The secret key stays in Edge Function secrets.
