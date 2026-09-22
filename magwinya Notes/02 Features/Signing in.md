# Signing in

Back to [[Ri-magwinya]]

Three screens — welcome, login, register — and one important idea underneath
them.

## Welcome

Brand mark, the name, and two cards: *I'm a student* and *I'm staff*.

**Those cards decide nothing.** They pick which sign-in copy you see and
that's all. Your actual role comes from your row in the database when you
sign in. Tap the staff card with a student account and you land on the student
menu.

The prototype set the role from the card. That's a demo shortcut. See
[[The two roles]].

No pre-filled credentials, no "switch role" button, no reset. Those were demo
controls and they're gone.

## Register

Four fields:

| Field | Rule |
|---|---|
| Full name | Required |
| Email | Must be a well-formed email address. **No domain restriction** |
| Student number | Required, and unique across all accounts |
| Password | 8 characters minimum |

Validation runs as you type, and the button stays disabled until all four are
good. Errors appear under the field they belong to, not as a single message at
the bottom.

A POPIA banner sits above the button explaining what's stored and why, with a
link to the full privacy page. South African law, and it needs to be visible
before the person commits, not buried in settings.

### No college domain

Students use ordinary personal accounts — Gmail and the like — so there's
nothing to check against. The prototype's `@rcconnect.edu.za` rule is gone,
along with `ALLOWED_EMAIL_DOMAIN`.

That makes the **student number** the only thing tying an account to a real
person. It's required here and it's `unique` in the database.

Two consequences, both known and accepted:

- Anyone can make an account. The wallet is what actually gates things — a
  fake account has R0.00 and can't do anything until a staff member has taken
  real money from a real person. See [[The campus wallet]].
- Student numbers are first-come. If someone claims a number that isn't
  theirs, the genuine student can't register and Dylan has to fix the row by
  hand. Known limitation at one-campus scale.

The duplicate case gets a clear message: *"That student number is already
registered."*

See [[Open decisions]] for the full reasoning.

## Login

Email and password. One error message for a failed sign-in:

> Email or password is incorrect.

Deliberately vague, and always the same regardless of which was wrong.
"No account with that email" tells anyone who asks which addresses have
accounts.

Network failure is a different thing and says so, with a retry.

## What's underneath

`supabase-kt`'s Auth module does sign-up, sign-in, sign-out, session storage
and token refresh.

Passwords are hashed with bcrypt **by Supabase**, on their servers. The app
never hashes anything, never stores a password, never sees one again after the
field is submitted. Rolling your own password handling is how you end up in
the news.

### Staying signed in

The session persists and the refresh token renews it in the background. Kill
the app, reopen it a week later, still signed in. Nobody signs into a tuckshop
app twice.

If biometric unlock is on, the fingerprint prompt gates the app on open — the
session is still there, it just isn't handed over until the prompt passes. See
[[Profile and settings]].

### The token on every request

An OkHttp interceptor attaches the live access token to every call:

```http
apikey: <anon key>
Authorization: Bearer <access token>
```

The anon key is public and does nothing on its own. The bearer token is what
the security rules read to decide what you can see. See [[Security rules]].

Tokens are never logged, in debug or anywhere else.

## After sign-in

Fetch the profile, read `role`, route:

- `student` → the menu
- `staff` → the queue

Server role, every time.

## Making a staff account

Not through the app. There's no "sign up as staff" — it'd be the first thing
anyone tried.

Dylan creates the user in Supabase under *Authentication → Users*, then runs
one line of SQL to flip its role:

```sql
update profiles set role = 'staff' where email = 'someone@example.com';
```

A Phase 3 checkpoint. See [[What Dylan has to do himself]].
