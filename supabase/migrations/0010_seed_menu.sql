-- 0010_seed_menu.sql
--
-- The opening menu, from section 8 of the brief with the descriptions taken
-- from MENU_SEED in the prototype.
--
-- Written as upserts on the slug so it can be re-run without duplicating
-- anything. Note that it does NOT reset stock_quantity on conflict — running
-- this again after a day of trading should not quietly refill the shelves.

insert into public.menu_items (
    slug, name, description, category, price, icon_key, stock_quantity,
    reorder_level, temperature_tag,
    base_step_label, base_step_singular, base_step_min, sort_order
)
values
    ('wors', 'Boerewors roll',
     'Grilled wors on a soft roll with tomato and onion relish.',
     'Meals', 30, 'wors', 22, 5, 'hot', null, null, null, 10),

    ('vetkoek', 'Vetkoek',
     'Fried fresh every morning. Build it however you like it.',
     'Meals', 3, 'vetkoek', 40, 8, 'hot', 'Vetkoeks', 'vetkoek', 0, 20),

    ('chips', 'Fried chips',
     'Hot slap chips. Salt and vinegar at the counter.',
     'Meals', 28, 'chips', 30, 5, 'hot', null, null, null, 30),

    ('simba', 'Simba chips', 'Assorted flavours.',
     'Snacks', 12, 'packet', 36, 6, null, null, null, null, 40),

    ('doritos', 'Doritos', 'Sharing bag.',
     'Snacks', 25, 'triangle', 18, 5, null, null, null, null, 50),

    ('sweets', 'Assorted sweets', 'Pick and mix from the jar at the counter.',
     'Snacks', 0, 'sweet', 80, 10, null, null, null, null, 60),

    ('tea', 'Tea', 'Served hot. Brewed to order at the counter.',
     'Drinks', 10, 'cup', 50, 10, 'hot', null, null, null, 70),

    ('coffee', 'Coffee', 'Filter coffee, served hot.',
     'Drinks', 10, 'cup', 50, 10, 'hot', null, null, null, 80),

    ('coke', 'Coca-Cola 440ml', 'Served chilled.',
     'Drinks', 16, 'can', 44, 6, 'cold', null, null, null, 90),

    ('fanta', 'Fanta Orange 440ml', 'Served chilled.',
     'Drinks', 16, 'can', 32, 6, 'cold', null, null, null, 100),

    ('stoney', 'Stoney Ginger Beer 440ml', '440ml can.',
     'Drinks', 16, 'can', 24, 6, 'cold', null, null, null, 110),

    ('cremesoda', 'Sparletta Creme Soda 440ml', '440ml can.',
     'Drinks', 16, 'can', 20, 6, 'cold', null, null, null, 120),

    ('twizza', 'Twizza 500ml', 'Local and easy on the pocket.',
     'Drinks', 12, 'bottle', 28, 6, 'cold', null, null, null, 130),

    ('energade', 'Energade 500ml', 'Sports drink, assorted flavours.',
     'Drinks', 18, 'bottle', 22, 5, 'cold', null, null, null, 140),

    ('switch', 'Switch Energy 500ml', 'South African, halaal certified.',
     'Drinks', 20, 'energy', 16, 5, 'cold', null, null, null, 150),

    ('mofaya', 'MoFaya 500ml', 'Proudly local energy drink.',
     'Drinks', 22, 'energy', 14, 5, 'cold', null, null, null, 160),

    ('score', 'Score Energy 500ml', 'Classic value energy drink.',
     'Drinks', 18, 'energy', 4, 5, 'cold', null, null, null, 170)

on conflict (slug) do update set
    name               = excluded.name,
    description        = excluded.description,
    category           = excluded.category,
    price              = excluded.price,
    icon_key           = excluded.icon_key,
    reorder_level      = excluded.reorder_level,
    temperature_tag    = excluded.temperature_tag,
    base_step_label    = excluded.base_step_label,
    base_step_singular = excluded.base_step_singular,
    base_step_min      = excluded.base_step_min,
    sort_order         = excluded.sort_order;
    -- stock_quantity deliberately not touched.


-- ---------------------------------------------------------------------------
-- Option groups and their options
-- ---------------------------------------------------------------------------
-- A small helper keeps the repetition down and makes the intent readable.
create or replace function pg_temp.seed_group(
    p_slug     text,
    p_key      text,
    p_label    text,
    p_type     text,
    p_replaces boolean,
    p_sort     int,
    p_options  jsonb   -- [{key,name,price,is_default}]
)
returns void
language plpgsql
as $$
declare
    v_item_id  uuid;
    v_group_id uuid;
    v_opt      jsonb;
    v_i        int := 0;
