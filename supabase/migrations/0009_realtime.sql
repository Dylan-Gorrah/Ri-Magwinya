-- 0009_realtime.sql
--
-- Three tables broadcast their changes.
--
--   orders           a student placing one appears on the staff queue, and a
--                    staff tap appears on the student's order screen, with
--                    nobody refreshing anything
--   menu_items       the last Score energy selling goes Sold out on every
--                    phone at once
--   collection_slots a break filling up while someone is looking at the
--                    checkout screen, rather than failing at the last step
--
-- Realtime respects RLS, so a student subscribed to orders still only
-- receives their own.

alter publication supabase_realtime add table public.orders;
alter publication supabase_realtime add table public.menu_items;
alter publication supabase_realtime add table public.collection_slots;

-- Realtime sends the old row on updates and deletes only when the table has
-- a replica identity to build it from. Without this, a status change arrives
-- with no way to tell what it changed from.
alter table public.orders           replica identity full;
alter table public.menu_items       replica identity full;
alter table public.collection_slots replica identity full;
