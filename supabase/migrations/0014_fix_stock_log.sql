-- 0014_fix_stock_log.sql
--
-- log_stock_change() from 0004 wrote its CASE result straight into
-- stock_movements.reason. A CASE of string literals is text, the column is
-- the stock_reason enum, and Postgres does not cast between them implicitly
-- here — so every manual stock change by staff failed with 42804. Orders
-- never hit it because place_order and set_order_status log their own
-- movements and switch the trigger off. Found by the first real
-- adjust_stock call.

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

    if coalesce(current_setting('rimagwinya.skip_stock_log', true), 'off') = 'on' then
        return new;
    end if;

    insert into public.stock_movements (item_id, change_amount, reason, staff_id)
    values (
        new.id,
        new.stock_quantity - old.stock_quantity,
        case
            when new.stock_quantity > old.stock_quantity then 'restock'::stock_reason
            else 'correction'::stock_reason
        end,
        auth.uid()
    );

    return new;
end;
$$;

revoke execute on function public.log_stock_change() from public, anon, authenticated;
