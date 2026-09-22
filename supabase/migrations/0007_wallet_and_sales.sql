-- 0007_wallet_and_sales.sql
-- Loading a wallet, and reading the day back.

-- ---------------------------------------------------------------------------
-- load_wallet(): the only function that creates value out of nothing
-- ---------------------------------------------------------------------------
-- The money already moved: the student tapped their card on the tuckshop's
-- speed point, which is the bank's machine and has nothing to do with this
-- app. This records that it happened.
--
-- Because it is the one operation that mints balance, it is never anonymous —
-- staff_id is always written.
create or replace function public.load_wallet(
    p_student_number text,
    p_amount         numeric
)
returns public.profiles
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_staff   uuid := auth.uid();
    v_profile public.profiles;
begin
    if not public.is_staff() then
        raise exception 'FORBIDDEN';
    end if;

    -- Checked here as well as in the Edge Function and the app. The app's
    -- check makes the screen pleasant; this one makes the data safe.
    if p_amount is null or p_amount < 10 or p_amount > 1000 then
        raise exception 'AMOUNT_OUT_OF_RANGE';
    end if;

    select * into v_profile
    from public.profiles
    where student_number = p_student_number
    for update;

    if not found then
        raise exception 'STUDENT_NOT_FOUND';
    end if;

    insert into public.wallet_transactions (user_id, amount, type, staff_id)
    values (v_profile.id, p_amount, 'topup', v_staff);

    update public.profiles
    set wallet_balance = wallet_balance + p_amount
    where id = v_profile.id
    returning * into v_profile;

    insert into public.notifications (user_id, title, message, type)
    values (
        v_profile.id,
        'Wallet topped up',
        'R' || to_char(p_amount, 'FM999999990.00') ||
            ' added. Your balance is R' ||
            to_char(v_profile.wallet_balance, 'FM999999990.00') || '.',
        'wallet_topup'
    );

    return v_profile;
end;
$$;

-- ---------------------------------------------------------------------------
-- sales_summary(): the staff reporting screen, aggregated in the database
-- ---------------------------------------------------------------------------
-- Deliberately not "fetch every order and add them up in Kotlin". That
-- means downloading a year of history to show three numbers, over campus
-- wifi, getting slower every month.
create or replace function public.sales_summary(
    p_from date default current_date,
    p_to   date default current_date
)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, pg_temp
as $$
declare
    v_result jsonb;
begin
    if not public.is_staff() then
        raise exception 'FORBIDDEN';
    end if;

    with sold as (
        select *
        from public.orders
        where status = 'collected'
          and placed_at::date between p_from and p_to
    ),
    totals as (
        select
            coalesce(sum(total_amount), 0)::numeric(12, 2) as revenue,
            count(*)                                       as order_count,
            coalesce(avg(total_amount), 0)::numeric(10, 2) as average_order
        from sold
    ),
    by_hour as (
        select
            extract(hour from placed_at)::int as hour,
            count(*)                          as orders
        from sold
        group by 1
        order by 1
    ),
    top_items as (
        select
            oi.item_name,
            sum(oi.quantity)::int              as quantity,
            sum(oi.subtotal)::numeric(12, 2)   as revenue
        from public.order_items oi
        join sold o on o.id = oi.order_id
        group by oi.item_name
        order by quantity desc, revenue desc
        limit 10
    )
    select jsonb_build_object(
        'from',          p_from,
        'to',            p_to,
        'revenue',       (select revenue from totals),
        'order_count',   (select order_count from totals),
        'average_order', (select average_order from totals),
        'orders_by_hour', coalesce(
            (select jsonb_agg(jsonb_build_object('hour', hour, 'orders', orders))
             from by_hour), '[]'::jsonb),
        'top_items', coalesce(
            (select jsonb_agg(jsonb_build_object(
                'item_name', item_name,
                'quantity',  quantity,
                'revenue',   revenue))
             from top_items), '[]'::jsonb)
    )
    into v_result;

    return v_result;
end;
$$;
