# Notifications

Back to [[Ri-magwinya]]

The one that makes the whole app work. Without it, a student has to keep
opening the app to check whether their food is ready — which is just queueing
with extra steps.

Phase 11, and one of the four the PoE marks separately.

## The one that matters

**Order ready.**

```
  Ri-magwinya
  Order #1047 is ready
  Collect at the tuckshop. Your code is 4729.
```

The code is in the body on purpose, so it's readable from the lock screen
without unlocking anything. See [[The collection code]].

Tapping it deep-links straight to that order, not to the menu.

## The rest

| Type | When | To |
|---|---|---|
| `order_ready` | staff tap **Mark ready** | that student |
| `order_placed` | an order goes through | that student, as confirmation |
| `wallet_topup` | staff load money | that student |
| `low_stock` | an item drops to its reorder level | staff |

Three channels so people can silence one without losing the others:

- **Orders** — high importance, makes a sound. This is the one that's time
  sensitive.
- **Wallet** — default importance.
- **Stock** — staff only, low importance, no sound. Useful, not urgent.

## Two halves

### The push

Firebase Cloud Messaging. Reaches the phone whether the app is open, backgrounded
or closed.

Sent from **inside the Edge Functions**, never from the app. The FCM
credentials are server secrets and would be extractable from an APK in
minutes.

`order-status` sends it as part of handling the status change — set `ready`,
look up the student's `fcm_token`, send. If sending fails, the status change
still stands. A missed notification is annoying; a status change that rolls
back because a notification failed is a broken app.

### The in-app list

A `notifications` table read from the bell in the menu hero, opening a sheet.

```sql
notifications
  id       uuid
  user_id  uuid
  title    text
  message  text
  type     text     -- order_placed | order_ready | low_stock | wallet_topup
  is_read  boolean  default false
  order_id uuid
  sent_at  timestamptz
```

Rows get written whether or not the push was delivered, so a student who had
notifications off, or no signal, still finds out what happened. The bell shows
an unread count; opening the sheet marks them read.

Students can update `is_read` on their own rows and nothing else. See
[[Security rules]].

## Tokens

Each install gets an FCM token, stored on the profile:

```http
PATCH /rest/v1/profiles?id=eq.<id>
{ "fcm_token": "…" }
```

Written at sign-in and again whenever `onNewToken` fires — tokens rotate on
reinstall, restore, and sometimes for no visible reason. A stale token means
silent failure, so it's refreshed rather than assumed.

Signing out clears it, so the next person on that phone doesn't get the last
person's order notifications.

## Asking permission

Android 13 and up needs `POST_NOTIFICATIONS`, granted by the user.

It isn't asked for on first launch. A permission dialog before the person
knows what the app does gets denied, and Android only lets you ask twice. The
ask comes **after the first order is placed**, with a line of explanation
first:

> Let us tell you when your order is ready, so you don't have to keep
> checking.

Denied is fine. The order screen updates live over Realtime anyway — the
student just has to have the app open. See [[Order statuses]].

## The settings toggle

Profile → Notifications. Off means the app doesn't post the notification even
if the push arrives, and the in-app list still fills up normally.

Separate from the Android permission, which is the system's business. See
[[Profile and settings]].

## Setup — Dylan's job

Phase 11. See [[What Dylan has to do himself]].

1. Create a Firebase project, add an Android app with package
   `com.rimagwinya.app`, download `google-services.json` into `app/`
2. *Project settings → Service accounts → Generate new private key*
3. Set two Supabase Edge Function secrets: `FCM_SERVICE_ACCOUNT` (the whole
   JSON) and `FCM_PROJECT_ID`

`google-services.json` is gitignored. CI writes it from a base64 secret,
because the Google Services plugin fails the build without it. See
[[Testing and CI]].

## The bit that's easy to get wrong

The FCM sender is written in **Phase 5**, six phases before the secrets exist.

It has to do nothing quietly when `FCM_SERVICE_ACCOUNT` isn't set. If a
missing secret threw, placing an order would fail for six phases of
development for a reason that has nothing to do with placing orders.
