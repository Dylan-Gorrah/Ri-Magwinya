# The brief in plain English

Back to [[Ri-magwinya]]

**Module:** OPSC6312 Open Source Coding (Intermediate)
**Group:** Dylan Gorrah (ST10398445), Refiloe Matsau (ST10461896), Achuma Welemtsha (ST10447955)

The PoE is one full cycle: research, plan, design, build, evaluate. It's handed in three times.

| Hand-in | What it covers | Due |
|---|---|---|
| [[Part 1 - Research and design]] | Research, planning and design | 24 Aug |
| [[Part 2 - Build the app]] | Build the core app | 22 Sep |
| [[Part 3 - Final PoE]] | Finish it, polish it, evaluate it | 5 Nov |

## What every app must have

An app of our choice. It has to use an API, either one we find online or one we make ourselves.

| Feature | When it's marked | How we did it |
|---|---|---|
| Register and log in, with the password encrypted | Part 2 | Supabase Auth hashes with bcrypt. See [[Signing in]] |
| Change settings in the app | Part 2 | See [[Profile and settings]] |
| A REST API we create, connected to an online database | Part 2 | Supabase PostgREST plus our own functions. See [[Part 2 - Build the app]] |
| Single sign-on | PoE | Google. See [[Google sign-in]] |
| Offline mode with sync, using Room or SQLite | PoE | Room plus WorkManager. See [[Offline mode]] |
| Real-time push notifications | PoE | Firebase Cloud Messaging. See [[Notifications]] |
| At least two South African languages | PoE | Afrikaans and Sesotho. See [[Languages]] |

**For the end, as well:** the app must be ready to publish on the Google Play Store, and it must use unit tests with GitHub Actions running them.

## Why Sesotho

It's the most spoken home language in the Free State, so it's the right one for a Bloemfontein campus. That's the reason we gave in Part 1.
