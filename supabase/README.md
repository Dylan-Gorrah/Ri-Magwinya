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
SUPABASE_ANON_KEY=<the anon public key>
```

*Project Settings → API keys* → the **anon / public** key. Not the service
role key — that one never goes anywhere near the app.

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
