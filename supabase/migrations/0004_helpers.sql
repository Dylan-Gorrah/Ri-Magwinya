-- 0004_helpers.sql
-- The small functions and the triggers, before the big ones.

-- ---------------------------------------------------------------------------
-- is_staff(): the single source of truth for "may this caller do staff things"
-- ---------------------------------------------------------------------------
-- security definer because it reads profiles, and the RLS policies on
-- profiles are themselves written in terms of this function. Without
-- definer rights that is a loop.
create or replace function public.is_staff()
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select exists (
        select 1
        from public.profiles
        where id = auth.uid()
          and role = 'staff'
    );
$$;

-- ---------------------------------------------------------------------------
-- handle_new_user(): create the profile when an auth user appears
-- ---------------------------------------------------------------------------
-- Note what is NOT here: any check on the email domain. Students use
-- ordinary personal accounts, so there is nothing to check against. The
-- student number is the identity, and it is unique.
--
-- Role is always 'student'. A staff account is made by hand in the Supabase
-- dashboard and promoted with SQL, because "sign up as staff" would be the
-- first thing anyone tried.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_number text;
begin
    v_number := nullif(trim(new.raw_user_meta_data ->> 'student_number'), '');

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
        -- Almost always the student number, since the email is already
        -- unique in auth.users. Give the app something it can show.
        raise exception 'STUDENT_NUMBER_TAKEN'
            using hint = 'That student number is already registered.';
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row
    execute function public.handle_new_user();

-- ---------------------------------------------------------------------------
-- log_stock_change(): every movement of stock explains itself
-- ---------------------------------------------------------------------------
-- staff_id comes from auth.uid() inside the trigger rather than from the
-- app, so it cannot be spoofed or forgotten.
--
-- place_order and set_order_status write their own movement rows with the
-- right reason and order_id, and set the guard below so this trigger stays
-- out of their way.
create or replace function public.log_stock_change()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
    if new.stock_quantity = old.stock_quantity then
        return new;
    end if;

    -- Set by the order functions, which log their own movements.
    if coalesce(current_setting('rimagwinya.skip_stock_log', true), 'off') = 'on' then
        return new;
    end if;

    insert into public.stock_movements (item_id, change_amount, reason, staff_id)
    values (
        new.id,
        new.stock_quantity - old.stock_quantity,
        case
            when new.stock_quantity > old.stock_quantity then 'restock'
            else 'correction'
        end,
        auth.uid()
    );

    return new;
end;
$$;

create trigger menu_items_stock_logged
    after update of stock_quantity on public.menu_items
    for each row
    execute function public.log_stock_change();

-- Keep updated_at honest without the app having to remember.
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

create trigger menu_items_touch_updated
    before update on public.menu_items
    for each row
    execute function public.touch_updated_at();

-- ---------------------------------------------------------------------------
-- ensure_slots(): create the day's three breaks if they are not there
-- ---------------------------------------------------------------------------
-- Safe to call from every phone on every checkout load. Does nothing when
-- the rows already exist.
create or replace function public.ensure_slots(p_date date default current_date)
returns setof public.collection_slots
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
    insert into public.collection_slots (name, starts_at, ends_at, capacity, service_date)
    values
        ('First break',  time '09:40', time '10:00', 40, p_date),
        ('Second break', time '11:00', time '11:20', 40, p_date),
        ('Afternoon',    time '14:30', time '14:50', 25, p_date)
    on conflict (name, service_date) do nothing;

    return query
        select *
        from public.collection_slots
        where service_date = p_date
        order by starts_at;
end;
$$;

-- ---------------------------------------------------------------------------
-- next_collection_code(): four digits, unique among today's open orders
-- ---------------------------------------------------------------------------
-- Only has to be unique within the set staff are actually looking at, which
-- is a few dozen rows against 10,000 possibilities. On the rare collision it
-- simply picks again.
create or replace function public.next_collection_code()
returns char(4)
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_code char(4);
    v_tries int := 0;
begin
    loop
        v_code := lpad((floor(random() * 10000))::int::text, 4, '0');

        exit when not exists (
            select 1
            from public.orders
            where collection_code = v_code
              and status in ('placed', 'preparing', 'ready')
        );

        v_tries := v_tries + 1;
        if v_tries > 50 then
            raise exception 'CODE_SPACE_EXHAUSTED';
        end if;
    end loop;

    return v_code;
end;
$$;
