# Database tests

These run the migrations against a **throwaway local Postgres** and exercise
the pricing and the business rules. Nothing here ever touches the real
Supabase project.

The point is that `place_order` is the function that decides what a student
gets charged, and it is not the sort of thing to find out about in
production.

## Running them

Needs a local Postgres. Start a scratch cluster on port 55432:

```bash
PG="/c/Program Files/PostgreSQL/18/bin"
D=/tmp/rm-pgdata
"$PG/initdb.exe" -D "$D" -U postgres --auth=trust -E UTF8
"$PG/pg_ctl.exe" -D "$D" -l /tmp/rm-pg.log -o "-p 55432" start
```

Then:

```bash
bash supabase/tests/run.sh
```

It drops and recreates `rmtest`, applies all ten migrations in order, and
prints a PASS or FAIL per assertion.

To stop and throw it away:

```bash
"$PG/pg_ctl.exe" -D "$D" stop && rm -rf "$D"
```

## What is here

**`shim.sql`** — the parts of Supabase that are not plain Postgres: the
`auth` schema, a cut-down `auth.users`, `auth.uid()`, the `anon` /
`authenticated` roles and the `supabase_realtime` publication. `auth.uid()`
reads a setting so the tests can say *now act as this person*, via
`act_as(uuid)`.

**`pricing_test.sql`** — the check values from section 9.1 of `CLAUDE.md`,
placed as real orders and compared against the expected total. Also checks
`units_consumed` and the generated option label.

| Order | Expected |
|---|---|
| 2 vetkoeks + 2 polony + 1 cheese | R17.00, 2 units |
| 0 vetkoeks + 1 snoek | R10.00, **0 units** |
| 3 Chappies + 1 mix packet | R13.00 |
| Large chips | R40.00 |
| Tea with brand, milk and sugar | R10.00 |
| 4 Cokes | R64.00, 4 units |

**`rules_test.sql`** — the rules that are easy to get wrong:

- pay-at-counter blocked with no prior top-up, allowed after one, blocked
  again after two no-shows
- insufficient funds
- the same `client_ref` twice produces one order and one charge
- out of stock, and a build item with nothing selected
- a full slot
- the whole status transition table, including the moves that must fail
- cancelling restores stock, refunds the wallet and frees the slot
- cancelling after preparation has started is refused
- staff-only functions refuse students; top-up range and unknown student
- the Google sign-in window: a profile with no student number can exist,
  but cannot order; and a duplicate student number is refused

## Reading a failure

Each line prints what it expected and what it got:

```
0 vetkoeks + 1 snoek   expected R10.00  got R10.00  PASS  units=0  label=No vetkoek, Snoek
cancel refunds wallet  expected R0.00   got R0.00   PASS
```

`units=0` on the snoek line is the one worth watching: fillings on their own
must not consume vetkoek stock.
