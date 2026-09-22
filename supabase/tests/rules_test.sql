-- The business rules, exercised against the real schema. Test harness only.
\set ON_ERROR_STOP on
\pset pager off

create or replace function pg_temp.item(p_slug text)
returns uuid language sql as $$ select id from menu_items where slug = p_slug; $$;

create or replace function pg_temp.slot(p_n int default 1)
returns uuid language sql as $$
    select id from collection_slots where service_date = current_date
    order by starts_at offset (p_n - 1) limit 1;
$$;

create or replace function pg_temp.line(p_slug text, p_qty int default 1)
returns jsonb language sql as $$
    select jsonb_build_array(jsonb_build_object(
        'item_id', (select id from menu_items where slug = p_slug),
        'quantity', p_qty));
$$;

-- Runs something that should fail, and reports the error code it raised.
create or replace function pg_temp.expect_fail(p_label text, p_sql text, p_expected text)
returns text language plpgsql as $$
begin
    execute p_sql;
    return format('%-44s expected %-24s got SUCCESS       ** FAIL **', p_label, p_expected);
exception when others then
    return format('%-44s expected %-24s got %-16s %s',
        p_label, p_expected, sqlerrm,
        case when sqlerrm = p_expected then 'PASS' else '** FAIL **' end);
end;
$$;

create or replace function pg_temp.say(p_label text, p_got text, p_expected text)
returns text language sql as $$
    select format('%-44s expected %-24s got %-16s %s',
        p_label, p_expected, p_got,
        case when p_got = p_expected then 'PASS' else '** FAIL **' end);
$$;

-- --------------------------------------------------------------------------
-- A second student, with no top-up
-- --------------------------------------------------------------------------
insert into auth.users (id, email, raw_user_meta_data)
values ('33333333-3333-3333-3333-333333333333', 'new@gmail.com',
        '{"full_name":"New Student","student_number":"ST2024002"}'::jsonb);

-- ==========================================================================
select '--- pay at the counter ---' as section;
-- ==========================================================================
select act_as('33333333-3333-3333-3333-333333333333');

select pg_temp.expect_fail(
    'counter, never topped up',
    format('select place_order(%L, %L, %L::jsonb, gen_random_uuid())',
           pg_temp.slot(), 'counter', pg_temp.line('coke')),
    'COUNTER_BLOCKED');

select pg_temp.expect_fail(
    'wallet, empty wallet',
    format('select place_order(%L, %L, %L::jsonb, gen_random_uuid())',
           pg_temp.slot(), 'wallet', pg_temp.line('coke')),
    'INSUFFICIENT_FUNDS');

-- Staff take real money at the speed point, which unlocks counter payment.
select act_as('22222222-2222-2222-2222-222222222222');
select load_wallet('ST2024002', 20.00);
select act_as('33333333-3333-3333-3333-333333333333');

select pg_temp.say('counter, after one top-up',
    (select (place_order(pg_temp.slot(), 'counter', pg_temp.line('coke'),
        gen_random_uuid())).status::text), 'placed');

-- ==========================================================================
select '--- idempotency ---' as section;
-- ==========================================================================
select act_as('11111111-1111-1111-1111-111111111111');

do $$
declare
    v_ref uuid := gen_random_uuid();
    v_a uuid; v_b uuid; v_before numeric; v_after numeric;
begin
    select wallet_balance into v_before from profiles
    where id = '11111111-1111-1111-1111-111111111111';

    v_a := (place_order(pg_temp.slot(), 'wallet', pg_temp.line('coke'), v_ref)).id;
    v_b := (place_order(pg_temp.slot(), 'wallet', pg_temp.line('coke'), v_ref)).id;

    select wallet_balance into v_after from profiles
    where id = '11111111-1111-1111-1111-111111111111';

    raise notice '%', pg_temp.say('same client_ref twice, one order',
        case when v_a = v_b then 'same order' else 'two orders' end, 'same order');
    raise notice '%', pg_temp.say('charged once, not twice',
        'R' || to_char(v_before - v_after, 'FM990.00'), 'R16.00');
end $$;

-- ==========================================================================
select '--- stock ---' as section;
-- ==========================================================================
-- Score has 4 on the shelf.
select pg_temp.expect_fail(
    'ordering more than is in stock',
    format('select place_order(%L, %L, %L::jsonb, gen_random_uuid())',
           pg_temp.slot(), 'wallet', pg_temp.line('score', 5)),
    'OUT_OF_STOCK');

