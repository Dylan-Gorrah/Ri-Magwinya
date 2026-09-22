# The backend

Everything here runs on Supabase project `jykswltsegssjmvnqqbi`.

## Applying the migrations

Run them **in order**. They depend on each other: tables reference enums,
functions reference tables, policies reference functions.

| # | File | What it does |
|---|---|---|
| 0001 | `enums.sql` | The fixed vocabularies |
| 0002 | `tables.sql` | Every table and its constraints |
| 0003 | `indexes.sql` | Indexes for the queries the app makes |
| 0004 | `helpers.sql` | `is_staff`, `handle_new_user`, stock logging, `ensure_slots`, code generation |
| 0005 | `place_order.sql` | The big one — pricing, stock, slots, money, all in one transaction |
| 0006 | `set_order_status.sql` | The status transition table |
| 0007 | `wallet_and_sales.sql` | `load_wallet`, `sales_summary` |
| 0008 | `rls.sql` | Row Level Security and column grants |
| 0009 | `realtime.sql` | Live updates on orders, menu items and slots |
| 0010 | `seed_menu.sql` | The opening menu and today's slots |
| 0011 | `claim_student_number.sql` | Set a student number once, for Google sign-in |
| 0012 | `hardening.sql` | Function privileges, Johannesburg time zone, student-number casing |

### Through the SQL Editor

Supabase dashboard → **SQL Editor** → paste one file → **Run** → check it says
success → next file.

### Through the MCP connector

Once the connector is signed in, Claude applies them directly and reports
back. Nothing to paste.

## After the migrations

**1. Turn off email confirmation** for development.

*Authentication → Sign In / Providers → Email* → switch off **Confirm email**.
Otherwise every test account has to be confirmed from an inbox before it can
sign in.

**2. Put the keys in `local.properties`** (gitignored, never committed):

```properties
SUPABASE_URL=https://jykswltsegssjmvnqqbi.supabase.co
SUPABASE_ANON_KEY=<the publishable key, sb_publishable_…>
```

*Project Settings → API keys* → the **publishable** key. Not the secret key —
that one never goes anywhere near the app.

Then rebuild. `AppConfig.isBackendConfigured` flips to true and the menu
screen starts fetching.

## Checking it worked

```sql
-- 17 items
select count(*) from menu_items;

-- 8 option groups across vetkoek, chips, sweets, tea and coffee
select mi.slug, og.label, og.type, og.replaces_price
from option_groups og join menu_items mi on mi.id = og.item_id
order by mi.sort_order, og.sort_order;

-- today's three breaks
select name, starts_at, ends_at, capacity, orders_taken
from collection_slots where service_date = current_date order by starts_at;
```

And the one that proves Row Level Security is doing its job — an anonymous
caller can read the menu but not orders:

```bash
# 200, with the menu
curl "https://jykswltsegssjmvnqqbi.supabase.co/rest/v1/menu_items?select=name" \
  -H "apikey: <anon key>"

# [] — the rows exist, the policy hides them
curl "https://jykswltsegssjmvnqqbi.supabase.co/rest/v1/orders?select=*" \
  -H "apikey: <anon key>"
```

An empty array rather than a 403 is correct: RLS filters rows, it does not
announce that there were rows to filter.

## Making a staff account

There is no "sign up as staff" — it would be the first thing anyone tried.

1. *Authentication → Users → Add user*, with an email and password
2. Then promote it:

```sql
update profiles
set role = 'staff', student_number = coalesce(student_number, 'STAFF001')
where email = 'the-address-you-used@example.com';
```

## Edge Functions

Three thin wrappers in `functions/`, each calling one SQL function:
`place-order`, `order-status`, `load-wallet`. Shared code is in
`functions/_shared/` (request handling, caller verification, the FCM sender).

**They are deployed with `verify_jwt = false`, on purpose.** This project
uses the new `sb_publishable_…` / `sb_secret_…` keys, and the platform's
built-in JWT check only understands the legacy keys. Each function verifies
the user's token itself in `_shared/auth.ts`. `config.toml` records the
setting so a CLI deploy keeps it.

Deploy through the MCP connector, or with the CLI:

```
supabase functions deploy place-order  --project-ref jykswltsegssjmvnqqbi
supabase functions deploy order-status --project-ref jykswltsegssjmvnqqbi
supabase functions deploy load-wallet  --project-ref jykswltsegssjmvnqqbi
```

### Trying them with curl

Get a user token first (student or staff):

```bash
K=<publishable key>; U=https://jykswltsegssjmvnqqbi.supabase.co
T=$(curl -s -X POST "$U/auth/v1/token?grant_type=password" -H "apikey: $K"   -H "Content-Type: application/json"   -d '{"email":"user123@gmail.com","password":"..."}' | jq -r .access_token)
```

Place an order — 2 vetkoeks with 2 polony and a cheese slice (R17):

```bash
curl -X POST "$U/functions/v1/place-order" -H "apikey: $K" -H "Authorization: Bearer $T"   -H "Content-Type: application/json" -d '{
    "slot_id": "<today's slot id>", "payment_method": "wallet",
    "client_ref": "'$(uuidgen)'",
    "lines": [{ "item_id": "<vetkoek id>", "base_qty": 2,
                "options": [{ "option_id": "<polony id>", "count": 2 },
                            { "option_id": "<cheese id>", "count": 1 }] }] }'
# 409 {"code":"INSUFFICIENT_FUNDS","detail":"15.00"}   detail = rands short
# 409 {"code":"OUT_OF_STOCK","detail":"Score Energy 500ml"}
# 409 {"code":"COUNTER_BLOCKED","detail":"NO_TOPUP_YET" | "TOO_MANY_NO_SHOWS"}
```

Move an order along (staff token), or cancel your own while placed (student):

```bash
curl -X PATCH "$U/functions/v1/order-status" -H "apikey: $K" -H "Authorization: Bearer $T"   -H "Content-Type: application/json" -d '{"order_id":"<id>","status":"preparing"}'
# 409 {"code":"INVALID_TRANSITION","detail":"placed -> ready"}
# 403 {"code":"FORBIDDEN"}   a student trying a staff move
```

Top up a wallet (staff token):

```bash
curl -X POST "$U/functions/v1/load-wallet" -H "apikey: $K" -H "Authorization: Bearer $T"   -H "Content-Type: application/json" -d '{"student_number":"TEST001","amount":100}'
# 200 {"full_name":"Test Student","student_number":"TEST001","wallet_balance":100}
# 404 {"code":"STUDENT_NOT_FOUND"} · 400 {"code":"AMOUNT_OUT_OF_RANGE"} · 403 not staff
```

Pushes are sent only once `FCM_SERVICE_ACCOUNT` and `FCM_PROJECT_ID` are set
as Edge Function secrets (Phase 11). Until then the sender does nothing and
never fails a request.

## Things worth knowing

**`student_number` is nullable.** Google sign-in creates a profile from a
token that has no student number in it, so there is a moment where the
account exists and the number does not. `place_order` refuses to work
without one, so such a profile can browse and nothing else.

**Nothing writes orders, stock or balances directly.** There are no insert or
update policies on `orders`, `order_items` or `wallet_transactions` at all.
Everything goes through the security definer functions, which is what makes
"one transaction that either completes or does nothing" true rather than
aspirational.

**`place_order` is idempotent** on `(student_id, client_ref)`. Send the same
`client_ref` twice and you get the same order back, not two. That is what
makes retries and offline replay safe.

**Prices are recomputed server-side** from the database's own rows. The
client sends selections, never prices.
