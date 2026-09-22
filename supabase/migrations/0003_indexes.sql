-- 0003_indexes.sql
-- Indexes for the queries the app actually makes.
--
-- Postgres indexes primary keys and unique constraints already, so only the
-- foreign keys and the filter columns need anything here.

-- The menu, ordered as the student sees it.
create index menu_items_sort_idx
    on public.menu_items (sort_order, name);

create index menu_items_category_idx
    on public.menu_items (category)
    where is_available;

-- Nested option loading: one request fetches items with their groups and
-- options, so both foreign keys get an index.
create index option_groups_item_idx on public.option_groups (item_id, sort_order);
create index options_group_idx      on public.options (group_id, sort_order);

-- The staff queue: everything still open, newest first.
create index orders_open_idx
    on public.orders (placed_at desc)
    where status in ('placed', 'preparing', 'ready');

-- A student's own order history.
create index orders_student_idx
    on public.orders (student_id, placed_at desc);

create index orders_slot_idx on public.orders (slot_id);

-- Collection code lookup is scoped to today's open orders, which is the only
-- set it has to be unique within.
create index orders_code_open_idx
    on public.orders (collection_code)
    where status in ('placed', 'preparing', 'ready');

create index order_items_order_idx on public.order_items (order_id);

-- Slots are always fetched for one day.
create index collection_slots_date_idx
    on public.collection_slots (service_date, starts_at);

-- Ledgers, read newest first.
create index wallet_transactions_user_idx
    on public.wallet_transactions (user_id, created_at desc);

-- Used to decide pay-at-counter eligibility, which asks only whether any
-- top-up exists for this user.
create index wallet_transactions_topup_idx
    on public.wallet_transactions (user_id)
    where type = 'topup';

create index stock_movements_item_idx
    on public.stock_movements (item_id, created_at desc);

-- The bell's unread count.
create index notifications_user_idx
    on public.notifications (user_id, sent_at desc);

create index notifications_unread_idx
    on public.notifications (user_id)
    where not is_read;

create index loyalty_stamps_user_idx
    on public.loyalty_stamps (user_id)
    where not redeemed;

-- Staff lookup by student number on the top-up screen.
create index profiles_student_number_idx on public.profiles (student_number);
