-- 0002_tables.sql
-- Every table, with its constraints.
--
-- A note on where the rules live: as much as possible is a database
-- constraint rather than a check in application code. A slot cannot be
-- oversold because orders_taken has a check against capacity; a wallet cannot
-- go negative because the column says so. Application bugs then produce an
-- error instead of bad data.

-- ---------------------------------------------------------------------------
-- profiles: one row per auth user
-- ---------------------------------------------------------------------------
create table public.profiles (
    id              uuid primary key references auth.users (id) on delete cascade,
    full_name       text        not null,
    email           text        not null unique,
    -- The only thing tying an account to a real person: sign-up is open, and
    -- no email domain is enforced. First come, first served.
    --
    -- Nullable, deliberately. Google sign-in (Phase 12) creates the profile
    -- from a token that has no student number in it, so there is a moment
    -- between the account existing and the number being known. The app asks
    -- for it on the next screen; place_order refuses to work without it, so
    -- a profile in that state can browse and nothing else. Postgres allows
    -- many NULLs under a unique constraint, which is what makes this safe.
    student_number  text        unique,
    role            user_role   not null default 'student',
    wallet_balance  numeric(10, 2) not null default 0 check (wallet_balance >= 0),
    no_show_count   int         not null default 0 check (no_show_count >= 0),
    language        text        not null default 'en'
                    check (language in ('en', 'af', 'st')),
    fcm_token       text,
    created_at      timestamptz not null default now(),
    last_login      timestamptz
);

comment on column public.profiles.wallet_balance is
    'Running total. wallet_transactions is the truth; this is for speed.';

-- ---------------------------------------------------------------------------
-- menu_items and their options
-- ---------------------------------------------------------------------------
create table public.menu_items (
    id                  uuid primary key default gen_random_uuid(),
    slug                text not null unique,
    name                text not null,
    description         text,
    category            menu_category not null,
    price               numeric(10, 2) not null check (price >= 0),
    image_url           text,
    icon_key            text not null,
    stock_quantity      int  not null default 0 check (stock_quantity >= 0),
    reorder_level       int  not null default 5 check (reorder_level >= 0),
    is_available        boolean not null default true,
    temperature_tag     text check (temperature_tag in ('hot', 'cold')),

    -- The base step, for items built from a count of themselves plus extras.
    -- Vetkoek is the only one today: "Vetkoeks" / "vetkoek", minimum 0.
    base_step_label     text,
    base_step_singular  text,
    base_step_min       int check (base_step_min >= 0),

    sort_order          int not null default 0,
    updated_at          timestamptz not null default now(),

    -- Either all three base-step columns are set or none of them are.
    constraint base_step_all_or_nothing check (
        (base_step_label is null and base_step_singular is null and base_step_min is null)
        or
        (base_step_label is not null and base_step_singular is not null and base_step_min is not null)
    )
);

create table public.option_groups (
    id              uuid primary key default gen_random_uuid(),
    item_id         uuid not null references public.menu_items (id) on delete cascade,
    key             text not null,
    label           text not null,
    type            text not null check (type in ('single', 'qty')),
    -- When true, the chosen option's price replaces the item price rather
    -- than adding to it. Chips sizes are the only use: large is R40, not
    -- R28 + R40.
    replaces_price  boolean not null default false,
    sort_order      int not null default 0,

    unique (item_id, key),

    -- Only a single-select group can replace the price. A qty group has no
    -- single answer to replace it with.
    constraint only_single_replaces check (not replaces_price or type = 'single')
);

create table public.options (
    id          uuid primary key default gen_random_uuid(),
    group_id    uuid not null references public.option_groups (id) on delete cascade,
    key         text not null,
    name        text not null,
    price       numeric(10, 2) not null default 0 check (price >= 0),
    is_default  boolean not null default false,
    sort_order  int not null default 0,

    unique (group_id, key)
);

