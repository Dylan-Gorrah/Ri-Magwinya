-- 0006_set_order_status.sql
--
-- The only way an order changes state. The transition table lives here, not
-- in the app, so a modified client cannot un-collect an order or skip
-- straight from placed to ready.
--
--   placed    -> preparing   staff
--   preparing -> ready       staff      pushes a notification
--   ready     -> collected   staff      +1 loyalty stamp
--   ready     -> no_show     staff      +1 no_show_count
--   placed    -> cancelled   the student who placed it
--
-- Anything else is INVALID_TRANSITION.

create or replace function public.set_order_status(
    p_order_id uuid,
    p_status   order_status
)
returns public.orders
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user    uuid := auth.uid();
    v_staff   boolean := public.is_staff();
    v_order   public.orders;
    v_line    public.order_items;
begin
    if v_user is null then
        raise exception 'NOT_AUTHENTICATED';
    end if;

    select * into v_order
    from public.orders
    where id = p_order_id
    for update;

    if not found then
        raise exception 'ORDER_NOT_FOUND';
    end if;

    -- -----------------------------------------------------------------
    -- Who is allowed to ask for this
    -- -----------------------------------------------------------------
    if p_status = 'cancelled' then
        -- A student may cancel their own order, and only while nothing has
        -- been made yet. Once the kitchen starts, cancelling means the
        -- tuckshop eats the cost.
        if not v_staff and v_order.student_id <> v_user then
            raise exception 'FORBIDDEN';
        end if;
        if v_order.status <> 'placed' then
            raise exception 'INVALID_TRANSITION'
                using detail = v_order.status::text || ' -> cancelled';
        end if;
    else
        -- Everything else is staff only. In particular 'collected': if
        -- students could close their own orders, the record of what went
        -- over the counter would be a record of what students said went
        -- over the counter.
        if not v_staff then
            raise exception 'FORBIDDEN';
        end if;

        if not (
            (v_order.status = 'placed'    and p_status = 'preparing') or
            (v_order.status = 'preparing' and p_status = 'ready')     or
            (v_order.status = 'ready'     and p_status = 'collected') or
            (v_order.status = 'ready'     and p_status = 'no_show')
        ) then
            raise exception 'INVALID_TRANSITION'
                using detail = v_order.status::text || ' -> ' || p_status::text;
        end if;
    end if;

    -- -----------------------------------------------------------------
    -- Side effects
    -- -----------------------------------------------------------------
    if p_status = 'cancelled' then
        perform set_config('rimagwinya.skip_stock_log', 'on', true);

        -- Stock back, with a reason that explains itself in the history.
        for v_line in
            select * from public.order_items where order_id = v_order.id
        loop
            if v_line.units_consumed > 0 and v_line.item_id is not null then
                update public.menu_items
                set stock_quantity = stock_quantity + v_line.units_consumed
                where id = v_line.item_id;

                insert into public.stock_movements
                    (item_id, change_amount, reason, staff_id, order_id)
                values
                    (v_line.item_id, v_line.units_consumed, 'cancel_restore',
                     case when v_staff then v_user else null end, v_order.id);
            end if;
        end loop;

        -- Money back, but only if money was taken. A counter order has
        -- nothing to refund.
        if v_order.payment_method = 'wallet' then
            update public.profiles
            set wallet_balance = wallet_balance + v_order.total_amount
            where id = v_order.student_id;

            insert into public.wallet_transactions (user_id, amount, type, order_id)
            values (v_order.student_id, v_order.total_amount, 'refund', v_order.id);
        end if;

        -- The slot goes back on sale.
        update public.collection_slots
        set orders_taken = greatest(orders_taken - 1, 0)
        where id = v_order.slot_id;

        perform set_config('rimagwinya.skip_stock_log', 'off', true);
    end if;

    if p_status = 'no_show' then
        -- Nothing is given back. The food was made, the slot was held, the
        -- money stays taken. What changes is the count that withdraws
        -- pay-at-counter at two.
        update public.profiles
        set no_show_count = no_show_count + 1
        where id = v_order.student_id;
    end if;

    if p_status = 'collected' then
        -- order_id is unique on loyalty_stamps, so a double tap or a
        -- retried request cannot mint a second stamp.
        insert into public.loyalty_stamps (user_id, order_id)
        values (v_order.student_id, v_order.id)
        on conflict (order_id) do nothing;
    end if;

    -- -----------------------------------------------------------------
    -- Write it
    -- -----------------------------------------------------------------
    update public.orders
    set status       = p_status,
        prepared_at  = case when p_status = 'preparing' then now() else prepared_at end,
        ready_at     = case when p_status = 'ready'     then now() else ready_at end,
        completed_at = case when p_status = 'collected' then now() else completed_at end,
        cancelled_at = case when p_status in ('cancelled', 'no_show') then now() else cancelled_at end
    where id = v_order.id
    returning * into v_order;

    if p_status = 'ready' then
        insert into public.notifications (user_id, title, message, type, order_id)
        values (
            v_order.student_id,
            'Order #' || v_order.order_number || ' is ready',
            'Collect at the tuckshop. Your code is ' || v_order.collection_code || '.',
            'order_ready',
            v_order.id
        );
    end if;

    return v_order;
end;
$$;
