-- 0008_rls.sql
--
-- Row Level Security on every table.
--
-- The important idea: the app sends the same query whether a student or a
-- staff member is signed in. `select * from orders` returns your own orders
-- or all of them depending on who you are, decided here rather than by a
-- filter in the URL that a modified client could simply drop.
--
-- Orders, wallets and stock counts are never written directly. Those all go
-- through the security definer functions, which is why there is no insert or
-- update policy for them at all.

alter table public.profiles            enable row level security;
alter table public.menu_items          enable row level security;
alter table public.option_groups       enable row level security;
alter table public.options             enable row level security;
alter table public.collection_slots    enable row level security;
alter table public.orders              enable row level security;
alter table public.order_items         enable row level security;
alter table public.stock_movements     enable row level security;
alter table public.wallet_transactions enable row level security;
alter table public.notifications       enable row level security;
alter table public.loyalty_stamps      enable row level security;

-- ---------------------------------------------------------------------------
-- profiles
-- ---------------------------------------------------------------------------
create policy profiles_select_own on public.profiles
    for select to authenticated
    using (id = auth.uid() or public.is_staff());

-- A student may edit their own row, but only three columns — see the column
-- grants below. Role, wallet_balance and no_show_count are not among them,
-- so the app cannot promote itself or give itself money.
create policy profiles_update_own on public.profiles
    for update to authenticated
    using (id = auth.uid())
    with check (id = auth.uid());

-- ---------------------------------------------------------------------------
-- The menu: readable by anyone, writable by staff
-- ---------------------------------------------------------------------------
create policy menu_items_read on public.menu_items
    for select to anon, authenticated
    using (true);

create policy menu_items_write on public.menu_items
    for all to authenticated
    using (public.is_staff())
    with check (public.is_staff());

create policy option_groups_read on public.option_groups
    for select to anon, authenticated
    using (true);

create policy option_groups_write on public.option_groups
    for all to authenticated
    using (public.is_staff())
    with check (public.is_staff());

create policy options_read on public.options
    for select to anon, authenticated
    using (true);

create policy options_write on public.options
    for all to authenticated
    using (public.is_staff())
    with check (public.is_staff());

-- ---------------------------------------------------------------------------
-- collection_slots: read only. ensure_slots and place_order do the writing.
-- ---------------------------------------------------------------------------
create policy slots_read on public.collection_slots
    for select to authenticated
    using (true);

-- ---------------------------------------------------------------------------
-- orders and order_items: read only from the client
-- ---------------------------------------------------------------------------
-- No insert policy and no update policy anywhere here. place_order and
-- set_order_status are the only routes in, and they run as definer.
create policy orders_select on public.orders
    for select to authenticated
    using (student_id = auth.uid() or public.is_staff());

create policy order_items_select on public.order_items
    for select to authenticated
    using (
        exists (
            select 1 from public.orders o
            where o.id = order_items.order_id
              and (o.student_id = auth.uid() or public.is_staff())
        )
    );

-- ---------------------------------------------------------------------------
-- Ledgers
-- ---------------------------------------------------------------------------
create policy wallet_select on public.wallet_transactions
    for select to authenticated
    using (user_id = auth.uid() or public.is_staff());

create policy stock_movements_select on public.stock_movements
    for select to authenticated
    using (public.is_staff());

create policy loyalty_select on public.loyalty_stamps
    for select to authenticated
    using (user_id = auth.uid() or public.is_staff());

-- ---------------------------------------------------------------------------
-- notifications: yours, and you may only mark them read
-- ---------------------------------------------------------------------------
create policy notifications_select on public.notifications
    for select to authenticated
    using (user_id = auth.uid());

create policy notifications_mark_read on public.notifications
    for update to authenticated
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- ---------------------------------------------------------------------------
-- Column grants
-- ---------------------------------------------------------------------------
-- RLS decides which rows. These decide which columns, which is what actually
-- stops a student PATCHing their own wallet_balance or role: the policy
-- above would allow the row, and this refuses the column.
revoke all on public.profiles from anon, authenticated;
grant select on public.profiles to authenticated;
grant update (full_name, language, fcm_token) on public.profiles to authenticated;

revoke all on public.notifications from anon, authenticated;
grant select on public.notifications to authenticated;
grant update (is_read) on public.notifications to authenticated;

grant select on public.menu_items, public.option_groups, public.options
    to anon, authenticated;
grant insert, update, delete on public.menu_items, public.option_groups, public.options
    to authenticated;

grant select on public.collection_slots, public.orders, public.order_items,
                public.wallet_transactions, public.stock_movements,
                public.loyalty_stamps
    to authenticated;

-- ---------------------------------------------------------------------------
-- Function grants
-- ---------------------------------------------------------------------------
grant execute on function public.ensure_slots(date)                         to authenticated;
grant execute on function public.sales_summary(date, date)                  to authenticated;
grant execute on function public.place_order(uuid, payment_method, jsonb, uuid) to authenticated;
grant execute on function public.set_order_status(uuid, order_status)       to authenticated;
grant execute on function public.load_wallet(text, numeric)                 to authenticated;
grant execute on function public.is_staff()                                 to authenticated;

-- next_collection_code is an internal detail of place_order.
revoke execute on function public.next_collection_code() from anon, authenticated;
