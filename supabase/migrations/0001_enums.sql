-- 0001_enums.sql
-- The fixed vocabularies the rest of the schema is built from.
--
-- These are enums rather than text-with-a-check because they are referenced
-- from function signatures and from the app, and a typo should be a database
-- error rather than a row nobody notices.

create type user_role as enum ('student', 'staff');

create type menu_category as enum ('Meals', 'Snacks', 'Drinks');

create type order_status as enum (
    'placed',
    'preparing',
    'ready',
    'collected',
    'cancelled',
    'no_show'
);

create type payment_method as enum ('wallet', 'counter');

create type stock_reason as enum (
    'sale',
    'restock',
    'waste',
    'correction',
    'cancel_restore'
);

create type wallet_txn_type as enum ('topup', 'payment', 'refund');

create type notification_type as enum (
    'order_placed',
    'order_ready',
    'low_stock',
    'wallet_topup'
);