-- ---------------------------------------------------------------------------
-- collection_slots: per day, not templates
-- ---------------------------------------------------------------------------
create table public.collection_slots (
    id            uuid primary key default gen_random_uuid(),
    name          text not null,
    starts_at     time not null,
    ends_at       time not null,
    capacity      int  not null default 40 check (capacity > 0),
    orders_taken  int  not null default 0 check (orders_taken >= 0),
    service_date  date not null,

    constraint ends_after_start check (ends_at > starts_at),
    -- The cap is a database constraint. Even a bug in place_order cannot
    -- oversell a break.
    constraint within_capacity check (orders_taken <= capacity),
    unique (name, service_date)
);

-- ---------------------------------------------------------------------------
-- orders
-- ---------------------------------------------------------------------------
create table public.orders (
    id               uuid primary key default gen_random_uuid(),
    -- The #1047 a student sees. Separate from collection_code.
    order_number     bigint generated always as identity,
    student_id       uuid not null references public.profiles (id),
    collection_code  char(4) not null,
    slot_id          uuid not null references public.collection_slots (id),
    total_amount     numeric(10, 2) not null check (total_amount > 0),
    status           order_status not null default 'placed',
    payment_method   payment_method not null,

    -- Generated by the phone before the request goes out. This is what makes
    -- a retry safe: the same ref returns the existing order instead of
    -- creating a second one. Offline sync depends on it entirely.
    client_ref       uuid not null,

    placed_at        timestamptz not null default now(),
    prepared_at      timestamptz,
    ready_at         timestamptz,
    completed_at     timestamptz,
    cancelled_at     timestamptz,

    unique (student_id, client_ref)
);

create table public.order_items (
    id              uuid primary key default gen_random_uuid(),
    order_id        uuid not null references public.orders (id) on delete cascade,
    -- set null, not cascade: removing an item from the menu must not erase
    -- the history of it having been sold.
    item_id         uuid references public.menu_items (id) on delete set null,

    -- Snapshots, frozen at the moment the order was placed, so staff read
    -- what the student saw even after the menu changes.
    item_name       text not null,
    options_label   text,
    options         jsonb,

    quantity        int not null check (quantity > 0),
    unit_price      numeric(10, 2) not null check (unit_price >= 0),
    -- Differs from quantity for base-step items: zero vetkoeks with a snoek
    -- filling consumes no vetkoek stock.
    units_consumed  int not null check (units_consumed >= 0),

    subtotal        numeric(10, 2)
                    generated always as (quantity * unit_price) stored
);

-- ---------------------------------------------------------------------------
-- Ledgers and history
-- ---------------------------------------------------------------------------
create table public.stock_movements (
    id             uuid primary key default gen_random_uuid(),
    item_id        uuid references public.menu_items (id) on delete set null,
    -- A delta, never an absolute. Two staff each selling three lands at -6,
    -- which is what makes offline stock edits safe to replay.
    change_amount  int not null,
    reason         stock_reason not null,
    staff_id       uuid references public.profiles (id),
    order_id       uuid references public.orders (id) on delete set null,
    created_at     timestamptz not null default now()
);

create table public.wallet_transactions (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references public.profiles (id) on delete cascade,
    -- Positive in, negative out. Sum these and you get wallet_balance.
    amount      numeric(10, 2) not null check (amount <> 0),
    type        wallet_txn_type not null,
    order_id    uuid references public.orders (id) on delete set null,
    staff_id    uuid references public.profiles (id),
    created_at  timestamptz not null default now()
);

create table public.notifications (
    id        uuid primary key default gen_random_uuid(),
    user_id   uuid not null references public.profiles (id) on delete cascade,
    title     text not null,
    message   text not null,
    type      notification_type not null,
    is_read   boolean not null default false,
    order_id  uuid references public.orders (id) on delete cascade,
    sent_at   timestamptz not null default now()
);

create table public.loyalty_stamps (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references public.profiles (id) on delete cascade,
    -- One order can only ever produce one stamp. A unique constraint instead
    -- of careful code.
    order_id    uuid not null unique references public.orders (id) on delete cascade,
    -- Unused: loyalty is progress only, no redemption. Kept so that adding
    -- redemption later is a feature rather than a migration.
    redeemed    boolean not null default false,
    created_at  timestamptz not null default now()
);