select pg_temp.expect_fail(
    'sweets with nothing chosen',
    format('select place_order(%L, %L, %L::jsonb, gen_random_uuid())',
           pg_temp.slot(), 'wallet',
           jsonb_build_array(jsonb_build_object('item_id', pg_temp.item('sweets')))),
    'EMPTY_SELECTION');

-- ==========================================================================
select '--- slot capacity ---' as section;
-- ==========================================================================
-- Fill the afternoon slot: one order in, then shrink capacity to match.
-- (capacity must stay > 0, so the slot is filled rather than zeroed.)
select place_order(pg_temp.slot(3), 'wallet', pg_temp.line('coke'), gen_random_uuid());
update collection_slots set capacity = orders_taken
where id = pg_temp.slot(3) and orders_taken > 0;

select pg_temp.expect_fail(
    'a full slot',
    format('select place_order(%L, %L, %L::jsonb, gen_random_uuid())',
           pg_temp.slot(3), 'wallet', pg_temp.line('coke')),
    'SLOT_FULL');

-- ==========================================================================
select '--- status transitions ---' as section;
-- ==========================================================================
do $$
declare
    v_order orders;
    v_stock_before int;
    v_stock_after int;
    v_balance_before numeric;
    v_balance_after numeric;
    v_taken_before int;
    v_taken_after int;
begin
    -- a fresh order to push through the happy path
    perform act_as('11111111-1111-1111-1111-111111111111');
    v_order := place_order(pg_temp.slot(), 'wallet', pg_temp.line('twizza'), gen_random_uuid());

    -- a student may not promote their own order
    begin
        perform set_order_status(v_order.id, 'preparing');
        raise notice '%', pg_temp.say('student marking own order preparing', 'SUCCESS', 'FORBIDDEN');
    exception when others then
        raise notice '%', pg_temp.say('student marking own order preparing', sqlerrm, 'FORBIDDEN');
    end;

    perform act_as('22222222-2222-2222-2222-222222222222');

    -- skipping a step is refused
    begin
        perform set_order_status(v_order.id, 'ready');
        raise notice '%', pg_temp.say('staff skipping placed -> ready', 'SUCCESS', 'INVALID_TRANSITION');
    exception when others then
        raise notice '%', pg_temp.say('staff skipping placed -> ready', sqlerrm, 'INVALID_TRANSITION');
    end;

    v_order := set_order_status(v_order.id, 'preparing');
    v_order := set_order_status(v_order.id, 'ready');
    v_order := set_order_status(v_order.id, 'collected');

    raise notice '%', pg_temp.say('placed -> preparing -> ready -> collected',
        v_order.status::text, 'collected');
    raise notice '%', pg_temp.say('loyalty stamp written on collect',
        (select count(*)::text from loyalty_stamps where order_id = v_order.id), '1');
    raise notice '%', pg_temp.say('completed_at set',
        case when v_order.completed_at is not null then 'set' else 'null' end, 'set');

    -- and it cannot be moved again
    begin
        perform set_order_status(v_order.id, 'ready');
        raise notice '%', pg_temp.say('un-collecting an order', 'SUCCESS', 'INVALID_TRANSITION');
    exception when others then
        raise notice '%', pg_temp.say('un-collecting an order', sqlerrm, 'INVALID_TRANSITION');
    end;

    -- -------------------------------------------------------------
    -- cancelling puts everything back
    -- -------------------------------------------------------------
    perform act_as('11111111-1111-1111-1111-111111111111');
    select stock_quantity into v_stock_before from menu_items where slug = 'energade';
    select wallet_balance into v_balance_before from profiles
        where id = '11111111-1111-1111-1111-111111111111';
    select orders_taken into v_taken_before from collection_slots where id = pg_temp.slot();

    v_order := place_order(pg_temp.slot(), 'wallet', pg_temp.line('energade', 2), gen_random_uuid());
    v_order := set_order_status(v_order.id, 'cancelled');

    select stock_quantity into v_stock_after from menu_items where slug = 'energade';
    select wallet_balance into v_balance_after from profiles
        where id = '11111111-1111-1111-1111-111111111111';
    select orders_taken into v_taken_after from collection_slots where id = pg_temp.slot();

    raise notice '%', pg_temp.say('cancel restores stock',
        (v_stock_after - v_stock_before)::text, '0');
    raise notice '%', pg_temp.say('cancel refunds the wallet',
        'R' || to_char(v_balance_after - v_balance_before, 'FM990.00'), 'R0.00');
    raise notice '%', pg_temp.say('cancel frees the slot',
        (v_taken_after - v_taken_before)::text, '0');
    raise notice '%', pg_temp.say('refund row written',
        (select count(*)::text from wallet_transactions
         where order_id = v_order.id and type = 'refund'), '1');

    -- once preparing, the student loses the option
    perform act_as('11111111-1111-1111-1111-111111111111');
    v_order := place_order(pg_temp.slot(), 'wallet', pg_temp.line('twizza'), gen_random_uuid());
    perform act_as('22222222-2222-2222-2222-222222222222');
    v_order := set_order_status(v_order.id, 'preparing');
    perform act_as('11111111-1111-1111-1111-111111111111');
    begin
        perform set_order_status(v_order.id, 'cancelled');
        raise notice '%', pg_temp.say('cancel after preparing starts', 'SUCCESS', 'INVALID_TRANSITION');
    exception when others then
        raise notice '%', pg_temp.say('cancel after preparing starts', sqlerrm, 'INVALID_TRANSITION');
    end;
