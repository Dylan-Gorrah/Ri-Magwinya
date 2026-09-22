-- Runs the brief's check values (section 9.1) through place_order, against
-- the real schema. Test harness only.

\set ON_ERROR_STOP on
\pset pager off

-- --------------------------------------------------------------------------
-- People
-- --------------------------------------------------------------------------
insert into auth.users (id, email, raw_user_meta_data)
values (
    '11111111-1111-1111-1111-111111111111',
    'thabo@gmail.com',
    '{"full_name":"Thabo Mokoena","student_number":"ST2024001"}'::jsonb
);

insert into auth.users (id, email, raw_user_meta_data)
values (
    '22222222-2222-2222-2222-222222222222',
    'staff@gmail.com',
    '{"full_name":"Tuckshop Staff","student_number":"STAFF001"}'::jsonb
);

update profiles set role = 'staff'
where id = '22222222-2222-2222-2222-222222222222';

-- Staff loads the student's wallet, which is also what unlocks
-- pay-at-counter for them later.
select act_as('22222222-2222-2222-2222-222222222222');
select load_wallet('ST2024001', 500.00);

select act_as('11111111-1111-1111-1111-111111111111');

-- --------------------------------------------------------------------------
-- Helpers to build the lines JSON from slugs and keys
-- --------------------------------------------------------------------------
create or replace function pg_temp.item(p_slug text)
returns uuid language sql as $$
    select id from menu_items where slug = p_slug;
$$;

create or replace function pg_temp.opt(p_slug text, p_group text, p_option text)
returns uuid language sql as $$
    select o.id
    from options o
    join option_groups og on og.id = o.group_id
    join menu_items mi on mi.id = og.item_id
    where mi.slug = p_slug and og.key = p_group and o.key = p_option;
$$;

create or replace function pg_temp.slot()
returns uuid language sql as $$
    select id from collection_slots
    where service_date = current_date order by starts_at limit 1;
$$;

create or replace function pg_temp.check_total(
    p_label text, p_lines jsonb, p_expected numeric
)
returns text language plpgsql as $$
declare
    v_order orders;
    v_units int;
begin
    v_order := place_order(pg_temp.slot(), 'wallet', p_lines, gen_random_uuid());
    select sum(units_consumed) into v_units from order_items where order_id = v_order.id;

    return format('%-46s expected %8s  got %8s  %s   units=%s  label=%s',
        p_label,
        'R' || to_char(p_expected, 'FM990.00'),
        'R' || to_char(v_order.total_amount, 'FM990.00'),
        case when v_order.total_amount = p_expected then 'PASS' else '** FAIL **' end,
        v_units,
        (select options_label from order_items where order_id = v_order.id limit 1)
    );
end;
$$;

-- --------------------------------------------------------------------------
-- The check values
-- --------------------------------------------------------------------------
select pg_temp.check_total(
    '2 vetkoeks + 2 polony + 1 cheese',
    jsonb_build_array(jsonb_build_object(
        'item_id', pg_temp.item('vetkoek'),
        'base_qty', 2,
        'options', jsonb_build_array(
            jsonb_build_object('option_id', pg_temp.opt('vetkoek','fill','polony'), 'count', 2),
            jsonb_build_object('option_id', pg_temp.opt('vetkoek','fill','cheese'), 'count', 1)
        ))),
    17.00);

select pg_temp.check_total(
    '0 vetkoeks + 1 snoek  (fillings only)',
    jsonb_build_array(jsonb_build_object(
        'item_id', pg_temp.item('vetkoek'),
        'base_qty', 0,
        'options', jsonb_build_array(
            jsonb_build_object('option_id', pg_temp.opt('vetkoek','fill','snoek'), 'count', 1)
        ))),
    10.00);

select pg_temp.check_total(
    '3 Chappies + 1 assorted mix packet',
    jsonb_build_array(jsonb_build_object(
        'item_id', pg_temp.item('sweets'),
        'options', jsonb_build_array(
            jsonb_build_object('option_id', pg_temp.opt('sweets','pick','chappies'), 'count', 3),
            jsonb_build_object('option_id', pg_temp.opt('sweets','pick','mix'), 'count', 1)
        ))),
    13.00);

select pg_temp.check_total(
    'Large chips  (size replaces the price)',
    jsonb_build_array(jsonb_build_object(
        'item_id', pg_temp.item('chips'),
        'quantity', 1,
        'options', jsonb_build_array(
            jsonb_build_object('option_id', pg_temp.opt('chips','size','l'))
        ))),
    40.00);

select pg_temp.check_total(
    'Tea, Five Roses, with milk, 2 sugars',
    jsonb_build_array(jsonb_build_object(
        'item_id', pg_temp.item('tea'),
        'quantity', 1,
        'options', jsonb_build_array(
            jsonb_build_object('option_id', pg_temp.opt('tea','brand','five')),
            jsonb_build_object('option_id', pg_temp.opt('tea','milk','with')),
            jsonb_build_object('option_id', pg_temp.opt('tea','sugar','s2'))
        ))),
    10.00);

select pg_temp.check_total(
    '4 Cokes  (outer quantity, not a build item)',
    jsonb_build_array(jsonb_build_object(
        'item_id', pg_temp.item('coke'),
        'quantity', 4)),
    64.00);
