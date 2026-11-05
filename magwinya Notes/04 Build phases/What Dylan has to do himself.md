# What Dylan has to do himself

Back to [[Ri-magwinya]] · [[The build plan]]

Claude Code can't do anything that needs a password, a secret, a dashboard click or the phone in your hand. This is every one of those in one list. The statuses come from the 23 Sep notes, so update them with what you've done since.

## Why Firebase and Google Cloud, when we already have Supabase

- **Firebase** is only used for push. Supabase stores the data and does the login, but it can't wake up a phone. On Android, push notifications are delivered by Google's service, Firebase Cloud Messaging. Supabase tells Firebase "send this to that phone", and Firebase delivers it, even when the app is closed. It's free, and the data stays in Supabase.
- **Google Cloud** is where Google makes you register the app before "Sign in with Google" works. You get a client ID, which tells Google the sign-in request really comes from Ri-magwinya. It's free.
- **Do them in one go.** Make the Firebase project first, because it creates a Google Cloud project automatically. Then open console.cloud.google.com, pick that same project, and make the client IDs there. One project covers both jobs.

## Left over from Part 2

| Job | Where | Status |
|---|---|---|
| Rotate the Supabase secret key, because it was pasted into a chat | Supabase, Project Settings, API keys | Check |
| Switch "Confirm email" off | Supabase, Authentication, Sign In / Providers, Email | Check |
| Launcher icon | Android Studio, File, New, Image Asset | Check |
| Staff account | SQL editor | Done (`admin@gmail.com`) |
| GitHub repo | github.com | Done |
| Supabase GitHub secrets `SUPABASE_URL`, `SUPABASE_ANON_KEY` | GitHub, Settings, Secrets and variables, Actions | Check |
| Release keystore and the 4 `RELEASE_*` secrets | Android Studio, Build, Generate Signed App Bundle | Probably done, since v1.0.1 was set up to publish signed |

## For Part 3

| Job | Where | Why |
|---|---|---|
| Make a Firebase project and add an Android app with package `com.rimagwinya.app`. Download `google-services.json` into `app/` | console.firebase.google.com | Push notifications |
| Generate a service account key. Put the whole JSON in Supabase secret `FCM_SERVICE_ACCOUNT`, and the project ID in `FCM_PROJECT_ID` | Firebase, Project settings, Service accounts. Then Supabase, Edge Functions, Secrets | So the server can send the pushes |
| Add `google-services.json` (base64) as GitHub secret `GOOGLE_SERVICES_JSON` | GitHub, Settings, Secrets | So CI still builds once Firebase is on |
| OAuth consent screen, a Web client ID, and an Android client ID with the SHA-1 from `./gradlew signingReport` | console.cloud.google.com | Google sign-in |
| Add the **release** SHA-1 to the Android client ID as well as the debug one | Same place | Without it, Google sign-in fails on the release APK even though it works from Android Studio |
| Switch on the Google provider and paste the Web client ID and secret | Supabase, Authentication, Providers, Google | Google sign-in |
| Put `GOOGLE_WEB_CLIENT_ID=...` in `local.properties`, and as a GitHub secret if the CI build should have it | Project root | The Google button only shows when this is set |
| Get a first-language speaker to read the Afrikaans and Sesotho | `res/values-af`, `res/values-st` | Machine translation has mistakes |
| Record the new video on the phone | | |
| Upload it unlisted, then check it opens in an incognito window | YouTube | A private video can't be watched by the lecturer |

## Facebook login (extra, no marks)

Only after Google sign-in and push work. Give up on it after 45 minutes.

| Job | Where |
|---|---|
| Sign in with your Facebook account and register as a developer. It may ask to verify your phone | developers.facebook.com |
| Create an app. Pick "Authenticate and request data from users with Facebook Login", then "Consumer" | Same place |
| Facebook Login, Settings: put `https://jykswltsegssjmvnqqbi.supabase.co/auth/v1/callback` in "Valid OAuth Redirect URIs" | Meta app dashboard |
| App settings, Basic: copy the App ID and App Secret | Meta app dashboard |
| Switch Facebook on and paste the App ID and App Secret | Supabase, Authentication, Providers, Facebook |
| Add `rimagwinya://login-callback` to Redirect URLs | Supabase, Authentication, URL Configuration |

The Meta app stays in development mode, so only your account (and anyone you add as a tester) can sign in with Facebook. That's fine for the video.
