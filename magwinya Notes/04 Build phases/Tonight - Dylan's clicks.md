# Tonight: Dylan's clicks

Back to [[Ri-magwinya]] · [[What Dylan has to do himself]] · [[Part 3 phases]]

What Dylan does while Claude Code works through Phases 0 to 3. Do the parts in order. Keep a Notepad open to paste things into.

## Part A: Firebase (for push), about 10 minutes

1. Go to **console.firebase.google.com** and sign in with your Google account.
2. Click **Create a project**. Name it `Ri-magwinya`, then click **Continue** and accept the terms.
3. Turn off the Gemini AI help and Google Analytics, then click **Create project**. Wait, then click **Continue**.
4. On the project home page, click the **Android** icon to add an app.
5. **Package name:** `com.rimagwinya.app`, typed exactly like that. **Nickname:** `Ri-magwinya`. Leave SHA-1 empty. Click **Register app**.
6. Click **Download google-services.json**. Move it into `Desktop\RI-magwinya\app\`, the same folder that has `build.gradle.kts` in it. Check the name is exactly `google-services.json`, not `google-services (1).json`.
7. Click **Next**, **Next**, then **Continue to console**. Claude Code does the code part.
8. Click the gear icon next to "Project Overview", then **Project settings**. On the **General** tab, copy the **Project ID** (something like `ri-magwinya-1a2b3`) into Notepad.
9. Open the **Cloud Messaging** tab. "Firebase Cloud Messaging API (V1)" should say **Enabled**. If it doesn't, click the three dots next to it, open it in Google Cloud, and click **Enable**.
10. Open the **Service accounts** tab, click **Generate new private key**, then **Generate key**. A `.json` file downloads.
    **Don't** put this file in the project folder, and don't paste it into any chat. It only goes into Supabase in Part B.

## Part B: Supabase secrets, about 3 minutes

11. Go to **supabase.com/dashboard** and open the Ri-magwinya project.
12. In the left menu, click **Edge Functions**, then **Secrets**.
13. Add a secret. **Name:** `FCM_PROJECT_ID`. **Value:** the Project ID from step 8.
14. Add another one. **Name:** `FCM_SERVICE_ACCOUNT`. **Value:** open the downloaded `.json` file in Notepad, press Ctrl+A then Ctrl+C, and paste the whole thing.
15. Click **Save**. Then move that `.json` file out of Downloads to somewhere safe.

## Part C: Google Cloud (for Google sign-in), about 10 minutes

You need the two SHA-1 codes from Claude Code for this part. It prints them first thing.

16. Go to **console.cloud.google.com**. In the project picker at the top, choose the **Ri-magwinya** project. Firebase made it in Part A.
17. Search for **Google Auth Platform** at the top and open it. Click **Get started**.
18. **App name:** `Ri-magwinya`. **User support email:** your Gmail. Click **Next**.
19. **Audience:** choose **External**, then **Next**. **Contact email:** your Gmail, then **Next**. Tick the agreement, then **Continue**, then **Create**.
20. In the left menu, click **Audience**, then **Publish app**, then **Confirm**. Without this, only test users can sign in.
21. In the left menu, click **Clients**, then **Create client**.
    - **Application type:** Web application
    - **Name:** `Ri-magwinya Web`
    - Click **Create**.
    - Copy the **Client ID** and the **Client secret** into Notepad. The secret is only shown once.
22. Click **Create client** again.
    - **Application type:** Android
    - **Name:** `Ri-magwinya debug`
    - **Package name:** `com.rimagwinya.app`
    - **SHA-1:** the **debug** one from Claude Code
    - Click **Create**.
23. Do step 22 once more, named `Ri-magwinya release`, with the **release** SHA-1.

## Part D: Switch Google on in Supabase, about 2 minutes

24. In the Supabase dashboard, click **Authentication**, then **Sign In / Providers**, then **Google**.
25. Turn on **Enable Sign in with Google**.
    - **Client IDs:** paste the Web Client ID from step 21.
    - **Client Secret:** paste the secret from step 21.
26. Click **Save**. Leave "Skip nonce checks" off. Only turn it on if Google sign-in fails with a "nonce" error.

## Part E: Tell Claude Code

27. Paste this into Claude Code, with your Web Client ID filled in:

```
Firebase and Google are set up.
- google-services.json is in app/
- Supabase secrets FCM_PROJECT_ID and FCM_SERVICE_ACCOUNT are saved
- Supabase Google provider is on
- Web client ID: PASTE-IT-HERE
Do Phase 4 and Phase 5 when you're done with the current phase.
```

The Web client ID isn't secret, so it's fine to paste. The client secret and the service account JSON only ever go into Supabase.
