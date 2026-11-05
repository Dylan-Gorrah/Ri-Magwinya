# Logging

Back to [[Ri-magwinya]] · [[Part 3 phases]]

Added 5 Nov for Part 3. The brief asks for `Log.d` and `Log.e` "to show your understanding of your code".

## How to read it

In Android Studio, open **Logcat** and type `Rm` in the filter. Every class logs under its own tag starting with `Rm`:

| Tag | What it tells you |
|---|---|
| `RmAuth` | Sign-up, sign-in, Google token exchange, sign-out, profile loads |
| `RmSession` | Session changes and which half of the app it routes to |
| `RmLogin`, `RmRegister`, `RmGoogle` | The sign-in screens and the Google account sheet |
| `RmMenu` | Menu loaded from Supabase, or from the cache when offline |
| `RmCheckout`, `RmOrders` | Placing an order, the order number, refusals like `SLOT_FULL` |
| `RmQueue`, `RmTopUp`, `RmStaff` | Staff moving orders, stock changes, wallet top-ups, sales |
| `RmSync` | Orders saved offline and replayed when the phone is back online |
| `RmPush` | Push tokens and incoming notifications |

`Log.d` is the normal flow. `Log.e` is a failure, usually with the error type.

## What is never logged

Passwords, tokens, keys and email addresses. Sign-in failures are logged by error type only, because Supabase's error text can include the request headers, and those carry the access token.

## References

Code that follows an official guide has an IIE Harvard reference in a comment next to it (Supabase auth, Google sign-in, WorkManager, Firebase messaging, the splash screen, navigation, `Log`). The same list is in the README under References.
