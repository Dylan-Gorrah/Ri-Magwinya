-- 0012_hardening.sql
--
-- Fixes from the first run against the real Supabase project.
--
-- 1. Function privileges. Postgres grants EXECUTE on every new function to
--    PUBLIC, and Supabase additionally grants it to anon. So every function
--    was callable signed out through /rest/v1/rpc/*. Each one checked
--    auth.uid() and refused, so nothing leaked, but a signed-out caller
--    should not reach them at all. Trigger functions and internal helpers
--    are now callable by nobody; the app-facing ones by signed-in users only.
--
-- 2. Time zone. The database runs in UTC and the tuckshop in
--    Africa/Johannesburg (UTC+2). current_date and extract(hour ...) were
--    UTC, so between midnight and 02:00 "today" was yesterday, and the sales
--    chart put the 10:00 rush at 08:00. Pinned per function rather than for
--    the whole database, so it cannot be undone by a dashboard setting.
--
-- 3. Student numbers. claim_student_number upper-cased and trimmed; sign-up
--    and load_wallet did not. "ab123" and "AB123" could both be registered,
--    and staff typing lowercase on the top-up screen would miss the student.
--    All three now normalise the same way.
--
-- 4. touch_updated_at had no fixed search_path.

-- ---------------------------------------------------------------------------
-- 1. Privileges
-- ---------------------------------------------------------------------------
revoke execute on function public.handle_new_user()      from public, anon, authenticated;
revoke execute on function public.log_stock_change()     from public, anon, authenticated;
revoke execute on function public.touch_updated_at()     from public, anon, authenticated;
revoke execute on function public.next_collection_code() from public, anon, authenticated;

revoke execute on function public.is_staff()                                    from public, anon;
revoke execute on function public.ensure_slots(date)                            from public, anon;
revoke execute on function public.place_order(uuid, payment_method, jsonb, uuid) from public, anon;
revoke execute on function public.set_order_status(uuid, order_status)          from public, anon;
revoke execute on function public.load_wallet(text, numeric)                    from public, anon;
revoke execute on function public.sales_summary(date, date)                     from public, anon;
revoke execute on function public.claim_student_number(text)                    from public, anon;

-- ---------------------------------------------------------------------------
-- 2. Time zone
-- ---------------------------------------------------------------------------
alter function public.place_order(uuid, payment_method, jsonb, uuid) set timezone = 'Africa/Johannesburg';
alter function public.ensure_slots(date)                            set timezone = 'Africa/Johannesburg';
alter function public.sales_summary(date, date)                     set timezone = 'Africa/Johannesburg';

-- ---------------------------------------------------------------------------
-- 3. Student numbers
-- ---------------------------------------------------------------------------
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_number text;
begin
    v_number := nullif(upper(trim(new.raw_user_meta_data ->> 'student_number')), '');

    insert into public.profiles (id, full_name, email, student_number, role)
    values (
        new.id,
        coalesce(
            nullif(trim(new.raw_user_meta_data ->> 'full_name'), ''),
            nullif(trim(new.raw_user_meta_data ->> 'name'), ''),
            split_part(new.email, '@', 1)
        ),
        new.email,
        v_number,
        'student'
    );

    return new;
exception
    when unique_violation then
        raise exception 'STUDENT_NUMBER_TAKEN'
            using hint = 'That student number is already registered.';
end;
$$;

-- create or replace keeps grants, but be explicit: nobody calls this directly.
revoke execute on function public.handle_new_user() from public, anon, authenticated;

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

    if p_amount is null or p_amount < 10 or p_amount > 1000 then
        raise exception 'AMOUNT_OUT_OF_RANGE';
    end if;

    select * into v_profile
    from public.profiles
    where student_number = upper(trim(p_student_number))
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

-- Any rows written before this migration.
update public.profiles
set student_number = upper(trim(student_number))
where student_number is not null
  and student_number <> upper(trim(student_number));

-- ---------------------------------------------------------------------------
-- 4. search_path
-- ---------------------------------------------------------------------------
alter function public.touch_updated_at() set search_path = public, pg_temp;
