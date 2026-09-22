-- 0005_place_order.sql
--
-- The most important function in the app. One transaction that either
-- completes entirely or does nothing at all.
--
-- The price is recomputed here from the database's own rows. Whatever the
-- phone thought the total was is ignored. PriceCalculator.kt runs the same
-- formula so the student sees the right number, but this is the one that
-- decides what they are charged.
--
-- Expected lines shape:
--   [
--     { "item_id": uuid,
--       "base_qty": int,          -- base-step items only
--       "quantity": int,          -- non-build items only, defaults to 1
--       "options": [ { "option_id": uuid, "count": int } ] }
--   ]

create or replace function public.place_order(
    p_slot_id        uuid,
    p_payment_method payment_method,
    p_lines          jsonb,
    p_client_ref     uuid
)
returns public.orders
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_user      uuid := auth.uid();
    v_profile   public.profiles;
    v_slot      public.collection_slots;
    v_order     public.orders;
    v_existing  public.orders;

    v_line          jsonb;
    v_item          public.menu_items;
    v_group         public.option_groups;
    v_option        public.options;
    v_opt           jsonb;

    v_has_base      boolean;
    v_has_qty_group boolean;
    v_is_build      boolean;
    v_base_qty      int;
    v_quantity      int;
    v_units         int;
    v_unit_price    numeric(10, 2);
    v_replaced      boolean;
    v_count         int;
    v_total         numeric(10, 2) := 0;

    v_label_parts   text[];
    v_label         text;
    v_code          char(4);
    v_has_topped_up boolean;
