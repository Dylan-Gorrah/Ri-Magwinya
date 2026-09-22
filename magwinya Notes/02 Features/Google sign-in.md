# Google sign-in

Back to [[Ri-magwinya]]

One tap instead of another password. Built in Phase 12, and one of the four
things the PoE marks separately.

## What the student sees

A **Continue with Google** button on both login and register.

Tap it, Android shows the accounts already on the phone, pick one, done. No
browser, no redirect, no typing.

If it's a brand new account, one extra screen appears before they go through:

```
  Almost there

  What's your student number?
  [ ST2024001            ]

  We use this to find you at the counter.

  [ Continue ]
```

That's the only extra step, and only ever once.

## Any Google account works

No domain restriction. Students use personal Gmail accounts, so there's
nothing to check against. See [[Open decisions]].

Which means the student number screen isn't optional decoration — it's the
only thing connecting a Google account to a person the tuckshop can identify.
Same rules as the register form: required, unique, and *"That student number
is already registered"* if it's taken. See [[Signing in]].

## How it actually works

Three steps, and the middle one is the part worth understanding.

**1. Credential Manager.** The modern Android API
(`androidx.credentials` plus the Google ID helper). It's what draws the
account picker. Replaces the old Google Sign-In SDK, which is deprecated.

**2. A Google ID token comes back.** A signed JWT that says *Google confirms
this is dylan@gmail.com*. Signed by Google, so it can't be faked, and it
expires quickly.

**3. Supabase takes the token.**

```kotlin
supabase.auth.signInWith(IDToken) {
    idToken = googleIdToken
    provider = Google
}
```

Supabase verifies the signature against Google's public keys, and if it checks
out, creates or finds the user and issues its own session. From that point on
it's an ordinary session — same token on every request, same security rules,
nothing downstream knows or cares how it started.

The app never handles a Google password and never sees a Google secret.

## First sign-in creates a profile

The `handle_new_user()` trigger fires on the new `auth.users` row exactly as
it does for a password sign-up: role `student`, balance R0.00, name from the
Google profile.

`student_number` is the one thing Google can't supply, which is why the app
asks for it straight after and PATCHes it onto the profile before letting the
student into the menu.

## Setup — Dylan's job

Two dashboards, done once, in Phase 12. See
[[What Dylan has to do himself]].

**Google Cloud Console**

1. OAuth consent screen
2. A **Web** client ID — this is the one Supabase needs, which is confusing,
   because the app is Android. Supabase is the thing doing the verifying, and
   it's a server
3. An **Android** client ID, which needs the SHA-1 fingerprint from:
   ```
   ./gradlew signingReport
   ```

**Supabase** → *Authentication → Providers → Google* → enable, paste the Web
client ID and secret.

### The debug/release SHA-1 trap

The SHA-1 from `signingReport` is your **debug** key. Sign a release build
with a different keystore and Google sign-in stops working, because the
fingerprint no longer matches.

The release keystore gets made in Phase 16, and its SHA-1 has to be added to
the Android client ID as a second fingerprint. Easy to forget, and the symptom
is a sign-in that works perfectly in testing and fails on the installed app.

## Why not Microsoft

It was on the table — if college mail ran on Microsoft 365, Supabase's Azure
provider would have been the better fit. It doesn't. Google it is, and the
Azure option is dropped rather than left half-there.
