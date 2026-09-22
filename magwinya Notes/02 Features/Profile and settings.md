# Profile and settings

Back to [[Ri-magwinya]]

The fourth tab, for both roles. Phase 9, and the module marks it as a required
feature.

## Profile

**The identity card.** Name, email, student number, and a role pill —
`Student` in navy, `Staff` in gold. Gold is one of only three things allowed
to use it. See [[The design system]].

**Students also get:**

- Wallet balance, with the full transaction history behind it. See
  [[The campus wallet]]
- Loyalty progress — *7 of 10*, display only. See [[Loyalty stamps]]

**Staff get neither.** No wallet, no stamps. Staff use the app to run the
tuckshop, not to buy from it.

## Settings

| Setting | What it does |
|---|---|
| Language | English, Afrikaans, Sesotho. See [[Languages]] |
| Theme | System, Light, Dark |
| Order notifications | Whether the app posts notifications. See [[Notifications]] |
| Biometric unlock | Fingerprint on open |
| Change password | Through Supabase Auth |
| Privacy and POPIA | The full policy, in the app |
| Sign out | Clears the session and the FCM token |

### Where they're stored

**DataStore**, on the phone, for theme, notifications and biometrics. They're
device settings — someone might want dark mode on their phone and not on a
tablet, and the app needs to know the theme before any network call so it
doesn't flash white on launch.

**The server**, for language, because it should follow the student to a new
phone and because notification text may one day be sent in it.

### Theme

Three options, and **System** is the default. The app follows the phone unless
told otherwise.

Switching is instant — the colour scheme is a Compose state, not a restart.
Both schemes are defined in full in the design system, so nothing is an
afterthought in dark mode.

## Biometric unlock

Off by default. On, and the app asks for a fingerprint or face every time it
opens.

Important: this is a **lock on an existing session**, not a way of signing in.
The session is already valid — `BiometricPrompt` decides whether to hand it
over. It isn't authentication against the server, it's a door on the app.

It's there because the wallet has real money in it, and because phones get
left on desks. The whole point of the app is that four digits and a tap get
you food.

```kotlin
BiometricPrompt(...).authenticate(promptInfo)
```

With `androidx.biometric`, which handles the fingerprint/face/none differences
across the range of phones this has to run on.

**Password is always the fallback.** Fingerprints fail — wet hands, a cracked
sensor, a phone with no sensor at all. If the prompt can't be satisfied the
student can sign in with their password instead. An app that can lock you out
of your own money because of a sensor is worse than one with no biometrics.

## Change password

Handed to Supabase Auth. The app collects the new password, checks it's 8
characters, and sends it. It never sees the old one, never hashes anything.
See [[Signing in]].

## Privacy and POPIA

A real page with real text, not a link to a website.

POPIA is South Africa's data protection law, and this app stores names, email
addresses, student numbers and a purchase history tied to a person. That needs
saying plainly: what's collected, why, who sees it, how long it's kept, and
how to get it deleted.

The same text ships as markdown for the Play Store listing in Phase 16, so
there's one version of it rather than two that drift.

A short POPIA banner also appears above the register button, because consent
belongs before the account is made, not in a settings screen nobody opens.

## Sign out

Clears the Supabase session, clears the local cache, and **clears
`profiles.fcm_token`**.

That last one matters on a shared or handed-down phone. Leave the token
behind, and the next person gets notifications about the last person's orders.