begin
    if v_user is null then
        raise exception 'NOT_AUTHENTICATED';
    end if;

    -- Idempotency. A retried request, or an offline order being replayed,
    -- returns the order that already exists rather than making another.
    -- Without this a flaky connection is the difference between one vetkoek
    -- and two.
    select * into v_existing
    from public.orders
    where student_id = v_user and client_ref = p_client_ref;

    if found then
        return v_existing;
    end if;

    if p_lines is null or jsonb_array_length(p_lines) = 0 then
        raise exception 'EMPTY_ORDER';
    end if;

    -- Lock the buyer. Two orders from the same student cannot both pass the
    -- balance check on the same money.
    select * into v_profile
    from public.profiles
    where id = v_user
    for update;

    if not found then
        raise exception 'NO_PROFILE';
    end if;

    -- Browsing is allowed without a student number; ordering is not, because
    -- staff need something to call out at the counter.
    if v_profile.student_number is null then
        raise exception 'STUDENT_NUMBER_REQUIRED';
    end if;

    -- -----------------------------------------------------------------
    -- Pay at the counter: the one place the tuckshop extends credit
    -- -----------------------------------------------------------------
    if p_payment_method = 'counter' then
        -- Sign-up is open, so a brand new account is not necessarily a real
        -- person. A top-up means staff have physically seen them at the
        -- speed point with real money, which no sign-up form can check.
        select exists (
            select 1 from public.wallet_transactions
            where user_id = v_user and type = 'topup'
        ) into v_has_topped_up;

        if not v_has_topped_up then
            raise exception 'COUNTER_BLOCKED'
                using detail = 'NO_TOPUP_YET';
        end if;

        if v_profile.no_show_count >= 2 then
            raise exception 'COUNTER_BLOCKED'
                using detail = 'TOO_MANY_NO_SHOWS';
        end if;
    end if;

    -- -----------------------------------------------------------------
    -- The slot
    -- -----------------------------------------------------------------
    select * into v_slot
    from public.collection_slots
    where id = p_slot_id
    for update;

    if not found then
        raise exception 'SLOT_NOT_FOUND';
    end if;

    if v_slot.service_date <> current_date then
        raise exception 'SLOT_NOT_TODAY';
    end if;

    if v_slot.orders_taken >= v_slot.capacity then
        raise exception 'SLOT_FULL'
            using detail = v_slot.name;
    end if;

    -- -----------------------------------------------------------------
    -- Create the order shell so the lines have something to hang off
    -- -----------------------------------------------------------------
    v_code := public.next_collection_code();

    insert into public.orders (
        student_id, collection_code, slot_id, total_amount,
        status, payment_method, client_ref
    )
    values (
        v_user, v_code, p_slot_id,
        -- Replaced below once the lines are priced. The check constraint
        -- wants a positive number, so start at one cent.
        0.01,
        'placed', p_payment_method, p_client_ref
    )
    returning * into v_order;

    -- The order functions log their own stock movements with the right
    -- reason and order_id, so the generic trigger stands down.
    perform set_config('rimagwinya.skip_stock_log', 'on', true);

    -- -----------------------------------------------------------------
    -- Price and write every line
    -- -----------------------------------------------------------------
    for v_line in select * from jsonb_array_elements(p_lines)
    loop
        select * into v_item
        from public.menu_items
        where id = (v_line ->> 'item_id')::uuid
        for update;

        if not found then
            raise exception 'ITEM_NOT_FOUND';
        end if;

        if not v_item.is_available then
            raise exception 'OUT_OF_STOCK' using detail = v_item.name;
        end if;

        v_has_base := v_item.base_step_label is not null;

        select exists (
            select 1 from public.option_groups
            where item_id = v_item.id and type = 'qty'
        ) into v_has_qty_group;

        -- The steppers ARE the quantity on a build item, so there is no
        -- outer quantity to multiply by.
        v_is_build := v_has_base or v_has_qty_group;

        v_base_qty := coalesce((v_line ->> 'base_qty')::int, 0);
        v_quantity := case
            when v_is_build then 1
            else greatest(coalesce((v_line ->> 'quantity')::int, 1), 1)
        end;

        if v_has_base then
            if v_base_qty < coalesce(v_item.base_step_min, 0) then
                raise exception 'INVALID_BASE_QTY';
            end if;
            -- Zero vetkoeks is legal: fillings on their own.
            v_unit_price := v_item.price * v_base_qty;
            v_units := v_base_qty;
        else
            v_unit_price := v_item.price;
            v_units := v_quantity;
        end if;

        v_label_parts := array[]::text[];
        v_replaced := false;

        if v_has_base then
            v_label_parts := v_label_parts || case
                when v_base_qty = 0 then 'No ' || v_item.base_step_singular
                when v_base_qty = 1 then '1 ' || v_item.base_step_singular
                else v_base_qty || ' ' || lower(v_item.base_step_label)
            end;
        end if;

        -- Walk the item's groups in order, so the label reads the way the
        -- sheet was laid out rather than the order the phone sent.
        for v_group in
            select * from public.option_groups
            where item_id = v_item.id
            order by sort_order, label
        loop
            if v_group.type = 'qty' then
                for v_option in
                    select * from public.options
                    where group_id = v_group.id
                    order by sort_order, name
                loop
                    select coalesce((elem ->> 'count')::int, 0)
                    into v_count
                    from jsonb_array_elements(coalesce(v_line -> 'options', '[]'::jsonb)) elem
                    where (elem ->> 'option_id')::uuid = v_option.id
                    limit 1;

                    v_count := coalesce(v_count, 0);

                    if v_count > 0 then
                        v_unit_price := v_unit_price + v_option.price * v_count;
                        v_label_parts := v_label_parts || case
                            when v_count = 1 then v_option.name
                            else v_count || ' ' || v_option.name
                        end;
                    end if;
                end loop;
            else
                select o.* into v_option
                from public.options o
                join jsonb_array_elements(coalesce(v_line -> 'options', '[]'::jsonb)) elem
                     on (elem ->> 'option_id')::uuid = o.id
                where o.group_id = v_group.id
                limit 1;

                if found then
                    if v_group.replaces_price then
                        -- Large chips is R40, not R28 + R40.
                        v_unit_price := v_unit_price - v_item.price + v_option.price;
                        v_replaced := true;
                    else
                        v_unit_price := v_unit_price + v_option.price;
                    end if;
                    v_label_parts := v_label_parts || v_option.name;
                elsif v_group.replaces_price then
                    raise exception 'OPTION_REQUIRED' using detail = v_group.label;
                end if;
            end if;
        end loop;

        -- Sweets are priced at zero with everything in a qty group, so an
        -- untouched sheet costs nothing and must not be orderable.
        if v_unit_price <= 0 then
            raise exception 'EMPTY_SELECTION' using detail = v_item.name;
        end if;

        if v_units > 0 and v_item.stock_quantity < v_units then
            raise exception 'OUT_OF_STOCK' using detail = v_item.name;
        end if;

        v_label := nullif(array_to_string(v_label_parts, ', '), '');

        insert into public.order_items (
            order_id, item_id, item_name, options_label, options,
            quantity, unit_price, units_consumed
        )
        values (
            v_order.id, v_item.id, v_item.name, v_label,
            coalesce(v_line -> 'options', '[]'::jsonb),
            v_quantity, v_unit_price, v_units
        );

        v_total := v_total + v_unit_price * v_quantity;

        if v_units > 0 then
            update public.menu_items
            set stock_quantity = stock_quantity - v_units
            where id = v_item.id;

            insert into public.stock_movements
                (item_id, change_amount, reason, staff_id, order_id)
            values (v_item.id, -v_units, 'sale', null, v_order.id);
        end if;
    end loop;

    -- -----------------------------------------------------------------
    -- Money
    -- -----------------------------------------------------------------
    if p_payment_method = 'wallet' then
        if v_profile.wallet_balance < v_total then
            raise exception 'INSUFFICIENT_FUNDS'
                using detail = (v_total - v_profile.wallet_balance)::text;
        end if;

        update public.profiles
        set wallet_balance = wallet_balance - v_total
        where id = v_user;

        insert into public.wallet_transactions (user_id, amount, type, order_id)
        values (v_user, -v_total, 'payment', v_order.id);
    end if;

    update public.orders
    set total_amount = v_total
    where id = v_order.id
    returning * into v_order;

    update public.collection_slots
    set orders_taken = orders_taken + 1
    where id = p_slot_id;

    insert into public.notifications (user_id, title, message, type, order_id)
    values (
        v_user,
        'Order #' || v_order.order_number || ' placed',
        'Collect at ' || v_slot.name || '. Your code is ' || v_order.collection_code || '.',
        'order_placed',
        v_order.id
    );

    perform set_config('rimagwinya.skip_stock_log', 'off', true);

    return v_order;
end;
$$;