begin
    select id into v_item_id from public.menu_items where slug = p_slug;
    if v_item_id is null then
        raise exception 'seed: no menu item with slug %', p_slug;
    end if;

    insert into public.option_groups (item_id, key, label, type, replaces_price, sort_order)
    values (v_item_id, p_key, p_label, p_type, p_replaces, p_sort)
    on conflict (item_id, key) do update set
        label          = excluded.label,
        type           = excluded.type,
        replaces_price = excluded.replaces_price,
        sort_order     = excluded.sort_order
    returning id into v_group_id;

    for v_opt in select * from jsonb_array_elements(p_options)
    loop
        v_i := v_i + 10;
        insert into public.options (group_id, key, name, price, is_default, sort_order)
        values (
            v_group_id,
            v_opt ->> 'key',
            v_opt ->> 'name',
            coalesce((v_opt ->> 'price')::numeric, 0),
            coalesce((v_opt ->> 'is_default')::boolean, false),
            v_i
        )
        on conflict (group_id, key) do update set
            name       = excluded.name,
            price      = excluded.price,
            is_default = excluded.is_default,
            sort_order = excluded.sort_order;
    end loop;
end;
$$;

-- Vetkoek fillings. A qty group, so each filling has its own stepper and the
-- vetkoek becomes a build item: quantity is always 1 and the steppers carry
-- the count.
select pg_temp.seed_group('vetkoek', 'fill', 'Fillings', 'qty', false, 10, '[
    {"key": "polony", "name": "Polony",       "price": 3},
    {"key": "liver",  "name": "Liver spread", "price": 4},
    {"key": "salami", "name": "Salami",       "price": 4},
    {"key": "cheese", "name": "Cheese slice", "price": 5},
    {"key": "achar",  "name": "Achar",        "price": 5},
    {"key": "snoek",  "name": "Snoek",        "price": 10}
]'::jsonb);

-- Chips sizes. The only group in the app that replaces the price: large is
-- R40, not R28 + R40.
select pg_temp.seed_group('chips', 'size', 'Choose a size', 'single', true, 10, '[
    {"key": "s", "name": "Small",  "price": 28, "is_default": true},
    {"key": "m", "name": "Medium", "price": 35},
    {"key": "l", "name": "Large",  "price": 40}
]'::jsonb);

-- Sweets. Priced at R0 with everything in a qty group, which is why
-- place_order refuses a line whose unit price came out at zero.
select pg_temp.seed_group('sweets', 'pick', 'Pick your sweets', 'qty', false, 10, '[
    {"key": "chappies", "name": "Chappies",            "price": 1},
    {"key": "fizzer",   "name": "Fizzer",              "price": 2},
    {"key": "sparkles", "name": "Sparkles",            "price": 3},
    {"key": "lolly",    "name": "Lollipop",            "price": 3},
    {"key": "mix",      "name": "Assorted mix packet", "price": 10}
]'::jsonb);

-- Tea and coffee. Every option is free, so tea is R10 whatever you do to it.
-- They still matter: staff need to know what to make.
select pg_temp.seed_group('tea', 'brand', 'Which tea', 'single', false, 10, '[
    {"key": "five",    "name": "Five Roses",       "price": 0, "is_default": true},
    {"key": "rooibos", "name": "Freshpak Rooibos", "price": 0},
    {"key": "joko",    "name": "Joko",             "price": 0},
    {"key": "glen",    "name": "Glen",             "price": 0}
]'::jsonb);

select pg_temp.seed_group('tea', 'milk', 'Milk', 'single', false, 20, '[
    {"key": "with",  "name": "With milk", "price": 0, "is_default": true},
    {"key": "black", "name": "Black",     "price": 0}
]'::jsonb);

select pg_temp.seed_group('tea', 'sugar', 'Sugar', 'single', false, 30, '[
    {"key": "s0", "name": "No sugar", "price": 0, "is_default": true},
    {"key": "s1", "name": "1 spoon",  "price": 0},
    {"key": "s2", "name": "2 spoons", "price": 0},
    {"key": "s3", "name": "3 spoons", "price": 0}
]'::jsonb);

select pg_temp.seed_group('coffee', 'milk', 'Milk', 'single', false, 10, '[
    {"key": "with",  "name": "With milk", "price": 0, "is_default": true},
    {"key": "black", "name": "Black",     "price": 0}
]'::jsonb);

select pg_temp.seed_group('coffee', 'sugar', 'Sugar', 'single', false, 20, '[
    {"key": "s0", "name": "No sugar", "price": 0, "is_default": true},
    {"key": "s1", "name": "1 spoon",  "price": 0},
    {"key": "s2", "name": "2 spoons", "price": 0},
    {"key": "s3", "name": "3 spoons", "price": 0}
]'::jsonb);

-- Today's slots, so the app has something to show before anyone opens
-- checkout. ensure_slots creates them on every other day.
select public.ensure_slots(current_date);
