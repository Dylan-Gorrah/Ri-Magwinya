-- 0013_adjust_stock.sql
--
-- Stock steppers send a change, not a total.
--
-- The brief had the stock screen PATCH stock_quantity to a number. That
-- loses sales: a staff member reads 12, a student buys one (11), the staff
-- member taps + and writes 13 — the sale has vanished from the count. A
-- delta applied in the database cannot lose anything, and it is what makes
-- replaying a queued offline adjustment safe (Phase 10): +3 applied late is
-- still +3.
--
-- The movement is logged by the existing log_stock_change trigger, with
-- auth.uid() as the staff member.

create or replace function public.adjust_stock(p_item_id uuid, p_delta int)
returns public.menu_items
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_item public.menu_items;
begin
    if not public.is_staff() then
        raise exception 'FORBIDDEN';
    end if;

    if p_delta is null or p_delta = 0 or abs(p_delta) > 1000 then
        raise exception 'BAD_DELTA';
    end if;

    update public.menu_items
    -- Never below zero: taking off more than is there means "none left".
    set stock_quantity = greatest(stock_quantity + p_delta, 0)
    where id = p_item_id
    returning * into v_item;

    if not found then
        raise exception 'ITEM_NOT_FOUND';
    end if;

    return v_item;
end;
$$;

revoke execute on function public.adjust_stock(uuid, int) from public, anon;
grant execute on function public.adjust_stock(uuid, int) to authenticated;