end $$;

-- ==========================================================================
select '--- no-shows withdraw counter payment ---' as section;
-- ==========================================================================
do $$
declare
    v_order orders;
    i int;
begin
    for i in 1..2 loop
        perform act_as('33333333-3333-3333-3333-333333333333');
        v_order := place_order(pg_temp.slot(), 'counter', pg_temp.line('coke'), gen_random_uuid());
        perform act_as('22222222-2222-2222-2222-222222222222');
        v_order := set_order_status(v_order.id, 'preparing');
        v_order := set_order_status(v_order.id, 'ready');
        v_order := set_order_status(v_order.id, 'no_show');
    end loop;

    raise notice '%', pg_temp.say('no_show_count after two',
        (select no_show_count::text from profiles
         where id = '33333333-3333-3333-3333-333333333333'), '2');

    perform act_as('33333333-3333-3333-3333-333333333333');
    begin
        perform place_order(pg_temp.slot(), 'counter', pg_temp.line('coke'), gen_random_uuid());
        raise notice '%', pg_temp.say('counter after two no-shows', 'SUCCESS', 'COUNTER_BLOCKED');
    exception when others then
        raise notice '%', pg_temp.say('counter after two no-shows', sqlerrm, 'COUNTER_BLOCKED');
    end;
end $$;

-- ==========================================================================
select '--- staff-only functions ---' as section;
-- ==========================================================================
select act_as('11111111-1111-1111-1111-111111111111');

select pg_temp.expect_fail('student calling load_wallet',
    'select load_wallet(''ST2024001'', 50)', 'FORBIDDEN');
select pg_temp.expect_fail('student calling sales_summary',
    'select sales_summary()', 'FORBIDDEN');
-- As staff, so the range check is what fires rather than the role check.
select act_as('22222222-2222-2222-2222-222222222222');

select pg_temp.expect_fail('top-up above the ceiling',
    'select load_wallet(''ST2024001'', 5000)', 'AMOUNT_OUT_OF_RANGE');
select pg_temp.expect_fail('top-up below the floor',
    'select load_wallet(''ST2024001'', 5)', 'AMOUNT_OUT_OF_RANGE');
select pg_temp.expect_fail('top-up for an unknown student',
    'select load_wallet(''NOPE999'', 50)', 'STUDENT_NOT_FOUND');

-- ==========================================================================
select '--- the Google sign-in window ---' as section;
-- ==========================================================================
-- A profile can exist before its student number does: Phase 12 creates it
-- from a token that has no number in it. Browsing is fine; ordering is not.
insert into auth.users (id, email, raw_user_meta_data)
values ('44444444-4444-4444-4444-444444444444', 'google@gmail.com',
        '{"full_name":"Google Signin"}'::jsonb);

select pg_temp.say('profile created without a number',
    (select case when student_number is null then 'null' else 'set' end
     from profiles where id = '44444444-4444-4444-4444-444444444444'), 'null');

select act_as('44444444-4444-4444-4444-444444444444');
select pg_temp.expect_fail('ordering without a student number',
    format('select place_order(%L, %L, %L::jsonb, gen_random_uuid())',
           pg_temp.slot(), 'wallet', pg_temp.line('coke')),
    'STUDENT_NUMBER_REQUIRED');

select pg_temp.expect_fail('claiming a taken student number',
    'insert into auth.users (email, raw_user_meta_data) values (''dup@gmail.com'', ''{"full_name":"Dup","student_number":"ST2024001"}''::jsonb)',
    'STUDENT_NUMBER_TAKEN');
